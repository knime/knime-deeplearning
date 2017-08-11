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
 *   Jul 21, 2017 (marcel): created
 */
package org.knime.dl.core;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Arrays;

import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Abstract base class for {@link DLFixedLayerDataShape fixed-size layer data shapes}.
 *
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
public abstract class DLAbstractFixedLayerDataShape extends DLAbstractLayerDataShape implements DLFixedLayerDataShape {

	private static final long serialVersionUID = 1L;
	
    private final long[] m_shape;

    /**
     * @param shape the shape of the layer data. Must be at least one-dimensional. Each shape dimension must be greater
     *            than zero.
     */
    protected DLAbstractFixedLayerDataShape(final long[] shape) {
        checkNotNull(shape);
        checkArgument(shape.length > 0, "Invalid layer data shape. Expected dimensionality greater than 0, was %s.",
            shape.length);
        for (int d = 0; d < shape.length; d++) {
            checkArgument(shape[d] > 0,
                "Invalid layer data shape. Expected shape dimension greater than 0, was %s in dimension %s.", shape[d],
                d);
        }
        m_shape = shape.clone();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNumDimensions() {
        return m_shape.length;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long[] getShape() {
        return m_shape;
    }

    @Override
    public String toString() {
    	return Arrays.toString(getShape());
    }

    @Override
    protected void hashCodeInternal(final HashCodeBuilder b) {
		b.append(getShape());
    }

    @Override
    protected boolean equalsInternal(final DLLayerDataShape other) {
		return Arrays.equals(((DLFixedLayerDataShape) other).getShape(), getShape());
    }
}
