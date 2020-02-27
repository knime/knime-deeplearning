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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.knime.dl.testing.DLTestUtil.DOUBLE_EPSILON;
import static org.knime.dl.testing.DLTestUtil.alternatingBooleanArray;
import static org.knime.dl.testing.DLTestUtil.byteRange;
import static org.knime.dl.testing.DLTestUtil.toByte;
import static org.knime.dl.testing.DLTestUtil.toDouble;
import static org.knime.dl.testing.DLTestUtil.toInt;
import static org.knime.dl.testing.DLTestUtil.toLong;
import static org.knime.dl.testing.DLTestUtil.toShort;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.util.Arrays;

import org.junit.Test;
import org.knime.dl.core.data.DLDefaultByteBuffer;

/**
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("javadoc")
public class DLDefaultByteBufferTest {

    @Test
    public void testPutBoolean() throws Exception {
        try (DLDefaultByteBuffer buffer = new DLDefaultByteBuffer(10)) {
            assertEquals(buffer.m_nextWrite, 0);
            buffer.put(true);
            assertEquals(1, buffer.m_storage[0]);
            assertEquals(1, buffer.m_nextWrite);
            buffer.put(false);
            assertEquals(0, buffer.m_storage[1]);
        }
    }

    @Test(expected = BufferOverflowException.class)
    public void testPutBooleanOverflow() throws Exception {
        try (DLDefaultByteBuffer buffer = new DLDefaultByteBuffer(1)) {
            buffer.put(true);
            buffer.put(false);
        }
    }

    @Test
    public void testPutAllBoolean() throws Exception {
        try (DLDefaultByteBuffer buffer = new DLDefaultByteBuffer(10)) {
            final boolean[] expected = alternatingBooleanArray(10);
            buffer.putAll(expected);
            assertArrayEquals(toByte(expected), buffer.m_storage);
        }
    }

    @Test(expected = BufferOverflowException.class)
    public void testPutAllBooleanOverflow() throws Exception {
        try (DLDefaultByteBuffer buffer = new DLDefaultByteBuffer(1)) {
            buffer.putAll(alternatingBooleanArray(10));
        }
    }

    @Test
    public void testPutByte() throws Exception {
        try (DLDefaultByteBuffer buffer = new DLDefaultByteBuffer(10)) {
            assertEquals(buffer.m_nextWrite, 0);
            buffer.put((byte)1);
            assertEquals(1, buffer.m_storage[0]);
            assertEquals(1, buffer.m_nextWrite);
            buffer.put((byte)-5);
            assertEquals(-5, buffer.m_storage[1]);
        }
    }

    @Test(expected = BufferOverflowException.class)
    public void testPutByteOverflow() throws Exception {
        try (DLDefaultByteBuffer buffer = new DLDefaultByteBuffer(1)) {
            buffer.put((byte)1);
            buffer.put((byte)2);
        }
    }

    @Test
    public void testPutAllByte() throws Exception {
        try (DLDefaultByteBuffer buffer = new DLDefaultByteBuffer(10)) {
            final byte[] expected = byteRange(10);
            buffer.putAll(expected);
            assertArrayEquals(expected, buffer.m_storage);
        }
    }

    @Test(expected = BufferOverflowException.class)
    public void testPutAllByteOverflow() throws Exception {
        try (DLDefaultByteBuffer buffer = new DLDefaultByteBuffer(1)) {
            buffer.putAll(byteRange(10));
        }
    }

    @Test
    public void testToDoubleArray() throws Exception {
        try (DLDefaultByteBuffer buffer = new DLDefaultByteBuffer(10)) {
            final byte[] expected = byteRange(10);
            buffer.putAll(expected);
            assertArrayEquals(toDouble(expected), buffer.toDoubleArray(), DOUBLE_EPSILON);
        }
    }

    @Test
    public void testToShortArray() throws Exception {
        try (DLDefaultByteBuffer buffer = new DLDefaultByteBuffer(10)) {
            final byte[] expected = byteRange(10);
            buffer.putAll(expected);
            assertArrayEquals(toShort(expected), buffer.toShortArray());
        }
    }

    @Test
    public void testToIntArray() throws Exception {
        try (DLDefaultByteBuffer buffer = new DLDefaultByteBuffer(10)) {
            final byte[] expected = byteRange(10);
            buffer.putAll(expected);
            assertArrayEquals(toInt(expected), buffer.toIntArray());
        }
    }

    @Test
    public void testToLongArray() throws Exception {
        try (DLDefaultByteBuffer buffer = new DLDefaultByteBuffer(10)) {
            final byte[] expected = byteRange(10);
            buffer.putAll(expected);
            assertArrayEquals(toLong(expected), buffer.toLongArray());
        }
    }

    @Test
    public void testReadToDoubleArray() throws Exception {
        try (DLDefaultByteBuffer buffer = new DLDefaultByteBuffer(10)) {
            final byte[] expected = byteRange(10);
            buffer.putAll(expected);
            final double[] filled = new double[expected.length];
            buffer.readToDoubleArray(filled, 0, filled.length);
            assertArrayEquals(toDouble(expected), filled, DOUBLE_EPSILON);
            Arrays.fill(filled, -1);
            expected[0] = -1;
            expected[9] = -1;
            buffer.resetRead();
            buffer.readNextDouble();
            buffer.readToDoubleArray(filled, 1, 8);
            assertArrayEquals(toDouble(expected), filled, DOUBLE_EPSILON);
        }
    }

    @Test(expected = BufferUnderflowException.class)
    public void testReadToDoubleArrayUnderflow() throws Exception {
        try (DLDefaultByteBuffer buffer = new DLDefaultByteBuffer(10)) {
            final byte[] expected = byteRange(10);
            buffer.putAll(expected);
            final double[] filled = new double[11];
            buffer.readToDoubleArray(filled, 0, filled.length);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReadToDoubleArrayNonPositiveLength() throws Exception {
        try (DLDefaultByteBuffer buffer = new DLDefaultByteBuffer(10)) {
            final byte[] expected = byteRange(10);
            buffer.putAll(expected);
            final double[] filled = new double[10];
            buffer.readToDoubleArray(filled, 0, 0);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReadToDoubleArrayNegativePos() throws Exception {
        try (DLDefaultByteBuffer buffer = new DLDefaultByteBuffer(10)) {
            final byte[] expected = byteRange(10);
            buffer.putAll(expected);
            final double[] filled = new double[10];
            buffer.readToDoubleArray(filled, -1, 10);
        }
    }

    @Test
    public void testReadToByteArray() throws Exception {
        try (DLDefaultByteBuffer buffer = new DLDefaultByteBuffer(10)) {
            final byte[] expected = byteRange(10);
            buffer.putAll(expected);
            final byte[] filled = new byte[expected.length];
            buffer.readToByteArray(filled, 0, filled.length);
            assertArrayEquals(expected, filled);
            Arrays.fill(filled, (byte)-1);
            expected[0] = -1;
            expected[9] = -1;
            buffer.resetRead();
            buffer.readNextDouble();
            buffer.readToByteArray(filled, 1, 8);
            assertArrayEquals(expected, filled);
        }
    }

    @Test(expected = BufferUnderflowException.class)
    public void testReadToByteArrayUnderflow() throws Exception {
        try (DLDefaultByteBuffer buffer = new DLDefaultByteBuffer(10)) {
            final byte[] expected = byteRange(10);
            buffer.putAll(expected);
            final byte[] filled = new byte[11];
            buffer.readToByteArray(filled, 0, filled.length);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReadToByteArrayNonPositiveLength() throws Exception {
        try (DLDefaultByteBuffer buffer = new DLDefaultByteBuffer(10)) {
            final byte[] expected = byteRange(10);
            buffer.putAll(expected);
            final byte[] filled = new byte[10];
            buffer.readToByteArray(filled, 0, 0);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReadToByteArrayNegativePos() throws Exception {
        try (DLDefaultByteBuffer buffer = new DLDefaultByteBuffer(10)) {
            final byte[] expected = byteRange(10);
            buffer.putAll(expected);
            final byte[] filled = new byte[10];
            buffer.readToByteArray(filled, -1, 10);
        }
    }

    @Test
    public void testReadToShortArray() throws Exception {
        try (DLDefaultByteBuffer buffer = new DLDefaultByteBuffer(10)) {
            final byte[] expected = byteRange(10);
            buffer.putAll(expected);
            final short[] filled = new short[expected.length];
            buffer.readToShortArray(filled, 0, filled.length);
            assertArrayEquals(toShort(expected), filled);
            Arrays.fill(filled, (short)-1);
            expected[0] = -1;
            expected[9] = -1;
            buffer.resetRead();
            buffer.readNextDouble();
            buffer.readToShortArray(filled, 1, 8);
            assertArrayEquals(toShort(expected), filled);
        }
    }

    @Test(expected = BufferUnderflowException.class)
    public void testReadToShortArrayUnderflow() throws Exception {
        try (DLDefaultByteBuffer buffer = new DLDefaultByteBuffer(10)) {
            final byte[] expected = byteRange(10);
            buffer.putAll(expected);
            final short[] filled = new short[11];
            buffer.readToShortArray(filled, 0, filled.length);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReadToShortArrayNonPositiveLength() throws Exception {
        try (DLDefaultByteBuffer buffer = new DLDefaultByteBuffer(10)) {
            final byte[] expected = byteRange(10);
            buffer.putAll(expected);
            final short[] filled = new short[10];
            buffer.readToShortArray(filled, 0, 0);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReadToShortArrayNegativePos() throws Exception {
        try (DLDefaultByteBuffer buffer = new DLDefaultByteBuffer(10)) {
            final byte[] expected = byteRange(10);
            buffer.putAll(expected);
            final short[] filled = new short[10];
            buffer.readToShortArray(filled, -1, 10);
        }
    }

    @Test
    public void testReadToIntArray() throws Exception {
        try (DLDefaultByteBuffer buffer = new DLDefaultByteBuffer(10)) {
            final byte[] expected = byteRange(10);
            buffer.putAll(expected);
            final int[] filled = new int[expected.length];
            buffer.readToIntArray(filled, 0, filled.length);
            assertArrayEquals(toInt(expected), filled);
            Arrays.fill(filled, -1);
            expected[0] = -1;
            expected[9] = -1;
            buffer.resetRead();
            buffer.readNextDouble();
            buffer.readToIntArray(filled, 1, 8);
            assertArrayEquals(toInt(expected), filled);
        }
    }

    @Test(expected = BufferUnderflowException.class)
    public void testReadToIntArrayUnderflow() throws Exception {
        try (DLDefaultByteBuffer buffer = new DLDefaultByteBuffer(10)) {
            final byte[] expected = byteRange(10);
            buffer.putAll(expected);
            final int[] filled = new int[11];
            buffer.readToIntArray(filled, 0, filled.length);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReadToIntArrayNonPositiveLength() throws Exception {
        try (DLDefaultByteBuffer buffer = new DLDefaultByteBuffer(10)) {
            final byte[] expected = byteRange(10);
            buffer.putAll(expected);
            final int[] filled = new int[10];
            buffer.readToIntArray(filled, 0, 0);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReadToIntArrayNegativePos() throws Exception {
        try (DLDefaultByteBuffer buffer = new DLDefaultByteBuffer(10)) {
            final byte[] expected = byteRange(10);
            buffer.putAll(expected);
            final int[] filled = new int[10];
            buffer.readToIntArray(filled, -1, 10);
        }
    }

    @Test
    public void testReadToLongArray() throws Exception {
        try (DLDefaultByteBuffer buffer = new DLDefaultByteBuffer(10)) {
            final byte[] expected = byteRange(10);
            buffer.putAll(expected);
            final long[] filled = new long[expected.length];
            buffer.readToLongArray(filled, 0, filled.length);
            assertArrayEquals(toLong(expected), filled);
            Arrays.fill(filled, -1);
            expected[0] = -1;
            expected[9] = -1;
            buffer.resetRead();
            buffer.readNextDouble();
            buffer.readToLongArray(filled, 1, 8);
            assertArrayEquals(toLong(expected), filled);
        }
    }

    @Test(expected = BufferUnderflowException.class)
    public void testReadToLongArrayUnderflow() throws Exception {
        try (DLDefaultByteBuffer buffer = new DLDefaultByteBuffer(10)) {
            final byte[] expected = byteRange(10);
            buffer.putAll(expected);
            final long[] filled = new long[11];
            buffer.readToLongArray(filled, 0, filled.length);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReadToLongArrayNonPositiveLength() throws Exception {
        try (DLDefaultByteBuffer buffer = new DLDefaultByteBuffer(10)) {
            final byte[] expected = byteRange(10);
            buffer.putAll(expected);
            final long[] filled = new long[10];
            buffer.readToLongArray(filled, 0, 0);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReadToLongArrayNegativePos() throws Exception {
        try (DLDefaultByteBuffer buffer = new DLDefaultByteBuffer(10)) {
            final byte[] expected = byteRange(10);
            buffer.putAll(expected);
            final long[] filled = new long[10];
            buffer.readToLongArray(filled, -1, 10);
        }
    }

    @Test
    public void testReadNextDouble() throws Exception {
        try (DLDefaultByteBuffer buffer = new DLDefaultByteBuffer(10)) {
            final byte[] expected = byteRange(10);
            buffer.putAll(expected);
            for (int i = 0; i < expected.length; i++) {
                assertEquals(expected[i], buffer.readNextDouble(), DOUBLE_EPSILON);
            }
        }
    }

    @Test(expected = BufferUnderflowException.class)
    public void testReadNextDoubleUnderflow() throws Exception {
        try (DLDefaultByteBuffer buffer = new DLDefaultByteBuffer(10)) {
            final byte[] expected = byteRange(10);
            buffer.putAll(expected);
            for (int i = 0; i < expected.length; i++) {
                assertEquals(expected[i], buffer.readNextDouble(), DOUBLE_EPSILON);
            }
            buffer.readNextDouble();
        }
    }

    @Test
    public void testReadNextByte() throws Exception {
        try (DLDefaultByteBuffer buffer = new DLDefaultByteBuffer(10)) {
            final byte[] expected = byteRange(10);
            buffer.putAll(expected);
            for (int i = 0; i < expected.length; i++) {
                assertEquals(expected[i], buffer.readNextByte());
            }
        }
    }

    @Test(expected = BufferUnderflowException.class)
    public void testReadNextByteUnderflow() throws Exception {
        try (DLDefaultByteBuffer buffer = new DLDefaultByteBuffer(10)) {
            final byte[] expected = byteRange(10);
            buffer.putAll(expected);
            for (int i = 0; i < expected.length; i++) {
                assertEquals(expected[i], buffer.readNextByte());
            }
            buffer.readNextByte();
        }
    }

    @Test
    public void testReadNextShort() throws Exception {
        try (DLDefaultByteBuffer buffer = new DLDefaultByteBuffer(10)) {
            final byte[] expected = byteRange(10);
            buffer.putAll(expected);
            for (int i = 0; i < expected.length; i++) {
                assertEquals(expected[i], buffer.readNextShort());
            }
        }
    }

    @Test(expected = BufferUnderflowException.class)
    public void testReadNextShortUnderflow() throws Exception {
        try (DLDefaultByteBuffer buffer = new DLDefaultByteBuffer(10)) {
            final byte[] expected = byteRange(10);
            buffer.putAll(expected);
            for (int i = 0; i < expected.length; i++) {
                assertEquals(expected[i], buffer.readNextShort());
            }
            buffer.readNextShort();
        }
    }

    @Test
    public void testReadNextInt() throws Exception {
        try (DLDefaultByteBuffer buffer = new DLDefaultByteBuffer(10)) {
            final byte[] expected = byteRange(10);
            buffer.putAll(expected);
            for (int i = 0; i < expected.length; i++) {
                assertEquals(expected[i], buffer.readNextInt());
            }
        }
    }

    @Test(expected = BufferUnderflowException.class)
    public void testReadNextIntUnderflow() throws Exception {
        try (DLDefaultByteBuffer buffer = new DLDefaultByteBuffer(10)) {
            final byte[] expected = byteRange(10);
            buffer.putAll(expected);
            for (int i = 0; i < expected.length; i++) {
                assertEquals(expected[i], buffer.readNextInt());
            }
            buffer.readNextInt();
        }
    }

    @Test
    public void testReadNextLong() throws Exception {
        try (DLDefaultByteBuffer buffer = new DLDefaultByteBuffer(10)) {
            final byte[] expected = byteRange(10);
            buffer.putAll(expected);
            for (int i = 0; i < expected.length; i++) {
                assertEquals(expected[i], buffer.readNextLong());
            }
        }
    }

    @Test(expected = BufferUnderflowException.class)
    public void testReadNextLongUnderflow() throws Exception {
        try (DLDefaultByteBuffer buffer = new DLDefaultByteBuffer(10)) {
            final byte[] expected = byteRange(10);
            buffer.putAll(expected);
            for (int i = 0; i < expected.length; i++) {
                assertEquals(expected[i], buffer.readNextLong());
            }
            buffer.readNextLong();
        }
    }

    @Test
    public void testZeroPad() throws Exception {
        try (DLDefaultByteBuffer buffer = new DLDefaultByteBuffer(10)) {
            buffer.putAll(byteRange(10));
            buffer.reset();
            buffer.zeroPad(10);
            final byte[] expected = new byte[10];
            assertArrayEquals(expected, buffer.m_storage);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testZeroPadNonPositiveLength() throws Exception {
        try (DLDefaultByteBuffer buffer = new DLDefaultByteBuffer(10)) {
            buffer.zeroPad(0);
        }
    }

    @Test(expected = BufferOverflowException.class)
    public void testZeroPadOverflow() throws Exception {
        try (DLDefaultByteBuffer buffer = new DLDefaultByteBuffer(10)) {
            buffer.zeroPad(11);
        }
    }

    @Test
    public void testSetStorage() throws Exception {
        try (DLDefaultByteBuffer buffer = new DLDefaultByteBuffer(10)) {
            final byte[] expected = byteRange(10);
            buffer.setStorage(expected, 10);
            assertEquals(0, buffer.m_nextRead);
            assertArrayEquals(expected, buffer.m_storage);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetStorageWrongCapacity() throws Exception {
        try (DLDefaultByteBuffer buffer = new DLDefaultByteBuffer(10)) {
            final byte[] storage = byteRange(11);
            buffer.setStorage(storage, 10);
        }
    }
}
