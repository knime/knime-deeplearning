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
import java.util.Optional;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.dl.keras.core.struct.Member;
import org.knime.dl.keras.core.struct.instance.MemberReadInstance;
import org.knime.dl.keras.core.struct.instance.MemberWriteInstance;
import org.knime.dl.keras.core.struct.param.DefaultParameterMember;
import org.knime.dl.keras.core.struct.param.OptionalStatus;
import org.knime.dl.keras.core.struct.param.ParameterChoices;
import org.knime.dl.keras.core.struct.param.ParameterMember;

import net.miginfocom.swing.MigLayout;

/**
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
class SwingOptionalWidgetFactory<T> implements SwingWidgetFactory<T> {

    @Override
    public boolean supports(final Member<?> member) {
        return member instanceof ParameterMember
            && OptionalStatus.isOptional(((ParameterMember<?>)member).getOptionalStatus());
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

            // TODO wrapper
            m_widget = SwingWidgetRegistry.getInstance().createWidget(new ParameterMemberWrapper<T>((ParameterMember<T>)member()));
            m_activateBox = new JCheckBox();
            m_activateBox.addItemListener((i) -> {
                m_widget.setEnabled(m_activateBox.isSelected());
            });
            m_panel.add(m_activateBox);
            m_panel.add(m_widget.getComponent(), "growx");

            OptionalStatus os = SwingWidgets.optionalStatus(this);
            if (os.equals(OptionalStatus.OptionalAndNotEnabled)) {
                m_activateBox.setSelected(false);
                setEnabled(false);
            } else if (os.equals(OptionalStatus.OptionalAndEnabled)) {
                m_activateBox.setSelected(true);
                setEnabled(true);
            } else {
                // will not happen
                throw new IllegalStateException("The optional status must not be " + os
                    + " at this point. This is most likely an implementation error.");
            }

            return m_panel;
        }

        private void setMemberEnabledStatus(Optional<Boolean> isEnabled) {
            if (member() instanceof DefaultParameterMember) {
                DefaultParameterMember<T> dpm = (DefaultParameterMember<T>)member();
                dpm.setIsEnabled(isEnabled);
            }
        }

        @Override
        public void loadFrom(MemberReadInstance<T> instance, PortObjectSpec[] spec) throws InvalidSettingsException {
            // Trigger settings loading
            boolean isActive = instance.get() != null;
            Optional<Boolean> isEnabled = Optional.empty();
            if (member() instanceof DefaultParameterMember) {
                DefaultParameterMember<T> dpm = (DefaultParameterMember<T>)member();
                isEnabled = dpm.isEnabled();
            }
            
            // Only load the enabled status if it was was previously saved, hence there is an optional
            if (isEnabled.isPresent()) {
                m_activateBox.setSelected(isEnabled.get());
                setEnabled(isEnabled.get());
            }
            // Always load if there is something to load from
            if (isActive) {
                m_widget.loadFrom(instance, spec);
            }
        }

        @Override
        public void saveTo(MemberWriteInstance<T> instance) throws InvalidSettingsException {
            setMemberEnabledStatus(Optional.of(m_activateBox.isSelected()));
            if (m_activateBox.isSelected()) {
                m_widget.saveTo(instance);
            } else {
                instance.set(null);
            }
        }

        @Override
        public void setEnabled(boolean enabled) {
            m_widget.setEnabled(enabled);
        }
    }
    
    private class ParameterMemberWrapper<M> implements ParameterMember<M> {
        final ParameterMember<M> m_member;
        
        /**
         * @param member 
         */
        public ParameterMemberWrapper(final ParameterMember<M> member) {
            m_member = member;
        }
        
        @Override
        public OptionalStatus getOptionalStatus() {
            return OptionalStatus.NotOptional;
        }

        @Override
        public ParameterChoices<M> choices() {
            return m_member.choices();
        }

        @Override
        public String getKey() {
            return m_member.getKey();
        }

        @Override
        public Type getType() {
            return m_member.getType();
        }
        
        @Override
        public String getLabel() {
            return m_member.getLabel();
        }
        
        @Override
        public Object getMaximumValue() {
            return m_member.getMaximumValue();
        }
        
        @Override
        public Object getMinimumValue() {
            return m_member.getMinimumValue();
        }
        
        @Override
        public Object getStepSize() {
            return m_member.getStepSize();
        }
        
        @Override
        public Class<M> getRawType() {
            return m_member.getRawType();
        }
        
        @Override
        public String getTab() {
            return m_member.getTab();
        }
        
        @Override
        public String getWidgetStyle() {
            return m_member.getWidgetStyle();
        }
    }
}
