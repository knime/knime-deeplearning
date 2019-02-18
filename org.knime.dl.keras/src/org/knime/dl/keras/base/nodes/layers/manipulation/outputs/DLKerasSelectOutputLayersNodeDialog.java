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
package org.knime.dl.keras.base.nodes.layers.manipulation.outputs;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.keras.base.nodes.layers.manipulation.DLKerasAbstractManipulationNodeModel;
import org.knime.dl.keras.base.portobjects.DLKerasNetworkPortObjectSpecBase;
import org.knime.dl.keras.core.DLKerasNetworkSpec;

/**
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 */
public class DLKerasSelectOutputLayersNodeDialog extends NodeDialogPane {

    private final static NodeLogger LOGGER = NodeLogger.getLogger(DLKerasSelectOutputLayersNodeDialog.class);

    private final JPanel m_panel;

    private final GridBagConstraints m_gbc;

    private final List<String> m_selectedTensorIds;

    private final Map<String, OutputSelectionPanel> m_visiblePanels;

    private final Map<String, DLTensorSpec> m_availableTensorsFromName;

    private final Map<String, DLTensorSpec> m_availableTensorsFromId;

    private final List<String> m_availableTensorsNames;

    DLKerasSelectOutputLayersNodeDialog() {
        m_selectedTensorIds = new ArrayList<>();
        m_visiblePanels = new HashMap<>();

        m_availableTensorsFromName = new HashMap<>();
        m_availableTensorsFromId = new HashMap<>();
        m_availableTensorsNames = new ArrayList<>();

        m_panel = new JPanel(new GridBagLayout());
        m_gbc = new GridBagConstraints();
        m_gbc.gridx = 0;
        m_gbc.gridy = 0;

        // Add output button
        final JButton addBtn = new JButton("Add output");
        addBtn.addActionListener(e -> addAction());
        m_gbc.insets = new Insets(10, 4, 4, 10);
        m_gbc.anchor = GridBagConstraints.EAST;
        m_gbc.weightx = 1;
        m_panel.add(addBtn, m_gbc);

        // Prepare grid bag constrains for output panels
        m_gbc.insets = new Insets(4, 4, 4, 4);
        m_gbc.fill = GridBagConstraints.HORIZONTAL;
        m_gbc.weightx = 1;

        final JPanel alignNorthPanel = new JPanel(new BorderLayout());
        alignNorthPanel.add(m_panel, BorderLayout.NORTH);
        addTab("Output Selection", alignNorthPanel);
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        final SettingsModelStringArray sm = DLKerasSelectOutputLayersNodeModel.createOutputTensorsSM();
        if (m_selectedTensorIds.isEmpty()) {
            throw new InvalidSettingsException("No output selected. Select at least one output tensor.");
        }
        sm.setStringArrayValue(m_selectedTensorIds.toArray(new String[m_selectedTensorIds.size()]));
        sm.saveSettingsTo(settings);
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        final DLKerasNetworkPortObjectSpecBase spec =
            (DLKerasNetworkPortObjectSpecBase)specs[DLKerasAbstractManipulationNodeModel.IN_NETWORK_PORT_IDX];
        if (spec == null) {
            throw new NotConfigurableException(
                "Can't configure the node without specification of the input network. Please configure or execute the predecessor.");
        }
        final DLKerasNetworkSpec networkSpec = spec.getNetworkSpec();

        // Update available tensors
        m_availableTensorsFromName.clear();
        m_availableTensorsFromId.clear();
        m_availableTensorsNames.clear();
        addToAvailableTensors(networkSpec.getHiddenOutputSpecs());
        addToAvailableTensors(networkSpec.getOutputSpecs());
        Collections.reverse(m_availableTensorsNames);

        // Clear the visible output panels
        for (final OutputSelectionPanel o : m_visiblePanels.values()) {
            m_panel.remove(o);
        }
        m_visiblePanels.clear();
        m_selectedTensorIds.clear();

        // Update the dialog with the saved settings
        final SettingsModelStringArray sm = DLKerasSelectOutputLayersNodeModel.createOutputTensorsSM();
        try {
            sm.loadSettingsFrom(settings);
            for (final String output : sm.getStringArrayValue()) {
                // Only add the output if it is available
                if (m_availableTensorsFromId.containsKey(output)) {
                    addOutput(m_availableTensorsFromId.get(output));
                } else {
                    LOGGER.warn("The output with the identifier '" + output + "' is not avaiable anymore.");
                }
            }
        } catch (final InvalidSettingsException e) {
            // No valid settings are saved: Just show a dialog with no output selected yet
        }
        updatePanel();
    }

