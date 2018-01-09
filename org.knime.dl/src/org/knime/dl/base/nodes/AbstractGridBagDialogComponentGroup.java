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
 * History
 *   25.08.2016 (David Kolb): created
 */
package org.knime.dl.base.nodes;

import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTextField;

import org.jdesktop.swingx.JXCollapsiblePane;
import org.jdesktop.swingx.JXCollapsiblePane.Direction;
import org.knime.core.data.DataValue;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter2;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentNumberEdit;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.DialogComponentStringSelection;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;
import org.knime.core.node.defaultnodesettings.SettingsModelNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.ColumnSelectionPanel;
import org.knime.dl.base.settings.ConfigEntry;

/**
 * Abstract implementation of a {@link IDialogComponentGroup} using a GridBagLayout to layout the contained
 * {@link DialogComponent}s. The saving and loading to the NodeSettings is handled automatically. The class allows to
 * add common combinations of DialogComponents to its panel as well as to make the collapsible in order to create
 * context sensitive dialogs.
 *
 * @author David Kolb, KNIME.com GmbH
 */
public abstract class AbstractGridBagDialogComponentGroup implements IDialogComponentGroup {

	private static final int NUMBER_OF_COLUMNS = 3;

	private static final NodeLogger logger = NodeLogger.getLogger(AbstractGridBagDialogComponentGroup.class);

	private final JPanel m_wrapperPanel;

	private JPanel m_dynamicPanel;

	private final List<JXCollapsiblePane> m_collapsibles;

	private JXCollapsiblePane m_collapseWrapper;

	private boolean m_useDynamicPanel;

	private int m_currentRow;

	/**
	 * Constructor for class AbstractGridBagDialogComponentGroup.
	 */
	protected AbstractGridBagDialogComponentGroup() {
		m_wrapperPanel = new JPanel(new GridBagLayout());
		m_collapsibles = new ArrayList<>();
		m_collapseWrapper = new JXCollapsiblePane();
		m_currentRow = 0;
		m_useDynamicPanel = false;
	}

	/**
	 * Starts a dynamic/collapsible group. This means that after this method call all further rows will be added to this
	 * groups so they can be collapsed together. Rows will be added to this group until <code>endDynamicGroup()</code>
	 * is be called. Each group can be identified by its index which is in the order of the call of this method. Indexes
	 * are zero based. E.g. first group has index 0, second group index 1 and so on.
	 */
	protected void startDynamicGroup() {
		m_dynamicPanel = new JPanel(new GridBagLayout());
		m_useDynamicPanel = true;
	}

	/**
	 * Ends the current dynamic/collapsible group specifying whether the group should be collapsed or expanded by
	 * default.
	 *
	 * @param isCollapsed whether the group should be collapsed by default
	 */
	protected void endDynamicGroup(final boolean isCollapsed) {
		m_useDynamicPanel = false;

		// wrap the dynamic panel with a collapsible panel
		m_collapseWrapper = new JXCollapsiblePane(Direction.UP);
		m_collapseWrapper.setCollapsed(isCollapsed);
		m_collapseWrapper.add(m_dynamicPanel);

		// save all collapsible panels to identify them for collapse/expand actions
		m_collapsibles.add(m_collapseWrapper);

		// add the collapsible dynamic panel to the dialog
		final GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(0, 20, 0, 20);
		gbc.gridx = 0;
		gbc.gridy = m_currentRow;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.gridwidth = NUMBER_OF_COLUMNS;

		addToPanel(m_collapseWrapper, gbc);
		m_currentRow++;
	}

	/**
	 * Adds a row containing the specified component.
	 *
	 * @param comp
	 */
	protected void addComponent(final Component comp) {
		final GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(0, 20, 0, 20);
		gbc.gridx = 0;
		gbc.gridy = m_currentRow;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.gridwidth = NUMBER_OF_COLUMNS;

		addToPanel(comp, gbc);
		m_currentRow++;
	}

