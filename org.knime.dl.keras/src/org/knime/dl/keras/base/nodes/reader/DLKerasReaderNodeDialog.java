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
 *   Jun 12, 2017 (marcel): created
 */
package org.knime.dl.keras.base.nodes.reader;

import java.awt.CardLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.FilesHistoryPanel;
import org.knime.core.node.util.FilesHistoryPanel.LocationValidation;
import org.knime.core.util.Pair;
import org.knime.dl.base.nodes.DialogComponentIdFromPrettyStringSelection;
import org.knime.dl.core.DLCanceledExecutionException;
import org.knime.dl.core.DLInstallationTestTimeoutException;
import org.knime.dl.core.DLMissingDependencyException;
import org.knime.dl.core.DLNotCancelable;
import org.knime.dl.keras.core.DLKerasNetworkLoader;
import org.knime.dl.keras.core.DLKerasPythonContext;
import org.knime.dl.python.core.DLPythonContext;
import org.knime.dl.python.core.DLPythonNetworkLoaderRegistry;
import org.knime.python2.PythonCommand;
import org.knime.python2.base.PythonBasedDataUnawareNodeDialog;
import org.knime.python2.config.PythonCommandConfig;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
final class DLKerasReaderNodeDialog extends PythonBasedDataUnawareNodeDialog {

	private static final NodeLogger LOGGER = NodeLogger.getLogger(DLKerasReaderNodeModel.class);

    private final PythonCommandConfig m_pythonCommandConfig = DLKerasReaderNodeModel.createPythonCommandConfig();

	private final FilesHistoryPanel m_files;

	private final SettingsModelString m_smFilePath;

	private final DialogComponentIdFromPrettyStringSelection m_dcBackend;

	private final SettingsModelStringArray m_smBackend;

	private final DialogComponentBoolean m_dcCopyNetwork;

	private final CardLayout m_loadingLayout;

	private final JPanel m_loading;

	public DLKerasReaderNodeDialog() {
		final JPanel filesPanel = new JPanel(new GridBagLayout());
		filesPanel.setBorder(BorderFactory.createTitledBorder("Input Location"));
		final GridBagConstraints filesPanelConstr = new GridBagConstraints();
		filesPanelConstr.gridx = 0;
		filesPanelConstr.gridy = 0;
		filesPanelConstr.weightx = 1;
		filesPanelConstr.weighty = 0;
		filesPanelConstr.anchor = GridBagConstraints.NORTHWEST;
		filesPanelConstr.fill = GridBagConstraints.VERTICAL;
		m_files = new FilesHistoryPanel("org.knime.dl.keras.base.nodes.reader", LocationValidation.FileInput);
		m_files.setSuffixes(DLKerasReaderNodeModel.getValidInputFileExtensions().stream().map(s -> "." + s)
				.collect(Collectors.joining("|")));
		m_smFilePath = DLKerasReaderNodeModel.createFilePathStringModel(m_files.getSelectedFile());
		m_files.addChangeListener(e -> m_smFilePath.setStringValue(m_files.getSelectedFile()));
		filesPanel.add(m_files, filesPanelConstr);
		filesPanelConstr.gridy++;

		m_smBackend = DLKerasReaderNodeModel.createKerasBackendModel();
		m_dcBackend = new DialogComponentIdFromPrettyStringSelection(
				new SettingsModelStringArray("proxy", m_smBackend.getStringArrayValue()), "Keras back end:", (e) -> {
					m_smBackend.setStringArrayValue(
							((DialogComponentIdFromPrettyStringSelection) e.getSource()).getSelection());
				});
		filesPanel.add(m_dcBackend.getComponentPanel(), filesPanelConstr);
		filesPanelConstr.gridy++;

		final SettingsModelBoolean smCopyNetwork = DLKerasReaderNodeModel.createCopyNetworkSettingsModel();
		m_dcCopyNetwork = new DialogComponentBoolean(smCopyNetwork, "Copy deep learning network into KNIME workflow?");
		filesPanelConstr.weightx = 1;
		filesPanelConstr.weighty = 1;
		filesPanelConstr.anchor = GridBagConstraints.NORTHWEST;
		filesPanelConstr.fill = GridBagConstraints.NONE;
		filesPanel.add(m_dcCopyNetwork.getComponentPanel(), filesPanelConstr);
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
        addDefaultPythonExecutableSelectionTab(m_pythonCommandConfig);
	}

