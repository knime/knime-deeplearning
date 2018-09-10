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
import static org.knime.dl.testing.DLTestUtil.booleanRange;
import static org.knime.dl.testing.DLTestUtil.toByte;
import static org.knime.dl.testing.DLTestUtil.toDouble;
import static org.knime.dl.testing.DLTestUtil.toInt;
import static org.knime.dl.testing.DLTestUtil.toLong;
import static org.knime.dl.testing.DLTestUtil.toShort;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.util.Arrays;

import org.junit.Test;

/**
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("javadoc")
public class DLDefaultBitBufferTest {

    @Test
    public void testPutBoolean() throws Exception {
        try (DLDefaultBitBuffer buffer = new DLDefaultBitBuffer(10)) {
            assertEquals(buffer.m_nextWrite, 0);
            buffer.put(true);
            assertEquals(true, buffer.m_storage[0]);
            assertEquals(1, buffer.m_nextWrite);
            buffer.put(false);
            assertEquals(false, buffer.m_storage[1]);
        }
    }

    @Test(expected = BufferOverflowException.class)
    public void testPutBooleanOverflow() throws Exception {
        try (DLDefaultBitBuffer buffer = new DLDefaultBitBuffer(1)) {
            buffer.put(true);
            buffer.put(false);
        }
    }

    @Test
    public void testPutAllBoolean() throws Exception {
        try (DLDefaultBitBuffer buffer = new DLDefaultBitBuffer(10)) {
            final boolean[] expected = alternatingBooleanArray(10);
            buffer.putAll(expected);
            assertArrayEquals(expected, buffer.m_storage);
        }
    }

    @Test(expected = BufferOverflowException.class)
    public void testPutAllBooleanOverflow() throws Exception {
        try (DLDefaultBitBuffer buffer = new DLDefaultBitBuffer(1)) {
            buffer.putAll(alternatingBooleanArray(10));
        }
    }

    @Test
    public void testToDoubleArray() throws Exception {
        try (DLDefaultBitBuffer buffer = new DLDefaultBitBuffer(10)) {
            final boolean[] expected = booleanRange(10);
            buffer.putAll(expected);
            assertArrayEquals(toDouble(expected), buffer.toDoubleArray(), DOUBLE_EPSILON);
        }
    }

    @Test
    public void testToShortArray() throws Exception {
        try (DLDefaultBitBuffer buffer = new DLDefaultBitBuffer(10)) {
            final boolean[] expected = booleanRange(10);
            buffer.putAll(expected);
            assertArrayEquals(toShort(expected), buffer.toShortArray());
        }
    }

    @Test
    public void testToIntArray() throws Exception {
        try (DLDefaultBitBuffer buffer = new DLDefaultBitBuffer(10)) {
            final boolean[] expected = booleanRange(10);
            buffer.putAll(expected);
            assertArrayEquals(toInt(expected), buffer.toIntArray());
        }
    }

    @Test
    public void testToLongArray() throws Exception {
        try (DLDefaultBitBuffer buffer = new DLDefaultBitBuffer(10)) {
            final boolean[] expected = booleanRange(10);
            buffer.putAll(expected);
            assertArrayEquals(toLong(expected), buffer.toLongArray());
        }
    }

    @Test
    public void testReadToDoubleArray() throws Exception {
        try (DLDefaultBitBuffer buffer = new DLDefaultBitBuffer(10)) {
            final boolean[] expected = booleanRange(10);
            buffer.putAll(expected);
            final double[] filled = new double[expected.length];
            buffer.readToDoubleArray(filled, 0, filled.length);
            assertArrayEquals(toDouble(expected), filled, DOUBLE_EPSILON);
            Arrays.fill(filled, 1);
            expected[0] = true;
            expected[9] = true;
            buffer.resetRead();
            buffer.readNextDouble();
            buffer.readToDoubleArray(filled, 1, 8);
            assertArrayEquals(toDouble(expected), filled, DOUBLE_EPSILON);
        }
    }

    @Test(expected = BufferUnderflowException.class)
    public void testReadToDoubleArrayUnderflow() throws Exception {
        try (DLDefaultBitBuffer buffer = new DLDefaultBitBuffer(10)) {
            final boolean[] expected = booleanRange(10);
            buffer.putAll(expected);
            final double[] filled = new double[11];
            buffer.readToDoubleArray(filled, 0, filled.length);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReadToDoubleArrayNonPositiveLength() throws Exception {
        try (DLDefaultBitBuffer buffer = new DLDefaultBitBuffer(10)) {
            final boolean[] expected = booleanRange(10);
            buffer.putAll(expected);
            final double[] filled = new double[10];
            buffer.readToDoubleArray(filled, 0, 0);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReadToDoubleArrayNegativePos() throws Exception {
        try (DLDefaultBitBuffer buffer = new DLDefaultBitBuffer(10)) {
            final boolean[] expected = booleanRange(10);
            buffer.putAll(expected);
            final double[] filled = new double[10];
            buffer.readToDoubleArray(filled, -1, 10);
        }
    }

    @Test
    public void testReadToBitArray() throws Exception {
        try (DLDefaultBitBuffer buffer = new DLDefaultBitBuffer(10)) {
            final boolean[] expected = booleanRange(10);
            buffer.putAll(expected);
            final boolean[] filled = new boolean[expected.length];
            buffer.readToBitArray(filled, 0, filled.length);
            assertArrayEquals(expected, filled);
            Arrays.fill(filled, true);
            expected[0] = true;
            expected[9] = true;
            buffer.resetRead();
            buffer.readNextDouble();
            buffer.readToBitArray(filled, 1, 8);
            assertArrayEquals(expected, filled);
        }
    }

    @Test(expected = BufferUnderflowException.class)
    public void testReadToBitArrayUnderflow() throws Exception {
        try (DLDefaultBitBuffer buffer = new DLDefaultBitBuffer(10)) {
            final boolean[] expected = booleanRange(10);
            buffer.putAll(expected);
            final boolean[] filled = new boolean[11];
            buffer.readToBitArray(filled, 0, filled.length);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReadToBitArrayNonPositiveLength() throws Exception {
        try (DLDefaultBitBuffer buffer = new DLDefaultBitBuffer(10)) {
            final boolean[] expected = booleanRange(10);
            buffer.putAll(expected);
            final boolean[] filled = new boolean[10];
            buffer.readToBitArray(filled, 0, 0);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReadToBitArrayNegativePos() throws Exception {
        try (DLDefaultBitBuffer buffer = new DLDefaultBitBuffer(10)) {
            final boolean[] expected = booleanRange(10);
            buffer.putAll(expected);
            final boolean[] filled = new boolean[10];
            buffer.readToBitArray(filled, -1, 10);
        }
    }

    @Test
    public void testReadToByteArray() throws Exception {
        try (DLDefaultBitBuffer buffer = new DLDefaultBitBuffer(10)) {
            final boolean[] expected = booleanRange(10);
            buffer.putAll(expected);
            final byte[] filled = new byte[expected.length];
            buffer.readToByteArray(filled, 0, filled.length);
            assertArrayEquals(toByte(expected), filled);
            Arrays.fill(filled, (byte)1);
            expected[0] = true;
            expected[9] = true;
            buffer.resetRead();
            buffer.readNextDouble();
            buffer.readToByteArray(filled, 1, 8);
            assertArrayEquals(toByte(expected), filled);
        }
    }

    @Test(expected = BufferUnderflowException.class)
    public void testReadToByteArrayUnderflow() throws Exception {
        try (DLDefaultBitBuffer buffer = new DLDefaultBitBuffer(10)) {
            final boolean[] expected = booleanRange(10);
            buffer.putAll(expected);
            final byte[] filled = new byte[11];
            buffer.readToByteArray(filled, 0, filled.length);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReadToByteArrayNonPositiveLength() throws Exception {
        try (DLDefaultBitBuffer buffer = new DLDefaultBitBuffer(10)) {
            final boolean[] expected = booleanRange(10);
            buffer.putAll(expected);
            final byte[] filled = new byte[10];
            buffer.readToByteArray(filled, 0, 0);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReadToByteArrayNegativePos() throws Exception {
        try (DLDefaultBitBuffer buffer = new DLDefaultBitBuffer(10)) {
            final boolean[] expected = booleanRange(10);
            buffer.putAll(expected);
            final byte[] filled = new byte[10];
            buffer.readToByteArray(filled, -1, 10);
        }
    }

    @Test
    public void testReadToShortArray() throws Exception {
        try (DLDefaultBitBuffer buffer = new DLDefaultBitBuffer(10)) {
            final boolean[] expected = booleanRange(10);
            buffer.putAll(expected);
            final short[] filled = new short[expected.length];
            buffer.readToShortArray(filled, 0, filled.length);
            assertArrayEquals(toShort(expected), filled);
            Arrays.fill(filled, (short)1);
            expected[0] = true;
            expected[9] = true;
            buffer.resetRead();
            buffer.readNextDouble();
            buffer.readToShortArray(filled, 1, 8);
            assertArrayEquals(toShort(expected), filled);
        }
    }

    @Test(expected = BufferUnderflowException.class)
    public void testReadToShortArrayUnderflow() throws Exception {
        try (DLDefaultBitBuffer buffer = new DLDefaultBitBuffer(10)) {
            final boolean[] expected = booleanRange(10);
            buffer.putAll(expected);
            final short[] filled = new short[11];
            buffer.readToShortArray(filled, 0, filled.length);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReadToShortArrayNonPositiveLength() throws Exception {
        try (DLDefaultBitBuffer buffer = new DLDefaultBitBuffer(10)) {
            final boolean[] expected = booleanRange(10);
            buffer.putAll(expected);
            final short[] filled = new short[10];
            buffer.readToShortArray(filled, 0, 0);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReadToShortArrayNegativePos() throws Exception {
        try (DLDefaultBitBuffer buffer = new DLDefaultBitBuffer(10)) {
            final boolean[] expected = booleanRange(10);
            buffer.putAll(expected);
            final short[] filled = new short[10];
            buffer.readToShortArray(filled, -1, 10);
        }
    }

    @Test
    public void testReadToIntArray() throws Exception {
        try (DLDefaultBitBuffer buffer = new DLDefaultBitBuffer(10)) {
            final boolean[] expected = booleanRange(10);
            buffer.putAll(expected);
            final int[] filled = new int[expected.length];
            buffer.readToIntArray(filled, 0, filled.length);
            assertArrayEquals(toInt(expected), filled);
            Arrays.fill(filled, 1);
            expected[0] = true;
            expected[9] = true;
            buffer.resetRead();
            buffer.readNextDouble();
            buffer.readToIntArray(filled, 1, 8);
            assertArrayEquals(toInt(expected), filled);
        }
    }

    @Test(expected = BufferUnderflowException.class)
    public void testReadToIntArrayUnderflow() throws Exception {
        try (DLDefaultBitBuffer buffer = new DLDefaultBitBuffer(10)) {
            final boolean[] expected = booleanRange(10);
            buffer.putAll(expected);
            final int[] filled = new int[11];
            buffer.readToIntArray(filled, 0, filled.length);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReadToIntArrayNonPositiveLength() throws Exception {
        try (DLDefaultBitBuffer buffer = new DLDefaultBitBuffer(10)) {
            final boolean[] expected = booleanRange(10);
            buffer.putAll(expected);
            final int[] filled = new int[10];
            buffer.readToIntArray(filled, 0, 0);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReadToIntArrayNegativePos() throws Exception {
        try (DLDefaultBitBuffer buffer = new DLDefaultBitBuffer(10)) {
            final boolean[] expected = booleanRange(10);
            buffer.putAll(expected);
            final int[] filled = new int[10];
            buffer.readToIntArray(filled, -1, 10);
        }
    }

    @Test
    public void testReadToLongArray() throws Exception {
        try (DLDefaultBitBuffer buffer = new DLDefaultBitBuffer(10)) {
            final boolean[] expected = booleanRange(10);
            buffer.putAll(expected);
            final long[] filled = new long[expected.length];
            buffer.readToLongArray(filled, 0, filled.length);
            assertArrayEquals(toLong(expected), filled);
            Arrays.fill(filled, 1);
            expected[0] = true;
            expected[9] = true;
            buffer.resetRead();
            buffer.readNextDouble();
            buffer.readToLongArray(filled, 1, 8);
            assertArrayEquals(toLong(expected), filled);
        }
    }

    @Test(expected = BufferUnderflowException.class)
    public void testReadToLongArrayUnderflow() throws Exception {
        try (DLDefaultBitBuffer buffer = new DLDefaultBitBuffer(10)) {
            final boolean[] expected = booleanRange(10);
            buffer.putAll(expected);
            final long[] filled = new long[11];
            buffer.readToLongArray(filled, 0, filled.length);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReadToLongArrayNonPositiveLength() throws Exception {
        try (DLDefaultBitBuffer buffer = new DLDefaultBitBuffer(10)) {
            final boolean[] expected = booleanRange(10);
            buffer.putAll(expected);
            final long[] filled = new long[10];
            buffer.readToLongArray(filled, 0, 0);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReadToLongArrayNegativePos() throws Exception {
        try (DLDefaultBitBuffer buffer = new DLDefaultBitBuffer(10)) {
            final boolean[] expected = booleanRange(10);
            buffer.putAll(expected);
            final long[] filled = new long[10];
            buffer.readToLongArray(filled, -1, 10);
        }
    }

    @Test
    public void testReadNextDouble() throws Exception {
        try (DLDefaultBitBuffer buffer = new DLDefaultBitBuffer(10)) {
            final boolean[] expected = booleanRange(10);
            buffer.putAll(expected);
            for (int i = 0; i < expected.length; i++) {
                assertEquals(toDouble(expected)[i], buffer.readNextDouble(), DOUBLE_EPSILON);
            }
        }
    }

    @Test(expected = BufferUnderflowException.class)
    public void testReadNextDoubleUnderflow() throws Exception {
        try (DLDefaultBitBuffer buffer = new DLDefaultBitBuffer(10)) {
            final boolean[] expected = booleanRange(10);
            buffer.putAll(expected);
            for (int i = 0; i < expected.length; i++) {
                assertEquals(toDouble(expected)[i], buffer.readNextDouble(), DOUBLE_EPSILON);
            }
            buffer.readNextDouble();
        }
    }

    @Test
    public void testReadNextBit() throws Exception {
        try (DLDefaultBitBuffer buffer = new DLDefaultBitBuffer(10)) {
            final boolean[] expected = booleanRange(10);
            buffer.putAll(expected);
            for (int i = 0; i < expected.length; i++) {
                assertEquals(expected[i], buffer.readNextBit());
            }
        }
    }

    @Test(expected = BufferUnderflowException.class)
    public void testReadNextBitUnderflow() throws Exception {
        try (DLDefaultBitBuffer buffer = new DLDefaultBitBuffer(10)) {
            final boolean[] expected = booleanRange(10);
            buffer.putAll(expected);
            for (int i = 0; i < expected.length; i++) {
                assertEquals(expected[i], buffer.readNextBit());
            }
            buffer.readNextBit();
        }
    }

    @Test
    public void testReadNextByte() throws Exception {
        try (DLDefaultBitBuffer buffer = new DLDefaultBitBuffer(10)) {
            final boolean[] expected = booleanRange(10);
            buffer.putAll(expected);
            for (int i = 0; i < expected.length; i++) {
                assertEquals(toByte(expected)[i], buffer.readNextByte());
            }
        }
    }

    @Test(expected = BufferUnderflowException.class)
    public void testReadNextByteUnderflow() throws Exception {
        try (DLDefaultBitBuffer buffer = new DLDefaultBitBuffer(10)) {
            final boolean[] expected = booleanRange(10);
            buffer.putAll(expected);
            for (int i = 0; i < expected.length; i++) {
                assertEquals(toByte(expected)[i], buffer.readNextByte());
            }
            buffer.readNextByte();
        }
    }

    @Test
    public void testReadNextShort() throws Exception {
        try (DLDefaultBitBuffer buffer = new DLDefaultBitBuffer(10)) {
            final boolean[] expected = booleanRange(10);
            buffer.putAll(expected);
            for (int i = 0; i < expected.length; i++) {
                assertEquals(toShort(expected)[i], buffer.readNextShort());
            }
        }
    }

    @Test(expected = BufferUnderflowException.class)
    public void testReadNextShortUnderflow() throws Exception {
        try (DLDefaultBitBuffer buffer = new DLDefaultBitBuffer(10)) {
            final boolean[] expected = booleanRange(10);
            buffer.putAll(expected);
            for (int i = 0; i < expected.length; i++) {
                assertEquals(toShort(expected)[i], buffer.readNextShort());
            }
            buffer.readNextShort();
        }
    }

    @Test
    public void testReadNextInt() throws Exception {
        try (DLDefaultBitBuffer buffer = new DLDefaultBitBuffer(10)) {
            final boolean[] expected = booleanRange(10);
            buffer.putAll(expected);
            for (int i = 0; i < expected.length; i++) {
                assertEquals(toInt(expected)[i], buffer.readNextInt());
            }
        }
    }

    @Test(expected = BufferUnderflowException.class)
    public void testReadNextIntUnderflow() throws Exception {
        try (DLDefaultBitBuffer buffer = new DLDefaultBitBuffer(10)) {
            final boolean[] expected = booleanRange(10);
            buffer.putAll(expected);
            for (int i = 0; i < expected.length; i++) {
                assertEquals(toInt(expected)[i], buffer.readNextInt());
            }
            buffer.readNextInt();
        }
    }

    @Test
    public void testReadNextLong() throws Exception {
        try (DLDefaultBitBuffer buffer = new DLDefaultBitBuffer(10)) {
            final boolean[] expected = booleanRange(10);
            buffer.putAll(expected);
            for (int i = 0; i < expected.length; i++) {
                assertEquals(toLong(expected)[i], buffer.readNextLong());
            }
        }
    }

    @Test(expected = BufferUnderflowException.class)
    public void testReadNextLongUnderflow() throws Exception {
        try (DLDefaultBitBuffer buffer = new DLDefaultBitBuffer(10)) {
            final boolean[] expected = booleanRange(10);
            buffer.putAll(expected);
            for (int i = 0; i < expected.length; i++) {
                assertEquals(toLong(expected)[i], buffer.readNextLong());
            }
            buffer.readNextLong();
        }
    }

    @Test
    public void testZeroPad() throws Exception {
        try (DLDefaultBitBuffer buffer = new DLDefaultBitBuffer(10)) {
            buffer.putAll(booleanRange(10));
            buffer.reset();
            buffer.zeroPad(10);
            final boolean[] expected = new boolean[10];
            assertArrayEquals(expected, buffer.m_storage);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testZeroPadNonPositiveLength() throws Exception {
        try (DLDefaultBitBuffer buffer = new DLDefaultBitBuffer(10)) {
            buffer.zeroPad(0);
        }
    }

    @Test(expected = BufferOverflowException.class)
    public void testZeroPadOverflow() throws Exception {
        try (DLDefaultBitBuffer buffer = new DLDefaultBitBuffer(10)) {
            buffer.zeroPad(11);
        }
    }

    @Test
    public void testSetStorage() throws Exception {
        try (DLDefaultBitBuffer buffer = new DLDefaultBitBuffer(10)) {
            final boolean[] expected = booleanRange(10);
            buffer.setStorage(expected, 10);
            assertEquals(0, buffer.m_nextRead);
            assertArrayEquals(expected, buffer.m_storage);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetStorageWrongCapacity() throws Exception {
        try (DLDefaultBitBuffer buffer = new DLDefaultBitBuffer(10)) {
            final boolean[] storage = booleanRange(11);
            buffer.setStorage(storage, 10);
        }
    }
}
