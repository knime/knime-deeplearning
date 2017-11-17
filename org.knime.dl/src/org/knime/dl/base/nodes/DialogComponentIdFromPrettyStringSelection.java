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
package org.knime.dl.base.nodes;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.util.HashMap;
import java.util.function.Consumer;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.event.ChangeEvent;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;
import org.knime.core.node.port.PortObjectSpec;

/**
 * Modified from {@link DialogComponentStringSelection}.
 * <P>
 * Maintains a settings model of two strings where the "pretty string" is selectable by the user via the component's
 * combo box and and the other one is treated as a hidden id counterpart which is of interest to the node model.
 *
 * @see DialogComponentStringSelection
 *
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public final class DialogComponentIdFromPrettyStringSelection extends DialogComponent {

	private final HashMap<String, String> m_byPretty;

	private final JLabel m_label;

	private final JComboBox<String> m_combobox;

	private final Consumer<ChangeEvent> m_selectionChangeListener;

	/**
	 * Creates a new instance of this dialog component.
	 *
	 * @param stringPairModel the underlying settings model, whose string array must be of size two (elements can be
	 *            null at the beginning): the first element will be used to store the pretty string, the second one to
	 *            store the id
	 * @param label the label
	 */
	public DialogComponentIdFromPrettyStringSelection(final SettingsModelStringArray stringPairModel,
			final String label, final Consumer<ChangeEvent> selectionChangeListener) {
		super(stringPairModel);
		checkArgument(checkNotNull(stringPairModel.getStringArrayValue()).length == 2);
		checkNotNull(selectionChangeListener);
		m_byPretty = new HashMap<>();
		m_label = new JLabel(label);
		getComponentPanel().add(m_label);
		m_combobox = new JComboBox<>();
		getComponentPanel().add(m_combobox);
		m_selectionChangeListener = selectionChangeListener;

		getModel().addChangeListener(e -> updateComponent());

		m_combobox.addItemListener(e -> {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				onSelectionChanged();
			}
		});
	}

	/**
	 * Returns the currently selected pretty name-id pair.
	 *
	 * @return the current selection
	 */
	public String[] getSelection() {
		final Object selectedPretty = m_combobox.getSelectedItem();
		return new String[] { (String) selectedPretty, m_byPretty.get(selectedPretty) };
	}

	/**
	 * Replaces the list of selectable strings in the component. If <code>selectedPretty</code> is specified (not null)
	 * and it exists in the collection, it will be selected. If <code>selectedPretty</code> is null, the pretty entry
	 * that corresponds to the previous hidden's value will stay selected (if it exists in the new list).
	 *
	 * @param newPretties the items that will be displayed in the dialog component. No null values, no duplicate values.
	 *            Must be at least of length one.
	 * @param newIds the hidden counterparts of the visible items. No null values, no duplicate values. Must be at least
	 *            of length one.
	 * @param newSelectedPretty the item to select after the replace. Can be null, in which case the previous selection
	 *            is tried to be preserved.
	 */
	public void replaceListItems(final String[] newPretties, final String[] newIds, final String newSelectedPretty) {
		checkNotNull(newPretties);
		checkNotNull(newIds);
		checkArgument(newPretties.length > 0 && newPretties.length == newIds.length);
		final String newPretty;
		if (newSelectedPretty == null) {
			newPretty = getStringArrayModel().getStringArrayValue()[0];
		} else {
			newPretty = newSelectedPretty;
		}
		m_byPretty.clear();
		m_combobox.removeAllItems();
		for (int i = 0; i < newPretties.length; i++) {
			m_byPretty.put(newPretties[i], newIds[i]);
			m_combobox.addItem(newPretties[i]);
		}
		if (newPretty == null) {
			m_combobox.setSelectedIndex(0);
		} else {
			m_combobox.setSelectedItem(newPretty);
		}
		m_combobox.setSize(m_combobox.getPreferredSize());
		getComponentPanel().validate();
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

	@Override
	public void setToolTipText(final String text) {
		m_label.setToolTipText(text);
		m_combobox.setToolTipText(text);
	}

	@Override
	protected void updateComponent() {
		final String[] newPrettyHidden = getStringArrayModel().getStringArrayValue();
		final String oldPretty = (String) m_combobox.getSelectedItem();
		final boolean updateSelection;
		if (newPrettyHidden[0] == null) {
			updateSelection = oldPretty != null;
		} else {
			updateSelection = !newPrettyHidden[0].equals(oldPretty);
		}
		if (updateSelection) {
			m_combobox.setSelectedItem(newPrettyHidden[0]);
		}
		setEnabledComponents(getModel().isEnabled());
		final String newPretty = (String) m_combobox.getSelectedItem();
		final boolean selectionChanged;
		if (newPretty == null) {
			selectionChanged = newPrettyHidden[0] != null;
		} else {
			selectionChanged = !newPretty.equals(newPrettyHidden[0]);
		}
		if (selectionChanged) {
			onSelectionChanged();
		}
	}

	@Override
	protected void validateSettingsBeforeSave() throws InvalidSettingsException {
		try {
			onSelectionChanged();
		} catch (final Exception e) {
			throw new InvalidSettingsException(e.getMessage(), e);
		}
	}

	@Override
	protected void checkConfigurabilityBeforeLoad(final PortObjectSpec[] specs) throws NotConfigurableException {
		// no op
	}

	@Override
	protected void setEnabledComponents(final boolean enabled) {
		m_combobox.setEnabled(enabled);
	}

	private void onSelectionChanged() {
		final String newPretty = (String) m_combobox.getSelectedItem();
		if (newPretty == null) {
			m_combobox.setBackground(Color.RED);
			m_combobox.addActionListener(e -> m_combobox.setBackground(DialogComponent.DEFAULT_BG));
			m_selectionChangeListener.accept(new ChangeEvent(this));
			throw new IllegalStateException("Please select an item from the list.");
		}
		m_selectionChangeListener.accept(new ChangeEvent(this));
	}

	private SettingsModelStringArray getStringArrayModel() {
		return (SettingsModelStringArray) getModel();
	}
}
