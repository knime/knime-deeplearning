package org.knime.dl.keras.base.nodes.learner.view;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeView;
import org.knime.dl.keras.base.nodes.learner.view.jfreechart.DLJFreeChartLinePlotTab;
import org.knime.dl.keras.base.nodes.learner.view.jfreechart.DLJFreeChartLinePlotViewSpec;

// TODO actually this should just delegate to some object which can be anything (even a JavaScript thingy). We don't want to enforce an implementation here.
public class DLNodeView<M extends NodeModel & DLInteractiveLearnerNodeModel> extends NodeView<M> {
	
	private static String TIME_DISPLAY_FORMAT = "%d:%d:%d";

	private Map<String, DLJFreeChartLinePlotTab> m_views;
	private DLLearningProgressBar m_epochProgressBar;
	private DLLearningProgressBar m_batchProgressBar;
	private LabelWithValue m_startTime;
	private LabelWithValue m_elapsedTime;
	private DLViewSpec[] m_specs;

	public DLNodeView(final M model, DLViewSpec... specs) {
		super(model);
		setShowNODATALabel(false);
		m_specs = specs;

		initView();
		DLProgressMonitor monitor = model.getProgressMonitor();
		updateView(monitor);
	}

	private void initView() {
		JPanel wrapper = new JPanel(new GridBagLayout());

		JTabbedPane plotsWithHistory = new JTabbedPane();
		m_views = new HashMap<>();

		for (final DLViewSpec spec : m_specs) {
			// assume DLJFreeChartLinePlotViewSpec for now
			DLJFreeChartLinePlotTab tab = new DLJFreeChartLinePlotTab((DLJFreeChartLinePlotViewSpec) spec);
			
			m_views.put(spec.id(), tab);
			plotsWithHistory.addTab(spec.title(), tab);
		}
		

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbc.weightx = 1;
		gbc.weighty = 1;
		gbc.fill = GridBagConstraints.BOTH;
		wrapper.add(plotsWithHistory, gbc);

		gbc.gridy++;
		gbc.weighty = 0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.insets = new Insets(10, 10, 0, 0);
		m_epochProgressBar = new DLLearningProgressBar("Epoch", "Seconds/Epoch");
		wrapper.add(m_epochProgressBar, gbc);

		gbc.gridy++;
		gbc.insets = new Insets(0, 10, 10, 0);
		m_batchProgressBar = new DLLearningProgressBar("Batch", "Seconds/Batch");
		wrapper.add(m_batchProgressBar, gbc);
		
		gbc.gridy++;
		gbc.insets = new Insets(0, 50, 10, 0);
		m_startTime = new LabelWithValue("Start Time:");
		m_startTime.setValue(String.format(TIME_DISPLAY_FORMAT, 0, 0, 0));
		wrapper.add(m_startTime, gbc);

		gbc.gridy++;
		m_elapsedTime = new LabelWithValue("Elapsed:");
		m_elapsedTime.setValue(String.format(TIME_DISPLAY_FORMAT, 0, 0, 0));
		wrapper.add(m_elapsedTime, gbc);
		
		gbc.gridy++;
		LeftAlignButton stopButton = new LeftAlignButton("Stop Learning");
		stopButton.addActionListener((e) -> getNodeModel().stopLearning());
		gbc.insets = new Insets(10, 10, 10, 0);
		wrapper.add(stopButton, gbc);

		setComponent(wrapper);
	}

	@SuppressWarnings("unchecked")
	private void updateView(final DLProgressMonitor monitor) {
		if (monitor.isRunning()) {
			startAllTimers();
			
			m_epochProgressBar.setMaxProgress(monitor.getNumBatchesPerEpoch() * monitor.getNumEpochs());
			m_batchProgressBar.setMaxProgress(monitor.getNumBatchesPerEpoch());
			
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
		stopAllTimers();
		initView();
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
	
	private void stopAllTimers(){
		for(Entry<String, DLJFreeChartLinePlotTab> e : m_views.entrySet()){
			e.getValue().stopCurrentValueUpdate();
		}
	}
	
	private void startAllTimers(){
		for(Entry<String, DLJFreeChartLinePlotTab> e : m_views.entrySet()){
			e.getValue().startCurrentValueUpdate();
		}
	}
	
	private class LeftAlignButton extends JPanel {
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
		
		public void addActionListener(ActionListener al){
			m_button.addActionListener(al);
		}
	}
	
	public class LabelWithValue extends JPanel {

		private JLabel m_valueToDisplay = new JLabel();
		
		public LabelWithValue(final String label) {
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
		
		public void setValue(String value){
			m_valueToDisplay.setText(value);
		}

	}

}