	/**
	 * Collapses the group with the specified index.
	 *
	 * @param index
	 */
	protected void collapseDynamicGroup(final int index) {
		try {
			m_collapsibles.get(index).setCollapsed(true);
		} catch (final IndexOutOfBoundsException e) {
			logger.coding("No collapsible panel with index: " + index + " existing!", e);
		}
	}

	/**
	 * Expands the group with the specified inedx.
	 *
	 * @param index
	 */
	protected void expandDynamicGroup(final int index) {
		try {
			m_collapsibles.get(index).setCollapsed(false);
		} catch (final IndexOutOfBoundsException e) {
			logger.coding("No collapsible panel with index: " + index + " existing!", e);
		}

	}

	/**
	 * Toggels the collapse status of the group with the specified index.
	 *
	 * @param index
	 */
	protected void toggleDynamicGroup(final int index) {
		if (m_collapseWrapper.isCollapsed()) {
			expandDynamicGroup(index);
		} else {
			collapseDynamicGroup(index);
		}
	}

	/**
	 * Collapses all groups.
	 */
	protected void collapseAll() {
		for (final JXCollapsiblePane p : m_collapsibles) {
			p.setCollapsed(true);
		}
	}

	/**
	 * Expands all groups.
	 */
	protected void expandAll() {
		for (final JXCollapsiblePane p : m_collapsibles) {
			p.setCollapsed(false);
		}
	}

	/**
	 * Adds a row containing three components.
	 *
	 * @param compLeft
	 * @param compMiddle
	 * @param compRight
	 */
	private void addTripleColumnRow(final Component compLeft, final Component compMiddle, final Component compRight) {
		final GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(5, 10, 5, 10);
		gbc.gridx = 0;
		gbc.gridy = m_currentRow;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.WEST;
		addToPanel(compLeft, gbc);

		gbc.gridx++;
		gbc.weightx = 0.5;
		addToPanel(compMiddle, gbc);

		gbc.gridx++;
		addToPanel(compRight, gbc);
		m_currentRow++;
	}

	/**
	 * Adds a row containing two components.
	 *
	 * @param compLeft
	 * @param compRight
	 */
	protected void addDoubleColumnRow(final Component compLeft, final Component compRight) {
		final GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(5, 10, 5, 10);
		gbc.gridx = 0;
		gbc.gridy = m_currentRow;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.fill = GridBagConstraints.NONE;
		gbc.anchor = GridBagConstraints.WEST;
		addToPanel(compLeft, gbc);

		gbc.gridx++;
		gbc.weightx = 1;
		gbc.weighty = 0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridwidth = NUMBER_OF_COLUMNS - 1;

		addToPanel(compRight, gbc);
		m_currentRow++;
	}

	/**
	 * Adds a row containing one component using the specified 'fill' value for the constraints.
	 *
	 * @param comp
	 * @param fill
	 */
	private void addSingleColumnRow(final Component comp, final int fill) {
		final GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(5, 10, 5, 10);
		gbc.gridx = 0;
		gbc.gridy = m_currentRow;
		gbc.weightx = 1;
		gbc.weighty = 0;
		gbc.fill = fill;
		gbc.anchor = GridBagConstraints.WEST;
		gbc.gridwidth = NUMBER_OF_COLUMNS;

		addToPanel(comp, gbc);
		m_currentRow++;
	}

	/**
	 * Adds the specified component to the correct panel using the specified constrains depending on whether a dynamic
	 * group has currently be started or not.
	 *
	 * @param comp
	 * @param gbc
	 */
	private void addToPanel(final Component comp, final GridBagConstraints gbc) {
		if (m_useDynamicPanel) {
			m_dynamicPanel.add(comp, gbc);
		} else {
			m_wrapperPanel.add(comp, gbc);
		}
	}

	protected void addToggleComponentGroup(final ConfigEntry<?> entry, final String label,
			final IDialogComponentGroup componentGroup) {
		final SettingsModelBoolean toggleSettings = new SettingsModelBoolean(entry.getEntryKey() + "_toggle_settings",
				entry.getEnabled());
		entry.addLoadListener(e -> toggleSettings.setBooleanValue(e.getEnabled()));
		entry.addEnableChangeListener(e -> toggleSettings.setBooleanValue(e.getEnabled()));
		toggleSettings.addChangeListener(l -> entry.setEnabled(toggleSettings.getBooleanValue()));

		final DialogComponentBoolean booleanComponent = new DialogComponentBoolean(toggleSettings, label);

		addDoubleColumnRow(booleanComponent.getComponentPanel(), componentGroup.getComponentGroupPanel());
	}

