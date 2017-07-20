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
 *   May 30, 2017 (marcel): created
 */
package org.knime.dl.core;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.knime.dl.util.DLUtils.Preconditions.checkNotNullOrEmpty;

import java.util.Arrays;

import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Abstract base class for layer data spec implementations.
 *
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
public abstract class DLAbstractLayerDataSpec implements DLLayerDataSpec {

    private final String m_name;

    private final long m_batchSize;

    private final long[] m_shape;

    private final Class<?> m_dataType;

    private final int m_hashCode;

    /**
     * @param name the name of the layer data
     * @param batchSize the batch size of the layer data. Must be greater than zero.
     * @param shape the shape of the layer data. Must be at least one-dimensional. Each shape dimension must be greater
     *            than zero.
     * @param dataType the data type of the layer data's elements
     */
    protected DLAbstractLayerDataSpec(final String name, final long batchSize, final long[] shape,
        final Class<?> dataType) {
        m_name = checkNotNullOrEmpty(name);
        checkArgument(batchSize > 0, "Invalid layer data batch size. Expected value greater than 0, was %s.",
            batchSize);
        m_batchSize = batchSize;
        checkNotNull(shape);
        checkArgument(shape.length > 0, "Invalid layer data shape. Expected dimensionality greater than 0, was %s.",
            shape.length);
        for (int d = 0; d < shape.length; d++) {
            checkArgument(shape[d] > 0,
                "Invalid layer data shape. Expected shape dimension greater than 0, was %s in dimension %s.", shape[d],
                d);
        }
        m_shape = shape;
        m_dataType = checkNotNull(dataType);
        m_hashCode = hashCodeInternal();
    }

    /**
     * @param name the name of the layer data
     * @param shape the shape of the layer data. Must be at least one-dimensional. Each shape dimension must be greater
     *            than zero.
     * @param dataType the data type of the layer data's elements
     */
    protected DLAbstractLayerDataSpec(final String name, final long[] shape, final Class<?> dataType) {
        m_name = checkNotNullOrEmpty(name);
        m_batchSize = -1;
        checkNotNull(shape);
        checkArgument(shape.length > 0, "Invalid layer data shape. Expected dimensionality greater than 0, was %s.",
            shape.length);
        for (int d = 0; d < shape.length; d++) {
            checkArgument(shape[d] > 0,
                "Invalid layer data shape. Expected shape dimension greater than 0, was %s in dimension %s.", shape[d],
                d);
        }
        m_shape = shape;
        m_dataType = checkNotNull(dataType);
        m_hashCode = hashCodeInternal();
    }

    /**
     * @param name the name of the layer data
     * @param batchSize the batch size of the layer data. Must be greater than zero.
     * @param dataType the data type of the layer data's elements
     */
    protected DLAbstractLayerDataSpec(final String name, final long batchSize, final Class<?> dataType) {
        m_name = checkNotNullOrEmpty(name);
        checkArgument(batchSize > 0, "Invalid layer data batch size. Expected value greater than 0, was %s.",
            batchSize);
        m_batchSize = batchSize;
        m_shape = null;
        m_dataType = checkNotNull(dataType);
        m_hashCode = hashCodeInternal();
    }

    /**
     * @param name the name of the layer data
     * @param dataType the data type of the layer data's elements
     */
    protected DLAbstractLayerDataSpec(final String name, final Class<?> dataType) {
        m_name = checkNotNullOrEmpty(name);
        m_batchSize = -1;
        m_shape = null;
        m_dataType = checkNotNull(dataType);
        m_hashCode = hashCodeInternal();
    }

    protected abstract void hashCodeInternal(HashCodeBuilder b);

    protected abstract boolean equalsInternal(DLLayerDataSpec other);

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return m_name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasBatchSize() {
        return m_batchSize > 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasShape() {
        return m_shape != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getBatchSize() {
        return m_batchSize;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long[] getShape() {
        return m_shape;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<?> getElementType() {
        return m_dataType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return m_hashCode;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != getClass()) {
            return false;
        }
        final DLLayerDataSpec other = (DLLayerDataSpec)obj;
        return other.getName().equals(getName()) //
            && (!other.hasBatchSize() && !hasBatchSize() //
                || other.hasBatchSize() && hasBatchSize() && other.getBatchSize() == getBatchSize()) //
            && (!other.hasShape() && !hasShape() //
                || other.hasShape() && hasShape() && Arrays.equals(other.getShape(), getShape())) //
            && other.getElementType() == getElementType() //
            && equalsInternal(other);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return m_name + ": " + shapeToString() + ", " + elementTypeToString();
    }

    private int hashCodeInternal() {
        final HashCodeBuilder b = new HashCodeBuilder();
        b.append(m_name);
        if (hasBatchSize()) {
            b.append(m_batchSize);
        }
        if (hasShape()) {
            b.append(m_shape);
        }
        b.append(m_dataType);
        hashCodeInternal(b);
        return b.toHashCode();
    }
}
