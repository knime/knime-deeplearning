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

import java.util.List;
import java.util.OptionalLong;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.data.def.IntCell;
import org.knime.core.node.util.CheckUtils;
import org.knime.dl.core.DLInvalidNetworkInputException;
import org.knime.dl.core.DLTensor;
import org.knime.dl.core.DLTensorShape;
import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.core.data.DLWritableFloatBuffer;
import org.knime.dl.util.DLUtils;

/**
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public class DLIntCollectionValueToOneHotFloatTensorConverterFactory
	extends DLAbstractTensorDataValueToTensorConverterFactory<CollectionDataValue, DLWritableFloatBuffer> {

	private static final String NAME = "Collection of Number (integer) to One-Hot Tensor";
	
	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public Class<CollectionDataValue> getSourceType() {
		return CollectionDataValue.class;
	}

	@Override
	public Class<DLWritableFloatBuffer> getBufferType() {
		return DLWritableFloatBuffer.class;
	}

	@Override
	public OptionalLong getDestCount(List<DataColumnSpec> spec) {
		return OptionalLong.empty();
	}

	@Override
	public DLDataValueToTensorConverter<CollectionDataValue, DLWritableFloatBuffer> createConverter() {
		return new DLAbstractTensorDataValueToTensorConverter<CollectionDataValue, DLWritableFloatBuffer>() {

			@Override
			protected void convertInternal(CollectionDataValue element, DLTensor<DLWritableFloatBuffer> output) {
				checkType(element.getElementType());
				DLWritableFloatBuffer buffer = output.getBuffer();
				int featureDimSize = getFeatureDimSize(output.getSpec());
				byte[] dummyVector = new byte[featureDimSize];
				for (DataCell cell : element) {
					int index = ((IntCell)cell).getIntValue();
					checkIndexValid(index, featureDimSize);
					dummyVector[index] = 1;
					buffer.putAll(dummyVector);
					dummyVector[index] = 0;
				}
			}
		};
	}
	
	private int getFeatureDimSize(DLTensorSpec tensorSpec) {
		DLTensorShape shape = tensorSpec.getShape();
		// in case of 2D time series, the feature dimension is always the last one
		long featureDimSize = DLUtils.Shapes.getDimSize(shape, shape.getNumDimensions() - 1)
				.orElseThrow(() -> new DLInvalidNetworkInputException(
						"The feature dimension must be known to do the conversion."));
		if (featureDimSize > Integer.MAX_VALUE) {
			// might change in the future
			throw new DLInvalidNetworkInputException(
					"Currently are only feature dimensions of size up to Integer.MAX_VALUE supported.");
		}
		return (int) featureDimSize;
	}
	
	private void checkType(DataType type) {
		CheckUtils.checkArgument(type.equals(DataType.getType(IntCell.class)),
				"This converter supports only integer collections");
	}
	
	private void checkIndexValid(int index, long featureDimSize) {
		CheckUtils.checkArgument(index >= 0, "Negative index encountered.");
		CheckUtils.checkArgument(index < featureDimSize,
				"The index %s exceeds the size of the feature dimension %s.",
				index, featureDimSize);
	}
	
	@Override
	protected long[] getDataShapeInternal(CollectionDataValue element, DLTensorSpec tensorSpec) {
		checkType(element.getElementType());
		long featureDimSize = getFeatureDimSize(tensorSpec);
		for (DataCell cell : element) {
			IntCell intCell = (IntCell) cell;
			checkIndexValid(intCell.getIntValue(), featureDimSize);
		}
		return new long[] {element.size(), featureDimSize};
	}

}
