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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.Timer;
import javax.swing.event.DocumentListener;
import javax.swing.text.DefaultCaret;

import org.jfree.chart.axis.NumberAxis;
import org.jfree.data.Range;
import org.knime.core.node.util.SharedIcons;
import org.knime.dl.keras.base.nodes.learner.view.DLLinePlotView;
import org.knime.dl.keras.base.nodes.learner.view.DLLinePlotViewData.DLLinePlotViewDataEntry;
import org.knime.dl.keras.base.nodes.learner.view.rangeslider.RangeSlider;

import gnu.trove.TObjectFloatHashMap;

/**
 * DLView containing of a {@link JFreeChartLinePlotPanel} and a textual history view.
 *
 * @author David Kolb, KNIME GmbH, Konstanz, Germany
 */
public class DLJFreeChartLinePlotWithHistoryView implements DLLinePlotView<DLJFreeChartLinePlotViewSpec> {

    final static int SLIDER_MIN = 0;

    final static int SLIDER_MAX = 100;

    final static Dimension RESET_BUTON_DIMENSION = new Dimension(10, 10);

    private final JFreeChartLinePlotPanel m_linePlot;

    private final Map<String, JTextArea> m_historyAreas = new HashMap<>();

    private final Map<String, JLabel> m_currentValueLabels = new HashMap<>();

    private final TObjectFloatHashMap<String> m_currentValues;

    private boolean m_isRunning = false;

    private final JPanel m_component;

    private final Timer m_currentValueUpdateTimer = new Timer(1000, (e) -> updateCurrentValueLabels());

    private final SliderPlotSync m_sliderPlotSync;

    private NumberTextField m_absoluteLeftRange;

    private NumberTextField m_absoluteRightRange;

    private RangeSlider m_rangeSlider;

    private ToggleIcon m_scrollLockButton;

    /**
     * Constructor for class DLJFreeChartLinePlotWithHistoryView specifying the plot view spec.
     *
     * @param plotViewSpec
     */
    public DLJFreeChartLinePlotWithHistoryView(final DLJFreeChartLinePlotViewSpec plotViewSpec) {
        m_component = new JPanel(new GridBagLayout());

        m_currentValues = new TObjectFloatHashMap<>(plotViewSpec.numPlots());

        final JTabbedPane historyTabsPane = new JTabbedPane();
        GridBagConstraints gbc;

        for (int i = 0; i < plotViewSpec.numPlots(); i++) {
            final JTextArea historyArea = new JTextArea();
            final DefaultCaret caret = (DefaultCaret)historyArea.getCaret();
            // Enable automatic to bottom scrolling
            caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
            historyArea.setEditable(false);
            m_historyAreas.put(plotViewSpec.getLineLabel(i), historyArea);

            final JScrollPane historyScroller = new JScrollPane(historyArea);
            final JPanel historyWrapper = new JPanel(new GridBagLayout());
            gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.weightx = 1;
            gbc.weighty = 1;
            gbc.fill = GridBagConstraints.BOTH;
            historyWrapper.add(historyScroller, gbc);

            final JLabel currentValue = new JLabel("-");
            currentValue.setFont(new Font(currentValue.getFont().getName(), currentValue.getFont().getStyle(), 18));

            final JPanel valueWrapperWithBorder = new JPanel(new GridLayout(0, 1));
            valueWrapperWithBorder.setBorder(BorderFactory.createTitledBorder("Current Value:"));
            valueWrapperWithBorder.add(currentValue);
            m_currentValueLabels.put(plotViewSpec.getLineLabel(i), currentValue);

            gbc.gridy++;
            gbc.weighty = 0;
            gbc.insets = new Insets(10, 10, 10, 10);
            historyWrapper.add(valueWrapperWithBorder, gbc);

            historyTabsPane.addTab(plotViewSpec.getLineLabel(i), historyWrapper);
        }

        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.insets = new Insets(0, 0, 0, 5);
        gbc.fill = GridBagConstraints.BOTH;
        m_linePlot = new JFreeChartLinePlotPanel(plotViewSpec);
        m_component.add(createPlotWithControlsPanel(m_linePlot), gbc);

        historyTabsPane.setPreferredSize(new Dimension(180, 500));
        historyTabsPane.setMinimumSize(new Dimension(180, 500));
        gbc.gridx = 1;
        gbc.weightx = 0;
        gbc.weighty = 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.fill = GridBagConstraints.VERTICAL;
        m_component.add(historyTabsPane, gbc);

        m_sliderPlotSync = new SliderPlotSync(m_rangeSlider, m_linePlot);
    }

