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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataCellFactory.FromString;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.collection.CollectionCellFactory;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.dl.core.DLTensor;
import org.knime.dl.core.data.DLWritableBuffer;
import org.knime.dl.core.data.DLWritableDoubleBuffer;
import org.knime.dl.core.data.DLWritableFloatBuffer;
import org.knime.dl.core.data.DLWritableIntBuffer;
import org.knime.dl.core.data.DLWritableLongBuffer;
import org.knime.dl.core.data.convert.DLDataValueToTensorConverterFactory;
import org.knime.dl.core.data.convert.DLDataValueToTensorConverterRegistry;
import org.knime.dl.core.data.convert.DLIntCollectionValueToOneHotFloatTensorConverterFactory;
import org.knime.dl.testing.DLTestUtil;

import com.google.common.collect.Sets;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public class DLDataValueToTensorConverterRegistryTest {

	private static final DLDataValueToTensorConverterRegistry REGISTRY = DLDataValueToTensorConverterRegistry
			.getInstance();

	@Test
	public void testMatching() {
		// TODO: Bit vector, boolean, byte

		// Double
		DataType src = DoubleCell.TYPE;

		// .. to double
		Class<? extends DLWritableBuffer> dest = DLWritableDoubleBuffer.class;
		testForSourceDestCombination(src, dest, "1.0");

		// .. to float
		dest = DLWritableFloatBuffer.class;
		testForSourceDestCombination(src, dest, "1.0");

		// Int
		src = IntCell.TYPE;

		// .. to float
		testForSourceDestCombination(src, dest, "1");

		// .. to int
		dest = DLWritableIntBuffer.class;
		testForSourceDestCombination(src, dest, "1");

		// Long
		src = LongCell.TYPE;

		// .. to double
		dest = DLWritableDoubleBuffer.class;
		testForSourceDestCombination(src, dest, "1");

		// .. to long
		dest = DLWritableLongBuffer.class;
		testForSourceDestCombination(src, dest, "1");
	}

	private void testForSourceDestCombination(final DataType source, final Class<? extends DLWritableBuffer> dest,
			final String input) {
		final List<DLDataValueToTensorConverterFactory<? extends DataValue, ?>> converterFactories = REGISTRY
				.getConverterFactories(source, dest);

		Assert.assertFalse(converterFactories.isEmpty());
		Assert.assertTrue(converterFactories.contains(REGISTRY.getPreferredConverterFactory(source, dest).get()));
		final HashSet<DLDataValueToTensorConverterFactory<? extends DataValue, ?>> converterFactoriesForDest = new HashSet<>(
				REGISTRY.getConverterFactoriesForBufferType(dest));
		Assert.assertTrue(Sets.difference(new HashSet<>(converterFactories), converterFactoriesForDest).isEmpty());

		// NB: This method is only usable by source data types whose factory is available and supports string inputs.
		final FromString sourceFactory = ((FromString) source.getCellFactory(null).get());
		final List<DataCell> cell = Collections.singletonList(sourceFactory.createCell(input));
		final DLTensor<?> tensor = DLTestUtil.createTensorFromBufferType(dest, 1, 1);
		for (final DLDataValueToTensorConverterFactory factory : converterFactories) {
			Assert.assertTrue(REGISTRY.getConverterFactory(factory.getIdentifier()).get().equals(factory));
			factory.createConverter().convert(cell, tensor);
			tensor.getBuffer().reset();
		}

		// Test collection:
		final DataType collectionSource = ListCell.getCollectionType(source);
		final List<DLDataValueToTensorConverterFactory<? extends DataValue, ?>> collectionConverterFactories = REGISTRY
				.getConverterFactories(collectionSource, dest);

		Assert.assertFalse(collectionConverterFactories.isEmpty());
		Assert.assertTrue(collectionConverterFactories
				.contains(REGISTRY.getPreferredConverterFactory(collectionSource, dest).get()));
		Assert.assertTrue(
				Sets.difference(new HashSet<>(collectionConverterFactories), converterFactoriesForDest).isEmpty());

		final List<ListCell> collectionCell = Arrays.asList(CollectionCellFactory.createListCell(cell));
		for (final DLDataValueToTensorConverterFactory factory : collectionConverterFactories) {
			Assert.assertTrue(REGISTRY.getConverterFactory(factory.getIdentifier()).get().equals(factory));
			if (factory instanceof DLIntCollectionValueToOneHotFloatTensorConverterFactory) {
			    // will fail because it only supports Integer collection which we unfortunately can't detect prior to execution
			    continue;
			}
			factory.createConverter().convert(collectionCell, tensor);
			tensor.getBuffer().reset();
		}
	}
}
