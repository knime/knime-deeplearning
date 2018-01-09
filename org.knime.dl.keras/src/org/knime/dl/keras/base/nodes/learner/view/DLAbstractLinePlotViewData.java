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

import java.util.Iterator;

/**
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author David Kolb, KNIME GmbH, Konstanz, Germany
 */
public abstract class DLAbstractLinePlotViewData<S extends DLLinePlotViewSpec> implements DLLinePlotViewData<S> {

	protected S m_spec;

	protected float[][] m_data;

	private int m_length;

	protected DLAbstractLinePlotViewData(final S spec, final float[][] data, final int length) {
		m_spec = spec;
		m_data = data;
		m_length = length;
	}

	/**
	 * @return current index of iterator
	 */
	protected int currLength() {
		return m_length;
	}

	protected void incLength() {
		m_length++;
	}

	@Override
	public S getViewSpec() {
		return m_spec;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Iterator<DLFloatData>[] iterators() {
		final Iterator<DLFloatData>[] iterators = new Iterator[m_data.length];
		for (int i = 0; i < iterators.length; i++) {
			iterators[i] = new DLFloatDataIterator(i);
		}
		return iterators;
	}

	@Override
	public float[][] asArray() {
		final float[][] out;
		// all points have been set. no need to worry.
		if (m_length == m_data[0].length) {
			out = m_data;
		} else {
			out = new float[m_data.length][m_length];
			for (int i = 0; i < out.length; i++) {
				System.arraycopy(m_data[i], 0, out[i], 0, m_length);
			}
		}
		return out;
	}

	// simple helper
	class DLFloatDataIterator implements Iterator<DLFloatData> {

		private final DLFloatData m_proxy;

		// global counter
		private int m_idx = -1;

		public DLFloatDataIterator(final int lineIdx) {
			m_proxy = () -> m_data[lineIdx][m_idx];
		}

		@Override
		public boolean hasNext() {
			return m_idx < m_length - 1;
		}

		@Override
		public DLFloatData next() {
			m_idx++;
			return m_proxy;
		}
	}
}
