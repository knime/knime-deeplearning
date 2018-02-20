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
package org.knime.dl.core;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public class DLDefaultDimensionOrderTest {

	private static DLDimension[] createDimensionArrayFromString(String abbreviated) {
		DLDimension[] dimensions = new DLDimension[abbreviated.length()];
		for (int i = 0; i < dimensions.length; i++) {
			dimensions[i] = getDimensionForChar(abbreviated.charAt(i));
		}
		return dimensions;
	}
		
	private static DLDimension getDimensionForChar(char c) {
		switch (c) {
		case ('T'): return DLDimension.Time;
		case ('D'): return DLDimension.Depth;
		case ('H'): return DLDimension.Height;
		case ('W'): return DLDimension.Width;
		case ('C'): return DLDimension.Channel;
		}
		throw new IllegalArgumentException("Unknown dimension abbreviation '" + c);
	}
	
	@Test
	public void testInferMappingTDHWC() throws Exception {
		DLDimensionOrder dimOrder = DLDefaultDimensionOrder.TDHWC;
		assertArrayEquals(new int[] {0, 1, 2, 3, 4}, dimOrder.inferMappingFor(createDimensionArrayFromString("TDHWC")));
		assertArrayEquals(new int[] {0, 2, 3, 4, 1}, dimOrder.inferMappingFor(createDimensionArrayFromString("TCDHW")));
		assertArrayEquals(new int[] {0, 1, 2}, dimOrder.inferMappingFor(createDimensionArrayFromString("HWC")));
		assertArrayEquals(new int[] {1, 2, 0}, dimOrder.inferMappingFor(createDimensionArrayFromString("CHW")));
		assertArrayEquals(new int[] {0, 2, 3, 1}, dimOrder.inferMappingFor(createDimensionArrayFromString("TCHW")));
	}
	
	@Test
	public void testInferMappingTCDHW() throws Exception {
		DLDimensionOrder dimOrder = DLDefaultDimensionOrder.TCDHW;
		assertArrayEquals(new int[] {0, 1, 2, 3, 4}, dimOrder.inferMappingFor(createDimensionArrayFromString("TCDHW")));
		assertArrayEquals(new int[] {0, 4, 1, 2, 3}, dimOrder.inferMappingFor(createDimensionArrayFromString("TDHWC")));
		assertArrayEquals(new int[] {2, 0, 1}, dimOrder.inferMappingFor(createDimensionArrayFromString("HWC")));
		assertArrayEquals(new int[] {0, 1, 2}, dimOrder.inferMappingFor(createDimensionArrayFromString("CHW")));
	}
	
}
