package org.knime.dl.core.data;

import java.nio.BufferOverflowException;

/**
 * A {@link DLWritableBuffer writable} byte buffer.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public interface DLWritableUnsignedByteBuffer extends DLWritableBitBuffer {

	/**
	 * Writes an unsigned byte value into the buffer.
	 *
	 * @param value the value
	 * @throws BufferOverflowException if the buffer's {@link #getCapacity() capacity} is exceeded.
	 */
	void put(short value) throws BufferOverflowException;

	/**
	 * Copies an array into the buffer.
	 *
	 * @param values the array
	 * @throws BufferOverflowException if the buffer's {@link #getCapacity() capacity} is exceeded.
	 */
	void putAll(short[] values) throws BufferOverflowException;
}
