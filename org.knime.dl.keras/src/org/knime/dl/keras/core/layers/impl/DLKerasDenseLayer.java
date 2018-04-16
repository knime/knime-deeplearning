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
public final class DLKerasDenseLayer extends DLKerasAbstractUnaryInnerLayer {

    @Parameter(label = "Units", min = "1", max = "1000000", stepSize = "1")
    long m_units = 1;

    @Parameter(label = "Activation function", choices = {"elu", "hard_sigmoid", "linear", "relu", "selu", "sigmoid",
        "softmax", "softplus", "softsign", "tanh"})
    String m_activation = "linear";

    @Parameter(label = "Use bias?")
    boolean m_useBias = true;

    public DLKerasDenseLayer() {
        super("keras.layers.Dense");
    }

    @Override
    public void validateParameters() throws InvalidSettingsException {
        // TODO
    }

    @Override
    protected void validateInputSpec(final Class<?> inputElementType, final Long[] inputShape)
        throws DLInvalidTensorSpecException {
        checkInputSpec(inputShape.length >= 1, "Input shape must be at least one-dimensional.");
        checkInputSpec(inputShape[inputShape.length - 1] != null,
            "Last dimension of input shape must be defined two-dimensional.");
    }

    @Override
    protected Class<?> inferOutputElementType(final Class<?> inputElementType) {
        return inputElementType;
    }

    @Override
    protected Long[] inferOutputShape(final Long[] inputShape) {
        final Long[] outputShape = inputShape.clone();
        outputShape[outputShape.length - 1] = m_units;
        return outputShape;
    }

    @Override
    protected void populateParameters(final List<String> positionalParams, final Map<String, String> namedParams) {
        positionalParams.add(DLPythonUtils.toPython(m_units));
        namedParams.put("activation", DLPythonUtils.toPython(m_activation));
        namedParams.put("use_bias", DLPythonUtils.toPython(m_useBias));
    }
}
