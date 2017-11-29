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
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

/**
 * @author David Kolb, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 */
public class JFreeChartLinePlotPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	private static final int LINE_STROKE = 2;

	private final DLJFreeChartLinePlotViewSpec m_spec;

	private ChartPanel m_chartPanel;

	private XYSeriesCollection m_dataset;

	public JFreeChartLinePlotPanel(final DLJFreeChartLinePlotViewSpec spec) {
		m_spec = spec;
		add(getComponent());
	}

	private XYSeriesCollection createDataset(final DLJFreeChartLinePlotViewSpec spec) {
		final XYSeriesCollection lines = new XYSeriesCollection();
		for (int i = 0; i < spec.numPlots(); i++) {
			lines.addSeries(new XYSeries(spec.getLineLabel(i)));
		}
		return lines;
	}

	public Component getComponent() {
		if (m_chartPanel == null) {
			final JFreeChart lineChart = ChartFactory.createXYLineChart(m_spec.title(), m_spec.labelX(),
					m_spec.labelY(), m_dataset = createDataset(m_spec), PlotOrientation.VERTICAL, true, true, false);
			final XYPlot plot = (XYPlot) lineChart.getPlot();
			plot.setBackgroundPaint(Color.WHITE);
			plot.setDomainGridlinePaint(Color.WHITE);
			plot.setRangeGridlinePaint(Color.WHITE);
			plot.setOutlineVisible(false);
			lineChart.getLegend().setFrame(BlockBorder.NONE);
			final Font labelFont = lineChart.getLegend().getItemFont();
			lineChart.getLegend().setItemFont(new Font(labelFont.getName(), labelFont.getStyle(), 15));

			final Stroke defaultStroke = new BasicStroke(LINE_STROKE, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
			for (int i = 0; i < m_dataset.getSeriesCount(); i++) {
				plot.getRenderer().setSeriesStroke(i, defaultStroke);
			}

			plot.getRenderer().setBaseToolTipGenerator((dataset, arg1, arg2) -> {
				final Number x = dataset.getX(arg1, arg2);
				final Number y = dataset.getY(arg1, arg2);

				final StringBuilder stringBuilder = new StringBuilder();
				stringBuilder.append(String.format("<html>%s <br/>", m_spec.getLineLabel(arg1)));
				stringBuilder.append(String.format("%s: %s <br/>", m_spec.labelY(), y.floatValue()));
				stringBuilder.append(String.format("%s: %s ", m_spec.labelX(), x.floatValue()));
				stringBuilder.append("</html>");
				return stringBuilder.toString();
			});

			m_chartPanel = new ChartPanel(lineChart);
			m_chartPanel.setPreferredSize(new Dimension(800, 500));
			m_chartPanel.setMinimumSize(new Dimension(800, 500));

			m_chartPanel.setInitialDelay(0);
			m_chartPanel.setReshowDelay(0);
			// We do not want the tooltips to go away automatically, so set to high value.
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
	public void plotNext(final int plotIndex, final float value) {
		final XYSeries line = (XYSeries) m_dataset.getSeries().get(plotIndex);
		final int total = line.getItemCount();
		line.add(total + 1, value);
	}
}