	protected void addToggleNumberEditRowComponent(final ConfigEntry<?> entry, final String label,
			final SettingsModelNumber numberSettings) {
		final SettingsModelBoolean toggleSettings = new SettingsModelBoolean(entry.getEntryKey() + "_toggle_settings",
				entry.getEnabled());
		entry.addLoadListener(e -> toggleSettings.setBooleanValue(e.getEnabled()));
		entry.addEnableChangeListener(e -> toggleSettings.setBooleanValue(e.getEnabled()));
		toggleSettings.addChangeListener(l -> entry.setEnabled(toggleSettings.getBooleanValue()));

		final DialogComponentBoolean booleanComponent = new DialogComponentBoolean(toggleSettings, label);
		final DialogComponentNumberEdit numberComponent = new DialogComponentNumberEdit(numberSettings, label, 7);
		final JTextField textFieldComp = getFirstComponent(numberComponent, JTextField.class);
		textFieldComp.setHorizontalAlignment(JTextField.RIGHT);

		addDoubleColumnRow(booleanComponent.getComponentPanel(), textFieldComp);
	}

	/**
	 * Adds a row containing a checkbox and two number edit fields. The checkbox is intended to toggle the enable status
	 * of the number edit fields.
	 *
	 * @param toggleSettings the checkbox settings
	 * @param toggleLabel the checkbox label
	 * @param leftNumberSettings the settings of the left number edit field
	 * @param leftLabel the label of the left number edit field
	 * @param rightNumberSettings the settings of the right number edit field
	 * @param rightLabel the label of the right number edit field
	 */
	protected void addToggleDoubleNumberEditRowComponent(final SettingsModelBoolean toggleSettings,
			final String toggleLabel, final SettingsModelNumber leftNumberSettings, final String leftLabel,
			final SettingsModelNumber rightNumberSettings, final String rightLabel) {
		final DialogComponentBoolean booleanComponent = new DialogComponentBoolean(toggleSettings, toggleLabel);
		final DialogComponentNumberEdit leftNumberComponent = new DialogComponentNumberEdit(leftNumberSettings,
				leftLabel, 7);
		final DialogComponentNumberEdit rightNumberComponent = new DialogComponentNumberEdit(rightNumberSettings,
				rightLabel, 7);

		final JTextField leftTextFieldComp = getFirstComponent(leftNumberComponent, JTextField.class);
		final JLabel leftLabelComp = getFirstComponent(leftNumberComponent, JLabel.class);
		final JPanel leftNumberPanel = new JPanel(new GridBagLayout());

		final GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(0, 5, 0, 5);
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.CENTER;
		leftNumberPanel.add(leftLabelComp, gbc);
		gbc.gridx++;
		gbc.weightx = 1;
		leftNumberPanel.add(leftTextFieldComp, gbc);

		final JTextField rightTextFieldComp = getFirstComponent(rightNumberComponent, JTextField.class);
		final JLabel rightLabelComp = getFirstComponent(rightNumberComponent, JLabel.class);
		final JPanel rightNumberPanel = new JPanel(new GridBagLayout());
		gbc.insets = new Insets(0, 5, 0, 5);
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.CENTER;
		rightNumberPanel.add(rightLabelComp, gbc);
		gbc.gridx++;
		gbc.weightx = 1;
		rightNumberPanel.add(rightTextFieldComp, gbc);

		addTripleColumnRow(booleanComponent.getComponentPanel(), leftNumberPanel, rightNumberPanel);
	}

