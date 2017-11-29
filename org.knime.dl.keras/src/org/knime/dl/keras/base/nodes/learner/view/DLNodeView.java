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
import java.awt.event.ActionListener;
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
//TODO actually this should just delegate to some object which can be anything (even a JavaScript thingy). We don't want to enforce an implementation here.
public class DLNodeView<M extends NodeModel & DLInteractiveLearnerNodeModel> extends NodeView<M> {

	private final static String TIME_DISPLAY_FORMAT = "%d:%d:%d";

	/*
	 * Alternative to setShowNODATALabel() because the NODATA label of the 
	 * NodeView is also displayed during execution, which is exactly what
	 * we d not want.
	 */
	private final static JLabel NO_DATA_OVERLAY;

	private Map<String, DLJFreeChartLinePlotWithHistoryView> m_views;
	
	/*
	 * Data iterators for this view. Its important that each view has its own iterator state
	 * if we open several views at once.
	 */
	private Map<String, Iterator<DLFloatData>[]> m_dataIterators;

	private DLViewSpec[] m_specs;

	private DLLearningProgressBar m_epochProgressBar;
	private DLLearningProgressBar m_batchProgressBar;
	private LeftAlignLabelWithValue m_startTime;
	private LeftAlignLabelWithValue m_elapsedTime;
	private JPanel m_mainContainer;

	private final DLProgressMonitor m_progressMonitor;

	/*
	 * Flag indicating if the data iterators have been initialized.
	 */
	private boolean m_areIteratorsInit;
	
	static {
		NO_DATA_OVERLAY = new JLabel("<html><center>No data to display</center></html>",
				SwingConstants.CENTER);
		NO_DATA_OVERLAY.setPreferredSize(new Dimension(1000, 700));
	}

