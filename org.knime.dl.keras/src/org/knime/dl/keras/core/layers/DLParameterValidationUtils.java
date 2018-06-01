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

/**
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @author David Kolb, KNIME GmbH, Konstanz, Germany
 */
public final class DLParameterValidationUtils {

    private final static String PARTIAL_SHAPE_PATTERN = "(\\d+|\\?)(,\\s?(\\d+|\\?))*";

    private final static String SHAPE_PATTERN = "\\d+(,\\s?\\d+)*";

    private DLParameterValidationUtils() {
        // static utility class
    }

    /**
     * Checks if <b>object</b> is contained in <b>choices</b> and throws an exception if not.
     * 
     * @param object the object that should be contained in <b>set</b>
     * @param choices the set of possible choices
     * @param choiceLabel a name for what is contained in <b>choices</b>
     * @throws InvalidSettingsException if <b>set</b> does not contain <b>object</b>
     * 
     */
    public static <T> void checkContains(T object, Set<T> choices, String choiceLabel) throws InvalidSettingsException {
        if (!choices.contains(object)) {
            throw new InvalidSettingsException("Unsupported " + choiceLabel + " '" + object + "'.");
        }
    }

    /**
     * Throws an exception of the the values of the specified array are lower or equal to zero.
     * 
     * @param tuple the tuple to check
     * @param parameterName the parameter name for reporting
     * @throws InvalidSettingsException
     */
    public static void checkTupleNotZeroNotNegative(final Long[] tuple, final String parameterName)
        throws InvalidSettingsException {
        for (Long l : tuple) {
            if (l != null && l <= 0) {
                throw new InvalidSettingsException(
                    "Value/s of parameter " + parameterName + " must not be zero or negative.");
            }
        }
    }

    /**
     * Throws an exception of the the values of the specified array length is not equal to the specified length.
     * 
     * @param tuple the tuple to check
     * @param expectedLength the expected array length
     * @param parameterName the parameter name for reporting
     * @throws InvalidSettingsException
     */
    public static void checkTupleLength(final Long[] tuple, final int expectedLength, final String parameterName)
        throws InvalidSettingsException {
        if (tuple.length != expectedLength) {
            throw new InvalidSettingsException(
                "There must be exactly " + expectedLength + " values in tuple for parameter " + parameterName + ".");
        }
    }

    /**
     * Checks if the specified string representation of a tuple an be parsed to a shape array and throws an exception if
     * not.
     * 
     * @param tuple the tuple to check
     * @param partialAllowed whether partial shapes (question marks in the String) are allowed or not
     * @throws InvalidSettingsException
     */
    public static void checkTupleString(final String tuple, final boolean partialAllowed)
        throws InvalidSettingsException {
        if (partialAllowed) {
            if (!tuple.matches(PARTIAL_SHAPE_PATTERN)) {
                throw new InvalidSettingsException(
                    "Invalid tuple format: " + tuple + ". Must be digits or a question mark separated by a comma.");
            }
        } else {
            if (!tuple.matches(SHAPE_PATTERN)) {
                throw new InvalidSettingsException(
                    "Invalid tuple format: " + tuple + ". Must be digits separated by a comma.");
            }
        }

    }
}
