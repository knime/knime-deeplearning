package org.knime.dl.core;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Arrays;

import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Abstract base class for network spec implementations.
 *
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
public abstract class DLAbstractNetworkSpec implements DLNetworkSpec {

    private final DLLayerDataSpec[] m_inputSpecs;

    private final DLLayerDataSpec[] m_intermediateOutputSpecs;

    private final DLLayerDataSpec[] m_outputSpecs;

    private final int m_hashCode;

    /**
     * Creates a new instance of this network spec.
     *
     * @param inputSpecs the input layer data specs, can be empty
     * @param intermediateOutputSpecs the intermediate output layer data specs, can be empty
     * @param outputSpecs the output layer data specs, can be empty
     */
    protected DLAbstractNetworkSpec(final DLLayerDataSpec[] inputSpecs, final DLLayerDataSpec[] intermediateOutputSpecs,
        final DLLayerDataSpec[] outputSpecs) {
        m_inputSpecs = checkNotNull(inputSpecs);
        m_intermediateOutputSpecs = checkNotNull(intermediateOutputSpecs);
        m_outputSpecs = checkNotNull(outputSpecs);
        m_hashCode = hashCodeInternal();
    }

    protected abstract void hashCodeInternal(HashCodeBuilder b);

    protected abstract boolean equalsInternal(DLNetworkSpec other);

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

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return m_hashCode;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != getClass()) {
            return false;
        }
        final DLNetworkSpec other = (DLNetworkSpec)obj;
        return other.getInputSpecs().length == getInputSpecs().length //
            && other.getIntermediateOutputSpecs().length == getIntermediateOutputSpecs().length //
            && other.getOutputSpecs().length == getOutputSpecs().length //
            && Arrays.deepEquals(other.getInputSpecs(), getInputSpecs()) //
            && Arrays.deepEquals(other.getIntermediateOutputSpecs(), getIntermediateOutputSpecs()) //
            && Arrays.deepEquals(other.getOutputSpecs(), getOutputSpecs()) //
            && equalsInternal(other);
    }

    private int hashCodeInternal() {
        final HashCodeBuilder b = new HashCodeBuilder();
        b.append(m_inputSpecs);
        b.append(m_intermediateOutputSpecs);
        b.append(m_outputSpecs);
        hashCodeInternal(b);
        return b.toHashCode();
    }
}
