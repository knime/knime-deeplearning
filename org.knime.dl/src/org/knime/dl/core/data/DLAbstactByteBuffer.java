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
package org.knime.dl.core.data;

import static com.google.common.base.Preconditions.checkArgument;

import java.nio.BufferOverflowException;

/**
 * Byte type implementation of {@link DLWrappingDataBuffer}.
 *
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 */
public class DLAbstactByteBuffer extends DLAbstractFlatWrappingDataBuffer<byte[]>
    implements DLWritableByteBuffer, DLWritableUnsignedByteBuffer {

    private static final int MAX_UNSIGNED_VAL = (1 << Byte.SIZE) - 1;

    /**
     * Creates a new instance of this buffer.
     *
     * @param capacity the immutable capacity of the buffer
     */
    public DLAbstactByteBuffer(final long capacity) {
        super(capacity);
    }

    @Override
    public void setStorage(final byte[] storage, final long storageSize) throws IllegalArgumentException {
        checkArgument(storage.length == m_capacity, "Input storage capacity does not match buffer capacity.");
        m_storage = storage;
        m_nextWrite = (int)storageSize;
        resetRead();
    }

    @Override
    public void zeroPad(long length) throws IllegalArgumentException, BufferOverflowException {
        checkArgument(length > 0);
        checkOverflow(m_nextWrite + length <= m_capacity);
        for (int i = 0; i < length; i++) {
            m_storage[m_nextWrite++] = 0;
        }
    }

    @Override
    public void put(boolean value) throws BufferOverflowException {
        checkOverflow(m_nextWrite < m_capacity);
        m_storage[m_nextWrite++] = (byte)(value ? 1 : 0);
    }

    @Override
    public void putAll(boolean[] values) throws BufferOverflowException {
        checkOverflow(m_nextWrite + values.length <= m_capacity);
        for (int i = 0; i < values.length; i++) {
            m_storage[m_nextWrite++] = (byte)(values[i] ? 1 : 0);
        }
    }

    @Override
    public void put(byte value) throws BufferOverflowException {
        checkOverflow(m_nextWrite < m_capacity);
        m_storage[m_nextWrite++] = value;
    }

    @Override
    public void putAll(byte[] values) throws BufferOverflowException {
        checkOverflow(m_nextWrite + values.length <= m_capacity);
        System.arraycopy(values, 0, m_storage, m_nextWrite, values.length);
        m_nextWrite += values.length;
    }

    @Override
    public void put(short value) throws BufferOverflowException {
        checkOverflow(m_nextWrite < m_capacity);
        checkArgument(0 <= value && value <= MAX_UNSIGNED_VAL, "Unsinged byte must be between 0 and %s.",
            MAX_UNSIGNED_VAL);
        m_storage[m_nextWrite++] = (byte)value;
    }

    @Override
    public void putAll(short[] values) throws BufferOverflowException {
        checkOverflow(m_nextWrite + values.length <= m_capacity);
        for (int i = 0; i < values.length; i++) {
            checkArgument(0 <= values[i] && values[i] <= MAX_UNSIGNED_VAL, "Unsinged byte must be between 0 and %s.",
                MAX_UNSIGNED_VAL);
            m_storage[m_nextWrite++] = (byte)values[i];
        }
    }

    @Override
    protected byte[] createStorage() {
        return new byte[m_capacity];
    }
}
