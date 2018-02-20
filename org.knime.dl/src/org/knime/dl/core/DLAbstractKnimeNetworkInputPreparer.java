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
package org.knime.dl.core;

import static com.google.common.base.Preconditions.checkNotNull;

import java.nio.BufferOverflowException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.knime.core.data.DataValue;
import org.knime.dl.core.data.DLWritableBuffer;
import org.knime.dl.core.data.convert.DLDataValueToTensorConverter;
import org.knime.dl.core.data.convert.DLDataValueToTensorConverterFactory;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public abstract class DLAbstractKnimeNetworkInputPreparer implements DLNetworkInputPreparer {

	protected final DLRowIterator m_iterator;

	protected final int m_batchSize;

	protected final Map<DLTensorId, DLDataValueToTensorConverter<?, ?>> m_converters;

	/**
	 * @param iterator provides the input data rows that are used by this instance to prepare (fill) the network tensors
	 *            fed to {@link #prepare(Map, long)}.
	 * @param batchSize the batch size of the tensors that will be prepared by this instance
	 * @param converters the converters that are used to write the data rows into the tensors. The given tensor ids
	 *            determine the set of tensors supported by {@link #prepare(Map, long)}.
	 */
	public DLAbstractKnimeNetworkInputPreparer(final DLRowIterator iterator, final int batchSize,
			final Map<DLTensorId, DLDataValueToTensorConverterFactory<?, ?>> converters) {
		m_iterator = checkNotNull(iterator);
		m_batchSize = batchSize;
		m_converters = new HashMap<>(checkNotNull(converters).size());
		for (final Entry<DLTensorId, DLDataValueToTensorConverterFactory<?, ?>> converter : converters.entrySet()) {
			m_converters.put(converter.getKey(), converter.getValue().createConverter());
		}
	}

	@Override
	public void close() throws Exception {
		m_iterator.close();
	}

	/**
	 * @param dataValues the data values which to write in the tensors
	 * @param tensors the tensors in which to write the data values
	 * @throws DLBufferOverflowExceptionForTensor if writing in a tensor exceeds its buffer's capacity. The affected
	 *             tensor can be retrieved via {@link DLBufferOverflowExceptionForTensor#getTensor()}.
	 */
	protected final void writeDataValuesInTensors(final Map<DLTensorId, List<DataValue>> dataValues,
			final Map<DLTensorId, DLTensor<? extends DLWritableBuffer>> tensors)
			throws DLBufferOverflowExceptionForTensor {
		for (final Entry<DLTensorId, DLTensor<? extends DLWritableBuffer>> entry : tensors.entrySet()) {
			final DLTensorId identifier = entry.getKey();
			final DLTensor<? extends DLWritableBuffer> tensor = entry.getValue();
			final DLDataValueToTensorConverter converter = m_converters.get(identifier);
			try {
				converter.convert(dataValues.get(identifier), tensor);
			} catch (final BufferOverflowException ex) {
				throw new DLBufferOverflowExceptionForTensor(ex, tensor);
			}
		}
	}

	/**
	 * Thrown by {@link DLAbstractKnimeNetworkInputPreparer#writeDataValuesInTensors(Map, Map)} if a
	 * <code>BufferOverflowException</code> occurs while filling a tensor.
	 */
	protected static class DLBufferOverflowExceptionForTensor extends Exception {

		private static final long serialVersionUID = 1L;

		private final DLTensor<?> m_tensor;

		private DLBufferOverflowExceptionForTensor(final BufferOverflowException cause, final DLTensor<?> tensor) {
			super(cause);
			m_tensor = tensor;
		}

		/**
		 * Returns the underlying <code>BufferOverflowException</code>.
		 *
		 * @return the underlying <code>BufferOverflowException</code>
		 */
		@Override
		public synchronized BufferOverflowException getCause() {
			return (BufferOverflowException) super.getCause();
		}

		/**
		 * @return the affected tensor
		 */
		public DLTensor<?> getTensor() {
			return m_tensor;
		}
	}
}
