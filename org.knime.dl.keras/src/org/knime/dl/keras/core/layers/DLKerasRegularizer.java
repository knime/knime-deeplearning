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

import static org.knime.dl.python.util.DLPythonUtils.*;

import java.util.List;
import java.util.Map;

import org.knime.core.node.InvalidSettingsException;
import org.scijava.param2.Parameter;

/**
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public interface DLKerasRegularizer extends DLKerasUtilityObject {

    // marker interface
    
    /**
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    static final class DLKerasL1Regularizer extends DLKerasAbstractUtilityObject implements DLKerasRegularizer {

        @Parameter(label = "L1 regularization factor", min = "0.0000001")
        private float m_l1;
        
        /**
         */
        public DLKerasL1Regularizer() {
            super("keras.regularizers.l1", "L1 regularizer");
        }

        @Override
        public void validateParameters() throws InvalidSettingsException {
            if (m_l1 < 0) {
                throw new InvalidSettingsException("The l1 regularization factor must be non-negative.");
            }
        }

        @Override
        protected void populateParameters(List<String> positionalParams, Map<String, String> namedParams) {
            namedParams.put("l1", toPython(m_l1));
        }
        
    }
    
    /**
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    static final class DLKerasL2Regularizer extends DLKerasAbstractUtilityObject implements DLKerasRegularizer {

        @Parameter(label = "L2 regularization factor", min = "0.0000001")
        private float m_l2;
        
        /**
         */
        public DLKerasL2Regularizer() {
            super("keras.regularizers.l2", "L2 regularizer");
        }

        @Override
        public void validateParameters() throws InvalidSettingsException {
            if (m_l2 < 0) {
                throw new InvalidSettingsException("The l2 regularization factor must be non-negative.");
            }
        }

        @Override
        protected void populateParameters(List<String> positionalParams, Map<String, String> namedParams) {
            namedParams.put("l2", toPython(m_l2));
        }
        
    }
    
    /**
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    static final class DLKerasL1L2Regularizer extends DLKerasAbstractUtilityObject implements DLKerasRegularizer {

        @Parameter(label = "L1 regularization factor", min = "0.0000001")
        private float m_l1;
        
        @Parameter(label = "L2 regularization factor", min = "0.0000001")
        private float m_l2;
        
        /**
         */
        public DLKerasL1L2Regularizer() {
            super("keras.regularizers.l1_l2", "L1 L2 regularizer");
        }

        @Override
        public void validateParameters() throws InvalidSettingsException {
            if (m_l1 < 0) {
                throw new InvalidSettingsException("The l1 regularization factor must be non-negative.");
            }
            if (m_l2 < 0) {
                throw new InvalidSettingsException("The l2 regularization factor must be non-negative.");
            }
        }

        @Override
        protected void populateParameters(List<String> positionalParams, Map<String, String> namedParams) {
            namedParams.put("l1", toPython(m_l1));
            namedParams.put("l2", toPython(m_l2));
        }
        
    }
}
