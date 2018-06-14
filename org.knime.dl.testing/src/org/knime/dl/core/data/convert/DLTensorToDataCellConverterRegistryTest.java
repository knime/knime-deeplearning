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

import java.lang.reflect.Array;
import java.util.HashSet;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.knime.core.data.DataCell;
import org.knime.dl.core.DLDefaultFixedTensorShape;
import org.knime.dl.core.DLDefaultTensorId;
import org.knime.dl.core.DLDefaultTensorSpec;
import org.knime.dl.core.DLDimensionOrder;
import org.knime.dl.core.DLTensor;
import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.core.data.DLReadableBuffer;
import org.knime.dl.core.data.DLReadableDoubleBuffer;
import org.knime.dl.core.data.DLReadableFloatBuffer;
import org.knime.dl.core.data.DLReadableIntBuffer;
import org.knime.dl.core.data.DLReadableLongBuffer;
import org.knime.dl.core.data.DLWritableBuffer;
import org.knime.dl.testing.DLTestUtil;
import org.knime.dl.util.DLUtils;

import com.google.common.collect.Sets;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public class DLTensorToDataCellConverterRegistryTest {

	private static final DLTensorToDataCellConverterRegistry REGISTRY = DLTensorToDataCellConverterRegistry
			.getInstance();

	@Test
	public void testMatching() {
		// TODO: Bit, byte, short

		// Double
		Class<? extends DLReadableBuffer> src = DLReadableDoubleBuffer.class;
		testForSource(src);

		// Float
		src = DLReadableFloatBuffer.class;
		testForSource(src);

		// Int
		src = DLReadableIntBuffer.class;
		testForSource(src);

		// Long
		src = DLReadableLongBuffer.class;
		testForSource(src);
	}

	private void testForSource(final Class<? extends DLReadableBuffer> source) {
		final DLTensorSpec spec = new DLDefaultTensorSpec(new DLDefaultTensorId("input"), "input", 1,
				new DLDefaultFixedTensorShape(new long[] { 2l }), DLTestUtil.TENSOR_FACTORY.getElementType(source),
				DLDimensionOrder.TCDHW);

		final List<DLTensorToDataCellConverterFactory<?, ? extends DataCell>> converterFactories = REGISTRY
				.getFactoriesForSourceType(source, spec);

		Assert.assertFalse(converterFactories.isEmpty());
		Assert.assertTrue(Sets.difference(new HashSet<>(REGISTRY.getPreferredFactoriesForSourceType(source, spec)),
				new HashSet<>(converterFactories)).isEmpty());

		final DLTensor<?> tensor = DLTestUtil.createTensorFromBufferType(source, spec.getBatchSize().getAsLong(),
				DLUtils.Shapes.getFixedShape(spec.getShape()).get());
		((DLWritableBuffer) tensor.getBuffer()).zeroPad(tensor.getExampleSize());
		for (final DLTensorToDataCellConverterFactory factory : converterFactories) {
			Assert.assertTrue(REGISTRY.getConverterFactory(factory.getIdentifier()).get().equals(factory));
			final DataCell[] cells = (DataCell[]) Array.newInstance(factory.getDestType().getCellClass(),
					(int) factory.getDestCount(spec).getAsLong());
			try {
				factory.createConverter().convert(tensor, cells, null);
			} catch (final NullPointerException ex) {
				// TODO: Image converters fail because of some file store stuff (missing execution context?).
				if (!factory.getClass().getCanonicalName().contains("org.knime.ip.dl")) {
					throw ex;
				}
			}
			((DLReadableBuffer) tensor.getBuffer()).resetRead();
		}
	}
}
