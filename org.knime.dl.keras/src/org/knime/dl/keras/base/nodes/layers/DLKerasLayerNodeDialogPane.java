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
package org.knime.dl.keras.base.nodes.layers;

import java.util.Map.Entry;

import javax.swing.JPanel;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialog;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.dl.keras.core.layers.DLKerasLayer;
import org.knime.dl.keras.core.struct.Struct;
import org.knime.dl.keras.core.struct.access.MemberReadAccess;
import org.knime.dl.keras.core.struct.access.MemberWriteAccess;
import org.knime.dl.keras.core.struct.access.StructAccess;
import org.knime.dl.keras.core.struct.dialog.DefaultSwingWidgetPanelFactory;
import org.knime.dl.keras.core.struct.dialog.SwingWidgetPanel;
import org.knime.dl.keras.core.struct.instance.StructInstances;
import org.knime.dl.keras.core.struct.nodesettings.NodeSettingsStructs;
import org.knime.dl.keras.core.struct.param.ParameterStructs;
import org.knime.dl.keras.core.struct.param.ValidityException;

import net.miginfocom.swing.MigLayout;

/**
 * Implementation of a {@link NodeDialog} working with {@link DLKerasLayer}s.
 * 
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
final class DLKerasLayerNodeDialogPane<T extends DLKerasLayer> extends NodeDialogPane {

    private final StructAccess<MemberReadAccess<?, NodeSettingsRO>> m_settingsRO;

    private final StructAccess<MemberWriteAccess<?, NodeSettingsWO>> m_settingsWO;

    private final SwingWidgetPanel m_panel;

    public DLKerasLayerNodeDialogPane(Class<T> layerType) throws ValidityException {
        final Struct struct = ParameterStructs.structOf(layerType);
        m_settingsRO = NodeSettingsStructs.createStructROAccess(struct);
        m_settingsWO = NodeSettingsStructs.createStructWOAccess(struct);
        final DefaultSwingWidgetPanelFactory factory = new DefaultSwingWidgetPanelFactory();
        m_panel = factory.createPanel(struct);
        for (Entry<String, JPanel> e : m_panel.getComponents().entrySet()) {
            final JPanel nodeDialogPanel = new JPanel(new MigLayout("", "[grow]", ""));
            nodeDialogPanel.add(e.getValue(), "growx");
            addTab(e.getKey(), nodeDialogPanel);
        }

    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        try {
            m_panel.loadFrom(StructInstances.createReadInstance(settings, m_settingsRO), specs);
        } catch (InvalidSettingsException e) {
            throw new NotConfigurableException("Can't load settings. No settings available, yet.", e);
        }
    }

    @Override
    protected void saveSettingsTo(NodeSettingsWO settings) throws InvalidSettingsException {
        m_panel.saveTo(StructInstances.createWriteInstance(settings, m_settingsWO));
    }

}
