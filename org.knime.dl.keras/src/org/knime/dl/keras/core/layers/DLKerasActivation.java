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

import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;

import org.knime.core.node.InvalidSettingsException;
import org.knime.dl.keras.core.struct.param.Parameter;
import org.knime.dl.python.util.DLPythonUtils;

/**
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public interface DLKerasActivation extends DLKerasUtilityObject {

    // marker interface
    
    /**
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    static abstract class DLKerasParameterLessActivation extends DLKerasAbstractUtilityObject implements DLKerasActivation {

        /**
         * @param kerasIdentifier
         * @param name
         */
        public DLKerasParameterLessActivation(String kerasIdentifier, String name) {
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
    static final class DLKerasSoftmaxActivation extends DLKerasAbstractUtilityObject implements DLKerasActivation {

        @Parameter(label = "Axis")
        private int m_axis = -1;
        
        /**
         */
        public DLKerasSoftmaxActivation() {
            super("keras.activations.softmax", "Softmax activation");
        }

        @Override
        public void validateParameters() throws InvalidSettingsException {
            // TODO validation can only be done if input spec is available
        }

        @Override
        protected void populateParameters(List<String> positionalParams, Map<String, String> namedParams) {
            namedParams.put("axis", DLPythonUtils.toPython(m_axis));
        }
    }
    
    /**
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    static abstract class DLKerasAbstractLinearActivation extends DLKerasAbstractUtilityObject implements DLKerasActivation {

        @Parameter(label = "Alpha")
        private float m_alpha;
        
        /**
         * @param kerasIdentifier
         * @param name
         * @param defaultAlpha 
         */
        public DLKerasAbstractLinearActivation(String kerasIdentifier, String name, float defaultAlpha) {
            super(kerasIdentifier, name);
            m_alpha = defaultAlpha;
        }
        
        @Override
        public void validateParameters() throws InvalidSettingsException {
            // TODO Keras doesn't make any assumptions but we might want to prevent non-sense (would first have to figure out what non-sense is though)
        }
        
        @Override
        protected void populateParameters(List<String> positionalParams, Map<String, String> namedParams) {
            namedParams.put("alpha", DLPythonUtils.toPython(m_alpha));
        }
        
    }
    
    
    /**
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    static final class DLKerasELUActivation extends DLKerasAbstractLinearActivation {

        private static final float DEFAULT_ALPHA = 1.0f;
        
        /**
         */
        public DLKerasELUActivation() {
            super("keras.activations.elu", "ELU activation", DEFAULT_ALPHA);
        }
    }
    
    /**
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    static final class DLKerasReLuActivation extends DLKerasAbstractLinearActivation {

        private static final float DEFAULT_ALPHA = 0.0f;
        
        @Parameter(label = "Max value", required = false)
        private OptionalDouble m_maxValue = OptionalDouble.empty(); 
        
        /**
         */
        public DLKerasReLuActivation() {
            super("keras.activations.relu", "ReLU activation", DEFAULT_ALPHA);
        }
        
        @Override
        public void validateParameters() throws InvalidSettingsException {
            // TODO Keras doesn't make any assumptions but we might want to prevent non-sense
            super.validateParameters();
        }
        
        @Override
        protected void populateParameters(List<String> positionalParams, Map<String, String> namedParams) {
            super.populateParameters(positionalParams, namedParams);
            namedParams.put("max_value", DLPythonUtils.toPython(m_maxValue));
        }
    }
    
    /**
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    static final class DLKerasSELUActivation extends DLKerasParameterLessActivation {

        /**
         */
        public DLKerasSELUActivation() {
            super("keras.activations.selu", "SELU activation");
        }
    }
    
    /**
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    static final class DLKerasSoftPlusActivation extends DLKerasParameterLessActivation {

        /**
         * @param kerasIdentifier
         * @param name
         */
        public DLKerasSoftPlusActivation(String kerasIdentifier, String name) {
            super("keras.activations.softplus", "Softplus activation");
        }
        
    }
    
    /**
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    static final class DLKerasSoftSignActivation extends DLKerasParameterLessActivation {

        /**
         */
        public DLKerasSoftSignActivation() {
            super("keras.activations.softsign", "Softsign activation");
        }
        
    }
    
    /**
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    static final class DLKerasTanhActivation extends DLKerasParameterLessActivation {

        /**
         */
        public DLKerasTanhActivation() {
            super("keras.activations.tanh", "Tanh activation");
        }
        
    }
    
    /**
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    static final class DLKerasSigmoidActivation extends DLKerasParameterLessActivation {

        /**
         */
        public DLKerasSigmoidActivation() {
            super("keras.activations.sigmoid", "Sigmoid activation");
        }
        
    }
    
    /**
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    static final class DLKerasHardSigmoidActivation extends DLKerasParameterLessActivation {

        /**
         */
        public DLKerasHardSigmoidActivation() {
            super("keras.activations.hard_sigmoid", "Hard sigmoid activation");
        }
        
    }
    
    /**
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    static final class DLKerasLinearActivation extends DLKerasParameterLessActivation {

        /**
         */
        public DLKerasLinearActivation() {
            super("keras.activations.linear", "Linear activation");
        }
        
    }
}
