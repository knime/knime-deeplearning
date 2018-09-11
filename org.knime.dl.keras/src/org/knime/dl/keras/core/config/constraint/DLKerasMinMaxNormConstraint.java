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
package org.knime.dl.keras.core.config.constraint;

import java.util.List;
import java.util.Map;

import org.knime.core.node.InvalidSettingsException;
import org.knime.dl.keras.core.struct.param.Parameter;
import org.knime.dl.python.util.DLPythonUtils;

/**
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public class DLKerasMinMaxNormConstraint extends DLKerasAbstractAxisConstraint {

    @Parameter(label = "Minimum norm", min = "0.0", stepSize = "0.1")
    private float m_minValue = 0.0f;

    @Parameter(label = "Maximum norm", min = "0.0000001", stepSize = "0.0000001")
    private float m_maxValue = 1.0f;

    @Parameter(label = "Rate", min = "0.0", max = "1.0", stepSize = "0.1")
    private float m_rate = 1.0f;

    /**
     */
    public DLKerasMinMaxNormConstraint() {
        super("keras.constraints.MinMaxNorm");
    }

    @Override
    public void validateParameters() throws InvalidSettingsException {
        super.validateParameters();
        if (m_maxValue <= 0) {
            throw new InvalidSettingsException("The maximum norm must be positive.");
        }
        if (m_minValue < 0) {
            throw new InvalidSettingsException("The minimum norm must be non-negative.");
        }
        if (m_rate < 0 || m_rate > 1) {
            throw new InvalidSettingsException("The rate must be in the [0, 1] interval.");
        }
        if (m_minValue > m_maxValue) {
            throw new InvalidSettingsException(
                "The minimum value (" + m_minValue + ") must be smaller than the maximum value (" + m_maxValue + ")");
        }
    }

    @Override
    protected void populateParameters(List<String> positionalParams, Map<String, String> namedParams) {
        namedParams.put("min_value", DLPythonUtils.toPython(m_minValue));
        namedParams.put("max_value", DLPythonUtils.toPython(m_maxValue));
        namedParams.put("rate", DLPythonUtils.toPython(m_rate));
        super.populateParameters(positionalParams, namedParams);
    }

}