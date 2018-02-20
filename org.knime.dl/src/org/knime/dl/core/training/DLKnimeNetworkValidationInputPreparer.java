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
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
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
 */
package org.knime.dl.core.training;

import java.util.Map;
import java.util.Map.Entry;

import org.knime.core.data.DataRow;
import org.knime.core.node.NodeLogger;
import org.knime.dl.core.DLAbstractKnimeNetworkInputPreparer;
import org.knime.dl.core.DLCanceledExecutionException;
import org.knime.dl.core.DLInvalidNetworkInputException;
import org.knime.dl.core.DLRowIterator;
import org.knime.dl.core.DLTensor;
import org.knime.dl.core.DLTensorId;
import org.knime.dl.core.data.DLWritableBuffer;
import org.knime.dl.core.data.convert.DLDataValueToTensorConverterFactory;
import org.knime.dl.util.DLUtils;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public final class DLKnimeNetworkValidationInputPreparer extends DLAbstractKnimeNetworkInputPreparer {

	private static final NodeLogger LOGGER = NodeLogger.getLogger(DLKnimeNetworkValidationInputPreparer.class);

	/**
	 * @param iterator provides the input data rows that are used by this instance to prepare (fill) the network tensors
	 *            fed to {@link #prepare(Map, long)}. The iterator must know its size and must be resettable. It must be
	 *            in a proper initial state (i.e. reset).
	 * @param batchSize the batch size of the tensors that will be prepared by this instance
	 * @param converters the converters that are used to write the data rows into the tensors. The given tensor ids
	 *            determine the set of tensors supported by {@link #prepare(Map, long)}.
	 */
	public DLKnimeNetworkValidationInputPreparer(final DLRowIterator iterator, final int batchSize,
			final Map<DLTensorId, DLDataValueToTensorConverterFactory<?, ?>> converters) {
		super(iterator, batchSize, converters);
		final long size = iterator.size();
		if (size % batchSize != 0) {
			LOGGER.warn("The number of rows of the input validation data table (" + size
					+ ") is not a multiple of the selected validation batch size (" + batchSize
					+ "). Thus, the last batch of each validation phase will continue at the beginning of the "
					+ "validation data table after reaching its end. You can avoid that by adjusting the number of "
					+ "rows of the table or the batch size if desired.");
		}
	}

	@Override
	public long getNumBatches() {
		// TODO: only valid if we don't crop the last batch. This has to be taken into consideration if we want to add
		// 'crop' as an alternative strategy for handling incomplete batches.
		return (long) Math.ceil(m_iterator.size() / (double) m_batchSize);
	}

	@Override
	public void prepare(final Map<DLTensorId, DLTensor<? extends DLWritableBuffer>> input, final long batchIndex)
			throws DLCanceledExecutionException, DLInvalidNetworkInputException {
		boolean reset = false;
		for (long i = 0; i < m_batchSize; i++) {
			if (!m_iterator.hasNext()) {
				// continue at the beginning of the table to fill up incomplete batch
				m_iterator.reset();
				reset = true;
			}
			final DataRow row = m_iterator.next();
			try {
				writeDataValuesInTensors(m_iterator.groupByTensor(row), input);
			} catch (final DLBufferOverflowExceptionForTensor ex) {
				final DLTensor<?> tensor = ex.getTensor();
				// must be present
				final long exampleSize = DLUtils.Shapes.getFixedSize(tensor.getSpec().getShape()).getAsLong();
				// must be present
				final long batchSize = tensor.getSpec().getBatchSize().getAsLong();
				throw new DLInvalidNetworkInputException(
						"Node validation data size for input/target '" + tensor.getSpec().getName()
								+ "' exceeds the expected size. Neuron count of this input/target is " + exampleSize
								+ ", batch size is " + batchSize + ". Thus, expected validation data size is "
								+ exampleSize * batchSize + ". Please check the column selection for this input/target "
								+ "and validate the node's validation data.",
						ex);
			}
		}
		if (reset) {
			// Validation outcomes must be comparable. Each validation phase should be executed with the same set of
			// batches.
			m_iterator.reset();
		}
		// check if tensors were filled correctly
		for (final Entry<DLTensorId, DLTensor<? extends DLWritableBuffer>> entry : input.entrySet()) {
			final DLTensor<? extends DLWritableBuffer> tensor = entry.getValue();
			if (tensor.getBuffer().size() != tensor.getExampleSize() * m_batchSize) {
				// Must be present. Note that exampleSize == tensor.getExampleSize() does not necessarily hold
				// as the latter is expressed in terms of buffer elements, not input elements ("neurons").
				final long exampleSize = DLUtils.Shapes.getFixedSize(tensor.getSpec().getShape()).getAsLong();
				final long bufferSizeInNeurons = tensor.getBuffer().size() * (exampleSize / tensor.getExampleSize());
				throw new DLInvalidNetworkInputException("Node validation data size for network input/target '"
						+ tensor.getSpec().getName() + "' does not match the expected size. Neuron count is "
						+ exampleSize + ", batch size is " + m_batchSize + ". Thus, expected validation data size is "
						+ exampleSize * m_batchSize + ". However, node validation data size is " + bufferSizeInNeurons
						+ ". Please check the column selection for this input/target "
						+ "and validate the node's validation data.");
			}
		}
	}
}
