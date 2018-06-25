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

package org.knime.dl.keras.core.layers.dialog.spec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JPanel;

import org.jdesktop.swingx.combobox.ListComboBoxModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.dl.core.DLNetworkSpec;
import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.keras.base.portobjects.DLKerasNetworkPortObjectSpecBase;
import org.knime.dl.keras.core.struct.Member;
import org.knime.dl.keras.core.struct.dialog.AbstractSwingWidget;
import org.knime.dl.keras.core.struct.dialog.SwingWidget;
import org.knime.dl.keras.core.struct.dialog.SwingWidgetFactory;
import org.knime.dl.keras.core.struct.instance.MemberReadInstance;
import org.knime.dl.keras.core.struct.instance.MemberWriteInstance;
import org.knime.dl.keras.core.struct.param.OptionalStatus;
import org.knime.dl.keras.core.struct.param.ParameterMember;

import net.miginfocom.swing.MigLayout;

/**
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public class SwingDLTensorSpecWidgetFactory implements SwingWidgetFactory<DLTensorSpec> {

    @Override
    public boolean supports(final Member<?> member) {
        return DLTensorSpec.class.isAssignableFrom(member.getRawType())
            && ((ParameterMember<?>)member).getOptionalStatus().equals(OptionalStatus.NotOptional);
    }

    @Override
    public SwingWidget<DLTensorSpec> create(final Member<DLTensorSpec> model) {
        return new Widget(model);
    }

    // -- Helper classes --

    private class Widget extends AbstractSwingWidget<DLTensorSpec> {

        private JPanel panel;

        private JComboBox<DLTensorSpecItem> m_tensorSpecItems;

        private DLNetworkSpec m_lastNSpecs;

        public Widget(final Member<DLTensorSpec> model) {
            super(model);
        }

        @Override
        public JPanel getComponent() {
            if (panel != null)
                return panel;
            panel = new JPanel();
            final MigLayout layout = new MigLayout("ins 0 0 0 0", "[fill,grow]", "");
            panel.setLayout(layout);
            m_tensorSpecItems = new JComboBox<>();
            panel.add(m_tensorSpecItems, "growx");
            return panel;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void loadFrom(MemberReadInstance<DLTensorSpec> instance, PortObjectSpec[] specs)
            throws InvalidSettingsException {
            if (specs[0] == null) {
                throw new InvalidSettingsException("Can't open configuration dialog. No input network provided.");
            }
            final Member<DLTensorSpec> member = instance.member();

            if (member instanceof ParameterMember) {
                final DLTensorSpec selected = instance.get();

                // TODO WUHU WORKAROUND. WE HAVE TO REPLACE THIS WITH A PROPER FIELD IN PARAMETERS. :-)
                int portIdx = Integer.valueOf((String)((ParameterMember<?>)member).getMinimumValue());
                DLKerasNetworkPortObjectSpecBase poSpec =(DLKerasNetworkPortObjectSpecBase)specs[portIdx];
                if (poSpec == null) {
                    m_tensorSpecItems.setModel(new ListComboBoxModel<>(Collections.EMPTY_LIST));
                    m_lastNSpecs = null;
                    return;
                }
                final DLNetworkSpec nSpec = ((DLKerasNetworkPortObjectSpecBase)specs[portIdx]).getNetworkSpec();
                if (!nSpec.equals(m_lastNSpecs)) {
                    m_lastNSpecs = nSpec;
                    final List<DLTensorSpecItem> items = new ArrayList<>();
                    DLTensorSpecItem selectedItem = null;
                    for (final DLTensorSpec tSpec : nSpec.getOutputSpecs()) {
                        final DLTensorSpecItem item = new DLTensorSpecItem(tSpec, false);
                        if (selected != null && selected.getIdentifier().equals(tSpec.getIdentifier())) {
                            selectedItem = item;
                        }
                        items.add(item);
                    }

                    //                    for (final DLTensorSpec tSpec : nSpec.getHiddenOutputSpecs()) {
                    //                        final DLTensorSpecItem item = new DLTensorSpecItem(tSpec, true);
                    //                        if (selected != null && selected.getIdentifier().equals(tSpec.getIdentifier())) {
                    //                            selectedItem = item;
                    //                        }
                    //                        items.add(item);
                    //                    }

                    m_tensorSpecItems.setModel(new ListComboBoxModel<DLTensorSpecItem>(items));
                    if (selectedItem != null) {
                        m_tensorSpecItems.setSelectedItem(selectedItem);
                    }
                }
            } else {
                throw new InvalidSettingsException(
                    "Can't load settings for instance " + instance + ". Not a ParameterMember!");
            }
        }

        @Override
        public void saveTo(MemberWriteInstance<DLTensorSpec> instance) throws InvalidSettingsException {
            DLTensorSpecItem selected = (DLTensorSpecItem)m_tensorSpecItems.getSelectedItem();
            instance.set(selected == null ? null :selected.m_spec);
        }

        @Override
        public void setEnabled(boolean enabled) {
            m_tensorSpecItems.setEnabled(enabled);
        }

        private class DLTensorSpecItem {
            private final boolean m_isHidden;

            private final DLTensorSpec m_spec;

            private DLTensorSpecItem(final DLTensorSpec spec, final boolean isHidden) {
                m_isHidden = isHidden;
                m_spec = spec;
            }

            @Override
            public String toString() {
                return m_spec.getName() + (m_isHidden ? " (hidden)" : "" + " " + m_spec.getShape() + " " + m_spec.getElementType());
            }

            @Override
            public boolean equals(Object obj) {
                if (obj != null && obj instanceof DLTensorSpecItem) {
                    return m_spec.getIdentifier().equals(((DLTensorSpecItem)obj).m_spec.getIdentifier());
                } else {
                    return false;
                }
            }

            @Override
            public int hashCode() {
                return m_spec.hashCode();
            }
        }
    }
}
