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
import org.knime.dl.keras.core.layers.DLConvolutionLayerUtils;
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
public final class DLKerasConv2DLayer extends DLKerasAbstractUnaryLayer {

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

    // TODO USE CHOICES
    //    @Parameter(label = "Activation function", strings = {"elu", "hard_sigmoid", "linear", "relu", "selu", "sigmoid",
    //        "softmax", "softplus", "softsign", "tanh"})
    private String m_activation = "linear";

    // TODO initializers

    // TODO regularizers

    // TODO constraints

    @Parameter(label = "Use bias?")
    boolean m_useBias = true;

    /**
     * Constructor
     */
    public DLKerasConv2DLayer() {
        super("keras.layers.Conv2D");
    }

    @Override
    public void validateParameters() throws InvalidSettingsException {
        DLConvolutionLayerUtils.validateTupleStrings(new String[]{m_kernelSize, m_strides, m_dilationRate},
            new String[]{"Pool size", "Strides", "Dilation Rate"}, 2, false);
    }

    @Override
    protected void validateInputSpec(final Class<?> inputElementType, final Long[] inputShape)
        throws DLInvalidTensorSpecException {
        // TODO check input shape
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
        namedParams.put("kernel_size", DLPythonUtils.toPython(m_kernelSize));
        namedParams.put("strides", DLPythonUtils.toPython(m_strides));
        namedParams.put("padding", DLPythonUtils.toPython(m_padding.value()));
        namedParams.put("data_format", DLPythonUtils.toPython(m_dataFormat.value()));
        namedParams.put("dilation_rate", DLPythonUtils.toPython(m_dilationRate));
        namedParams.put("activation", DLPythonUtils.toPython(m_activation));
        namedParams.put("use_bias", DLPythonUtils.toPython(m_useBias));
        // TODO initializers
        // TODO regularizers
        // TODO constraints
    }
}
