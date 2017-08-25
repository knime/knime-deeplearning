/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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

import java.util.Arrays;

import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Abstract base class for network spec implementations.
 *
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
public abstract class DLAbstractNetworkSpec<NT extends DLNetworkType<?, ?>> implements DLNetworkSpec {

	private static final long serialVersionUID = 1L;

	private final NT m_networkType;

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
	protected DLAbstractNetworkSpec(final NT networkType, final DLLayerDataSpec[] inputSpecs,
			final DLLayerDataSpec[] intermediateOutputSpecs, final DLLayerDataSpec[] outputSpecs) {
		m_networkType = checkNotNull(networkType);
		m_inputSpecs = checkNotNull(inputSpecs);
		m_intermediateOutputSpecs = checkNotNull(intermediateOutputSpecs);
		m_outputSpecs = checkNotNull(outputSpecs);
		m_hashCode = hashCodeInternal();
	}

	protected abstract void hashCodeInternal(HashCodeBuilder b);

	protected abstract boolean equalsInternal(DLNetworkSpec other);

	@Override
	public NT getNetworkType() {
		return m_networkType;
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
		final DLNetworkSpec other = (DLNetworkSpec) obj;
		return other.getNetworkType().equals(getNetworkType()) //
				&& other.getInputSpecs().length == getInputSpecs().length //
				&& other.getIntermediateOutputSpecs().length == getIntermediateOutputSpecs().length //
				&& other.getOutputSpecs().length == getOutputSpecs().length //
				&& Arrays.deepEquals(other.getInputSpecs(), getInputSpecs()) //
				&& Arrays.deepEquals(other.getIntermediateOutputSpecs(), getIntermediateOutputSpecs()) //
				&& Arrays.deepEquals(other.getOutputSpecs(), getOutputSpecs()) //
				&& equalsInternal(other);
	}

	private int hashCodeInternal() {
		final HashCodeBuilder b = new HashCodeBuilder();
		b.append(m_networkType);
		b.append(m_inputSpecs);
		b.append(m_intermediateOutputSpecs);
		b.append(m_outputSpecs);
		hashCodeInternal(b);
		return b.toHashCode();
	}
}
