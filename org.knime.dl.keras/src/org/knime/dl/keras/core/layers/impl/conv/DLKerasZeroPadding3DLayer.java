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
import org.knime.dl.keras.core.layers.DLInputSpecValidationUtils;
import org.knime.dl.keras.core.layers.DLInvalidTensorSpecException;
import org.knime.dl.keras.core.layers.DLKerasAbstractUnaryLayer;
import org.knime.dl.keras.core.layers.DLKerasDataFormat;
import org.knime.dl.keras.core.struct.param.Parameter;
import org.knime.dl.python.util.DLPythonUtils;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 */
public final class DLKerasZeroPadding3DLayer extends DLKerasAbstractUnaryLayer {

    @Parameter(label = "Padding Dimension 1")
    private String m_paddingDim1 = "0, 0";

    @Parameter(label = "Padding Dimension 2")
    private String m_paddingDim2 = "0, 0";

    @Parameter(label = "Padding Dimension 3")
    private String m_paddingDim3 = "0, 0";

    @Parameter(label = "Data Format")
    private DLKerasDataFormat m_dataFormat = DLKerasDataFormat.CHANNEL_LAST;

    /**
     * Constructor
     */
    public DLKerasZeroPadding3DLayer() {
        super("keras.layers.ZeroPadding3D");
    }

    @Override
    public void validateParameters() throws InvalidSettingsException {
        DLConvolutionLayerUtils.validateTupleStrings(new String[]{m_paddingDim1, m_paddingDim2, m_paddingDim3},
            new String[]{"Padding Dimension 1", "Padding Dimension 2", "Padding Dimension 3"}, 2, true);
    }

    @Override
    protected void validateInputSpec(final Class<?> inputElementType, final Long[] inputShape)
        throws DLInvalidTensorSpecException {
        DLInputSpecValidationUtils.validateInputRank(inputShape, 4);
    }

    @Override
    protected Long[] inferOutputShape(final Long[] inputShape) {
        final Long[][] padding =
            DLConvolutionLayerUtils.parseCroppingOrPadding(m_paddingDim1, m_paddingDim2, m_paddingDim3);
        return DLConvolutionLayerUtils.computePaddingOutputShape(inputShape, padding, m_dataFormat);
    }

    @Override
    protected void populateParameters(final List<String> positionalParams, final Map<String, String> namedParams) {
        final Long[][] padding =
            DLConvolutionLayerUtils.parseCroppingOrPadding(m_paddingDim1, m_paddingDim2, m_paddingDim3);
        namedParams.put("padding", DLPythonUtils.toPython(padding));
        namedParams.put("data_format", DLPythonUtils.toPython(m_dataFormat.value()));
    }
}
