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
 *   Jun 2, 2017 (marcel): created
 */
package org.knime.dl.base.nodes.executor;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Optional;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;

import org.knime.core.data.DataTableSpec;
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
import org.knime.dl.base.portobjects.DLNetworkPortObject;
import org.knime.dl.base.portobjects.DLNetworkPortObjectSpec;
import org.knime.dl.core.DLNetwork;
import org.knime.dl.core.DLNetworkSpec;
import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.util.DLUtils;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
final class DLExecutorNodeDialog extends NodeDialogPane {

	private static final NodeLogger LOGGER = NodeLogger.getLogger(DLExecutorNodeModel.class);

	private DLExecutorGeneralPanel m_generalPanel;

	private DLExecutorGeneralConfig m_generalCfg;

	private ArrayList<DLExecutorInputPanel> m_inputPanels;

	private LinkedHashMap<String, DLExecutorOutputPanel> m_outputPanels;

	private JPanel m_root;

	private JScrollPane m_rootScrollableView;

	private GridBagConstraints m_rootConstr;

	private JButton m_addOutputButton;

	private DLNetworkSpec m_lastIncomingNetworkSpec;

	private DLNetworkSpec m_lastConfiguredNetworkSpec;

	private DataTableSpec m_lastConfiguredTableSpec;

	/**
	 * Creates a new dialog.
	 */
	public DLExecutorNodeDialog() {
		resetSettings();
		addTab("Options", m_rootScrollableView);
	}

	private void resetSettings() {
		m_generalCfg = DLExecutorNodeModel.createGeneralModelConfig();
		m_inputPanels = new ArrayList<>();
		m_outputPanels = new LinkedHashMap<>();
		// root panel; content will be generated based on input network specs
		m_root = new JPanel(new GridBagLayout());
		m_rootConstr = new GridBagConstraints();
		resetDialog();
		m_rootScrollableView = new JScrollPane();
		final JPanel rootWrapper = new JPanel(new BorderLayout());
		rootWrapper.add(m_root, BorderLayout.NORTH);
		m_rootScrollableView.setViewportView(rootWrapper);
	}

