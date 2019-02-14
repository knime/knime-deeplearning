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
package org.knime.dl.keras.base.nodes.layers.manipulation.freeze;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.filter.StringFilterPanel;
import org.knime.dl.keras.base.nodes.layers.manipulation.DLKerasAbstractManipulationNodeModel;
import org.knime.dl.keras.base.portobjects.DLKerasNetworkPortObjectSpecBase;
import org.knime.dl.keras.core.DLKerasNetworkSpec;

/**
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 */
public class DLKerasFreezeLayersNodeDialog extends NodeDialogPane {

    private final StringFilterPanel m_layerFilterPanel;

    DLKerasFreezeLayersNodeDialog() {
        m_layerFilterPanel = new StringFilterPanel();
        m_layerFilterPanel.setIncludeTitle("Not trainable layers");
        m_layerFilterPanel.setExcludeTitle("Trainable layers");
        addTab("Freeze Layers", m_layerFilterPanel);
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        final StringFilterConfiguration config = DLKerasFreezeLayersNodeModel.createLayerFilterConfig();
        m_layerFilterPanel.saveConfiguration(config);
        config.saveConfiguration(settings);
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
        final String[] layers = DLKerasFreezeLayersNodeModel.getLayerNames(networkSpec);

        final StringFilterConfiguration config = DLKerasFreezeLayersNodeModel.createLayerFilterConfig();
        config.loadConfigurationInDialogParent(settings, layers);
        m_layerFilterPanel.loadConfiguration(config, layers);
    }
}