    private Component createPlotWithControlsPanel(final Component chartPanel) {
        final JPanel wrapper = new JPanel(new GridBagLayout());

        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weighty = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.BOTH;
        wrapper.add(chartPanel, gbc);

        final JTabbedPane rangeControlTabsPane = new JTabbedPane();
        rangeControlTabsPane.addChangeListener(e -> {
            if (rangeControlTabsPane.getSelectedIndex() == 0) {
                if (m_sliderPlotSync != null) {
                    m_sliderPlotSync.reset();
                    m_scrollLockButton.setIsSelected(true);
                }
            } else {
                applyAbsoluteRanges();
                m_sliderPlotSync.setIsAbsoluteRange(true);
            }
        });
        rangeControlTabsPane.addTab("Relative Zoom", createRelativeXRangeControls());
        rangeControlTabsPane.addTab("Absolute Zoom", createAbsoluteXRangeControls());

        gbc.gridy = 1;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        wrapper.add(rangeControlTabsPane, gbc);

        gbc.gridy = 2;
        wrapper.add(createSmoothingControls(), gbc);

        return wrapper;
    }

    private Component createSmoothingControls() {
        final JPanel wrapper = new JPanel(new GridBagLayout());

        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(10, 10, 10, 0);

        final JCheckBox enableSmoothingBox = new JCheckBox("Smoothing");
        wrapper.add(enableSmoothingBox, gbc);

        gbc.gridx = 1;
        gbc.insets = new Insets(10, 5, 10, 10);
        final JSpinner smoothingAlphaSpinner =
            new JSpinner(new SpinnerNumberModel(1 - JFreeChartLinePlotPanel.SMOOTHING_ALPHA_DEFAULT, 0.0, 1.0, 0.005));
        smoothingAlphaSpinner.setPreferredSize(new Dimension(60, 25));
        smoothingAlphaSpinner.setMinimumSize(new Dimension(60, 25));
        smoothingAlphaSpinner.setEnabled(false);
        wrapper.add(smoothingAlphaSpinner, gbc);

        smoothingAlphaSpinner.addChangeListener(arg0 -> {
            final double currentSpinnerValue = ((double)smoothingAlphaSpinner.getValue());
            m_linePlot.setSmoothingAlpha(1 - currentSpinnerValue);
            m_linePlot.triggerSmoothedLinesUpdate();
        });

        enableSmoothingBox.addItemListener(e -> {
            smoothingAlphaSpinner.setEnabled(enableSmoothingBox.isSelected());
            m_linePlot.setEnableSmoothedLines(enableSmoothingBox.isSelected());
            m_linePlot.triggerSmoothedLinesUpdate();
        });

        final JCheckBox enableLogScaleBox = new JCheckBox("Log Scale");
        gbc.gridx++;
        gbc.insets = new Insets(10, 10, 10, 10);
        wrapper.add(enableLogScaleBox, gbc);

        enableLogScaleBox.addItemListener(e -> {
            m_sliderPlotSync.setIsLogScale(enableLogScaleBox.isSelected());
        });

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx++;
        gbc.weightx = 1;
        wrapper.add(new Box(0), gbc);

        final JPanel border = new JPanel(new GridLayout());
        border.add(wrapper);
        border.setBorder(BorderFactory.createTitledBorder(""));
        return border;
    }

    private Component createAbsoluteXRangeControls() {
        final JPanel wrapper = new JPanel(new GridBagLayout());

        m_absoluteLeftRange = new NumberTextField((DocumentAdapter)e -> {
            if (m_absoluteLeftRange.getText().isEmpty()) {
                m_sliderPlotSync.setHorizontalRange(0.0, m_absoluteRightRange.getNumber());
            } else {
                applyAbsoluteRanges();
            }
        });
        m_absoluteLeftRange.setFixedSize(new Dimension(100, 20));
        m_absoluteRightRange = new NumberTextField((DocumentAdapter)e -> {
            if (m_absoluteRightRange.getText().isEmpty()) {
                m_sliderPlotSync.setHorizontalRange(m_absoluteLeftRange.getNumber(), m_sliderPlotSync.getMaxX());
            } else {
                applyAbsoluteRanges();
            }
        });
        m_absoluteRightRange.setFixedSize(new Dimension(100, 20));

        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.insets = new Insets(0, 10, 0, 0);
        wrapper.add(new JLabel("Left Bound:"), gbc);
        gbc.gridx++;
        wrapper.add(m_absoluteLeftRange, gbc);
        gbc.gridx++;
        wrapper.add(new JLabel("Right Bound:"), gbc);
        gbc.gridx++;
        wrapper.add(m_absoluteRightRange, gbc);
        gbc.gridx++;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        wrapper.add(new Box(0), gbc);

        return wrapper;
    }

