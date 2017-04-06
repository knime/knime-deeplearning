package org.knime.dl.core.data.writables;

public interface DLWritableDoubleBuffer extends DLWritableFloatBuffer {

	void put(double value);

	void putAll(double[] values);
}
