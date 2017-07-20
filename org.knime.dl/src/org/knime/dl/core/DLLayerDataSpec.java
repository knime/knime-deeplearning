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
 *   Apr 17, 2017 (dietzc): created
 */
package org.knime.dl.core;

import java.util.Arrays;

/**
 * The spec of {@link DLLayerData}.
 * <P>
 * Deep learning spec objects are intended to be used throughout the application and must not reference heavy data
 * objects or external resources.
 * <P>
 * Implementations of this interface must override {@link #equals(Object)} and {@link #hashCode()} in a value-based way.
 *
 * @author Christian Dietz, KNIME, Konstanz, Germany
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 */
public interface DLLayerDataSpec {

    /**
     * @return the name of the layer data
     */
    String getName();

    /**
     * Returns whether this layer data instance has a batch size assigned.
     *
     * @return true if this layer data instance has a batch size assigned
     */
    boolean hasBatchSize();

    /**
     * Returns whether this layer data instance has a shape assigned.
     *
     * @return true if this layer data instance has a shape assigned
     */
    boolean hasShape();

    /**
     * Returns the batch size of the layer data. This is an <b>optional property</b> that is not necessarily set for
     * each layer data. Check {@link #hasBatchSize()} to see if this instance has a batch size assigned.
     *
     * @return the batch size of the layer data
     */
    long getBatchSize();

    /**
     * Returns the shape of the layer data. This is an <b>optional property</b> that is not necessarily set for each
     * layer data. Check {@link #hasShape()} to see if this instance has a shape assigned. If it is present, the shape
     * is at least one-dimensional and each shape dimension is greater than zero.
     *
     * @return the shape of the layer data
     */
    long[] getShape();

    /**
     * @return the type of the layer data's elements
     */
    Class<?> getElementType();

    /**
     * Returns a string representation of the layer data shape which can be presented to the user.
     *
     * @return the string representation of the shape
     */
    default String shapeToString() {
        if (hasShape()) {
            return Arrays.toString(getShape());
        } else {
            return "no shape";
        }
    }

    /**
     * Returns a string representation of the type of the layer data's elements which can be presented to the user.
     *
     * @return the string representation of the data type
     */
    default String elementTypeToString() {
        return getElementType().getSimpleName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    int hashCode();

    /**
     * {@inheritDoc}
     */
    @Override
    boolean equals(Object obj);
}
