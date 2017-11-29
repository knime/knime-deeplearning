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
package org.knime.dl.keras.base.nodes.learner.view;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

/**
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author David Kolb, KNIME GmbH, Konstanz, Germany
 */
public class DLLearningProgressBar extends JPanel {

	private static final long serialVersionUID = 1L;

	private static final String PROGRESS_COUNTER_FORMAT = "%d / %d";

	private final JProgressBar m_progressBar;

	private final JLabel m_progressCounter;

	private final JLabel m_timeCounter;

	public DLLearningProgressBar(final String progressLabel, final String timeLabel) {
		super(new GridBagLayout());

		m_progressBar = new JProgressBar();
		m_progressBar.setPreferredSize(new Dimension(100, 30));

		final GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(5, 5, 5, 5);
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 0.8;
		gbc.weighty = 0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		add(m_progressBar, gbc);

		m_progressCounter = new JLabel();
		m_progressCounter.setText(String.format(PROGRESS_COUNTER_FORMAT, 0, 0));
		final JPanel progressCounterBox = new JPanel(new GridLayout(0, 1));
		progressCounterBox.setBorder(BorderFactory.createTitledBorder(progressLabel + ":"));
		progressCounterBox.add(m_progressCounter);
		progressCounterBox.setPreferredSize(new Dimension(100, 40));
		progressCounterBox.setMinimumSize(new Dimension(100, 40));
		gbc.insets = new Insets(5, 5, 5, 5);
		gbc.weightx = 0.1;
		gbc.gridx++;
		add(progressCounterBox, gbc);

		m_timeCounter = new JLabel();
		m_timeCounter.setText("0");
		final JPanel timeCounterBox = new JPanel(new GridLayout(0, 1));
		timeCounterBox.setBorder(BorderFactory.createTitledBorder(timeLabel + ":"));
		timeCounterBox.add(m_timeCounter);
		timeCounterBox.setPreferredSize(new Dimension(100, 40));
		timeCounterBox.setMinimumSize(new Dimension(100, 40));
		gbc.gridx++;
		add(timeCounterBox, gbc);
	}

	public void setProgress(final int progress) {
		m_progressBar.setValue(progress);
	}

	public void setProgressText(final int current, final int max) {
		m_progressCounter.setText(String.format(PROGRESS_COUNTER_FORMAT, current, max));
	}

	public void setTime(final int timeInSec) {
		m_timeCounter.setText(timeInSec + "");
	}

	public void setMaxProgress(final int maxProgress) {
		m_progressBar.setMaximum(maxProgress);
	}
}
