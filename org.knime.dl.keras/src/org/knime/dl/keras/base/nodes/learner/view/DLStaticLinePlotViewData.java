package org.knime.dl.keras.base.nodes.learner.view;

public class DLStaticLinePlotViewData<S extends DLLinePlotViewSpec> extends DLAbstractLinePlotViewData<S> {

	public DLStaticLinePlotViewData(S spec, float[][] data) {
		super(spec, data, data[0].length);
	}

}
