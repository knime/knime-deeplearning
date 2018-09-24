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
package org.knime.dl.keras.core.layers.impl.recurrent2;

import java.util.List;
import java.util.Map;

import org.knime.core.node.InvalidSettingsException;
import org.knime.dl.keras.core.config.activation.DLKerasActivation;
import org.knime.dl.python.util.DLPythonUtils;

/**
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
abstract class DLKerasAbstractNativeRNNLayer2 extends DLKerasAbstractFCRNNLayer2 {


    /**
     * @return the activation
     */
    protected abstract DLKerasActivation getActivation();

    /**
     * @return the useBias
     */
    protected abstract boolean isUseBias();

    /**
     * @return the dropout
     */
    protected abstract float getDropout();

    /**
     * @return the recurrentDropout
     */
    protected abstract float getRecurrentDropout();

    /**
     * @return the goBackwards
     */
    protected abstract boolean isGoBackwards();

    /**
     * @return the unroll
     */
    protected abstract boolean isUnroll();

    /**
     * @param kerasIdentifier
     * @param numHiddenStates the number of hidden states
     */
    public DLKerasAbstractNativeRNNLayer2(String kerasIdentifier, int numHiddenStates) {
        super(kerasIdentifier, numHiddenStates);
    }

    @Override
    public void validateParameters() throws InvalidSettingsException {
        super.validateParameters();
        checkDropout(getDropout(), "dropout");
        checkDropout(getRecurrentDropout(), "recurrent dropout");
    }

    private static void checkDropout(float dropoutRate, String label) throws InvalidSettingsException {
        if (dropoutRate < 0.0f || dropoutRate > 1.0f) {
            throw new InvalidSettingsException(
                "The " + label + " must be in the interval [0,1] but was " + dropoutRate + ".");
        }
    }
    
    @Override
    protected void populateParameters(List<String> positionalParams, Map<String, String> namedParams) {
        super.populateParameters(positionalParams, namedParams);
        namedParams.put("activation", DLPythonUtils.toPython(getActivation().value()));
        namedParams.put("use_bias", DLPythonUtils.toPython(isUseBias()));
        namedParams.put("dropout", DLPythonUtils.toPython(getDropout()));
        namedParams.put("recurrent_dropout", DLPythonUtils.toPython(getRecurrentDropout()));
        namedParams.put("go_backwards", DLPythonUtils.toPython(isGoBackwards()));
        namedParams.put("unroll", DLPythonUtils.toPython(isUnroll()));
    }

}