    private void addToAvailableTensors(final DLTensorSpec[] tensors) {
        for (final DLTensorSpec s : tensors) {
            m_availableTensorsFromName.put(s.getName(), s);
            m_availableTensorsFromId.put(s.getIdentifier().getIdentifierString(), s);
            m_availableTensorsNames.add(s.getName());
        }
    }

    private void removeOutput(final String identifier, final OutputSelectionPanel panel) {
        m_panel.remove(panel);
        updatePanel();
        m_selectedTensorIds.remove(identifier);
        m_visiblePanels.remove(identifier);
    }

    private void addOutput(final DLTensorSpec tensor) {
        // Add a panel for the output tensor
        final String id = tensor.getIdentifier().getIdentifierString();
        final String name = tensor.getName();
        final String shape = tensor.getShape().toString();
        final String dtype = tensor.getElementType().getSimpleName();
        final OutputSelectionPanel output = new OutputSelectionPanel(id, name, shape, dtype);
        m_gbc.gridy++;
        m_panel.add(output, m_gbc);
        updatePanel();

        // Add it to the currently selected tensors
        m_selectedTensorIds.add(id);
        m_visiblePanels.put(id, output);
    }

    private void updatePanel() {
        getPanel().validate();
        getPanel().repaint();
    }

    private void addAction() {
        // 'add output' dialog
        final JPanel outputsAddDlg = new JPanel(new GridBagLayout());
        final GridBagConstraints addOutputDialogConstr = new GridBagConstraints();
        addOutputDialogConstr.gridx = 0;
        addOutputDialogConstr.gridy = 0;
        addOutputDialogConstr.weightx = 1;
        addOutputDialogConstr.anchor = GridBagConstraints.WEST;
        addOutputDialogConstr.fill = GridBagConstraints.VERTICAL;
        // get the tensor names that are available and not yet selected
        final ArrayList<String> availableForSelection = new ArrayList<>(m_availableTensorsNames);
        availableForSelection.removeAll(m_selectedTensorIds.stream().map(m_availableTensorsFromId::get)
            .map(DLTensorSpec::getName).collect(Collectors.toList()));
        // output selection
        final SettingsModelString smOutput = new SettingsModelString("output", availableForSelection.get(0));
        final DialogComponentStringSelection dcOutput =
            new DialogComponentStringSelection(smOutput, "Output", availableForSelection);
        outputsAddDlg.add(dcOutput.getComponentPanel(), addOutputDialogConstr);
        final int selectedOption = JOptionPane.showConfirmDialog(getPanel(), outputsAddDlg, "Add output...",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (selectedOption == JOptionPane.OK_OPTION) {
            final DLTensorSpec outputTensorSpec = m_availableTensorsFromName.get(smOutput.getStringValue());
            addOutput(outputTensorSpec);
        }
    }

    private final class OutputSelectionPanel extends JPanel {

        private static final long serialVersionUID = 1L;

        public OutputSelectionPanel(final String identifier, final String name, final String shape,
            final String dtype) {
            super(new GridBagLayout());

            // Set the border
            final TitledBorder border = BorderFactory.createTitledBorder(name);
            setBorder(border);

            final GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.insets = new Insets(4, 4, 4, 4);
            gbc.anchor = GridBagConstraints.NORTHWEST;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 2;

            // Add the information
            add(new JLabel("Shape: " + shape), gbc);
            gbc.gridy++;
            add(new JLabel("Data type: " + dtype), gbc);

            // Add the remove button
            gbc.gridy = 0;
            gbc.gridx = 1;
            gbc.weightx = 0;
            gbc.anchor = GridBagConstraints.NORTHEAST;
            final JButton removeBtn = new JButton("Remove");
            add(removeBtn, gbc);

            removeBtn.addActionListener(e -> removeOutput(identifier, this));
        }
    }
}
