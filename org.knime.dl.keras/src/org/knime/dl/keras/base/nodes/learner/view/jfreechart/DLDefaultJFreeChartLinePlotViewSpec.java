package org.knime.dl.keras.base.nodes.learner.view.jfreechart;

public class DLDefaultJFreeChartLinePlotViewSpec implements DLJFreeChartLinePlotViewSpec {

	private String m_id;
	private String m_title;
	private String m_labelY;
	private String m_labelX;
	private String[] m_lineLabels;

	public DLDefaultJFreeChartLinePlotViewSpec(String id, String title, String labelY, String labelX, String[] lineLabels) {
		m_id = id;
		m_title = title;
		m_labelY = labelY;
		m_labelX = labelX;
		m_lineLabels = lineLabels;
	}

	@Override
	public String id() {
		return m_id;
	}

	@Override
	public String title() {
		return m_title;
	}

	@Override
	public String labelY() {
		return m_labelY;
	}

	@Override
	public String labelX() {
		return m_labelX;
	}

	@Override
	public int numPlots() {
		return m_lineLabels.length;
	}

	@Override
	public String getLineLabel(int i) {
		return m_lineLabels[i];
	}

}
