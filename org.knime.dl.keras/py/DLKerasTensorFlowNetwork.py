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

from keras.models import load_model
from keras.models import model_from_json
from keras.models import model_from_yaml

from DLKerasNetwork import DLKerasNetwork
from DLKerasNetwork import DLKerasNetworkReader
from DLKerasNetwork import DLKerasNetworkSpec

from DLPythonNetwork import DLPythonTensorSpec


class DLKerasTensorFlowNetworkReader(DLKerasNetworkReader):

    def read(self, path):
        model = load_model(path)
        return DLKerasTensorFlowNetwork(model)

    def read_from_json(self, path):
        f = open(path, 'r')
        model_json_string = f.read()
        f.close()
        model = model_from_json(model_json_string)
        return DLKerasTensorFlowNetwork(model)

    def read_from_yaml(self, path):
        f = open(path, 'r')
        model_yaml_string = f.read()
        f.close()
        model = model_from_yaml(model_yaml_string)
        return DLKerasTensorFlowNetwork(model)


class DLKerasTensorFlowNetwork(DLKerasNetwork):

    def __init__(self, model):
        super().__init__(model)

    def train(self, training_data, target_data):
        # TODO: support TensorBoard?
        #config = self._spec.training_config
        #from keras.callbacks import TensorBoard
        #if config and not any(isinstance(cb, TensorBoard) for cb in config.callbacks):
        #    tb = TensorBoard(...) # TODO
        #    config.callbacks.append(tb)
        super().train(training_data, target_data)

    def _get_tensor_spec(self, layer, node_idx, tensor_idx, tensor_id, tensor, tensor_shape):
        name = tensor.name
        element_type = tensor.dtype.name  # TensorFlow returns a TF dtype
        return DLPythonTensorSpec(name, tensor_shape[0], list(tensor_shape[1:]), element_type)


class DLKerasTensorFlowNetworkSpec(DLKerasNetworkSpec):

    def __init__(self, input_specs, intermediate_output_specs, output_specs):
        super().__init__(input_specs, intermediate_output_specs, output_specs)

    @property
    def network_type(self):
        from DLKerasTensorFlowNetworkType import instance as TensorFlow
        return TensorFlow()
