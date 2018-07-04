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
package org.knime.dl.keras.core.layers.impl.locally;

import java.util.List;
import java.util.Map;

import org.knime.core.node.InvalidSettingsException;
import org.knime.dl.keras.core.config.DLKerasConfigObjectUtils;
import org.knime.dl.keras.core.config.activation.DLKerasActivation;
import org.knime.dl.keras.core.config.constraint.DLKerasConstraint;
import org.knime.dl.keras.core.config.constraint.DLKerasConstraintChoices;
import org.knime.dl.keras.core.config.initializer.DLKerasGlorotUniformInitializer;
import org.knime.dl.keras.core.config.initializer.DLKerasInitializer;
import org.knime.dl.keras.core.config.initializer.DLKerasInitializerChoices;
import org.knime.dl.keras.core.config.initializer.DLKerasZerosInitializer;
import org.knime.dl.keras.core.config.regularizer.DLKerasRegularizer;
import org.knime.dl.keras.core.config.regularizer.DLKerasRegularizerChoices;
import org.knime.dl.keras.core.layers.DLConvolutionLayerUtils;
import org.knime.dl.keras.core.layers.DLInputShapeValidationUtils;
import org.knime.dl.keras.core.layers.DLInvalidTensorSpecException;
import org.knime.dl.keras.core.layers.DLKerasAbstractUnaryLayer;
import org.knime.dl.keras.core.layers.DLKerasDataFormat;
import org.knime.dl.keras.core.layers.DLKerasPadding;
import org.knime.dl.keras.core.layers.DLLayerUtils;
import org.knime.dl.keras.core.layers.DLParameterValidationUtils;
import org.knime.dl.keras.core.struct.param.Required;
import org.knime.dl.keras.core.struct.param.Parameter;
import org.knime.dl.python.util.DLPythonUtils;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 * @author David Kolb, KNIME GmbH, Konstanz, Germany
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 */
public final class DLKerasLocallyConnected1DLayer extends DLKerasAbstractUnaryLayer {

    @Parameter(label = "Filters", min = "1")
    private int m_filters = 1;

    @Parameter(label = "Kernel size", min = "1")
    private int m_kernelSize = 1;

    @Parameter(label = "Strides", min = "1")
    private int m_strides = 1;

    /**
     * Only "valid" is allowed for Locally Connected in Keras
     */
    private DLKerasPadding m_padding = DLKerasPadding.VALID;

    /**
     * Effectively 'channels last' in Keras
     */
    private DLKerasDataFormat m_dataFormat = DLKerasDataFormat.CHANNEL_LAST;

    @Parameter(label = "Activation function")
    private DLKerasActivation m_activation = DLKerasActivation.LINEAR;

    @Parameter(label = "Use bias?", tab = "Advanced")
    boolean m_useBias = true;

    @Parameter(label = "Kernel initializer", choices = DLKerasInitializerChoices.class, tab = "Advanced")
    private DLKerasInitializer m_kernelInitializer = new DLKerasGlorotUniformInitializer();

    @Parameter(label = "Bias initializer", choices = DLKerasInitializerChoices.class, tab = "Advanced")
    private DLKerasInitializer m_biasInitializer = new DLKerasZerosInitializer();

    @Parameter(label = "Kernel regularizer", required = Required.OptionalAndNotEnabled, choices = DLKerasRegularizerChoices.class, tab = "Advanced")
    private DLKerasRegularizer m_kernelRegularizer = null;

    @Parameter(label = "Bias regularizer", required = Required.OptionalAndNotEnabled, choices = DLKerasRegularizerChoices.class, tab = "Advanced")
    private DLKerasRegularizer m_biasRegularizer = null;

    @Parameter(label = "Activity regularizer", required = Required.OptionalAndNotEnabled, choices = DLKerasRegularizerChoices.class, tab = "Advanced")
    private DLKerasRegularizer m_activityRegularizer = null;

    @Parameter(label = "Kernel constraint", required = Required.OptionalAndNotEnabled, choices = DLKerasConstraintChoices.class, tab = "Advanced")
    private DLKerasConstraint m_kernelConstraint = null;

    @Parameter(label = "Bias constraint", required = Required.OptionalAndNotEnabled, choices = DLKerasConstraintChoices.class, tab = "Advanced")
    private DLKerasConstraint m_biasConstraint = null;

    /**
     * Constructor
     */
    public DLKerasLocallyConnected1DLayer() {
        super("keras.layers.LocallyConnected1D", DLLayerUtils.FLOATING_POINT_DTYPES);
    }

    @Override
    public void validateParameters() throws InvalidSettingsException {
        // nothing to do
    }

    @Override
    protected void validateInputShape(final Long[] inputShape)
        throws DLInvalidTensorSpecException {
        DLInputShapeValidationUtils.dimsExactly(inputShape, 2);

        final String message =
            DLParameterValidationUtils.checkConvolutionOutputGreaterThanZero(inferOutputShape(inputShape));
        checkInputSpec(message == null, message);
    }

    @Override
    protected Long[] inferOutputShape(final Long[] inputShape) {
        final Long[] kernelSize = {new Long(m_kernelSize)};
        final Long[] strides = {new Long(m_strides)};
        return DLConvolutionLayerUtils.computeOutputShape(inputShape, m_filters, kernelSize, strides,
            DLConvolutionLayerUtils.DEFAULT_1D_DILATION, m_padding.value(), m_dataFormat.value());
    }

    @Override
    protected void populateParameters(final List<String> positionalParams, final Map<String, String> namedParams) {
        namedParams.put("filters", DLPythonUtils.toPython(m_filters));
        namedParams.put("kernel_size", DLPythonUtils.toPython(m_kernelSize));
        namedParams.put("strides", DLPythonUtils.toPython(m_strides));
        namedParams.put("padding", DLPythonUtils.toPython(m_padding.value()));
        namedParams.put("data_format", DLPythonUtils.toPython(m_dataFormat.value()));
        namedParams.put("activation", DLPythonUtils.toPython(m_activation.value()));
        namedParams.put("use_bias", DLPythonUtils.toPython(m_useBias));
        namedParams.put("kernel_initializer", DLKerasConfigObjectUtils.toPython(m_kernelInitializer));
        namedParams.put("bias_initializer", DLKerasConfigObjectUtils.toPython(m_biasInitializer));
        namedParams.put("kernel_regularizer", DLKerasConfigObjectUtils.toPython(m_kernelRegularizer));
        namedParams.put("bias_regularizer", DLKerasConfigObjectUtils.toPython(m_biasRegularizer));
        namedParams.put("activity_regularizer", DLKerasConfigObjectUtils.toPython(m_activityRegularizer));
        namedParams.put("kernel_constraint", DLKerasConfigObjectUtils.toPython(m_kernelConstraint));
        namedParams.put("bias_constraint", DLKerasConfigObjectUtils.toPython(m_biasConstraint));
    }
}
