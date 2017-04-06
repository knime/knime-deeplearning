package org.knime.dl.core.data.writables;

public interface DLWritableBitBuffer extends DLWritableBuffer {

    void put(boolean value);

    void putAll(boolean[] values);
}
