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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.knime.core.node.InvalidSettingsException;
import org.knime.dl.keras.core.config.constraint.DLKerasConstraint;
import org.knime.dl.keras.core.config.constraint.DLKerasConstraintChoices2;
import org.knime.dl.keras.core.config.initializer.DLKerasGlorotUniformInitializer;
import org.knime.dl.keras.core.config.initializer.DLKerasInitializer;
import org.knime.dl.keras.core.config.initializer.DLKerasInitializerChoices2;
import org.knime.dl.keras.core.config.initializer.DLKerasOrthogonalInitializer;
import org.knime.dl.keras.core.config.initializer.DLKerasZerosInitializer;
import org.knime.dl.keras.core.config.regularizer.DLKerasRegularizer;
import org.knime.dl.keras.core.config.regularizer.DLKerasRegularizerChoices2;
import org.knime.dl.keras.core.layers.DLInvalidTensorSpecException;
import org.knime.dl.keras.core.layers.DLKerasAbstractInnerLayer;
import org.knime.dl.keras.core.layers.DLKerasRNNLayer;
import org.knime.dl.keras.core.layers.DLLayerUtils;
import org.knime.dl.keras.core.struct.param.Parameter;
import org.knime.dl.keras.core.struct.param.Required;
import org.knime.dl.keras.util.DLKerasUtils;
import org.knime.dl.python.util.DLPythonUtils;

