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
 */
package org.knime.dl.core.execution;

import static com.google.common.base.Preconditions.checkNotNull;

import java.nio.BufferOverflowException;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataValue;
import org.knime.core.node.NodeLogger;
import org.knime.dl.core.DLCanceledExecutionException;
import org.knime.dl.core.DLRowIterator;
import org.knime.dl.core.DLTensor;
import org.knime.dl.core.DLTensorId;
import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.core.data.DLWritableBuffer;
import org.knime.dl.core.data.convert.DLDataValueToTensorConverter;
import org.knime.dl.core.data.convert.DLDataValueToTensorConverterFactory;
import org.knime.dl.util.DLUtils;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public class DLKnimeNetworkInputPreparer implements DLNetworkInputPreparer {

	private static final NodeLogger LOGGER = NodeLogger.getLogger(DLKnimeNetworkInputPreparer.class);

	private final DLRowIterator m_iterator;

	private final int m_batchSize;

	private final Map<DLTensorId, DLDataValueToTensorConverter<?, ?>> m_converters;

	private final Queue<DataRow> m_baseRows;

	public DLKnimeNetworkInputPreparer(final DLRowIterator iterator, final int batchSize,
			final Map<DLTensorId, DLDataValueToTensorConverterFactory<?, ?>> converters) {
		m_iterator = checkNotNull(iterator);
		m_batchSize = batchSize;
		m_converters = new HashMap<>(checkNotNull(converters).size());
		for (final Entry<DLTensorId, DLDataValueToTensorConverterFactory<?, ?>> converter : converters.entrySet()) {
			m_converters.put(converter.getKey(), converter.getValue().createConverter());
		}
		m_baseRows = new ArrayDeque<>(m_batchSize);

		iterator.size().ifPresent(sz -> {
			if (sz % batchSize != 0) {
				LOGGER.warn("The number of rows of the input table (" + sz
						+ ") is not a multiple of the selected batch size (" + batchSize
						+ "). Thus, the last batch of each epoch will continue at the beginning of the table after reaching its end. "
						+ "You can avoid that by adjusting the number of rows of the input table or the batch size if desired.");
			}
		});
	}

	@Override
	public long getNumBatches() {
		// TODO: only valid if we don't crop the last batch. This has to be considered if we want to add 'crop' as an
		// alternative strategy for handling incomplete batches.
		return (long) Math.ceil(m_iterator.size()
				.orElseThrow(() -> new UnsupportedOperationException("Input iterator does not know its datas' size."))
				/ (double) m_batchSize);
	}

	public Queue<DataRow> getBaseRows() {
		return m_baseRows;
	}

	@Override
	public void prepare(final Map<DLTensorId, DLTensor<? extends DLWritableBuffer>> input, final long batchIndex)
			throws DLCanceledExecutionException, DLInvalidNetworkInputException {
		for (long i = 0; i < m_batchSize; i++) {
			if (!m_iterator.hasNext()) {
				m_iterator.reset();
			}
			final DataRow row = m_iterator.next();
			m_baseRows.add(row);
			final Map<DLTensorId, List<DataValue>> inputForTensor = m_iterator.groupByTensor(row);
			for (final Entry<DLTensorId, DLTensor<? extends DLWritableBuffer>> entry : input.entrySet()) {
				final DLTensorId identifier = entry.getKey();
				final DLTensor<? extends DLWritableBuffer> tensor = entry.getValue();
				final DLDataValueToTensorConverter converter = m_converters.get(identifier);
				try {
					converter.convert(inputForTensor.get(identifier), tensor);
				} catch (final BufferOverflowException ex) {
					// must be present
					final long sampleSize = DLUtils.Shapes.getFixedSize(tensor.getSpec().getShape()).getAsLong();
					// must be present
					final long batchSize = tensor.getSpec().getBatchSize().getAsLong();
					throw new DLInvalidNetworkInputException(
							"Node input/target data size exceeds the expected size of network input/target '"
									+ tensor.getSpec().getName() + "'. Neuron count is " + sampleSize
									+ ", batch size is " + batchSize + ". Thus, expected input/target data size is "
									+ sampleSize * batchSize
									+ ". Please check the column selection for this input/target "
									+ "and validate the node's input/target data.",
							ex);
				}
			}
		}

		for (final Entry<DLTensorId, DLTensor<? extends DLWritableBuffer>> entry : input.entrySet()) {
			final DLTensor<? extends DLWritableBuffer> tensor = entry.getValue();
			final DLTensorSpec spec = tensor.getSpec();
			if (tensor.getBuffer().size() != tensor.getBuffer().getCapacity()) {
				// must be present
				final long sampleSize = DLUtils.Shapes.getFixedSize(spec.getShape()).getAsLong();
				if (tensor.getBuffer().size() % (sampleSize * m_batchSize) != 0) {
					// TODO: learner vs. executor
					throw new DLInvalidNetworkInputException(
							"Node input/target data size does not match the expected size of network input/target '"
									+ tensor.getSpec().getName() + "'. Neuron count is " + sampleSize
									+ ", batch size is " + m_batchSize + ". Thus, expected input/target size is "
									+ sampleSize * m_batchSize + ". However, node input/target data size is "
									+ tensor.getBuffer().size()
									+ ". Please check the column selection for this input/target "
									+ "and validate the node's input/target data.");
				}
			}
		}
	}

	@Override
	public void close() throws Exception {
		m_iterator.close();
	}
}