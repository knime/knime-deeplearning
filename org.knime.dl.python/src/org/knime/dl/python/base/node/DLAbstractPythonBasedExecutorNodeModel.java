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
 * History
 *   Nov 21, 2020 (marcel): created
 */
package org.knime.dl.python.base.node;

import java.util.function.Supplier;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortType;
import org.knime.dl.base.nodes.executor2.DLAbstractExecutorNodeModel;
import org.knime.dl.core.execution.DLExecutionContext;
import org.knime.dl.python.core.DLPythonContext;
import org.knime.dl.python.core.DLPythonDefaultContext;
import org.knime.dl.python.prefs.DLPythonPreferences;
import org.knime.python2.PythonCommand;
import org.knime.python2.PythonVersion;
import org.knime.python2.config.PythonCommandFlowVariableConfig;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 */
public abstract class DLAbstractPythonBasedExecutorNodeModel extends DLAbstractExecutorNodeModel<DLPythonContext> {

    static PythonCommandFlowVariableConfig createPythonCommandConfig() {
        return new PythonCommandFlowVariableConfig(PythonVersion.PYTHON3,
            DLPythonPreferences::getCondaInstallationPath);
    }

    private final PythonCommandFlowVariableConfig m_pythonCommandConfig = createPythonCommandConfig();

    private final Supplier<PythonCommand> m_commandPreference;

    public DLAbstractPythonBasedExecutorNodeModel(final PortType networkPortType,
        final Supplier<PythonCommand> commandPreference) {
        super(networkPortType);
        m_commandPreference = commandPreference;
    }

    @Override
    protected DLPythonContext getContext(final DLExecutionContext<?, ?> ctx) {
        return new DLPythonDefaultContext(m_pythonCommandConfig.getCommand().orElseGet(m_commandPreference));
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        super.saveSettingsTo(settings);
        m_pythonCommandConfig.saveSettingsTo(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_pythonCommandConfig.validateSettings(settings);
        super.validateSettings(settings);
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_pythonCommandConfig.loadSettingsFrom(settings);
        super.loadValidatedSettingsFrom(settings);
    }
}
