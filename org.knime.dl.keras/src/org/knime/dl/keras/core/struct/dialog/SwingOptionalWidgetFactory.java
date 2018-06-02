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

import java.lang.reflect.Type;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

import org.knime.core.node.InvalidSettingsException;
import org.knime.dl.keras.core.struct.Member;
import org.knime.dl.keras.core.struct.instance.MemberReadInstance;
import org.knime.dl.keras.core.struct.instance.MemberWriteInstance;
import org.knime.dl.keras.core.struct.param.ParameterChoices;
import org.knime.dl.keras.core.struct.param.ParameterMember;

import net.miginfocom.swing.MigLayout;

/**
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
class SwingOptionalWidgetFactory<T> implements SwingWidgetFactory<T> {

    @Override
    public boolean supports(final Member<?> member) {
        return member instanceof ParameterMember && !((ParameterMember<?>)member).isRequired();
    }

    @Override
    public SwingWidget<T> create(final Member<T> model) {
        return new Widget(model);
    }

    // -- Helper classes --

    private class Widget extends AbstractSwingWidget<T> {

        private JPanel m_panel;

        private JCheckBox m_activateBox;

        private SwingWidget<T> m_widget;

        public Widget(final Member<T> member) {
            super(member);
            getComponent();
        }

        @Override
        public JPanel getComponent() {
            if (m_panel != null)
                return m_panel;

            m_panel = new JPanel();

            // TODO wrapper
            m_widget = SwingWidgetRegistry.getInstance().createWidget(new ParameterMember<T>() {
                final ParameterMember<T> casted = (ParameterMember<T>)member();

                @Override
                public boolean isRequired() {
                    return true;
                }

                @Override
                public ParameterChoices<T> choices() {
                    return casted.choices();
                }

                @Override
                public String getKey() {
                    return casted.getKey();
                }

                @Override
                public Type getType() {
                    return casted.getType();
                }
            });
            m_activateBox = new JCheckBox();
            m_activateBox.addItemListener((i) -> {
                m_widget.setEnabled(m_activateBox.isSelected());
            });
            m_panel.add(m_activateBox);
            m_panel.add(m_widget.getComponent());

            return m_panel;
        }

        // -- DocumentListener methods --

        // -- Model change event listener --
        @Override
        public void loadFrom(MemberReadInstance<T> instance) throws InvalidSettingsException {
            final boolean isActive = instance.get() != null;
            m_activateBox.setSelected(isActive);
            if (isActive) {
                m_widget.loadFrom(instance);
            } else {
                m_widget.setEnabled(false);
            }
        }

        @Override
        public void saveTo(MemberWriteInstance<T> instance) throws InvalidSettingsException {
            if (m_activateBox.isSelected()) {
                m_widget.saveTo(instance);
            } else {
                instance.set(null);
            }
        }

        @Override
        public void setEnabled(boolean enabled) {
            m_activateBox.setEnabled(enabled);
            m_widget.setEnabled(enabled);
        }
    }
}
