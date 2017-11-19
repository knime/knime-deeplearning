package org.knime.dl.keras.base.nodes.learner.view;

import java.util.Iterator;

public interface DLLinePlotViewData<S extends DLLinePlotViewSpec> extends DLViewData<S> {

	/**
	 * Get iterator over complete data. Iterator is stateful for one execution
	 * of node.
	 * 
	 * @param lineIdx
	 *            of iterator
	 * @return the iterator over {@link DLFloatData}
	 */
	Iterator<DLFloatData> getData(int lineIdx);

	/**
	 * Get all valid data as array
	 * 
	 * @return all data as float[].
	 */
	float[][] asArray();
}
