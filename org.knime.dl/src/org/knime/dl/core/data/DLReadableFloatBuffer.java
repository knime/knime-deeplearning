package org.knime.dl.core.data;

import java.nio.BufferUnderflowException;

/**
 * A {@link DLReadableBuffer readable} float buffer.
 *
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
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
	
	/**
	 * Reads <b>length</b> values from the buffer into the <b>dest</b> array starting from the next value in the buffer.
	 * 
	 * @param dest destination array
	 * @param destPos position at which to start writing in <b>dest</b>
	 * @param length number of elements to read from the buffer
	 * @throws BufferUnderflowException if the buffer's {@link #size() size} is exceeded.
	 */
	void readToFloatArray(float[] dest, int destPos, int length);
}
