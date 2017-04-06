package org.knime.dl.core;

import org.knime.dl.core.data.DLBuffer;

public abstract class DLAbstractLayerData<S extends DLBuffer> implements DLLayerData<S> {

	private final DLLayerDataSpec m_spec;

	private final S m_data;

	protected DLAbstractLayerData(final DLLayerDataSpec spec, final S data) {
		m_spec = spec;
		m_data = data;
	}

	@Override
	public DLLayerDataSpec getSpec() {
		return m_spec;
	}

	@Override
	public S getData() {
		return m_data;
	}

	@Override
	public void close() throws Exception {
		m_data.close();
	}
}
