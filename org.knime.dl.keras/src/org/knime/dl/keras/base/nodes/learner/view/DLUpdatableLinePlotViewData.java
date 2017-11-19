package org.knime.dl.keras.base.nodes.learner.view;

public class DLUpdatableLinePlotViewData<S extends DLLinePlotViewSpec> extends DLAbstractLinePlotViewData<S> {

	public DLUpdatableLinePlotViewData(final S spec, int numBatches) {
		super(spec, new float[spec.numPlots()][numBatches], 0);
	}

	/**
	 * Adds a data element to the data.
	 * 
	 * @param data
	 *            float array of length numLines with single update per line.
	 */
	public void add(float... data) {
		if (data.length != m_spec.numPlots() || currLength() == m_data[0].length) {
			throw new IllegalArgumentException("Error while adding data to line-plot. Implementation error.");
		}
		for (int i = 0; i < data.length; i++) {
			m_data[i][currLength()] = data[i];
		}

		incLength();
	}

}
