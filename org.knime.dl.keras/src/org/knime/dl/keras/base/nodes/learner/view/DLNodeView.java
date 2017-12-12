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
import java.awt.Insets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;

import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeView;
import org.knime.dl.keras.base.nodes.learner.view.jfreechart.DLJFreeChartLinePlotViewSpec;
import org.knime.dl.keras.base.nodes.learner.view.jfreechart.DLJFreeChartLinePlotWithHistoryView;

/**
 * @author David Kolb, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 */
// TODO: actually this should just delegate to some object which can be anything (even a JavaScript thingy).
// We don't want to enforce an implementation here.
public class DLNodeView<M extends NodeModel & DLInteractiveLearnerNodeModel> extends NodeView<M> {

	private static final DateTimeFormatter START_TIME_DISPLAY_FORMATTER = DateTimeFormatter
			.ofPattern("E, dd MMM yyyy HH:mm:ss");

	private static final String ELAPSED_TIME_DISPLAY_FORMAT = "%02d:%02d:%02d (hh:mm:ss)";

	/**
	 * Alternative to setShowNODATALabel() because the NODATA label of the NodeView is also displayed during execution,
	 * which is exactly what we do not want.
	 */
	private static final JLabel NO_DATA_OVERLAY;

	static {
		NO_DATA_OVERLAY = new JLabel("<html><center>No data to display</center></html>", SwingConstants.CENTER);
		NO_DATA_OVERLAY.setPreferredSize(new Dimension(1000, 700));
	}

	private static String formatStartTime(final LocalDateTime startTime) {
		return startTime != null ? startTime.format(START_TIME_DISPLAY_FORMATTER) : "-";
	}

	private static String formatElapsedTime(final Duration elapsedTime) {
		if (elapsedTime == null) {
			return "-";
		}
		final long elapsedSeconds = elapsedTime.getSeconds();
		final long hours = elapsedSeconds / 3600;
		final int minutes = (int) ((elapsedSeconds % 3600) / 60);
		final int secs = (int) (elapsedSeconds % 60);
		return String.format(ELAPSED_TIME_DISPLAY_FORMAT, hours, minutes, secs);
	}

	private Map<String, DLJFreeChartLinePlotWithHistoryView> m_views;

	/**
	 * Data iterators for this view. Its important that each view has its own iterator state if we open several views at
	 * once.
	 */
	private Map<String, Iterator<DLFloatData>[]> m_dataIterators;

	private final DLViewSpec[] m_specs;

	private DLLearningProgressBar m_epochProgressBar;

	private DLLearningProgressBar m_batchProgressBar;

	private LeftAlignLabelWithValue m_startTime;

	private LeftAlignLabelWithValue m_elapsedTime;

	private LeftAlignButton m_stopButton;

	private JPanel m_mainContainer;

	private final DLProgressMonitor m_progressMonitor;

	private int m_lastEpoch = 0;

	/**
	 * Flag indicating if the data iterators have been initialized.
	 */
	private boolean m_areIteratorsInit;

	public DLNodeView(final M model, final DLViewSpec... specs) {
		super(model);
		// Use own NODATA label instead of NodeView impl
		setShowNODATALabel(false);
		showNoDataOverlay(true);
		m_specs = specs;
		m_progressMonitor = model.getProgressMonitor();
		m_areIteratorsInit = false;

		initView();
		updateView(m_progressMonitor);
	}

	/**
	 * Set data iterators of this view using the the specified view data array. If the iterators are already initialized
	 * this method will do nothing.
	 *
	 * @param dataArray data to get iterators from
	 * @throws IllegalArgumentException If the dataArray is null or contains null.
	 */
	@SuppressWarnings("unchecked")
	private void initDataIterators(final DLViewData<?>[] dataArray) {
		if (!m_areIteratorsInit) {
			try {
				for (final DLViewData<?> viewData : dataArray) {
					m_dataIterators.put(viewData.getViewSpec().id(),
							((DLLinePlotViewData<DLJFreeChartLinePlotViewSpec>) viewData).iterators());
				}
				m_areIteratorsInit = true;
			} catch (final NullPointerException npe) {
				throw new IllegalArgumentException(
						"Exception while trying to initialize view data iterators. Most likely an implementation error!",
						npe);
			}
		}
	}

