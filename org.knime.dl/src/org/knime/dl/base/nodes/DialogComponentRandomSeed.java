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
package org.knime.dl.base.nodes;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Arrays;
import java.util.Random;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import org.knime.core.node.defaultnodesettings.DialogComponentNumberEdit;
import org.knime.core.node.defaultnodesettings.SettingsModelLong;

/**
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public class DialogComponentRandomSeed extends JPanel implements IDialogComponentGroup {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * @param randomSeedModel 
	 * 
	 */
	public DialogComponentRandomSeed(SettingsModelLong randomSeedModel) {
		DialogComponentNumberEdit field = new DialogComponentNumberEdit(randomSeedModel, "", 20);
		JButton newButton = new JButton("New seed");
		Random random = new Random();
		randomSeedModel.addChangeListener(e -> newButton.setEnabled(randomSeedModel.isEnabled()));
		newButton.addActionListener(e -> randomSeedModel.setLongValue(random.nextLong()));
		layoutComponents(getTextField(field.getComponentPanel()), newButton);
	}
	
	private JTextField getTextField(JPanel seedFieldPanel) {
		return (JTextField) Arrays.stream(seedFieldPanel.getComponents())
				.filter(c -> c instanceof JTextField)
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("The number edit component did not contain a text field."));
	}
	
	private void layoutComponents(JTextField seedTextField, JButton newButton) {
		setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		seedTextField.setHorizontalAlignment(SwingConstants.RIGHT);
		add(seedTextField, gbc);
		gbc.gridx = 1;
		gbc.weightx = 0;
		gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(0, 10, 0, 0);
        add(newButton, gbc);
	}

	@Override
	public JPanel getComponentGroupPanel() {
		return this;
	}

}
