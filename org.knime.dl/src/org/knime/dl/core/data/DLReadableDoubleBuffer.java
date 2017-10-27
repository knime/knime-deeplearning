package org.knime.dl.core.data;

import java.nio.BufferUnderflowException;

/**
 * A {@link DLReadableBuffer readable} double buffer.
 *
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
public interface DLReadableDoubleBuffer extends DLReadableBuffer {

	/**
	 * Reads the next value from the buffer.
	 *
	 * @return the value
	 * @throws BufferUnderflowException if the buffer's {@link #size() size} is exceeded.
	 */
	double readNextDouble() throws BufferUnderflowException;

	/**
	 * Returns a copy of the buffer's content.
	 *
	 * @return the buffer's content
	 */
	double[] toDoubleArray();
}
