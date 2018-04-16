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
package org.knime.dl.keras.core.layers.impl;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.knime.core.node.InvalidSettingsException;
import org.knime.dl.keras.core.layers.DLInvalidTensorSpecException;
import org.knime.dl.keras.core.layers.DLKerasAbstractUnaryInnerLayer;
import org.knime.dl.python.util.DLPythonUtils;
import org.scijava.param2.Parameter;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public final class DLKerasConv2DLayer extends DLKerasAbstractUnaryInnerLayer {

    @Parameter(label = "Filters", min = "1", max = "1000000", stepSize = "1")
    int m_filters = 1;

    @Parameter(label = "Kernel size")
    String m_kernelSize = "1, 1";

    @Parameter(label = "Strides")
    String m_strides = "1, 1";

    @Parameter(label = "Padding", choices = {"same", "valid"})
    String m_padding = "valid";

    @Parameter(label = "Activation function", choices = {"elu", "hard_sigmoid", "linear", "relu", "selu", "sigmoid",
        "softmax", "softplus", "softsign", "tanh"})
    String m_activation = "linear";

    @Parameter(label = "Use bias?")
    boolean m_useBias = true;

    public DLKerasConv2DLayer() {
        super("keras.layers.Conv2D");
    }

    @Override
    public void validateParameters() throws InvalidSettingsException {
        throw new RuntimeException("not yet implemented"); // TODO: NYI
    }

    @Override
    protected void validateInputSpec(final Class<?> inputElementType, final Long[] inputShape)
        throws DLInvalidTensorSpecException {
        throw new RuntimeException("not yet implemented"); // TODO: NYI
    }

    @Override
    protected Class<?> inferOutputElementType(final Class<?> inputElementType) {
        throw new RuntimeException("not yet implemented"); // TODO: NYI
    }

    @Override
    protected Long[] inferOutputShape(final Long[] inputShape) {
        throw new RuntimeException("not yet implemented"); // TODO: NYI
    }

    @Override
    protected void populateParameters(final List<String> positionalParams, final Map<String, String> namedParams) {
        positionalParams.add(DLPythonUtils.toPython(m_filters));
        final int[] kernelSize = getKernelSize();
        positionalParams.add(kernelSize.length == 1 //
            ? DLPythonUtils.toPython(kernelSize[0]) : DLPythonUtils.toPython(kernelSize));
        final int[] strides = getStrides();
        namedParams.put("strides", strides.length == 1 //
            ? DLPythonUtils.toPython(strides[0]) : DLPythonUtils.toPython(strides));
        namedParams.put("padding", DLPythonUtils.toPython(m_padding));
        namedParams.put("activation", DLPythonUtils.toPython(m_activation));
        namedParams.put("use_bias", DLPythonUtils.toPython(m_useBias));
    }

    private int[] getKernelSize() {
        return Arrays.stream(m_kernelSize.split(",")).mapToInt(Integer::parseInt).toArray();
    }

    private int[] getStrides() {
        return Arrays.stream(m_strides.split(",")).mapToInt(Integer::parseInt).toArray();
    }
}
