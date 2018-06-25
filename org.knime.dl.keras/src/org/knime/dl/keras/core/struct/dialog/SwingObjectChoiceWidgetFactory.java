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

import java.awt.Component;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;

import org.jdesktop.swingx.JXCollapsiblePane;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.dl.keras.core.struct.Member;
import org.knime.dl.keras.core.struct.instance.MemberReadInstance;
import org.knime.dl.keras.core.struct.instance.MemberWriteInstance;
import org.knime.dl.keras.core.struct.instance.StructInstances;
import org.knime.dl.keras.core.struct.param.OptionalStatus;
import org.knime.dl.keras.core.struct.param.ParameterChoice;
import org.knime.dl.keras.core.struct.param.ParameterChoices;
import org.knime.dl.keras.core.struct.param.ParameterMember;
import org.knime.dl.keras.core.struct.param.ParameterNestedStructChoice;

import net.miginfocom.swing.MigLayout;

/**
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 * @param <T> type of choice.
 */
class SwingObjectChoiceWidgetFactory<T> implements SwingWidgetFactory<T> {

    private static final String EMPTY_PLACEHOLDER_KEY = "EMPTY_PANEL" + UUID.randomUUID().toString();

    @Override
    public boolean supports(final Member<?> member) {
        if (member instanceof ParameterMember) {
            final ParameterMember<?> casted = (ParameterMember<?>)member;
            return casted.choices() != null && casted.getOptionalStatus().equals(OptionalStatus.NotOptional);
        }
        return false;
    }

    @Override
    public SwingWidget<T> create(final Member<T> member) {
        return new Widget(member);
    }

    // -- Helper classes --

    private class Widget extends AbstractSwingWidget<T> {

        private JPanel m_panel;

        private final Map<String, SwingWidgetPanel> m_subPanels = new HashMap<>();

        /** Wrap each sub panel in a JXCollapsiblePane. These will always resize to its contents. */
        private final Map<String, JXCollapsiblePane> m_subPanelCollapsibles = new HashMap<>();

        private JComboBox<ParameterChoice<?>> m_comboBox;

        private ParameterChoices<T> m_choices;

        private final Map<String, Integer> m_choiceKeyToIndex = new HashMap<>();

        private SwingWidgetPanel m_currentSwingWidgetPanel;

        private PortObjectSpec[] m_lastSpec;

        public Widget(final Member<T> member) {
            super(member);
        }

        @Override
        public JPanel getComponent() {
            if (m_panel != null)
                return m_panel;

            initChoices();

            m_panel = new JPanel(new MigLayout("", "[grow]", ""));
            m_panel.setBorder(BorderFactory.createTitledBorder(""));

            m_comboBox = new JComboBox<>(m_choices.choices());
            m_comboBox.setRenderer(new DefaultListCellRenderer() {

                private static final long serialVersionUID = 1L;

                @Override
                public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index,
                    final boolean isSelected, final boolean cellHasFocus) {
                    final Component c =
                        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    if (value != null) {
                        setText(((ParameterChoice<?>)value).toString());
                    }
                    return c;
                }
            });

            m_comboBox.addItemListener(ie -> {
                refreshSubPanels();
            });
            m_panel.add(m_comboBox, "grow, wrap");

            JXCollapsiblePane emptyPanel = new JXCollapsiblePane();
            m_subPanelCollapsibles.put(EMPTY_PLACEHOLDER_KEY, emptyPanel);
            m_panel.add(emptyPanel, "grow, wrap");

            return m_panel;
        }

