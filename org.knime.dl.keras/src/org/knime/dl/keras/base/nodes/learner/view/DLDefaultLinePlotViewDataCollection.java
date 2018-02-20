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
package org.knime.dl.keras.base.nodes.learner.view;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;
import java.util.Iterator;

/**
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author David Kolb, KNIME GmbH, Konstanz, Germany
 */
public final class DLDefaultLinePlotViewDataCollection<S extends DLLinePlotViewSpec>
		implements DLLinePlotViewDataCollection {

	private final S m_spec;

	private DLLinePlotViewData[] m_viewData;

	/**
	 * @param viewData current limitation: array can only contain elements of types {@link DLDenseLinePlotViewData} and
	 *            {@link DLSparseLinePlotViewData}
	 */
	public DLDefaultLinePlotViewDataCollection(final S spec, final DLLinePlotViewData... viewData) {
		checkArgument(checkNotNull(spec).numPlots() == checkNotNull(viewData).length);
		m_spec = spec;
		m_viewData = viewData;
	}

	/**
	 * Deserialization constructor. Must not be used for other purposes.
	 */
	public DLDefaultLinePlotViewDataCollection(final S spec) {
		m_spec = spec;
	}

	@Override
	public S getSpec() {
		return m_spec;
	}

	@Override
	public DLLinePlotViewData get(final int index) {
		return m_viewData[index];
	}

	@Override
	public Iterator<DLLinePlotViewData> iterator() {
		return Arrays.stream(m_viewData).iterator();
	}

	@Override
	public void writeExternal(final ObjectOutput objOut) throws IOException {
		objOut.writeInt(m_viewData.length);
		for (int i = 0; i < m_viewData.length; i++) {
			objOut.writeBoolean(m_viewData[i].getClass() == DLDenseLinePlotViewData.class);
			m_viewData[i].writeExternal(objOut);
		}
	}

	@Override
	public void readExternal(final ObjectInput objIn) throws IOException, ClassNotFoundException {
		m_viewData = new DLLinePlotViewData[objIn.readInt()];
		for (int i = 0; i < m_viewData.length; i++) {
			final DLLinePlotViewData viewData = objIn.readBoolean() ? new DLDenseLinePlotViewData()
					: new DLSparseLinePlotViewData();
			viewData.readExternal(objIn);
			m_viewData[i] = viewData;
		}
	}
}
