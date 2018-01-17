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
package org.knime.dl.python.core.data;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

import org.knime.core.data.DataCell;
import org.knime.dl.core.data.DLReadableBuffer;
import org.knime.dl.core.data.DLWrappingDataBuffer;
import org.knime.dl.core.data.DLWritableBuffer;
import org.knime.python.typeextension.Deserializer;

/**
 * Abstract base class for {@link DLReadableBuffer readable} and {@link DLWritableBuffer writable} buffers specifically
 * for use with Python deep learning back ends. Instances of this class simply delegate to another matching buffer
 * instance.<br>
 * This is needed because {@link Deserializer Python deserializers} are working with {@link DataCell}.
 *
 * @param <B> the delegate buffer type
 * @param <S> the storage type
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("serial") // not intended for serialization
public abstract class DLPythonAbstractDataBuffer<B extends DLWrappingDataBuffer<S>, S> extends DataCell
		implements DLPythonDataBuffer<S> {

	protected final B m_buffer;

	/**
	 * Creates a new instance of this buffer.
	 *
	 * @param buffer the delegate buffer
	 */
	protected DLPythonAbstractDataBuffer(final B buffer) {
		m_buffer = buffer;
	}

	@Override
	public long size() {
		return m_buffer.size();
	}

	@Override
	public long getCapacity() {
		return m_buffer.getCapacity();
	}

	@Override
	public void setStorage(final S storage, final long storageSize) throws IllegalArgumentException {
		m_buffer.setStorage(storage, storageSize);
	}

	@Override
	public long getNextReadPosition() {
		return m_buffer.getNextReadPosition();
	}

	@Override
	public S getStorageForReading(final long startPos, final long length) throws BufferUnderflowException {
		return m_buffer.getStorageForReading(startPos, length);
	}

	@Override
	public S getStorageForWriting(final long startPos, final long length) throws BufferOverflowException {
		return m_buffer.getStorageForWriting(startPos, length);
	}

	@Override
	public void zeroPad(final long length) throws IllegalArgumentException, BufferOverflowException {
		m_buffer.zeroPad(length);
	}

	@Override
	public void resetRead() {
		m_buffer.resetRead();
	}

	@Override
	public void resetWrite() {
		m_buffer.resetWrite();
	}

	@Override
	public void close() {
		m_buffer.close();
	}

	@Override
	public int hashCode() { // DataCell#equals(Object) is final
		return m_buffer.hashCode();
	}

	@Override
	public String toString() {
		return m_buffer.toString();
	}

	@Override
	protected boolean equalsDataCell(final DataCell dc) {
		return m_buffer.equals(((DLPythonAbstractDataBuffer<?, ?>) dc).m_buffer);
	}
}
