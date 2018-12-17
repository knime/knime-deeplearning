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
import java.util.function.IntFunction;

/**
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany, KNIME GmbH, Konstanz, Germany
 * @param <T> Type of object stored in this buffer
 */
public abstract class DLAbstractObjectBuffer<T> extends DLAbstractFlatWrappingDataBuffer<T[]>
    implements DLWritableObjectBuffer<T>, DLReadableObjectBuffer<T> {
        
        private final T m_zeroValue;

    /**
     * @param capacity
     * @param storageCreator function that creates storage array e.g. String[]::new
     * @param zeroValue 
     */
    public DLAbstractObjectBuffer(final long capacity, final T zeroValue) {
        super(capacity);
        m_zeroValue = zeroValue;
    }

    @Override
    public void zeroPad(long length) throws IllegalArgumentException, BufferOverflowException {
        checkArgument(length > 0);
        checkOverflow(m_nextWrite + length <= m_capacity);
        for (int i = 0; i < length; i++) {
            m_storage[m_nextWrite++] = m_zeroValue;
        }
    }

    @Override
    public void setStorage(T[] storage, long storageSize) throws IllegalArgumentException {
        checkArgument(storage.length == m_capacity, "Input storage capacity does not match buffer capacity.");
        m_storage = storage;
        m_nextWrite = (int)storageSize;
        resetRead();
    }

    @Override
    public T readNext() {
        checkUnderflow(m_nextRead < m_nextWrite);
        return m_storage[m_nextRead++];
    }

    @Override
    public void put(T value) {
        checkOverflow(m_nextWrite < m_capacity);
        m_storage[m_nextWrite++] = value;
    }

    @Override
    public void putAll(T[] values) {
        checkOverflow(m_nextWrite + values.length <= m_capacity);
        for (int i = 0; i < values.length; i++) {
            m_storage[m_nextWrite++] = values[i];
        }
    }

}