	@Override
	protected void onPythonCommandChanged(final PythonCommandConfig config) {
	    // Nothing to do.
	}

    @Override
    protected void loadSettingsFromDerived(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
		m_loadingLayout.show(m_loading, "blank");
		m_files.updateHistory();
		try {
			m_smFilePath.loadSettingsFrom(settings);
			m_files.setSelectedFile(m_smFilePath.getStringValue());
		} catch (final InvalidSettingsException e) {
			m_smFilePath.setStringValue(m_files.getSelectedFile());
		}

		try {
			m_smBackend.loadSettingsFrom(settings);
		} catch (final InvalidSettingsException e) {
			// ignore
		}

        final Pair<List<DLKerasNetworkLoader<?>>, List<String>> p =
            getAvailableLoaders(m_pythonCommandConfig.getCommand());
        final List<DLKerasNetworkLoader<?>> availableLoaders = p.getFirst();
        final List<String> unavailableLoaderIds = p.getSecond();

		final String[] names = new String[availableLoaders.size()];
		final String[] ids = new String[availableLoaders.size()];
		for (int i = 0; i < availableLoaders.size(); i++) {
			final DLKerasNetworkLoader<?> loader = availableLoaders.get(i);
			names[i] = loader.getName();
			ids[i] = loader.getNetworkType().getCanonicalName();
		}
		final String[] backend = m_smBackend.getStringArrayValue();
		final String selectedName;
		if (backend[1] != null) {
			final int idx;
			if ((idx = Arrays.asList(ids).indexOf(backend[1])) != -1) {
				selectedName = names[idx];
			} else {
				final String msg;
				if (unavailableLoaderIds.contains(backend[1])) {
					msg = "Selected Keras back end '" + backend[0]
							+ "' is not available anymore. Please check your local installation.";
				} else {
					msg = "Selected Keras back end '" + backend[0]
							+ "' cannot be found. Are you missing a KNIME Deep Learning extension?";
				}
				LOGGER.warn(msg);
				selectedName = names[0];
			}
		} else {
			selectedName = names[0];
		}
		m_dcBackend.replaceListItems(names, ids, selectedName);

		m_dcCopyNetwork.loadSettingsFrom(settings, specs);
	}

    private static Pair<List<DLKerasNetworkLoader<?>>, List<String>>
        getAvailableLoaders(final PythonCommand pythonCommand) throws NotConfigurableException {
        final List<DLKerasNetworkLoader<?>> availableLoaders =
            DLPythonNetworkLoaderRegistry.getInstance().getAllNetworkLoaders() //
                .stream() //
                .filter(nl -> nl instanceof DLKerasNetworkLoader) //
                .map(nl -> (DLKerasNetworkLoader<?>)nl) //
                .sorted(Comparator.comparing(DLKerasNetworkLoader::getName)) //
                .collect(Collectors.toList());
        final List<String> unavailableLoaderIds = new ArrayList<>();
        for (int i = availableLoaders.size() - 1; i >= 0; i--) {
            final DLKerasNetworkLoader<?> kerasNetworkLoader = availableLoaders.get(i);
            try (final DLPythonContext context = new DLKerasPythonContext(pythonCommand)) {
                DLPythonNetworkLoaderRegistry.getInstance();
                kerasNetworkLoader.checkAvailability(context, false,
                    DLPythonNetworkLoaderRegistry.getInstallationTestTimeout(), DLNotCancelable.INSTANCE);
            } catch (final DLMissingDependencyException | DLInstallationTestTimeoutException
                    | DLCanceledExecutionException e) {
                LOGGER.debug(e);
                availableLoaders.remove(i);
                unavailableLoaderIds.add(kerasNetworkLoader.getNetworkType().getCanonicalName());
            }
        }
        if (availableLoaders.isEmpty()) {
            throw new NotConfigurableException(
                "There is no available Keras back end. Please check your local installation.");
        }
        return new Pair<>(availableLoaders, unavailableLoaderIds);
    }

	@Override
	protected void saveSettingsToDerived(final NodeSettingsWO settings) throws InvalidSettingsException {
		// TODO: pressing "Apply" also shows the loading label, loading only
		// happens on "OK" - we need to observe the
		// node model
		m_loadingLayout.show(m_loading, "label");
		m_smFilePath.saveSettingsTo(settings);
		m_smBackend.saveSettingsTo(settings);
		m_dcCopyNetwork.saveSettingsTo(settings);
	}
}
