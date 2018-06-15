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
package org.knime.dl.keras.core.layers.impl.recurrent;

import java.util.List;
import java.util.Map;

import org.knime.core.node.InvalidSettingsException;
import org.knime.dl.keras.core.layers.DLInvalidTensorSpecException;
import org.knime.dl.python.util.DLPythonUtils;

/**
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public abstract class DLKerasAbstractFCRNNLayer extends DLKerasAbstractRNNLayer {

    /**
     * 
     */
    private static final int INPUT_RANK = 2;
    
    /**
     * 
     */
    protected static final int DEFAULT_UNITS = 100;

    /**
     * Constructor for {@link DLKerasAbstractFCRNNLayer}s.
     * 
     * @param kerasIdentifier the full python path to the layer constructor
     * @param numHiddenStates the number of hidden states
     */
    public DLKerasAbstractFCRNNLayer(String kerasIdentifier, int numHiddenStates) {
        super(kerasIdentifier, numHiddenStates);
    }
    
    @Override
    protected void validateInputShapes(List<Long[]> inputShapes)
        throws DLInvalidTensorSpecException {
        validateInputShape(inputShapes.get(0));
        int numStateInputs = inputShapes.size() - 1;
        if (numStateInputs > 0) {
            checkInputSpec(numStateInputs == getNumHiddenStates(),
                "Expected " + getNumHiddenStates() + " state inputs but received " + numStateInputs + ".");
            validateHiddenStateShapes(inputShapes.subList(1, inputShapes.size()));
        }
    }

    private static void validateInputShape(Long[] inputShape) throws DLInvalidTensorSpecException {
        checkInputSpec(inputShape.length == INPUT_RANK, "Expected an input with shape [time, input_dim].");
        checkInputSpec(inputShape[1] != null, "The feature dimension of the input must be known.");
    }

    private void validateHiddenStateShapes(List<Long[]> stateShapes) throws DLInvalidTensorSpecException {
        checkInputSpec(stateShapes.stream().allMatch(s -> s.length == 1), "Expected state input with shape [units].");
        checkInputSpec(stateShapes.stream().allMatch(s -> s[0] == getUnits()),
            "The feature dimension of the initial states must match the feature dimension of the internal state.");
    }

    @Override
    public void validateParameters() throws InvalidSettingsException {
        if (getUnits() <= 0) {
            throw new InvalidSettingsException("The number of units must be positive but was " + getUnits() + ".");
        }
    }

    /**
     * @return the number of units of this rnn layer i.e. the size of the feature dimension of its output
     * 
     */
    protected abstract int getUnits();

    @Override
    protected Long[] getOutputShape(Long[] inputShape) {
        if (returnState()) {
            return new Long[]{inputShape[0], (long)getUnits()};
        } else {
            return new Long[]{(long)getUnits()};
        }
    }

    @Override
    protected void populateParameters(List<String> positionalParams, Map<String, String> namedParams) {
        positionalParams.add(DLPythonUtils.toPython(getUnits()));
    }

}
