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
package org.knime.dl.core.data;

import java.util.Arrays;

/**
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class DLTestUtil {
	public static final double EPSILON = 1e-6;
	
	private DLTestUtil() {
		// utility class
	}
	
	public static double[] doubleRange(int length) {
		double[] array = new double[length];
		for (int i = 0; i < length; i++) {
			array[i] = i;
		}
		return array;
	}
	
	public static int[] intRange(int length) {
		int[] array = new int[length];
		for (int i = 0; i < length; i++) {
			array[i] = i;
		}
		return array;
	}
	
	public static byte[] byteRange(int length) {
		byte[] array = new byte[length];
		for (int i = 0; i < length; i++) {
			array[i] = (byte) i;
		}
		return array;
	}
	
	public static short[] shortRange(int length) {
		short[] array = new short[length];
		for (int i = 0; i < length; i++) {
			array[i] = (short) i;
		}
		return array;
	}
	
	public static long[] longRange(int length) {
		long[] array = new long[length];
		for (int i = 0; i < length; i++) {
			array[i] = i;
		}
		return array;
	}
	
	public static float[] floatRange(int length) {
		float[] array = new float[length];
		for (int i = 0; i < length; i++) {
			array[i] = i;
		}
		return array;
	}
	
	public static boolean[] alternatingBooleanArray(int length) {
		boolean[] array = new boolean[length];
		for (int i = 0; i < length; i++) {
			array[i] = i % 2 == 0;
		}
		return array;
	}
	
	public static double[] toDouble(boolean[] array) {
		double[] result = new double[array.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = array[i] ? 1.0 : 0.0;
		}
		return result;
	}
	
	public static double[] toDouble(int[] array) {
		return Arrays.stream(array).asDoubleStream().toArray();
	}
	
	public static double[] toDouble(short[] array) {
		double[] result = new double[array.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = array[i];
		}
		return result;
	}
	
	public static double[] toDouble(byte[] array) {
		double[] result = new double[array.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = array[i];
		}
		return result;
	}
	
	public static double[] toDouble(float[] array) {
		double[] result = new double[array.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = array[i];
		}
		return result;
	}

}