	/**
	 * Adds a row containing a checkbox, a string edit, and a number edit field. The checkbox is intended to toggle the
	 * enable status of the string and number edit field.
	 *
	 * @param toggleSettings the checkbox settings
	 * @param toggleLabel the checkbox label
	 * @param leftNumberSettings the settings of the left number edit field
	 * @param leftLabel the label of the left number edit field
	 * @param rightStringSettings the setting of the right string edit field
	 * @param rightLabel the label of the right string edit field
	 */
	protected void addToggleNumberAndStringEditRowComponent(final SettingsModelBoolean toggleSettings,
			final String toggleLabel, final SettingsModelNumber leftNumberSettings, final String leftLabel,
			final SettingsModelString rightStringSettings, final String rightLabel) {
		final DialogComponentBoolean booleanComponent = new DialogComponentBoolean(toggleSettings, toggleLabel);
		final DialogComponentNumberEdit leftNumberComponent = new DialogComponentNumberEdit(leftNumberSettings,
				leftLabel, 7);
		final DialogComponentString rightStringComponent = new DialogComponentString(rightStringSettings, rightLabel);

		final JTextField leftTextFieldComp = getFirstComponent(leftNumberComponent, JTextField.class);
		final JLabel leftLabelComp = getFirstComponent(leftNumberComponent, JLabel.class);
		final JPanel leftNumberPanel = new JPanel(new GridBagLayout());

		final GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(0, 5, 0, 5);
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.CENTER;
		leftNumberPanel.add(leftLabelComp, gbc);
		gbc.gridx++;
		gbc.weightx = 1;
		leftNumberPanel.add(leftTextFieldComp, gbc);

		final JTextField rightTextFieldComp = getFirstComponent(rightStringComponent, JTextField.class);
		final JLabel rightLabelComp = getFirstComponent(rightStringComponent, JLabel.class);
		final JPanel rightNumberPanel = new JPanel(new GridBagLayout());
		gbc.insets = new Insets(0, 5, 0, 5);
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 0;
		gbc.weighty = 0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.CENTER;
		rightNumberPanel.add(rightLabelComp, gbc);
		gbc.gridx++;
		gbc.weightx = 1;
		rightNumberPanel.add(rightTextFieldComp, gbc);

		addTripleColumnRow(booleanComponent.getComponentPanel(), leftNumberPanel, rightNumberPanel);
	}

	/**
	 * Adds a row containing a checkbox and a combobox. The checkbox is intended to toggle the enable status of the
	 * combobox.
	 *
	 * @param toggleSettings the checkbox settings
	 * @param label the checkbox label
	 * @param comboBoxSettings the settings of the combobox
	 * @param values the values to be displayed in the combobox
	 */
	protected void addToggleComboBoxRow(final SettingsModelBoolean toggleSettings, final String label,
			final SettingsModelString comboBoxSettings, final Collection<String> values) {
		final DialogComponentBoolean booleanComponent = new DialogComponentBoolean(toggleSettings, label);
		final DialogComponentStringSelection comboBoxComponent = new DialogComponentStringSelection(comboBoxSettings,
				label, values);

		final JComboBox<?> comboBoxComp = getFirstComponent(comboBoxComponent, JComboBox.class);

		addDoubleColumnRow(booleanComponent.getComponentPanel(), comboBoxComp);
	}

	/**
	 * Adds a row containing a checkbox and a number edit field. The checkbox is intended to toggle the enable status of
	 * the number edit field.
	 *
	 * @param toggleSettings the checkbox settings
	 * @param label the checkbox label
	 * @param numberSettings the settings of the number edit field
	 */
	protected DialogComponentNumberEdit addToggleNumberEditRowComponent(final SettingsModelBoolean toggleSettings,
			final String label, final SettingsModelNumber numberSettings) {
		final DialogComponentBoolean booleanComponent = new DialogComponentBoolean(toggleSettings, label);
		final DialogComponentNumberEdit numberComponent = new DialogComponentNumberEdit(numberSettings, label, 7);

		final JTextField textFieldComp = getFirstComponent(numberComponent, JTextField.class);

		addDoubleColumnRow(booleanComponent.getComponentPanel(), textFieldComp);

		return numberComponent;
	}

