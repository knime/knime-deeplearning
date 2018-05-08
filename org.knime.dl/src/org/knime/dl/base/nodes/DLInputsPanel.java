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

import java.util.LinkedHashMap;
import java.util.Set;
import java.util.function.BiFunction;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.dl.core.DLTensorSpec;

import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

/**
 * Handles the dialog components for the network inputs.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <P> the type of {@link DLInputPanel} this object should manage
 */
public final class DLInputsPanel<P extends DLInputPanel<?>>
    extends AbstractGridBagDialogComponentGroup {

    private final LinkedHashMap<DLTensorSpec, P> m_inputPanels = new LinkedHashMap<>();

    private final BiFunction<DLTensorSpec, DataTableSpec, P> m_inputPanelCreator;
    
    private final String m_inputsCfgKey;

    private final String m_borderLabel;


    /**
     * @param inputPanelCreator a {@link BiFunction} that creates an {@link DLInputPanel} from a tensor spec and a table
     *            spec
     * @param inputsCfgKey the config key for the input configs
     * @param borderLabel Label displayed on the border of a single input
     */
    public DLInputsPanel(final BiFunction<DLTensorSpec, DataTableSpec, P> inputPanelCreator,
        final String inputsCfgKey, String borderLabel) {
        m_inputsCfgKey = inputsCfgKey;
        m_borderLabel = borderLabel;
        m_inputPanelCreator = inputPanelCreator;
    }


    private void removeInputPanel(P panel) {
        panel.unregisterListeners();
        m_inputPanels.remove(panel);
    }

    private void addInputPanel(final DLTensorSpec inputTensorSpec, final DataTableSpec tableSpec) {
        final P inputPanel = m_inputPanelCreator.apply(inputTensorSpec, tableSpec);
        // add input panel to dialog
        m_inputPanels.put(inputTensorSpec, inputPanel);
        final JPanel panel = inputPanel.getComponentGroupPanel();
        panel.setBorder(BorderFactory.createTitledBorder(m_borderLabel + ": " + inputTensorSpec.getName()));
        addComponent(panel);
    }

    @Override
    public void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        final NodeSettingsWO inputSettings = settings.addNodeSettings(m_inputsCfgKey);
        for (final DLInputPanel<?> inputPanel : m_inputPanels.values()) {
            inputPanel.saveToSettings(inputSettings);
        }
    }

    /**
     * @param settings 
     * @param tensorSpecs 
     * @param tableSpec 
     * @throws NotConfigurableException 
     * 
     */
    public void loadSettingsFrom(final NodeSettingsRO settings, final DLTensorSpec[] tensorSpecs, final DataTableSpec tableSpec)
        throws NotConfigurableException {
        updatePanelList(tensorSpecs, tableSpec);
        loadPanels(settings, tableSpec);
    }
    
    private void updatePanelList(final DLTensorSpec[] tensorSpecs, final DataTableSpec tableSpec) {
        Set<DLTensorSpec> newSpecs = Sets.newHashSet(tensorSpecs);
        Set<DLTensorSpec> panelSpecs = m_inputPanels.keySet();
        if (newSpecs.equals(panelSpecs)) {
            return; // nothing to do all specs are already displayed correctly
        }

        SetView<DLTensorSpec> removedTensors = Sets.difference(panelSpecs, newSpecs);
        removedTensors.stream().map(m_inputPanels::get).forEach(this::removeInputPanel);

        SetView<DLTensorSpec> additionalTensors = Sets.difference(newSpecs, panelSpecs);
        additionalTensors.forEach(t -> addInputPanel(t, tableSpec));
    }
    
    private void loadPanels(final NodeSettingsRO settings, final DataTableSpec tableSpec) throws NotConfigurableException {
        if (settings.containsKey(m_inputsCfgKey)) {
            final NodeSettingsRO inputSettings;
            try {
                inputSettings = settings.getNodeSettings(m_inputsCfgKey);
            } catch (final InvalidSettingsException e) {
                throw new NotConfigurableException(e.getMessage(), e);
            }
            for (final P inputPanel : m_inputPanels.values()) {
                inputPanel.loadSettingsFrom(inputSettings, tableSpec);
            }
        }
    }

}
