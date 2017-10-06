/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
package org.knime.dl.base.nodes.executor;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.dl.base.nodes.DialogComponentIdFromPrettyStringSelection;
import org.knime.dl.core.DLNetworkSpec;
import org.knime.dl.core.execution.DLExecutionContext;
import org.knime.dl.core.execution.DLExecutionContextRegistry;

/**
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
class DLExecutorGeneralPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	private final DLExecutorGeneralConfig m_cfg;

	private final DLNetworkSpec<?> m_networkSpec;

	private final DialogComponentIdFromPrettyStringSelection m_dcBackend;

	DLExecutorGeneralPanel(final DLExecutorGeneralConfig cfg, final DLNetworkSpec<?> networkSpec)
			throws NotConfigurableException {
		super(new GridBagLayout());
		m_cfg = cfg;
		m_networkSpec = networkSpec;

		// construct panel:

		setBorder(BorderFactory.createTitledBorder("General Settings"));
		final GridBagConstraints constr = new GridBagConstraints();
		constr.gridx = 0;
		constr.gridy = 0;
		constr.weightx = 1;
		constr.anchor = GridBagConstraints.WEST;
		constr.fill = GridBagConstraints.VERTICAL;
		// execution context ("back end") selection
		m_dcBackend = new DialogComponentIdFromPrettyStringSelection(
				new SettingsModelStringArray("proxy", m_cfg.getExecutionContext()), "Back end", (e) -> {
					final String[] newExecCtx =
							((DialogComponentIdFromPrettyStringSelection) e.getSource()).getSelection();
					m_cfg.setExecutionContext(newExecCtx[0], newExecCtx[1]);
				});
		add(m_dcBackend.getComponentPanel(), constr);
		constr.gridy++;
		// batch size input
		final DialogComponentNumber cdBatchSize =
				new DialogComponentNumber(m_cfg.getBatchSizeModel(), "Input batch size", 100);
		add(cdBatchSize.getComponentPanel(), constr);
		constr.gridy++;
		final DialogComponentBoolean appendColumnComponent =
				new DialogComponentBoolean(m_cfg.getKeepInputColumnsModel(), "Keep input columns in output table");
		add(appendColumnComponent.getComponentPanel(), constr);
	}

	void loadFromSettings(final NodeSettingsRO settings, final PortObjectSpec[] specs) throws NotConfigurableException {
		try {
			m_cfg.loadFromSettings(settings);
		} catch (final InvalidSettingsException e) {
			// ignore
		}
		refreshAvailableBackends();
	}

	void saveToSettings(final NodeSettingsWO settings) {
		m_cfg.saveToSettings(settings);
	}

	void refreshAvailableBackends() throws NotConfigurableException {
		final List<DLExecutionContext<?>> availableExecutionContexts = DLExecutionContextRegistry.getInstance()
				.getExecutionContextsForNetworkType((m_networkSpec.getNetworkType())) //
				.stream() //
				.sorted(Comparator.comparing(DLExecutionContext::getName)) //
				.collect(Collectors.toList());
		if (availableExecutionContexts.size() == 0) {
			throw new NotConfigurableException("There is no available back end that supports the input network.");
		}
		final String[] names = new String[availableExecutionContexts.size()];
		final String[] ids = new String[availableExecutionContexts.size()];
		for (int i = 0; i < availableExecutionContexts.size(); i++) {
			final DLExecutionContext<?> executionContext = availableExecutionContexts.get(i);
			names[i] = executionContext.getName();
			ids[i] = executionContext.getIdentifier();
		}
		final String selectedName = m_cfg.getExecutionContext()[1] != null ? m_cfg.getExecutionContext()[0] : names[0];
		m_dcBackend.replaceListItems(names, ids, selectedName);
	}
}
