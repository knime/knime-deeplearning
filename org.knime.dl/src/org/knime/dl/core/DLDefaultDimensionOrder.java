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

import java.util.Arrays;
import java.util.EnumMap;

/**
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public enum DLDefaultDimensionOrder implements DLDimensionOrder {
	
	/**
	 * Stands for Time-Depth-Height-Width-Channel ordering, corresponds to channels_last in Keras (default)
	 */
	TDHWC(new DLDimension[] {DLDimension.Time, DLDimension.Depth, DLDimension.Height, DLDimension.Width, DLDimension.Channel}),
	/**
	 * Stands for Time-Channel-Depth-Height-Width ordering, corresponds to channels_first in Keras.
	 * This is the default ordering used in KNIP.
	 */
	TCDHW(new DLDimension[] {DLDimension.Time, DLDimension.Channel, DLDimension.Depth, DLDimension.Height, DLDimension.Width}),
	/**
	 * In case the dimension order is unknown. This is a special case and has to be handled separately.
	 */
	Unknown(null);
	
	
	private final DLDimension[] m_dimensionOrder;
	private final EnumMap<DLDimension, Integer> m_dimensionMap;
	
	DLDefaultDimensionOrder(final DLDimension[] dimensionOrder) {
		if (dimensionOrder != null) {
			m_dimensionOrder = dimensionOrder;
			m_dimensionMap = createDimensionMap(m_dimensionOrder);
		} else {
			m_dimensionOrder = null;
			m_dimensionMap = null;
		}
	}
	
	private EnumMap<DLDimension, Integer> createDimensionMap(DLDimension[] dimensionOrder) {
		EnumMap<DLDimension, Integer> dimensionMap = new EnumMap<>(DLDimension.class);
		for (int i = 0; i < dimensionOrder.length; i++) {
			dimensionMap.put(dimensionOrder[i], i);
		}
		return dimensionMap;
	}

	@Override
	public DLDimension[] getDimensions() {
		if (this == Unknown) {
			throw new UnsupportedOperationException("The dimension order is unknown.");
		}
		return m_dimensionOrder.clone();
	}
	
	@Override
	public int[] inferMappingFor(DLDimension[] dimensions) {
		if (this == Unknown) {
			throw new UnsupportedOperationException("Can't infer mapping from unknown dimension.");
		}
		int[] holeyMapping = inferHoleyMapping(dimensions);
		return compressHoleyMapping(holeyMapping, dimensions.length);
		
	}
	
	private int[] inferHoleyMapping(DLDimension[] dimensions) {
		int[] holeyMapping = new int[m_dimensionOrder.length];
		Arrays.fill(holeyMapping, -1);
		for (int i = 0; i < dimensions.length; i++) {
			Integer newIdx = m_dimensionMap.get(dimensions[i]);
			if (newIdx == null) {
				// can only happen if the DLDimension enum is extended without extending the arrays in this enum
				throw new IllegalArgumentException("The provided dimension array contains the incompatible dimension '"
						+ dimensions[i] + "'.");
			}
			holeyMapping[newIdx] = i;
		}
		return holeyMapping;
	}
	
	private int[] compressHoleyMapping(int[] holeyMapping, int numDimensions) {
		int[] mapping = new int[numDimensions];
		int counter = 0;
		for (int i = 0; i < holeyMapping.length; i++) {
			if (holeyMapping[i] == -1) {
				continue;
			}
			mapping[counter++] = holeyMapping[i];
		}
		return mapping;
	}
	
}
