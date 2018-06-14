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
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.knime.core.node.InvalidSettingsException;
import org.knime.dl.keras.core.config.activation.DLKerasActivation;
import org.knime.dl.keras.core.config.constraint.DLKerasConstraint;
import org.knime.dl.keras.core.config.constraint.DLKerasConstraintChoices;
import org.knime.dl.keras.core.config.initializer.DLKerasGlorotUniformInitializer;
import org.knime.dl.keras.core.config.initializer.DLKerasInitializer;
import org.knime.dl.keras.core.config.initializer.DLKerasInitializerChoices;
import org.knime.dl.keras.core.config.initializer.DLKerasOrthogonalInitializer;
import org.knime.dl.keras.core.config.initializer.DLKerasZerosInitializer;
import org.knime.dl.keras.core.config.regularizer.DLKerasRegularizer;
import org.knime.dl.keras.core.config.regularizer.DLKerasRegularizerChoices;
import org.knime.dl.keras.core.layers.DLConvolutionLayerUtils;
import org.knime.dl.keras.core.layers.DLInvalidTensorSpecException;
import org.knime.dl.keras.core.layers.DLKerasAbstractInnerLayer;
import org.knime.dl.keras.core.layers.DLKerasDataFormat;
import org.knime.dl.keras.core.layers.DLKerasPadding;
import org.knime.dl.keras.core.layers.DLKerasRNNLayer;
import org.knime.dl.keras.core.layers.dialog.tuple.DLKerasTuple;
import org.knime.dl.keras.core.layers.dialog.tuple.DLKerasTuple.Constraint;
import org.knime.dl.keras.core.struct.param.Parameter;
import org.knime.dl.keras.util.DLKerasUtils;
import org.knime.dl.python.util.DLPythonUtils;

