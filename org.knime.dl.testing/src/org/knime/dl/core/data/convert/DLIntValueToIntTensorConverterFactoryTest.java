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
import org.knime.core.data.IntValue;
import org.knime.core.data.def.IntCell;
import org.knime.dl.core.DLTensor;
import org.knime.dl.core.data.DLReadableIntBuffer;
import org.knime.dl.core.data.DLWritableIntBuffer;

/**
 * @author Lukas Siedentop, KNIME GmbH, Konstanz, Germany
 */

public class DLIntValueToIntTensorConverterFactoryTest {

	@Test
	public void testConvert() {
		final DLIntValueToIntTensorConverterFactory factory = new DLIntValueToIntTensorConverterFactory();
		final DLDataValueToTensorConverter<IntValue, DLWritableIntBuffer> converter = factory.createConverter();
		final List<IntValue> input = Arrays.asList(new IntCell(0), new IntCell(Integer.MAX_VALUE),
				new IntCell(Integer.MIN_VALUE), new IntCell(Integer.MAX_VALUE + 1), new IntCell(Integer.MIN_VALUE - 1));

		final DLTensor<DLWritableIntBuffer> output = (DLTensor<DLWritableIntBuffer>) createTensor(Integer.class,
				1, input.size());

		converter.convert(input, output);
		final DLReadableIntBuffer outputAsReadable = (DLReadableIntBuffer) output.getBuffer();

		// assertEquals(input.size(), outputAsReadable.size() / output.getExampleSize());
		assertEquals(input.size(), outputAsReadable.size());
		assertEquals(input.get(0).getIntValue(), outputAsReadable.readNextInt());
		assertEquals(input.get(1).getIntValue(), outputAsReadable.readNextInt());
		assertEquals(input.get(2).getIntValue(), outputAsReadable.readNextInt());
		assertEquals(input.get(3).getIntValue(), outputAsReadable.readNextInt());
		assertEquals(input.get(4).getIntValue(), outputAsReadable.readNextInt());
	}

	@Test
	public void testGetDestCount() {
		final DLIntValueToIntTensorConverterFactory factory = new DLIntValueToIntTensorConverterFactory();

		final List<DataColumnSpec> spec = new ArrayList<>();
		final DataColumnSpecCreator cr = new DataColumnSpecCreator("creator", IntCell.TYPE);

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
		final DLIntValueToIntTensorConverterFactory factory = new DLIntValueToIntTensorConverterFactory();
		assertEquals("Number (integer)", factory.getName());
	}

	@Test
	public void testGetSourceType() {
		final DLIntValueToIntTensorConverterFactory factory = new DLIntValueToIntTensorConverterFactory();
		assertEquals(IntValue.class, factory.getSourceType());
	}
}
