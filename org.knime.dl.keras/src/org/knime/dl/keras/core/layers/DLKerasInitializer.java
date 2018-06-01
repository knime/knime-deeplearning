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
import org.knime.dl.python.util.DLPythonUtils;

/**
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public interface DLKerasInitializer extends DLKerasUtilityObject {
    // marker interface

    
    /**
     * Choices for {@link DLKerasInitializer}s to be used in layers.
     * 
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    static final class DLKerasInitializerChoices extends DLKerasAbstractUtilityObjectChoices<DLKerasInitializer> {

        /**
         */
        @SuppressWarnings("unchecked")
        public DLKerasInitializerChoices() {
            super(new Class[]{DLKerasIdentityInitializer.class,
                DLKerasZerosInitializer.class, DLKerasOnesInitializer.class, DLKerasConstantInitializer.class,
                DLKerasRandomNormalInitializer.class, DLKerasRandomUniformInitializer.class,
                DLKerasTruncatedNormalInitializer.class, DLKerasVarianceScalingInitializer.class,
                DLKerasOrthogonalInitializer.class, DLKerasLeCunUniformInitializer.class,
                DLKerasGlorotNormalInitializer.class, DLKerasGlorotUniformInitializer.class,
                DLKerasHeNormalInitializer.class, DLKerasLeCunNormalInitializer.class, DLKerasHeUniformInitializer.class});
        }
    }

    /**
     * Declares parameter validation and population to reduce boilerplate in deriving classes.
     * 
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    static abstract class DLKerasAbstractInitializer extends DLKerasAbstractUtilityObject
        implements DLKerasInitializer {

        /**
         * @param kerasIdentifier
         * @param name
         */
        public DLKerasAbstractInitializer(String kerasIdentifier, String name) {
            super(kerasIdentifier, name);
        }

        @Override
        public void validateParameters() throws InvalidSettingsException {
            // nothing to validate
        }

        @Override
        protected void populateParameters(List<String> positionalParams, Map<String, String> namedParams) {
            // nothing to populate
        }
    }

    /**
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    static final class DLKerasIdentityInitializer extends DLKerasAbstractInitializer {

        @Parameter(label = "Gain")
        private float m_gain = 1.0f;

        /**
         */
        public DLKerasIdentityInitializer() {
            super("keras.initializers.Identity", "Identity initializer");
        }

        @Override
        protected void populateParameters(List<String> positionalParams, Map<String, String> namedParams) {
            namedParams.put("gain", toPython(m_gain));
            super.populateParameters(positionalParams, namedParams);
        }

    }

    /**
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    static final class DLKerasZerosInitializer extends DLKerasAbstractInitializer {

        /**
         */
        public DLKerasZerosInitializer() {
            super("keras.initializers.Zeros", "Zeros initializer");
        }

    }

    /**
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    static final class DLKerasOnesInitializer extends DLKerasAbstractInitializer {

        /**
         */
        public DLKerasOnesInitializer() {
            super("keras.initializers.Ones", "Ones initializer");
        }

    }

    /**
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    static final class DLKerasConstantInitializer extends DLKerasAbstractInitializer {

        @Parameter(label = "Value")
        private float m_value = 0;

        /**
         */
        public DLKerasConstantInitializer() {
            super("keras.initializers.Constant", "Constant initializer");
        }

        @Override
        protected void populateParameters(List<String> positionalParams, Map<String, String> namedParams) {
            namedParams.put("value", toPython(m_value));
        }

    }

    /**
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    static abstract class DLKerasAbstractSeededInitializer extends DLKerasAbstractInitializer {

        // TODO make component for seed
        @Parameter(label = "Seed", required = false)
        private Long m_seed = null;

        /**
         * @param kerasIdentifier
         * @param name
         */
        public DLKerasAbstractSeededInitializer(String kerasIdentifier, String name) {
            super(kerasIdentifier, name);
        }

        @Override
        protected void populateParameters(List<String> positionalParams, Map<String, String> namedParams) {
            namedParams.put("seed", DLPythonUtils.toPython(m_seed));
        }

    }

    /**
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    static abstract class DLKerasAbstractNormalInitializer extends DLKerasAbstractSeededInitializer {

        @Parameter(label = "Mean", min = "0.0000001")
        private float m_mean = 0.0f;

        @Parameter(label = "Standard deviation", min = "0.0000001")
        private float m_stddev = 0.05f;

        /**
         * @param kerasIdentifier
         * @param name
         */
        public DLKerasAbstractNormalInitializer(String kerasIdentifier, String name) {
            super(kerasIdentifier, name);
        }

        @Override
        protected final void populateParameters(List<String> positionalParams, Map<String, String> namedParams) {
            namedParams.put("mean", toPython(m_mean));
            namedParams.put("stddev", toPython(m_stddev));
            super.populateParameters(positionalParams, namedParams);
        }

        @Override
        public void validateParameters() throws InvalidSettingsException {
            if (m_stddev <= 0.0) {
                throw new InvalidSettingsException("The standard deviation must be positive.");
            }
        }

    }

    /**
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    static final class DLKerasRandomNormalInitializer extends DLKerasAbstractNormalInitializer {

        /**
         */
        public DLKerasRandomNormalInitializer() {
            super("keras.initializers.RandomNormal", "Random normal initializer");
        }

    }

    /**
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    static final class DLKerasTruncatedNormalInitializer extends DLKerasAbstractNormalInitializer {

        /**
         */
        public DLKerasTruncatedNormalInitializer() {
            super("keras.initializers.TruncatedNormal", "Truncated normal initializer");
        }

    }

    /**
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    static final class DLKerasRandomUniformInitializer extends DLKerasAbstractSeededInitializer {

        @Parameter(label = "Minimum value")
        private float m_minVal = -0.05f;

        @Parameter(label = "Maximum value")
        private float m_maxVal = 0.05f;

        /**
         */
        public DLKerasRandomUniformInitializer() {
            super("keras.initializers.RandomUniform", "Random uniform initializer");
        }

        @Override
        public void validateParameters() throws InvalidSettingsException {
            super.validateParameters();
            if (m_minVal >= m_maxVal) {
                throw new InvalidSettingsException(
                    "The minimum value (" + m_minVal + ") must be smaller than the maximum value(" + m_maxVal + ".");
            }
        }

        @Override
        protected void populateParameters(List<String> positionalParams, Map<String, String> namedParams) {
            namedParams.put("minval", toPython(m_minVal));
            namedParams.put("maxval", toPython(m_maxVal));
            super.populateParameters(positionalParams, namedParams);
        }

    }

    /**
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    static final class DLKerasVarianceScalingInitializer extends DLKerasAbstractSeededInitializer {

        @Parameter(label = "Scale")
        private float m_scale = 1.0f;

        @Parameter(label = "Mode")
        private DLKerasMode m_mode = DLKerasMode.FAN_IN;

        @Parameter(label = "Distribution")
        private DLKerasDistribution m_distribution = DLKerasDistribution.NORMAL;

        /**
         */
        public DLKerasVarianceScalingInitializer() {
            super("keras.initializers.VarianceScaling", "Variance scaling initializer");
        }

        @Override
        protected void populateParameters(List<String> positionalParams, Map<String, String> namedParams) {
            namedParams.put("scale", toPython(m_scale));
            namedParams.put("mode", toPython(m_mode.value()));
            namedParams.put("distribution", toPython(m_distribution.value()));
            super.populateParameters(positionalParams, namedParams);
        }

        @Override
        public void validateParameters() throws InvalidSettingsException {
            if (m_scale <= 0.0) {
                throw new InvalidSettingsException("Scale must be positive.");
            }
        }

    }

    /**
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    static final class DLKerasOrthogonalInitializer extends DLKerasAbstractSeededInitializer {

        @Parameter(label = "Gain")
        private float m_gain = 1.0f;

        /**
         */
        public DLKerasOrthogonalInitializer() {
            super("keras.initializers.Orthogonal", "Orthogonal initializer");
        }

        @Override
        protected void populateParameters(List<String> positionalParams, Map<String, String> namedParams) {
            namedParams.put("gain", toPython(m_gain));
            super.populateParameters(positionalParams, namedParams);
        }

        @Override
        public void validateParameters() throws InvalidSettingsException {
            // nothing to validate
        }

    }

    /**
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    static abstract class DLKerasAbstractPlainSeededInitializer extends DLKerasAbstractSeededInitializer {

        /**
         * @param kerasIdentifier
         * @param name
         */
        public DLKerasAbstractPlainSeededInitializer(String kerasIdentifier, String name) {
            super(kerasIdentifier, name);
        }

        @Override
        public final void validateParameters() throws InvalidSettingsException {
            // nothing to validate
        }

    }

    /**
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    static final class DLKerasLeCunUniformInitializer extends DLKerasAbstractPlainSeededInitializer {

        /**
         */
        public DLKerasLeCunUniformInitializer() {
            super("keras.initializers.lecun_uniform", "LeCun uniform initializer");
        }

    }

    /**
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    static final class DLKerasGlorotNormalInitializer extends DLKerasAbstractPlainSeededInitializer {

        /**
         */
        public DLKerasGlorotNormalInitializer() {
            super("keras.initializers.glorot_normal", "Glorot normal initializer");
        }

    }

    /**
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    static final class DLKerasGlorotUniformInitializer extends DLKerasAbstractPlainSeededInitializer {

        /**
         */
        public DLKerasGlorotUniformInitializer() {
            super("keras.initializers.glorot_uniform", "Glorot uniform initializer");
        }

    }

    /**
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    static final class DLKerasHeNormalInitializer extends DLKerasAbstractPlainSeededInitializer {

        /**
         */
        public DLKerasHeNormalInitializer() {
            super("keras.initializers.he_normal", "He normal initializer");
        }

    }

    /**
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    static final class DLKerasLeCunNormalInitializer extends DLKerasAbstractPlainSeededInitializer {

        /**
         */
        public DLKerasLeCunNormalInitializer() {
            super("keras.initializers.lecun_normal", "LeCun normal initializer");
        }

    }

    /**
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    static final class DLKerasHeUniformInitializer extends DLKerasAbstractPlainSeededInitializer {

        /**
         */
        public DLKerasHeUniformInitializer() {
            super("keras.initializers.he_uniform", "He uniform initializer");
        }

    }

}
