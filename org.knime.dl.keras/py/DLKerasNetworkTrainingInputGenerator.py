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

from DLPythonKernelGateway import global_workspace
from DLPythonNetworkTrainingInputGenerator import DLPythonNetworkTrainingInputGenerator


class DLKerasNetworkTrainingInputGenerator(DLPythonNetworkTrainingInputGenerator):
    def __init__(self, network, steps, batch_size, chunk_size, message_category, is_validation_data=False):
        assert network is not None
        input_names = [s.identifier for s in network.spec.input_specs]
        target_names = [s.identifier for s in network.spec.output_specs]
        super().__init__(input_names, target_names, steps, batch_size)
        self._network = network
        self._chunk_size = chunk_size
        self._message_category = message_category
        self._request_from_java = None
        self._is_validation_data = is_validation_data
        self._chunk_index = -1

    @property
    def request_from_java(self):
        return self._request_from_java

    @request_from_java.setter
    def request_from_java(self, request_from_java):
        self._request_from_java = request_from_java

    def _request_chunk(self, chunk_index):
        if self._chunk_index != chunk_index:
            self._request_from_java(self._message_category, chunk_index)
            # TODO what's about threading (lock at least?)

            # Get the requested data
            # TODO pre-allocate dictionaries
            input_data = {}
            for input_name in self._input_names:
                workspace_input_name = input_name + "_validation" if self._is_validation_data else input_name
                input_data[input_name] = global_workspace()[workspace_input_name]
            target_data = {}
            for target_name in self._target_names:
                workspace_target_name = target_name + "_validation" if self._is_validation_data else target_name
                target_data[target_name] = global_workspace()[workspace_target_name]

            # Format the input and target
            # TODO: move formatting logic from network to generator, remove dependency on network
            self._input_chunk = self._network._format_input(input_data, self._chunk_size * self._batch_size)
            self._target_chunk = self._network._format_target(target_data, self._chunk_size * self._batch_size)

            # Set the current chunk index
            self._chunk_index = chunk_index

    def _get_batch(self, batch_index):
        # Get required indices
        chunk_index = batch_index // self._chunk_size
        in_chunk_start = (batch_index % self._chunk_size) * self._batch_size
        in_chunk_end = ((batch_index % self._chunk_size) + 1) * self._batch_size
        self._request_chunk(chunk_index) # Only requests if not loaded already
        print('Batch {}, Chunk {}, Start {}, End {}'.format(batch_index, chunk_index, in_chunk_start, in_chunk_end)) # TODO remove debug output

        return ([inp[in_chunk_start:in_chunk_end] for inp in self._input_chunk],
                [oup[in_chunk_start:in_chunk_end] for oup in self._target_chunk])
