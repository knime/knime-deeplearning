package org.knime.dl.core;

import org.knime.dl.core.data.DLBuffer;

public class DLDefaultLayerData<S extends DLBuffer> extends DLAbstractLayerData<S> {

	public DLDefaultLayerData(final DLLayerDataSpec spec, final S data) {
		super(spec, data);
	}
}
