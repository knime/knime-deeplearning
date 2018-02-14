# -*- coding: utf-8 -*-

# ------------------------------------------------------------------------
#  Copyright by KNIME AG, Zurich, Switzerland
#  Website: http://www.knime.com; Email: contact@knime.com
#
#  This program is free software; you can redistribute it and/or modify
#  it under the terms of the GNU General Public License, Version 3, as
#  published by the Free Software Foundation.
#
#  This program is distributed in the hope that it will be useful, but
#  WITHOUT ANY WARRANTY; without even the implied warranty of
#  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
#  GNU General Public License for more details.
#
#  You should have received a copy of the GNU General Public License
#  along with this program; if not, see <http://www.gnu.org/licenses>.
#
#  Additional permission under GNU GPL version 3 section 7:
#
#  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
#  Hence, KNIME and ECLIPSE are both independent programs and are not
#  derived from each other. Should, however, the interpretation of the
#  GNU GPL Version 3 ("License") under any applicable laws result in
#  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
#  you the additional permission to use and propagate KNIME together with
#  ECLIPSE with only the license terms in place for ECLIPSE applying to
#  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
#  license terms of ECLIPSE themselves allow for the respective use and
#  propagation of ECLIPSE together with KNIME.
#
#  Additional permission relating to nodes for KNIME that extend the Node
#  Extension (and in particular that are based on subclasses of NodeModel,
#  NodeDialog, and NodeView) and that only interoperate with KNIME through
#  standard APIs ("Nodes"):
#  Nodes are deemed to be separate and independent programs and to not be
#  covered works.  Notwithstanding anything to the contrary in the
#  License, the License does not apply to Nodes, you are not required to
#  license Nodes under the License, and you are granted a license to
#  prepare and propagate Nodes, in each case even if such Nodes are
#  propagated with or for interoperation with KNIME.  The owner of a Node
#  may freely choose the license terms applicable to such Node, including
#  when such Node is propagated with or for interoperation with KNIME.
# ------------------------------------------------------------------------

'''
@author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
@author Christian Dietz, KNIME GmbH, Konstanz, Germany
'''

import abc

import keras
import numpy as np
import pandas as pd
from keras.models import load_model
from keras.models import model_from_json
from keras.models import model_from_yaml

from DLKerasTrainingCallbacks import DLKerasTrainingMonitor
from DLPythonDataBuffers import DLPythonDoubleBuffer
from DLPythonDataBuffers import DLPythonFloatBuffer
from DLPythonDataBuffers import DLPythonIntBuffer
from DLPythonDataBuffers import DLPythonLongBuffer
from DLPythonInstallationTester import compare_versions
from DLPythonNetwork import DLPythonNetwork
from DLPythonNetwork import DLPythonNetworkReader
from DLPythonNetwork import DLPythonNetworkSpec
from DLPythonNetwork import DLPythonTrainingConfig


class DLKerasNetworkReader(DLPythonNetworkReader):
    __metaclass__ = abc.ABCMeta

    @abc.abstractmethod
    def read(self, path, compile=True):
        raise NotImplementedError()

    @abc.abstractmethod
    def read_from_json(self, path):
        raise NotImplementedError()

    @abc.abstractmethod
    def read_from_yaml(self, path):
        raise NotImplementedError()

    def _read_internal(self, path, compile=True):
        return load_model(path, compile=compile)

    def _read_from_json_internal(self, path):
        f = open(path, 'r')
        model_json_string = f.read()
        f.close()
        return model_from_json(model_json_string)

    def _read_from_yaml_internal(self, path):
        f = open(path, 'r')
        model_yaml_string = f.read()
        f.close()
        return model_from_yaml(model_yaml_string, )