	/**
	 * Adds a row containing a checkbox and a string edit field. The checkbox is intended to toggle the enable status of
	 * the string edit field.
	 *
	 * @param toggleSettings the checkbox settings
	 * @param label the checkbox label
	 * @param stringSettings the settings of the string edit field
	 */
	protected void addToggleStringEditRowComponent(final SettingsModelBoolean toggleSettings, final String label,
			final SettingsModelString stringSettings) {
		final DialogComponentBoolean booleanComponent = new DialogComponentBoolean(toggleSettings, label);
		final DialogComponentString stringComponent = new DialogComponentString(stringSettings, label);

		final JTextField textFieldComp = getFirstComponent(stringComponent, JTextField.class);

		addDoubleColumnRow(booleanComponent.getComponentPanel(), textFieldComp);
	}

	/**
	 * Adds a row containing a string edit field.
	 *
	 * @param settings the settings of the string edit field
	 * @param label the label of the string edit field
	 */
	protected void addStringEditRowComponent(final SettingsModelString settings, final String label) {
		final DialogComponentString stringComponent = new DialogComponentString(settings, label);

		final JLabel labelComp = getFirstComponent(stringComponent, JLabel.class);
		final JTextField textFieldComp = getFirstComponent(stringComponent, JTextField.class);

		if (label.isEmpty()) {
			addSingleColumnRow(textFieldComp, GridBagConstraints.HORIZONTAL);
		} else {
			addDoubleColumnRow(labelComp, textFieldComp);
		}
	}

	/**
	 * Adds a row containing a number edit field.
	 *
	 * @param settings the settings of the number edit field
	 * @param label the label of the number edit field
	 */
	protected void addNumberEditRowComponent(final SettingsModelNumber settings, final String label) {
		final DialogComponentNumberEdit numberComponent = new DialogComponentNumberEdit(settings, label, 7);

		final JLabel labelComp = getFirstComponent(numberComponent, JLabel.class);
		final JTextField textFieldComp = getFirstComponent(numberComponent, JTextField.class);
		textFieldComp.setHorizontalAlignment(JTextField.RIGHT);

		addDoubleColumnRow(labelComp, textFieldComp);
	}

	/**
	 * Adds a row containing a number spinner field.
	 *
	 * @param settings the settings of the number spinner field
	 * @param label the label of the number spinner field
	 * @param stepSize the step size of the spinner
	 */
	protected DialogComponentNumber addNumberSpinnerRowComponent(final SettingsModelNumber settings, final String label,
			final double stepSize) {
		final DialogComponentNumber numberComponent = new DialogComponentNumber(settings, label, stepSize, 7);

		final JLabel labelComp = getFirstComponent(numberComponent, JLabel.class);
		final JSpinner spinnerComp = getFirstComponent(numberComponent, JSpinner.class);

		addDoubleColumnRow(labelComp, spinnerComp);

		return numberComponent;
	}

	/**
	 * Adds a row containing a checkbox. You can specify if the the checkbox and its label should be aligned left or if
	 * it should align to columns if there are other rows containing more than one column.
	 *
	 * @param settings the settings of the checkbox
	 * @param label the label of the checkbox
	 * @param alignLeft align left if true, align to columns if false
	 */
	protected void addCheckboxRow(final SettingsModelBoolean settings, final String label, final boolean alignLeft) {
		DialogComponentBoolean booleanComponent;

		if (alignLeft) {
			booleanComponent = new DialogComponentBoolean(settings, label);
			addSingleColumnRow(booleanComponent.getComponentPanel(), GridBagConstraints.NONE);
		} else {
			booleanComponent = new DialogComponentBoolean(settings, "");
			final JLabel labelComp = new JLabel(label);
			addDoubleColumnRow(booleanComponent.getComponentPanel(), labelComp);
		}
	}

	/**
	 * Adds a row containing a combobox.
	 *
	 * @param settings the settings of the combobox
	 * @param label the label of the combobox
	 * @param values the values to be displayed in the combobox
	 */
	protected void addComboBoxRow(final SettingsModelString settings, final String label,
			final Collection<String> values) {
		final DialogComponentStringSelection comboBoxComponent = new DialogComponentStringSelection(settings, label,
				values);

		final JLabel labelComp = getFirstComponent(comboBoxComponent, JLabel.class);
		final JComboBox<?> comboBoxComp = getFirstComponent(comboBoxComponent, JComboBox.class);

		addDoubleColumnRow(labelComp, comboBoxComp);
	}

