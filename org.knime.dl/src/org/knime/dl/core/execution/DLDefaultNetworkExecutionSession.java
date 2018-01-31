/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   Jul 3, 2017 (marcel): created
 */
package org.knime.dl.core.execution;

import java.util.Map;
import java.util.Map.Entry;

import org.knime.core.node.CanceledExecutionException;
import org.knime.dl.core.DLTensor;
import org.knime.dl.core.DLTensorId;
import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.core.data.DLWritableBuffer;
import org.knime.dl.util.DLUtils;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public class DLDefaultNetworkExecutionSession implements DLNetworkExecutionSession {

	private final DLExecutableNetwork m_network;

	public DLDefaultNetworkExecutionSession(final DLExecutableNetwork network) {
		m_network = network;
	}

	@Override
	public void run(final DLExecutionMonitor monitor) {
		// must be present
		final long batchSize = m_network.getSpec().getInputSpecs()[0].getBatchSize().getAsLong();
		final DLNetworkInputPreparer<DLTensor<? extends DLWritableBuffer>> inputPreparer = new DLNetworkInputPreparer<DLTensor<? extends DLWritableBuffer>>() {

			@Override
			public long getNumBatches() {
				return (long) Math.ceil(m_input.getNumSamples() / (double) batchSize);
			}

			@Override
			public void prepare(final Map<DLTensorId, DLTensor<? extends DLWritableBuffer>> inputTensors,
					final long batchIndex) throws CanceledExecutionException {
				// fill tensors (= batch) row by row of the input table
				long rowIndex;
				for (rowIndex = 0; rowIndex < batchSize; rowIndex++) {
					m_input.xy(inputTensors);
					if (!m_input.hasNext()) {
						break;
					}
				}
				// check if tensors are correctly filled
				for (final Entry<DLTensorId, DLTensor<? extends DLWritableBuffer>> entry : inputTensors.entrySet()) {
					final DLTensorSpec tensorSpec = entry.getValue().getSpec();
					final DLTensor<? extends DLWritableBuffer> tensor = entry.getValue();
					if (tensor.getBuffer().size() != tensor.getBuffer().getCapacity()) {
						// must be present
						final long sampleSize = DLUtils.Shapes.getFixedSize(tensorSpec.getShape()).getAsLong();
						if (isLastBatch) {
							// zero pad last, incomplete batch
							tensor.getBuffer().setSize(sampleSize * batchSize);
						} else if (tensor.getBuffer().size() % (sampleSize * batchSize) != 0) {
							throw new DLInvalidNetworkInputException(
									"Input size did not match the expected input size of network input '"
											+ tensor.getSpec().getName() + "'. Neuron count is " + sampleSize
											+ ", batch size is " + batchSize + ". Thus, expected input size is "
											+ sampleSize * batchSize + ". However, node input size was "
											+ tensor.getBuffer().size()
											+ ". Please check the column selection for this input "
											+ "and validate the node's input data.");
						}
					}
				}
			}
		};
	}

	@Override
	public void close() throws Exception {
		m_network.close();
		m_input.close();
		m_output.close();
	}
}
