package org.knime.dl.core.data.convert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.knime.dl.testing.DLTestUtil.DOUBLE_EPSILON;
import static org.knime.dl.testing.DLTestUtil.createTensor;

import org.junit.Test;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DoubleCell;
import org.knime.dl.core.DLDefaultDimensionOrder;
import org.knime.dl.core.DLDefaultFixedTensorShape;
import org.knime.dl.core.DLDefaultTensorId;
import org.knime.dl.core.DLDefaultTensorSpec;
import org.knime.dl.core.DLTensor;
import org.knime.dl.core.data.DLReadableDoubleBuffer;
import org.knime.dl.core.data.DLWritableDoubleBuffer;

/**
 * @author Lukas Siedentop, KNIME GmbH, Konstanz, Germany
 */
public class DLDoubleTensorToDoubleCellConverterFactoryTest {

	@Test
	public void testConvert() {
		final DLDoubleTensorToDoubleCellConverterFactory factory = new DLDoubleTensorToDoubleCellConverterFactory();
		final DLTensorToDataCellConverter<DLReadableDoubleBuffer, DoubleCell> converter = factory.createConverter();
		final DLTensor<DLReadableDoubleBuffer> input = (DLTensor<DLReadableDoubleBuffer>) createTensor(Double.class, 1,
				3);
		final DLWritableDoubleBuffer buffer = (DLWritableDoubleBuffer) input.getBuffer();
		// data for testing
		buffer.put(0d);
		buffer.put(0d / 0d);
		buffer.put(1d / 0d);

		final DoubleCell[] output = new DoubleCell[3];

		converter.convert(input, output, null);

		assertEquals(3, output.length);
		assertEquals(0d, output[0].getDoubleValue(), DOUBLE_EPSILON);
		assertEquals(0d / 0d, output[1].getDoubleValue(), DOUBLE_EPSILON);
		assertEquals(1d / 0d, output[2].getDoubleValue(), DOUBLE_EPSILON);
	}

	@Test
	public void testGetDestCount() {
		final DLDoubleTensorToDoubleCellConverterFactory factory = new DLDoubleTensorToDoubleCellConverterFactory();
		final long[] shape = { 3 };
		final DLDefaultTensorSpec spec = new DLDefaultTensorSpec(new DLDefaultTensorId("1"), "Tspec", 1,
				new DLDefaultFixedTensorShape(shape), Double.class, DLDefaultDimensionOrder.TCDHW);

		assertTrue(factory.getDestCount(spec).isPresent());
		assertEquals(3l, factory.getDestCount(spec).getAsLong());
	}

	@Test
	public void testGetName() {
		final DLDoubleTensorToDoubleCellConverterFactory factory = new DLDoubleTensorToDoubleCellConverterFactory();
		assertEquals("Number (double)", factory.getName());
	}

	@Test
	public void testGetBufferType() {
		final DLDoubleTensorToDoubleCellConverterFactory factory = new DLDoubleTensorToDoubleCellConverterFactory();
		assertEquals(DLReadableDoubleBuffer.class, factory.getBufferType());
	}

	@Test
	public void testGetDestType() {
		final DLDoubleTensorToDoubleCellConverterFactory factory = new DLDoubleTensorToDoubleCellConverterFactory();
		assertEquals(factory.getDestType(), DataType.getType(DoubleCell.class));
	}
}
