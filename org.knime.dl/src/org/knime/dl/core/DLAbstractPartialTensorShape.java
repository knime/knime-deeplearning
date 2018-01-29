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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;
import java.util.Arrays;
import java.util.OptionalLong;
import java.util.stream.Collectors;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.knime.dl.util.DLUtils;

/**
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public abstract class DLAbstractPartialTensorShape extends DLAbstractTensorShape implements DLPartialTensorShape {

	private static final long serialVersionUID = 1L;

	/**
	 * Stored as long array because OptionalLong is not {@link Serializable}
	 */
	private final long[] m_shape;

	private final int m_numUnknown;

	private final long m_knownSize;

	public DLAbstractPartialTensorShape(final OptionalLong[] shape) {
		checkNotNull(shape);
		checkArgument(shape.length > 0, "Invalid tensor shape. Expected dimensionality greater than 0, was %s.",
				shape.length);
		m_shape = new long[shape.length];
		int unknown = 0;
		long knownSize = 1;
		for (int d = 0; d < shape.length; d++) {
			final OptionalLong dim = shape[d];
			if (dim.isPresent()) {
				knownSize = knownSize * dim.getAsLong();
				checkArgument(dim.getAsLong() > 0,
						"Invalid tensor shape. Expected shape dimension greater than 0 or unknown, but was %s in"
								+ " dimension %s.",
						dim.getAsLong(), d);
				m_shape[d] = dim.getAsLong();
			} else {
				unknown++;
				m_shape[d] = -1;
			}
		}
		checkArgument(unknown > 0, "Shape contains no unknown dimension sizes. Use a DLFixedTensorShape instead.");
		m_numUnknown = unknown;
		m_knownSize = knownSize;
	}

	@Override
	public int getNumDimensions() {
		return m_shape.length;
	}

	@Override
	public int getNumUnknownDimensions() {
		return m_numUnknown;
	}

	@Override
	public long getKnownSize() {
		return m_knownSize;
	}

	@Override
	public OptionalLong getDimension(final int i) {
		if (i < 0) {
			throw new IndexOutOfBoundsException("The dimension index must be positive.");
		} else if (i >= getNumDimensions()) {
			throw new IndexOutOfBoundsException(
					"The dimension index was >= the number of dimensions: " + i + " vs. " + getNumDimensions() + ".");
		}
		return m_shape[i] == -1 ? OptionalLong.empty() : OptionalLong.of(m_shape[i]);
	}

	@Override
	public String toString() {
		return Arrays.stream(m_shape).mapToObj(d -> d == -1 ? DLUtils.Shapes.UNKNOWN_DIM_SIZE_REPR : Long.toString(d))
				.collect(Collectors.joining(", ", "[", "]"));
	}

	@Override
	protected void hashCodeInternal(final HashCodeBuilder b) {
		b.append(m_shape);
	}

	@Override
	protected boolean equalsInternal(final DLTensorShape other) {
		return Arrays.equals(((DLAbstractPartialTensorShape) other).m_shape, m_shape);
	}
}
