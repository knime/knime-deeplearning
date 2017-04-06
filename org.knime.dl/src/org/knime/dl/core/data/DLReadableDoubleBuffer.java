package org.knime.dl.core.data;

public interface DLReadableDoubleBuffer extends DLReadableBuffer {

    double readNextDouble();

    double[] toDoubleArray();
}