	@Override
	protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
			throws NotConfigurableException {
		if (specs[DLExecutorNodeModel.IN_NETWORK_PORT_IDX] == null) {
			throw new NotConfigurableException("Input deep learning network port object is missing.");
		}
		if (specs[DLExecutorNodeModel.IN_DATA_PORT_IDX] == null) {
			throw new NotConfigurableException("Input data table is missing.");
		}
		if (!DLNetworkPortObject.TYPE.acceptsPortObjectSpec(specs[DLExecutorNodeModel.IN_NETWORK_PORT_IDX])) {
			throw new NotConfigurableException("Input port object is not a valid deep learning network port object.");
		}
		if (((DataTableSpec) specs[DLExecutorNodeModel.IN_DATA_PORT_IDX]).getNumColumns() == 0) {
			throw new NotConfigurableException("Input table has no columns.");
		}

		final DLNetworkPortObjectSpec currNetworkSpec = (DLNetworkPortObjectSpec) specs[DLExecutorNodeModel.IN_NETWORK_PORT_IDX];
		final DataTableSpec currTableSpec = (DataTableSpec) specs[DLExecutorNodeModel.IN_DATA_PORT_IDX];

		if (currNetworkSpec.getNetworkSpec() == null) {
			throw new NotConfigurableException("Input port object's deep learning network spec is missing.");
		}
		if (currNetworkSpec.getNetworkType() == null) {
			throw new NotConfigurableException("Input port object's deep learning network type is missing.");
		}

		final DLNetworkSpec networkSpec = currNetworkSpec.getNetworkSpec();
		m_lastIncomingNetworkSpec = networkSpec;

		if (networkSpec.getInputSpecs().length == 0) {
			LOGGER.warn("Input deep learning network has no input specs.");
		}
		if (networkSpec.getOutputSpecs().length == 0 && networkSpec.getHiddenOutputSpecs().length == 0) {
			LOGGER.warn("Input deep learning network has no output specs.");
		}

		final boolean networkChanged = !m_lastIncomingNetworkSpec.equals(m_lastConfiguredNetworkSpec);
		final boolean tableSpecChanged = !currTableSpec.equals(m_lastConfiguredTableSpec);

		if (networkChanged || tableSpecChanged) {
			resetDialog();
			createDialogContent(currNetworkSpec, currTableSpec);
			m_generalPanel.refreshAvailableBackends();
			for (final DLExecutorInputPanel inputPanel : m_inputPanels) {
				inputPanel.refreshAvailableConverters();
				inputPanel.refreshAllowedInputColumns();
			}
		}
		if (m_lastConfiguredNetworkSpec == null || !networkChanged) {
			m_generalPanel.loadFromSettings(settings, specs);

			if (settings.containsKey(DLExecutorNodeModel.CFG_KEY_INPUTS)) {
				final NodeSettingsRO inputSettings;
				try {
					inputSettings = settings.getNodeSettings(DLExecutorNodeModel.CFG_KEY_INPUTS);
				} catch (final InvalidSettingsException e) {
					throw new NotConfigurableException(e.getMessage(), e);
				}
				for (final DLExecutorInputPanel inputPanel : m_inputPanels) {
					inputPanel.loadFromSettings(inputSettings, specs);
				}
			}
			if (settings.containsKey(DLExecutorNodeModel.CFG_KEY_OUTPUTS)) {
				final NodeSettingsRO outputSettings;
				try {
					outputSettings = settings.getNodeSettings(DLExecutorNodeModel.CFG_KEY_OUTPUTS);
				} catch (final InvalidSettingsException e) {
					throw new NotConfigurableException(e.getMessage(), e);
				}
				for (final String layerName : outputSettings) {
					if (!m_outputPanels.containsKey(layerName)) {
						// add output to the dialog (when loading the dialog for
						// the first time)
						final Optional<DLTensorSpec> spec = DLUtils.Networks.findSpec(layerName,
								networkSpec.getOutputSpecs(), networkSpec.getHiddenOutputSpecs());
						if (spec.isPresent()) {
							addOutputPanel(spec.get(), m_generalCfg);
						}
					}
				}
				for (final DLExecutorOutputPanel outputPanel : m_outputPanels.values()) {
					outputPanel.loadFromSettings(outputSettings, specs);
				}
			}
		}

		m_lastConfiguredNetworkSpec = m_lastIncomingNetworkSpec;
		m_lastConfiguredTableSpec = currTableSpec;
	}

	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
		m_generalPanel.saveToSettings(settings);

		final NodeSettingsWO inputSettings = settings.addNodeSettings(DLExecutorNodeModel.CFG_KEY_INPUTS);
		for (final DLExecutorInputPanel inputPanel : m_inputPanels) {
			inputPanel.saveToSettings(inputSettings);
		}

		final NodeSettingsWO outputSettings = settings.addNodeSettings(DLExecutorNodeModel.CFG_KEY_OUTPUTS);
		for (final DLExecutorOutputPanel outputPanel : m_outputPanels.values()) {
			outputPanel.saveToSettings(outputSettings);
		}

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

	private void resetDialog() {
		m_inputPanels.clear();
		m_outputPanels.clear();
		m_root.removeAll();
		m_rootConstr.gridx = 0;
		m_rootConstr.gridy = 0;
		m_rootConstr.gridwidth = 1;
		m_rootConstr.gridheight = 1;
		m_rootConstr.weightx = 1;
		m_rootConstr.weighty = 0;
		m_rootConstr.anchor = GridBagConstraints.WEST;
		m_rootConstr.fill = GridBagConstraints.BOTH;
		m_rootConstr.insets = new Insets(5, 5, 5, 5);
		m_rootConstr.ipadx = 0;
		m_rootConstr.ipady = 0;
	}

