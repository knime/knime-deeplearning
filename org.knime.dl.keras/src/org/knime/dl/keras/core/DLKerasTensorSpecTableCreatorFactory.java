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
 * History
 *   May 30, 2017 (marcel): created
 */
package org.knime.dl.keras.core;

import java.util.Arrays;
import java.util.OptionalLong;

import org.knime.dl.core.DLDefaultDimensionOrder;
import org.knime.dl.core.DLDefaultFixedTensorShape;
import org.knime.dl.core.DLDefaultPartialTensorShape;
import org.knime.dl.core.DLDefaultTensorId;
import org.knime.dl.core.DLDefaultTensorSpec;
import org.knime.dl.core.DLDimensionOrder;
import org.knime.dl.core.DLTensorId;
import org.knime.dl.core.DLTensorShape;
import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.python.core.data.DLPythonTypeMap;
import org.knime.python2.extensions.serializationlibrary.interfaces.Row;
import org.knime.python2.extensions.serializationlibrary.interfaces.TableCreator;
import org.knime.python2.extensions.serializationlibrary.interfaces.TableCreatorFactory;
import org.knime.python2.extensions.serializationlibrary.interfaces.TableSpec;
import org.knime.python2.extensions.serializationlibrary.interfaces.Type;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public class DLKerasTensorSpecTableCreatorFactory implements TableCreatorFactory {

	private static final int ID_IDX = 0;

	private static final int NAME_IDX = 1;

	private static final int BATCH_SIZE_IDX = 2;

	private static final int SHAPE_IDX = 3;

	private static final int TYPE_IDX = 4;
	
	private static final int DIMENSION_ORDER_IDX = 5;

	private final DLPythonTypeMap m_typeMap;

	public DLKerasTensorSpecTableCreatorFactory(final DLPythonTypeMap typeMap) {
		m_typeMap = typeMap;
	}

	@Override
	public TableCreator<DLDefaultTensorSpec[]> createTableCreator(final TableSpec spec, final int tableSize) {
		return new DLKerasTensorSpecTableCreator(spec, tableSize, m_typeMap);
	}

	public static class DLKerasTensorSpecTableCreator implements TableCreator<DLDefaultTensorSpec[]> {

		private static boolean checkTableSpec(final TableSpec spec) {
			final String[] colNames = spec.getColumnNames();
			final Type[] colTypes = spec.getColumnTypes();
			return spec.getNumberColumns() == 6 //
					&& colNames[ID_IDX].equals("id") //
					&& colNames[NAME_IDX].equals("name") //
					&& colNames[BATCH_SIZE_IDX].equals("batch_size") //
					&& colNames[SHAPE_IDX].equals("shape") //
					&& colNames[TYPE_IDX].equals("type") //
					&& colNames[DIMENSION_ORDER_IDX].equals("dimension_order") //
					&& colTypes[ID_IDX].equals(Type.STRING) //
					&& colTypes[NAME_IDX].equals(Type.STRING) //
					&& (colTypes[BATCH_SIZE_IDX].equals(Type.LONG) //
							|| colTypes[BATCH_SIZE_IDX].equals(Type.INTEGER) //
							|| colTypes[BATCH_SIZE_IDX].equals(Type.DOUBLE) //
							|| colTypes[BATCH_SIZE_IDX].equals(Type.STRING) /*
																			 * TODO: we should only allow long/integer
																			 * and force Python to comply 
																			 */) //
					&& (colTypes[SHAPE_IDX].equals(Type.LONG_LIST) //
							|| colTypes[SHAPE_IDX].equals(Type.INTEGER_LIST)) //
					&& colTypes[TYPE_IDX].equals(Type.STRING)
					&& colTypes[DIMENSION_ORDER_IDX].equals(Type.STRING);
		}

		private final DLDefaultTensorSpec[] m_tensorSpecs;

		private int m_nextIdx;

		private final TableSpec m_tableSpec;

		private final DLPythonTypeMap m_typeMap;

		public DLKerasTensorSpecTableCreator(final TableSpec spec, final int tableSize, final DLPythonTypeMap typeMap) {
			if (!checkTableSpec(spec)) {
				throw new IllegalStateException("Python side sent an invalid tensor specs table.");
			}
			m_tensorSpecs = new DLDefaultTensorSpec[tableSize];
			m_nextIdx = 0;
			m_tableSpec = spec;
			m_typeMap = typeMap;
		}

		@Override
		public synchronized void addRow(final Row row) {
			
			m_tensorSpecs[m_nextIdx++] = parseTensorSpec(row);
		}
		
		private DLDefaultTensorSpec parseTensorSpec(final Row row) {
			final DLTensorId id = new DLDefaultTensorId(row.getCell(ID_IDX).getStringValue());
			final String name = row.getCell(NAME_IDX).getStringValue();
			final Class<?> type = m_typeMap.getPreferredInternalType(row.getCell(TYPE_IDX).getStringValue());
			final DLDimensionOrder dimensionOrder = DLDefaultDimensionOrder.valueOf(
					row.getCell(DIMENSION_ORDER_IDX).getStringValue());
			long batchSize = getBatchSize(row);
			DLTensorShape shape = getTensorShape(row);
			return createTensorSpec(id, name, batchSize, shape, type, dimensionOrder);
		}
		
		private static DLDefaultTensorSpec createTensorSpec(DLTensorId id, String name, long batchSize,
				DLTensorShape shape, Class<?> type, DLDimensionOrder dimensionOrder) {
			if (batchSize > 0) {
				if (shape != null) {
					return new DLDefaultTensorSpec(
							id, name, batchSize, shape, type, dimensionOrder);
				} else {
					return new DLDefaultTensorSpec(id, name, batchSize, type, dimensionOrder);
				}
			} else {
				if (shape != null) {
					return new DLDefaultTensorSpec(id, name, shape, type, dimensionOrder);
				} else {
					return new DLDefaultTensorSpec(id, name, type, dimensionOrder);
				}
			}
		}
		
		private long getBatchSize(final Row row) {
			long batchSize = -1;
			if (!row.getCell(BATCH_SIZE_IDX).isMissing()) {
				if (row.getCell(BATCH_SIZE_IDX).getColumnType().equals(Type.LONG)) {
					batchSize = row.getCell(BATCH_SIZE_IDX).getLongValue();
				} else if (row.getCell(BATCH_SIZE_IDX).getColumnType().equals(Type.INTEGER)) {
					batchSize = row.getCell(BATCH_SIZE_IDX).getIntegerValue();
				} else if (row.getCell(BATCH_SIZE_IDX).getColumnType().equals(Type.DOUBLE)) {
					batchSize = (long) (row.getCell(BATCH_SIZE_IDX).getDoubleValue() + 0.5);
				} else if (row.getCell(BATCH_SIZE_IDX).getColumnType().equals(Type.STRING)) {
					batchSize = Long.parseLong(row.getCell(BATCH_SIZE_IDX).getStringValue());
				}
			}
			return batchSize;
		}
		
		private DLTensorShape getTensorShape(final Row row) {
			DLTensorShape tensorShape = null;
			if (!row.getCell(SHAPE_IDX).isMissing()) {
				if (row.getCell(SHAPE_IDX).getColumnType().equals(Type.LONG_LIST)) {
					try {
						tensorShape = createShape(row.getCell(SHAPE_IDX).getLongArrayValue());
					} catch (final NullPointerException ex) {
						// shape stays null
					}
				} else if (row.getCell(SHAPE_IDX).getColumnType().equals(Type.INTEGER_LIST)) {
					final int[] tensorShapeInt = row.getCell(SHAPE_IDX).getIntegerArrayValue();
					tensorShape = createShape(Arrays.stream(tensorShapeInt).mapToLong(i -> i).toArray());
				}
			}
			return tensorShape;
		}
		
		@Override
		public TableSpec getTableSpec() {
			return m_tableSpec;
		}

		@Override
		public DLDefaultTensorSpec[] getTable() {
			return m_tensorSpecs;
		}

		private DLTensorShape createShape(final long[] shape) {
			if (Arrays.stream(shape).allMatch(d -> d != -1L)) {
				return new DLDefaultFixedTensorShape(shape);
			}
			return new DLDefaultPartialTensorShape(
					Arrays.stream(shape).mapToObj(d -> d == -1 ? OptionalLong.empty() : OptionalLong.of(d))
							.toArray(i -> new OptionalLong[i]));
		}
	}
}
