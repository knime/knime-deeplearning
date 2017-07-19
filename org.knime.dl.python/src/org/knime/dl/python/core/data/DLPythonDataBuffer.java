package org.knime.dl.python.core.data;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

import org.knime.core.data.DataValue;
import org.knime.dl.core.data.DLReadableBuffer;
import org.knime.dl.core.data.writables.DLWritableBuffer;

/**
 * A {@link DLReadableBuffer readable} and {@link DLWritableBuffer writable} buffer specifically for use with Python
 * deep learning back ends.
 *
 * @param <S> the storage type
 *
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
public interface DLPythonDataBuffer<S> extends DLReadableBuffer, DLWritableBuffer, DataValue {

    /**
     * Returns the actual storage object that holds the buffer's data. The returned object must not be modified. It can
     * be read in a way that conforms to the arguments.
     *
     * @param startPos the position where the caller will start reading
     * @param length the number of elements that will be read
     * @return the buffer's storage
     * @throws BufferUnderflowException if the buffer's {@link #size() size} will be exceeded.
     */
    S getStorageForReading(long startPos, long length) throws BufferUnderflowException;

    /**
     * Returns the actual storage object that holds the buffer's data. The returned object can be modified in a way that
     * conforms to the arguments. Note that {@link #size()} will return a value equal to {@code startPos} +
     * {@code length} immediately after this method returns.
     *
     * @param startPos the position where the caller will start writing
     * @param length the number of elements that will be written
     * @return the buffer's storage
     * @throws BufferOverflowException if the buffer's {@link #getCapacity() capacity} will be exceeded.
     */
    S getStorageForWriting(long startPos, long length) throws BufferOverflowException;

    /**
     * Sets the buffer's internal storage to the first argument if its capacity matches {@link #getCapacity()}. Sets the
     * buffer's size to the second argument, i.e. {@link #size()} reports that value immediately after this method
     * returns.<br>
     * Also, this method has side effects that are equivalent to a call of {@link #resetRead()}.
     *
     * @param storage the storage to be set
     * @param storageSize the size of the storage
     * @throws IllegalArgumentException if the first argument's capacity does not equal {@link #getCapacity()}.
     */
    void setStorage(final S storage, long storageSize) throws IllegalArgumentException;

    /**
     * {@inheritDoc}
     */
    @Override
    default void reset() {
        resetRead();
        resetWrite();
    }
}
