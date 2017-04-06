package org.knime.dl.core.data.writables;

public interface DLWritableIntBuffer extends DLWritableShortBuffer {

    void put(int value);

    void putAll(int[] values);
}
