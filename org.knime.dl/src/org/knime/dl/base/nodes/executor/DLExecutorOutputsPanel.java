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
package org.knime.dl.base.nodes.executor;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Optional;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.dl.base.portobjects.DLNetworkPortObjectSpec;
import org.knime.dl.core.DLNetworkSpec;
import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.util.DLUtils;

/**
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class DLExecutorOutputsPanel {
    
    private static final NodeLogger LOGGER = NodeLogger.getLogger(DLExecutorNodeModel.class);

    private LinkedHashMap<String, DLExecutorOutputPanel> m_outputPanels = new LinkedHashMap<>();
    
    private JButton m_addOutputButton = new JButton("add output");
    
    private int m_numPossibleOutputs = 0;
    
    private DLNetworkSpec m_networkSpec = null;
    
    private final DLExecutorGeneralConfig m_generalCfg;
    
    private final GridBagConstraints m_gbc = createRootConstraints();
    
    private final CallBackAction m_callBack;
    
    private final JPanel m_panel = new JPanel(new GridBagLayout());
    
    /**
     * @param generalCfg 
     * @param dialogPanel 
     * @param callBack 
     * 
     */
    public DLExecutorOutputsPanel(final DLExecutorGeneralConfig generalCfg, JPanel dialogPanel, CallBackAction callBack) {
        m_callBack = callBack;
        m_generalCfg = generalCfg;
        m_addOutputButton.addActionListener(e -> addAction(generalCfg, dialogPanel));
        m_panel.add(m_addOutputButton, createAddBtnConstraints());
        m_gbc.gridy++;
    }
    
    
    public JPanel getPanel() {
        return m_panel;
    }

    private void addAction(final DLExecutorGeneralConfig generalCfg,
        JPanel dialogPanel) {
        assert m_networkSpec != null;
        // 'add output' dialog
        final JPanel outputsAddDlg = new JPanel(new GridBagLayout());
        final GridBagConstraints addOutputDialogConstr = new GridBagConstraints();
        addOutputDialogConstr.gridx = 0;
        addOutputDialogConstr.gridy = 0;
        addOutputDialogConstr.weightx = 1;
        addOutputDialogConstr.anchor = GridBagConstraints.WEST;
        addOutputDialogConstr.fill = GridBagConstraints.VERTICAL;
        // available outputs
        final ArrayList<String> availableOutputs = new ArrayList<>(m_numPossibleOutputs - m_outputPanels.size());
        final HashMap<String, DLTensorSpec> availableOutputsMap = new HashMap<>(availableOutputs.size());
        final HashMap<String, String> availableOutputsSuffixMap = new HashMap<>(availableOutputs.size());
        fillAvailableOutputs(m_networkSpec, availableOutputs, availableOutputsMap, availableOutputsSuffixMap);
        // output selection
        final SettingsModelString smOutput = new SettingsModelString("output", availableOutputs.get(0));
        final DialogComponentStringSelection dcOutput = new DialogComponentStringSelection(smOutput, "Output",
                availableOutputs);
        outputsAddDlg.add(dcOutput.getComponentPanel(), addOutputDialogConstr);
        final int selectedOption = JOptionPane.showConfirmDialog(dialogPanel,
                outputsAddDlg, "Add output...", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (selectedOption == JOptionPane.OK_OPTION) {
            final DLTensorSpec outputTensorSpec = availableOutputsMap.get(smOutput.getStringValue());
            final String suffix = availableOutputsSuffixMap.get(smOutput.getStringValue());
            if (!DLUtils.Shapes.isKnown(outputTensorSpec.getShape())) {
                final String msg = "Output '" + outputTensorSpec.getName()
                        + "' has an unknown shape. This is not supported.";
                LOGGER.error(msg);
                throw new IllegalStateException(msg);
            }
            try {
                addOutputPanel(outputTensorSpec, generalCfg, suffix);
            } catch (final Exception ex) {
                LOGGER.error(ex.getMessage());
                throw new IllegalStateException(ex.getMessage(), ex);
            }
        }
    }

    private void fillAvailableOutputs(final DLNetworkSpec networkSpec, final ArrayList<String> availableOutputs,
        final HashMap<String, DLTensorSpec> availableOutputsMap, final HashMap<String, String> availableOutputsSuffixMap) {
        for (final DLTensorSpec outputSpec : networkSpec.getOutputSpecs()) {
            addToAvailableOutputs(availableOutputs, availableOutputsMap, availableOutputsSuffixMap, outputSpec, "");
        }
        for (int i = networkSpec.getHiddenOutputSpecs().length - 1; i >= 0; i--) {
            final DLTensorSpec intermediateSpec = networkSpec.getHiddenOutputSpecs()[i];
            addToAvailableOutputs(availableOutputs, availableOutputsMap, availableOutputsSuffixMap, intermediateSpec, " (hidden)");
        }
    }

    private void addToAvailableOutputs(final ArrayList<String> availableOutputs,
        final HashMap<String, DLTensorSpec> availableOutputsMap, final HashMap<String, String> availableOutputsSuffixMap,
        final DLTensorSpec outputSpec, final String nameSuffix) {
        final String outputName = outputSpec.getName() + nameSuffix;
        if (!m_outputPanels.containsKey(outputSpec.getName())) {
            availableOutputs.add(outputName);
            availableOutputsMap.put(outputName, outputSpec);
            availableOutputsSuffixMap.put(outputName, nameSuffix);
        }
    }
    
    private static GridBagConstraints createAddBtnConstraints() {
        final GridBagConstraints outputsAddBtnConstr = new GridBagConstraints();
        outputsAddBtnConstr.weightx = 1;
        outputsAddBtnConstr.anchor = GridBagConstraints.EAST;
        outputsAddBtnConstr.fill = GridBagConstraints.NONE;
        outputsAddBtnConstr.insets = new Insets(0, 5, 10, 5);
        return outputsAddBtnConstr;
    }
    
    private static GridBagConstraints createRootConstraints() {
        final GridBagConstraints rootConstr = new GridBagConstraints();
        rootConstr.gridx = 0;
        rootConstr.gridy = 0;
        rootConstr.gridwidth = 1;
        rootConstr.gridheight = 1;
        rootConstr.weightx = 1;
        rootConstr.weighty = 0;
        rootConstr.anchor = GridBagConstraints.WEST;
        rootConstr.fill = GridBagConstraints.BOTH;
        rootConstr.insets = new Insets(5, 5, 5, 5);
        rootConstr.ipadx = 0;
        rootConstr.ipady = 0;
        return rootConstr;
    }
    
    private void addOutputPanel(final DLTensorSpec outputTensorSpec, final DLExecutorGeneralConfig generalCfg, final String suffix)
            throws NotConfigurableException {
        final String outputName = outputTensorSpec.getName();
        if (!m_outputPanels.containsKey(outputName)) {
            final DLExecutorOutputConfig outputCfg = DLExecutorNodeModel.createOutputTensorModelConfig(outputName,
                    generalCfg);
            final DLExecutorOutputPanel outputPanel = new DLExecutorOutputPanel(outputCfg, outputTensorSpec, suffix);
            outputPanel.addRemoveListener(e -> removeOutputPanel(outputName, outputPanel));
            // add output panel to dialog
            m_outputPanels.put(outputName, outputPanel);
            m_panel.add(outputPanel, m_gbc);
            if (m_outputPanels.size() == m_numPossibleOutputs) {
                m_addOutputButton.setEnabled(false);
            }
            m_gbc.gridy++;
            m_callBack.callBack();
        }
    }
    
    private void removeOutputPanel(final String outputName, final DLExecutorOutputPanel outputPanel) {
        if (m_outputPanels.remove(outputName) != null) {
            doRemoval(outputPanel);
        }
    }


    private void doRemoval(final DLExecutorOutputPanel outputPanel) {
        outputPanel.unregisterListeners();
        m_panel.remove(outputPanel);
        m_addOutputButton.setEnabled(true);
        m_callBack.callBack();
    }
    
    
    void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs) throws NotConfigurableException {
        if (settings.containsKey(DLExecutorNodeModel.CFG_KEY_OUTPUTS)) {
            // if we don't clear we might have panels that were not saved (previous dialog was canceled with added output)
            clearOutputs();
            final NodeSettingsRO outputSettings;
            final String[] orderedOutputs;
            m_networkSpec =
                    ((DLNetworkPortObjectSpec)specs[DLExecutorNodeModel.IN_NETWORK_PORT_IDX]).getNetworkSpec();
            m_numPossibleOutputs = m_networkSpec.getOutputSpecs().length + m_networkSpec.getHiddenOutputSpecs().length;
            try {
                outputSettings = settings.getNodeSettings(DLExecutorNodeModel.CFG_KEY_OUTPUTS);
                orderedOutputs = loadOutputOrder(settings, outputSettings);
            } catch (final InvalidSettingsException e) {
                throw new NotConfigurableException(e.getMessage(), e);
            }
            for (final String layerName : orderedOutputs) {
                if (!m_outputPanels.containsKey(layerName)) {
                    String suffix = "";
                    Optional<DLTensorSpec> spec = DLUtils.Networks.findSpec(layerName, m_networkSpec.getOutputSpecs());
                    if (!spec.isPresent()) {
                        spec = DLUtils.Networks.findSpec(layerName, m_networkSpec.getHiddenOutputSpecs());
                        suffix = " (hidden)";
                    }
                    if (spec.isPresent()) {
                        addOutputPanel(spec.get(), m_generalCfg, suffix);
                    }
                }
            }
            for (final DLExecutorOutputPanel outputPanel : m_outputPanels.values()) {
                outputPanel.loadFromSettings(outputSettings, specs);
            }
        }
    }
    
    private void clearOutputs() {
        for (DLExecutorOutputPanel outputPanel : m_outputPanels.values()) {
            doRemoval(outputPanel);
        }
        m_outputPanels.clear();
    }
    

    private static String[] loadOutputOrder(final NodeSettingsRO settings, final NodeSettingsRO outputSettings)
        throws InvalidSettingsException {
        final String[] orderedOutputs;
        if (outputSettings.getChildCount() > 0) {
            final SettingsModelStringArray outputOrder =
                DLExecutorNodeModel.createOutputOrderSettingsModel(outputSettings.getChildCount());
            outputOrder.loadSettingsFrom(settings);
            orderedOutputs = outputOrder.getStringArrayValue();
        } else {
            orderedOutputs = new String[0];
        }
        return orderedOutputs;
    }
        
    public void saveToSettings(final NodeSettingsWO settings) throws InvalidSettingsException {
        final NodeSettingsWO outputSettings = settings.addNodeSettings(DLExecutorNodeModel.CFG_KEY_OUTPUTS);
        saveOutputs(outputSettings);
        saveOutputOrder(settings);
    }

    private void saveOutputOrder(final NodeSettingsWO settings) {
        final SettingsModelStringArray outputOrder = DLExecutorNodeModel
                .createOutputOrderSettingsModel(m_outputPanels.size());
        final String[] outputs = new String[m_outputPanels.size()];

        int i = 0;
        for (final String output : m_outputPanels.keySet()) {
            outputs[i++] = output;
        }
        outputOrder.setStringArrayValue(outputs);
        outputOrder.saveSettingsTo(settings);
    }

    private void saveOutputs(final NodeSettingsWO outputSettings) throws InvalidSettingsException {
        for (final DLExecutorOutputPanel outputPanel : m_outputPanels.values()) {
            outputPanel.saveToSettings(outputSettings);
        }
    }
    
    interface CallBackAction {
        void callBack();
    }
}
