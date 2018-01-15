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

import java.nio.BufferOverflowException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataValue;
import org.knime.dl.core.DLAbstractKnimeNetworkInputPreparer;
import org.knime.dl.core.DLCanceledExecutionException;
import org.knime.dl.core.DLInvalidNetworkInputException;
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
public final class DLKnimeNetworkExecutionInputPreparer extends DLAbstractKnimeNetworkInputPreparer {

	public DLKnimeNetworkExecutionInputPreparer(final DLRowIterator iterator, final int batchSize,
			final Map<DLTensorId, DLDataValueToTensorConverterFactory<?, ?>> converters) {
		super(iterator, batchSize, converters);
	}

	@Override
	public long getNumBatches() {
		return (long) Math.ceil(m_iterator.size() / (double) m_batchSize);
	}

	@Override
	public void prepare(final Map<DLTensorId, DLTensor<? extends DLWritableBuffer>> input, final long batchIndex)
			throws DLCanceledExecutionException, DLInvalidNetworkInputException {
		long i;
		for (i = 0; i < m_batchSize; i++) {
			if (!m_iterator.hasNext()) {
				// batch will be incomplete, handled below
				break;
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
							"Node input data size exceeds the expected size of network input '"
									+ tensor.getSpec().getName() + "'. Neuron count is " + sampleSize
									+ ", batch size is " + batchSize + ". Thus, expected input data size is "
									+ sampleSize * batchSize + ". Please check the column selection for this input "
									+ "and validate the node's input data.",
							ex);
				}
			}
		}
		// check if tensors were filled correctly
		for (final Entry<DLTensorId, DLTensor<? extends DLWritableBuffer>> entry : input.entrySet()) {
			final DLTensor<? extends DLWritableBuffer> tensor = entry.getValue();
			final DLTensorSpec spec = tensor.getSpec();
			if (tensor.getBuffer().size() != tensor.getBuffer().getCapacity()) {
				// must be present
				final long sampleSize = DLUtils.Shapes.getFixedSize(spec.getShape()).getAsLong();
				if (i < m_batchSize && tensor.getBuffer().size() % sampleSize == 0) {
					// batch is incomplete but was correctly filled: ignore - downstream code has to handle this
				} else if (tensor.getBuffer().size() % (sampleSize * m_batchSize) != 0) {
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
}