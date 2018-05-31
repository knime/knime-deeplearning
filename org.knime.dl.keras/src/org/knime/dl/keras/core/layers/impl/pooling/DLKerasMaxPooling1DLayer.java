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
package org.knime.dl.keras.core.layers.impl.pooling;

import java.util.List;
import java.util.Map;

import org.knime.core.node.InvalidSettingsException;
import org.knime.dl.keras.core.layers.DLConvolutionLayerUtils;
import org.knime.dl.keras.core.layers.DLInvalidTensorSpecException;
import org.knime.dl.keras.core.layers.DLKerasAbstractUnaryLayer;
import org.knime.dl.keras.core.layers.DLParameterValidationUtils;
import org.knime.dl.keras.core.struct.param.Parameter;
import org.knime.dl.python.util.DLPythonUtils;

/**
 * @author David Kolb, KNIME GmbH, Konstanz, Germany
 */
public final class DLKerasMaxPooling1DLayer extends DLKerasAbstractUnaryLayer {

    @Parameter(label = "Pool size")
    private String m_poolSize = "2";

    @Parameter(label = "Strides")
    private String m_strides = "1";

    /**
     * This is hard-coded to "channels_last" in Keras
     */
    private String m_dataFormat = "channels_last";

    @Parameter(label = "Padding", strings = {"valid", "same", "full"})
    private String m_padding = "valid";

    public DLKerasMaxPooling1DLayer() {
        super("keras.layers.MaxPooling1D");
    }

    @Override
    public void validateParameters() throws InvalidSettingsException {
        DLConvolutionLayerUtils.validateTupleStrings(new String[]{m_poolSize, m_strides},
            new String[]{"Pool size", "Strides"}, 1);
        DLParameterValidationUtils.checkContains(m_dataFormat, DLConvolutionLayerUtils.DATA_FORMATS, "data format");
        DLParameterValidationUtils.checkContains(m_padding, DLConvolutionLayerUtils.PADDINGS, "data format");
    }

    @Override
    protected void validateInputSpec(final Class<?> inputElementType, final Long[] inputShape)
        throws DLInvalidTensorSpecException {
        // nothing to do here
    }

    @Override
    protected Long[] inferOutputShape(final Long[] inputShape) {
        Long[] poolSize = DLPythonUtils.parseShape(m_poolSize);
        Long[] strides = DLPythonUtils.parseShape(m_strides);
        return DLConvolutionLayerUtils.computeOutputShape(inputShape, poolSize, strides,
            DLConvolutionLayerUtils.DEFAULT_1D_DILATION, m_padding, m_dataFormat);
    }

    @Override
    protected void populateParameters(final List<String> positionalParams, final Map<String, String> namedParams) {
        namedParams.put("pool_size", DLPythonUtils.toPython(m_poolSize));
        namedParams.put("strides", DLPythonUtils.toPython(m_strides));
        namedParams.put("data_format", DLPythonUtils.toPython(m_dataFormat));
        namedParams.put("padding", DLPythonUtils.toPython(m_padding));
    }
}