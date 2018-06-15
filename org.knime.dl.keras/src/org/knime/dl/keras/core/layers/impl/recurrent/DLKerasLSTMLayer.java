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

import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.keras.core.config.activation.DLKerasActivation;
import org.knime.dl.keras.core.struct.param.Parameter;
import org.knime.dl.python.util.DLPythonUtils;

/**
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class DLKerasLSTMLayer extends DLKerasAbstractGatedRNNLayer {
    
    /**
     * 
     */
    private static final int STATE_ONE = 1;

    /**
     * 
     */
    private static final int STATE_TWO = 2;

    /**
     * 
     */
    private static final int NUM_HIDDEN_STATES = 2;

    @Parameter(label = "Unit forget bias", tab = "Initializers")
    private boolean m_unitForgetBias = true;
    
    @Parameter(label = "Input tensor", min = "0")
    private DLTensorSpec m_inputTensor = null;
    
    @Parameter(label = "First hidden state tensor", min = "1")
    private DLTensorSpec m_hiddenStateTensor1 = null;
    
    @Parameter(label = "Second hidden state tensor", min = "2")
    private DLTensorSpec m_hiddenStateTensor2 = null;
    
    @Parameter(label = "Units", min = "1", stepSize = "1")
    private int m_units = DEFAULT_UNITS;
    
    @Parameter(label = "Activation")
    private DLKerasActivation m_activation = DLKerasActivation.TANH;
    
    @Parameter(label = "Recurrent activation")
    private DLKerasActivation m_recurrentActivation = DLKerasActivation.HARD_SIGMOID;

    @Parameter(label = "Use bias")
    private boolean m_useBias = true;

    @Parameter(label = "Dropout", min = "0.0", max = "1.0", stepSize = "0.1")
    private float m_dropout = 0.0f;

    @Parameter(label = "Recurrent dropout", min = "0.0", max = "1.0", stepSize = "0.1")
    private float m_recurrentDropout = 0.0f;
    
    @Parameter(label = "Implementation")
    private DLKerasGatedRNNImplementation m_implementation = DLKerasGatedRNNImplementation.ONE;
    
    @Parameter(label = "Return sequences")
    private boolean m_returnSequences = false;
    
    @Parameter(label = "Return state")
    private boolean m_returnState = false;

    @Parameter(label = "Go backwards")
    private boolean m_goBackwards = false;

    @Parameter(label = "Unroll")
    private boolean m_unroll = false;

    /**
     * Constructor for {@link DLKerasLSTMLayer}s.
     */
    public DLKerasLSTMLayer() {
        super("keras.layers.recurrent.LSTM", NUM_HIDDEN_STATES);
    }

    @Override
    protected void populateParameters(List<String> positionalParams, Map<String, String> namedParams) {
        super.populateParameters(positionalParams, namedParams);
        namedParams.put("unit_forget_bias", DLPythonUtils.toPython(m_unitForgetBias));
    }
    
    @Override
    public DLTensorSpec getInputTensorSpec(int index) {
        if (index == 0) {
            return m_inputTensor;
        } else if (index == STATE_ONE) {
            return m_hiddenStateTensor1;
        } else if (index == STATE_TWO) {
            return m_hiddenStateTensor2;
        } else {
            throw new IllegalArgumentException("This layer has only 3 possible input ports.");
        }
    }
    
    @Override
    protected int getUnits() {
        return m_units;
    }

    @Override
    protected boolean returnState() {
        return m_returnState;
    }

    @Override
    protected boolean returnSequences() {
        return m_returnSequences;
    }

    @Override
    protected DLKerasActivation getActivation() {
        return m_activation;
    }

    @Override
    protected boolean isUseBias() {
        return m_useBias;
    }

    @Override
    protected float getDropout() {
        return m_dropout;
    }

    @Override
    protected float getRecurrentDropout() {
        return m_recurrentDropout;
    }

    @Override
    protected boolean isGoBackwards() {
        return m_goBackwards;
    }

    @Override
    protected boolean isUnroll() {
        return m_unroll;
    }
    
    @Override
    protected DLKerasActivation getRecurrentActivation() {
        return m_recurrentActivation;
    }

    @Override
    protected DLKerasGatedRNNImplementation getImplementation() {
        return m_implementation;
    }
}
