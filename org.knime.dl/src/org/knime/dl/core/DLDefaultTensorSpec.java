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
 *   May 8, 2017 (marcel): created
 */
package org.knime.dl.core;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.knime.dl.util.DLUtils;

/**
 * Default implementation of a {@link DLTensorSpec tensor spec}.
 *
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public final class DLDefaultTensorSpec extends DLAbstractTensorSpec {

    /**
     * Creates a new instance of this tensor spec. This is a convenience method that maps to an appropriate constructor
     * of {@link DLDefaultTensorSpec} based on the passed arguments (see below).
     *
     * @param identifier the identifier of the tensor
     * @param name the name of the tensor
     * @param batchSize the batch size of the tensor. May be <code>null</code>. Must be greater than zero if non-null.
     * @param shape the shape of the tensor. Does not include the batch size. May be <code>null</code> or empty in which
     *            case it is interpreted as {@link DLUnknownTensorShape unknown shape}. May contain <code>null</code>
     *            elements which are interpreted as {@link DLPartialTensorShape unknown dimensions}.
     * @param elementType the data type of the tensor's elements
     * @param dimensionOrder the dimension order this tensor expects, e.g. TDHWC
     * @return the created spec
     */
    public static final DLDefaultTensorSpec create(final DLTensorId identifier, final String name, final Long batchSize,
        final Long[] shape, final Class<?> elementType, final DLDimensionOrder dimensionOrder) {
        final DLTensorShape matchingShape = DLUtils.Shapes.shapeFromLongArray(shape);
        if (batchSize != null) {
            return new DLDefaultTensorSpec(identifier, name, batchSize, matchingShape, elementType, dimensionOrder);
        } else {
            return new DLDefaultTensorSpec(identifier, name, matchingShape, elementType, dimensionOrder);
        }
    }

    private static final long serialVersionUID = 1L;

    /**
     * Creates a new instance of this tensor spec that has a batch size.
     *
     * @param identifier the identifier of the tensor
     * @param name the name of the tensor
     * @param batchSize the batch size of the tensor. Must be greater than zero.
     * @param shape the shape of the tensor. Does not include the batch size.
     * @param elementType the data type of the tensor's elements
     * @param dimensionOrder the dimension order this tensor expects, e.g. TDHWC
     */
    public DLDefaultTensorSpec(final DLTensorId identifier, final String name, final long batchSize,
        final DLTensorShape shape, final Class<?> elementType, final DLDimensionOrder dimensionOrder) {
        super(identifier, name, batchSize, shape, elementType, dimensionOrder);
    }

    /**
     * Creates a new instance of this tensor spec that does not have a batch size.
     *
     * @param identifier the identifier of the tensor
     * @param name the name of the tensor
     * @param shape the shape of the tensor. Does not include the batch size.
     * @param elementType the data type of the tensor's elements
     * @param dimensionOrder the dimension order this tensor expects, e.g. TDHWC
     */
    public DLDefaultTensorSpec(final DLTensorId identifier, final String name, final DLTensorShape shape,
        final Class<?> elementType, final DLDimensionOrder dimensionOrder) {
        super(identifier, name, shape, elementType, dimensionOrder);
    }

    /**
     * Creates a new instance of tensor spec that does not have a shape.
     *
     * @param identifier the identifier of the tensor
     * @param name the name of the tensor
     * @param batchSize the batch size of the tensor. Must be greater than zero.
     * @param elementType the data type of the tensor's elements
     * @param dimensionOrder the dimension order this tensor expects, e.g. TDHWC
     */
    public DLDefaultTensorSpec(final DLTensorId identifier, final String name, final long batchSize,
        final Class<?> elementType, final DLDimensionOrder dimensionOrder) {
        super(identifier, name, batchSize, elementType, dimensionOrder);
    }

    /**
     * Creates a new instance of tensor spec that does not have a batch size or a shape.
     *
     * @param identifier the identifier of the tensor
     * @param name the name of the tensor
     * @param elementType the data type of the tensor's elements
     * @param dimensionOrder the dimension order this tensor expects e.g. TDHWC
     */
    public DLDefaultTensorSpec(final DLTensorId identifier, final String name, final Class<?> elementType,
        final DLDimensionOrder dimensionOrder) {
        super(identifier, name, elementType, dimensionOrder);
    }

    @Override
    protected void hashCodeInternal(final HashCodeBuilder b) {
        // no op - everything's handled in abstract base class
    }

    @Override
    protected boolean equalsInternal(final DLTensorSpec other) {
        // no op - everything's handled in abstract base class
        return true;
    }
}
