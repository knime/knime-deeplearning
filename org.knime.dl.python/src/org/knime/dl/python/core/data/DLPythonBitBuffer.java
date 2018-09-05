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

import org.knime.core.data.DataType;
import org.knime.dl.core.data.DLDefaultBitBuffer;
import org.knime.dl.core.data.DLReadableBitBuffer;
import org.knime.dl.core.data.DLWritableBitBuffer;

/**
 * Int type implementation of {@link DLPythonAbstractDataBuffer}.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("serial") // not intended for serialization
public class DLPythonBitBuffer extends DLPythonAbstractDataBuffer<DLDefaultBitBuffer, boolean[]>
    implements DLWritableBitBuffer, DLReadableBitBuffer {

    /**
     * This buffer's {@link DataType}.
     */
    public static final DataType TYPE = DataType.getType(DLPythonBitBuffer.class);

    /**
     * Creates a new instance of this buffer.
     *
     * @param capacity the immutable capacity of the buffer
     */
    public DLPythonBitBuffer(final long capacity) {
        super(new DLDefaultBitBuffer(capacity));
    }

    @Override
    public boolean readNextBit() throws BufferUnderflowException {
        return m_buffer.readNextBit();
    }

    @Override
    public boolean[] toBitArray() {
        return m_buffer.toBitArray();
    }

    @Override
    public void readToBitArray(boolean[] dest, int destPos, int length) {
        m_buffer.readToBitArray(dest, destPos, length);
    }

    @Override
    public byte readNextByte() throws BufferUnderflowException {
        return m_buffer.readNextByte();
    }

    @Override
    public byte[] toByteArray() {
        return m_buffer.toByteArray();
    }

    @Override
    public void readToByteArray(byte[] dest, int destPos, int length) {
        m_buffer.readToByteArray(dest, destPos, length);
    }

    @Override
    public short readNextShort() throws BufferUnderflowException {
        return m_buffer.readNextShort();
    }

    @Override
    public short[] toShortArray() {
        return m_buffer.toShortArray();
    }

    @Override
    public void readToShortArray(short[] dest, int destPos, int length) {
        m_buffer.readToShortArray(dest, destPos, length);
    }

    @Override
    public int readNextInt() throws BufferUnderflowException {
        return m_buffer.readNextInt();
    }

    @Override
    public int[] toIntArray() {
        return m_buffer.toIntArray();
    }

    @Override
    public void readToIntArray(int[] dest, int destPos, int length) {
        m_buffer.readToIntArray(dest, destPos, length);
    }

    @Override
    public long readNextLong() throws BufferUnderflowException {
        return m_buffer.readNextLong();
    }

    @Override
    public long[] toLongArray() {
        return m_buffer.toLongArray();
    }

    @Override
    public void readToLongArray(long[] dest, int destPos, int length) {
        m_buffer.readToLongArray(dest, destPos, length);
    }

    @Override
    public float readNextFloat() throws BufferUnderflowException {
        return m_buffer.readNextFloat();
    }

    @Override
    public float[] toFloatArray() {
        return m_buffer.toFloatArray();
    }

    @Override
    public void readToFloatArray(float[] dest, int destPos, int length) {
        m_buffer.readToFloatArray(dest, destPos, length);
    }

    @Override
    public double readNextDouble() throws BufferUnderflowException {
        return m_buffer.readNextDouble();
    }

    @Override
    public double[] toDoubleArray() {
        return m_buffer.toDoubleArray();
    }

    @Override
    public void readToDoubleArray(double[] dest, int destPos, int length) {
        m_buffer.readToDoubleArray(dest, destPos, length);
    }

    @Override
    public void put(final boolean value) throws BufferOverflowException {
        m_buffer.put(value);
    }

    @Override
    public void putAll(final boolean[] values) throws BufferOverflowException {
        m_buffer.putAll(values);
    }
}
