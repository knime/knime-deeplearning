package org.knime.dl.core.data.writables;

public interface DLWritableByteBuffer extends DLWritableBitBuffer {

    void put(byte value);

    void putAll(byte[] values);
}
