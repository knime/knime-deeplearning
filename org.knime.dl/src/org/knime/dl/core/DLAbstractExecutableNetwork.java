package org.knime.dl.core;

import org.knime.dl.core.backend.DLBackend;

public abstract class DLAbstractExecutableNetwork implements DLExecutableNetwork {

	private final DLBackend m_backend;

	private final DLExecutableNetworkSpec m_spec;

	public DLAbstractExecutableNetwork(final DLBackend backend, final DLExecutableNetworkSpec spec) {
		m_backend = backend;
		m_spec = spec;
	}

	@Override
	public DLBackend getBackend() {
		return m_backend;
	}

	@Override
	public DLExecutableNetworkSpec getSpec() {
		return m_spec;
	}
}
