/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 * History
 *   Jun 28, 2017 (marcel): created
 */
package org.knime.dl.python.core.data;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.util.Arrays;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.dl.core.data.DLReadableDoubleBuffer;
import org.knime.dl.core.data.writables.DLWritableDoubleBuffer;

/**
 *
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
@SuppressWarnings("serial")
public class DLPythonDoubleBuffer extends AbstractDLPythonDataDataBuffer<double[]>
    implements DLWritableDoubleBuffer, DLReadableDoubleBuffer {

    public static final DataType TYPE = DataType.getType(DLPythonDoubleBuffer.class);

    private int m_nextWrite = 0;

    private int m_nextRead = 0;

    public DLPythonDoubleBuffer(long capacity) {
        super(capacity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long size() {
        return m_storage.length;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double readNextDouble() {
        checkState(m_nextRead < size(), "Buffer size exceeded.");
        return getStorage()[m_nextRead++];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(final double value) {
        checkState(m_nextWrite < getCapacity(), "Buffer capacity exceeded.");
        getStorage()[m_nextWrite++] = value;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putAll(final double[] values) {
        checkArgument(m_nextWrite + values.length <= getCapacity(), "Input array exceeds buffer capacity.");
        System.arraycopy(values, 0, getStorage(), m_nextWrite, values.length);
        m_nextWrite += values.length;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putStorage(final double[] storage) {
        checkArgument(storage.length <= getCapacity(), "Input storage exceeds buffer capacity.");
        if (storage.length == getCapacity()) {
            setValidatedStorage(storage);
            m_nextWrite = storage.length;
        } else {
            putAll(storage);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void resetWrite() {
        m_nextWrite = 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean equalsDataCell(final DataCell dc) {
        return Arrays.equals(m_storage, ((DLPythonDoubleBuffer)dc).m_storage);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return m_storage.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected double[] createStorage() {
        return new double[getCapacity()];
    }

    @Override
    public double[] toDoubleArray() {
        return getStorage().clone();
    }

    @Override
    public void resetRead() {
        m_nextRead = 0;
    }

    @Override
    public void put(final float value) {
        checkState(m_nextWrite < getCapacity(), "Buffer capacity exceeded.");
        getStorage()[m_nextWrite++] = value;
    }

    @Override
    public void putAll(final float[] values) {
        checkArgument(m_nextWrite + values.length <= getCapacity(), "Input array exceeds buffer capacity.");
        for (int i = 0; i < values.length; i++) {
            getStorage()[m_nextWrite++] = values[i];
        }
        m_nextWrite += values.length;
    }

    @Override
    public void put(final int value) {
        checkState(m_nextWrite < getCapacity(), "Buffer capacity exceeded.");
        getStorage()[m_nextWrite++] = value;
    }

    @Override
    public void putAll(final int[] values) {
        checkArgument(m_nextWrite + values.length <= getCapacity(), "Input array exceeds buffer capacity.");
        for (int i = 0; i < values.length; i++) {
            getStorage()[m_nextWrite++] = values[i];
        }
        m_nextWrite += values.length;
    }

    @Override
    public void put(final short value) {
        checkState(m_nextWrite < getCapacity(), "Buffer capacity exceeded.");
        getStorage()[m_nextWrite++] = value;
    }

    @Override
    public void putAll(final short[] values) {
        checkArgument(m_nextWrite + values.length <= getCapacity(), "Input array exceeds buffer capacity.");
        for (int i = 0; i < values.length; i++) {
            getStorage()[m_nextWrite++] = values[i];
        }
        m_nextWrite += values.length;
    }

    @Override
    public void put(final byte value) {
        checkState(m_nextWrite < getCapacity(), "Buffer capacity exceeded.");
        getStorage()[m_nextWrite++] = value;
    }

    @Override
    public void putAll(final byte[] values) {
        checkArgument(m_nextWrite + values.length <= getCapacity(), "Input array exceeds buffer capacity.");
        for (int i = 0; i < values.length; i++) {
            getStorage()[m_nextWrite++] = values[i];
        }
        m_nextWrite += values.length;
    }

    @Override
    public void put(final boolean value) {
        checkState(m_nextWrite < getCapacity(), "Buffer capacity exceeded.");
        getStorage()[m_nextWrite++] = value ? 1d : 0d;
    }

    @Override
    public void putAll(final boolean[] values) {
        checkArgument(m_nextWrite + values.length <= getCapacity(), "Input array exceeds buffer capacity.");
        for (int i = 0; i < values.length; i++) {
            getStorage()[m_nextWrite++] = values[i] ? 1d : 0d;
        }
        m_nextWrite += values.length;
    }
}