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
package org.knime.dl.keras.core.struct.dialog;

import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.knime.core.node.InvalidSettingsException;
import org.knime.dl.keras.core.struct.Struct;
import org.knime.dl.keras.core.struct.instance.MemberReadInstance;
import org.knime.dl.keras.core.struct.instance.MemberWriteInstance;
import org.knime.dl.keras.core.struct.instance.StructInstance;

import net.miginfocom.swing.MigLayout;

/**
 * Default implementation of {@link SwingWidgetPanel}.
 * 
 * <p>
 * NB: Heavily inspired by work of Curtis Rueden.
 * </p>
 * 
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public class DefaultSwingWidgetPanelFactory implements SwingWidgetPanelFactory {

    @Override
    public SwingWidgetPanel createPanel(Struct instance) {
        return new DefaultSwingWidgetPanel(SwingWidgetRegistry.getInstance().createWidgets(instance));
    }

    // -- Helper classes --
    class DefaultSwingWidgetPanel implements SwingWidgetPanel {

        private final Map<String, ? extends SwingWidget<?>> m_widgets;

        private JPanel panel;

        public DefaultSwingWidgetPanel(final Map<String, ? extends SwingWidget<?>> widgets) {
            this.m_widgets = widgets;
        }

        @Override
        public JPanel getComponent() {
            if (panel != null)
                return panel;

            panel = new JPanel();
            final MigLayout layout = new MigLayout("fillx,wrap 2", "[right]10[fill,grow]");
            panel.setLayout(layout);

            for (final SwingWidget<?> widget : m_widgets.values()) {
                // add widget to panel
                final String label = SwingWidgets.label(widget);
                if (label != null) {
                    // widget is prefixed by a label
                    final JLabel l = new JLabel(label);
                    panel.add(l);
                    panel.add(widget.getComponent());
                } else {
                    // widget occupies entire row
                    getComponent().add(widget.getComponent(), "span");
                }
            }

            return panel;
        }

        @Override
        public void loadFrom(StructInstance<? extends MemberReadInstance<?>, ?> structInstance)
            throws InvalidSettingsException {
            for (MemberReadInstance<?> memberInstance : structInstance) {
                load(memberInstance);
            }
        }

        @Override
        public void saveTo(StructInstance<? extends MemberWriteInstance<?>, ?> structInstance)
            throws InvalidSettingsException {
            for (MemberWriteInstance<?> memberInstance : structInstance) {
                save(memberInstance);
            }
        }

        private <T> void save(MemberWriteInstance<T> memberInstance) throws InvalidSettingsException {
            @SuppressWarnings("unchecked")
            final SwingWidget<T> swingWidget = (SwingWidget<T>)m_widgets.get(memberInstance.member().getKey());
            swingWidget.saveTo(memberInstance);
        }

        private <T> void load(MemberReadInstance<T> memberInstance) throws InvalidSettingsException {
            @SuppressWarnings("unchecked")
            final SwingWidget<T> swingWidget = (SwingWidget<T>)m_widgets.get(memberInstance.member().getKey());
            swingWidget.loadFrom(memberInstance);
        }

        @Override
        public void setEnabled(boolean enabled) {
            for (final SwingWidget<?> widget : m_widgets.values()) {
                widget.setEnabled(enabled);
            }
        }
    }

}
