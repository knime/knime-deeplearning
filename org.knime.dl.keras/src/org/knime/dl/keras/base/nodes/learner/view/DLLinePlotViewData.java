package org.knime.dl.keras.base.nodes.learner.view;

import java.util.Iterator;

public interface DLLinePlotViewData<S extends DLLinePlotViewSpec> extends DLViewData<S> {

	/**
	 * Creates and returns new iterators over complete data.
	 * 
	 * @return the iterators over {@link DLFloatData}
	 */
	Iterator<DLFloatData>[] iterators();

	/**
	 * Get all valid data as array
	 * 
	 * @return all data as float[].
	 */
	float[][] asArray();
}
