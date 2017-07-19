package org.knime.dl.core.data.writables;

import java.nio.BufferOverflowException;

/**
 * A {@link DLWritableBuffer writable} long buffer.
 *
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
public interface DLWritableLongBuffer extends DLWritableIntBuffer {

    /**
     * Writes a value into the buffer.
     *
     * @param value the value
     * @throws BufferOverflowException if the buffer's {@link #getCapacity() capacity} is exceeded.
     */
    void put(long value) throws BufferOverflowException;

    /**
     * Copies an array into the buffer.
     *
     * @param values the array
     * @throws BufferOverflowException if the buffer's {@link #getCapacity() capacity} is exceeded.
     */
    void putAll(long[] values) throws BufferOverflowException;
}
