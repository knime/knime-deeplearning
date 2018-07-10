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

import java.util.Set;

import org.knime.core.node.InvalidSettingsException;
import org.knime.dl.keras.core.config.DLKerasConfigObject;

import com.google.common.collect.ImmutableSet;

/**
 * @author David Kolb, KNIME GmbH, Konstanz, Germany
 */
public final class DLLayerUtils {

    public static final Set<Class<?>> FLOATING_POINT_DTYPES = ImmutableSet.of(float.class, double.class);

    public static final Set<Class<?>> NUMERICAL_DTYPES =
        ImmutableSet.of(float.class, double.class, long.class, byte.class, int.class, short.class);

    public static final Set<Class<?>> ALL_DTYPES =
        ImmutableSet.of(float.class, double.class, long.class, byte.class, int.class, short.class, boolean.class);

    private DLLayerUtils() {
        // static utility class
    }
    
    /**
     * Calculated the total number of elements of the specified shape.
     * 
     * @param shape the shape
     * @return the total number of elements
     */
    public static Long numberOfElements(final Long[] shape) {
        if (shape.length == 0) {
            return 0L;
        }
        Long numElems = 1l;
        for (Long l : shape) {
            numElems *= l;
        }
        return numElems;
    }
    
    /**
     * Check whether the specified shape is fully defined or not. 
     * 
     * @param shape 
     * @return true if the specified shape is fully defined, false if not
     */
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
     * @param rank the rank of the input tensor (excluding the batch dimension)
     * @return the actual axis index (into the tensor without the batch dimension)
     * @throws IllegalArgumentException if <b>axis</b> is not compatible with rank e.g. too in magnitude
     * 
     */
    public static int getAxisIndex(int axis, int rank) {
        int index;
        int batchRank = rank + 1;
        if (axis >= 0) {
            if (axis >= batchRank) {
                throw new IllegalArgumentException(
                    "The specified concatenation axis exceeds the rank of the input tensor.");
            }
            index = axis;
        } else {
            int posAxis = batchRank + axis;
            if (posAxis < 0) {
                throw new IllegalArgumentException(
                    "The specified concatenation axis exceeds the rank of the input tensor.");
            }
            index = posAxis;
        }
        if (index == 0) {
            throw new IllegalArgumentException(
                "The specified index corresponds to the batch dimension. This is not supported.");
        }
        return index - 1;
    }

    /**
     * Validates the configuration for a {@link DLKerasConfigObject} provided it is not {@code null}. This includes
     * objects such as: initializers, regularizers, and constraints.
     *
     * @param parameter the {@link DLKerasConfigObject} to validate
     * @throws InvalidSettingsException if {@code parameter} has invalid settings
     */
    public static void validateOptionalParameter(final DLKerasConfigObject parameter) throws InvalidSettingsException {
        if (parameter == null)
            return;
        parameter.validateParameters();
    }

}
