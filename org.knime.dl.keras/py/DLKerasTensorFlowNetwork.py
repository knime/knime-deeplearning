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

from keras.models import load_model
from keras.models import model_from_json
from keras.models import model_from_yaml

from DLKerasNetwork import DLKerasNetwork
from DLKerasNetwork import DLKerasNetworkReader
from DLKerasNetwork import DLKerasNetworkSpec
from DLKerasNetwork import DLKerasNetworkType

from DLPythonDataBuffers import DLPythonDoubleBuffer
from DLPythonDataBuffers import DLPythonFloatBuffer
from DLPythonDataBuffers import DLPythonIntBuffer
from DLPythonDataBuffers import DLPythonLongBuffer

from DLPythonNetwork import DLPythonLayerDataSpec

import DLPythonNetworkType

import numpy as np
import pandas as pd


class DLKerasTensorFlowNetworkReader(DLKerasNetworkReader):
    
    def read(self, path):
        model = load_model(path)
        return DLKerasTensorFlowNetwork(model)
    
    def readFromJson(self, path):
        f = open(path, 'r')
        model_json_string = f.read()
        f.close()
        model = model_from_json(model_json_string)
        return DLKerasTensorFlowNetwork(model)
    
    def readFromYaml(self, path):
        f = open(path, 'r')
        model_yaml_string = f.read()
        f.close()
        model = model_from_yaml(model_yaml_string)
        return DLKerasTensorFlowNetwork(model)


class DLKerasTensorFlowNetwork(DLKerasNetwork):
    
    # TODO: add DLKerasTensorFlowNetwork etc. - add 'backend_name' arg to constructor;
    # Keras backend has to be set accordingly whenever Keras is invoked.
    # Check if there could be collisions.
    
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
                            element_type = inp.dtype.name
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
                            element_type = inp.dtype.name
                            spec = DLPythonLayerDataSpec(out.name, shape[0], list(shape[1:]), element_type)
                            if out in model_outputs:
                                output_specs.append(spec)
                            else:
                                intermediate_output_specs.append(spec)
            self._spec = DLKerasTensorFlowNetworkSpec(input_specs, intermediate_output_specs, output_specs)
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


class DLKerasTensorFlowNetworkSpec(DLKerasNetworkSpec):
    
    def __init__(self, input_specs, intermediate_output_specs, output_specs):
        super().__init__(input_specs, intermediate_output_specs, output_specs)
    
    @property
    def network_type(self):
        return DLKerasTensorFlowNetworkType.instance()


class DLKerasTensorFlowNetworkType(DLKerasNetworkType):
    
    def __init__(self):
        super().__init__('org.knime.dl.keras.tensorflow.core.DLKerasTensorFlowNetworkType', 'tensorflow')
    
    @property
    def reader(self):
        return DLKerasTensorFlowNetworkReader()
    
    def wrap_model(self, model):
        return DLKerasTensorFlowNetwork(model)


# pseudo-singleton:
_instance = DLKerasTensorFlowNetworkType()
# register network type
DLPythonNetworkType.add_network_type(_instance)
# access point for other modules
def instance():
    return _instance
