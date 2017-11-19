package org.knime.dl.keras.base.nodes.learner.view;

import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.event.ChangeListener;

import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeView;
import org.knime.dl.keras.base.nodes.learner.view.jfreechart.DLJFreeChartLinePlotView;
import org.knime.dl.keras.base.nodes.learner.view.jfreechart.DLJFreeChartLinePlotViewSpec;

// TODO actually this should just delegate to some object which can be anything (even a JavaScript thingy). We don't want to enforce an implementation here.
public class DLNodeView<M extends NodeModel & DLInteractiveLearnerNodeModel> extends NodeView<M> {

	private Map<String, DLView<?>> m_views;
	private JProgressBar m_progressBar;
	private DLViewSpec[] m_specs;

	public DLNodeView(final M model, DLViewSpec... specs) {
		super(model);
		setShowNODATALabel(false);
		m_specs = specs;

		updateView(model.getProgressMonitor());
	}

	@SuppressWarnings("unchecked")
	private void updateView(final DLProgressMonitor monitor) {
		if (monitor.isRunning()) {
			if (m_views == null) {
				m_views = new HashMap<>();
				final JPanel container = new JPanel();
				for (final DLViewSpec spec : m_specs) {
					final DLView<?> view = createView(spec);
					m_views.put(spec.id(), view);
					container.add(view.getComponent());
				}

				container.add(m_progressBar = new JProgressBar(0, monitor.numBatchesPerEpoch() * monitor.numEpochs()));
				container.add(new JButton("Stop Learning") {
					/**
					 * 
					 */
					private static final long serialVersionUID = 1L;

					@Override
					public void addChangeListener(ChangeListener l) {
						getNodeModel().stopLearning();
					}
				});
				setComponent(container);
			}
			m_progressBar
					.setValue(monitor.numBatchesPerEpoch() * monitor.currentEpoch() + monitor.currentBatchInEpoch());

			for (DLViewData<?> data : monitor.getDataUpdate()) {
				@SuppressWarnings("rawtypes")
				final DLView view = m_views.get(data.getViewSpec().id());
				view.update(data);
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

	/* -- Internal helper -- */
	private DLView<?> createView(DLViewSpec spec) {
		// TODO we can add extension points for views later. for now we assume a
		// simple single view.
		if (spec instanceof DLJFreeChartLinePlotViewSpec) {
			return new DLJFreeChartLinePlotView((DLJFreeChartLinePlotViewSpec) spec);
		} else {
			throw new IllegalArgumentException("At the moment only DLLinePlotViewSpecs are supported!");
		}
	}

}
