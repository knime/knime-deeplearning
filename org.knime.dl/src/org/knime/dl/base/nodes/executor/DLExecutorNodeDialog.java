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

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.dl.base.nodes.DLDefaultNodeDialogTab;
import org.knime.dl.base.nodes.DLInputPanel;
import org.knime.dl.base.nodes.DLInputsPanel;
import org.knime.dl.base.nodes.DLTensorRole;
import org.knime.dl.base.nodes.DefaultDLNodeDialogPane;
import org.knime.dl.base.portobjects.DLNetworkPortObject;
import org.knime.dl.base.portobjects.DLNetworkPortObjectSpec;
import org.knime.dl.core.DLNetworkSpec;
import org.knime.dl.core.DLTensorSpec;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class DLExecutorNodeDialog extends DefaultDLNodeDialogPane {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(DLExecutorNodeModel.class);

    private final DLExecutorGeneralPanel m_generalPanel;

    private final DLExecutorGeneralConfig m_generalCfg;

    private final DLInputsPanel<DLInputPanel<?>> m_inputsPanel;

    private final DLExecutorOutputsPanel m_outputsPanel;

    /**
     * Creates a new dialog.
     */
    public DLExecutorNodeDialog() {
        DLDefaultNodeDialogTab optionsTab = new DLDefaultNodeDialogTab("Options");
        addTab(optionsTab.getTitle(), optionsTab.getTab());
        m_generalCfg = DLExecutorNodeModel.createGeneralModelConfig();
        m_generalPanel = new DLExecutorGeneralPanel(m_generalCfg);
        m_inputsPanel = new DLInputsPanel<>(this::createInputPanel, DLExecutorNodeModel.CFG_KEY_INPUTS, "Input");
        m_outputsPanel = new DLExecutorOutputsPanel(m_generalCfg, getPanel(), () -> {
                getPanel().validate();
                getPanel().repaint();
            });
        setWrapperPanel(optionsTab.getTabRoot());
        addSeparator("General Settings");
        addDialogComponentGroup(m_generalPanel);
        addSeparator("Inputs");
        addDialogComponentGroup(m_inputsPanel);
        addSeparator("Outputs");
        addPanelToWrapper(m_outputsPanel.getPanel());
    }

    private DLInputPanel<DLExecutorInputConfig> createInputPanel(final DLTensorSpec tensorSpec) {
        final DLExecutorInputConfig cfg = new DLExecutorInputConfig(tensorSpec.getName(), m_generalCfg);
        return new DLInputPanel<>(cfg, tensorSpec, "Input columns:", DLTensorRole.INPUT);
    }

    private static void checkPortObjectSpecs(final PortObjectSpec[] specs) throws NotConfigurableException {
        if (specs[DLExecutorNodeModel.IN_NETWORK_PORT_IDX] == null) {
            throw new NotConfigurableException("Input deep learning network port object is missing.");
        }
        if (specs[DLExecutorNodeModel.IN_DATA_PORT_IDX] == null) {
            throw new NotConfigurableException("Input data table is missing.");
        }
        if (!DLNetworkPortObject.TYPE.acceptsPortObjectSpec(specs[DLExecutorNodeModel.IN_NETWORK_PORT_IDX])) {
            throw new NotConfigurableException("Input port object is not a valid deep learning network port object.");
        }
        if (((DataTableSpec)specs[DLExecutorNodeModel.IN_DATA_PORT_IDX]).getNumColumns() == 0) {
            throw new NotConfigurableException("Input table has no columns.");
        }

        final DLNetworkPortObjectSpec currNetworkSpec =
            (DLNetworkPortObjectSpec)specs[DLExecutorNodeModel.IN_NETWORK_PORT_IDX];

        if (currNetworkSpec.getNetworkSpec() == null) {
            throw new NotConfigurableException("Input port object's deep learning network spec is missing.");
        }
        if (currNetworkSpec.getNetworkType() == null) {
            throw new NotConfigurableException("Input port object's deep learning network type is missing.");
        }
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {

        checkPortObjectSpecs(specs);

        final DLNetworkPortObjectSpec currNetworkSpec =
            (DLNetworkPortObjectSpec)specs[DLExecutorNodeModel.IN_NETWORK_PORT_IDX];
        final DataTableSpec currTableSpec = (DataTableSpec)specs[DLExecutorNodeModel.IN_DATA_PORT_IDX];

        final DLNetworkSpec networkSpec = currNetworkSpec.getNetworkSpec();

        if (networkSpec.getInputSpecs().length == 0) {
            LOGGER.warn("Input deep learning network has no input specs.");
        }
        if (networkSpec.getOutputSpecs().length == 0 && networkSpec.getHiddenOutputSpecs().length == 0) {
            LOGGER.warn("Input deep learning network has no output specs.");
        }

        try {
            // we can always try to load the general settings, even if the network has changed
            m_generalCfg.loadFromSettings(settings);
        } catch (final InvalidSettingsException e1) {
            throw new NotConfigurableException(e1.getMessage());
        }

        m_inputsPanel.loadSettingsFrom(settings, networkSpec.getInputSpecs(), currTableSpec);
        m_outputsPanel.loadSettingsFrom(settings, specs);
        super.loadSettingsFrom(settings, specs);

    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_generalCfg.saveToSettings(settings);
        m_inputsPanel.saveSettingsTo(settings);
        m_outputsPanel.saveToSettings(settings);
    }

}
