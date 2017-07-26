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
 * History
 *   Jun 12, 2017 (marcel): created
 */
package org.knime.dl.keras.base.nodes.reader;

import java.awt.CardLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.FilesHistoryPanel;
import org.knime.core.node.util.FilesHistoryPanel.LocationValidation;

/**
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
final class DLKerasReaderNodeDialog extends NodeDialogPane {

	private final FilesHistoryPanel m_files;

	private final SettingsModelString m_smFilePath;

	private final JPanel m_loading;

	private final CardLayout m_loadingLayout;

	public DLKerasReaderNodeDialog() {
		final JPanel filesPanel = new JPanel(new GridBagLayout());
		filesPanel.setBorder(BorderFactory.createTitledBorder("Input Location"));
		final GridBagConstraints filesPanelConstr = new GridBagConstraints();
		filesPanelConstr.gridx = 0;
		filesPanelConstr.gridy = 0;
		filesPanelConstr.weightx = 1;
		filesPanelConstr.weighty = 1;
		filesPanelConstr.anchor = GridBagConstraints.NORTHWEST;
		filesPanelConstr.fill = GridBagConstraints.BOTH;
		m_files = new FilesHistoryPanel("org.knime.dl.keras.base.nodes.reader", LocationValidation.FileInput);
		m_files.setSuffixes(DLKerasReaderNodeModel.getValidInputFileExtensions().stream().map(s -> "." + s)
				.collect(Collectors.joining("|")));
		m_smFilePath = DLKerasReaderNodeModel.createFilePathStringModel(m_files.getSelectedFile());
		m_files.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(final ChangeEvent e) {
				m_smFilePath.setStringValue(m_files.getSelectedFile());
			}
		});
		filesPanel.add(m_files, filesPanelConstr);
		filesPanelConstr.gridy++;

		m_loading = new JPanel();
		m_loadingLayout = new CardLayout();
		m_loading.setLayout(m_loadingLayout);
		m_loading.add(new JLabel("Loading network configuration..."), "label");
		m_loading.add(new JPanel(), "blank");
		m_loadingLayout.show(m_loading, "blank");
		filesPanelConstr.weighty = 0;
		filesPanelConstr.anchor = GridBagConstraints.SOUTHWEST;
		filesPanelConstr.fill = GridBagConstraints.NONE;
		filesPanelConstr.insets = new Insets(0, 5, 5, 0);
		filesPanel.add(m_loading, filesPanelConstr);
		filesPanelConstr.gridy++;

		addTab("Options", filesPanel);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
			throws NotConfigurableException {
		m_loadingLayout.show(m_loading, "blank");
		m_files.updateHistory();
		try {
			m_smFilePath.loadSettingsFrom(settings);
			m_files.setSelectedFile(m_smFilePath.getStringValue());
		} catch (final InvalidSettingsException e) {
			m_smFilePath.setStringValue(m_files.getSelectedFile());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
		// TODO: pressing "Apply" also shows the loading label, loading only happens on "OK" - we need to observe the
		// node model
		m_loadingLayout.show(m_loading, "label");
		m_smFilePath.saveSettingsTo(settings);
	}
}
