package org.knime.dl.core;
import java.io.Serializable;
import java.util.Arrays;
import java.util.OptionalLong;
import java.util.stream.Collectors;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.knime.dl.util.DLUtils;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;


public abstract class DLAbstractPartialTensorShape extends DLAbstractTensorShape implements DLPartialTensorShape {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * Stored as long array because OptionalLong is not {@link Serializable}
	 */
	private long[] m_shape;
	private int m_numUnknown;
	private long m_knownSize;
	
	public DLAbstractPartialTensorShape(OptionalLong[] shape) {
		checkNotNull(shape);
		checkArgument(shape.length > 0, "Invalid tensor shape. Expected dimensionality greater than 0, was %s.",
				shape.length);
		m_shape = new long[shape.length];
		int unKnown = 0;
		long knownSize = 1;
		for (int d = 0; d < shape.length; d++) {
			OptionalLong dim = shape[d];
			if (dim.isPresent()) {
				knownSize = knownSize * dim.getAsLong();
				checkArgument(dim.getAsLong() > 0,
						"Invalid tensor shape. Expected shape dimension greater than 0 or unknown, but was %s in"
						+ " dimension %s.", dim.getAsLong(),
						d);
				m_shape[d] = dim.getAsLong();
			} else {
				unKnown++;
				m_shape[d] = -1;
			}
		}
		checkArgument(unKnown > 0, "Shape contains no unknown dimension sizes. Use a DLFixedTensorShape instead.");
		m_numUnknown = unKnown;
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
	public OptionalLong getDimension(int i) {
		if (i < 0) {
			throw new IndexOutOfBoundsException("The dimension index must be positive.");
		} else if (i >= getNumDimensions()) {
			throw new IndexOutOfBoundsException(
					"The dimension index was >= the number of dimensions: " + i + " vs. " + getNumDimensions());
		}
		return m_shape[i] == -1 ? OptionalLong.empty() : OptionalLong.of(m_shape[i]);
	}

	@Override
	protected void hashCodeInternal(HashCodeBuilder b) {
		b.append(m_shape);
	}

	@Override
	protected boolean equalsInternal(DLTensorShape other) {
		return Arrays.equals(((DLAbstractPartialTensorShape) other).m_shape, m_shape);
	}
	
	@Override
	public String toString() {
		return Arrays.stream(m_shape).mapToObj(d -> d == -1 ? DLUtils.Shapes.UNKNOWN_DIM_SIZE_REPR : Long.toString(d))
				.collect(Collectors.joining(", ", "[", "]"));
	}


}
