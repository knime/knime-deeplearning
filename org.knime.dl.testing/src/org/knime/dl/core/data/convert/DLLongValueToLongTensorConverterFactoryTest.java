package org.knime.dl.core.data.convert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.knime.dl.testing.DLTestUtil.createTensor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.LongValue;
import org.knime.core.data.def.LongCell;
import org.knime.dl.core.DLTensor;
import org.knime.dl.core.data.DLReadableLongBuffer;
import org.knime.dl.core.data.DLWritableLongBuffer;

/**
 * @author Lukas Siedentop, KNIME GmbH, Konstanz, Germany
 */

public class DLLongValueToLongTensorConverterFactoryTest {

	@Test
	public void testConvert() {
		final DLLongValueToLongTensorConverterFactory factory = new DLLongValueToLongTensorConverterFactory();
		final DLDataValueToTensorConverter<LongValue, DLWritableLongBuffer> converter = factory.createConverter();
		final List<LongValue> input = Arrays.asList(new LongCell(0l), new LongCell(Long.MAX_VALUE),
				new LongCell(Long.MIN_VALUE), new LongCell(Long.MAX_VALUE + 1), new LongCell(Long.MIN_VALUE - 1));

		final DLTensor<DLWritableLongBuffer> output = (DLTensor<DLWritableLongBuffer>) createTensor(Long.class,
				1, input.size());
		converter.convert(input, output);
		final DLReadableLongBuffer outputAsReadable = (DLReadableLongBuffer) output.getBuffer();

		// assertEquals(input.size(), outputAsReadable.size() / output.getExampleSize());
		assertEquals(input.size(), outputAsReadable.size());
		assertEquals(input.get(0).getLongValue(), outputAsReadable.readNextLong());
		assertEquals(input.get(1).getLongValue(), outputAsReadable.readNextLong());
		assertEquals(input.get(2).getLongValue(), outputAsReadable.readNextLong());
		assertEquals(input.get(3).getLongValue(), outputAsReadable.readNextLong());
		assertEquals(input.get(4).getLongValue(), outputAsReadable.readNextLong());
	}

	@Test
	public void testGetDestCount() {
		final DLLongValueToLongTensorConverterFactory factory = new DLLongValueToLongTensorConverterFactory();

		final List<DataColumnSpec> spec = new ArrayList<>();
		final DataColumnSpecCreator cr = new DataColumnSpecCreator("creator", LongCell.TYPE);

		// Only size of list is returned, its content won't be checked
		assertTrue(factory.getDestCount(spec).isPresent());
		assertEquals(0l, factory.getDestCount(spec).getAsLong());
		spec.add(cr.createSpec());
		assertEquals(1l, factory.getDestCount(spec).getAsLong());
		spec.add(cr.createSpec());
		assertEquals(2l, factory.getDestCount(spec).getAsLong());
		spec.add(cr.createSpec());
		assertEquals(3l, factory.getDestCount(spec).getAsLong());
	}

	@Test
	public void testGetName() {
		final DLLongValueToLongTensorConverterFactory factory = new DLLongValueToLongTensorConverterFactory();
		assertEquals("Number (long)", factory.getName());
	}

	@Test
	public void testGetSourceType() {
		final DLLongValueToLongTensorConverterFactory factory = new DLLongValueToLongTensorConverterFactory();
		assertEquals(LongValue.class, factory.getSourceType());
	}
}
