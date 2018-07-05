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

import javax.swing.JCheckBox;
import javax.swing.JPanel;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.dl.keras.core.struct.Member;
import org.knime.dl.keras.core.struct.instance.MemberReadInstance;
import org.knime.dl.keras.core.struct.instance.MemberWriteInstance;
import org.knime.dl.keras.core.struct.param.FieldParameterMember;
import org.knime.dl.keras.core.struct.param.ParameterMember;
import org.knime.dl.keras.core.struct.param.Required;

import net.miginfocom.swing.MigLayout;

/**
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
class SwingOptionalWidgetFactory<T> implements SwingWidgetFactory<T> {

    @Override
    public boolean supports(final Member<?> member) {
        return member instanceof ParameterMember
            && Required.isOptional(((ParameterMember<?>)member).getOptionalStatus());
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

            m_panel = new JPanel(new MigLayout("ins 0 0 0 0", "[][fill,grow]"));

            // Set the optional status to required so we don't end up in an endless loop wrapping optional in optional
            m_widget = SwingWidgetRegistry.getInstance()
                .createWidget(new RequiredFieldParameterMember<T>((FieldParameterMember<T>)member()));
            m_activateBox = new JCheckBox();
            m_activateBox.addItemListener((i) -> {
                m_widget.setEnabled(m_activateBox.isSelected());
            });
            m_panel.add(m_activateBox);
            m_panel.add(m_widget.getComponent(), "growx");

            Required os = SwingWidgets.optionalStatus(this);
            if (os.equals(Required.OptionalAndNotEnabled)) {
                m_activateBox.setSelected(false);
                setEnabled(false);
            } else if (os.equals(Required.OptionalAndEnabled)) {
                m_activateBox.setSelected(true);
                setEnabled(true);
            } else {
                // will not happen
                throw new IllegalStateException("The optional status must not be " + os
                    + " at this point. This is most likely an implementation error.");
            }

            return m_panel;
        }

        @Override
        public void loadFrom(MemberReadInstance<T> instance, PortObjectSpec[] spec) throws InvalidSettingsException {
            // Trigger settings loading
            instance.get();
            boolean isEnabled = instance.isEnabled();
            // Only load the enabled status if it was was previously saved, hence there is an optional
            m_activateBox.setSelected(isEnabled);
            setEnabled(isEnabled);
            // Always load if there is something to load from
            if (instance.get() != null) {
                m_widget.loadFrom(instance, spec);
            }
        }

        @Override
        public void saveTo(MemberWriteInstance<T> instance) throws InvalidSettingsException {
            final boolean isEnabled = m_activateBox.isSelected();
            instance.setEnabled(isEnabled);
            if (!isEnabled) {
                instance.set(null);
            } else {
                m_widget.saveTo(instance);
            }
        }

        @Override
        public void setEnabled(boolean enabled) {
            m_widget.setEnabled(enabled);
        }
    }
}
