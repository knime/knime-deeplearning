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
package org.knime.dl.keras.core.layers.impl.conv;

import java.util.List;
import java.util.Map;

import org.knime.core.node.InvalidSettingsException;
import org.knime.dl.keras.core.config.DLKerasConfigObjectUtils;
import org.knime.dl.keras.core.config.activation.DLKerasActivation;
import org.knime.dl.keras.core.config.activation.DLKerasActivationChoices;
import org.knime.dl.keras.core.config.activation.DLKerasLinearActivation;
import org.knime.dl.keras.core.config.constraint.DLKerasConstraint;
import org.knime.dl.keras.core.config.constraint.DLKerasConstraintChoices;
import org.knime.dl.keras.core.config.initializer.DLKerasGlorotUniformInitializer;
import org.knime.dl.keras.core.config.initializer.DLKerasInitializer;
import org.knime.dl.keras.core.config.initializer.DLKerasInitializerChoices;
import org.knime.dl.keras.core.config.initializer.DLKerasZerosInitializer;
import org.knime.dl.keras.core.config.regularizer.DLKerasRegularizer;
import org.knime.dl.keras.core.config.regularizer.DLKerasRegularizerChoices;
import org.knime.dl.keras.core.layers.DLConvolutionLayerUtils;
import org.knime.dl.keras.core.layers.DLInputSpecValidationUtils;
import org.knime.dl.keras.core.layers.DLInvalidTensorSpecException;
import org.knime.dl.keras.core.layers.DLKerasAbstractUnaryLayer;
import org.knime.dl.keras.core.layers.DLKerasDataFormat;
import org.knime.dl.keras.core.layers.DLKerasPadding;
import org.knime.dl.keras.core.struct.param.Parameter;
import org.knime.dl.python.util.DLPythonUtils;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 * @author David Kolb, KNIME GmbH, Konstanz, Germany
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 */
public final class DLKerasSeparableConv2DLayer extends DLKerasAbstractUnaryLayer {

    @Parameter(label = "Filters", min = "1", max = "1000000", stepSize = "1")
    private int m_filters = 1;

    @Parameter(label = "Kernel size")
    private String m_kernelSize = "1, 1";

    @Parameter(label = "Strides")
    private String m_strides = "1, 1";

    @Parameter(label = "Padding")
    private DLKerasPadding m_padding = DLKerasPadding.VALID;

    @Parameter(label = "Data Format")
    private DLKerasDataFormat m_dataFormat = DLKerasDataFormat.CHANNEL_LAST;

    @Parameter(label = "Dilation Rate")
    private String m_dilationRate = "1, 1";

    @Parameter(label = "Depth Multiplier", min = "1", max = "1000000", stepSize = "1")
    private int m_depthMultiplier = 1;

    @Parameter(label = "Activation function", choices = DLKerasActivationChoices.class)
    private DLKerasActivation m_activation = new DLKerasLinearActivation();

    @Parameter(label = "Use bias?")
    boolean m_useBias = true;

    @Parameter(label = "Depthwise Initializer", choices = DLKerasInitializerChoices.class)
    private DLKerasInitializer m_depthwiseInitializer = new DLKerasGlorotUniformInitializer();

    @Parameter(label = "Pointwise Initializer", choices = DLKerasInitializerChoices.class)
    private DLKerasInitializer m_pointwiseInitializer = new DLKerasGlorotUniformInitializer();

    @Parameter(label = "Bias Initializer", choices = DLKerasInitializerChoices.class)
    private DLKerasInitializer m_biasInitializer = new DLKerasZerosInitializer();

    @Parameter(label = "Depthwise Regularizer", required = false, choices = DLKerasRegularizerChoices.class)
    private DLKerasRegularizer m_depthwiseRegularizer = null;

    @Parameter(label = "Pointwise Regularizer", required = false, choices = DLKerasRegularizerChoices.class)
    private DLKerasRegularizer m_pointwiseRegularizer = null;

    @Parameter(label = "Bias Regularizer", required = false, choices = DLKerasRegularizerChoices.class)
    private DLKerasRegularizer m_biasRegularizer = null;