	private void createDialogContent(final DLNetworkPortObjectSpec portObjectSpec, final DataTableSpec tableSpec)
			throws NotConfigurableException {
		final DLNetworkSpec networkSpec = portObjectSpec.getNetworkSpec();
		final Class<? extends DLNetwork> networkType = portObjectSpec.getNetworkType();

		// general settings:
		m_generalPanel = new DLExecutorGeneralPanel(m_generalCfg, networkSpec, networkType);
		m_root.add(m_generalPanel, m_rootConstr);
		m_rootConstr.gridy++;

		// input settings:
		final JPanel inputsSeparator = new JPanel(new GridBagLayout());
		final GridBagConstraints inputsSeparatorLabelConstr = new GridBagConstraints();
		inputsSeparatorLabelConstr.gridwidth = 1;
		inputsSeparatorLabelConstr.weightx = 0;
		inputsSeparatorLabelConstr.anchor = GridBagConstraints.WEST;
		inputsSeparatorLabelConstr.fill = GridBagConstraints.NONE;
		inputsSeparatorLabelConstr.insets = new Insets(7, 7, 7, 7);
		final GridBagConstraints inputsSeparatorSeparatorConstr = new GridBagConstraints();
		inputsSeparatorSeparatorConstr.gridwidth = GridBagConstraints.REMAINDER;
		inputsSeparatorSeparatorConstr.weightx = 1;
		inputsSeparatorSeparatorConstr.fill = GridBagConstraints.HORIZONTAL;
		inputsSeparator.add(new JLabel("Inputs"), inputsSeparatorLabelConstr);
		inputsSeparator.add(new JSeparator(), inputsSeparatorSeparatorConstr);
		m_root.add(inputsSeparator, m_rootConstr);
		m_rootConstr.gridy++;
		// inputs
		for (final DLTensorSpec inputDataSpec : networkSpec.getInputSpecs()) {
			if (!DLUtils.Shapes.isFixed(inputDataSpec.getShape())) {
				throw new NotConfigurableException("Input '" + inputDataSpec.getName()
						+ "' has an (at least partially) unknown shape. This is not supported.");
			}
			addInputPanel(inputDataSpec, tableSpec, m_generalCfg);
		}

		// output settings:
		final JPanel outputsSeparator = new JPanel(new GridBagLayout());
		final GridBagConstraints outputsSeparatorLabelConstr = new GridBagConstraints();
		outputsSeparatorLabelConstr.gridwidth = 1;
		outputsSeparatorLabelConstr.weightx = 0;
		outputsSeparatorLabelConstr.anchor = GridBagConstraints.WEST;
		outputsSeparatorLabelConstr.fill = GridBagConstraints.NONE;
		outputsSeparatorLabelConstr.insets = new Insets(7, 7, 7, 7);
		final GridBagConstraints outputsSeparatorSeparatorConstr = new GridBagConstraints();
		outputsSeparatorSeparatorConstr.gridwidth = GridBagConstraints.REMAINDER;
		outputsSeparatorSeparatorConstr.weightx = 1;
		outputsSeparatorSeparatorConstr.fill = GridBagConstraints.HORIZONTAL;
		outputsSeparator.add(new JLabel("Outputs"), outputsSeparatorLabelConstr);
		outputsSeparator.add(new JSeparator(), outputsSeparatorSeparatorConstr);
		m_root.add(outputsSeparator, m_rootConstr);
		m_rootConstr.gridy++;
		// 'add output' button
		m_addOutputButton = new JButton("add output");
		// 'add output' button click event: open dialog
		m_addOutputButton.addActionListener(e -> {
			// 'add output' dialog
			final JPanel outputsAddDlg = new JPanel(new GridBagLayout());
			final GridBagConstraints addOutputDialogConstr = new GridBagConstraints();
			addOutputDialogConstr.gridx = 0;
			addOutputDialogConstr.gridy = 0;
			addOutputDialogConstr.weightx = 1;
			addOutputDialogConstr.anchor = GridBagConstraints.WEST;
			addOutputDialogConstr.fill = GridBagConstraints.VERTICAL;
			// available outputs
			final ArrayList<String> availableOutputs = new ArrayList<>(networkSpec.getOutputSpecs().length
					+ networkSpec.getHiddenOutputSpecs().length - m_outputPanels.size());
			final HashMap<String, DLTensorSpec> availableOutputsMap = new HashMap<>(availableOutputs.size());
			for (final DLTensorSpec outputSpec : networkSpec.getOutputSpecs()) {
				final String outputName = outputSpec.getName();
				if (!m_outputPanels.containsKey(outputName)) {
					availableOutputs.add(outputName);
					availableOutputsMap.put(outputName, outputSpec);
				}
			}
			for (int i = networkSpec.getHiddenOutputSpecs().length - 1; i >= 0; i--) {
				final DLTensorSpec intermediateSpec = networkSpec.getHiddenOutputSpecs()[i];
				final String intermediateName = intermediateSpec.getName();
				if (!m_outputPanels.containsKey(intermediateName)) {
					final String intermediateDisplayName = intermediateName + " (hidden)";
					availableOutputs.add(intermediateDisplayName);
					availableOutputsMap.put(intermediateDisplayName, intermediateSpec);
				}
			}
			// output selection
			final SettingsModelString smOutput = new SettingsModelString("output", availableOutputs.get(0));
			final DialogComponentStringSelection dcOutput = new DialogComponentStringSelection(smOutput, "Output",
					availableOutputs);
			outputsAddDlg.add(dcOutput.getComponentPanel(), addOutputDialogConstr);
			final int selectedOption = JOptionPane.showConfirmDialog(DLExecutorNodeDialog.this.getPanel(),
					outputsAddDlg, "Add output...", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
			if (selectedOption == JOptionPane.OK_OPTION) {
				final DLTensorSpec outputDataSpec = availableOutputsMap.get(smOutput.getStringValue());
				if (!DLUtils.Shapes.isFixed(outputDataSpec.getShape())) {
					final String msg = "Output '" + outputDataSpec.getName()
							+ "' has an (at least partially) unknown shape. This is not supported.";
					LOGGER.error(msg);
					throw new IllegalStateException(msg);
				}
				try {
					addOutputPanel(outputDataSpec, m_generalCfg);
				} catch (final Exception ex) {
					LOGGER.error(ex.getMessage());
					throw new IllegalStateException(ex.getMessage(), ex);
				}
			}
		});
		final GridBagConstraints outputsAddBtnConstr = (GridBagConstraints) m_rootConstr.clone();
		outputsAddBtnConstr.weightx = 1;
		outputsAddBtnConstr.anchor = GridBagConstraints.EAST;
		outputsAddBtnConstr.fill = GridBagConstraints.NONE;
		outputsAddBtnConstr.insets = new Insets(0, 5, 10, 5);
		m_root.add(m_addOutputButton, outputsAddBtnConstr);
		m_rootConstr.gridy++;
	}

	private void addInputPanel(final DLTensorSpec inputDataSpec, final DataTableSpec tableSpec,
			final DLExecutorGeneralConfig generalCfg) throws NotConfigurableException {
		final DLExecutorInputConfig inputCfg = DLExecutorNodeModel.createInputTensorModelConfig(inputDataSpec.getName(),
				generalCfg);
		final DLExecutorInputPanel inputPanel = new DLExecutorInputPanel(inputCfg, inputDataSpec, tableSpec);
		// add input panel to dialog
		m_inputPanels.add(inputPanel);
		m_root.add(inputPanel, m_rootConstr);
		m_rootConstr.gridy++;
	}

	private void addOutputPanel(final DLTensorSpec outputDataSpec, final DLExecutorGeneralConfig generalCfg)
			throws NotConfigurableException {
		final String outputName = outputDataSpec.getName();
		if (!m_outputPanels.containsKey(outputName)) {
			final DLExecutorOutputConfig outputCfg = DLExecutorNodeModel.createOutputTensorModelConfig(outputName,
					generalCfg);
			final DLExecutorOutputPanel outputPanel = new DLExecutorOutputPanel(outputCfg, outputDataSpec);
			outputPanel.addRemoveListener(e -> removeOutputPanel(outputName, outputPanel, outputCfg));
			// add output panel to dialog
			m_outputPanels.put(outputName, outputPanel);
			m_root.add(outputPanel, m_rootConstr);
			m_rootConstr.gridy++;
			if (m_outputPanels.size() == m_lastIncomingNetworkSpec.getHiddenOutputSpecs().length
					+ m_lastIncomingNetworkSpec.getOutputSpecs().length) {
				m_addOutputButton.setEnabled(false);
			}
			m_rootScrollableView.validate();
			final JScrollBar scrollBar = m_rootScrollableView.getVerticalScrollBar();
			scrollBar.setValue(scrollBar.getMaximum());
			m_rootScrollableView.repaint();
		}
	}

	private void removeOutputPanel(final String outputName, final JPanel outputPanel,
			final DLExecutorOutputConfig outoutCfg) {
		if (m_outputPanels.remove(outputName) != null) {
			m_root.remove(outputPanel);
			m_addOutputButton.setEnabled(true);
			m_rootScrollableView.validate();
			m_rootScrollableView.repaint();
		}
	}
}
