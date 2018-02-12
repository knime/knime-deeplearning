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
 * History
 *   May 30, 2017 (marcel): created
 */
package org.knime.dl.core;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.knime.dl.util.DLUtils.Preconditions.checkNotNullOrEmpty;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Objects;
import java.util.OptionalLong;

import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Abstract base class for {@link DLTensorSpec tensor spec} implementations.
 *
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public abstract class DLAbstractTensorSpec implements DLTensorSpec {

	private static final long serialVersionUID = 1L;

	/**
	 * @since 3.6 - This field will be <code>null</code> when deserializing older versions of this spec. In this case,
	 *        the spec object is in a corrupt state and has to be discarded and the underlying network has to be reread
	 *        by the client code that triggered the deserialization. See {@link DLTensorSpec#getIdentifier()}.
	 */
	private final DLTensorId m_identifier;

	private final String m_name;

	private transient OptionalLong m_batchSize;

	private final DLTensorShape m_shape;
	
	private final DLDimensionOrder m_dimensionOrder;

	private final Class<?> m_elementType;

	private int m_hashCode = 0;

	/**
	 * @param identifier the identifier of the tensor
	 * @param name the name of the tensor
	 * @param batchSize the batch size of the tensor. Must be greater than zero.
	 * @param shape the shape of the tensor. Does not include the batch size.
	 * @param elementType the data type of the tensor's elements
	 */
	protected DLAbstractTensorSpec(final DLTensorId identifier, final String name, final long batchSize,
			final DLTensorShape shape, final Class<?> elementType) {
		m_identifier = checkNotNull(identifier);
		m_name = checkNotNullOrEmpty(name);
		checkArgument(batchSize > 0, "Invalid tensor batch size. Expected value greater than 0, was %s.", batchSize);
		m_batchSize = OptionalLong.of(batchSize);
		m_shape = checkNotNull(shape);
		m_elementType = checkNotNull(elementType);
		m_dimensionOrder = DLDefaultDimensionOrders.Unknown;
	}

	/**
	 * @param identifier the identifier of the tensor
	 * @param name the name of the tensor
	 * @param shape the shape of the tensor. Does not include the batch size.
	 * @param elementType the data type of the tensor's elements
	 */
	protected DLAbstractTensorSpec(final DLTensorId identifier, final String name, final DLTensorShape shape,
			final Class<?> elementType) {
		m_identifier = checkNotNull(identifier);
		m_name = checkNotNullOrEmpty(name);
		m_batchSize = OptionalLong.empty();
		m_shape = checkNotNull(shape);
		m_elementType = checkNotNull(elementType);
		m_dimensionOrder = DLDefaultDimensionOrders.Unknown;
	}

	/**
	 * @param identifier the identifier of the tensor
	 * @param name the name of the tensor
	 * @param batchSize the batch size of the tensor. Must be greater than zero.
	 * @param elementType the data type of the tensor's elements
	 */
	protected DLAbstractTensorSpec(final DLTensorId identifier, final String name, final long batchSize,
			final Class<?> elementType) {
		m_identifier = checkNotNull(identifier);
		m_name = checkNotNullOrEmpty(name);
		checkArgument(batchSize > 0, "Invalid tensor batch size. Expected value greater than 0, was %s.", batchSize);
		m_batchSize = OptionalLong.of(batchSize);
		m_shape = DLUnknownTensorShape.INSTANCE;
		m_elementType = checkNotNull(elementType);
		m_dimensionOrder = DLDefaultDimensionOrders.Unknown;
	}

	/**
	 * @param identifier the identifier of the tensor
	 * @param name the name of the tensor
	 * @param elementType the data type of the tensor's elements
	 */
	protected DLAbstractTensorSpec(final DLTensorId identifier, final String name, final Class<?> elementType) {
		m_identifier = checkNotNull(identifier);
		m_name = checkNotNullOrEmpty(name);
		m_batchSize = OptionalLong.empty();
		m_shape = DLUnknownTensorShape.INSTANCE;
		m_elementType = checkNotNull(elementType);
		m_dimensionOrder = DLDefaultDimensionOrders.Unknown;
	}

	protected abstract void hashCodeInternal(HashCodeBuilder b);

	protected abstract boolean equalsInternal(DLTensorSpec other);

	@Override
	public DLTensorId getIdentifier() {
		return m_identifier;
	}

	@Override
	public String getName() {
		return m_name;
	}

	@Override
	public OptionalLong getBatchSize() {
		return m_batchSize;
	}

	@Override
	public DLTensorShape getShape() {
		return m_shape;
	}
	
	@Override
	public DLDimensionOrder getDimensionOrder() {
		return m_dimensionOrder;
	}

	@Override
	public Class<?> getElementType() {
		return m_elementType;
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
		final DLAbstractTensorSpec other = (DLAbstractTensorSpec) obj;
		return Objects.equals(other.m_identifier, m_identifier) //
				&& other.m_name.equals(m_name) //
				&& other.m_batchSize.equals(m_batchSize) //
				&& other.m_shape.equals(m_shape) //
				&& other.m_elementType.equals(m_elementType) //
				&& equalsInternal(other);
	}

	@Override
	public String toString() {
		return Objects.toString(m_identifier) + " (" + m_name + "): "
				+ (m_batchSize.isPresent() ? m_batchSize.getAsLong() + ", " : "") + m_shape.toString() + ", "
				+ m_elementType.getSimpleName();
	}

	private void writeObject(final ObjectOutputStream stream) throws IOException {
		stream.defaultWriteObject();
		stream.writeLong(m_batchSize.orElse(-1));
	}

	private void readObject(final ObjectInputStream stream) throws IOException, ClassNotFoundException {
		stream.defaultReadObject();
		final long batchSize = stream.readLong();
		m_batchSize = batchSize != -1 ? OptionalLong.of(batchSize) : OptionalLong.empty();
	}

	private int hashCodeInternal() {
		final HashCodeBuilder b = new HashCodeBuilder(17, 37);
		b.append(m_identifier);
		b.append(m_name);
		b.append(m_batchSize);
		b.append(m_shape);
		b.append(m_elementType);
		hashCodeInternal(b);
		return b.toHashCode();
	}
}
