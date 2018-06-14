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
package org.knime.dl.keras.core.layers.dialog.tuple;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.dl.python.util.DLPythonUtils;

/**
 * Class representing a tuple.
 * 
 * @author David Kolb, KNIME GmbH, Konstanz, Germany
 */
public class DLKerasTuple {

    /** */
    public static final String SETTINGS_KEY_MIN = "DLKerasTuple.Min";

    /** */
    public static final String SETTINGS_KEY_MAX = "DLKerasTuple.Max";

    /** */
    public static final String SETTINGS_KEY_TUPLE = "DLKerasTuple.Tuple";

    private Long[] m_tuple;

    private int m_maxLength;

    private int m_minLength;

    private EnumSet<Constraint> m_constraints;

    /**
     * Constructor specifying the tuple. Its assumed that only tuples of the length of the specified array are allowed.
     * Also, partial tuples (may contain a question mark) are not allowed.
     * 
     * @param tuple the initial tuple
     */
    public DLKerasTuple(final Long[] tuple) {
        m_tuple = tuple;
        m_maxLength = tuple.length;
        m_minLength = tuple.length;

        m_constraints = EnumSet.noneOf(Constraint.class);
    }

    /**
     * Convenience constructor for the String representation of the tuple. Equivalent to
     * {@code new DLKerasTupel(DLKerasTupel.stringToTuple(tuple))}.
     * 
     * @param tuple
     */
    public DLKerasTuple(final String tuple) {
        this(stringToTuple(tuple));
    }

    /**
     * Constructor specifying the tuple, the minimum and maximum allowed tuple length, and if partial tuples (may
     * contain a question mark) are allowed.
     * 
     * @param tuple
     * @param minLength
     * @param maxLength
     * @param constraints
     */
    public DLKerasTuple(final Long[] tuple, final int minLength, final int maxLength,
        final EnumSet<Constraint> constraints) {
        if (maxLength < minLength) {
            throw new IllegalArgumentException("The maximum length must be bigger than the minimum length.");
        }
        m_minLength = minLength;
        m_maxLength = maxLength;

        if (tuple != null && (tuple.length > m_maxLength || tuple.length < m_minLength)) {
            throw new IllegalArgumentException(
                "Specified tuple length: " + tuple.length + " is not within the allowed bounds. Minimum length: "
                    + m_minLength + " Maximum length: " + m_maxLength);
        }
        m_tuple = tuple;

        m_constraints = constraints;

        if (!constraints.contains(Constraint.EMPTY)) {
            if (tuple == null) {
                throw new IllegalArgumentException("Specified tuple must not be empty.");
            }
        }

        if (!constraints.contains(Constraint.ZERO)) {
            if (containsZero(tuple)) {
                throw new IllegalArgumentException("Specified tuple must not contain zero values.");
            }
        }

        if (!constraints.contains(Constraint.NEGATIVE)) {
            if (containsNegatve(tuple)) {
                throw new IllegalArgumentException("Specified tuple must not contain negative values.");
            }
        }

        if (!constraints.contains(Constraint.PARTIAL)) {
            if (containsPartial(tuple)) {
                throw new IllegalArgumentException("Specified tuple must not contain partial values.");
            }
        }
    }

    /**
     * Convenience constructor for the String representation of the tuple. Equivalent to
     * {@code new DLKerasTupel(DLKerasTupel.stringToTuple(tuple), minLength, maxLength, isPartialAllowed)}.
     * 
     * @param tuple
     * @param minLength
     * @param maxLength
     * @param constraints
     */
    public DLKerasTuple(final String tuple, final int minLength, final int maxLength,
        final EnumSet<Constraint> constraints) {
        this(stringToTuple(tuple), minLength, maxLength, constraints);
    }

    /**
     * @return the tuple as a Long array
     */
    public Long[] getTuple() {
        return m_tuple;
    }

    /**
     * @return the maximum allowed tuple length
     */
    public int getMaxLength() {
        return m_maxLength;
    }

    /**
     * @return the minimum allowed tuple length
     */
    public int getMinLength() {
        return m_minLength;
    }

    /**
     * @return the constraints for this tuple. See {@link Constraint}.
     */
    public EnumSet<Constraint> getConstraints() {
        return m_constraints;
    }

    /**
     * @return whether partial tuples are allowed
     */
    public boolean isPartialAllowed() {
        return m_constraints.contains(Constraint.PARTIAL);
    }

    /**
     * @return whether the zero values are allowed for this tuple
     */
    public boolean isZeroAllowed() {
        return m_constraints.contains(Constraint.ZERO);
    }

    /**
     * @return whether the negative values are allowed for this tuple
     */
    public boolean isNegativeAllowed() {
        return m_constraints.contains(Constraint.NEGATIVE);
    }

