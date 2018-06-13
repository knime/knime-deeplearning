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
package org.knime.dl.keras.core.layers;

/**
 * @author David Kolb, KNIME GmbH, Konstanz, Germany
 */
public final class DLLayerUtils {

    private DLLayerUtils() {
        // static utility class
    }

    public static Long numberOfElements(final Long[] shape) {
        Long numElems = 1l;
        for (Long l : shape) {
            numElems *= l;
        }
        return numElems;
    }

    public static boolean isShapeFullyDefined(final Long[] shape) {
        for (Long l : shape) {
            if (l == null) {
                return false;
            }
        }
        return true;
    }

    /**
     * @param axis the axis index possibly negative similar to Python style negative indexing
     * @param rank the rank of the input tensor
     * @return the actual axis index
     * @throws IllegalArgumentException if <b>axis</b> is not compatible with rank e.g. too in magnitude
     * 
     */
    public static int getAxisIndex(int axis, int rank) {
        if (axis >= 0) {
            if (axis >= rank) {
                throw new IllegalArgumentException(
                    "The specified concatenation axis exceeds the rank of the input tensor.");
            }
            return axis;
        } else {
            int posAxis = rank + axis;
            if (posAxis < 0) {
                throw new IllegalArgumentException(
                    "The specified concatenation axis exceeds the rank of the input tensor.");
            }
            return posAxis;
        }
    }

    /**
     * Translates the index into an example shape i.e. a shape without batch dimension into an index into a shape
     * including the batch dimension.
     * 
     * @param index the index into an example shape
     * @return the corresponding index in batch shape
     */
    public static int exampleShapeIndexToBatchShapeIndex(int index) {
        return index >= 0 ? index + 1 : index;
    }
}
