package org.knime.dl.core.data.convert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.knime.dl.testing.DLTestUtil.createTensor;

import org.junit.Test;
import org.knime.core.data.DataType;
import org.knime.core.data.def.IntCell;
import org.knime.dl.core.DLDefaultDimensionOrder;
import org.knime.dl.core.DLDefaultFixedTensorShape;
import org.knime.dl.core.DLDefaultTensorId;
import org.knime.dl.core.DLDefaultTensorSpec;
import org.knime.dl.core.DLTensor;
import org.knime.dl.core.data.DLReadableIntBuffer;
import org.knime.dl.core.data.DLWritableIntBuffer;

/**
 * @author Lukas Siedentop, KNIME GmbH, Konstanz, Germany
 */
public class DLIntTensorToIntCellConverterFactoryTest {

	@Test
	public void testConvert() {
		final DLIntTensorToIntCellConverterFactory factory = new DLIntTensorToIntCellConverterFactory();
		final DLTensorToDataCellConverter<DLReadableIntBuffer, IntCell> converter = factory.createConverter();
		final DLTensor<DLReadableIntBuffer> input = (DLTensor<DLReadableIntBuffer>) createTensor(Integer.class, 1, 5);
		final DLWritableIntBuffer buffer = (DLWritableIntBuffer) input.getBuffer();
		// data for testing
		buffer.put(0);
		buffer.put(Integer.MAX_VALUE);
		buffer.put(Integer.MIN_VALUE);
		buffer.put(Integer.MAX_VALUE + 1);
		buffer.put(Integer.MIN_VALUE - 1);

		final IntCell[] output = new IntCell[5];

		converter.convert(input, output, null);

		assertEquals(5, output.length);
		assertEquals(0, output[0].getIntValue());
		assertEquals(Integer.MAX_VALUE, output[1].getIntValue());
		assertEquals(Integer.MIN_VALUE, output[2].getIntValue());
		assertEquals(Integer.MAX_VALUE + 1, output[3].getIntValue());
		assertEquals(Integer.MIN_VALUE - 1, output[4].getIntValue());
	}

	@Test
	public void testGetDestCount() {
		final DLIntTensorToIntCellConverterFactory factory = new DLIntTensorToIntCellConverterFactory();
		final long[] shape = { 3 };
		final DLDefaultTensorSpec spec = new DLDefaultTensorSpec(new DLDefaultTensorId("1"), "Tspec", 1,
				new DLDefaultFixedTensorShape(shape), Integer.class, DLDefaultDimensionOrder.TCDHW);

		assertTrue(factory.getDestCount(spec).isPresent());
		assertEquals(3l, factory.getDestCount(spec).getAsLong());
	}

	@Test
	public void testGetName() {
		final DLIntTensorToIntCellConverterFactory factory = new DLIntTensorToIntCellConverterFactory();
		assertEquals("Number (integer)", factory.getName());
	}

	@Test
	public void testGetBufferType() {
		final DLIntTensorToIntCellConverterFactory factory = new DLIntTensorToIntCellConverterFactory();
		assertEquals(factory.getBufferType(), DLReadableIntBuffer.class);
	}

	@Test
	public void testGetDestType() {
		final DLIntTensorToIntCellConverterFactory factory = new DLIntTensorToIntCellConverterFactory();
		assertEquals(DataType.getType(IntCell.class), factory.getDestType());
	}
}
