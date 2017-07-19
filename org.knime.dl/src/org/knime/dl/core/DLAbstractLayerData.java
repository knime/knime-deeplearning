package org.knime.dl.core;

import org.knime.dl.core.data.DLBuffer;

public abstract class DLAbstractLayerData<B extends DLBuffer> implements DLLayerData<B> {

    private final DLLayerDataSpec m_spec;

    private final B m_buffer;

    protected DLAbstractLayerData(final DLLayerDataSpec spec, final B buffer) {
        m_spec = spec;
        m_buffer = buffer;
    }

    @Override
    public DLLayerDataSpec getSpec() {
        return m_spec;
    }

    @Override
    public B getBuffer() {
        return m_buffer;
    }

    @Override
    public void close() throws Exception {
        m_buffer.close();
    }
}
