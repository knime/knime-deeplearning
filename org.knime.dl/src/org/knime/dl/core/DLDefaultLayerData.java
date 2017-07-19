package org.knime.dl.core;

import org.knime.dl.core.data.DLBuffer;

public class DLDefaultLayerData<B extends DLBuffer> extends DLAbstractLayerData<B> {

    public DLDefaultLayerData(final DLLayerDataSpec spec, final B buffer) {
        super(spec, buffer);
    }
}
