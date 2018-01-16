package org.knime.dl.core.data.convert;

import java.util.List;

import org.knime.core.data.DataValue;
import org.knime.dl.core.data.DLWritableBuffer;

public abstract class DLAbstractTensorDataValueToTensorConverterFactory 
<I extends DataValue, O extends DLWritableBuffer> implements DLDataValueToTensorConverterFactory<I, O> {

	@Override
	public final long[] getDataShape(List<? extends DataValue> input) {
		if (input.isEmpty()) {
			throw new IllegalArgumentException("No data values to extract shape from are provided");
		} else if (input.size() > 1) {
			throw new IllegalArgumentException(
					"For non-scalar data values, only single column selection is supported.");
		}
		I val;
		try {
			val = (I) input.get(0);
		} catch (ClassCastException e) {
			throw new IllegalArgumentException("The provided values are not compatible with the converter.");
		}
		return getDataShapeInternal((I) input.get(0));
	}
	
	protected abstract long[] getDataShapeInternal(I input);
}
