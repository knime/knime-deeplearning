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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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

import org.knime.dl.core.DLDefaultFixedTensorShape;
import org.knime.dl.core.DLDefaultTensorSpec;
import org.knime.dl.core.DLTensorShape;
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
			return spec.getNumberColumns() == 4 //
					&& colNames[0].equals("name") //
					&& colNames[1].equals("batch_size") //
					&& colNames[2].equals("shape") //
					&& colNames[3].equals("type") //
					&& colTypes[0].equals(Type.STRING) //
					&& (colTypes[1].equals(Type.LONG) //
							|| colTypes[1].equals(Type.INTEGER) //
							|| colTypes[1].equals(Type.DOUBLE) //
							|| colTypes[1].equals(Type.STRING) /*
																 * TODO: we should only allow long/integer and force
																 * Python to comply
																 */) //
					&& (colTypes[2].equals(Type.LONG_LIST) //
							|| colTypes[2].equals(Type.INTEGER_LIST)) //
					&& colTypes[3].equals(Type.STRING);
		}

		private final DLDefaultTensorSpec[] m_layerDataSpecs;

		private int m_nextIdx;

		private final TableSpec m_tableSpec;

		private final DLPythonTypeMap m_typeMap;

		public DLKerasTensorSpecTableCreator(final TableSpec spec, final int tableSize, final DLPythonTypeMap typeMap) {
			if (!checkTableSpec(spec)) {
				throw new IllegalStateException("Python side sent an invalid tensor specs table.");
			}
			m_layerDataSpecs = new DLDefaultTensorSpec[tableSize];
			m_nextIdx = 0;
			m_tableSpec = spec;
			m_typeMap = typeMap;
		}

		@Override
		public synchronized void addRow(final Row row) {
			final String layerDataName = row.getCell(0).getStringValue();
			long layerBatchSize = -1;
			if (!row.getCell(1).isMissing()) {
				if (row.getCell(1).getColumnType().equals(Type.LONG)) {
					layerBatchSize = row.getCell(1).getLongValue();
				} else if (row.getCell(1).getColumnType().equals(Type.INTEGER)) {
					layerBatchSize = row.getCell(1).getIntegerValue();
				} else if (row.getCell(1).getColumnType().equals(Type.DOUBLE)) {
					layerBatchSize = (long) (row.getCell(1).getDoubleValue() + 0.5);
				} else if (row.getCell(1).getColumnType().equals(Type.STRING)) {
					layerBatchSize = Long.parseLong(row.getCell(1).getStringValue());
				}
			}
			DLTensorShape layerDataShape = null;
			if (!row.getCell(2).isMissing()) {
				if (row.getCell(2).getColumnType().equals(Type.LONG_LIST)) {
					try {
						layerDataShape = new DLDefaultFixedTensorShape(row.getCell(2).getLongArrayValue());
					} catch (final NullPointerException ex) {
						// shape stays null
					}
				} else if (row.getCell(2).getColumnType().equals(Type.INTEGER_LIST)) {
					final int[] layerDataShapeInt = row.getCell(2).getIntegerArrayValue();
					final long[] shape = new long[layerDataShapeInt.length];
					if (!row.getCell(2).isMissing(0)) {
						for (int i = 0; i < layerDataShapeInt.length; i++) {
							shape[i] = layerDataShapeInt[i];
						}
						layerDataShape = new DLDefaultFixedTensorShape(shape);
					}
				}
			}
			final Class<?> layerDataType = m_typeMap.getPreferredInternalType(row.getCell(3).getStringValue());
			// TODO: a builder would be nice
			final DLDefaultTensorSpec spec;
			if (layerBatchSize > 0) {
				if (layerDataShape != null) {
					spec = new DLDefaultTensorSpec(layerDataName, layerBatchSize, layerDataShape, layerDataType);
				} else {
					spec = new DLDefaultTensorSpec(layerDataName, layerBatchSize, layerDataType);
				}
			} else {
				if (layerDataShape != null) {
					spec = new DLDefaultTensorSpec(layerDataName, layerDataShape, layerDataType);
				} else {
					spec = new DLDefaultTensorSpec(layerDataName, layerDataType);
				}
			}
			m_layerDataSpecs[m_nextIdx++] = spec;
		}

		@Override
		public TableSpec getTableSpec() {
			return m_tableSpec;
		}

		@Override
		public DLDefaultTensorSpec[] getTable() {
			return m_layerDataSpecs;
		}
	}
}