    /**
     * @return whether the empty value is allowed for this tuple
     */
    public boolean isEmptyAllowed() {
        return m_constraints.contains(Constraint.EMPTY);
    }

    /**
     * @param tuple
     * @return whether the specified tuple contains zero values
     */
    public static boolean containsZero(final Long[] tuple) {
        for (Long l : tuple) {
            if (l != null && l == 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param tuple
     * @return whether the specified tuple contains negative values
     */
    public static boolean containsNegatve(final Long[] tuple) {
        for (Long l : tuple) {
            if (l != null && l < 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param tuple
     * @return whether the specified tuple contains partial (null) values
     */
    public static boolean containsPartial(final Long[] tuple) {
        for (Long l : tuple) {
            if (l == null) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return the Python representation of this tuple
     */
    public String toPytonTuple() {
        return toPytonTuple(false);
    }

    /**
     * @param increment whether to add plus one to each value before converting to string
     * 
     * @return the Python representation of this tuple
     */
    public String toPytonTuple(final boolean increment) {
        if (m_tuple == null) {
            return DLPythonUtils.NONE;
        }
        if (increment) {
            return DLPythonUtils.toPythonTuple(
                Arrays.stream(m_tuple).map(l -> l + 1).map(l -> String.valueOf(l)).toArray(String[]::new));
        } else {
            return DLPythonUtils
                .toPythonTuple(Arrays.stream(m_tuple).map(l -> String.valueOf(l)).toArray(String[]::new));
        }
    }

    /**
     * Converts the specified tuple to its String representation.
     * 
     * @param tuple the tuple to convert to String
     * 
     * @return String representation of the tuple or null if the specified tuple is null
     */
    public static String tupleToString(final Long[] tuple) {
        if (tuple == null) {
            return null;
        }
        return String.join(", ",
            Arrays.stream(tuple).map(l -> l == null ? "?" : String.valueOf(l)).toArray(String[]::new));
    }

    /**
     * Converts the specified String representation of the tuple to a Long array.
     * 
     * @param tuple the String representation of the tuple
     * 
     * @return the tuple as Long array or null if the specified tuple is null or empty
     */
    public static Long[] stringToTuple(final String tuple) {
        if (tuple == null || tuple.isEmpty()) {
            return null;
        }
        return DLPythonUtils.parseShape(tuple);
    }

    /**
     * Convenience method to saves the specified tuple to the specified NodeSettings.
     * 
     * @param tuple the tuple to save
     * @param settings the settings to write to
     */
    public static void saveTo(DLKerasTuple tuple, NodeSettingsWO settings) {
        settings.addString(SETTINGS_KEY_TUPLE, tupleToString(tuple.getTuple()));
        settings.addInt(SETTINGS_KEY_MIN, tuple.getMinLength());
        settings.addInt(SETTINGS_KEY_MAX, tuple.getMaxLength());

        for (Constraint c : tuple.getConstraints()) {
            settings.addString(c.getKey(), c.name());
        }
    }

    /**
     * Convenience method to load a tuple from the specified NodeSettings.
     * 
     * @param settings the settings to load from
     * @return the loaded tuple if existing, else null
     * @throws InvalidSettingsException
     */
    public static DLKerasTuple loadFrom(NodeSettingsRO settings) throws InvalidSettingsException {
        if (settings.containsKey(SETTINGS_KEY_MAX) && settings.containsKey(SETTINGS_KEY_MIN)
            && settings.containsKey(SETTINGS_KEY_TUPLE)) {

            List<Constraint> cs = new ArrayList<>();
            for (Constraint c : Constraint.values()) {
                if (settings.containsKey(c.getKey())) {
                    cs.add(c);
                }
            }

            return new DLKerasTuple(settings.getString(SETTINGS_KEY_TUPLE), settings.getInt(SETTINGS_KEY_MIN),
                settings.getInt(SETTINGS_KEY_MAX), EnumSet.copyOf(cs));
        } else {
            return null;
        }
    }

    /**
     * Constraints for tuples.
     */
    public enum Constraint {

            /** Set this constraint if zero values are allowed. */
            ZERO("DLKerasTuple.Constraint.ZERO"),
            /** Set this constraint if negative values are allowed. */
            NEGATIVE("DLKerasTuple.Constraint.NEGATIVE"),
            /** Set this constraint if an empty input is allowed. */
            EMPTY("DLKerasTuple.Constraint.EMPTY"),
            /** Set this constraint if question marks are allowed. */
            PARTIAL("DLKerasTuple.Constraint.PARTIAL");

        private final String m_key;

        Constraint(String settingsKey) {
            m_key = settingsKey;
        }

        /**
         * @return the settings key for this constraint
         */
        public String getKey() {
            return m_key;
        }
    }
}
