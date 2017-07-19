package org.knime.dl.core;

import java.util.Arrays;

public abstract class DLAbstractNetworkSpec implements DLNetworkSpec {

	private final DLLayerDataSpec[] m_inputSpecs;
	private final DLLayerDataSpec[] m_intermediateOutputSpecs;
	private final DLLayerDataSpec[] m_outputSpecs;

	protected DLAbstractNetworkSpec(final DLLayerDataSpec[] inputSpecs, final DLLayerDataSpec[] intermediateOutputSpecs,
			final DLLayerDataSpec[] outputSpecs) {
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
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj.getClass().equals(getClass()))) {
			return false;
		} else {
			final DLNetworkSpec curr = (DLNetworkSpec) obj;
			if (curr.getInputSpecs().length != getInputSpecs().length
					|| curr.getIntermediateOutputSpecs().length != getIntermediateOutputSpecs().length
					|| curr.getOutputSpecs().length != getOutputSpecs().length) {
				return false;
			} else if (!Arrays.deepEquals(getInputSpecs(), curr.getInputSpecs())
					|| !Arrays.deepEquals(getIntermediateOutputSpecs(), curr.getIntermediateOutputSpecs())
					|| !Arrays.deepEquals(getOutputSpecs(), curr.getOutputSpecs())) {
				return false;
			}
		}
		return true;
	}
}
