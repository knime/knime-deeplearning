package org.knime.dl.core.data.writables;

import java.nio.BufferOverflowException;

/**
 * A {@link DLWritableBuffer writable} short buffer.
 *
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
public interface DLWritableShortBuffer extends DLWritableByteBuffer {

    /**
     * Writes a value into the buffer.
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