class DLKerasNetwork(DLPythonNetwork):
    __metaclass__ = abc.ABCMeta

    def __init__(self, model):
        super().__init__(model)

    @abc.abstractmethod
    def _get_tensor_spec(self, layer, node_idx, tensor_idx, tensor_id, tensor, tensor_shape, dimension_order):
        raise NotImplementedError()

    @property
    def spec(self):
        if self._spec is None:
            model = self._model
            model_inputs = model.inputs
            model_outputs = model.outputs

            # tensors:
            visited_inputs = set()
            visited_outputs = set()

            input_specs = [None] * len(model_inputs)
            intermediate_output_specs = list()
            output_specs = [None] * len(model_outputs)

            output_layer_tensor_names = {}

            dimension_order = self._determine_dimension_order()

            for layer in model.layers:
                # "inbound_nodes" became private API with Keras 2.1.3
                inbound_nodes = layer.inbound_nodes if hasattr(layer, 'inbound_nodes') else layer._inbound_nodes
                for node_idx in range(0, len(inbound_nodes)):
                    # inputs:
                    input_tensors = layer.get_input_at(node_idx)
                    input_shapes = layer.get_input_shape_at(node_idx)
                    # some layers have multiple inputs, some do not
                    if not isinstance(input_tensors, list):
                        input_tensors = [input_tensors]
                        input_shapes = [input_shapes]
                    for tensor_idx, input_tensor in enumerate(input_tensors):
                        if input_tensor in model_inputs and input_tensor not in visited_inputs:
                            visited_inputs.add(input_tensor)
                            # back end independent 'canonical' name
                            tensor_id = layer.name + '_' + str(node_idx) + ':' + str(tensor_idx) + '_in'
                            tensor_shape = input_shapes[tensor_idx]
                            tensor_spec = self._get_tensor_spec(layer, node_idx, tensor_idx,
                                                                tensor_id, input_tensor, tensor_shape, dimension_order)
                            # preserve order of input tensors
                            input_specs[model_inputs.index(input_tensor)] = tensor_spec
                    # outputs:
                    output_tensors = layer.get_output_at(node_idx)
                    output_shapes = layer.get_output_shape_at(node_idx)
                    # some layers have multiple outputs, some do not
                    if not isinstance(output_tensors, list):
                        output_tensors = [output_tensors]
                        output_shapes = [output_shapes]
                    for tensor_idx, output_tensor in enumerate(output_tensors):
                        if output_tensor not in visited_outputs:
                            visited_outputs.add(output_tensor)
                            # back end independent 'canonical' name
                            tensor_id = layer.name + '_' + str(node_idx) + ':' + str(tensor_idx) + '_out'
                            tensor_shape = output_shapes[tensor_idx]
                            tensor_spec = self._get_tensor_spec(layer, node_idx, tensor_idx,
                                                                tensor_id, output_tensor, tensor_shape, dimension_order)
                            if output_tensor in model_outputs:
                                # preserve order of output tensors
                                output_specs[model_outputs.index(output_tensor)] = tensor_spec
                                output_layer_tensor_names.setdefault(layer.name, []).append(tensor_spec.name)
                            else:
                                intermediate_output_specs.append(tensor_spec)
            assert all(spec is not None for spec in input_specs)
            assert all(spec is not None for spec in intermediate_output_specs)
            assert all(spec is not None for spec in output_specs)

            # training configuration:
            training_config = None
            from keras.models import Sequential
            if isinstance(model, Sequential):
                model = model.model
            # else, no training configuration is available
            if hasattr(model, 'optimizer') and hasattr(model, 'loss') and hasattr(model, 'metrics'):
                # optimizer:
                optimizer = model.optimizer
                # loss functions:
                # can be either a string, function, list or dict according to the Keras
                # API. In the end, we want a dictionary that maps an output tensor name to
                # a loss.
                if isinstance(model.loss, str) or callable(model.loss):
                    # normalize to list
                    losses = [model.loss] * len(output_specs)
                else:
                    losses = model.loss
                if not isinstance(losses, dict):
                    # must be a list, normalize to dict
                    losses = {output_specs[i].name: losses[i] for i in range(len(output_specs))}
                else:
                    # Keras stores dicts that map an output _layer_ name to a loss. We want an output tensor name.
                    temp = {}
                    for layer_name, loss in losses.items():
                        for tensor_name in output_layer_tensor_names[layer_name]:
                            temp[tensor_name] = loss
                    losses = temp
                import keras.losses as kl
                for tensor, loss in losses.items():
                    losses[tensor] = kl.get(loss).__name__
                # metrics:
                # We want a dictionary that maps an output tensor name to a list of metrics.
                metrics = model.metrics
                if metrics is None:
                    metrics = {}
                if not isinstance(metrics, dict):
                    metrics = {output_specs[i].name: metrics for i in range(len(output_specs))}
                else:
                    # Keras stores dicts that map an output _layer_ name to a (list of)
                    # metric(s). We want an output tensor name.
                    temp = {}
                    for layer_name, metric in metrics.items():
                        if isinstance(metric, str) or callable(metric):
                            # normalize to list
                            metric = [metric]
                        for tensor_name in output_layer_tensor_names[layer_name]:
                            temp[tensor_name] = metric
                    metrics = temp
                    import keras.metrics as km
                    for tensor, metric in metrics.items():
                        for idx, met in enumerate(metric):
                            metrics[tensor][idx] = km.get(met).__name__ if met != 'accuracy' and met != 'acc' else 'acc'
                training_config = DLKerasTrainingConfig()
                training_config.optimizer = optimizer
                training_config.loss = losses
                training_config.metrics = metrics
            self._spec = DLKerasNetworkSpec(input_specs, intermediate_output_specs, output_specs, training_config)
        return self._spec

    def _determine_dimension_order(self):
        data_format = self._determine_data_format()
        if data_format is 'channels_first':
            return 'TCDHW'
        else:
            return 'TDHWC'

    def _determine_data_format(self):
        data_formats = []
        for layer in self._model.layers:
            if hasattr(layer, 'data_format'):
                data_formats.append(layer.data_format)
        if len(data_formats) == 0:
            # use data format specified in keras config
            return keras.backend.image_data_format()
        elif len(set(data_formats)) > 1:
            raise ValueError("The network contains conflicting data_formats.")
        else:
            # we checked that data_formats is not empty and all data_formats are the same
            return data_formats[0]

    def execute(self, in_data, batch_size):
        X = self._format_input(in_data, batch_size)
        Y = self._model.predict(X, batch_size=batch_size, verbose=0)  # don't change to predict_proba
        return self._format_output(Y)

    def train(self, training_data_supplier, validation_data_supplier=None):
        assert training_data_supplier is not None
        config = self._spec.training_config
        if not config:
            raise ValueError("No training configuration available. Set configuration before training the network.")

        # TODO: before training: (re)compile model! (if pre-compiled: only compile if training config changed) Note that
        # we currently make some assumptions on how a model is compiled - e.g. we expect metrics to contain 'acc'.
        # HACK: old code, this should be a dictionary (layer_name, loss)!
        loss = []
        for output_spec in self.spec.output_specs:
            loss.append(config.loss[output_spec.name])
        metrics = config.metrics

        if not any(m == 'acc' or m == 'accuracy' for m in metrics):
            metrics.append('acc')

        self._model.compile(loss=loss, optimizer=config.optimizer, metrics=metrics)

        if not any(isinstance(c, DLKerasTrainingMonitor) for c in config.callbacks):
            config.callbacks.append(DLKerasTrainingMonitor(self))

        if validation_data_supplier is not None:
            validation_data_generator = validation_data_supplier.get_generator()
            validation_steps = validation_data_supplier.steps
        else:
            validation_data_generator = None
            validation_steps = None

        kw_max_queue = 'max_queue_size' if compare_versions(keras.__version__, "2.0.5") > 0 else 'max_q_size'
        history = self._model.fit_generator(training_data_supplier.get_generator(),
                                            training_data_supplier.steps,
                                            epochs=config.epochs,
                                            verbose=1,
                                            callbacks=config.callbacks,
                                            validation_data=validation_data_generator,
                                            validation_steps=validation_steps,
                                            **{kw_max_queue: 1})
        return history.history

    def save(self, path):
        if not (self._model.layers or []):
            raise ValueError("Failed to save empty Keras deep learning network. " +
                             "Please add at least one layer to the network in order to be able to save it.")
        try:
            self._model.save(path)
        except Exception as e:
            raise RuntimeError('Failed to save Keras deep learning network.') from e

    # "Protected" helper methods:

    def _format_input(self, in_data, batch_size):
        return self._format_tensor(in_data, self.spec.input_specs, batch_size)

    def _format_output(self, Y):
        # some networks have multiple outputs, some do not
        if not isinstance(Y, (list, tuple)):
            Y = [Y]
        # TODO: output selected outputs only, output intermediate outputs
        output = {}
        for idx, output_spec in enumerate(self.spec.output_specs):
            out = self._put_in_matching_buffer(Y[idx])
            out = pd.DataFrame({output_spec.identifier: [out]})
            output[output_spec.identifier] = out
        return output

    def _format_target(self, in_data, batch_size):
        return self._format_tensor(in_data, self.spec.output_specs, batch_size)

    def _format_tensor(self, in_data, specs, batch_size):
        tensors = []
        for spec in specs:
            tensor = in_data[spec.identifier].values[0][0].array
            tensor_shape = in_data[spec.identifier].values[0][1]
            tensor = tensor.reshape([batch_size] + tensor_shape)
            tensors.append(tensor)
        return tensors

    def _put_in_matching_buffer(self, y):
        t = y.dtype
        if t == np.float64:
            return DLPythonDoubleBuffer(y)
        elif t == np.float32:
            return DLPythonFloatBuffer(y)
        elif t == np.int32:
            return DLPythonIntBuffer(y)
        elif t == np.int64:
            return DLPythonLongBuffer(y)
        # TODO: support more types
        else:
            # TODO: warning to stderr? fail?
            return DLPythonDoubleBuffer(y)


class DLKerasNetworkSpec(DLPythonNetworkSpec):
    __metaclass__ = abc.ABCMeta

    def __init__(self, input_specs, intermediate_output_specs, output_specs, training_config=None):
        super().__init__(input_specs, intermediate_output_specs, output_specs)
        self._training_config = training_config

    @property
    def training_config(self):
        return self._training_config

    @training_config.setter
    def training_config(self, training_config):
        self._training_config = training_config


class DLKerasTrainingConfig(DLPythonTrainingConfig):
    def __init__(self):
        super().__init__()
        self.optimizer = None
        self.loss = {}
        self.metrics = ['acc']
        self.callbacks = []
