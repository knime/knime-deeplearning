/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 */
package org.knime.dl.core.data.convert;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.OptionalLong;
import java.util.stream.Stream;

import org.junit.Test;
import org.knime.core.data.DataCell;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.dl.core.DLDefaultFixedTensorShape;
import org.knime.dl.core.DLDefaultPartialTensorShape;
import org.knime.dl.core.DLDefaultTensor;
import org.knime.dl.core.DLDefaultTensorId;
import org.knime.dl.core.DLDefaultTensorSpec;
import org.knime.dl.core.DLDimensionOrder;
import org.knime.dl.core.DLInvalidNetworkInputException;
import org.knime.dl.core.DLTensor;
import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.core.data.DLDefaultFloatBuffer;
import org.knime.dl.core.data.DLWritableFloatBuffer;

/**
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public class DLIntCollectionValueToOneHotFloatTensorConverterFactoryTest {
	
	private static final float EPSILON = 0.00001f;
	
	private static CollectionDataValue createCollectionValue(String valueString, String typeString) {
		int[] values = Arrays.stream(valueString.split(",")).mapToInt(Integer::parseInt).toArray();
		String[] types = typeString.split(",");
		ArrayList<DataCell> cells = new ArrayList<>();
		for (int i = 0; i < types.length; i++) {
			switch (types[i]) {
			case "i":
				cells.add(new IntCell(values[i]));
				break;
			case "d":
				cells.add(new DoubleCell(values[i]));
				break;
			}
		}
		return CollectionCellFactory.createListCell(cells);
	}
	
	private static DLTensorSpec createTensorSpec(String shapeString) {
		Stream<String> shapeStream = Arrays.stream(shapeString.split(","));
		if (shapeString.contains("?")) {
			return new DLDefaultTensorSpec(new DLDefaultTensorId("id"), "tensor",
					new DLDefaultPartialTensorShape(
							shapeStream.map(s -> s.equals("?") ? OptionalLong.empty() 
									: OptionalLong.of(Long.parseLong(s)))
							.toArray(i -> new OptionalLong[i])),
					float.class, DLDimensionOrder.TDHWC);
		}
		return new DLDefaultTensorSpec(new DLDefaultTensorId("id"), "tensor",
				new DLDefaultFixedTensorShape(
						shapeStream.mapToLong(s -> Long.parseLong(s)).toArray()),
				float.class, DLDimensionOrder.TDHWC);
	}

	@Test
	public void testGetDataShape() throws Exception {
		DLIntCollectionValueToOneHotFloatTensorConverterFactory factory =
				new DLIntCollectionValueToOneHotFloatTensorConverterFactory();
		List<CollectionDataValue> value = Arrays.asList(createCollectionValue("0,1,2", "i,i,i"));
		DLTensorSpec spec = createTensorSpec("?,5");
		assertArrayEquals(new long[] {3, 5}, factory.getDataShape(value, spec));
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testGetDataShapeFailsOnTooLargeIndex() throws Exception {
		DLIntCollectionValueToOneHotFloatTensorConverterFactory factory =
				new DLIntCollectionValueToOneHotFloatTensorConverterFactory();
		List<CollectionDataValue> value = Arrays.asList(createCollectionValue("0,1,2", "i,i,i"));
		DLTensorSpec spec = createTensorSpec("?,2");
		factory.getDataShape(value, spec);
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testGetDataShapeFailsOnNegativeIndex() throws Exception {
		DLIntCollectionValueToOneHotFloatTensorConverterFactory factory =
				new DLIntCollectionValueToOneHotFloatTensorConverterFactory();
		List<CollectionDataValue> value = Arrays.asList(createCollectionValue("0,-1,2", "i,i,i"));
		DLTensorSpec spec = createTensorSpec("3,5");
		factory.getDataShape(value, spec);
	}
	
	@Test (expected = DLInvalidNetworkInputException.class)
	public void testGetDataShapeFailsOnPartialFeatureDim() throws Exception {
		DLIntCollectionValueToOneHotFloatTensorConverterFactory factory =
				new DLIntCollectionValueToOneHotFloatTensorConverterFactory();
		List<CollectionDataValue> value = Arrays.asList(createCollectionValue("0,1,2", "i,i,i"));
		DLTensorSpec spec = createTensorSpec("?,?");
		factory.getDataShape(value, spec);
	}
	
	@Test (expected = IllegalArgumentException.class)
	public void testGetDataShapeFailsOnNonIntCollection() throws Exception {
		DLIntCollectionValueToOneHotFloatTensorConverterFactory factory =
				new DLIntCollectionValueToOneHotFloatTensorConverterFactory();
		List<CollectionDataValue> value = Arrays.asList(createCollectionValue("0,1,2", "i,d,i"));
		DLTensorSpec spec = createTensorSpec("3,5");
		factory.getDataShape(value, spec);
	}
	
	@Test (expected = DLInvalidNetworkInputException.class)
	public void testGetDataShapeFailsOnTooLargeFeatureDim() throws Exception {
		DLIntCollectionValueToOneHotFloatTensorConverterFactory factory =
				new DLIntCollectionValueToOneHotFloatTensorConverterFactory();
		List<CollectionDataValue> value = Arrays.asList(createCollectionValue("0,1,2", "i,i,i"));
		DLTensorSpec spec = createTensorSpec("3," + Long.MAX_VALUE);
		factory.getDataShape(value, spec);
	}
	
	@Test
	public void testCreateConverter() throws Exception {
		DLIntCollectionValueToOneHotFloatTensorConverterFactory factory =
				new DLIntCollectionValueToOneHotFloatTensorConverterFactory();
		List<CollectionDataValue> value = Arrays.asList(createCollectionValue("0,1,2", "i,i,i"));
		DLTensorSpec spec = createTensorSpec("?,5");
		int exampleSize = 15;
		DLDataValueToTensorConverter<CollectionDataValue, DLWritableFloatBuffer> converter = factory.createConverter();
		try (DLDefaultFloatBuffer buffer = new DLDefaultFloatBuffer(exampleSize);
				DLTensor<DLWritableFloatBuffer> tensor = new DLDefaultTensor<>(spec, buffer, exampleSize)) {
			converter.convert(value, tensor);
			assertEquals(1, buffer.readNextFloat(), EPSILON);
			assertEquals(0, buffer.readNextFloat(), EPSILON);
			assertEquals(0, buffer.readNextFloat(), EPSILON);
			assertEquals(0, buffer.readNextFloat(), EPSILON);
			assertEquals(0, buffer.readNextFloat(), EPSILON);
			
			assertEquals(0, buffer.readNextFloat(), EPSILON);
			assertEquals(1, buffer.readNextFloat(), EPSILON);
			assertEquals(0, buffer.readNextFloat(), EPSILON);
			assertEquals(0, buffer.readNextFloat(), EPSILON);
			assertEquals(0, buffer.readNextFloat(), EPSILON);
			
			assertEquals(0, buffer.readNextFloat(), EPSILON);
			assertEquals(0, buffer.readNextFloat(), EPSILON);
			assertEquals(1, buffer.readNextFloat(), EPSILON);
			assertEquals(0, buffer.readNextFloat(), EPSILON);
			assertEquals(0, buffer.readNextFloat(), EPSILON);
		}
	}
}
