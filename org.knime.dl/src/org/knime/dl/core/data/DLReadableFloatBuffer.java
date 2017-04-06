package org.knime.dl.core.data;

public interface DLReadableFloatBuffer extends DLReadableDoubleBuffer {

    float readNextFloat();

    float[] toFloatArray();
}
