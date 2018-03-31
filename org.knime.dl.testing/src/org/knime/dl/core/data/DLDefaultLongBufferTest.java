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
import static org.knime.dl.testing.DLTestUtil.alternatingBooleanArray;
import static org.knime.dl.testing.DLTestUtil.byteRange;
import static org.knime.dl.testing.DLTestUtil.intRange;
import static org.knime.dl.testing.DLTestUtil.longRange;
import static org.knime.dl.testing.DLTestUtil.shortRange;
import static org.knime.dl.testing.DLTestUtil.toLong;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.util.Arrays;

import org.junit.Test;

/**
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public class DLDefaultLongBufferTest {

	@Test
	public void testPutBoolean() throws Exception {
		try (DLDefaultLongBuffer buffer = new DLDefaultLongBuffer(10)) {
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
		try (DLDefaultLongBuffer buffer = new DLDefaultLongBuffer(1)) {
			buffer.put(true);
			buffer.put(false);
		}
	}

	@Test
	public void testPutAllBoolean() throws Exception {
		try (DLDefaultLongBuffer buffer = new DLDefaultLongBuffer(10)) {
			final boolean[] expected = alternatingBooleanArray(10);
			buffer.putAll(expected);
			assertArrayEquals(toLong(expected), buffer.m_storage);
		}
	}

	@Test(expected = BufferOverflowException.class)
	public void testPutAllBooleanOverflow() throws Exception {
		try (DLDefaultLongBuffer buffer = new DLDefaultLongBuffer(1)) {
			buffer.putAll(alternatingBooleanArray(10));
		}
	}

	@Test
	public void testPutByte() throws Exception {
		try (DLDefaultLongBuffer buffer = new DLDefaultLongBuffer(10)) {
			assertEquals(buffer.m_nextWrite, 0);
			buffer.put((byte) 1);
			assertEquals(1, buffer.m_storage[0]);
			assertEquals(1, buffer.m_nextWrite);
			buffer.put((byte) -5);
			assertEquals(-5, buffer.m_storage[1]);
		}
	}

	@Test(expected = BufferOverflowException.class)
	public void testPutByteOverflow() throws Exception {
		try (DLDefaultLongBuffer buffer = new DLDefaultLongBuffer(1)) {
			buffer.put((byte) 1);
			buffer.put((byte) 2);
		}
	}

	@Test
	public void testPutAllByte() throws Exception {
		try (DLDefaultLongBuffer buffer = new DLDefaultLongBuffer(10)) {
			final byte[] expected = byteRange(10);
			buffer.putAll(expected);
			assertArrayEquals(toLong(expected), buffer.m_storage);
		}
	}

	@Test(expected = BufferOverflowException.class)
	public void testPutAllByteOverflow() throws Exception {
		try (DLDefaultLongBuffer buffer = new DLDefaultLongBuffer(1)) {
			buffer.putAll(byteRange(10));
		}
	}

	@Test
	public void testPutShort() throws Exception {
		try (DLDefaultLongBuffer buffer = new DLDefaultLongBuffer(10)) {
			assertEquals(0, buffer.m_nextWrite);
			buffer.put((short) 1);
			assertEquals(1, buffer.m_storage[0]);
			assertEquals((short) 1, buffer.m_nextWrite);
			buffer.put((short) -5);
			assertEquals(-5, buffer.m_storage[1]);
		}
	}

	@Test(expected = BufferOverflowException.class)
	public void testPutShortOverflow() throws Exception {
		try (DLDefaultLongBuffer buffer = new DLDefaultLongBuffer(1)) {
			buffer.put((short) 1);
			buffer.put((short) 2);
		}
	}

	@Test
	public void testPutAllShort() throws Exception {
		try (DLDefaultLongBuffer buffer = new DLDefaultLongBuffer(10)) {
			final short[] expected = shortRange(10);
			buffer.putAll(expected);
			assertArrayEquals(toLong(expected), buffer.m_storage);
		}
	}

	@Test(expected = BufferOverflowException.class)
	public void testPutAllShortOverflow() throws Exception {
		try (DLDefaultLongBuffer buffer = new DLDefaultLongBuffer(1)) {
			buffer.putAll(shortRange(10));
		}
	}

	@Test
	public void testPutInt() throws Exception {
		try (DLDefaultLongBuffer buffer = new DLDefaultLongBuffer(10)) {
			assertEquals(0, buffer.m_nextWrite);
			buffer.put(1);
			assertEquals(1, buffer.m_storage[0]);
			assertEquals(1, buffer.m_nextWrite);
			buffer.put(-5);
			assertEquals(-5, buffer.m_storage[1]);
		}
	}

	@Test(expected = BufferOverflowException.class)
	public void testPutIntOverflow() throws Exception {
		try (DLDefaultLongBuffer buffer = new DLDefaultLongBuffer(1)) {
			buffer.put(1);
			buffer.put(2);
		}
	}

	@Test
	public void testPutAllInt() throws Exception {
		try (DLDefaultLongBuffer buffer = new DLDefaultLongBuffer(10)) {
			final int[] expected = intRange(10);
			buffer.putAll(expected);
			assertArrayEquals(toLong(expected), buffer.m_storage);
		}
	}

	@Test(expected = BufferOverflowException.class)
	public void testPutAllIntOverflow() throws Exception {
		try (DLDefaultLongBuffer buffer = new DLDefaultLongBuffer(1)) {
			buffer.putAll(intRange(10));
		}
	}

	@Test
	public void testPutLong() throws Exception {
		try (DLDefaultLongBuffer buffer = new DLDefaultLongBuffer(10)) {
			assertEquals(0, buffer.m_nextWrite);
			buffer.put(1L);
			assertEquals(1L, buffer.m_storage[0]);
			assertEquals(1, buffer.m_nextWrite);
			buffer.put(-5L);
			assertEquals(-5L, buffer.m_storage[1]);
		}
	}

	@Test(expected = BufferOverflowException.class)
	public void testPutLongOverflow() throws Exception {
		try (DLDefaultLongBuffer buffer = new DLDefaultLongBuffer(1)) {
			buffer.put(1L);
			buffer.put(2L);
		}
	}

	@Test
	public void testPutAllLong() throws Exception {
		try (DLDefaultLongBuffer buffer = new DLDefaultLongBuffer(10)) {
			final long[] expected = longRange(10);
			buffer.putAll(expected);
			assertArrayEquals(expected, buffer.m_storage);
		}
	}

	@Test(expected = BufferOverflowException.class)
	public void testPutAllLongOverflow() throws Exception {
		try (DLDefaultLongBuffer buffer = new DLDefaultLongBuffer(1)) {
			buffer.putAll(longRange(10));
		}
	}

	@Test
	public void testToLongArray() throws Exception {
		try (DLDefaultLongBuffer buffer = new DLDefaultLongBuffer(10)) {
			final int[] expected = intRange(10);
			buffer.putAll(expected);
			assertArrayEquals(toLong(expected), buffer.toLongArray());
		}
	}

	@Test
	public void testReadToLongArray() throws Exception {
		try (DLDefaultLongBuffer buffer = new DLDefaultLongBuffer(10)) {
			final int[] expected = intRange(10);
			buffer.putAll(expected);
			final long[] filled = new long[expected.length];
			buffer.readToLongArray(filled, 0, filled.length);
			assertArrayEquals(toLong(expected), filled);
			Arrays.fill(filled, -1);
			expected[0] = -1;
			expected[9] = -1;
			buffer.resetRead();
			buffer.readNextLong();
			buffer.readToLongArray(filled, 1, 8);
			assertArrayEquals(toLong(expected), filled);
		}
	}

	@Test(expected = BufferUnderflowException.class)
	public void testReadToLongArrayUnderflow() throws Exception {
		try (DLDefaultLongBuffer buffer = new DLDefaultLongBuffer(10)) {
			final int[] expected = intRange(10);
			buffer.putAll(expected);
			final long[] filled = new long[11];
			buffer.readToLongArray(filled, 0, filled.length);
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testReadToLongArrayNonPositiveLength() throws Exception {
		try (DLDefaultLongBuffer buffer = new DLDefaultLongBuffer(10)) {
			final int[] expected = intRange(10);
			buffer.putAll(expected);
			final long[] filled = new long[10];
			buffer.readToLongArray(filled, 0, 0);
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testReadToLongArrayNegativePos() throws Exception {
		try (DLDefaultLongBuffer buffer = new DLDefaultLongBuffer(10)) {
			final int[] expected = intRange(10);
			buffer.putAll(expected);
			final long[] filled = new long[10];
			buffer.readToLongArray(filled, -1, 10);
		}
	}

	@Test
	public void testReadNextLong() throws Exception {
		try (DLDefaultLongBuffer buffer = new DLDefaultLongBuffer(10)) {
			final int[] expected = intRange(10);
			buffer.putAll(expected);
			for (int i = 0; i < expected.length; i++) {
				assertEquals(expected[i], buffer.readNextLong());
			}
		}
	}

	@Test(expected = BufferUnderflowException.class)
	public void testReadNextLongUnderflow() throws Exception {
		try (DLDefaultLongBuffer buffer = new DLDefaultLongBuffer(10)) {
			final int[] expected = intRange(10);
			buffer.putAll(expected);
			for (int i = 0; i < expected.length; i++) {
				assertEquals(expected[i], buffer.readNextLong());
			}
			buffer.readNextLong();
		}
	}

	@Test
	public void testZeroPad() throws Exception {
		try (DLDefaultLongBuffer buffer = new DLDefaultLongBuffer(10)) {
			buffer.putAll(longRange(10));
			buffer.reset();
			buffer.zeroPad(10);
			final long[] expected = new long[10];
			assertArrayEquals(expected, buffer.m_storage);
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testZeroPadNonPositiveLength() throws Exception {
		try (DLDefaultLongBuffer buffer = new DLDefaultLongBuffer(10)) {
			buffer.zeroPad(0);
		}
	}

	@Test(expected = BufferOverflowException.class)
	public void testZeroPadOverflow() throws Exception {
		try (DLDefaultLongBuffer buffer = new DLDefaultLongBuffer(10)) {
			buffer.zeroPad(11);
		}
	}

	@Test
	public void testSetStorage() throws Exception {
		try (DLDefaultLongBuffer buffer = new DLDefaultLongBuffer(10)) {
			final long[] expected = longRange(10);
			buffer.setStorage(expected, 10);
			assertEquals(0, buffer.m_nextRead);
			assertArrayEquals(expected, buffer.m_storage);
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testSetStorageWrongCapacity() throws Exception {
		try (DLDefaultLongBuffer buffer = new DLDefaultLongBuffer(10)) {
			final long[] storage = longRange(11);
			buffer.setStorage(storage, 10);
		}
	}
}
