/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 */
package org.knime.dl.core;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.knime.core.util.Version;
import org.knime.dl.core.training.DLTrainingConfig;

/**
 * Abstract base class for network spec implementations.
 * <P>
 * Classes that extend this class must take care of serialization of this class's state as well, e.g., using the
 * {@link DLAbstractNetworkSpec2.SerializationProxy serialization proxy} pattern. This class will throw an exception
 * when tried to be serialized using default serialization
 * 
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 * 
 * @param <CFG> The training configuration of this network type.
 */
@SuppressWarnings("serial") // Not intended for default serialization. Must be handled by extending classes.
public abstract class DLAbstractNetworkSpec2<CFG extends DLTrainingConfig> implements DLNetworkSpec {

    private static final int ZERO_HASHCODE = 0;

    private final Version m_bundleVersion;

    private final DLTensorSpec[] m_inputSpecs;

    private final DLTensorSpec[] m_hiddenOutputSpecs;

    private final DLTensorSpec[] m_outputSpecs;

    private final Optional<CFG> m_trainingConfig;

    private int m_hashCode = ZERO_HASHCODE;

    /**
     * Creates a new instance of this network spec.
     *
     * @param bundleVersion the version of the containing bundle
     * @param inputSpecs the input tensor specs, can be empty
     * @param hiddenOutputSpecs the hidden output tensor specs, can be empty
     * @param outputSpecs the output tensor specs, can be empty
     */
    protected DLAbstractNetworkSpec2(final Version bundleVersion, final DLTensorSpec[] inputSpecs,
        final DLTensorSpec[] hiddenOutputSpecs, final DLTensorSpec[] outputSpecs) {
        m_bundleVersion = checkNotNull(bundleVersion);
        m_inputSpecs = checkNotNull(inputSpecs, "Input data specs must not be null, but may be empty.").clone();
        m_hiddenOutputSpecs =
            checkNotNull(hiddenOutputSpecs, "Hidden output data specs must not be null, but may be empty.").clone();
        m_outputSpecs = checkNotNull(outputSpecs, "Output data specs must not be null, but may be empty.").clone();
        m_trainingConfig = Optional.empty();
    }

    /**
     * Creates a new instance of this network spec.
     *
     * @param bundleVersion the version of the containing bundle
     * @param inputSpecs the input tensor specs, can be empty
     * @param hiddenOutputSpecs the hidden output tensor specs, can be empty
     * @param outputSpecs the output tensor specs, can be empty
     * @param trainingConfig the {@link DLTrainingConfig training configuration}, must be {@link Serializable}
     */
    protected DLAbstractNetworkSpec2(final Version bundleVersion, final DLTensorSpec[] inputSpecs,
        final DLTensorSpec[] hiddenOutputSpecs, final DLTensorSpec[] outputSpecs, final CFG trainingConfig) {
        m_bundleVersion = checkNotNull(bundleVersion);
        m_inputSpecs = checkNotNull(inputSpecs, "Input data specs must not be null, but may be empty.").clone();
        m_hiddenOutputSpecs =
            checkNotNull(hiddenOutputSpecs, "Hidden output data specs must not be null, but may be empty.").clone();
        m_outputSpecs = checkNotNull(outputSpecs, "Output data specs must not be null, but may be empty.").clone();
        m_trainingConfig = Optional.of(checkNotNull(trainingConfig, "Training configuration must not be null."));
    }

    /**
     * Allows deriving classes to add own fields to the hash code computation.
     *
     * @param b The builder to which fields of the extending class can be added for hash code computation.
     */
    protected abstract void hashCodeInternal(HashCodeBuilder b);

    /**
     * Allows deriving classes to add own criteria to the equality check.
     *
     * @param other The network spec whose equality to this network spec is checked.
     * @return {@code true} if the given network spec equals this network spec with respect to the evaluation criteria
     *         added by the extending class.
     */
    protected abstract boolean equalsInternal(DLNetworkSpec other);

