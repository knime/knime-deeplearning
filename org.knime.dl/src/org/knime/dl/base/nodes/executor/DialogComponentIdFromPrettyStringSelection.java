/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
 * History
 *   Jul 13, 2017 (marcel): created
 */
package org.knime.dl.base.nodes.executor;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;

import com.google.common.collect.HashBiMap;

/**
 * Modified from {@link DialogComponentStringSelection}. Displays pretty strings and passes their respective hidden id
 * counterparts to the underlying settings model.
 *
 * @see DialogComponentStringSelection
 *
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
public final class DialogComponentIdFromPrettyStringSelection extends DialogComponent {

    private final HashBiMap<String, String> m_byPretty;

    private final JLabel m_label;

    private final JComboBox<String> m_combobox;

    /**
     * Creates a new instance of this dialog component.
     *
     * @param stringModel the underlying string model
     * @param label the label
     * @param pretties the items that will be displayed in the dialog component, no duplicate values
     * @param ids the hidden counterparts of the visible items, will be stored in the settings model, no duplicate
     *            values
     */
    public DialogComponentIdFromPrettyStringSelection(final SettingsModelString stringModel, final String label,
        final String[] pretties, final String[] ids) {
        super(stringModel);
        checkNotNull(pretties);
        checkNotNull(ids);
        checkArgument(pretties.length == ids.length);
        m_byPretty = HashBiMap.create(pretties.length);
        m_label = new JLabel(label);
        getComponentPanel().add(m_label);
        m_combobox = new JComboBox<>();
        for (int i = 0; i < pretties.length; i++) {
            m_byPretty.put(pretties[i], ids[i]);
            m_combobox.addItem(pretties[i]);
        }
        getComponentPanel().add(m_combobox);
        m_combobox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    try {
                        updateModel();
                    } catch (final InvalidSettingsException ise) {
                        // ignore
                    }
                }
            }
        });
        getModel().addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                updateComponent();
            }
        });
        updateComponent();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void updateComponent() {
        final String hidden = ((SettingsModelString)getModel()).getStringValue();
        final boolean updateSelection;
        if (hidden == null) {
            updateSelection = m_combobox.getSelectedItem() != null;
        } else {
            updateSelection =
                m_combobox.getSelectedItem() == null || !hidden.equals(m_byPretty.get(m_combobox.getSelectedItem()));
        }
        if (updateSelection) {
            m_combobox.setSelectedItem(m_byPretty.inverse().get(hidden));
        }
        setEnabledComponents(getModel().isEnabled());
        final String visible = (String)m_combobox.getSelectedItem();
        try {
            final boolean updateModel;
            if (visible == null) {
                updateModel = hidden != null;
            } else {
                updateModel = hidden == null || !hidden.equals(m_byPretty.get(m_combobox.getSelectedItem()));
            }
            if (updateModel) {
                updateModel();
            }
        } catch (final InvalidSettingsException e) {
            // ignore
        }
    }

    private void updateModel() throws InvalidSettingsException {
        if (m_combobox.getSelectedItem() == null) {
            ((SettingsModelString)getModel()).setStringValue(null);
            m_combobox.setBackground(Color.RED);
            m_combobox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {
                    m_combobox.setBackground(DialogComponent.DEFAULT_BG);
                }
            });
            throw new InvalidSettingsException("Please select an item from the list.");
        }
        final String hidden = m_byPretty.get(m_combobox.getSelectedItem());
        ((SettingsModelString)getModel()).setStringValue(hidden);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettingsBeforeSave() throws InvalidSettingsException {
        updateModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void checkConfigurabilityBeforeLoad(final PortObjectSpec[] specs) throws NotConfigurableException {
        // no op
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setEnabledComponents(final boolean enabled) {
        m_combobox.setEnabled(enabled);
    }

    /**
     * Sets the preferred size of the internal component.
     *
     * @param width The width.
     * @param height The height.
     */
    public void setSizeComponents(final int width, final int height) {
        m_combobox.setPreferredSize(new Dimension(width, height));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setToolTipText(final String text) {
        m_label.setToolTipText(text);
        m_combobox.setToolTipText(text);
    }

    /**
     * Replaces the list of selectable strings in the component. If <code>select</code> is specified (not null) and it
     * exists in the collection it will be selected. If <code>select</code> is null, the previous value will stay
     * selected (if it exists in the new list).
     *
     * @param selectedPretty the item to select after the replace. Can be null, in which case the previous selection
     *            remains - if it exists in the new list.
     */
    public void replaceListItems(final String[] pretties, final String[] ids, final String selectedPretty) {
        checkNotNull(pretties);
        checkNotNull(ids);
        checkArgument(pretties.length == ids.length);
        m_byPretty.clear();
        m_combobox.removeAllItems();
        for (int i = 0; i < pretties.length; i++) {
            m_byPretty.put(pretties[i], ids[i]);
            m_combobox.addItem(pretties[i]);
        }
        final String visible;
        if (selectedPretty == null) {
            final String hidden = ((SettingsModelString)getModel()).getStringValue();
            visible = m_byPretty.inverse().get(hidden);
        } else {
            visible = selectedPretty;
        }
        if (visible == null) {
            m_combobox.setSelectedIndex(0);
        } else {
            m_combobox.setSelectedItem(visible);
        }
        m_combobox.setSize(m_combobox.getPreferredSize());
        getComponentPanel().validate();
    }
}
