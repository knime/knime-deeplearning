package org.knime.dl.keras.base.nodes.learner.view;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeView;
import org.knime.dl.keras.base.nodes.learner.view.jfreechart.DLJFreeChartLinePlotTab;
import org.knime.dl.keras.base.nodes.learner.view.jfreechart.DLJFreeChartLinePlotViewSpec;

// TODO actually this should just delegate to some object which can be anything (even a JavaScript thingy). We don't want to enforce an implementation here.
public class DLNodeView<M extends NodeModel & DLInteractiveLearnerNodeModel> extends NodeView<M> {

	private Map<String, DLJFreeChartLinePlotTab> m_views;
	private DLLearningProgressBar m_epochProgressBar;
	private DLLearningProgressBar m_batchProgressBar;
	private DLViewSpec[] m_specs;

	public DLNodeView(final M model, DLViewSpec... specs) {
		super(model);
		setShowNODATALabel(false);
		m_specs = specs;

		updateView(model.getProgressMonitor());
	}

	private void initView(final int maxProgressEpochs, final int maxProgressBatch) {
		JPanel wrapper = new JPanel(new GridBagLayout());

		JTabbedPane tabbedPane = new JTabbedPane();
		m_views = new HashMap<>();

		for (final DLViewSpec spec : m_specs) {
			// assume DLJFreeChartLinePlotViewSpec for now
			DLJFreeChartLinePlotTab tab = new DLJFreeChartLinePlotTab((DLJFreeChartLinePlotViewSpec) spec);

			m_views.put(spec.id(), tab);
			tabbedPane.addTab(spec.title(), tab);
		}

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1;
		gbc.weighty = 0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		wrapper.add(tabbedPane, gbc);

		gbc.gridy++;
		gbc.weighty = 1;
		m_epochProgressBar = new DLLearningProgressBar(maxProgressEpochs, "Epoch", "Seconds/Epoch");
		wrapper.add(m_epochProgressBar, gbc);

		gbc.gridy++;
		m_batchProgressBar = new DLLearningProgressBar(maxProgressBatch, "Batch", "Seconds/Batch");
		wrapper.add(m_batchProgressBar, gbc);

		JButton stopButton = new JButton("Stop Learning");
		stopButton.addActionListener((e) -> getNodeModel().stopLearning());
		gbc.gridy++;
		wrapper.add(stopButton, gbc);

		setComponent(wrapper);
	}

	@SuppressWarnings("unchecked")
	private void updateView(final DLProgressMonitor monitor) {
		if (monitor.isRunning()) {
			if (m_views == null) {
				initView(monitor.getNumBatchesPerEpoch() * monitor.getNumEpochs(), monitor.getNumBatchesPerEpoch());
			}

			m_epochProgressBar.setProgress(
					monitor.getNumBatchesPerEpoch() * monitor.getCurrentEpoch() + monitor.getCurrentBatchInEpoch());
			m_epochProgressBar.setProgressText(monitor.getCurrentEpoch(), monitor.getNumEpochs());
			// clac time per epoch and set m_epochProgressBar.setTime(timeInSec)

			m_batchProgressBar.setProgress(monitor.getCurrentBatchInEpoch());
			m_batchProgressBar.setProgressText(monitor.getCurrentBatchInEpoch(), monitor.getNumBatchesPerEpoch());
			// clac time per epoch and set m_batchProgressBar.setTime(timeInSec)

			for (DLViewData<?> data : monitor.getDataUpdate()) {
				final DLJFreeChartLinePlotTab view = m_views.get(data.getViewSpec().id());
				view.update((DLLinePlotViewData<DLJFreeChartLinePlotViewSpec>) data);
			}

			getComponent().repaint();
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

		getComponent().repaint();
	}

	private void reset() {
		m_views = null;
		setComponent(new JLabel("Nothing to display."));
		getComponent().repaint();
	}

	@Override
	protected void onClose() {
		// NB: Nothing to do here
	}

	@Override
	protected void onOpen() {
		// NB: Nothing to do
	}

	@Override
	protected void modelChanged() {
		// NB: Nothing to do
	}

}