    @Override
    public Version getBundleVersion() {
        return m_bundleVersion;
    }

    @Override
    public DLTensorSpec[] getInputSpecs() {
        return m_inputSpecs.clone();
    }

    @Override
    public DLTensorSpec[] getHiddenOutputSpecs() {
        return m_hiddenOutputSpecs.clone();
    }

    @Override
    public DLTensorSpec[] getOutputSpecs() {
        return m_outputSpecs.clone();
    }

    @Override
    public Optional<CFG> getTrainingConfig() {
        return m_trainingConfig;
    }

    @Override
    public int hashCode() {
        if (m_hashCode == ZERO_HASHCODE) {
            m_hashCode = hashCodeInternal();
        }
        return m_hashCode;
    }

    private int hashCodeInternal() {
        final HashCodeBuilder b = new HashCodeBuilder(17, 37);
        // NOTE: Do not add bundle version.
        b.append(m_inputSpecs);
        b.append(m_hiddenOutputSpecs);
        b.append(m_outputSpecs);
        // TODO: Add as soon as training configuration offers value-based hash code computation.
        // b.append(m_trainingConfig);
        hashCodeInternal(b);
        return b.toHashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != getClass()) {
            return false;
        }
        final DLAbstractNetworkSpec2<?> other = (DLAbstractNetworkSpec2<?>)obj;
        return
        // NOTE: Do not add bundle version.
        other.m_inputSpecs.length == m_inputSpecs.length //
            && other.m_hiddenOutputSpecs.length == m_hiddenOutputSpecs.length //
            && other.m_outputSpecs.length == m_outputSpecs.length //
            && Arrays.deepEquals(other.m_inputSpecs, m_inputSpecs) //
            && Arrays.deepEquals(other.m_hiddenOutputSpecs, m_hiddenOutputSpecs) //
            && Arrays.deepEquals(other.m_outputSpecs, m_outputSpecs) //
            // TODO: Add as soon as training configuration offers value-based equality check.
            // && other.m_trainingConfig.equals(m_trainingConfig) //
            && equalsInternal(other);
    }

    @Override
    public String toString() {
        return "Version: " + Objects.toString(m_bundleVersion) + "\n" + //
            "Inputs: " + Arrays.toString(m_inputSpecs) + "\n" + //
            "Hidden outputs: " + Arrays.toString(m_hiddenOutputSpecs) + "\n" + //
            "Outputs: " + Arrays.toString(m_outputSpecs) + "\n" + //
            "Training config: " + //
            (m_trainingConfig.isPresent() ? "\n" + m_trainingConfig.get().toString() : "none");
    }

    @SuppressWarnings({"static-method", "unused"})
    private void writeObject(final ObjectOutputStream stream) throws IOException {
        throw new IllegalStateException("Implementing classes must implement the serialization.");
    }

    @SuppressWarnings({"static-method", "unused"})
    private void readObject(final ObjectInputStream stream) throws IOException, ClassNotFoundException {
        throw new IllegalStateException("Implementing classes must implement the serialization.");
    }

    /** Serialization proxy for {@link DLAbstractNetworkSpec2}. */
    @SuppressWarnings("javadoc")
    protected static final class SerializationProxy implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * We serialize the string representation here as {@link Version} is not intended for serialization to
         * persistent storage, only for network transfer.
         */
        public String m_bundleVersionString;

        public DLTensorSpec[] m_inputSpec;

        public DLTensorSpec[] m_hiddenOutputSpec;

        public DLTensorSpec[] m_outputSpec;

        public SerializationProxy(final DLAbstractNetworkSpec2<?> obj) {
            m_bundleVersionString = obj.m_bundleVersion.toString();
            m_inputSpec = obj.m_inputSpecs;
            m_hiddenOutputSpec = obj.m_hiddenOutputSpecs;
            m_outputSpec = obj.m_outputSpecs;
            // TODO: Add training config as soon as it is serializable.
        }
    }
}
