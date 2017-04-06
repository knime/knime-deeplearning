package org.knime.dl.python.core.data;

import org.knime.core.data.DataValue;
import org.knime.dl.core.data.DLReadableBuffer;
import org.knime.dl.core.data.writables.DLWritableBuffer;

public interface DLPythonDataBuffer<S> extends DLReadableBuffer, DLWritableBuffer, DataValue {

    /**
     * {@inheritDoc}
     */
    public void putStorage(final S storage);
}
