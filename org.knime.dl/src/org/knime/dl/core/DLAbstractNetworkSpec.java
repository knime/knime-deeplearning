/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.Optional;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.knime.dl.core.training.DLTrainingConfig;

/**
 * Abstract base class for network spec implementations.
 *
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
public abstract class DLAbstractNetworkSpec implements DLNetworkSpec {

	private static final long serialVersionUID = 1L;

	private final DLTensorSpec[] m_inputSpecs;

	private final DLTensorSpec[] m_hiddenOutputSpecs;

	private final DLTensorSpec[] m_outputSpecs;

	private transient Optional<CFG> m_trainingConfig;

	private int m_hashCode = 0;

	/**
	 * Creates a new instance of this network spec.
	 *
	 * @param inputSpecs the input tensor specs, can be empty
	 * @param hiddenOutputSpecs the hidden output tensor specs, can be empty
	 * @param outputSpecs the output tensor specs, can be empty
	 */
	protected DLAbstractNetworkSpec(final DLTensorSpec[] inputSpecs, final DLTensorSpec[] hiddenOutputSpecs,
			final DLTensorSpec[] outputSpecs) {
		m_inputSpecs = checkNotNull(inputSpecs, "Input data specs must not be null, but may be empty.");
		m_hiddenOutputSpecs =
				checkNotNull(hiddenOutputSpecs, "Hidden output data specs must not be null, but may be empty.");
		m_outputSpecs = checkNotNull(outputSpecs, "Output data specs must not be null, but may be empty.");
		m_trainingConfig = Optional.empty();
	}

	/**
	 * Creates a new instance of this network spec.
	 *
	 * @param inputSpecs the input tensor specs, can be empty
	 * @param hiddenOutputSpecs the hidden output tensor specs, can be empty
	 * @param outputSpecs the output tensor specs, can be empty
	 * @param the {@link DLTrainingConfig training configuration}
	 */
	protected DLAbstractNetworkSpec(final DLTensorSpec[] inputSpecs,
			final DLTensorSpec[] hiddenOutputSpecs, final DLTensorSpec[] outputSpecs, final CFG trainingConfig) {
		m_inputSpecs = checkNotNull(inputSpecs, "Input data specs must not be null, but may be empty.");
		m_hiddenOutputSpecs =
				checkNotNull(hiddenOutputSpecs, "Hidden output data specs must not be null, but may be empty.");
		m_outputSpecs = checkNotNull(outputSpecs, "Output data specs must not be null, but may be empty.");
		m_trainingConfig = Optional.of(checkNotNull(trainingConfig, "Training configuration must not be null."));
	}

	protected abstract void hashCodeInternal(HashCodeBuilder b);

	protected abstract boolean equalsInternal(DLNetworkSpec other);

	@Override
	public DLTensorSpec[] getInputSpecs() {
		return m_inputSpecs;
	}

	@Override
	public DLTensorSpec[] getHiddenOutputSpecs() {
		return m_hiddenOutputSpecs;
	}

	@Override
	public DLTensorSpec[] getOutputSpecs() {
		return m_outputSpecs;
	}

	@Override
	public Optional<CFG> getTrainingConfig() {
		return m_trainingConfig;
	}

	@Override
	public int hashCode() {
		if (m_hashCode == 0) {
			m_hashCode = hashCodeInternal();
		}
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
		final DLAbstractNetworkSpec other = (DLAbstractNetworkSpec) obj;
		return other.m_inputSpecs.length == m_inputSpecs.length //
				&& other.m_hiddenOutputSpecs.length == m_hiddenOutputSpecs.length //
				&& other.m_outputSpecs.length == m_outputSpecs.length //
				&& Arrays.deepEquals(other.m_inputSpecs, m_inputSpecs) //
				&& Arrays.deepEquals(other.m_hiddenOutputSpecs, m_hiddenOutputSpecs) //
				&& Arrays.deepEquals(other.m_outputSpecs, m_outputSpecs) //
				&& other.m_trainingConfig.equals(m_trainingConfig)//
				&& equalsInternal(other);
	}

	@Override
	public String toString() {
		return "Inputs: " + Arrays.toString(m_inputSpecs) + "\n" + //
				"Hidden outputs: " + Arrays.toString(m_hiddenOutputSpecs) + "\n" + //
				"Outputs: " + Arrays.toString(m_outputSpecs) + "\n" + //
				"Training config: " + //
				(m_trainingConfig.isPresent() ? "\n" + m_trainingConfig.get().toString() : "none");
	}

	private void writeObject(final ObjectOutputStream stream) throws IOException {
		stream.defaultWriteObject();
		stream.writeObject(m_trainingConfig.orElse(null));
	}

	private void readObject(final ObjectInputStream stream) throws IOException, ClassNotFoundException {
		stream.defaultReadObject();
		@SuppressWarnings("unchecked") // we know what we serialized
		final CFG trainingConfig = (CFG) stream.readObject();
		m_trainingConfig = Optional.ofNullable(trainingConfig);
	}

	private int hashCodeInternal() {
		final HashCodeBuilder b = new HashCodeBuilder(17, 37);
		b.append(m_inputSpecs);
		b.append(m_hiddenOutputSpecs);
		b.append(m_outputSpecs);
		b.append(m_trainingConfig);
		hashCodeInternal(b);
		return b.toHashCode();
	}
}
