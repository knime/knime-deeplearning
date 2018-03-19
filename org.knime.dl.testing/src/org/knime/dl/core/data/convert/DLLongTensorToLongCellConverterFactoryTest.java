package org.knime.dl.core.data.convert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.knime.dl.testing.DLTestUtil.createTensor;

import org.junit.Test;
import org.knime.core.data.DataType;
import org.knime.core.data.def.LongCell;
import org.knime.dl.core.DLDefaultDimensionOrder;
import org.knime.dl.core.DLDefaultFixedTensorShape;
import org.knime.dl.core.DLDefaultTensorId;
import org.knime.dl.core.DLDefaultTensorSpec;
import org.knime.dl.core.DLTensor;
import org.knime.dl.core.data.DLReadableLongBuffer;
import org.knime.dl.core.data.DLWritableLongBuffer;

/**
 * @author Lukas Siedentop, KNIME GmbH, Konstanz, Germany
 */
public class DLLongTensorToLongCellConverterFactoryTest {

	@Test
	public void testConvert() {
		final DLLongTensorToLongCellConverterFactory factory = new DLLongTensorToLongCellConverterFactory();
		final DLTensorToDataCellConverter<DLReadableLongBuffer, LongCell> converter = factory.createConverter();
		final DLTensor<DLReadableLongBuffer> input = (DLTensor<DLReadableLongBuffer>) createTensor(Long.class, 1, 5);
		final DLWritableLongBuffer buffer = (DLWritableLongBuffer) input.getBuffer();
		// data for testing
		buffer.put(0);
		buffer.put(Long.MAX_VALUE);
		buffer.put(Long.MIN_VALUE);
		buffer.put(Long.MAX_VALUE + 1);
		buffer.put(Long.MIN_VALUE - 1);

		final LongCell[] output = new LongCell[5];

		converter.convert(input, output, null);

		assertEquals(5, output.length);
		assertEquals(0, output[0].getLongValue());
		assertEquals(Long.MAX_VALUE, output[1].getLongValue());
		assertEquals(Long.MIN_VALUE, output[2].getLongValue());
		assertEquals(Long.MAX_VALUE + 1, output[3].getLongValue());
		assertEquals(Long.MIN_VALUE - 1, output[4].getLongValue());
	}

	@Test
	public void testGetDestCount() {
		final DLLongTensorToLongCellConverterFactory factory = new DLLongTensorToLongCellConverterFactory();
		final long[] shape = { 3 };
		final DLDefaultTensorSpec spec = new DLDefaultTensorSpec(new DLDefaultTensorId("1"), "Tspec", 1,
				new DLDefaultFixedTensorShape(shape), Long.class, DLDefaultDimensionOrder.TCDHW);

		assertTrue(factory.getDestCount(spec).isPresent());
		assertEquals(3l, factory.getDestCount(spec).getAsLong());
	}

	@Test
	public void testGetName() {
		final DLLongTensorToLongCellConverterFactory factory = new DLLongTensorToLongCellConverterFactory();
		assertEquals("Number (long)", factory.getName());
	}

	@Test
	public void testGetBufferType() {
		final DLLongTensorToLongCellConverterFactory factory = new DLLongTensorToLongCellConverterFactory();
		assertEquals(factory.getBufferType(), DLReadableLongBuffer.class);
	}

	@Test
	public void testGetDestType() {
		final DLLongTensorToLongCellConverterFactory factory = new DLLongTensorToLongCellConverterFactory();
		assertEquals(DataType.getType(LongCell.class), factory.getDestType());
	}
}
