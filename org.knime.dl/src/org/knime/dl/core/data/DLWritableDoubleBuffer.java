package org.knime.dl.core.data;

import java.nio.BufferOverflowException;

/**
 * A {@link DLWritableBuffer writable} double buffer.
 *
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
public interface DLWritableDoubleBuffer extends DLWritableFloatBuffer {

    /**
     * Writes a value into the buffer.
     *
     * @param value the value
     * @throws BufferOverflowException if the buffer's {@link #getCapacity() capacity} is exceeded.
     */
    void put(double value) throws BufferOverflowException;

    /**
     * Copies an array into the buffer.
     *
     * @param values the array
     * @throws BufferOverflowException if the buffer's {@link #getCapacity() capacity} is exceeded.
     */
    void putAll(double[] values) throws BufferOverflowException;
}
