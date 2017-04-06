package org.knime.dl.core.data.writables;

public interface DLWritableLongBuffer extends DLWritableIntBuffer {

    void put(long value);

    void putAll(long[] values);
}
