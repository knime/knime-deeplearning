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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.knime.core.node.InvalidSettingsException;
import org.knime.dl.keras.core.config.constraint.DLKerasConstraint;
import org.knime.dl.keras.core.config.constraint.DLKerasConstraintChoices;
import org.knime.dl.keras.core.config.initializer.DLKerasGlorotUniformInitializer;
import org.knime.dl.keras.core.config.initializer.DLKerasInitializer;
import org.knime.dl.keras.core.config.initializer.DLKerasInitializerChoices;
import org.knime.dl.keras.core.config.initializer.DLKerasOrthogonalInitializer;
import org.knime.dl.keras.core.config.initializer.DLKerasZerosInitializer;
import org.knime.dl.keras.core.config.regularizer.DLKerasRegularizer;
import org.knime.dl.keras.core.config.regularizer.DLKerasRegularizerChoices;
import org.knime.dl.keras.core.layers.DLInvalidTensorSpecException;
import org.knime.dl.keras.core.layers.DLKerasAbstractInnerLayer;
import org.knime.dl.keras.core.layers.DLKerasRNNLayer;
import org.knime.dl.keras.core.struct.param.Parameter;
import org.knime.dl.keras.util.DLKerasUtils;
import org.knime.dl.python.util.DLPythonUtils;

/**
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public abstract class DLKerasAbstractRNNLayer extends DLKerasAbstractInnerLayer implements DLKerasRNNLayer {

    @Parameter(label = "Units", min = "1", stepSize = "1")
    private int m_units = 100;

    @Parameter(label = "Kernel initializer", choices = DLKerasInitializerChoices.class, tab = "Initializers")
    private DLKerasInitializer m_kernelInitializer = new DLKerasGlorotUniformInitializer();

    @Parameter(label = "Recurrent initializer", choices = DLKerasInitializerChoices.class, tab = "Initializers")
    private DLKerasInitializer m_recurrentInitializer = new DLKerasOrthogonalInitializer();

    @Parameter(label = "Bias initializer", choices = DLKerasInitializerChoices.class, tab = "Initializers")
    private DLKerasInitializer m_biasInitializer = new DLKerasZerosInitializer();

    @Parameter(label = "Kernel regularizer", required = false, choices = DLKerasRegularizerChoices.class,
        tab = "Regularizers")
    private DLKerasRegularizer m_kernelRegularizer = null;

    @Parameter(label = "Recurrent regularizer", required = false, choices = DLKerasRegularizerChoices.class,
        tab = "Regularizers")
    private DLKerasRegularizer m_recurrentRegularizer = null;

    @Parameter(label = "Bias regularizer", required = false, choices = DLKerasRegularizerChoices.class,
        tab = "Regularizers")
    private DLKerasRegularizer m_biasRegularizer = null;

    @Parameter(label = "Activity regularizer", required = false, choices = DLKerasRegularizerChoices.class,
        tab = "Regularizers")
    private DLKerasRegularizer m_activityRegularizer = null;

    @Parameter(label = "Kernel constraint", required = false, choices = DLKerasConstraintChoices.class,
        tab = "Constraints")
    private DLKerasConstraint m_kernelConstraint = null;

    @Parameter(label = "Recurrent constraint", required = false, choices = DLKerasConstraintChoices.class,
        tab = "Constraints")
    private DLKerasConstraint m_recurrentConstraint = null;

    @Parameter(label = "Bias constraint", required = false, choices = DLKerasConstraintChoices.class,
        tab = "Constraints")
    private DLKerasConstraint m_biasConstraint = null;

    @Parameter(label = "Return sequences")
    private boolean m_returnSequences = false;

    @Parameter(label = "Return state")
    private boolean m_returnState = false;
    // TODO add parameter for stateful once we support stateful execution (and learning)

    private final int m_numHiddenStates;

    static <T> List<T> repeat(T obj, int times) {
        return IntStream.range(0, times).mapToObj(i -> obj).collect(Collectors.toList());
    }

    /**
     * @param kerasIdentifier
     * @param numHiddenStates
     */
    public DLKerasAbstractRNNLayer(String kerasIdentifier, int numHiddenStates) {
        super(kerasIdentifier, numHiddenStates + 1);
        m_numHiddenStates = numHiddenStates;
    }

    @Override
    public String populateCall(String[] inputTensors) {
        int maxInputs = m_numHiddenStates + 1;
        if (inputTensors.length != 1 && inputTensors.length != maxInputs) {
            throw new IllegalArgumentException("This recurrent layer expects either one input tensor or " + maxInputs
                + " input tensors if initial states are provided.");
        }
        if (inputTensors.length == 1) {
            return inputTensors[0];
        }
        return inputTensors[0] + ", initial_state="
            + Arrays.stream(inputTensors).skip(1).collect(Collectors.joining(",", "[", "]"));
    }

    @Override
    public void validateParameters() throws InvalidSettingsException {
        if (m_units <= 0) {
            throw new InvalidSettingsException("The number of units must be positive but was " + m_units + ".");
        }
    }

    @Override
    protected void validateInputSpecs(List<Class<?>> inputElementTypes, List<Long[]> inputShapes)
        throws DLInvalidTensorSpecException {
        checkInputSpec(inputElementTypes.stream().allMatch(t -> t.equals(float.class)),
            "Inputs to recurrent layers have to be of type float.");
        Long[] inputShape = inputShapes.get(0);
        checkInputSpec(inputShapes.get(0).length == 2, "Expected an input with shape [time, input_dim].");
        checkInputSpec(inputShape[1] != null, "The feature dimension of the input must be known.");
        int numStateInputs = inputShapes.size() - 1;
        if (numStateInputs > 0) {
            checkInputSpec(numStateInputs == m_numHiddenStates,
                "Expected " + m_numHiddenStates + " state inputs but received " + numStateInputs + ".");
            checkInputSpec(inputShapes.stream().skip(1).allMatch(s -> s.length == 1),
                "Expected state input with shape [units].");
            checkInputSpec(inputShapes.stream().skip(1).allMatch(s -> s[0] == m_units),
                "The feature dimension of the initial states must match the feature dimension of the internal state.");
        }
    }

    @Override
    protected List<Class<?>> inferOutputElementTypes(List<Class<?>> inputElementTypes)
        throws DLInvalidTensorSpecException {
        Class<?> type = float.class;
        int numOutputs = getNumOutputs();
        return repeat(type, numOutputs);
    }

    private int getNumOutputs() {
        int numOutputs = 1;
        if (m_returnState) {
            numOutputs += m_numHiddenStates;
        }
        return numOutputs;
    }

    @Override
    protected List<Long[]> inferOutputShapes(List<Long[]> inputShape) {
        Long[] outShape;
        Long[] inShape = inputShape.get(0);
        if (m_returnSequences) {
            outShape = new Long[]{inShape[0], (long)m_units};
        } else {
            outShape = new Long[]{(long)m_units};
        }
        return repeat(outShape, getNumOutputs());
    }

    @Override
    protected void populateParameters(List<String> positionalParams, Map<String, String> namedParams) {
        positionalParams.add(DLPythonUtils.toPython(m_units));
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
        namedParams.put("return_sequences", DLPythonUtils.toPython(m_returnSequences));
        namedParams.put("return_state", DLPythonUtils.toPython(m_returnState));
    }

}