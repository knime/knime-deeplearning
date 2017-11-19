package org.knime.dl.keras.base.nodes.learner.view;

public interface DLProgressMonitor {

	DLViewData<?>[] getDataUpdate();

	// TODO potentially later we can extend this...
	boolean isRunning();

	int numBatchesPerEpoch();

	int numEpochs();

	int currentBatchInEpoch();

	int currentEpoch();

}
