package org.knime.dl.keras.base.nodes.learner.view.jfreechart;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Stroke;

import javax.swing.JPanel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class JFreeChartLinePlotPanel extends JPanel {

	private static final long serialVersionUID = 1L;
	
	private final static int LINE_STROKE = 2;
	
	private DLJFreeChartLinePlotViewSpec m_spec;
	private ChartPanel m_chartPanel;
	private XYSeriesCollection m_dataset;

	public JFreeChartLinePlotPanel(DLJFreeChartLinePlotViewSpec spec) {
		m_spec = spec;
		add(getComponent());
	}

	private XYSeriesCollection createDataset(DLJFreeChartLinePlotViewSpec spec) {
		XYSeriesCollection lines = new XYSeriesCollection();
		for (int i = 0; i < spec.numPlots(); i++) {
			lines.addSeries(new XYSeries(spec.getLineLabel(i)));
		}
		return lines;
	}

	public Component getComponent() {
		if (m_chartPanel == null) {
			JFreeChart lineChart = ChartFactory.createXYLineChart(m_spec.title(), m_spec.labelX(), m_spec.labelY(),
					m_dataset = createDataset(m_spec), PlotOrientation.VERTICAL, true, true, false);
			XYPlot plot = (XYPlot) lineChart.getPlot();
			plot.setBackgroundPaint(Color.WHITE);
			plot.setDomainGridlinePaint(Color.WHITE);
			plot.setRangeGridlinePaint(Color.WHITE);
			plot.setOutlineVisible(false);
			lineChart.getLegend().setFrame(BlockBorder.NONE);
			Font labelFont = lineChart.getLegend().getItemFont();
			lineChart.getLegend().setItemFont(new Font(labelFont.getName(), labelFont.getStyle(), 15));

			Stroke defaultStroke = new BasicStroke(LINE_STROKE, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
			for (int i = 0; i < m_dataset.getSeriesCount(); i++) {
				plot.getRenderer().setSeriesStroke(i, defaultStroke);
			}

			plot.getRenderer().setBaseToolTipGenerator(new XYToolTipGenerator() {
				@Override
				public String generateToolTip(XYDataset dataset, int arg1, int arg2) {
					Number x = dataset.getX(arg1, arg2);
					Number y = dataset.getY(arg1, arg2);

					StringBuilder stringBuilder = new StringBuilder();
					stringBuilder.append(String.format("<html>%s <br/>", m_spec.getLineLabel(arg1)));
					stringBuilder.append(String.format("%s: %s <br/>", m_spec.labelY(), y.floatValue()));
					stringBuilder.append(String.format("%s: %s ", m_spec.labelX(), x.floatValue()));
					stringBuilder.append("</html>");
					return stringBuilder.toString();
				}
			});

			m_chartPanel = new ChartPanel(lineChart);
			m_chartPanel.setPreferredSize(new Dimension(800, 500));
			m_chartPanel.setMinimumSize(new Dimension(800, 500));

			m_chartPanel.setInitialDelay(0);
			m_chartPanel.setReshowDelay(0);
			// We do not want the tooltips to go away automatically, so set to
			// high value
			m_chartPanel.setDismissDelay(1000000);
		}
		return m_chartPanel;

	}

	/**
	 * Plots the specified value to the line with the specified index.
	 * 
	 * @param plotIndex the index of the line to plot to
	 * @param value the value to plot
	 */
	public void plotNext(int plotIndex, float value) {
		final XYSeries line = (XYSeries) m_dataset.getSeries().get(plotIndex);
		final int total = line.getItemCount();
		line.add(total + 1, value);
	}
}
