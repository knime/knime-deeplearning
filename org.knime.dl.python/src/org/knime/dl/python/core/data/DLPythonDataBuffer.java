package org.knime.dl.python.core.data;

import org.knime.core.data.DataValue;
import org.knime.dl.core.data.DLWrappingDataBuffer;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public interface DLPythonDataBuffer<S> extends DLWrappingDataBuffer<S>, DataValue {

	// NB: marker interface
}
