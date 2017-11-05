# -*- coding: utf-8 -*-

# ------------------------------------------------------------------------
#  Copyright by KNIME GmbH, Konstanz, Germany
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
#  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
@author Marcel Wiedenmann, KNIME, Konstanz, Germany
@author Christian Dietz, KNIME, Konstanz, Germany
'''

import abc
import keras

from DLPythonDataBuffers import DLPythonDoubleBuffer
from DLPythonDataBuffers import DLPythonFloatBuffer
from DLPythonDataBuffers import DLPythonIntBuffer
from DLPythonDataBuffers import DLPythonLongBuffer

from DLPythonNetwork import DLPythonNetwork
from DLPythonNetwork import DLPythonNetworkReader
from DLPythonNetwork import DLPythonNetworkSpec
from DLPythonNetwork import DLPythonTrainingConfig

import numpy as np
import pandas as pd


class DLKerasNetworkReader(DLPythonNetworkReader):
    __metaclass__ = abc.ABCMeta

    @abc.abstractmethod
    def read_from_json(self, path):
        raise NotImplementedError()

    @abc.abstractmethod
    def read_from_yaml(self, path):
        raise NotImplementedError()


class DLKerasNetwork(DLPythonNetwork):
    __metaclass__ = abc.ABCMeta

    def __init__(self, model):
        super().__init__(model)

    @abc.abstractmethod
    def _get_tensor_spec(self, layer, node_idx, tensor_idx, tensor_id, tensor, tensor_shape):
        raise NotImplementedError()

    @property
    def spec(self):
        if self._spec is None:
            model = self._model
            model_inputs = set(model.inputs)
            model_outputs = set(model.outputs)

            # tensors:
            visited_inputs = set()
            visited_outputs = set()

            input_specs = list()
            intermediate_output_specs = list()
            output_specs = list()

            output_layer_tensor_names = {}

            for layer in model.layers:
                for node_idx in range(0, len(layer.inbound_nodes)):
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
                            tensor_id = layer.name + '_' + str(node_idx) + ':' + str(tensor_idx)
                            tensor_shape = input_shapes[tensor_idx]
                            tensor_spec = self._get_tensor_spec(layer, node_idx, tensor_idx,
                                                                tensor_id, input_tensor, tensor_shape)
                            input_specs.append(tensor_spec)
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
                            tensor_id = layer.name + '_' + str(node_idx) + ':' + str(tensor_idx)
                            tensor_shape = output_shapes[tensor_idx]
                            tensor_spec = self._get_tensor_spec(layer, node_idx, tensor_idx,
                                                                tensor_id, output_tensor, tensor_shape)
                            if output_tensor in model_outputs:
                                output_specs.append(tensor_spec)
                                output_layer_tensor_names.setdefault(layer.name, []).append(tensor_spec.name)
                            else:
                                intermediate_output_specs.append(tensor_spec)

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
                metrics = model.metrics
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

    def execute(self, in_data, batch_size):
        X = self._format_input(in_data, batch_size)
        Y = self._model.predict(X, batch_size=batch_size, verbose=0)  # don't change to predict_proba
        return self._format_output(Y)

    def train(self, training_data, target_data):
        config = self._spec.training_config
        if not config:
            raise ValueError("No training configuration available. Set configuration before training the network.")

        # TODO: before training: (re)compile model! (if pre-compiled: only compile if training config changed)
        # HACK: old code
        loss = []
        for output_spec in self.spec.output_specs:
            loss.append(config.loss[output_spec.name])
        metrics = config.metrics

        self._model.compile(loss=loss, optimizer=config.optimizer, metrics=metrics)

        X1 = self._format_training(training_data, config.batch_size)
        X2 = self._format_target(target_data, config.batch_size)

        history = self._model.fit(X1, X2, batch_size=config.batch_size, epochs=config.epochs, verbose=1,
                                  callbacks=config.callbacks)
        return history.history

    def save(self, path):
        self._model.save(path)

    # "Protected" helper methods:

    def _format_input(self, in_data, batch_size):
        X = []
        for input_spec in self.spec.input_specs:
            tensor = in_data[input_spec.name].values[0][0].array
            tensor = tensor.reshape([batch_size] + input_spec.shape)
            X.append(tensor)
        return X

    def _format_output(self, Y):
        # some networks have multiple outputs, some do not
        if not isinstance(Y, (list, tuple)):
            Y = [Y]
        # TODO: output selected outputs only, output intermediate outputs
        output = {}
        for idx, output_spec in enumerate(self.spec.output_specs):
            out = self._put_in_matching_buffer(Y[idx])
            out = pd.DataFrame({output_spec.name: [out]})
            output[output_spec.name] = out
        return output

    # HACK: remove entire method afterwards, replace references by _format_input
    def _format_training(self, training_data, batch_size):
        X = []
        for input_spec in self.spec.input_specs:
            data = training_data[input_spec.name].values[0][0].array
            batch_shape = [batch_size] + input_spec.shape
            elems_per_batch = np.prod(batch_shape)
            num_batches = int(data.size / elems_per_batch)
            tensor_stack = []
            for i in range(num_batches):
                batch = data[i * elems_per_batch:(i + 1) * elems_per_batch].reshape(batch_shape)
                tensor_stack.append(batch)
            tensor = np.vstack(tensor_stack)
            X.append(tensor)

        return X

    def _format_target(self, target_data, batch_size):
        X = []
        for output_spec in self.spec.output_specs:

            # HACK >>

            data = target_data[output_spec.name].values[0][0].array

            batch_shape = [batch_size] + output_spec.shape
            elems_per_batch = np.prod(batch_shape)
            num_batches = int(data.size / elems_per_batch)
            tensor_stack = []
            for i in range(num_batches):
                batch = data[i * elems_per_batch:(i + 1) * elems_per_batch].reshape(batch_shape)
                tensor_stack.append(batch)
            tensor = np.vstack(tensor_stack)
            X.append(tensor)

            # --

            # tensor = target_data[output_spec.name].values[0][0].array
            # tensor = tensor.reshape([batch_size] + output_spec.shape)
            # X.append(tensor)

            # <<

        return X

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
        self._batch_size = 32
        self._epochs = 1
        self._optimizer = None
        self._loss = {}
        self._metrics = ['accuracy']
        self._callbacks = [DLKerasTrainingReporter(), DLKerasUserEarlyStopping()]

    @property
    def batch_size(self):
        return self._batch_size

    @batch_size.setter
    def batch_size(self, batch_size):
        self._batch_size = batch_size

    @property
    def epochs(self):
        return self._epochs

    @epochs.setter
    def epochs(self, epochs):
        self._epochs = epochs

    @property
    def optimizer(self):
        return self._optimizer

    @optimizer.setter
    def optimizer(self, optimizer):
        self._optimizer = optimizer

    @property
    def loss(self):
        return self._loss

    @loss.setter
    def loss(self, loss):
        self._loss = loss

    @property
    def metrics(self):
        return self._metrics

    @metrics.setter
    def metrics(self, metrics):
        self._metrics = metrics

    @property
    def callbacks(self):
        return self._callbacks

    @callbacks.setter
    def callbacks(self, callbacks):
        self._callbacks = callbacks


class DLKerasTrainingReporter(keras.callbacks.Callback):

    def on_train_begin(self, logs={}):
        metrics_names = self.params['metrics']
        self._metrics = pd.DataFrame(index=[0], columns=metrics_names)

    def on_batch_end(self, batch, logs={}):
        for k in self.params['metrics']:
            self._metrics.at[0, k] = logs[k]
        # TODO: send metrics back to Java


class DLKerasUserEarlyStopping(keras.callbacks.Callback):

    def on_train_begin(self, logs={}):
        self._stop = False

    def on_batch_end(self, batch, logs={}):
        if self._stop:
            self.model.stop_training = True

    # def on_train_end(self, logs=None):
    #    if self._stop:
    #        print('Training was stopped.')

    def stop(self):
        self._stop = True