	public DLNodeView(final M model, DLViewSpec... specs) {
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
	 * Set data iterators of this view using the the specified view data array.
	 * If the iterators are already initialized this method will do nothing.
	 * 
	 * @param dataArray data to get iterators from
	 * @throws IllegalArgumentException If the dataArray is null or contains null.
	 */
	@SuppressWarnings("unchecked")
	private void initDataIterators(DLViewData<?>[] dataArray) {
		if (!m_areIteratorsInit) {
			try {
				for (DLViewData<?> viewData : dataArray) {
					m_dataIterators.put(viewData.getViewSpec().id(),
							((DLLinePlotViewData<DLJFreeChartLinePlotViewSpec>) viewData).iterators());
				}
				m_areIteratorsInit = true;
			} catch (NullPointerException npe) {
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

		JTabbedPane plotsWithHistory = new JTabbedPane();
		for (DLViewSpec spec : m_specs) {
			// assume DLJFreeChartLinePlotViewSpec for now
			DLJFreeChartLinePlotWithHistoryView tab = new DLJFreeChartLinePlotWithHistoryView(
					(DLJFreeChartLinePlotViewSpec) spec);
			m_views.put(spec.id(), tab);

			plotsWithHistory.addTab(spec.title(), tab.getComponent());
		}

		GridBagConstraints gbc = new GridBagConstraints();
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
		m_epochProgressBar = new DLLearningProgressBar("Epoch", "Seconds/Epoch");
		m_mainContainer.add(m_epochProgressBar, gbc);

		gbc.gridy++;
		gbc.insets = new Insets(0, 10, 10, 0);
		m_batchProgressBar = new DLLearningProgressBar("Batch", "Seconds/Batch");
		m_mainContainer.add(m_batchProgressBar, gbc);

		gbc.gridy++;
		gbc.insets = new Insets(0, 50, 10, 0);
		m_startTime = new LeftAlignLabelWithValue("Start Time:");
		m_startTime.setValue(String.format(TIME_DISPLAY_FORMAT, 0, 0, 0));
		m_mainContainer.add(m_startTime, gbc);

		gbc.gridy++;
		m_elapsedTime = new LeftAlignLabelWithValue("Elapsed:");
		m_elapsedTime.setValue(String.format(TIME_DISPLAY_FORMAT, 0, 0, 0));
		m_mainContainer.add(m_elapsedTime, gbc);

		gbc.gridy++;
		LeftAlignButton stopButton = new LeftAlignButton("Stop Learning");
		stopButton.addActionListener((e) -> getNodeModel().stopLearning());
		gbc.insets = new Insets(10, 10, 10, 0);
		m_mainContainer.add(stopButton, gbc);

		setComponent(m_mainContainer);
	}

	private void updateView(final DLProgressMonitor monitor) {
		if (monitor.isRunning() || monitor.hasData()) {
			showNoDataOverlay(false);
			startAllTimers();
			initDataIterators(monitor.getDataUpdate());

			m_epochProgressBar.setMaxProgress(monitor.getNumBatchesPerEpoch() * monitor.getNumEpochs());
			m_batchProgressBar.setMaxProgress(monitor.getNumBatchesPerEpoch());

			m_epochProgressBar.setProgress(
					monitor.getNumBatchesPerEpoch() * monitor.getCurrentEpoch() + monitor.getCurrentBatchInEpoch());
			m_epochProgressBar.setProgressText(monitor.getCurrentEpoch(), monitor.getNumEpochs());
			// TODO calc time per epoch and set
			// m_epochProgressBar.setTime(timeInSec)

			m_batchProgressBar.setProgress(monitor.getCurrentBatchInEpoch());
			m_batchProgressBar.setProgressText(monitor.getCurrentBatchInEpoch(), monitor.getNumBatchesPerEpoch());
			// TODO calc time per epoch and set
			// m_batchProgressBar.setTime(timeInSec)

			// TODO set m_startTime.setValue(value) and
			// m_elapsedTime.setValue(value)

			for (DLViewSpec spec : m_specs) {
				final DLJFreeChartLinePlotWithHistoryView view = m_views.get(spec.id());
				view.update((DLJFreeChartLinePlotViewSpec) spec, m_dataIterators.get(spec.id()));
			}
		} else {
			reset();
		}
	}

	@Override
	protected void updateModel(Object arg) {
		if (arg == null) {
			reset();
			return;
		} else if (arg instanceof DLProgressMonitor) {
			updateView((DLProgressMonitor) arg);
		} else {
			throw new IllegalArgumentException("Can't handle objects of type " + arg.getClass()
					+ " in DLLearnerNodeView. Most likely an implementation error!");
		}
	}

	private void reset() {
		initView();
		stopAllTimers();
		m_areIteratorsInit = false;
		
		showNoDataOverlay(true);
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
		for (Entry<String, DLJFreeChartLinePlotWithHistoryView> e : m_views.entrySet()) {
			e.getValue().stopCurrentValueUpdate();
		}
	}

	private void startAllTimers() {
		for (Entry<String, DLJFreeChartLinePlotWithHistoryView> e : m_views.entrySet()) {
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

		private JButton m_button;

		public LeftAlignButton(final String buttonText) {
			super(new GridBagLayout());

			m_button = new JButton(buttonText);
			m_button.setPreferredSize(new Dimension(150, 50));

			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 0;
			gbc.weightx = 0;
			add(m_button, gbc);

			gbc.gridx++;
			gbc.weightx = 1;
			add(new Box(0), gbc);
		}

		public void addActionListener(ActionListener al) {
			m_button.addActionListener(al);
		}
	}

	/**
	 * Simple helper to left align a label with a changing value in a {@link GridBagLayout}.
	 */
	public class LeftAlignLabelWithValue extends JPanel {
		private static final long serialVersionUID = 1L;

		private JLabel m_valueToDisplay = new JLabel();

		public LeftAlignLabelWithValue(final String label) {
			super(new GridBagLayout());

			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 0;
			gbc.weightx = 0;

			JLabel startTimeLabel = new JLabel(label);
			startTimeLabel.setPreferredSize(new Dimension(80, 20));
			add(startTimeLabel, gbc);

			gbc.gridx++;
			add(m_valueToDisplay, gbc);

			// left align
			gbc.gridx++;
			gbc.weightx = 1;
			add(new Box(0), gbc);
		}

		public void setValue(String value) {
			m_valueToDisplay.setText(value);
		}
	}
}
