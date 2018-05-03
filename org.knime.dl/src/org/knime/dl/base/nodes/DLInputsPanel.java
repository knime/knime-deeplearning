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
package org.knime.dl.base.nodes;

import java.util.ArrayList;
import java.util.function.BiFunction;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.dl.base.settings.DLGeneralConfig;
import org.knime.dl.base.settings.DLInputConfig;
import org.knime.dl.core.DLNetworkSpec;
import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.util.DLUtils;

/**
 * Handles the dialog components for the network inputs.
 * 
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <C> the type of {@link DLGeneralConfig}
 * @param <I> the type of {@link DLInputConfig}
 */
public final class DLInputsPanel<C extends DLGeneralConfig<?>, I extends DLInputConfig<C>>
    extends AbstractGridBagDialogComponentGroup {

    private ArrayList<DLInputPanel<?, ?>> m_inputPanels = new ArrayList<>();

    private BiFunction<String, C, I> m_inputConfigurationCreator;

    private final int m_inDataPortIdx;
    
    private final String m_inputsCfgKey;
    
    private final String m_borderLabel;

    /**
     * @param networkSpec the spec of the deep learning network
     * @param tableSpec the spec of the input table
     * @param generalCfg the general configuration of the node
     * @param inputConfigurationCreator a {@link BiFunction} that creates an {@link DLInputConfig} from a
     * tensor name and <b>generalCfg</b>
     * @param inDataPortIdx the port index of the input table
     * @param inputsCfgKey the config key for the input configs
     * @param borderLabel Label displayed on the border of a single input
     * @throws NotConfigurableException if a tensor has an unknown shape
     */
    public DLInputsPanel(DLNetworkSpec networkSpec, DataTableSpec tableSpec, C generalCfg,
        BiFunction<String, C, I> inputConfigurationCreator, int inDataPortIdx, String inputsCfgKey,
        String borderLabel) throws NotConfigurableException {
        m_inputConfigurationCreator = inputConfigurationCreator;
        m_inDataPortIdx = inDataPortIdx;
        m_inputsCfgKey = inputsCfgKey;
        m_borderLabel = borderLabel;
        for (final DLTensorSpec inputTensorSpec : networkSpec.getInputSpecs()) {
            if (!DLUtils.Shapes.isKnown(inputTensorSpec.getShape())) {
                throw new NotConfigurableException(
                    "Input '" + inputTensorSpec.getName() + "' has an unknown shape. This is not supported.");
            }
            addInputPanel(inputTensorSpec, tableSpec, generalCfg);
        }
    }

    private void addInputPanel(final DLTensorSpec inputTensorSpec, final DataTableSpec tableSpec, final C generalCfg) {
        final I inputCfg = m_inputConfigurationCreator.apply(inputTensorSpec.getName(), generalCfg);
        final DLInputPanel<C, ?> inputPanel =
            new DLInputPanel<>(inputCfg, inputTensorSpec, tableSpec, m_inDataPortIdx, "Inputs", "input");
        // add input panel to dialog
        m_inputPanels.add(inputPanel);
        JPanel panel = inputPanel.getComponentGroupPanel();
        panel.setBorder(BorderFactory.createTitledBorder(m_borderLabel + ": " + inputTensorSpec.getName()));
        addComponent(panel);
    }
    
    @Override
    public void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        final NodeSettingsWO inputSettings = settings.addNodeSettings(m_inputsCfgKey);
        for (final DLInputPanel<?, ?> inputPanel : m_inputPanels) {
            inputPanel.saveToSettings(inputSettings);
        }
    }
    
    @Override
    public void loadSettingsFrom(final NodeSettingsRO settings, PortObjectSpec[] specs) throws NotConfigurableException {
        if (settings.containsKey(m_inputsCfgKey)) {
            final NodeSettingsRO inputSettings;
            try {
                inputSettings = settings.getNodeSettings(m_inputsCfgKey);
            } catch (final InvalidSettingsException e) {
                throw new NotConfigurableException(e.getMessage(), e);
            }
            for (final DLInputPanel<?, ?> inputPanel : m_inputPanels) {
                inputPanel.loadSettingsFrom(inputSettings, specs);
            }
        }
    }
    
    /**
     * Resets all input panels.
     * This especially means that the input panels unregister their listeners.
     */
    public void reset() {
        m_inputPanels.forEach(DLInputPanel::unregisterListeners);
        m_inputPanels.clear();
    }
}
