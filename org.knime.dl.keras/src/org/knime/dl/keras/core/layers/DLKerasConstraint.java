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

import static org.knime.dl.python.util.DLPythonUtils.toPython;

import java.util.List;
import java.util.Map;

import org.knime.core.node.InvalidSettingsException;
import org.knime.dl.keras.core.struct.param.Parameter;

/**
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public interface DLKerasConstraint extends DLKerasUtilityObject {

    // marker interface

    /**
     * Choices for {@link DLKerasConstraint}s to be used in layers.
     * 
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    static class DLKerasConstraintChoices extends DLKerasAbstractUtilityObjectChoices<DLKerasConstraint> {
        /**
         */
        @SuppressWarnings("unchecked")
        public DLKerasConstraintChoices() {
            super(new Class[]{DLKerasMaxNormConstraint.class,
                DLKerasNonNegativeConstraint.class, DLKerasUnitNormConstraint.class, DLKerasMinMaxNormConstraint.class});
        }
        
    }

    /**
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    static abstract class DLKerasAbstractConstraint extends DLKerasAbstractUtilityObject implements DLKerasConstraint {

        /**
         * @param kerasIdentifier
         * @param name
         */
        public DLKerasAbstractConstraint(String kerasIdentifier, String name) {
            super(kerasIdentifier, name);
        }

        @Override
        protected void populateParameters(List<String> positionalParams, Map<String, String> namedParams) {
            // nothing to populate
        }

        @Override
        public void validateParameters() throws InvalidSettingsException {
            // nothing to validate
        }

    }

    /**
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    static abstract class DLKerasAbstractAxisConstraint extends DLKerasAbstractConstraint {

        // how should axis be declared?
        @Parameter(label = "Axis")
        private int[] m_axis = {0};

        /**
         * @param kerasIdentifier
         * @param name
         */
        public DLKerasAbstractAxisConstraint(String kerasIdentifier, String name) {
            super(kerasIdentifier, name);
        }

        @Override
        public void validateParameters() throws InvalidSettingsException {
            // TODO do we support python style negative indexing?
            super.validateParameters();
        }

        @Override
        protected void populateParameters(List<String> positionalParams, Map<String, String> namedParams) {
            namedParams.put("axis", toPython(m_axis));
            super.populateParameters(positionalParams, namedParams);
        }
    }

    /**
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    static final class DLKerasMaxNormConstraint extends DLKerasAbstractAxisConstraint {

        @Parameter(label = "Maximum norm", min = "0.0000001")
        private float m_maxValue = 2.0f;

        /**
         */
        public DLKerasMaxNormConstraint() {
            super("keras.constraints.MaxNorm", "Maximum norm weight constraint");
        }

        @Override
        public void validateParameters() throws InvalidSettingsException {
            super.validateParameters();
            if (m_maxValue <= 0) {
                throw new InvalidSettingsException("The maximum norm must be positive.");
            }
        }

        @Override
        protected void populateParameters(List<String> positionalParams, Map<String, String> namedParams) {
            namedParams.put("max_value", toPython(m_maxValue));
            super.populateParameters(positionalParams, namedParams);
        }
    }

    /**
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    static final class DLKerasNonNegativeConstraint extends DLKerasAbstractConstraint {

        /**
         */
        public DLKerasNonNegativeConstraint() {
            super("keras.constraints.NonNeg", "Non negative constraint");
        }

    }

    /**
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    static final class DLKerasUnitNormConstraint extends DLKerasAbstractAxisConstraint {

        /**
         */
        public DLKerasUnitNormConstraint() {
            super("keras.constraints.UnitNorm", "Unit norm constraint");
        }

    }

    /**
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    static final class DLKerasMinMaxNormConstraint extends DLKerasAbstractAxisConstraint {

        @Parameter(label = "Minimum norm", min = "0.0")
        private float m_minValue = 0.0f;

        @Parameter(label = "Maximum norm", min = "0.0000001")
        private float m_maxValue = 1.0f;

        @Parameter(label = "Rate", min = "0.0", max = "1.0")
        private float m_rate = 1.0f;

        /**
         */
        public DLKerasMinMaxNormConstraint() {
            super("keras.constraints.MinMaxNorm", "Min-max norm constraint");
        }

        @Override
        public void validateParameters() throws InvalidSettingsException {
            super.validateParameters();
            if (m_maxValue <= 0) {
                throw new InvalidSettingsException("The maximum norm must be positive.");
            }
            if (m_minValue < 0) {
                throw new InvalidSettingsException("The minimum norm must be non-negative.");
            }
            if (m_rate < 0 || m_rate > 1) {
                throw new InvalidSettingsException("The rate must be in the [0, 1] interval.");
            }
        }

        @Override
        protected void populateParameters(List<String> positionalParams, Map<String, String> namedParams) {
            namedParams.put("min_value", toPython(m_minValue));
            namedParams.put("max_value", toPython(m_maxValue));
            namedParams.put("rate", toPython(m_rate));
            super.populateParameters(positionalParams, namedParams);
        }

    }
}
