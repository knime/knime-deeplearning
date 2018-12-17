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

import static org.junit.Assert.*;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.util.stream.IntStream;

import org.junit.Test;

/**
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany, KNIME GmbH, Konstanz, Germany
 */
public class DLAbstractObjectBufferTest {
    private static class ObjectBuffer extends DLAbstractObjectBuffer<Object> {
        
        private static final Object ZERO = new Object();

        /**
         * @param capacity
         * @param zeroValue
         */
        public ObjectBuffer(long capacity) {
            super(capacity, ZERO);
        }

        @Override
        protected Object[] createStorage() {
            return new Object[m_capacity];
        }
        
    }
    
    @Test (expected = IllegalArgumentException.class)
    public void testZeroPadFailOnNonPositiveLength() throws Exception {
        ObjectBuffer buffer = new ObjectBuffer(2);
        buffer.zeroPad(0);
    }
    
    @Test (expected = BufferOverflowException.class)
    public void testZeroPadLengthToLong() throws Exception {
        ObjectBuffer buffer = new ObjectBuffer(2);
        buffer.zeroPad(3);
    }
    
    @Test
    public void testZeroPad() throws Exception {
        ObjectBuffer buffer = new ObjectBuffer(5);
        buffer.zeroPad(3);
        assertEquals(3, buffer.size());
        for (int i = 0; i < 3; i++) {
            assertEquals(ObjectBuffer.ZERO, buffer.m_storage[i]);
        }
    }
    
    @Test (expected = IllegalArgumentException.class)
    public void testSetStorageIncompatibleCapacity() throws Exception {
        ObjectBuffer buffer = new ObjectBuffer(3);
        Object[] newStorage = new Object[4];
        buffer.setStorage(newStorage, 0);
    }
    
    @Test
    public void testSetStorage() throws Exception {
        ObjectBuffer buffer = new ObjectBuffer(3);
        Object[] newStorage = new Object[3];
        buffer.setStorage(newStorage, 2);
        assertEquals(2, buffer.size());
    }
    
    @Test (expected = BufferUnderflowException.class)
    public void testReadNextUnderFlow() throws Exception {
        ObjectBuffer buffer = new ObjectBuffer(3);
        buffer.readNext();
    }
    
    @Test
    public void testReadWrite() throws Exception {
        ObjectBuffer buffer = new ObjectBuffer(3);
        Object obj = new Object();
        buffer.put(obj);
        assertEquals(0L, buffer.getNextReadPosition());
        assertEquals(1L, buffer.size());
        Object readObj = buffer.readNext();
        assertEquals(obj, readObj);
        assertEquals(1L, buffer.getNextReadPosition());
    }
    
    @Test (expected = BufferOverflowException.class)
    public void testPutOverflow() throws Exception {
        ObjectBuffer buffer = new ObjectBuffer(1);
        buffer.put(new Object());
        buffer.put(new Object());
    }
    
    @Test (expected = BufferOverflowException.class)
    public void testPutAllOverflow() throws Exception {
        ObjectBuffer buffer = new ObjectBuffer(3);
        buffer.putAll(new Object[4]);
    }
    
    @Test
    public void testPutAll() throws Exception {
        ObjectBuffer buffer = new ObjectBuffer(4);
        Object[] objects = IntStream.range(0, 2).mapToObj(i -> new Object()).toArray();
        buffer.putAll(objects);
        for (int i = 0; i < objects.length; i++) {
            assertEquals(objects[i], buffer.m_storage[i]);
        }
    }

}
