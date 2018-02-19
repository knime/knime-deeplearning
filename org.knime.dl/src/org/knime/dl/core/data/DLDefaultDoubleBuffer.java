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
 * History
 *   Jun 28, 2017 (marcel): created
 */
package org.knime.dl.core.data;

import static com.google.common.base.Preconditions.checkArgument;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

/**
 * Double type implementation of {@link DLWrappingDataBuffer}.
 *
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public class DLDefaultDoubleBuffer extends DLAbstractWrappingDataBuffer<double[]>
		implements DLWritableDoubleBuffer, DLReadableDoubleBuffer {

	/**
	 * Creates a new instance of this buffer.
	 *
	 * @param capacity the immutable capacity of the buffer
	 */
	public DLDefaultDoubleBuffer(final long capacity) {
		super(capacity);
	}

	@Override
	public void setStorage(final double[] storage, final long storageSize) throws IllegalArgumentException {
		checkArgument(storage.length == m_capacity, "Input storage capacity does not match buffer capacity.");
		m_storage = storage;
		m_nextWrite = (int) storageSize;
		resetRead();
	}

	@Override
	public double readNextDouble() throws BufferUnderflowException {
		checkUnderflow(m_nextRead < m_nextWrite);
		return m_storage[m_nextRead++];
	}

	@Override
	public double[] toDoubleArray() {
		return m_storage.clone();
	}

	@Override
	public void put(final boolean value) throws BufferOverflowException {
		checkOverflow(m_nextWrite < m_capacity);
		m_storage[m_nextWrite++] = value ? 1d : 0d;
	}

	@Override
	public void putAll(final boolean[] values) throws BufferOverflowException {
		checkOverflow(m_nextWrite + values.length <= m_capacity);
		for (int i = 0; i < values.length; i++) {
			m_storage[m_nextWrite++] = values[i] ? 1d : 0d;
		}
	}

	@Override
	public void put(final byte value) throws BufferOverflowException {
		checkOverflow(m_nextWrite < m_capacity);
		m_storage[m_nextWrite++] = value;
	}

	@Override
	public void putAll(final byte[] values) throws BufferOverflowException {
		checkOverflow(m_nextWrite + values.length <= m_capacity);
		for (int i = 0; i < values.length; i++) {
			m_storage[m_nextWrite++] = values[i];
		}
	}

	@Override
	public void put(final double value) throws BufferOverflowException {
		checkOverflow(m_nextWrite < m_capacity);
		m_storage[m_nextWrite++] = value;
	}

	@Override
	public void putAll(final double[] values) throws BufferOverflowException {
		checkOverflow(m_nextWrite + values.length <= m_capacity);
		System.arraycopy(values, 0, m_storage, m_nextWrite, values.length);
		m_nextWrite += values.length;
	}

	@Override
	public void put(final float value) throws BufferOverflowException {
		checkOverflow(m_nextWrite < m_capacity);
		m_storage[m_nextWrite++] = value;
	}

	@Override
	public void putAll(final float[] values) throws BufferOverflowException {
		checkOverflow(m_nextWrite + values.length <= m_capacity);
		for (int i = 0; i < values.length; i++) {
			m_storage[m_nextWrite++] = values[i];
		}
	}

	@Override
	public void put(final int value) throws BufferOverflowException {
		checkOverflow(m_nextWrite < m_capacity);
		m_storage[m_nextWrite++] = value;
	}

	@Override
	public void putAll(final int[] values) throws BufferOverflowException {
		checkOverflow(m_nextWrite + values.length <= m_capacity);
		for (int i = 0; i < values.length; i++) {
			m_storage[m_nextWrite++] = values[i];
		}
	}

	@Override
	public void put(final short value) throws BufferOverflowException {
		checkOverflow(m_nextWrite < m_capacity);
		m_storage[m_nextWrite++] = value;
	}

	@Override
	public void putAll(final short[] values) throws BufferOverflowException {
		checkOverflow(m_nextWrite + values.length <= m_capacity);
		for (int i = 0; i < values.length; i++) {
			m_storage[m_nextWrite++] = values[i];
		}
	}

	@Override
	public void zeroPad(final long length) throws IllegalArgumentException, BufferOverflowException {
		checkArgument(length > 0);
		checkOverflow(m_nextWrite + length <= m_capacity);
		for (int i = 0; i < length; i++) {
			m_storage[m_nextWrite++] = 0.0;
		}
	}

	@Override
	protected double[] createStorage() {
		return new double[m_capacity];
	}

	@Override
	public void readToDoubleArray(double[] dest, int destPos, int length) {
		checkArgument(destPos > 0);
		checkArgument(length > 0);
		checkUnderflow(m_nextRead + length <= m_nextWrite);
		System.arraycopy(m_storage, m_nextRead, dest, destPos, length);
		m_nextRead += length;
	}
}
