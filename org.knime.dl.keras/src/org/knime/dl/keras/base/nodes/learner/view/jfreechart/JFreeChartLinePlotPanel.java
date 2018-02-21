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
package org.knime.dl.keras.base.nodes.learner.view.jfreechart;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Stroke;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

/**
 * @author David Kolb, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 */
public class JFreeChartLinePlotPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	/*
	 * Color list to use for plots, starting with the first one. If the number of plots in on chart exceeds the number
	 * of colors defined here we will start with the first color again. See getNextColor().
	 */
	private static final List<Color> LINE_COLORS = Collections.unmodifiableList(
			Arrays.asList(Color.RED, Color.BLUE, Color.GREEN, Color.CYAN, Color.MAGENTA, Color.ORANGE));

	private static final String SMOOTHED_LINE_KEY_SUFFIX = "(smoothed)";

	public static final double SMOOTHING_ALPHA_DEFAULT = 0.05;

	/* Global line width of all plots */
	private static final int LINE_STROKE = 2;

	private final DLJFreeChartLinePlotViewSpec m_spec;

	private ChartPanel m_chartPanel;

	private JFreeChart m_lineChart;

	private XYSeriesCollection m_dataset;

	private final Map<Integer, String> m_lineIndexToLineLabel = new HashMap<>();

	private final Map<String, Integer> m_lineLabelToLineIndex = new HashMap<>();

	private final Map<String, Color> m_lineReferenceColors = new HashMap<>();

	private final Map<String, AtomicBoolean> m_smoothedLineOutdated = new HashMap<>();

	private Map<String, ExponentialSmoothingIterator> m_smoothingIters;

	private XYPlot m_plot;

	private boolean m_smoothedLinesEnabled = false;

	private double m_smoothingAlpha = SMOOTHING_ALPHA_DEFAULT;

	private int m_colorIdx = 0;

	public JFreeChartLinePlotPanel(final DLJFreeChartLinePlotViewSpec spec) {
		super(new GridBagLayout());
		m_spec = spec;

		final GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weighty = 1;
		gbc.weightx = 1;

		add(getChartPanel(), gbc);
	}

	/**
	 * Add plots to dataset. For each plot a smooth version is added with the
	 * {@link JFreeChartLinePlotPanel#SMOOTHED_LINE_KEY_SUFFIX} added. Also initialized indexToLineLabel and
	 * lineLabelToIndex maps.
	 *
	 * @return the dataset containing the plots
	 */
	private XYSeriesCollection createDataset() {
		final XYSeriesCollection lines = new XYSeriesCollection();
		int lineCounter = 0;
		for (int i = 0; i < m_spec.numPlots(); i++) {
			final String lineLabel = m_spec.getLineLabel(i);
			final XYSeries line = new XYSeries(lineLabel);
			lines.addSeries(line);
			m_lineIndexToLineLabel.put(lineCounter, lineLabel);
			m_lineLabelToLineIndex.put(lineLabel, lineCounter);
			lineCounter++;

			final String smoothedLineLabel = lineLabel + SMOOTHED_LINE_KEY_SUFFIX;
			lines.addSeries(new XYSeries(smoothedLineLabel));
			m_lineIndexToLineLabel.put(lineCounter, smoothedLineLabel);
			m_lineLabelToLineIndex.put(smoothedLineLabel, lineCounter);
			m_smoothedLineOutdated.put(smoothedLineLabel, new AtomicBoolean());
			lineCounter++;
		}
		return lines;
	}

	/**
	 * Creates new smoothing iterators with the specified smoothing factor.
	 *
	 * @param smoothingAlpha
	 */
	private void initSmoothingIter(final String lineLabel, final double smoothingAlpha) {
		if (m_smoothingIters == null) {
			m_smoothingIters = new HashMap<>();
		}
		m_smoothingIters.put(lineLabel + SMOOTHED_LINE_KEY_SUFFIX,
				new ExponentialSmoothingIterator(m_dataset.getSeries(lineLabel), smoothingAlpha));
	}

	private ChartPanel getChartPanel() {
		if (m_chartPanel == null) {
			m_lineChart = ChartFactory.createXYLineChart(m_spec.title(), m_spec.labelX(), m_spec.labelY(),
					m_dataset = createDataset(), PlotOrientation.VERTICAL, true, true, false);

			for (int i = 0; i < m_spec.numPlots(); i++) {
				initSmoothingIter(m_spec.getLineLabel(i), m_smoothingAlpha);
			}

			// Remove the chart title
			m_lineChart.setTitle("");

			m_plot = (XYPlot) m_lineChart.getPlot();
			m_plot.setBackgroundPaint(Color.WHITE);
			m_plot.setDomainGridlinePaint(Color.WHITE);
			m_plot.setRangeGridlinePaint(Color.WHITE);
			m_plot.setOutlineVisible(false);
			m_lineChart.getLegend().setFrame(BlockBorder.NONE);
			final Font labelFont = m_lineChart.getLegend().getItemFont();
			m_lineChart.getLegend().setItemFont(new Font(labelFont.getName(), labelFont.getStyle(), 15));

			final Stroke defaultStroke = new BasicStroke(LINE_STROKE, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
			for (int i = 0; i < m_dataset.getSeriesCount(); i++) {
				m_plot.getRenderer().setSeriesStroke(i, defaultStroke);
			}

			m_plot.getRenderer().setBaseToolTipGenerator((dataset, arg1, arg2) -> {
				final Number x = dataset.getX(arg1, arg2);
				final Number y = dataset.getY(arg1, arg2);

				final StringBuilder stringBuilder = new StringBuilder();
				stringBuilder.append(String.format("<html>%s <br/>", m_lineIndexToLineLabel.get(arg1)));
				stringBuilder.append(String.format("%s: %s <br/>", m_spec.labelY(), y.floatValue()));
				stringBuilder.append(String.format("%s: %s ", m_spec.labelX(), x.floatValue()));
				stringBuilder.append("</html>");
				return stringBuilder.toString();
			});

			m_chartPanel = new ChartPanel(m_lineChart, true, true, false, false, true);
			m_chartPanel.setPreferredSize(new Dimension(1000, 500));
			m_chartPanel.setMinimumSize(new Dimension(1000, 500));
			m_chartPanel.setInitialDelay(0);
			m_chartPanel.setReshowDelay(0);
			// We do not want the tooltips to go away automatically, so set to
			// high value.
			m_chartPanel.setDismissDelay(1000000);

			// After the size defined here is exceeded, the plot will not be
			// redrawn but rescaled. Therefore, set to something high
			// to avoid stretching of the plot labels.
			m_chartPanel.setMaximumDrawWidth(2000);
			m_chartPanel.setMaximumDrawHeight(2000);

			// Update the line style to default, no transparency for raw plot
			for (int i = 0; i < m_spec.numPlots(); i++) {
				updateLineStyle(m_spec.getLineLabel(i), false);
			}
		}
		return m_chartPanel;
	}

	/**
	 * Plots the specified value to the line with the specified label.
	 *
	 * @param lineLabel the label of the line to plot to
	 * @param valueX the x-value to plot
	 * @param valueY the y-value to plot
	 */
	public void plotNext(final String lineLabel, final int valueX, final float valueY) {
		// All updates of the lines need to happen in the EDT
		SwingUtilities.invokeLater(() -> {
			final XYSeries line = m_dataset.getSeries(lineLabel);
			// TODO: we need to differentiate between line plots and scatter plots somewhere
			line.add(valueX, valueY);
			plotSmoothed(lineLabel);
		});
	}

	/**
	 * Trigger a redraw of the smoothed lines. This will only happen if smoothed lines are enabled and the smoothing
	 * alpha changed.
	 */
	public void triggerSmoothedLinesUpdate() {
		// All updates of the lines need to happen in the EDT
		SwingUtilities.invokeLater(() -> {
			for (int i = 0; i < m_spec.numPlots(); i++) {
				plotSmoothed(m_spec.getLineLabel(i));
			}
		});
	}

	/**
	 * Set smoothing factor for smoothed lines. If called while the plot is currently updating, the smoothed line will
	 * be drawn automatically. Otherwise, call {@link JFreeChartLinePlotPanel#triggerSmoothedLinesUpdate()} to force an
	 * update.
	 *
	 * @param smoothingAlpha
	 */
	public void setSmoothingAlpha(final double smoothingAlpha) {
		if (smoothingAlpha != m_smoothingAlpha) {
			m_smoothingAlpha = smoothingAlpha;
			m_smoothedLineOutdated.values().forEach(b -> b.set(true));
		}
	}

	/**
	 * Set enable status of smoothed lines. If called while the plot is currently updating, the smoothed line will be
	 * drawn automatically. Otherwise, call {@link JFreeChartLinePlotPanel#triggerSmoothedLinesUpdate()} to force an
	 * update.
	 *
	 * @param enabled
	 */
	public void setEnableSmoothedLines(final boolean enabled) {
		if (enabled != m_smoothedLinesEnabled) {
			m_smoothedLinesEnabled = enabled;
			m_smoothedLineOutdated.values().forEach(b -> b.set(true));
		}
	}

	/**
	 * Update the line style, sets colors, line transparency and hides legend.
	 *
	 * @param smoothedLinesEnabled If true: original line becomes transparent and smoothed line will be fully visible.
	 *            Also shows legend of smoothed line. If false: other way around.
	 */
	private void updateLineStyle(final String lineLabel, final boolean smoothedLinesEnabled) {
		// All updates of the lines need to happen in the EDT
		SwingUtilities.invokeLater(() -> {
			final int lineIndex = m_lineLabelToLineIndex.get(lineLabel);
			final int lineSmoothedIndex = m_lineLabelToLineIndex.get(lineLabel + SMOOTHED_LINE_KEY_SUFFIX);
			final XYItemRenderer r = getRenderer();

			if (smoothedLinesEnabled) {
				r.setSeriesPaint(lineIndex, getColorWithTransparecy(lineIndex, 0.2f));
				r.setSeriesPaint(lineSmoothedIndex, getColorWithTransparecy(lineIndex, 1));
				r.setSeriesVisibleInLegend(lineSmoothedIndex, true, false);
			} else {
				r.setSeriesPaint(lineIndex, getColorWithTransparecy(lineIndex, 1f));
				r.setSeriesPaint(lineSmoothedIndex, getColorWithTransparecy(lineIndex, 0.2f));
				r.setSeriesVisibleInLegend(lineSmoothedIndex, false, false);
			}
		});
	}

	/**
	 * Get the color for the line with specified index and specified transparency.
	 *
	 * @param lineIndex the index of the line to get the color from
	 * @param transparency transparency value in range [0(fully transparent),1(fully visible)]
	 * @return
	 */
	private Color getColorWithTransparecy(final int lineIndex, final float transparency) {
		final String lineLabel = m_lineIndexToLineLabel.get(lineIndex);
		final Color c = m_lineReferenceColors.computeIfAbsent(lineLabel, key -> getNextColor());
		final float[] cComp = c.getColorComponents(null);
		return new Color(cComp[0], cComp[1], cComp[2], transparency);
	}

	private void plotSmoothed(final String lineLabel) {
		final AtomicBoolean lineOutdated = m_smoothedLineOutdated.get(lineLabel + SMOOTHED_LINE_KEY_SUFFIX);
		if (!m_smoothedLinesEnabled) {
			if (lineOutdated.get()) {
				clearSmoothedLine(lineLabel);
				updateLineStyle(lineLabel, false);
				lineOutdated.set(false);
			}
			return;
		}

		if (lineOutdated.get()) {
			initSmoothingIter(lineLabel, m_smoothingAlpha);
			clearSmoothedLine(lineLabel);
			updateLineStyle(lineLabel, true);
			lineOutdated.set(false);
		}

		final XYSeries line = m_dataset.getSeries(lineLabel + SMOOTHED_LINE_KEY_SUFFIX);
		final ExponentialSmoothingIterator iter = m_smoothingIters.get(lineLabel + SMOOTHED_LINE_KEY_SUFFIX);
		while (iter.hasNext()) {
			if (lineOutdated.get()) {
				return;
			}
			line.add(iter.next());
		}
	}

	private void clearSmoothedLine(final String lineLabel) {
		final String key = lineLabel + SMOOTHED_LINE_KEY_SUFFIX;
		m_dataset.getSeries(key).clear();
	}

	public NumberAxis getHorizontalAxis() {
		return (NumberAxis) m_plot.getDomainAxis();
	}

	public NumberAxis getVerticalAxis() {
		return (NumberAxis) m_plot.getRangeAxis();
	}

	public XYItemRenderer getRenderer() {
		return m_plot.getRenderer();
	}

	public XYSeriesCollection getDataset() {
		return m_dataset;
	}

	public XYPlot getPlot() {
		return m_plot;
	}

	/**
	 * Set range of vertical axis to current max and min visible in plot. This is useful if we manually set the
	 * horizontal axis range.
	 */
	public void autoRangeVerticalAxis() {
		getVerticalAxis().setRange(getRenderer().findRangeBounds(getDataset()));
	}

	/**
	 * Set range of horizontal axis to current max and min visible in plot. This is useful if we manually set the
	 * vertical axis range.
	 */
	public void autoRangeHorizontalAxis() {
		getHorizontalAxis().setRange(getRenderer().findDomainBounds(getDataset()));
	}

	public void restoreVerticalDomainBounds() {
		m_chartPanel.restoreAutoRangeBounds();
	}

	public void restoreHorizontalDomainBounds() {
		m_chartPanel.restoreAutoDomainBounds();
	}

	/**
	 * @return the maximum item count of all plots in this chart
	 */
	@SuppressWarnings("unchecked")
	public int getMaxItemCount() {
		return m_dataset.getSeries().stream().mapToInt(series -> ((XYSeries) series).getItemCount()).max().getAsInt();
	}

	/**
	 * Return the next color in {@link JFreeChartLinePlotPanel#LINE_COLORS}. If the end is reached we will start from
	 * the beginning.
	 */
	private Color getNextColor() {
		return LINE_COLORS.get(m_colorIdx++ % LINE_COLORS.size());
	}

	/**
	 * Iterator backed by a XYSeries which calculates a smoothed version of the series on the fly. See:
	 * https://en.wikipedia.org/wiki/Exponential_smoothing
	 */
	private class ExponentialSmoothingIterator implements Iterator<XYDataItem> {

		private final XYSeries m_data;

		private int m_idx = 0;

		private final double m_alpha;

		private double m_current;
		private double m_previous;

		public ExponentialSmoothingIterator(final XYSeries data, final double alpha) {
			m_data = data;
			m_alpha = alpha;
			if (m_alpha > 1.0 || m_alpha < 0.0) {
				throw new IllegalArgumentException("Alpha must be in range [0,1]");
			}
		}

		@Override
		public boolean hasNext() {
			return m_idx < m_data.getItemCount();
		}

		@Override
		public XYDataItem next() {
			m_previous = m_current;
			if (m_idx == 0) {
				m_current = (double) m_data.getY(m_idx);
			} else {
				m_current = (m_alpha * (double) m_data.getY(m_idx)) + ((1 - m_alpha) * m_previous);
			}
			final XYDataItem item = new XYDataItem(m_data.getX(m_idx), m_current);
			m_idx++;
			return item;
		}
	}
}
