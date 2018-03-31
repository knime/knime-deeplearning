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

import static org.junit.Assert.assertEquals;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

import org.junit.Test;

/**
 * @author Adrian, KNIME GmbH, Konstanz, Germany
 */
public class DLAbstractWrappingBufferTest {

	@Test
	public void testSimpleGetter() throws Exception {
		try (DLDefaultDoubleBuffer buffer = new DLDefaultDoubleBuffer(10)) {
			assertEquals(10, buffer.getCapacity());
			assertEquals(0, buffer.size());
			assertEquals(0, buffer.getNextReadPosition());
			buffer.put(1);
			assertEquals(10, buffer.getCapacity());
			assertEquals(1, buffer.size());
			assertEquals(0, buffer.getNextReadPosition());
			buffer.readNextDouble();
			assertEquals(10, buffer.getCapacity());
			assertEquals(1, buffer.size());
			assertEquals(1, buffer.getNextReadPosition());
		}
	}

	@Test
	public void testGetStorageForReading() throws Exception {
		try (DLDefaultDoubleBuffer buffer = new DLDefaultDoubleBuffer(10)) {
			buffer.putAll(new double[10]);
			final double[] storage = buffer.getStorageForReading(0, 10);
			assertEquals(10, storage.length);
		}
	}

	@Test(expected = BufferUnderflowException.class)
	public void testGetStorageForReadingUnderflow() throws Exception {
		try (DLDefaultDoubleBuffer buffer = new DLDefaultDoubleBuffer(10)) {
			final double[] storage = buffer.getStorageForReading(1, 10);
		}
	}

	@Test
	public void testGetStorageForWriting() throws Exception {
		try (DLDefaultDoubleBuffer buffer = new DLDefaultDoubleBuffer(10)) {
			final double[] storage = buffer.getStorageForWriting(0, 10);
			assertEquals(10, storage.length);
		}
	}

	@Test(expected = BufferOverflowException.class)
	public void testGetStorageForWritingOverflow() throws Exception {
		try (DLDefaultDoubleBuffer buffer = new DLDefaultDoubleBuffer(10)) {
			final double[] storage = buffer.getStorageForWriting(1, 10);
		}
	}
}
