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
package org.knime.dl.testing;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.InvalidPathException;
import java.util.Arrays;
import java.util.OptionalLong;
import java.util.Random;

import org.apache.commons.lang3.RandomStringUtils;
import org.knime.core.util.FileUtil;
import org.knime.dl.core.DLDefaultDimensionOrder;
import org.knime.dl.core.DLDefaultFixedTensorShape;
import org.knime.dl.core.DLDefaultPartialTensorShape;
import org.knime.dl.core.DLDefaultTensorId;
import org.knime.dl.core.DLDefaultTensorSpec;
import org.knime.dl.core.DLDimensionOrder;
import org.knime.dl.core.DLTensor;
import org.knime.dl.core.DLTensorId;
import org.knime.dl.core.DLTensorShape;
import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.core.DLUnknownTensorShape;

/**
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public final class DLTestUtil {

	public static final double DOUBLE_EPSILON = 1e-6;
	public static final float FLOAT_EPSILON = 1e-6f;

	public static final DLTestingTensorFactory TENSOR_FACTORY = new DLTestingTensorFactory();

	private static final Class<?>[] ELEMENT_TYPES = new Class<?>[] { //
			boolean.class, //
			byte.class, //
			short.class, //
			int.class, //
			long.class, //
			float.class, //
			double.class, //
			String.class //
	};

	private static final DLDimensionOrder[] DIMENSION_ORDERS = DLDefaultDimensionOrder.values();

	private static final int MAX_SHAPE_DIMENSIONALITY = 5;
	private static final int MAX_SHAPE_DIMENSION = 2000;

	private DLTestUtil() {
		// utility class
	}

	// Array utils:

	public static double[] doubleRange(final int length) {
		final double[] array = new double[length];
		for (int i = 0; i < length; i++) {
			array[i] = i;
		}
		return array;
	}

	public static int[] intRange(final int length) {
		final int[] array = new int[length];
		for (int i = 0; i < length; i++) {
			array[i] = i;
		}
		return array;
	}

	public static byte[] byteRange(final int length) {
		final byte[] array = new byte[length];
		for (int i = 0; i < length; i++) {
			array[i] = (byte) i;
		}
		return array;
	}

	public static short[] shortRange(final int length) {
		final short[] array = new short[length];
		for (int i = 0; i < length; i++) {
			array[i] = (short) i;
		}
		return array;
	}

	public static long[] longRange(final int length) {
		final long[] array = new long[length];
		for (int i = 0; i < length; i++) {
			array[i] = i;
		}
		return array;
	}

	public static float[] floatRange(final int length) {
		final float[] array = new float[length];
		for (int i = 0; i < length; i++) {
			array[i] = i;
		}
		return array;
	}

	public static boolean[] alternatingBooleanArray(final int length) {
		final boolean[] array = new boolean[length];
		for (int i = 0; i < length; i++) {
			array[i] = i % 2 == 0;
		}
		return array;
	}

	public static double[] toDouble(final boolean[] array) {
		final double[] result = new double[array.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = array[i] ? 1.0 : 0.0;
		}
		return result;
	}

	public static double[] toDouble(final int[] array) {
		return Arrays.stream(array).asDoubleStream().toArray();
	}

	public static long[] toLong(final int[] array) {
		return Arrays.stream(array).asLongStream().toArray();
	}

	public static long[] toLong(final boolean[] array) {
		final long[] result = new long[array.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = array[i] ? 1L : 0L;
		}
		return result;
	}

	public static long[] toLong(final byte[] array) {
		final long[] result = new long[array.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = array[i];
		}
		return result;
	}

	public static long[] toLong(final short[] array) {
		final long[] result = new long[array.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = array[i];
		}
		return result;
	}

	public static double[] toDouble(final short[] array) {
		final double[] result = new double[array.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = array[i];
		}
		return result;
	}

	public static double[] toDouble(final byte[] array) {
		final double[] result = new double[array.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = array[i];
		}
		return result;
	}

	public static double[] toDouble(final float[] array) {
		final double[] result = new double[array.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = array[i];
		}
		return result;
	}

	public static float[] toFloat(final int[] array) {
		final float[] result = new float[array.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = array[i];
		}
		return result;
	}

	public static float[] toFloat(final short[] array) {
		final float[] result = new float[array.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = array[i];
		}
		return result;
	}

	public static float[] toFloat(final byte[] array) {
		final float[] result = new float[array.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = array[i];
		}
		return result;
	}

	public static float[] toFloat(final boolean[] array) {
		final float[] result = new float[array.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = array[i] ? 1 : 0;
		}
		return result;
	}

	public static int[] toInt(final boolean[] array) {
		final int[] result = new int[array.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = array[i] ? 1 : 0;
		}
		return result;
	}

	public static int[] toInt(final byte[] array) {
		final int[] result = new int[array.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = array[i];
		}
		return result;
	}

	public static int[] toInt(final short[] array) {
		final int[] result = new int[array.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = array[i];
		}
		return result;
	}

	// Tensor:

	public static DLTensor<?> createTensor(final Class<?> elementType, final long batchSize, final long... shape) {
		final DLDefaultTensorSpec spec = new DLDefaultTensorSpec(new DLDefaultTensorId("input"), "input", batchSize,
				new DLDefaultFixedTensorShape(shape), elementType, DLDefaultDimensionOrder.TCDHW);
		return TENSOR_FACTORY.createWritableTensor(spec);
	}

	// Random specs:

	public static URL randomNetworkSource(final Random random) {
		try {
			return FileUtil.toURL(RandomStringUtils.random(10, 0, 0, true, true, null, random) + ".h5");
		} catch (InvalidPathException | MalformedURLException e) {
			throw new RuntimeException();
		}
	}

	public static DLTensorSpec randomTensorSpec(final Random random) {
		final DLTensorId id = randomTensorId(random);
		final String name = randomTensorName(random);
		final DLTensorShape shape = randomTensorShape(random);
		final Class<?> elementType = randomElementType(random);
		final DLDimensionOrder dimensionOrder = randomDimensionOrder(random);
		if (random.nextBoolean()) {
			final long batchSize = random.nextInt(Integer.MAX_VALUE);
			return new DLDefaultTensorSpec(id, name, batchSize, shape, elementType, dimensionOrder);
		} else {
			return new DLDefaultTensorSpec(id, name, shape, elementType, dimensionOrder);
		}
	}

	public static DLTensorId randomTensorId(final Random random) {
		return new DLDefaultTensorId(RandomStringUtils.random(10, 0, 0, true, true, null, random));
	}

	public static String randomTensorName(final Random random) {
		return RandomStringUtils.random(10, 0, 0, true, true, null, random);
	}

	public static DLTensorShape randomTensorShape(final Random random) {
		final int choice = random.nextInt(3);
		final int numDimensions;
		switch (choice) {
		case 0:
			numDimensions = random.nextInt(MAX_SHAPE_DIMENSIONALITY) + 1;
			final long[] shape = new long[numDimensions];
			for (int i = 0; i < numDimensions; i++) {
				shape[i] = random.nextInt(MAX_SHAPE_DIMENSION) + 1l;
			}
			return new DLDefaultFixedTensorShape(shape);
		case 1:
			numDimensions = random.nextInt(MAX_SHAPE_DIMENSIONALITY) + 1;
			final OptionalLong[] partialShape = new OptionalLong[numDimensions];
			boolean hasUnknownDim = false;
			for (int i = 0; i < numDimensions; i++) {
				if (random.nextBoolean()) {
					partialShape[i] = OptionalLong.of(random.nextInt(MAX_SHAPE_DIMENSION) + 1l);
				} else {
					partialShape[i] = OptionalLong.empty();
					hasUnknownDim = true;
				}
			}
			if (!hasUnknownDim) {
				partialShape[random.nextInt(numDimensions)] = OptionalLong.empty();
			}
			return new DLDefaultPartialTensorShape(partialShape);
		case 2:
			return DLUnknownTensorShape.INSTANCE;
		default:
			throw new RuntimeException();
		}
	}

	public static Class<?> randomElementType(final Random random) {
		return ELEMENT_TYPES[random.nextInt(ELEMENT_TYPES.length)];
	}

	public static DLDimensionOrder randomDimensionOrder(final Random random) {
		// TODO: make more diverse? i.e. return random permutation of all available DLDimensions?
		return DIMENSION_ORDERS[random.nextInt(DIMENSION_ORDERS.length)];
	}
}
