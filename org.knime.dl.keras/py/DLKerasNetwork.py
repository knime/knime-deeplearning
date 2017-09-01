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

from DLPythonNetwork import DLPythonLayerDataSpec
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
                            spec = DLPythonLayerDataSpec(inp.name, shape[0], list(shape[1:]), element_type)
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
                            specs = DLPythonLayerDataSpec(out.name, shape[0], list(shape[1:]), element_type)
                            if out in model_outputs:
                                output_specs.append(specs)
                            else:
                                intermediate_output_specs.append(specs)
            self._spec = DLKerasNetworkSpec(input_specs, intermediate_output_specs, output_specs)
        return self._spec

    def execute(self, in_data):
        # TODO: this does not yet take (predefined) batch size (of the input) into account
        X = []
        for input_spec in self.spec.input_specs:
            data = in_data[input_spec.name].values
            data = list(map((lambda b: b[0].array.reshape([1] + input_spec.shape)), data))
            X.append(np.vstack(data))
        Y = self._model.predict(X, verbose=0)  # don't change to predict_proba
        # some networks have multiple outputs, some do not
        if not isinstance(Y, (list, tuple)):
            Y = [Y]
        # TODO: output selected outputs only, intermediate outputs
        output = {}
        for idx, output_spec in enumerate(self.spec.output_specs):
            batch_size = 1
            if len(Y[idx].shape) > len(output_spec.shape):
                batch_size = Y[idx].shape[0]
            out = []
            for i in range(0, batch_size):
                out.append(self.__putInMatchingBuffer(Y[idx][i]))
            out = pd.DataFrame({output_spec.name:out})
            output[output_spec.name] = out
        return output

    def save(self, path):
        self._model.save(path)

    # Private helper methods:

    def __putInMatchingBuffer(self, y):
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
            # TODO: warning to stderr?
            return DLPythonDoubleBuffer(y)


class DLKerasNetworkSpec(DLPythonNetworkSpec):
    __metaclass__ = abc.ABCMeta

    def __init__(self, input_specs, intermediate_output_specs, output_specs):
        super().__init__(input_specs, intermediate_output_specs, output_specs)
