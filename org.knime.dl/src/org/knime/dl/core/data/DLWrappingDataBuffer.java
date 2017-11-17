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
package org.knime.dl.core.data;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

/**
 * A {@link DLReadableBuffer readable} and {@link DLWritableBuffer writable} buffer that simply wraps a storage.
 *
 * @param <S> the storage type
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public interface DLWrappingDataBuffer<S> extends DLReadableBuffer, DLWritableBuffer {

	long getNextReadPosition();

	/**
	 * Returns the actual storage object that holds the buffer's data. The returned object must not be modified. It can
	 * be read in a way that conforms to the arguments.
	 *
	 * @param startPos the position where the caller will start reading
	 * @param length the number of elements that will be read
	 * @return the buffer's storage
	 * @throws BufferUnderflowException if the buffer's {@link #size() size} will be exceeded.
	 */
	S getStorageForReading(long startPos, long length) throws BufferUnderflowException;

	/**
	 * Returns the actual storage object that holds the buffer's data. The returned object can be modified in a way that
	 * conforms to the arguments. Note that {@link #size()} will return a value equal to {@code startPos} +
	 * {@code length} immediately after this method returns.
	 *
	 * @param startPos the position where the caller will start writing
	 * @param length the number of elements that will be written
	 * @return the buffer's storage
	 * @throws BufferOverflowException if the buffer's {@link #getCapacity() capacity} will be exceeded.
	 */
	S getStorageForWriting(long startPos, long length) throws BufferOverflowException;

	/**
	 * Sets the buffer's internal storage to the first argument if its capacity matches {@link #getCapacity()}. Sets the
	 * buffer's size to the second argument, i.e. {@link #size()} reports that value immediately after this method
	 * returns.<br>
	 * Also, this method has side effects that are equivalent to a call of {@link #resetRead()}.
	 *
	 * @param storage the storage to be set
	 * @param storageSize the size of the storage
	 * @throws IllegalArgumentException if the first argument's capacity does not equal {@link #getCapacity()}.
	 */
	void setStorage(final S storage, long storageSize) throws IllegalArgumentException;

	@Override
	default void reset() {
		resetRead();
		resetWrite();
	}
}
