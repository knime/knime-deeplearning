/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *   Jun 28, 2017 (marcel): created
 */
package org.knime.dl.python.core.data;

import static com.google.common.base.Preconditions.checkArgument;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

import org.knime.core.data.DataCell;
import org.knime.dl.core.data.DLReadableBuffer;
import org.knime.dl.core.data.DLWritableBuffer;

/**
 * Abstract base class for {@link DLReadableBuffer readable} and {@link DLWritableBuffer writable} buffers specifically
 * for use with Python deep learning back ends.
 *
 * @param <S> the storage type
 *
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
public abstract class DLPythonAbstractDataBuffer<S> extends DataCell implements DLPythonDataBuffer<S> {

	private static final long serialVersionUID = 1L;

	/**
	 * @param expression a boolean expression
	 * @throws BufferOverflowException if {@code expression} is false
	 */
	protected static void checkOverflow(final boolean expression) throws BufferOverflowException {
		if (!expression) {
			throw new BufferOverflowException();
		}
	}

	/**
	 * @param expression a boolean expression
	 * @throws BufferUnderflowException if {@code expression} is false *
	 */
	protected static void checkUnderflow(final boolean expression) throws BufferUnderflowException {
		if (!expression) {
			throw new BufferUnderflowException();
		}
	}

	/**
	 * The immutable capacity of the buffer.
	 */
	protected final int m_capacity;

	/**
	 * The internal storage.
	 */
	protected S m_storage;

	/**
	 * The next write position. Equals {@links #size()}.
	 */
	protected int m_nextWrite = 0;

	/**
	 * The next read position.
	 */
	protected int m_nextRead = 0;

	/**
	 * Creates a new instance of this buffer.
	 *
	 * @param capacity the immutable capacity of the buffer
	 */
	protected DLPythonAbstractDataBuffer(final long capacity) {
		checkArgument(capacity <= Integer.MAX_VALUE,
				"Invalid input capacity. Buffer only supports capacities up to " + Integer.MAX_VALUE + ".");
		m_capacity = (int) capacity;
		m_storage = createStorage();
	}

	/**
	 * Creates the internal storage of this buffer. This method is only called once during construction of the instance.
	 *
	 * @return the internal storage
	 */
	protected abstract S createStorage();


	@Override
	public long size() {
		return m_nextWrite;
	}


	@Override
	public long getCapacity() {
		return m_capacity;
	}


	@Override
	public S getStorageForReading(final long startPos, final long length) throws BufferUnderflowException {
		checkUnderflow(startPos + length <= m_nextWrite);
		return m_storage;
	}


	@Override
	public S getStorageForWriting(final long startPos, final long length) throws BufferOverflowException {
		checkOverflow(startPos + length <= m_capacity);
		m_nextWrite = (int) (startPos + length);
		return m_storage;
	}


	@Override
	public void resetRead() {
		m_nextRead = 0;
	}


	@Override
	public void resetWrite() {
		m_nextWrite = 0;
	}


	@Override
	public void close() throws Exception {
		m_storage = null;
	}


	@Override
	public int hashCode() {
		return m_storage.hashCode();
	}


	@Override
	public String toString() {
		return "Buffer with capacity: " + m_capacity;
	}
}
