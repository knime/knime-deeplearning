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
package org.knime.dl.base.portobjects;

import static com.google.common.base.Preconditions.checkNotNull;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.knime.dl.core.DLNetwork;
import org.knime.dl.core.DLNetworkSpec;

/**
 * Abstract base class for deep learning {@link DLNetworkPortObjectSpec network port object specs}.
 *
 * @param <S> the network spec type
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public abstract class DLAbstractNetworkPortObjectSpec<S extends DLNetworkSpec> implements DLNetworkPortObjectSpec {

	/**
	 * The contained network spec. Must be populated by the port object spec's serializer.
	 */
	protected final S m_spec;

	/**
	 * The type of the network that is associated with the contained network spec. Must be populated by the port object
	 * spec's serializer.
	 */
	protected final Class<? extends DLNetwork> m_type;

	/**
	 * Creates a new instance of this port object spec.
	 *
	 * @param spec the network spec
	 * @param type the type of the network that is associated with the network spec
	 */
	protected DLAbstractNetworkPortObjectSpec(final S spec, final Class<? extends DLNetwork> type) {
		m_spec = checkNotNull(spec);
		m_type = checkNotNull(type);
	}

	protected abstract void hashCodeInternal(HashCodeBuilder b);

	protected abstract boolean equalsInternal(DLNetworkPortObjectSpec other);

	@Override
	public S getNetworkSpec() {
		return m_spec;
	}

	@Override
	public Class<? extends DLNetwork> getNetworkType() {
		return m_type;
	}

	@Override
	public final int hashCode() {
		final HashCodeBuilder b = new HashCodeBuilder(17, 37);
		b.append(m_spec);
		b.append(m_type.getCanonicalName());
		hashCodeInternal(b);
		return b.toHashCode();
	}

	@Override
	public final boolean equals(final Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj == null || obj.getClass() != getClass()) {
			return false;
		}
		final DLAbstractNetworkPortObjectSpec<?> other = (DLAbstractNetworkPortObjectSpec<?>) obj;
		return other.m_spec.equals(m_spec) //
				&& other.m_type.equals(m_type) //
				&& equalsInternal(other);
	}
}
