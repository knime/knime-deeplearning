/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 *
 * History
 *   Sep 25, 2014 (Patrick Winter): created
 */
package org.knime.dl.python.base.node.editor;

import java.io.IOException;

import org.knime.core.node.DataAwareNodeDialogPane;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.NodeContext;
import org.knime.dl.core.DLInvalidSourceException;
import org.knime.dl.python.base.node.DLPythonSourceCodePanel;
import org.knime.dl.python.core.DLPythonDefaultContext;
import org.knime.dl.python.core.DLPythonNetwork;
import org.knime.dl.python.core.DLPythonNetworkPortObject;
import org.knime.python2.config.PythonSourceCodeOptionsPanel;
import org.knime.python2.config.PythonSourceCodeOptionsPanel.EnforcePythonVersion;
import org.knime.python2.config.WorkspacePreparer;
import org.knime.python2.kernel.FlowVariableOptions;

/**
 * Shamelessly copied and pasted from python predictor.
 *
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
final class DLPythonEditorNodeDialog extends DataAwareNodeDialogPane {

	private final DLPythonSourceCodePanel m_sourceCodePanel;

	private final PythonSourceCodeOptionsPanel m_sourceCodeOptionsPanel;

	private WorkspacePreparer m_workspacePreparer;

	DLPythonEditorNodeDialog() {
		m_sourceCodePanel = new DLPythonSourceCodePanel(DLPythonEditorNodeConfig.getVariableNames(),
				FlowVariableOptions.parse(getAvailableFlowVariables()));
		m_sourceCodeOptionsPanel = new PythonSourceCodeOptionsPanel(m_sourceCodePanel, EnforcePythonVersion.PYTHON3);
		addTab("Script", m_sourceCodePanel, false);
		addTab("Options", m_sourceCodeOptionsPanel, true);
	}

	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
		final DLPythonEditorNodeConfig config = new DLPythonEditorNodeConfig();
		m_sourceCodePanel.saveSettingsTo(config);
		m_sourceCodeOptionsPanel.saveSettingsTo(config);
		config.saveTo(settings);
	}

	@Override
	protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
			throws NotConfigurableException {
		final DLPythonEditorNodeConfig config = new DLPythonEditorNodeConfig();
		config.loadFromInDialog(settings);
		m_sourceCodePanel.loadSettingsFrom(config, specs);
		m_sourceCodePanel.updateFlowVariables(
				getAvailableFlowVariables().values().toArray(new FlowVariable[getAvailableFlowVariables().size()]));
		m_sourceCodeOptionsPanel.loadSettingsFrom(config);
	}

	@Override
	protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObject[] input)
			throws NotConfigurableException {
		final DLPythonNetworkPortObject<?> portObject =
				(DLPythonNetworkPortObject<?>) input[DLPythonEditorNodeModel.IN_NETWORK_PORT_IDX];
		if (portObject == null) {
			throw new NotConfigurableException("Input deep learning network port object is missing.");
		}
		final DLPythonNetwork network;
		try {
			network = portObject.getNetwork();
		} catch (final DLInvalidSourceException | IOException e) {
			throw new NotConfigurableException(e.getMessage());
		}
		if (!(network instanceof DLPythonNetwork)) {
			throw new NotConfigurableException("Input deep learning network is not Python compatible.");
		}

		final PortObjectSpec[] specs = new PortObjectSpec[input.length];
		for (int i = 0; i < specs.length; i++) {
			specs[i] = input[i] == null ? null : input[i].getSpec();
		}
		loadSettingsFrom(settings, specs);

		if (m_workspacePreparer != null) {
			m_sourceCodePanel.unregisterWorkspacePreparer(m_workspacePreparer);
		}
		m_workspacePreparer = kernel -> {
			try {
				NodeContext.pushContext(DLPythonEditorNodeDialog.this.getNodeContext());
				DLPythonEditorNodeModel.setupNetwork(network, new DLPythonDefaultContext(kernel));
				m_sourceCodePanel.updateVariables();
			} catch (final Exception e) {
				m_sourceCodePanel.errorToConsole(
						"Deep Learning network could not be loaded. Try again by pressing the \"Reset workspace\" button.");
			}
		};
		m_sourceCodePanel.registerWorkspacePreparer(m_workspacePreparer);
	}

	@Override
	public boolean closeOnESC() {
		return false;
	}

	@Override
	public void onOpen() {
		m_sourceCodePanel.open();
	}

	@Override
	public void onClose() {
		m_sourceCodePanel.close();
	}
}