	private void initView() {
		m_views = new HashMap<>();
		m_dataIterators = new HashMap<>();
		m_mainContainer = new JPanel(new GridBagLayout());

		final JTabbedPane plotsWithHistory = new JTabbedPane();
		for (final DLViewSpec spec : m_specs) {
			// assume DLJFreeChartLinePlotViewSpec for now
			final DLJFreeChartLinePlotWithHistoryView tab = new DLJFreeChartLinePlotWithHistoryView(
					(DLJFreeChartLinePlotViewSpec) spec);
			m_views.put(spec.id(), tab);

			plotsWithHistory.addTab(spec.title(), tab.getComponent());
		}

		final GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1;
		gbc.weighty = 1;
		gbc.fill = GridBagConstraints.BOTH;
		m_mainContainer.add(plotsWithHistory, gbc);

		gbc.gridy++;
		gbc.weighty = 0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(10, 10, 0, 0);
		m_epochProgressBar = new DLLearningProgressBar("Epoch", "Avg. duration / epoch");
		m_mainContainer.add(m_epochProgressBar, gbc);

		gbc.gridy++;
		gbc.insets = new Insets(0, 10, 10, 0);
		m_batchProgressBar = new DLLearningProgressBar("Batch", "Avg. duration / batch");
		m_mainContainer.add(m_batchProgressBar, gbc);

		gbc.gridy++;
		gbc.insets = new Insets(0, 15, 10, 0);
		m_startTime = new LeftAlignLabelWithValue("Start time: ");
		m_startTime.setValue(formatStartTime(null));
		m_mainContainer.add(m_startTime, gbc);

		gbc.gridy++;
		m_elapsedTime = new LeftAlignLabelWithValue("Elapsed: ");
		m_elapsedTime.setValue(formatElapsedTime(null));
		m_mainContainer.add(m_elapsedTime, gbc);

		gbc.gridy++;
		m_stopButton = new LeftAlignButton("Stop learning");
		m_stopButton.getButton().addActionListener(e -> {
			getNodeModel().stopLearning();
			setHasStoppedButtonStatus();
		});
		gbc.insets = new Insets(10, 15, 10, 0);
		m_mainContainer.add(m_stopButton, gbc);

		setComponent(m_mainContainer);
	}

	private void updateView(final DLProgressMonitor monitor) {
		if (monitor.isRunning() || monitor.hasData()) {
			showNoDataOverlay(false);
			startAllTimers();
			initDataIterators(monitor.getDataUpdate());

			if (monitor.hasStoppedEarly()) {
				setHasStoppedButtonStatus();
			}

			m_epochProgressBar.setMaxProgress(monitor.getNumBatchesPerEpoch() * monitor.getNumEpochs());
			m_batchProgressBar.setMaxProgress(monitor.getNumBatchesPerEpoch());

			m_epochProgressBar.setProgress(
					monitor.getNumBatchesPerEpoch() * monitor.getCurrentEpoch() + monitor.getCurrentBatchInEpoch());
			m_epochProgressBar.setProgressText(monitor.getCurrentEpoch(), monitor.getNumEpochs());

			m_batchProgressBar.setProgress(monitor.getCurrentBatchInEpoch());
			m_batchProgressBar.setProgressText(monitor.getCurrentBatchInEpoch(), monitor.getNumBatchesPerEpoch());

			final LocalDateTime startTime = monitor.getStartDateTime();
			if (startTime != null) {
				LocalDateTime endTime = monitor.getEndDateTime();
				if (endTime == null) {
					endTime = LocalDateTime.now();
				}
				final Duration elapsedTime = Duration.between(startTime, endTime);

				final int currentEpoch = monitor.getCurrentEpoch();
				if (currentEpoch != m_lastEpoch || currentEpoch == monitor.getNumEpochs()) {
					m_epochProgressBar.setDuration(elapsedTime.dividedBy(currentEpoch + 1));
					m_lastEpoch = currentEpoch;
				}
				final int currentBatch = monitor.getCurrentBatchInEpoch();
				m_batchProgressBar.setDuration(elapsedTime.dividedBy(
						(currentBatch != 0 ? currentBatch : 1) + currentEpoch * monitor.getNumBatchesPerEpoch()));

				m_startTime.setValue(formatStartTime(startTime));
				m_elapsedTime.setValue(formatElapsedTime(elapsedTime));
			}
			for (final DLViewSpec spec : m_specs) {
				final DLJFreeChartLinePlotWithHistoryView view = m_views.get(spec.id());
				view.update((DLJFreeChartLinePlotViewSpec) spec, m_dataIterators.get(spec.id()));
			}
		} else {
			reset();
		}
	}