	/**
	 * Adds a row containing a column name selection combobox.
	 *
	 * @param settings the settings of the column name selection combobox
	 * @param label the label of the column name selection combobox
	 * @param specIndex the port index the column name selection combobox should scan for possible columns
	 * @param classFilter the allowed types
	 */
	@SuppressWarnings("unchecked")
	protected void addColumnNameSelectionRowComponent(final SettingsModelString settings, final String label,
			final int specIndex, final Class<? extends DataValue>... classFilter) {
		final DialogComponentColumnNameSelection nameSelectionComponent = new DialogComponentColumnNameSelection(
				settings, label, specIndex, classFilter);

		final JLabel labelComp = getFirstComponent(nameSelectionComponent, JLabel.class);
		final ColumnSelectionPanel selectionComp = getFirstComponent(nameSelectionComponent,
				ColumnSelectionPanel.class);

		addDoubleColumnRow(labelComp, selectionComp);
	}

	/**
	 * Adds a row containing a column filter component.
	 *
	 * @param settings the settings of the column filter component
	 * @param specIndex the port index the column filter component should scan for possible columns
	 */
	protected void addColumnFilterRowComponent(final SettingsModelColumnFilter2 settings, final int specIndex) {
		final DialogComponentColumnFilter2 columnFilterComp = new DialogComponentColumnFilter2(settings, specIndex);

		addSingleColumnRow(columnFilterComp.getComponentPanel(), GridBagConstraints.HORIZONTAL);
	}

	/**
	 * Adds a row containing a label.
	 *
	 * @param label the label to add
	 * @param fontSize the font size of the label
	 */
	protected void addLabelRow(final String label, final int fontSize) {
		final JLabel labelComp = new JLabel(label);
		if (fontSize != 0) {
			labelComp.setFont(new Font(labelComp.getFont().getName(), labelComp.getFont().getStyle(), fontSize));
		}
		addSingleColumnRow(labelComp, GridBagConstraints.NONE);
	}

	/**
	 * Adds a row containing a label.
	 *
	 * @param label the label to add
	 */
	protected void addLabelRow(final String label) {
		addLabelRow(label, 0);
	}

	/**
	 * Adds a whitespace row with the specified height.
	 *
	 * @param height the height of the whitespace
	 */
	protected void addWhitespaceRow(final int height) {
		final JPanel whitespace = new JPanel();

		final GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(height, 0, 0, 0);
		gbc.gridx = 0;
		gbc.gridy = m_currentRow;
		gbc.weightx = 1;
		gbc.weighty = 0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.gridwidth = NUMBER_OF_COLUMNS;

		addToPanel(whitespace, gbc);
		m_currentRow++;
	}

	/**
	 * Adds a row containing a horizontal separator line.
	 */
	protected void addHorizontalSeparator() {
		final JSeparator hs = new JSeparator();

		final GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(5, 10, 5, 10);
		gbc.gridx = 0;
		gbc.gridy = m_currentRow;
		gbc.weightx = 1;
		gbc.weighty = 0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.CENTER;
		gbc.gridwidth = NUMBER_OF_COLUMNS;

		addToPanel(hs, gbc);
		m_currentRow++;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public JPanel getComponentGroupPanel() {
		return m_wrapperPanel;
	}

	/**
	 * Returns the first found component contained in the specified component matching the specified class.
	 *
	 * @param comp the component to search in
	 * @param compClass the class to search for
	 * @return the first component matching the specified class
	 */
	@SuppressWarnings("unchecked")
	protected <T extends Component> T getFirstComponent(final DialogComponent comp, final Class<T> compClass) {
		for (final Component c : comp.getComponentPanel().getComponents()) {
			if (compClass.isInstance(c)) {
				return (T) c;
			}
		}
		return null;
	}
}
