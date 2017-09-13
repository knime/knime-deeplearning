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
package org.knime.dl.base.nodes;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.util.List;
import java.util.function.Function;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.dl.base.settings.ConfigEntry;

/**
 * Modified from {@link DialogComponentStringSelection}.
 *
 * @see DialogComponentStringSelection
 * @param <T> the item type of the selection component
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
public final class DialogComponentObjectSelection<T> extends DialogComponent {

	private final ConfigEntry<T> m_config;

	private final Function<? super T, String> m_printer;

	private final JLabel m_label;

	private final JComboBox<T> m_combobox;

	private boolean m_isReplacing = false;

	/**
	 * Creates a new instance of this dialog component.
	 *
	 * @param config the config which stores the component's selected item
	 * @param printer the function that turns the component's items into a renderable string representation
	 * @param label the label of the component
	 */
	public DialogComponentObjectSelection(final ConfigEntry<T> config, final Function<? super T, String> printer,
			final String label) {
		super(new SettingsModelString("dummy", "dummy"));
		m_config = config;
		m_printer = printer;
		// Build panel:
		m_label = new JLabel(label);
		getComponentPanel().add(m_label);
		m_combobox = new JComboBox<>();
		getComponentPanel().add(m_combobox);
		// String renderer for T
		final DefaultListCellRenderer renderer = new DefaultListCellRenderer() {

			private static final long serialVersionUID = 1L;

			@SuppressWarnings("unchecked") // we know that list items are of type T
			@Override
			public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index,
					final boolean isSelected, final boolean cellHasFocus) {
				return super.getListCellRendererComponent(list, value != null ? m_printer.apply((T) value) : "", index,
						isSelected, cellHasFocus);
			}
		};
		m_combobox.setRenderer(renderer);
		// Config changes
		config.addValueChangeOrLoadListener((e, oldValue) -> updateComponent());
		config.addEnabledChangeOrLoadListener((e, oldEnabled) -> updateComponent());
		// Selection changes
		m_combobox.addItemListener(e -> {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				onSelectionChanged();
			}
		});
	}

	public ConfigEntry<T> getConfigEntry() {
		return m_config;
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

	/**
	 * Replaces the list of selectable strings in the component. If <code>selectedPretty</code> is specified (not null)
	 * and it exists in the collection, it will be selected. If <code>selectedPretty</code> is null, the pretty entry
	 * that corresponds to the previous hidden's value will stay selected (if it exists in the new list).
	 *
	 * @param newItems the items that will be displayed in the dialog component. No null values, no duplicate values.
	 *            Must be at least of length one.
	 * @param newSelectedItem the item to select after replacing. Can be null, in which case the previous selection is
	 *            tried to be preserved.
	 */
	public void replaceListItems(final List<T> newItems, final T newSelectedItem) {
		checkNotNull(newItems);
		checkArgument(!newItems.isEmpty());
		final T newItem = newSelectedItem != null ? newSelectedItem : m_config.getValue();
		m_isReplacing = true;
		m_combobox.removeAllItems();
		for (int i = 0; i < newItems.size(); i++) {
			m_combobox.addItem(newItems.get(i));
		}
		if (newItem != null) {
			m_combobox.setSelectedItem(newItem);
		} else {
			m_combobox.setSelectedIndex(0);
		}
		m_isReplacing = false;
		m_combobox.setSize(m_combobox.getPreferredSize());
		getComponentPanel().validate();
		onSelectionChanged();
	}

	@SuppressWarnings("unchecked") // we know that list items are of type T
	@Override
	protected void updateComponent() {
		final T newValue = m_config.getValue();
		final T oldValue = (T) m_combobox.getSelectedItem();
		final boolean updateSelection;
		if (newValue == null) {
			updateSelection = oldValue != null;
		} else {
			updateSelection = !newValue.equals(oldValue);
		}
		if (updateSelection) {
			m_combobox.setSelectedItem(newValue);
		}
		setEnabledComponents(m_config.getEnabled());
		final T newValueAfterUpdate = (T) m_combobox.getSelectedItem();
		final boolean selectionChanged;
		if (newValueAfterUpdate == null) {
			selectionChanged = newValue != null;
		} else {
			selectionChanged = !newValueAfterUpdate.equals(newValue);
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

	@SuppressWarnings("unchecked") // we know that list items are of type T
	private void onSelectionChanged() {
		if (m_isReplacing) {
			return;
		}
		final T newValue = (T) m_combobox.getSelectedItem();
		m_config.setValue(newValue);
		if (newValue == null) {
			m_combobox.setBackground(Color.RED);
			m_combobox.addActionListener(e -> m_combobox.setBackground(DialogComponent.DEFAULT_BG));
			throw new IllegalStateException("Please select an item from the list.");
		}
	}
}