	@Override
	protected void updateModel(final Object arg) {
		if (arg == null) {
			reset();
			return;
		} else if (arg instanceof DLProgressMonitor) {
			updateView((DLProgressMonitor) arg);
		} else {
			throw new IllegalArgumentException("Can't handle objects of type " + arg.getClass()
					+ " in DLLearnerNodeView. Most likely an implementation error.");
		}
	}

	private void reset() {
		initView();
		stopAllTimers();
		m_areIteratorsInit = false;

		showNoDataOverlay(true);
	}

	private void setHasStoppedButtonStatus() {
		m_stopButton.getButton().setText("Learning stopped");
		m_stopButton.getButton().setEnabled(false);
	}

	@Override
	protected void onClose() {
		stopAllTimers();
	}

	@Override
	protected void onOpen() {
		// NB: Nothing to do
	}

	@Override
	protected void modelChanged() {
		// NB: Nothing to do
	}

	private void stopAllTimers() {
		for (final Entry<String, DLJFreeChartLinePlotWithHistoryView> e : m_views.entrySet()) {
			e.getValue().stopCurrentValueUpdate();
		}
	}

	private void startAllTimers() {
		for (final Entry<String, DLJFreeChartLinePlotWithHistoryView> e : m_views.entrySet()) {
			e.getValue().startCurrentValueUpdate();
		}
	}

	private void showNoDataOverlay(final boolean showIt) {
		if (showIt) {
			setComponent(NO_DATA_OVERLAY);
		} else {
			setComponent(m_mainContainer);
		}
	}

	/**
	 * Simple helper to left align a button in a {@link GridBagLayout}.
	 */
	private class LeftAlignButton extends JPanel {
		private static final long serialVersionUID = 1L;

		private final JButton m_button;

		public LeftAlignButton(final String buttonText) {
			super(new GridBagLayout());

			m_button = new JButton(buttonText);
			m_button.setPreferredSize(new Dimension(180, 50));
			m_button.setSize(new Dimension(180, 50));
			m_button.setMinimumSize(new Dimension(180, 50));

			final GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 0;
			gbc.weightx = 0;
			add(m_button, gbc);

			gbc.gridx++;
			gbc.weightx = 1;
			add(new Box(0), gbc);
		}

		public JButton getButton() {
			return m_button;
		}
	}

	/**
	 * Simple helper to left align a label with a changing value in a {@link GridBagLayout}.
	 */
	public class LeftAlignLabelWithValue extends JPanel {
		private static final long serialVersionUID = 1L;

		private final JLabel m_valueToDisplay = new JLabel();

		public LeftAlignLabelWithValue(final String label) {
			super(new GridBagLayout());

			final GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 0;
			gbc.weightx = 0;

			final JLabel startTimeLabel = new JLabel(label);
			startTimeLabel.setPreferredSize(new Dimension(85, 20));
			add(startTimeLabel, gbc);

			gbc.gridx++;
			add(m_valueToDisplay, gbc);

			// left align
			gbc.gridx++;
			gbc.weightx = 1;
			add(new Box(0), gbc);
		}

		public void setValue(final String value) {
			m_valueToDisplay.setText(value);
		}
	}
}
