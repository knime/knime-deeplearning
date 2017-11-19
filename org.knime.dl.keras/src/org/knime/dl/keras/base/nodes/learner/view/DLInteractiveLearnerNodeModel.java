package org.knime.dl.keras.base.nodes.learner.view;

public interface DLInteractiveLearnerNodeModel {
	void stopLearning();

	DLProgressMonitor getProgressMonitor();
}