/**
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
abstract class DLKerasAbstractRNNLayer2 extends DLKerasAbstractInnerLayer implements DLKerasRNNLayer {
    
    @Parameter(label = "Kernel initializer", choices = DLKerasInitializerChoices2.class, tab = "Initializers")
    private DLKerasInitializer m_kernelInitializer = new DLKerasGlorotUniformInitializer();

    @Parameter(label = "Recurrent initializer", choices = DLKerasInitializerChoices2.class, tab = "Initializers")
    private DLKerasInitializer m_recurrentInitializer = new DLKerasOrthogonalInitializer();

    @Parameter(label = "Bias initializer", choices = DLKerasInitializerChoices2.class, tab = "Initializers")
    private DLKerasInitializer m_biasInitializer = new DLKerasZerosInitializer();

    @Parameter(label = "Kernel regularizer", required = Required.OptionalAndNotEnabled, choices = DLKerasRegularizerChoices2.class,
        tab = "Regularizers")
    private DLKerasRegularizer m_kernelRegularizer = null;

    @Parameter(label = "Recurrent regularizer", required = Required.OptionalAndNotEnabled, choices = DLKerasRegularizerChoices2.class,
        tab = "Regularizers")
    private DLKerasRegularizer m_recurrentRegularizer = null;

    @Parameter(label = "Bias regularizer", required = Required.OptionalAndNotEnabled, choices = DLKerasRegularizerChoices2.class,
        tab = "Regularizers")
    private DLKerasRegularizer m_biasRegularizer = null;

    @Parameter(label = "Activity regularizer", required = Required.OptionalAndNotEnabled, choices = DLKerasRegularizerChoices2.class,
        tab = "Regularizers")
    private DLKerasRegularizer m_activityRegularizer = null;

    @Parameter(label = "Kernel constraint", required = Required.OptionalAndNotEnabled, choices = DLKerasConstraintChoices2.class,
        tab = "Constraints")
    private DLKerasConstraint m_kernelConstraint = null;

    @Parameter(label = "Recurrent constraint", required = Required.OptionalAndNotEnabled, choices = DLKerasConstraintChoices2.class,
        tab = "Constraints")
    private DLKerasConstraint m_recurrentConstraint = null;

    @Parameter(label = "Bias constraint", required = Required.OptionalAndNotEnabled, choices = DLKerasConstraintChoices2.class,
        tab = "Constraints")
    private DLKerasConstraint m_biasConstraint = null;
    
    private final int m_numHiddenStates;
    
    /**
     * @param kerasIdentifier 
     * @param numHiddenStates 
     * 
     */
    public DLKerasAbstractRNNLayer2(String kerasIdentifier, int numHiddenStates) {
        super(kerasIdentifier, numHiddenStates + 1);
        m_numHiddenStates = numHiddenStates;
    }
    

    @Override
    public final String populateCall(List<String> inputTensors) {
        int maxInputs = m_numHiddenStates + 1;
        if (inputTensors.size() != 1 && inputTensors.size() != maxInputs) {
            throw new IllegalArgumentException("This recurrent layer expects either one input tensor or " + maxInputs
                + " input tensors if initial states are provided.");
        }
        if (inputTensors.size() == 1) {
            return inputTensors.get(0);
        }
        return inputTensors.get(0) + ", initial_state="
            + inputTensors.stream().skip(1).collect(Collectors.joining(",", "[", "]"));
    }
    
    protected int getNumHiddenStates() {
        return m_numHiddenStates;
    }
    
    protected abstract boolean returnState();
    
    protected abstract boolean returnSequences();
    
    @Override
    protected final void validateInputSpecs(List<Class<?>> inputElementTypes, List<Long[]> inputShapes)
        throws DLInvalidTensorSpecException {
        checkInputSpec(inputElementTypes.stream().allMatch(t -> t.equals(float.class)),
                "Inputs to recurrent layers have to be of type float.");
        validateInputShapes(inputShapes);
    }
    
    protected abstract void validateInputShapes(List<Long[]> inputShapes) throws DLInvalidTensorSpecException;
    
    @Override
    public void validateParameters() throws InvalidSettingsException {
        m_kernelInitializer.validateParameters();
        m_recurrentInitializer.validateParameters();
        m_biasInitializer.validateParameters();
        DLLayerUtils.validateOptionalParameter(m_kernelRegularizer);
        DLLayerUtils.validateOptionalParameter(m_recurrentRegularizer);
        DLLayerUtils.validateOptionalParameter(m_biasRegularizer);
        DLLayerUtils.validateOptionalParameter(m_activityRegularizer);
        DLLayerUtils.validateOptionalParameter(m_kernelConstraint);
        DLLayerUtils.validateOptionalParameter(m_recurrentConstraint);
        DLLayerUtils.validateOptionalParameter(m_biasConstraint);
    }
    
    protected static <T> List<T> repeat(T obj, int times) {
        return IntStream.range(0, times).mapToObj(i -> obj).collect(Collectors.toList());
    }


    @Override
    protected final List<Class<?>> inferOutputElementTypes(List<Class<?>> inputElementTypes)
        throws DLInvalidTensorSpecException {
        Class<?> type = float.class;
        int numOutputs = getNumOutputs();
        return repeat(type, numOutputs);
    }

    @Override
    protected void populateParameters(List<String> positionalParams, Map<String, String> namedParams) {
        namedParams.put("return_sequences", DLPythonUtils.toPython(returnSequences()));
        namedParams.put("return_state", DLPythonUtils.toPython(returnState()));
        namedParams.put("kernel_initializer", m_kernelInitializer.getBackendRepresentation());
        namedParams.put("recurrent_initializer", m_recurrentInitializer.getBackendRepresentation());
        namedParams.put("bias_initializer", m_biasInitializer.getBackendRepresentation());
        namedParams.put("kernel_regularizer", DLKerasUtils.Layers.toPython(m_kernelRegularizer));
        namedParams.put("recurrent_regularizer", DLKerasUtils.Layers.toPython(m_recurrentRegularizer));
        namedParams.put("bias_regularizer", DLKerasUtils.Layers.toPython(m_biasRegularizer));
        namedParams.put("activity_regularizer", DLKerasUtils.Layers.toPython(m_activityRegularizer));
        namedParams.put("kernel_constraint", DLKerasUtils.Layers.toPython(m_kernelConstraint));
        namedParams.put("recurrent_constraint", DLKerasUtils.Layers.toPython(m_recurrentConstraint));
        namedParams.put("bias_constraint", DLKerasUtils.Layers.toPython(m_biasConstraint));
    }


    private int getNumOutputs() {
        int numOutputs = 1;
        if (returnState()) {
            numOutputs += getNumHiddenStates();
        }
        return numOutputs;
    }


    @Override
    protected final List<Long[]> inferOutputShapes(List<Long[]> inputShape) {
        Long[] inShape = inputShape.get(0);
        List<Long[]> outputShapes = new ArrayList<>(getNumOutputs());
        outputShapes.add(getOutputShape(inShape));
        if (returnState()) {
            outputShapes.addAll(repeat(getStateShape(inShape), getNumHiddenStates()));
        }
        return outputShapes;
    }
    
    protected abstract Long[] getOutputShape(Long[] inputShape);
    
    protected abstract Long[] getStateShape(Long[] inputShape);

}
