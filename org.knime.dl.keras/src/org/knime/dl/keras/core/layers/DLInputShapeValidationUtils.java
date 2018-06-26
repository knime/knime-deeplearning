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
public final class DLInputShapeValidationUtils {

    private DLInputShapeValidationUtils() {
        // static utility class
    }

    /**
     * Checks if the specified shape has exactly the specified number of dimensions. Throws exception if not.
     *
     * @param inputShape the shape of the input
     * @param expectedNumberOfDims the expected dimensionality
     * @throws DLInvalidTensorSpecException if the input doesn't have the expected dimensionality
     */
    public static void dimsExactly(final Long[] inputShape, final int expectedNumberOfDims)
        throws DLInvalidTensorSpecException {
        checkPredicate(inputShape.length == expectedNumberOfDims, "The input must be " + expectedNumberOfDims
            + "-dimensional, but was " + inputShape.length + "-dimensional.");
    }
    
    /**
     * Checks if the specified shapes have exactly the same number of dimensions. Throws exception if not.
     *
     * @param inputShape1 the shape of the first input
     * @param inputShape2 the shape of the second input
     * @throws DLInvalidTensorSpecException if the input doesn't have the expected dimensionality
     */
    public static void dimsAgree(final Long[] inputShape1, final Long[] inputShape2)
        throws DLInvalidTensorSpecException {
        checkPredicate(inputShape1.length == inputShape2.length,
            "Both shapes must have the same dimensionality, but shape one was " + inputShape1.length
                + "-dimensional and shape two was " + inputShape2.length + "-dimensional.");
    }

    /**
     * Checks if the specified shape has at least the specified number of dimensions. Throws exception if not.
     *
     * @param inputShape the shape of the input
     * @param lowestNumberOfDims the minimum dimensionality
     * @throws DLInvalidTensorSpecException if the input doesn't have at least the specified dimensionality
     */
    public static void dimsGreaterOrEqual(final Long[] inputShape, final int lowestNumberOfDims)
        throws DLInvalidTensorSpecException {
        checkPredicate(inputShape.length >= lowestNumberOfDims, "The input must be at least " + lowestNumberOfDims
            + "-dimensional, but was " + inputShape.length + "-dimensional.");
    }   

    private static void checkPredicate(final boolean pred, final String message) throws DLInvalidTensorSpecException {
        if (!pred) {
            throw new DLInvalidTensorSpecException("Invalid input specs. " + message);
        }
    }
}
