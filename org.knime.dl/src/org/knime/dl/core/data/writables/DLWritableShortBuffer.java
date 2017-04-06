package org.knime.dl.core.data.writables;

public interface DLWritableShortBuffer extends DLWritableByteBuffer {

    void put(short value);

    void putAll(short[] values);
}
