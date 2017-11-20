package org.knime.dl.keras.base.nodes.learner.view.jfreechart;

import java.awt.Color;
import java.awt.Component;
import java.util.Iterator;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.knime.dl.keras.base.nodes.learner.view.DLFloatData;
import org.knime.dl.keras.base.nodes.learner.view.DLLinePlotViewData;
import org.knime.dl.keras.base.nodes.learner.view.DLView;

public class DLJFreeChartLinePlotView implements DLView<DLLinePlotViewData<DLJFreeChartLinePlotViewSpec>> {

	private DLJFreeChartLinePlotViewSpec m_spec;
	private ChartPanel m_chartPanel;
	private XYSeriesCollection m_dataset;

	public DLJFreeChartLinePlotView(DLJFreeChartLinePlotViewSpec spec) {
		m_spec = spec;
	}

	private XYSeriesCollection createDataset(DLJFreeChartLinePlotViewSpec spec) {
		XYSeriesCollection lines = new XYSeriesCollection();
		for (int i = 0; i < spec.numPlots(); i++) {
			lines.addSeries(new XYSeries(spec.getLineLabel(i)));
		}
		return lines;
	}

	@Override
	public Component getComponent() {
		if (m_chartPanel == null) {
			JFreeChart lineChart = ChartFactory.createXYLineChart(m_spec.title(), m_spec.labelY(), m_spec.labelX(),
					m_dataset = createDataset(m_spec), PlotOrientation.VERTICAL, true, true, false);
			lineChart.setBackgroundPaint(Color.white);
			m_chartPanel = new ChartPanel(lineChart);
			m_chartPanel.setPreferredSize(new java.awt.Dimension(560, 367));
		}
		return m_chartPanel;

	}

	@Override
	public void update(DLLinePlotViewData<DLJFreeChartLinePlotViewSpec> data) {
		for (int i = 0; i < data.getViewSpec().numPlots(); i++) {
			final XYSeries line = (XYSeries) m_dataset.getSeries().get(i);
			Iterator<DLFloatData> it = data.getData(i);
			while (it.hasNext()) {
				plotNext(line, it.next().get());
			}
		}
		m_chartPanel.repaint();
	}
	
	private void plotNext(XYSeries line, float value) {
		final int total = line.getItemCount();
		line.add(total + 1, value);
	}

	public void plotNext(int plotIndex, float value) {
		final XYSeries line = (XYSeries) m_dataset.getSeries().get(plotIndex);
		plotNext(line, value);
		m_chartPanel.repaint();
	}
}
