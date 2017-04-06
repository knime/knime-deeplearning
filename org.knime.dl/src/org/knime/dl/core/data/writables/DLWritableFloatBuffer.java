package org.knime.dl.core.data.writables;

public interface DLWritableFloatBuffer extends DLWritableIntBuffer {

    void put(float value);

    void putAll(float[] values);
}
