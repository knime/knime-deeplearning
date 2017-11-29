package org.knime.dl.keras.base.nodes.learner.view;

import java.util.Iterator;

public abstract class DLAbstractLinePlotViewData<S extends DLLinePlotViewSpec> implements DLLinePlotViewData<S> {

	protected S m_spec;
	protected float[][] m_data;
	private int m_length;

	protected DLAbstractLinePlotViewData(final S spec, float[][] data, int length) {
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
		Iterator<DLFloatData>[] iterators = new Iterator[m_data.length];
		for (int i = 0; i < iterators.length; i++) {
			iterators[i] = new DLFloatDataIterator(i);
		}
		return iterators;
	}

	@Override
	public float[][] asArray() {
		// TODO TEST
		final float[][] out;
		// all points have been set. no need to worry.
		if (m_length == m_data[0].length) {
			out = m_data;
		} else {
			out = new float[m_data.length][m_length];
			for (int i = 0; i < out.length; i++) {
				System.arraycopy(m_data[i], 0, out, 0, m_length);
			}
		}
		return out;
	}

	// simple helper
	class DLFloatDataIterator implements Iterator<DLFloatData> {

		private DLFloatData m_proxy;

		// global counter
		private int m_idx = -1;

		public DLFloatDataIterator(int lineIdx) {
			m_proxy = new DLFloatData() {
				public float get() {
					return m_data[lineIdx][m_idx];
				}
			};
		}

		@Override
		public boolean hasNext() {
			return m_idx < m_length-1;
		}

		@Override
		public DLFloatData next() {
			m_idx++;
			return m_proxy;
		}
	}
}
