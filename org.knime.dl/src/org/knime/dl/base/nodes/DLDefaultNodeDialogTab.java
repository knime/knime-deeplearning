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
 */
package org.knime.dl.base.nodes;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
public class DLDefaultNodeDialogTab {

	private final String m_title;

	private final JScrollPane m_rootScrollableWrapper;

	private final JPanel m_root;

	private final GridBagConstraints m_rootConstr;

	public DLDefaultNodeDialogTab(final String title) {
		m_title = title;
		m_root = new JPanel(new GridBagLayout());
		m_rootConstr = new GridBagConstraints();
		m_rootScrollableWrapper = new JScrollPane();
		final JPanel rootWrapper = new JPanel(new BorderLayout());
		rootWrapper.add(m_root, BorderLayout.NORTH);
		m_rootScrollableWrapper.setViewportView(rootWrapper);
		reset();
	}

	public String getTitle() {
		return m_title;
	}

	public JComponent getTab() {
		return m_rootScrollableWrapper;
	}

	public JPanel getTabRoot() {
		return m_root;
	}

	public GridBagConstraints getRootConstraints() {
		return m_rootConstr;
	}

	public void reset() {
		m_root.removeAll();
		m_rootConstr.gridx = 0;
		m_rootConstr.gridy = 0;
		m_rootConstr.gridwidth = 1;
		m_rootConstr.gridheight = 1;
		m_rootConstr.weightx = 1;
		m_rootConstr.weighty = 0;
		m_rootConstr.anchor = GridBagConstraints.WEST;
		m_rootConstr.fill = GridBagConstraints.BOTH;
		m_rootConstr.insets = new Insets(5, 5, 5, 5);
		m_rootConstr.ipadx = 0;
		m_rootConstr.ipady = 0;
	}
}
