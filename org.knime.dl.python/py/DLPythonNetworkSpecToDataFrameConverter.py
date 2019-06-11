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

import DLPythonNetwork

import pandas as pd


class DLPythonNetworkSpecToDataFrameConverter(object):

    def __init__(self, network_spec):
        self._network_spec = network_spec

    def input_specs_to_data_frame(self):
        return self.__layer_data_specs_to_data_frame(self._network_spec.input_specs)

    def intermediate_output_specs_to_data_frame(self):
        return self.__layer_data_specs_to_data_frame(self._network_spec.intermediate_output_specs)

    def output_specs_to_data_frame(self):
        return self.__layer_data_specs_to_data_frame(self._network_spec.output_specs)

    def training_config_to_data_frame(self):
        spec = self._network_spec
        if spec.training_config is None:
            return None, None, None
        # TODO: optimizer
        optimizer_df = None
        loss_df = pd.DataFrame.from_dict(spec.training_config.loss, orient='index')
        # metrics_df = pd.DataFrame.from_dict(spec.training_config.metrics, orient='index')
        metrics_df = pd.DataFrame(spec.training_config.metrics)
        metrics_df = metrics_df.transpose()
        return optimizer_df, loss_df, metrics_df

    def __layer_data_specs_to_data_frame(self, layer_specs):
        specs = pd.DataFrame(index=range(len(layer_specs)),
                             columns=('id', 'name', 'batch_size', 'shape', 'type', 'dimension_order'))
        for idx, tensor_spec in enumerate(layer_specs):
            specs.iloc[idx, 0] = tensor_spec.identifier
            specs.iloc[idx, 1] = tensor_spec.name
            specs.iloc[idx, 2] = tensor_spec.batch_size
            specs.iloc[idx, 3] = tensor_spec.shape
            specs.iloc[idx, 4] = tensor_spec.element_type
            specs.iloc[idx, 5] = tensor_spec.dimension_order

        specs_with_numeric_types = specs.convert_objects(convert_numeric=True)
        specs_with_numeric_types['id'] = specs_with_numeric_types['id'].astype(str)
        specs_with_numeric_types['name'] = specs_with_numeric_types['name'].astype(str)
        return specs_with_numeric_types


def get_layer_data_specs_as_data_frames(identifier):
    network = DLPythonNetwork.get_network(identifier)
    extractor = DLPythonNetworkSpecToDataFrameConverter(network.spec)
    input_specs = extractor.input_specs_to_data_frame()
    intermediate_output_specs = extractor.intermediate_output_specs_to_data_frame()
    output_specs = extractor.output_specs_to_data_frame()
    optimizer, losses, metrics = extractor.training_config_to_data_frame()
    return input_specs, intermediate_output_specs, output_specs, optimizer, losses, metrics