    @Parameter(label = "Activity Regularizer", required = false, choices = DLKerasRegularizerChoices.class)
    private DLKerasRegularizer m_activityRegularizer = null;

    @Parameter(label = "Depthwise Constraint", required = false, choices = DLKerasConstraintChoices.class)
    private DLKerasConstraint m_depthwiseConstraint = null;

    @Parameter(label = "Pointwise Constraint", required = false, choices = DLKerasConstraintChoices.class)
    private DLKerasConstraint m_pointwiseConstraint = null;

    @Parameter(label = "Bias Constraint", required = false, choices = DLKerasConstraintChoices.class)
    private DLKerasConstraint m_biasConstraint = null;

    /**
     * Constructor
     */
    public DLKerasSeparableConv2DLayer() {
        super("keras.layers.SeparableConv2D");
    }

    @Override
    public void validateParameters() throws InvalidSettingsException {
        DLConvolutionLayerUtils.validateTupleStrings(new String[]{m_kernelSize, m_strides, m_dilationRate},
            new String[]{"Kernel size", "Strides", "Dilation Rate"}, 2, false);
    }

    @Override
    protected void validateInputSpec(final Class<?> inputElementType, final Long[] inputShape)
        throws DLInvalidTensorSpecException {
        DLInputSpecValidationUtils.validateInputRank(inputShape, 3);
    }

    @Override
    protected Long[] inferOutputShape(final Long[] inputShape) {
        final Long[] kernelSize = DLPythonUtils.parseShape(m_kernelSize);
        final Long[] strides = DLPythonUtils.parseShape(m_strides);
        final Long[] dilationRate = DLPythonUtils.parseShape(m_dilationRate);
        return DLConvolutionLayerUtils.computeOutputShape(inputShape, kernelSize, strides, dilationRate,
            m_padding.value(), m_dataFormat.value());
    }

    @Override
    protected void populateParameters(final List<String> positionalParams, final Map<String, String> namedParams) {
        namedParams.put("filters", DLPythonUtils.toPython(m_filters));
        namedParams.put("kernel_size", DLPythonUtils.toPythonTuple(m_kernelSize));
        namedParams.put("strides", DLPythonUtils.toPythonTuple(m_strides));
        namedParams.put("padding", DLPythonUtils.toPython(m_padding.value()));
        namedParams.put("data_format", DLPythonUtils.toPython(m_dataFormat.value()));
        namedParams.put("dilation_rate", DLPythonUtils.toPythonTuple(m_dilationRate));
        namedParams.put("depth_multipier", DLPythonUtils.toPython(m_depthMultiplier));
        namedParams.put("activation", DLKerasConfigObjectUtils.toPython(m_activation));
        namedParams.put("use_bias", DLPythonUtils.toPython(m_useBias));
        namedParams.put("depthwise_initializer", DLKerasConfigObjectUtils.toPython(m_depthwiseInitializer));
        namedParams.put("pointwise_initializer", DLKerasConfigObjectUtils.toPython(m_pointwiseInitializer));
        namedParams.put("bias_initializer", DLKerasConfigObjectUtils.toPython(m_biasInitializer));
        namedParams.put("depthwise_regularizer", DLKerasConfigObjectUtils.toPython(m_depthwiseRegularizer));
        namedParams.put("pointwise_regularizer", DLKerasConfigObjectUtils.toPython(m_pointwiseRegularizer));
        namedParams.put("bias_regularizer", DLKerasConfigObjectUtils.toPython(m_biasRegularizer));
        namedParams.put("activity_regularizer", DLKerasConfigObjectUtils.toPython(m_activityRegularizer));
        namedParams.put("depthwise_constraint", DLKerasConfigObjectUtils.toPython(m_depthwiseConstraint));
        namedParams.put("pointwise_constraint", DLKerasConfigObjectUtils.toPython(m_pointwiseConstraint));
        namedParams.put("bias_constraint", DLKerasConfigObjectUtils.toPython(m_biasConstraint));
    }
}
