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

from DLPythonDataBuffers import DLPythonDoubleBuffer
from DLPythonDataBuffers import DLPythonFloatBuffer
from DLPythonDataBuffers import DLPythonIntBuffer
from DLPythonDataBuffers import DLPythonLongBuffer

from DLPythonNetwork import DLPythonLayerDataSpec
from DLPythonNetwork import DLPythonNetwork
from DLPythonNetwork import DLPythonNetworkReader
from DLPythonNetwork import DLPythonNetworkSpec

import DLPythonNetworkType 

import numpy as np
import pandas as pd


class DLKerasNetworkReader(DLPythonNetworkReader):
    
    def read(self, path):
        model = load_model(path)
        return DLKerasNetwork(model)
    
    def readFromJson(self, path):
        f = open(path, 'r')
        model_json_string = f.read()
        f.close()
        model = model_from_json(model_json_string)
        return DLKerasNetwork(model)
    
    def readFromYaml(self, path):
        f = open(path, 'r')
        model_yaml_string = f.read()
        f.close()
        model = model_from_yaml(model_yaml_string)
        return DLKerasNetwork(model)


class DLKerasNetwork(DLPythonNetwork):
    
    def __init__(self, model):
        super().__init__(model)
    
    @property
    def spec(self):
        if self._spec is None:
            input_specs = self.__get_specs_for(self._model.inputs)
            intermediate_outputs = []
            for l in self._model.layers:
                for idx in range (0, len(l.inbound_nodes)):
                    o = l.get_output_at(idx)
                    if o not in self._model.outputs:
                        intermediate_outputs.append(o)
            intermediate_output_specs = self.__get_specs_for(intermediate_outputs)
            output_specs = self.__get_specs_for(self._model.outputs)
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
        # put single outputs in list to have a common way of handling single and multiple outputs
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
        
    def __get_specs_for(self, layer_data):
        layer_data_specs = []
        for ld in layer_data:
            name = ld.name
            shape = ld.shape.as_list()
            batch_size = shape[0]
            shape = shape[1:]
            element_type = ld.dtype.name
            layer_data_specs.append(DLPythonLayerDataSpec(name, batch_size, shape, element_type))
        return layer_data_specs
    
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
    
    def __init__(self, input_specs, intermediate_output_specs, output_specs):
        super().__init__(input_specs, intermediate_output_specs, output_specs)
    
    @property
    def network_type(self):
        return DLKerasNetworkType.instance()


class DLKerasNetworkType(DLPythonNetworkType.DLPythonNetworkType):
    
    def __init__(self):
        super().__init__('org.knime.dl.keras.core.DLKerasNetworkType', frozenset(['keras']))
    
    @property
    def reader(self):
        return DLKerasNetworkReader()
    
    def wrap_model(self, model):
        return DLKerasNetwork(model)


# pseudo-singleton:
_instance = DLKerasNetworkType()
# register network type
DLPythonNetworkType.add_network_type(_instance)
# access point for other modules
def instance():
    return _instance