    private void applyAbsoluteRanges() {
        m_sliderPlotSync.setHorizontalRange(m_absoluteLeftRange.getNumber(), m_absoluteRightRange.getNumber());
    }

    private Component createRelativeXRangeControls() {
        final JPanel wrapper = new JPanel(new GridBagLayout());

        m_rangeSlider = new RangeSlider();
        m_rangeSlider.setValue(SLIDER_MIN);
        m_rangeSlider.setUpperValue(SLIDER_MAX);

        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.insets = new Insets(0, 10, 0, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        wrapper.add(m_rangeSlider, gbc);

        final JButton resetSliderButton = new JButton("Reset");
        gbc.gridx = 1;
        gbc.weightx = 0;
        gbc.insets = new Insets(0, 10, 5, 5);
        wrapper.add(resetSliderButton, gbc);

        m_scrollLockButton = new ToggleIcon(SharedIcons.ADD_PLUS.get());
        m_scrollLockButton.setIsSelected(true);
        m_scrollLockButton.setToolTipText("Auto Range Plot");
        gbc.gridx = 2;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.insets = new Insets(0, 0, 5, 0);
        wrapper.add(m_scrollLockButton, gbc);

        m_scrollLockButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                m_sliderPlotSync.setIsScrollLock(!m_scrollLockButton.isSelected());
            }
        });

        resetSliderButton.addActionListener(e -> {
            m_sliderPlotSync.reset();
        });

        return wrapper;
    }

    private void updateCurrentValueLabels() {
        m_currentValues.forEachEntry((k, v) -> {
            m_currentValueLabels.get(k).setText(Float.toString(v));
            return true;
        });
    }

    /**
     * Set the update delay of the current value filed in the history view.
     *
     * @param milliseconds the delay in milliseconds
     */
    public void setCurrentValueTimerUpdateDelay(final int milliseconds) {
        m_currentValueUpdateTimer.setDelay(milliseconds);
    }

    /**
     * Start the timed update of the current value filed in the history view.
     */
    public void startCurrentValueUpdate() {
        if (!m_currentValueUpdateTimer.isRunning()) {
            m_currentValueUpdateTimer.start();
        }
    }

    /**
     * Stop the timed update of the current value filed in the history view.
     */
    public void stopCurrentValueUpdate() {
        if (m_currentValueUpdateTimer.isRunning()) {
            m_currentValueUpdateTimer.stop();
        }
    }

    @Override
    public Component getComponent() {
        return m_component;
    }

    @Override
    public void update(final String lineLabel, final Iterator<DLLinePlotViewDataEntry> iterator) {
        while (iterator.hasNext()) {
            final DLLinePlotViewDataEntry dataEntry = iterator.next();

            m_linePlot.plotNext(lineLabel, dataEntry.getX() + 1, dataEntry.getY()); // x-values are 0-based
            m_historyAreas.get(lineLabel).append(dataEntry.getY() + "\n");
            m_currentValues.put(lineLabel, dataEntry.getY());

            m_sliderPlotSync.updateMaxXValue(dataEntry.getX() + 1);
            m_sliderPlotSync.updateYBounds(dataEntry.getY());
            m_sliderPlotSync.updateOnData();
        }
    }

    /**
     * Get the isRunning flag.
     *
     * @return the isRunning flag
     */
    public boolean isRunning() {
        return m_isRunning;
    }

    /**
     * Set the isRuinning flag.
     *
     * @param isRunning
     */
    public void setIsRunning(final boolean isRunning) {
        m_isRunning = isRunning;
    }

    /**
     * A JTextField that turns red if no double number is entered.
     */
    private class NumberTextField extends JTextField {

        private static final long serialVersionUID = 1L;

        private final static String NUMBER_PATTERN = "\\d+";

        public NumberTextField(final DocumentListener documentListener) {
            super();
            this.getDocument().addDocumentListener(documentListener);
            this.getDocument().addDocumentListener((DocumentAdapter)e -> checkText());
        }

        public void setFixedSize(final Dimension dim) {
            this.setMinimumSize(dim);
            this.setPreferredSize(dim);
            this.setSize(dim);
        }

        private void checkText() {
            final String text = this.getText();
            if (!text.matches(NUMBER_PATTERN) || text.isEmpty()) {
                this.setForeground(Color.RED);
            } else {
                this.setForeground(null);
            }
        }

        public Double getNumber() {
            try {
                return Math.floor(Double.parseDouble(this.getText()));
            } catch (final NumberFormatException e) {
                return null;
            }
        }
    }

    /**
     * A clickable icon that behaves like a checkbox.
     */
    private class ToggleIcon extends JPanel {

        private static final long serialVersionUID = 1L;

        private boolean m_isSelected = false;

        private final JLabel m_iconLabel;

        final Color[] m_colors = new Color[3];

        public ToggleIcon(final Icon icon) {
            m_iconLabel = new JLabel(icon);
            m_iconLabel.setPreferredSize(new Dimension(icon.getIconWidth() + 12, icon.getIconHeight() + 12));
            m_iconLabel.setOpaque(true);

            m_colors[0] = new Color(220f / 255f, 220f / 255f, 220f / 255f);
            m_colors[1] = new Color(200f / 255f, 200f / 255f, 200f / 255f);
            m_colors[2] = null;

            m_iconLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(final MouseEvent e) {
                    m_isSelected = !m_isSelected;
                    updateIconAppearance();
                }

                @Override
                public void mouseEntered(final MouseEvent e) {
                    if (!m_isSelected) {
                        m_iconLabel.setBackground(m_colors[0]);
                    }
                }

                @Override
                public void mouseExited(final MouseEvent e) {
                    if (!m_isSelected) {
                        m_iconLabel.setBackground(m_colors[2]);
                    }
                }
            });

            add(m_iconLabel);
        }

        private void updateIconAppearance() {
            m_iconLabel.setBackground(m_colors[m_isSelected ? 1 : 0]);
        }

        @Override
        public synchronized void addMouseListener(final MouseListener l) {
            m_iconLabel.addMouseListener(l);
        }

        @Override
        public void setToolTipText(final String text) {
            m_iconLabel.setToolTipText(text);
        }

        public void setIsSelected(final boolean isSelected) {
            m_isSelected = isSelected;
            updateIconAppearance();
        }

        public boolean isSelected() {
            return m_isSelected;
        }
    }

    /**
     * Helper class which manages the synchronization of the horizontal range controls and the line plot.
     */
    private class SliderPlotSync {

        private final NumberFormat DOUBLE_FORMAT = new DecimalFormat("#0");

        private final static double EPSILON = 0.01;

        private final RangeSlider m_slider;

        private final JFreeChartLinePlotPanel m_linePlot;

        private boolean m_isScrollLock = false;

        private double m_maxXValue = 0.0;

        private Double m_maxYValue;

        /**
         * A lower range of zero is not allowed for the log axis. As this value is only used as a lower range bound for
         * the y-axis we want to find the lowest value which is not zero.
         */
        private Double m_minButZeroYValue;

        private boolean m_isEnabled = true;

        private boolean m_isLogScale = false;

        private boolean m_isAbsoluteRange = false;

        public SliderPlotSync(final RangeSlider slider, final JFreeChartLinePlotPanel linePlot) {
            m_slider = slider;
            m_linePlot = linePlot;

            m_slider.addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseDragged(final MouseEvent e) {
                    syncPlot();
                }
            });

            // Hack to detect a mouse released event after the mouse was dragged.
            final boolean[] dragged = new boolean[1];
            m_linePlot.getChartPanel().addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseDragged(final MouseEvent e) {
                    dragged[0] = true;
                }
            });

            m_linePlot.getChartPanel().addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(final MouseEvent e) {
                    if (dragged[0]) {
                        dragged[0] = false;
                        syncSlider();
                        autoZoomYAxis();

                        final Range axisRange = m_linePlot.getHorizontalAxis().getRange();
                        m_absoluteLeftRange.setText(DOUBLE_FORMAT.format(axisRange.getLowerBound()));
                        m_absoluteRightRange.setText(DOUBLE_FORMAT.format(axisRange.getUpperBound()));

                        if (m_isAbsoluteRange || m_isScrollLock) {
                            disable();
                        } else {
                            enable();
                        }
                    }
                }
            });

        }

        private void syncSlider() {
            final double maxItemCount = m_maxXValue;
            final Range axisRange = m_linePlot.getHorizontalAxis().getRange();
            final int lowerSliderPos =
                new Double(Math.rint((axisRange.getLowerBound() / maxItemCount) * 100)).intValue();
            final int upperSliderPos =
                new Double(Math.rint((axisRange.getUpperBound() / maxItemCount) * 100)).intValue();
            m_slider.setValue(lowerSliderPos);
            m_slider.setUpperValue(upperSliderPos);
        }

        private void syncPlot() {
            final double maxX = m_maxXValue;
            final double lowerBound = (m_slider.getValue() / 100.0) * maxX;
            final double upperBound = (m_slider.getUpperValue() / 100.0) * maxX;

            if (lowerBound < upperBound) {
                m_linePlot.getHorizontalAxis().setRange(lowerBound, upperBound);
                autoZoomYAxis();
            }
        }

        private void autoZoomYAxis() {
            double lowerY = 0.0;
            double upperY = m_maxYValue != null ? m_maxYValue : 0.0;
            if (m_isLogScale) {
                lowerY = m_minButZeroYValue;
            }
            // This may be null in the beginning as we add data to the plot asynchronously
            final Range yRange = m_linePlot.getCurrentYBounds();
            if (yRange != null) {
                upperY = yRange.getUpperBound();
            }
            // If the ranges are the same we display a very small window
            if (lowerY == upperY) {
                lowerY = lowerY - EPSILON / 2.0;
                upperY = upperY + EPSILON / 2.0;
            }
            m_linePlot.getVerticalAxis().setRange(lowerY, upperY);
        }

        public void updateOnData() {
            if (m_isEnabled) {
                if (m_isScrollLock) {
                    syncSlider();
                } else {
                    syncPlot();
                }
            }
        }

        public void reset() {
            m_slider.setValue(SLIDER_MIN);
            m_slider.setUpperValue(SLIDER_MAX);
            setIsAbsoluteRange(false);
            setIsScrollLock(false);
            enable();
            updateOnData();
        }

        public void setIsScrollLock(final boolean isScrollLock) {
            m_isScrollLock = isScrollLock;
            updateOnData();
        }

        public void updateMaxXValue(final double max) {
            if (max > m_maxXValue) {
                m_maxXValue = max;
            }
        }

        public void updateYBounds(final double update) {
            // A lower range of zero is not allowed for the log axis. As this value is only used as a lower range bound
            // for the y-axis we want to find the lowest value which is not zero.
            if (update > 0.0) {
                if (m_minButZeroYValue == null) {
                    m_minButZeroYValue = update;
                } else if (update < m_minButZeroYValue) {
                    m_minButZeroYValue = update;
                }
            }

            if (m_maxYValue == null) {
                m_maxYValue = update;
            } else if (update > m_maxYValue) {
                m_maxYValue = update;
            }
        }

        public void setHorizontalRange(final Double lower, final Double upper) {
            final NumberAxis axis = m_linePlot.getHorizontalAxis();

            double l = 0.0;
            double u = 0.0;

            if (lower != null && upper != null) {
                disable();
                l = lower;
                u = upper;
            } else if (lower != null && upper == null) {
                disable();
                final Range r = axis.getRange();
                l = lower;
                u = r.getUpperBound();
            } else if (lower == null && upper != null) {
                disable();
                final Range r = axis.getRange();
                l = r.getLowerBound();
                u = upper;
            } else {
                disable();
                final Range r = axis.getRange();
                l = r.getLowerBound();
                u = r.getUpperBound();
            }

            if (l < u && (l != axis.getLowerBound() || u != axis.getUpperBound())) {
                axis.setRange(new Range(l, u));
            }
            autoZoomYAxis();
        }

        public double getMaxX() {
            return m_maxXValue;
        }

        public void enable() {
            m_isEnabled = true;
        }

        public void disable() {
            m_isEnabled = false;
        }

        public void setIsLogScale(final boolean isLogScale) {
            m_isLogScale = isLogScale;
            if (isLogScale) {
                m_linePlot.enableLogAxis();
            } else {
                m_linePlot.restoreDefaultAxis();
            }
            autoZoomYAxis();
        }

        public void setIsAbsoluteRange(final boolean isAbsoluteRange) {
            m_isAbsoluteRange = isAbsoluteRange;
        }
    }
}
