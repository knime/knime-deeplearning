package org.knime.dl.core.data;

import java.nio.BufferUnderflowException;

/**
 * A {@link DLReadableBuffer readable} float buffer.
 *
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
public interface DLReadableFloatBuffer extends DLReadableDoubleBuffer {

	/**
	 * Reads the next value from the buffer.
	 *
	 * @return the value
	 * @throws BufferUnderflowException if the buffer's {@link #size() size} is exceeded.
	 */
	float readNextFloat() throws BufferUnderflowException;

	/**
	 * Returns a copy of the buffer's content.
	 *
	 * @return the buffer's content
	 */
	float[] toFloatArray();
}