/**
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class DLKerasConvLSTM2DLayer extends DLKerasAbstractInnerLayer implements DLKerasRNNLayer {

    @Parameter(label = "Filters", min = "1", stepSize = "1")
    private int m_filters = 100;

    @Parameter(label = "Kernel size")
    private DLKerasTuple m_kernelSize = new DLKerasTuple("3, 3", 2, 2, EnumSet.noneOf(Constraint.class));

    @Parameter(label = "Strides")
    private DLKerasTuple m_strides = new DLKerasTuple("1, 1", 2, 2, EnumSet.noneOf(Constraint.class));

    @Parameter(label = "Padding")
    private DLKerasPadding m_padding = DLKerasPadding.SAME;

    @Parameter(label = "Data format")
    private DLKerasDataFormat m_dataFormat = DLKerasDataFormat.CHANNEL_LAST;

    @Parameter(label = "Dilation rate")
    private DLKerasTuple m_dilationRate = new DLKerasTuple("1, 1", 2, 2, EnumSet.noneOf(Constraint.class));

    @Parameter(label = "Activation")
    private DLKerasActivation m_activation = DLKerasActivation.TANH;

    @Parameter(label = "Recurrent activation")
    private DLKerasActivation m_recurrentActivation = DLKerasActivation.HARD_SIGMOID;

    @Parameter(label = "Use bias")
    private boolean m_useBias = true;

    @Parameter(label = "Kernel initializer", choices = DLKerasInitializerChoices.class, tab = "Initializers")
    private DLKerasInitializer m_kernelInitializer = new DLKerasGlorotUniformInitializer();

    @Parameter(label = "Recurrent initializer", choices = DLKerasInitializerChoices.class, tab = "Initializers")
    private DLKerasInitializer m_recurrentInitializer = new DLKerasOrthogonalInitializer();

    @Parameter(label = "Bias initializer", choices = DLKerasInitializerChoices.class, tab = "Initializers")
    private DLKerasInitializer m_biasInitializer = new DLKerasZerosInitializer();

    @Parameter(label = "Unit forget bias", tab = "Initializers")
    private boolean m_unitForgetBias = true;

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

    @Parameter(label = "Go backwards")
    private boolean m_goBackwards = false;

    @Parameter(label = "Return sequences")
    private boolean m_returnSequences = false;

    @Parameter(label = "Return state")
    private boolean m_returnState = false;
    // TODO add parameter for stateful once we support stateful execution (and learning)

    @Parameter(label = "Dropout", min = "0.0", max = "1.0", stepSize = "0.1")
    private float m_dropout = 0.0f;

    @Parameter(label = "Recurrent dropout", min = "0.0", max = "1.0", stepSize = "0.1")
    private float m_recurrentDropout = 0.0f;

    /**
     */
    public DLKerasConvLSTM2DLayer() {
        super("keras.layers.ConvLSTM2D", 3);
    }

    @Override
    public String populateCall(String[] inputTensors) {
        int maxInputs = 3;
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
        if (m_filters <= 0) {
            throw new InvalidSettingsException("The number of filters must be positive but was " + m_filters + ".");
        }
        checkDropoutRate(m_dropout, "dropout");
        checkDropoutRate(m_recurrentDropout, "recurrent dropout");
        Long[] strides = m_strides.getTuple();
        Long[] dilationRate = m_dilationRate.getTuple();
        if (Arrays.stream(strides).anyMatch(s -> s > 1) && Arrays.stream(dilationRate).anyMatch(d -> d > 1)) {
            throw new InvalidSettingsException(
                "Specifying any stride value != 1 is incompatible with specifying any dilation rate value != 1");
        }
    }

    private static void checkDropoutRate(float rate, String label) throws InvalidSettingsException {
        if (rate < 0 || rate >= 1) {
            throw new InvalidSettingsException("The " + label + " rate must be in the interval [0, 1).");
        }
    }

    @Override
    protected void validateInputSpecs(List<Class<?>> inputElementTypes, List<Long[]> inputShapes)
        throws DLInvalidTensorSpecException {
        checkInputSpec(inputElementTypes.stream().allMatch(t -> t.equals(float.class)),
            "Inputs to recurrent layers have to be of type float.");

        Long[] inputShape = inputShapes.get(0);
        checkInputSpec(inputShape.length == 4, "Expected an input with shape " + getShapeString(true) + ".");
        checkInputSpec(inputShape[3] != null, "The number of channels must be defined for the input.");
        if (m_padding == DLKerasPadding.VALID) {
            checkInputSpec(Arrays.stream(getOutputShape(inputShape)).allMatch(d -> d == null || d > 0),
                    "The kernel size may not exceed the height and width in case of valid padding.");
        }
        int numStateInputs = inputShapes.size() - 1;
        if (numStateInputs > 0) {
            checkInputSpec(numStateInputs == 2, "Expected 2 state inputs but received " + numStateInputs + ".");
            checkInputSpec(inputShapes.stream().skip(1).allMatch(s -> s.length == 3),
                "Expected state input with shape " + getShapeString(false));
            checkInputSpec(inputShapes.stream().skip(1).allMatch(s -> s[2] == m_filters),
                "The channels of the initial shape must match the shape of the internal state.");
        }
    }

    private Long[] getOutputShape(Long[] inputShape) {
        Long[] convShape = Arrays.stream(inputShape).skip(1).toArray(Long[]::new);
        return DLConvolutionLayerUtils.computeOutputShape(convShape, m_filters, m_kernelSize.getTuple(),
            m_strides.getTuple(), m_dilationRate.getTuple(), m_padding.value(),
            m_dataFormat.value());
    }
    

    private String getShapeString(boolean withTime) {
        String suffix = "channel, height, width]";
        if (m_dataFormat == DLKerasDataFormat.CHANNEL_FIRST) {
            return withTime ? "[time, " + suffix : "[" + suffix;
        } else {
            return withTime ? "[time, " + suffix : "[" + suffix;
        }
    }

    @Override
    protected List<Class<?>> inferOutputElementTypes(List<Class<?>> inputElementTypes)
        throws DLInvalidTensorSpecException {
        Class<?> type = float.class;
        int numOutputs = m_returnState ? 3 : 1;
        return DLKerasAbstractRNNLayer.repeat(type, numOutputs);
    }

    @Override
    protected List<Long[]> inferOutputShapes(List<Long[]> inputShape) {
        Long[] inShape = inputShape.get(0);
        Long[] convShape = getOutputShape(inShape);
        Long[] outShape;
        if (m_returnSequences) {
            outShape = new Long[inShape.length];
            outShape[0] = inShape[0];
            IntStream.range(1, outShape.length).forEach(i -> outShape[i] = convShape[i - 1]);
        } else {
            outShape = convShape;
        }
        return DLKerasAbstractRNNLayer.repeat(outShape, m_returnState ? 3 : 1);
    }

    @Override
    protected void populateParameters(List<String> positionalParams, Map<String, String> namedParams) {
        positionalParams.add(DLPythonUtils.toPython(m_filters));
        positionalParams.add(m_kernelSize.toPytonTuple());
        namedParams.put("strides", m_strides.toPytonTuple());
        namedParams.put("padding", DLPythonUtils.toPython(m_padding.value()));
        namedParams.put("data_format", DLPythonUtils.toPython(m_dataFormat.value()));
        namedParams.put("dilation_rate", m_dilationRate.toPytonTuple());
        namedParams.put("activation", DLPythonUtils.toPython(m_activation.value()));
        namedParams.put("recurrent_activation", DLPythonUtils.toPython(m_recurrentActivation.value()));
        namedParams.put("use_bias", DLPythonUtils.toPython(m_useBias));
        namedParams.put("kernel_initializer", m_kernelInitializer.getBackendRepresentation());
        namedParams.put("recurrent_initializer", m_recurrentInitializer.getBackendRepresentation());
        namedParams.put("bias_initializer", m_biasInitializer.getBackendRepresentation());
        namedParams.put("unit_forget_bias", DLPythonUtils.toPython(m_unitForgetBias));
        namedParams.put("kernel_regularizer", DLKerasUtils.Layers.toPython(m_kernelRegularizer));
        namedParams.put("recurrent_regularizer", DLKerasUtils.Layers.toPython(m_recurrentRegularizer));
        namedParams.put("bias_regularizer", DLKerasUtils.Layers.toPython(m_biasRegularizer));
        namedParams.put("activity_regularizer", DLKerasUtils.Layers.toPython(m_activityRegularizer));
        namedParams.put("kernel_constraint", DLKerasUtils.Layers.toPython(m_kernelConstraint));
        namedParams.put("recurrent_constraint", DLKerasUtils.Layers.toPython(m_recurrentConstraint));
        namedParams.put("bias_constraint", DLKerasUtils.Layers.toPython(m_biasConstraint));
        namedParams.put("return_sequences", DLPythonUtils.toPython(m_returnSequences));
        namedParams.put("return_state", DLPythonUtils.toPython(m_returnState));
        namedParams.put("go_backwards", DLPythonUtils.toPython(m_goBackwards));
        namedParams.put("dropout", DLPythonUtils.toPython(m_dropout));
        namedParams.put("recurrent_dropout", DLPythonUtils.toPython(m_recurrentDropout));
    }
}
