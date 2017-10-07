# -*- coding: utf-8 -*-

# ------------------------------------------------------------------------
#  Copyright by KNIME GmbH, Konstanz, Germany
#  Website: http://www.knime.org; Email: contact@knime.org
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

from DLPythonDataBuffers import DLPythonDoubleBuffer
from DLPythonDataBuffers import DLPythonFloatBuffer
from DLPythonDataBuffers import DLPythonIntBuffer
from DLPythonDataBuffers import DLPythonLongBuffer

from DLPythonNetwork import DLPythonTensorSpec
from DLPythonNetwork import DLPythonNetwork
from DLPythonNetwork import DLPythonNetworkReader
from DLPythonNetwork import DLPythonNetworkSpec

import numpy as np
import pandas as pd


class DLKerasNetworkReader(DLPythonNetworkReader):
    __metaclass__ = abc.ABCMeta

    @abc.abstractmethod
    def readFromJson(self, path):
        return

    @abc.abstractmethod
    def readFromYaml(self, path):
        return


class DLKerasNetwork(DLPythonNetwork):
    __metaclass__ = abc.ABCMeta

    def __init__(self, model):
        super().__init__(model)
        # TODO: Do we really want to introduce state here? Alternatively, add config parameter to train method.
        self._training_config = None

    @property
    def spec(self):
        if self._spec is None:
            model_inputs = set(self._model.inputs)
            model_outputs = set(self._model.outputs)
            visited = set()

            input_specs = list()
            intermediate_output_specs = list()
            output_specs = list()

            for l in self._model.layers:
                # inputs:
                for idx in range (0, len(l.inbound_nodes)):
                    inputs = l.get_input_at(idx)
                    input_shapes = l.get_input_shape_at(idx)
                    # some layers have multiple inputs, some do not
                    if not isinstance(inputs, list):
                        inputs = [inputs]
                        input_shapes = [input_shapes]
                    for i, inp in enumerate(inputs):
                        if inp in model_inputs and inp not in visited:
                            visited.add(inp)
                            shape = input_shapes[i]
                            element_type = inp.dtype
                            # Theano returns a string, TensorFlow a dtype object
                            if(not isinstance(element_type, str)):
                                element_type = element_type.name
                            spec = DLPythonTensorSpec(inp.name, shape[0], list(shape[1:]), element_type)
                            input_specs.append(spec)
                # outputs:
                for idx in range (0, len(l.inbound_nodes)):  # inbound_nodes (sic)
                    outputs = l.get_output_at(idx)
                    output_shapes = l.get_output_shape_at(idx)
                    # some layers have multiple outputs, some do not
                    if not isinstance(outputs, list):
                        outputs = [outputs]
                        output_shapes = [output_shapes]
                    for i, out in enumerate(outputs):
                        if out not in visited:
                            visited.add(out)
                            shape = output_shapes[i]
                            element_type = out.dtype
                            # Theano returns a string, TensorFlow a dtype object
                            if(not isinstance(element_type, str)):
                                element_type = element_type.name
                            specs = DLPythonTensorSpec(out.name, shape[0], list(shape[1:]), element_type)
                            if out in model_outputs:
                                output_specs.append(specs)
                            else:
                                intermediate_output_specs.append(specs)
            self._spec = DLKerasNetworkSpec(input_specs, intermediate_output_specs, output_specs)
        return self._spec

    @property
    def training_config(self):
        return self._training_config

    @training_config.setter
    def training_config(self, config):
        self._training_config = config
        loss = []
        for output_spec in self.spec.output_specs:
            loss.append(config.loss[output_spec.name])
        self._model.compile(loss=loss, optimizer=config.optimizer, metrics=config.metrics)

    def execute(self, in_data):
        X = self._format_input(in_data)
        Y = self._model.predict(X, verbose=0)  # don't change to predict_proba
        return self._format_output(Y)

    def train(self, training_data, target_data):
        config = self._training_config
        if not config:
            raise ValueError("No training configuration available. Set configuration before training the network.")
        X1 = self._format_input(training_data)
        X2 = self._format_target(target_data)
        history = self._model.fit(X1, X2, batch_size=config.batch_size, epochs=config.epochs, verbose=1)
        return history.history

    def save(self, path):
        self._model.save(path)

    # "Protected" helper methods:

    def _format_input(self, in_data):
        # TODO: this does not yet take (predefined) batch size of the input into account
        X = []
        for input_spec in self.spec.input_specs:
            data = in_data[input_spec.name].values
            data = list(map((lambda b: b[0].array.reshape([1] + input_spec.shape)), data))
            X.append(np.vstack(data))
        return X

    def _format_output(self, Y):
        # some networks have multiple outputs, some do not
        if not isinstance(Y, (list, tuple)):
            Y = [Y]
        # TODO: output selected outputs only, output intermediate outputs
        output = {}
        for idx, output_spec in enumerate(self.spec.output_specs):
            batch_size = 1
            if len(Y[idx].shape) > len(output_spec.shape):
                batch_size = Y[idx].shape[0]
            out = []
            for i in range(0, batch_size):
                out.append(self._putInMatchingBuffer(Y[idx][i]))
            out = pd.DataFrame({output_spec.name:out})
            output[output_spec.name] = out
        return Y

    def _format_target(self, target_data):
        # TODO: this does not yet take (predefined) batch size of the output into account
        X = []
        for output_spec in self.spec.output_specs:
            data = target_data[output_spec.name].values
            data = list(map((lambda b: b[0].array.reshape([1] + output_spec.shape)), data))
            X.append(np.vstack(data))
        return X

    def _putInMatchingBuffer(self, y):
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

    def __init__(self, input_specs, intermediate_output_specs, output_specs):
        super().__init__(input_specs, intermediate_output_specs, output_specs)


class DLKerasTrainingConfig(object):

    def __init__(self):
        self._batch_size = 32
        self._epochs = 1
        self._optimizer = None
        self._loss = {}
        self._metrics = ['accuracy']

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
