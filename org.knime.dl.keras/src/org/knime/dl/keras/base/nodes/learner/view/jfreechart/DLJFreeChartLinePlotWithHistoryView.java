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
package org.knime.dl.keras.base.nodes.learner.view.jfreechart;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.Timer;
import javax.swing.text.DefaultCaret;

import org.knime.dl.keras.base.nodes.learner.view.DLFloatData;
import org.knime.dl.keras.base.nodes.learner.view.DLView;

/**
 * DLView containing of a {@link JFreeChartLinePlotPanel} and a textual history view.
 *
 * @author David Kolb, KNIME GmbH, Konstanz, Germany
 */
public class DLJFreeChartLinePlotWithHistoryView implements DLView<DLJFreeChartLinePlotViewSpec> {

	private final JFreeChartLinePlotPanel m_linePlot;
	private final List<JTextArea> m_historyAreas = new ArrayList<>();
	private final List<JLabel> m_currentValueLabels = new ArrayList<>();
	private final float[] m_currentValues;

	private final JPanel m_component;

	private final Timer m_currentValueUpdateTimer = new Timer(1000, (e) -> updateCurrentValueLabels());

	public DLJFreeChartLinePlotWithHistoryView(final DLJFreeChartLinePlotViewSpec plotViewSpec) {
		m_component = new JPanel(new GridBagLayout());

		m_currentValues = new float[plotViewSpec.numPlots()];

		final JTabbedPane historyTabsPane = new JTabbedPane();
		GridBagConstraints gbc;

		for (int i = 0; i < plotViewSpec.numPlots(); i++) {
			final JTextArea historyArea = new JTextArea();
			final DefaultCaret caret = (DefaultCaret) historyArea.getCaret();
			// Enable automatic to bottom scrolling
			caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
			historyArea.setEditable(false);
			m_historyAreas.add(historyArea);

			final JScrollPane historyScroller = new JScrollPane(historyArea);
			final JPanel historyWrapper = new JPanel(new GridBagLayout());
			gbc = new GridBagConstraints();
			gbc.gridx = 0;
			gbc.gridy = 0;
			gbc.weightx = 1;
			gbc.weighty = 1;
			gbc.fill = GridBagConstraints.BOTH;
			historyWrapper.add(historyScroller, gbc);

			final JLabel currentValue = new JLabel("-");
			currentValue.setFont(new Font(currentValue.getFont().getName(), currentValue.getFont().getStyle(), 18));

			final JPanel valueWrapperWithBorder = new JPanel(new GridLayout(0, 1));
			valueWrapperWithBorder.setBorder(BorderFactory.createTitledBorder("Current Value:"));
			valueWrapperWithBorder.add(currentValue);
			m_currentValueLabels.add(currentValue);

			gbc.gridy++;
			gbc.weighty = 0;
			gbc.insets = new Insets(10, 10, 10, 10);
			historyWrapper.add(valueWrapperWithBorder, gbc);

			historyTabsPane.addTab(plotViewSpec.getLineLabel(i), historyWrapper);
		}

		m_linePlot = new JFreeChartLinePlotPanel(plotViewSpec);
		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 0.6;
		gbc.weighty = 1;
		gbc.insets = new Insets(0, 0, 0, 10);
		gbc.fill = GridBagConstraints.BOTH;
		m_component.add(m_linePlot.getComponent(), gbc);

		historyTabsPane.setPreferredSize(new Dimension(250, 500));
		historyTabsPane.setMinimumSize(new Dimension(250, 500));

		gbc.gridx = 1;
		gbc.weightx = 0.4;
		gbc.weighty = 1;
		m_component.add(historyTabsPane, gbc);
	}

	private void updateCurrentValueLabels() {
		for (int i = 0; i < m_currentValues.length; i++) {
			m_currentValueLabels.get(i).setText(m_currentValues[i] + "");
		}
	}

	public void setCurrentValueTimerUpdateDelay(final int miliseconds) {
		m_currentValueUpdateTimer.setDelay(miliseconds);
	}

	public void startCurrentValueUpdate() {
		if (!m_currentValueUpdateTimer.isRunning()) {
			m_currentValueUpdateTimer.start();
		}
	}

	public void stopCurrentValueUpdate() {
		if (m_currentValueUpdateTimer.isRunning()) {
			m_currentValueUpdateTimer.stop();
		}
	}

	@Override
	public Component getComponent() {
		return m_component;
	}

	@Override
	public void update(final DLJFreeChartLinePlotViewSpec spec, final Iterator<DLFloatData>[] iterators) {
		for (int i = 0; i < iterators.length; i++) {
			final Iterator<DLFloatData> it = iterators[i];
			while (it.hasNext()) {
				final float value = it.next().get();
				m_linePlot.plotNext(i, value);
				m_historyAreas.get(i).append(value + "\n");
				m_currentValues[i] = value;
			}
		}
	}

}
