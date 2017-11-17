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
 */
package org.knime.dl.core;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.RowIterator;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.NodeLogger;
import org.knime.dl.core.execution.DLInvalidNetworkInputException;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public final class DLDefaultRowIterator implements DLRowIterator {

	private final BufferedDataTable m_table;

	private final Map<? extends DLTensorSpec, int[]> m_columns;

	private RowIterator m_iterator;

	private long m_lastIndex = -1;

	private final Map<DLTensorSpec, List<DataValue>> m_temp;

	public DLDefaultRowIterator(final BufferedDataTable table, final Map<? extends DLTensorSpec, int[]> columns) {
		m_table = checkNotNull(table);
		m_columns = new HashMap<>(checkNotNull(columns));
		m_iterator = table.iterator();
		m_temp = new HashMap<>(columns.size());
		for (final Entry<? extends DLTensorSpec, int[]> entry : columns.entrySet()) {
			final int numColumns = entry.getValue().length;
			final ArrayList<DataValue> list = new ArrayList<>(
					Collections.nCopies(numColumns, DataType.getMissingCell()));
			m_temp.put(entry.getKey(), list);
		}
	}

	@Override
	public long size() {
		return m_table.size();
	}

	@Override
	public boolean hasNext() {
		return m_iterator.hasNext();
	}

	@Override
	public Map<DLTensorSpec, List<DataValue>> next() {
		final DataRow row = m_iterator.next();
		m_lastIndex++;
		for (final Entry<? extends DLTensorSpec, int[]> entry : m_columns.entrySet()) {
			final int[] columns = entry.getValue();
			final List<DataValue> list = m_temp.get(entry.getKey());
			for (int i = 0; i < columns.length; i++) {
				final int column = columns[i];
				final DataCell cell = row.getCell(column);
				if (cell.isMissing()) {
					throw new DLInvalidNetworkInputException("Missing cell in input row '" + row.getKey()
							+ "', column '" + m_table.getDataTableSpec().getColumnSpec(column).getName() + "'.");
				}
				list.set(i, cell);
			}
		}
		return m_temp;
	}

	@Override
	public Map<DLTensorSpec, List<DataValue>> get(final long index) {
		if (index - 1 == m_lastIndex) {
			return next();
		}
		if (index == 0) {
			reset();
			return next();
		}
		// TODO: (pseudo) random access
		NodeLogger.getLogger(DLDefaultRowIterator.class).debug(
				"Random access is not yet implemented (and should not be necessary at this point of development, either). "
						+ "This will be a future feature that is needed when caching of input tensors is employed.");
		throw new UnsupportedOperationException("Random access is not yet implemented.");
	}

	@Override
	public void reset() {
		closeCurrentIterator();
		m_iterator = m_table.iterator();
		m_lastIndex = -1;
	}

	@Override
	public void close() {
		closeCurrentIterator();
	}

	private void closeCurrentIterator() {
		if (m_iterator instanceof CloseableRowIterator) {
			((CloseableRowIterator) m_iterator).close();
		}
	}
}