        private void refreshSubPanels() {
            final ParameterChoice<?> choice = (ParameterChoice<?>)m_comboBox.getSelectedItem();
            if (choice instanceof ParameterNestedStructChoice) {
                @SuppressWarnings("unchecked")
                final ParameterNestedStructChoice<T> casted = (ParameterNestedStructChoice<T>)choice;
                // remove all old listeners...
                if (!casted.access().members().isEmpty()) {
                    // If we did not yet compute the value for the choice key, then there can't be any settings yet.
                    // Otherwise, the SwingWidgetPanel will be initialized with already saved settings. Hence, we 
                    // need to load the defaults below. 
                    boolean defaultsLoaded = m_subPanels.containsKey(choice.getKey());

                    // create the SwingWidgetPanel if absent
                    m_currentSwingWidgetPanel = m_subPanels.computeIfAbsent(choice.getKey(),
                        t -> new DefaultSwingWidgetPanelFactory().createPanel(casted.access().struct()));
                    // create and wrap the corresponding sub panel
                    JXCollapsiblePane currentCollapsible =
                        m_subPanelCollapsibles.computeIfAbsent(choice.getKey(), t -> {
                            JPanel merged = getMergedComponents(m_currentSwingWidgetPanel);
                            JXCollapsiblePane collapsible = new JXCollapsiblePane();
                            collapsible.add(merged);
                            collapsible.setAnimated(false);
                            m_panel.add(collapsible, "grow, wrap");
                            return collapsible;
                        });

                    collapseAll();
                    currentCollapsible.setCollapsed(false);

                    if (!defaultsLoaded) {
                        try {
                            m_currentSwingWidgetPanel
                                .loadFrom(StructInstances.createReadInstance(casted.get(), casted.access()), m_lastSpec);
                        } catch (InvalidSettingsException e) {
                            // Can't load defaults.
                        } finally {
                            defaultsLoaded = true;
                        }
                    }
                    return;
                }
            }
            showEmpty();
        }

        /**
         * Collapses all panels.
         */
        private void collapseAll() {
            for (JXCollapsiblePane pane : m_subPanelCollapsibles.values()) {
                pane.setCollapsed(true);
            }
        }

        /**
         * Because we ignore tabs for nested panels, merges the components of each tab returned by the specified
         * SwingWidgetPanel into one panel.
         * 
         * @param swp the panel to get the tabs from
         * 
         * @return the JPanels for all tabs merged into one JPanel
         */
        private JPanel getMergedComponents(final SwingWidgetPanel swp) {
            JPanel merged = new JPanel();
            for (JPanel panel : swp.getComponents().values()) {
                merged.add(panel);
            }
            return merged;
        }

        private void showEmpty() {
            collapseAll();
            m_subPanelCollapsibles.get(EMPTY_PLACEHOLDER_KEY).setCollapsed(false);
        }

        private void initChoices() {
            if (m_choices != null) {
                return;
            }

            m_choices = ((ParameterMember<T>)member()).choices();

            int index = 0;
            for (final ParameterChoice<?> c : m_choices.choices()) {
                m_choiceKeyToIndex.put(c.getKey(), index);
                index++;
            }
        }

        @Override
        public void saveTo(MemberWriteInstance<T> instance) throws InvalidSettingsException {
            instance.set(save());
        }

        @SuppressWarnings("unchecked")
        private <V extends T> V save() throws InvalidSettingsException {
            final ParameterChoice<V> choice = (ParameterChoice<V>)m_comboBox.getSelectedItem();
            if (m_currentSwingWidgetPanel == null) {
                return choice.get();
            } else {
                final V inst = ((ParameterChoice<V>)m_comboBox.getSelectedItem()).get();
                m_currentSwingWidgetPanel.saveTo(
                    StructInstances.createWriteInstance(inst, ((ParameterNestedStructChoice<T>)choice).access()));
                return inst;
            }
        }

        @Override
        public void loadFrom(MemberReadInstance<T> instance, PortObjectSpec[] spec) throws InvalidSettingsException {
            m_lastSpec = spec;
            T obj = instance.get();
            // init any if empty
            if (obj == null) {
                obj = m_choices.choices()[0].get();
            }
            load(obj, m_choices.fromObject(obj));
            refreshSubPanels();
        }

        @SuppressWarnings("unchecked")
        private <V extends T> void load(T obj, ParameterChoice<V> choice) throws InvalidSettingsException {
            final V casted = (V)obj;
            m_comboBox.setSelectedIndex(m_choiceKeyToIndex.get(choice.getKey()));
            refreshSubPanels();
            if (m_currentSwingWidgetPanel != null) {
                m_currentSwingWidgetPanel.loadFrom(
                    StructInstances.createReadWriteInstance(casted, ((ParameterNestedStructChoice<T>)choice).access()), m_lastSpec);
            }
        }

        @Override
        public void setEnabled(boolean enabled) {
            if (m_currentSwingWidgetPanel != null)
                m_currentSwingWidgetPanel.setEnabled(enabled);

            if (enabled) {
                refreshSubPanels();
            } else {
                showEmpty();
            }

            m_comboBox.setEnabled(enabled);
        }
    }
}
