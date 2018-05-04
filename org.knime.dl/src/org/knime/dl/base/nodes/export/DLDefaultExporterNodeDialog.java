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
package org.knime.dl.base.nodes.export;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Arrays;
import java.util.Set;

import javax.swing.JFileChooser;
import javax.swing.JPanel;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
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
import org.knime.dl.base.nodes.DialogComponentIdFromPrettyStringSelection;
import org.knime.dl.base.portobjects.DLNetworkPortObjectSpec;
import org.knime.dl.core.export.DLNetworkExporter;
import org.knime.dl.core.export.DLNetworkExporterRegistry;

/**
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 */
public final class DLDefaultExporterNodeDialog extends NodeDialogPane {

    private static final DLNetworkExporterRegistry EXPORTER_REGISTRY = DLNetworkExporterRegistry.getInstance();

    private final SettingsModelStringArray m_smExporterId = DLDefaultExporterNodeModel.createExporterIdSettingsModel();

    private final SettingsModelString m_smFilePath = DLDefaultExporterNodeModel.createFilePathSettingsModel();

    private final SettingsModelBoolean m_smOverwrite = DLDefaultExporterNodeModel.createOverwriteSettingsModel();

    private final DialogComponentIdFromPrettyStringSelection m_dcExporterId;

    private final DialogComponentBoolean m_dcOverwrite;

    private final FilesHistoryPanel m_filePanel;

    /**
     * Creates a new default exporter node dialog. This dialog consists of a combobox where the exporter is selected, a
     * file chooser and a overwrite checkbox.
     *
     * @param historyId the history identifier for the file chooser
     */
    public DLDefaultExporterNodeDialog(final String historyId) {
        // Create the dialog components
        m_dcExporterId = new DialogComponentIdFromPrettyStringSelection(m_smExporterId, "Exporter", e -> {
            // TODO Wow that's dirty! The dialog component should update the settings model itself.
            // But the dialog component is used in many places and I don't want to change everything right now.
            m_smExporterId
                .setStringArrayValue(((DialogComponentIdFromPrettyStringSelection)e.getSource()).getSelection());
            exporterChanged();
        });
        // TODO some exporters may allow folders
        m_filePanel = new FilesHistoryPanel(historyId, LocationValidation.FileOutput);
        m_filePanel.setDialogType(JFileChooser.SAVE_DIALOG);
        m_filePanel.addChangeListener(e -> m_smFilePath.setStringValue(m_filePanel.getSelectedFile()));
        m_dcOverwrite = new DialogComponentBoolean(m_smOverwrite, "Overwrite the file if it exists.");

        // Add the dialog components
        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.VERTICAL;

        panel.add(m_dcExporterId.getComponentPanel(), gbc);
        gbc.gridy++;
        panel.add(m_filePanel, gbc);
        gbc.gridy++;
        gbc.weighty = 1;
        panel.add(m_dcOverwrite.getComponentPanel(), gbc);

        addTab("Settings", panel);
    }

    private void exporterChanged() {
        final DLNetworkExporter<?> exporter = EXPORTER_REGISTRY.getExporterWithId(m_dcExporterId.getSelection()[1]).get();
        final String[] suffixes = Arrays.stream(exporter.getValidExtensions()).map(s -> "." + s).toArray(String[]::new);
        m_filePanel.setSuffixes(suffixes);
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        // Check if this node is configurable
        if (!(specs[0] instanceof DLNetworkPortObjectSpec)) {
            throw new NotConfigurableException("Cannot be configured without input network.");
        }

        // Get the list of exporters
        final DLNetworkPortObjectSpec spec = (DLNetworkPortObjectSpec)specs[0];
        final Set<DLNetworkExporter<?>> exporters = EXPORTER_REGISTRY.getExportersForType(spec.getNetworkType());
        if (exporters.isEmpty()) {
            throw new NotConfigurableException(
                "There is no exporter available for the given network. Are you missing a KNIME extension?");
        }
        final String[] names = exporters.stream().map(e -> e.getName()).toArray(String[]::new);
        final String[] ids = exporters.stream().map(e -> e.getIdentifier()).toArray(String[]::new);
        m_dcExporterId.replaceListItems(names, ids, null);

        // Load the settings into the dialog components
        m_dcExporterId.loadSettingsFrom(settings, specs);
        m_dcOverwrite.loadSettingsFrom(settings, specs);
        try {
            m_smFilePath.loadSettingsFrom(settings);
            m_filePanel.setSelectedFile(m_smFilePath.getStringValue());
        } catch (final InvalidSettingsException e) {
            m_smFilePath.setStringValue(m_filePanel.getSelectedFile());
        }
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_smExporterId.saveSettingsTo(settings);
        m_smOverwrite.saveSettingsTo(settings);
        m_smFilePath.saveSettingsTo(settings);
    }
}
