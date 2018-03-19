package org.knime.dl.core.data.convert;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.knime.dl.testing.DLTestUtil.DOUBLE_EPSILON;
import static org.knime.dl.testing.DLTestUtil.createTensor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.def.DoubleCell;
import org.knime.dl.core.DLTensor;
import org.knime.dl.core.data.DLReadableFloatBuffer;
import org.knime.dl.core.data.DLWritableFloatBuffer;

/**
 * @author Lukas Siedentop, KNIME GmbH, Konstanz, Germany
 */

public class DLDoubleValueToFloatTensorConverterFactoryTest {

	@Test
	public void testConvert() {
		final DLDoubleValueToFloatTensorConverterFactory factory = new DLDoubleValueToFloatTensorConverterFactory();
		final DLDataValueToTensorConverter<DoubleValue, DLWritableFloatBuffer> converter = factory.createConverter();
		final List<DoubleValue> input = Arrays.asList(new DoubleCell(0d), new DoubleCell(0d / 0d),
				new DoubleCell(1d / 0d));

		final DLTensor<DLWritableFloatBuffer> output = (DLTensor<DLWritableFloatBuffer>) createTensor(Float.class,
				1, input.size());
		converter.convert(input, output);
		final DLReadableFloatBuffer outputAsReadable = (DLReadableFloatBuffer) output.getBuffer();

		// assertEquals(input.size(), outputAsReadable.size() / output.getExampleSize());
		assertEquals(input.size(), outputAsReadable.size());
		assertEquals(input.get(0).getDoubleValue(), outputAsReadable.readNextDouble(), DOUBLE_EPSILON);
		assertTrue(Double.isNaN(input.get(1).getDoubleValue()));
		assertTrue(Double.isInfinite(input.get(2).getDoubleValue()));
	}

	@Test
	public void testGetDestCount() {
		final DLDoubleValueToFloatTensorConverterFactory factory = new DLDoubleValueToFloatTensorConverterFactory();
		final List<DataColumnSpec> spec = new ArrayList<>();
		final DataColumnSpecCreator cr = new DataColumnSpecCreator("creator", DoubleCell.TYPE);

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
		final DLDoubleValueToFloatTensorConverterFactory factory = new DLDoubleValueToFloatTensorConverterFactory();
		assertEquals("Number (double)", factory.getName());
	}

	@Test
	public void testGetSourceType() {
		final DLDoubleValueToFloatTensorConverterFactory factory = new DLDoubleValueToFloatTensorConverterFactory();
		assertEquals(DoubleValue.class, factory.getSourceType());
	}
}
