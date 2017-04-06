package org.knime.dl.core;

public abstract class DLAbstractExecutableNetworkSpec implements DLExecutableNetworkSpec {

	private final DLLayerDataSpec[] m_inputSpecs;
	private final DLLayerDataSpec[] m_intermediateOutputSpecs;
	private final DLLayerDataSpec[] m_outputSpecs;

	protected DLAbstractExecutableNetworkSpec(final DLLayerDataSpec[] inputSpecs,
			final DLLayerDataSpec[] intermediateOutputSpecs, final DLLayerDataSpec[] outputSpecs) {
		m_inputSpecs = inputSpecs;
		m_intermediateOutputSpecs = intermediateOutputSpecs;
		m_outputSpecs = outputSpecs;
	}

	@Override
	public DLLayerDataSpec[] getInputSpecs() {
		return m_inputSpecs;
	}

	@Override
	public DLLayerDataSpec[] getIntermediateOutputSpecs() {
		return m_intermediateOutputSpecs;
	}

	@Override
	public DLLayerDataSpec[] getOutputSpecs() {
		return m_outputSpecs;
	}
}
