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

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import org.knime.core.data.DataRow;
import org.knime.core.data.container.CloseableRowIterator;
import org.knime.core.node.BufferedDataTable;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
abstract class DLAbstractDataTableRowIterator extends DLAbstractRowIterator {

	private final BufferedDataTable m_input;

	private final long m_size;

	protected CloseableRowIterator m_iterator;

	private DataRow m_lastPeeked;

	/**
	 * Subclasses must set the initial iterator in their constructor call.
	 * 
	 * @param input the data table
	 * @param columns a map specifying which columns belong to which tensor
	 */
	protected DLAbstractDataTableRowIterator(final BufferedDataTable input, final Map<DLTensorId, int[]> columns) {
		super(input.getDataTableSpec(), columns);
		m_input = checkNotNull(input);
		m_size = input.size();
		// must be set in subclass
		m_iterator = null;
	}

	@Override
	public final long size() {
		return m_size;
	}

	@Override
	public final boolean hasNext() {
		return m_lastPeeked != null || m_iterator.hasNext();
	}

	@Override
	public final DataRow peek() {
		if (m_lastPeeked == null) {
			m_lastPeeked = m_iterator.next();
		}
		return m_lastPeeked;
	}

	@Override
	public final DataRow next() {
		DataRow nextDataRow;
		if (m_lastPeeked != null) {
			nextDataRow = m_lastPeeked;
			m_lastPeeked = null;
		} else {
			nextDataRow = m_iterator.next();
		}
		return nextDataRow;
	}

	@Override
	public final void reset() {
		m_iterator.close();
		m_lastPeeked = null;
		m_iterator = makeNewIterator();
	}
	
	protected abstract CloseableRowIterator makeNewIterator();
	
	protected final BufferedDataTable getInputTable() {
		return m_input;
	}

	@Override
	public final void close() {
		m_iterator.close();
	}

}
