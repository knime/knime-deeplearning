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
@author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
@author Christian Dietz, KNIME GmbH, Konstanz, Germany
'''

import abc
import re
from distutils.version import LooseVersion

import keras
import numpy as np
import pandas as pd
from keras.models import Model
from keras.models import Sequential
from keras.models import load_model
from keras.models import model_from_json
from keras.models import model_from_yaml
from keras.layers import Lambda

import DLPythonKernelGateway
from DLKerasTrainingCallbacks import DLKerasTrainingMonitor
from DLPythonDataBuffers import DLPythonDoubleBuffer
from DLPythonDataBuffers import DLPythonFloatBuffer
from DLPythonDataBuffers import DLPythonBitBuffer
from DLPythonDataBuffers import DLPythonByteBuffer
from DLPythonDataBuffers import DLPythonUnsignedByteBuffer
from DLPythonDataBuffers import DLPythonShortBuffer
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
    def read(self, path, compile=True, compatibility_mode=False):
        raise NotImplementedError()

    @abc.abstractmethod
    def read_from_json(self, path, compatibility_mode=False):
        raise NotImplementedError()

    @abc.abstractmethod
    def read_from_yaml(self, path, compatibility_mode=False):
        raise NotImplementedError()

    def _read_internal(self, path, compile=True):
        return load_model(path, compile=compile)

    def _read_from_json_internal(self, path):
        with open(path, 'r') as f:
            model_json_string = f.read()
        return model_from_json(model_json_string)

    def _read_from_yaml_internal(self, path):
        with open(path, 'r') as f:
            model_yaml_string = f.read()
        return model_from_yaml(model_yaml_string)


class DLKerasNetwork(DLPythonNetwork):
    __metaclass__ = abc.ABCMeta

    @staticmethod
    def _convert_sequential_to_model(model, compatibility_mode=False):
        if isinstance(model, Sequential) and DLKerasNetwork._higher_than_keras_2_2_0():
            # Keras 2.2.0 or higher
            if compatibility_mode:
                # Wrap again
                # -> Training configuration will get lost but this is the best we can do
                return Model(inputs=model.inputs, outputs=model.outputs)
            else:
                # Since Keras 2.2.0 a sequential model is also a model
                return model
        elif isinstance(model, Sequential):
            if model.model and isinstance(model.model, Model):
                return model.model
            else:
                return Model(inputs=model.inputs, outputs=model.outputs)
        else:
            return model

    @staticmethod
    def _higher_than_keras_2_2_0():
        return LooseVersion(keras.__version__) >= LooseVersion("2.2.0")

    def __init__(self, model, compatibility_mode=False):
        super().__init__(DLKerasNetwork._convert_sequential_to_model(model, compatibility_mode))
        self._training_monitor = None

    @abc.abstractmethod
    def _extract_model_spec(self):
        raise NotImplementedError()

    @property
    def spec(self):
        if self._spec is None:
            self._spec = self._extract_model_spec()
        return self._spec

    def execute(self, in_data, batch_size, output_identifiers):
        model = self._model
        X = self._format_input(in_data, batch_size)

        # Get the requested output tensors
        outputs = []
        for id in output_identifiers:
            matcher = re.match(r'^(.*)_(\d+):(\d+)$', id)
            layer_name = matcher.group(1)
            node_idx = int(matcher.group(2))
            tensor_idx = int(matcher.group(3))
            output_tensors = model.get_layer(layer_name).get_output_at(node_idx)
            if not isinstance(output_tensors, list):
                output_tensors = [output_tensors]
            if output_tensors[tensor_idx] in model.inputs:
                output_tensors[tensor_idx] = Lambda(lambda x: x)(output_tensors[tensor_idx])
            outputs.append(output_tensors[tensor_idx])

        # Build the model with the requested outputs
        intermediate_model = Model(inputs=model.inputs, outputs=outputs)
        Y = intermediate_model.predict(X, batch_size=batch_size, verbose=0)
        return self._format_output(Y, output_identifiers)

    def train(self, training_data_supplier, validation_data_supplier=None, send_to_java=None):
        assert training_data_supplier is not None
        config = self._spec.training_config
        if not config:
            raise ValueError("No training configuration available. Set configuration before training the network.")

        # TODO: before training: (re)compile model! (if pre-compiled: only compile if training config changed) Note that
        # we currently make some assumptions on how a model is compiled - e.g. we expect metrics to contain 'acc'.
        # HACK: old code, this should be a dictionary (layer_name, loss)!
        loss = []
        for output_spec in self.spec.output_specs:
            loss.append(config.loss[output_spec.identifier])
        metrics = config.metrics

        # TODO: support per-output metrics
        if not any(m == 'acc' or m == 'accuracy' for m in metrics):
            metrics.append('acc')

        self._model.compile(loss=loss, optimizer=config.optimizer, metrics=metrics)

        if not any(isinstance(c, DLKerasTrainingMonitor) for c in config.callbacks):
            training_monitor = DLKerasTrainingMonitor(self)
            config.callbacks.append(training_monitor)
            self._training_monitor = training_monitor

        for c in config.callbacks:
            c.send_to_java = send_to_java

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

    def stop_early(self):
        if self._training_monitor is not None:
            self._training_monitor.stop_early()
        else:
            raise RuntimeError("Cannot stop training. Training monitor is None.")

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

    def _format_output(self, Y, output_identifiers):
        # some networks have multiple outputs, some do not
        if not isinstance(Y, (list, tuple)):
            Y = [Y]
        out_and_hidden_specs = self.spec.output_specs + self.spec.intermediate_output_specs
        output_specs = [[s for s in out_and_hidden_specs if s.identifier == id][0] for id in output_identifiers]
        output = {}
        for idx, output_spec in enumerate(output_specs):
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
        elif t == np.bool_:
            return DLPythonBitBuffer(y)
        elif t == np.int8:
            return DLPythonByteBuffer(y)
        elif t == np.uint8:
            return DLPythonUnsignedByteBuffer(y)
        elif t == np.int16:
            return DLPythonShortBuffer(y)
        elif t == np.int32:
            return DLPythonIntBuffer(y)
        elif t == np.int64:
            return DLPythonLongBuffer(y)
        # TODO: support more types
        else:
            raise ValueError('Output type of the network \'{}\' is not supported.'.format(y.dtype))


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
