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

import java.nio.BufferUnderflowException;

/**
 * Unsigned byte type implementation of {@link DLAbstractByteBuffer}.
 *
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 */
public class DLDefaultUnsignedByteBuffer extends DLAbstractByteBuffer implements DLReadableUnsignedByteBuffer {

    private static final int UNSIGNED_MASK = 0xFF;

    /**
     * Creates a new instance of this buffer.
     *
     * @param capacity the immutable capacity of the buffer
     */
    public DLDefaultUnsignedByteBuffer(final long capacity) {
        super(capacity);
    }

    @Override
    public short readNextUnsignedByte() throws BufferUnderflowException {
        checkUnderflow(m_nextRead < m_nextWrite);
        return (short)(m_storage[m_nextRead++] & UNSIGNED_MASK);
    }

    @Override
    public short[] toUnsignedByteArray() {
        final short[] tmp = new short[m_storage.length];
        for (int i = 0; i < m_storage.length; i++) {
            tmp[i] = (short)(m_storage[i] & UNSIGNED_MASK);
        }
        return tmp;
    }

    @Override
    public void readToUnsignedByteArray(short[] dest, int destPos, int length) {
        checkArgument(destPos >= 0);
        checkArgument(length > 0);
        checkUnderflow(m_nextRead + length <= m_nextWrite);
        for (int i = 0; i < length; i++) {
            dest[destPos + i] = (short)(m_storage[m_nextRead + i] & UNSIGNED_MASK);
        }
        m_nextRead += length;
    }

    @Override
    public short readNextShort() throws BufferUnderflowException {
        checkUnderflow(m_nextRead < m_nextWrite);
        return (short)(m_storage[m_nextRead++] & UNSIGNED_MASK);
    }

    @Override
    public short[] toShortArray() {
        final short[] tmp = new short[m_storage.length];
        for (int i = 0; i < m_storage.length; i++) {
            tmp[i] = (short)(m_storage[i] & UNSIGNED_MASK);
        }
        return tmp;
    }

    @Override
    public void readToShortArray(short[] dest, int destPos, int length) {
        checkArgument(destPos >= 0);
        checkArgument(length > 0);
        checkUnderflow(m_nextRead + length <= m_nextWrite);
        for (int i = 0; i < length; i++) {
            dest[destPos + i] = (short)(m_storage[m_nextRead + i] & UNSIGNED_MASK);
        }
        m_nextRead += length;
    }

    @Override
    public int readNextInt() throws BufferUnderflowException {
        checkUnderflow(m_nextRead < m_nextWrite);
        return m_storage[m_nextRead++] & UNSIGNED_MASK;
    }

    @Override
    public int[] toIntArray() {
        final int[] tmp = new int[m_storage.length];
        for (int i = 0; i < m_storage.length; i++) {
            tmp[i] = m_storage[i] & UNSIGNED_MASK;
        }
        return tmp;
    }

    @Override
    public void readToIntArray(int[] dest, int destPos, int length) {
        checkArgument(destPos >= 0);
        checkArgument(length > 0);
        checkUnderflow(m_nextRead + length <= m_nextWrite);
        for (int i = 0; i < length; i++) {
            dest[destPos + i] = m_storage[m_nextRead + i] & UNSIGNED_MASK;
        }
        m_nextRead += length;
    }

    @Override
    public long readNextLong() throws BufferUnderflowException {
        checkUnderflow(m_nextRead < m_nextWrite);
        return m_storage[m_nextRead++] & UNSIGNED_MASK;
    }

    @Override
    public long[] toLongArray() {
        final long[] tmp = new long[m_storage.length];
        for (int i = 0; i < m_storage.length; i++) {
            tmp[i] = m_storage[i] & UNSIGNED_MASK;
        }
        return tmp;
    }

    @Override
    public void readToLongArray(long[] dest, int destPos, int length) {
        checkArgument(destPos >= 0);
        checkArgument(length > 0);
        checkUnderflow(m_nextRead + length <= m_nextWrite);
        for (int i = 0; i < length; i++) {
            dest[destPos + i] = m_storage[m_nextRead + i] & UNSIGNED_MASK;
        }
        m_nextRead += length;
    }

    @Override
    public float readNextFloat() throws BufferUnderflowException {
        checkUnderflow(m_nextRead < m_nextWrite);
        return m_storage[m_nextRead++] & UNSIGNED_MASK;
    }

    @Override
    public float[] toFloatArray() {
        final float[] tmp = new float[m_storage.length];
        for (int i = 0; i < m_storage.length; i++) {
            tmp[i] = m_storage[i] & UNSIGNED_MASK;
        }
        return tmp;
    }

    @Override
    public void readToFloatArray(float[] dest, int destPos, int length) {
        checkArgument(destPos >= 0);
        checkArgument(length > 0);
        checkUnderflow(m_nextRead + length <= m_nextWrite);
        for (int i = 0; i < length; i++) {
            dest[destPos + i] = m_storage[m_nextRead + i] & UNSIGNED_MASK;
        }
        m_nextRead += length;
    }

    @Override
    public double readNextDouble() throws BufferUnderflowException {
        checkUnderflow(m_nextRead < m_nextWrite);
        return m_storage[m_nextRead++] & UNSIGNED_MASK;
    }

    @Override
    public double[] toDoubleArray() {
        final double[] tmp = new double[m_storage.length];
        for (int i = 0; i < m_storage.length; i++) {
            tmp[i] = m_storage[i] & UNSIGNED_MASK;
        }
        return tmp;
    }

    @Override
    public void readToDoubleArray(double[] dest, int destPos, int length) {
        checkArgument(destPos >= 0);
        checkArgument(length > 0);
        checkUnderflow(m_nextRead + length <= m_nextWrite);
        for (int i = 0; i < length; i++) {
            dest[destPos + i] = m_storage[m_nextRead + i] & UNSIGNED_MASK;
        }
        m_nextRead += length;
    }
}
