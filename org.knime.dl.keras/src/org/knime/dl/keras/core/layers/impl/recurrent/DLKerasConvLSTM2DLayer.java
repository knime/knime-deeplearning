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
import java.util.stream.IntStream;

import org.knime.core.node.InvalidSettingsException;
import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.keras.core.config.activation.DLKerasActivation;
import org.knime.dl.keras.core.layers.DLConvolutionLayerUtils;
import org.knime.dl.keras.core.layers.DLInvalidTensorSpecException;
import org.knime.dl.keras.core.layers.DLKerasDataFormat;
import org.knime.dl.keras.core.layers.DLKerasPadding;
import org.knime.dl.keras.core.layers.dialog.tuple.DLKerasTuple;
import org.knime.dl.keras.core.layers.dialog.tuple.DLKerasTuple.Constraint;
import org.knime.dl.keras.core.struct.param.Parameter;
import org.knime.dl.python.util.DLPythonUtils;

/**
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class DLKerasConvLSTM2DLayer extends DLKerasAbstractRNNLayer {

    /**
     * 
     */
    private static final int DEFAULT_FILTERS = 100;

    /**
     * 
     */
    private static final int INPUT_RANK = 4;

    /**
     * 
     */
    private static final int HIDDEN_STATE_RANK = 3;

    /**
     * 
     */
    private static final int SPATIAL_DIMENSIONS = 2;

    /**
     * 
     */
    private static final int NUM_HIDDEN_STATES = 2;

    @Parameter(label = "Input tensor", min = "0")
    private DLTensorSpec m_inputTensor = null;

    @Parameter(label = "First hidden state tensor", min = "1")
    private DLTensorSpec m_hiddenStateTensor1 = null;

    @Parameter(label = "Second hidden state tensor", min = "2")
    private DLTensorSpec m_hiddenStateTensor2 = null;

    @Parameter(label = "Filters", min = "1", stepSize = "1")
    private int m_filters = DEFAULT_FILTERS;

    @Parameter(label = "Kernel size")
    private DLKerasTuple m_kernelSize =
        new DLKerasTuple("3, 3", SPATIAL_DIMENSIONS, SPATIAL_DIMENSIONS, EnumSet.noneOf(Constraint.class));

    @Parameter(label = "Strides")
    private DLKerasTuple m_strides =
        new DLKerasTuple("1, 1", SPATIAL_DIMENSIONS, SPATIAL_DIMENSIONS, EnumSet.noneOf(Constraint.class));

    @Parameter(label = "Padding")
    private DLKerasPadding m_padding = DLKerasPadding.SAME;

    @Parameter(label = "Data format")
    private DLKerasDataFormat m_dataFormat = DLKerasDataFormat.CHANNEL_LAST;

    @Parameter(label = "Dilation rate")
    private DLKerasTuple m_dilationRate =
        new DLKerasTuple("1, 1", SPATIAL_DIMENSIONS, SPATIAL_DIMENSIONS, EnumSet.noneOf(Constraint.class));

    @Parameter(label = "Activation")
    private DLKerasActivation m_activation = DLKerasActivation.TANH;

    @Parameter(label = "Recurrent activation")
    private DLKerasActivation m_recurrentActivation = DLKerasActivation.HARD_SIGMOID;

    @Parameter(label = "Use bias")
    private boolean m_useBias = true;

    @Parameter(label = "Unit forget bias", tab = "Initializers")
    private boolean m_unitForgetBias = true;

    @Parameter(label = "Return sequences")
    private boolean m_returnSequences = false;

    @Parameter(label = "Return state")
    private boolean m_returnState = false;
    // TODO add parameter for stateful once we support stateful execution (and learning)
    
    @Parameter(label = "Go backwards")
    private boolean m_goBackwards = false;

    @Parameter(label = "Dropout", min = "0.0", max = "1.0", stepSize = "0.1")
    private float m_dropout = 0.0f;

    @Parameter(label = "Recurrent dropout", min = "0.0", max = "1.0", stepSize = "0.1")
    private float m_recurrentDropout = 0.0f;
    

    /**
     * Constructor for {@link DLKerasConvLSTM2DLayer}s.
     */
    public DLKerasConvLSTM2DLayer() {
        super("keras.layers.ConvLSTM2D", NUM_HIDDEN_STATES);
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
    protected void validateInputShapes(List<Long[]> inputShapes) throws DLInvalidTensorSpecException {
        Long[] inputShape = inputShapes.get(0);
        checkInputSpec(inputShape.length == INPUT_RANK, "Expected an input with shape " + getShapeString(true) + ".");
        int channelIdx = m_dataFormat == DLKerasDataFormat.CHANNEL_FIRST ? 1 : INPUT_RANK - 1;
        checkInputSpec(inputShape[channelIdx] != null, "The number of channels must be defined for the input.");
        if (m_padding == DLKerasPadding.VALID) {
            checkInputSpec(Arrays.stream(getSpatialOutputShape(inputShape)).allMatch(d -> d == null || d > 0),
                "The kernel size may not exceed the height and width in case of valid padding.");
        }
        int numStateInputs = inputShapes.size() - 1;
        if (numStateInputs > 0) {
            checkInputSpec(numStateInputs == SPATIAL_DIMENSIONS,
                "Expected 2 state inputs but received " + numStateInputs + ".");
            checkInputSpec(inputShapes.stream().skip(1).allMatch(s -> s.length == HIDDEN_STATE_RANK),
                "Expected state input with shape " + getShapeString(false));
            checkInputSpec(inputShapes.stream().skip(1).allMatch(s -> s[channelIdx - 1] == m_filters),
                "The channels of the initial shape must match the shape of the internal state.");
        }
    }

    @Override
    protected Long[] getOutputShape(Long[] inputShape) {
        return getOutputShape(inputShape, m_returnSequences);
    }
    
    @Override
    protected Long[] getStateShape(Long[] inputShape) {
        return getOutputShape(inputShape, false);
    }
    
    private Long[] getOutputShape(Long[] inputShape, boolean sequence) {
        Long[] convShape = getSpatialOutputShape(inputShape);
        Long[] outShape;
        if (sequence) {
            outShape = new Long[inputShape.length];
            outShape[0] = inputShape[0];
            IntStream.range(1, outShape.length).forEach(i -> outShape[i] = convShape[i - 1]);
        } else {
            outShape = convShape;
        }
        return outShape;
    }

    private Long[] getSpatialOutputShape(Long[] inputShape) {
        Long[] convShape = Arrays.stream(inputShape).skip(1).toArray(Long[]::new);
        return DLConvolutionLayerUtils.computeOutputShape(convShape, m_filters, m_kernelSize.getTuple(),
            m_strides.getTuple(), m_dilationRate.getTuple(), m_padding.value(), m_dataFormat.value());
    }

    private String getShapeString(boolean withTime) {
        String spatial = "height, width";
        if (m_dataFormat == DLKerasDataFormat.CHANNEL_FIRST) {
            return withTime ? "[time, channel, " + spatial + "]" : "[channel, " + spatial + "]";
        } else {
            return withTime ? "[time, " + spatial + ", channel]" : "[" + spatial + ", channel]";
        }
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
        namedParams.put("unit_forget_bias", DLPythonUtils.toPython(m_unitForgetBias));
        namedParams.put("go_backwards", DLPythonUtils.toPython(m_goBackwards));
        namedParams.put("dropout", DLPythonUtils.toPython(m_dropout));
        namedParams.put("recurrent_dropout", DLPythonUtils.toPython(m_recurrentDropout));
        super.populateParameters(positionalParams, namedParams);
    }

    @Override
    public DLTensorSpec getInputTensorSpec(int index) {
        if (index == 0) {
            return m_inputTensor;
        } else if (index == 1) {
            return m_hiddenStateTensor1;
        } else if (index == SPATIAL_DIMENSIONS) {
            return m_hiddenStateTensor2;
        } else {
            throw new IllegalArgumentException("This layer has only 3 possible input ports.");
        }
    }

    @Override
    public void setInputTensorSpec(int index, DLTensorSpec inputTensorSpec) {
        if (index == 0) {
            m_inputTensor = inputTensorSpec;
        } else if (index == 1) {
            m_hiddenStateTensor1 = inputTensorSpec;
        } else if (index == SPATIAL_DIMENSIONS) {
            m_hiddenStateTensor2 = inputTensorSpec;
        } else {
            throw new IllegalArgumentException("This layer has only 3 possible input ports.");
        }
    }

    @Override
    protected boolean returnState() {
        return m_returnState;
    }

    @Override
    protected boolean returnSequences() {
        return m_returnSequences;
    }

}
