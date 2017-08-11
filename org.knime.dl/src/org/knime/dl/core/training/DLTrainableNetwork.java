package org.knime.dl.core.training;

import java.util.Iterator;
import java.util.Set;

import org.knime.core.util.Pair;
import org.knime.dl.core.DLNetwork;
import org.knime.dl.core.execution.DLLayerDataBatch;

public interface DLTrainableNetwork extends DLNetwork {

	// TODO make more beautiful signature
	// why return network?
	DLNetwork train(Iterator<Pair<Set<DLLayerDataBatch<?>>, Set<DLLayerDataBatch<?>>>> trainingData);
}
