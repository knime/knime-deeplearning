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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.text.DefaultCaret;

import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeView;
import org.knime.dl.core.DLDefaultEvent;
import org.knime.dl.core.DLEvent;
import org.knime.dl.keras.base.nodes.learner.view.DLLinePlotViewData.DLLinePlotViewDataEntry;
import org.knime.dl.keras.base.nodes.learner.view.jfreechart.DLJFreeChartLinePlotViewSpec;
import org.knime.dl.keras.base.nodes.learner.view.jfreechart.DLJFreeChartLinePlotWithHistoryView;

/**
 * @author David Kolb, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 */
// TODO: actually this should just delegate to some object which can be anything (even a JavaScript thingy).
// We don't want to enforce an implementation here.
public class DLKerasLearnerNodeView<M extends NodeModel & DLInteractiveLearnerNodeModel> extends NodeView<M> {

    /**
     * Alternative to setShowNODATALabel() because the NODATA label of the NodeView is also displayed during execution,
     * which is exactly what we do not want.
     */
    private static final Component NO_DATA_OVERLAY = new DLKerasLearnerNodeViewNoDataOverlay().getComponent();

    private DLKerasLearnerNodeViewContentPanel m_content;

    public DLKerasLearnerNodeView(final M model) {
        super(model);
        // Use own NODATA label instead of NodeView impl
        setShowNODATALabel(false);
        updateModel(model.getProgressMonitor());
    }

    @Override
    protected void updateModel(final Object arg) {
        if (arg == null) {
            setComponent(NO_DATA_OVERLAY);
            if (m_content != null) {
                m_content.reset();
                m_content = null;
            }
        } else if (arg instanceof DLProgressMonitor) {
            final DLProgressMonitor monitor = (DLProgressMonitor)arg;
            if (monitor.isRunning() || monitor.hasData()) {
                if (m_content == null) {
                    final DLViewSpec[] viewSpecs = monitor.getViewSpecs();
                    if (viewSpecs != null) {
                        // initialize panel
                        m_content = new DLKerasLearnerNodeViewContentPanel(viewSpecs);
                        m_content.userStoppedLearning().addListener((src, v) -> getNodeModel().stopLearning());
                    } else {
                        setComponent(NO_DATA_OVERLAY);
                        return;
                    }
                }
                setComponent(m_content.getComponent());
                m_content.update(monitor);
            } else {
                setComponent(NO_DATA_OVERLAY);
            }
        } else {
            throw new IllegalArgumentException("Can't handle objects of type '" + arg.getClass()
                + "' in node view. Most likely an implementation error.");
        }
    }

    @Override
    protected void modelChanged() {
        // no op
    }

    @Override
    protected void onOpen() {
        // no op
    }

    @Override
    protected void onClose() {
        if (m_content != null) {
            m_content.reset();
        }
    }

    private static class DLKerasLearnerNodeViewNoDataOverlay {

        private final Component m_component;

        public DLKerasLearnerNodeViewNoDataOverlay() {
            m_component = new JLabel("<html><center>No data to display</center></html>", SwingConstants.CENTER);

            Dimension size = DLKerasLearnerNodeViewContentPanel.getSensibleDisplayArea();
            m_component.setPreferredSize(size);
            m_component.setMinimumSize(size);
        }

        public Component getComponent() {
            return m_component;
        }
    }

    private static class DLKerasLearnerNodeViewContentPanel {

        private static final DateTimeFormatter START_TIME_DISPLAY_FORMATTER =
            DateTimeFormatter.ofPattern("E, dd MMM yyyy HH:mm:ss");

        private static final String ELAPSED_TIME_DISPLAY_FORMAT = "%02d:%02d:%02d (hh:mm:ss)";

        private static String formatStartTime(final LocalDateTime startTime) {
            return startTime != null ? startTime.format(START_TIME_DISPLAY_FORMATTER) : "-";
        }

        private static String formatElapsedTime(final Duration elapsedTime) {
            if (elapsedTime == null) {
                return "-";
            }
            final long elapsedSeconds = elapsedTime.getSeconds();
            final long hours = elapsedSeconds / 3600;
            final int minutes = (int)((elapsedSeconds % 3600) / 60);
            final int secs = (int)(elapsedSeconds % 60);
            return String.format(ELAPSED_TIME_DISPLAY_FORMAT, hours, minutes, secs);
        }

        private final DLViewSpec[] m_viewSpecs;

        private final Map<String, DLJFreeChartLinePlotWithHistoryView> m_views;

        private final JPanel m_component;

        private final DLLearningProgressBar m_epochProgressBar;

        private final DLLearningProgressBar m_batchProgressBar;

        private final LeftAlignLabelWithValue m_startTime;

        private final LeftAlignLabelWithValue m_elapsedTime;

        private final LeftAlignButton m_stopButton;

        private final DLEvent<Void> m_userStoppedLearning = new DLDefaultEvent<>();

        private final JTextArea m_pythonStdOutOutputArea;

        private final JTextArea m_pythonStdErrOutputArea;

        /**
         * Data iterators for this view. Its important that each view has its own iterator state if we open several
         * views at once.
         */
        private Map<String, List<Iterator<DLLinePlotViewDataEntry>>> m_dataIterators;

        private int m_lastEpoch = 0;

        public DLKerasLearnerNodeViewContentPanel(final DLViewSpec[] viewSpecs) {
            m_viewSpecs = viewSpecs;
            m_views = new HashMap<>(viewSpecs.length);
            m_component = new JPanel(new GridBagLayout());

            // Add lineplot tabs
            final JTabbedPane tabs = new JTabbedPane();
            tabs.setFont(new Font(tabs.getFont().getName(), tabs.getFont().getStyle(), 14));

            for (final DLViewSpec spec : viewSpecs) {
                // assume DLJFreeChartLinePlotViewSpec for now
                final DLJFreeChartLinePlotWithHistoryView tab =
                    new DLJFreeChartLinePlotWithHistoryView((DLJFreeChartLinePlotViewSpec)spec);
                m_views.put(spec.id(), tab);
                tabs.addTab(spec.title(), tab.getComponent());
            }

            final JPanel logPanel = new JPanel(new GridBagLayout());
            final GridBagConstraints logGbc = new GridBagConstraints();

            logGbc.fill = GridBagConstraints.BOTH;
            logGbc.insets = new Insets(2, 2, 2, 2);
            logGbc.gridx = 0;
            logGbc.gridy = 0;
            logPanel.add(new JLabel("Log"), logGbc);

            logGbc.gridy++;
            logGbc.weightx = 1.;
            logGbc.weighty = 1.;
            m_pythonStdOutOutputArea = new JTextArea();
            ((DefaultCaret)m_pythonStdOutOutputArea.getCaret()).setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
            m_pythonStdOutOutputArea.setEditable(false);
            m_pythonStdOutOutputArea.setFont(new Font("monospaced", Font.PLAIN, 12));
            final JScrollPane stdOutScrollPane = new JScrollPane(m_pythonStdOutOutputArea);
            // Otherwise it will mess with the GridBagLayout
            stdOutScrollPane.setPreferredSize(new Dimension(1, 1));
            logPanel.add(stdOutScrollPane, logGbc);

            logGbc.weightx = 0.;
            logGbc.weighty = 0.;
            logGbc.gridy++;
            logGbc.insets = new Insets(6, 2, 2, 2);
            logPanel.add(new JLabel("Error Log"), logGbc);

            logGbc.gridy++;
            logGbc.weightx = 1.;
            logGbc.weighty = 1.;
            logGbc.insets = new Insets(2, 2, 2, 2);
            m_pythonStdErrOutputArea = new JTextArea();
            ((DefaultCaret)m_pythonStdErrOutputArea.getCaret()).setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
            m_pythonStdErrOutputArea.setEditable(false);
            m_pythonStdErrOutputArea.setFont(new Font("monospaced", Font.PLAIN, 12));
            final JScrollPane stdErrScrollPane = new JScrollPane(m_pythonStdErrOutputArea);
            // Otherwise it will mess with the GridBagLayout
            stdErrScrollPane.setPreferredSize(new Dimension(1, 1));
            logPanel.add(stdErrScrollPane, logGbc);
            tabs.addTab("Keras Log Output", logPanel);

            final GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.weightx = 1;
            gbc.weighty = 1;
            gbc.fill = GridBagConstraints.BOTH;
            m_component.add(tabs, gbc);

            // Epoch progress bar
            gbc.gridy++;
            gbc.weighty = 0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(10, 10, 0, 0);
            m_epochProgressBar = new DLLearningProgressBar("Epoch", "Avg. duration / epoch");
            m_component.add(m_epochProgressBar, gbc);

            // Batch progress bar
            gbc.gridy++;
            gbc.insets = new Insets(0, 10, 10, 0);
            m_batchProgressBar = new DLLearningProgressBar("Batch", "Avg. duration / batch");
            m_component.add(m_batchProgressBar, gbc);

            // Start time
            gbc.gridy++;
            gbc.insets = new Insets(0, 15, 10, 0);
            m_startTime = new LeftAlignLabelWithValue("Start time: ");
            m_startTime.setValue(formatStartTime(null));
            m_component.add(m_startTime, gbc);

            // Elapsed time
            gbc.gridy++;
            m_elapsedTime = new LeftAlignLabelWithValue("Elapsed: ");
            m_elapsedTime.setValue(formatElapsedTime(null));
            m_component.add(m_elapsedTime, gbc);

            // Stop button
            gbc.gridy++;
            m_stopButton = new LeftAlignButton("Stop learning");
            m_stopButton.getButton().addActionListener(e -> {
                m_userStoppedLearning.raise(null);
                setHasStoppedButtonStatus(true);
            });
            gbc.insets = new Insets(10, 15, 10, 0);
            m_component.add(m_stopButton, gbc);

            Dimension size = getSensibleDisplayArea();
            m_component.setPreferredSize(size);
            m_component.setMinimumSize(size);
        }

        /**
         * Return the current screen resolution times 2/3.
         */
        private static Dimension getSensibleDisplayArea() {
            Dimension pdr = Toolkit.getDefaultToolkit().getScreenSize();
            int newWidth = (int)Math.rint(pdr.getWidth() * (2.0 / 3.0));
            int newHeight = (int)Math.rint(pdr.getHeight() * (2.0 / 3.0));
            return new Dimension(newWidth, newHeight);
        }

        public Component getComponent() {
            return m_component;
        }

        public DLEvent<Void> userStoppedLearning() {
            return m_userStoppedLearning;
        }

        public void update(final DLProgressMonitor monitor) {
            if (m_dataIterators == null && monitor.hasData()) {
                // Initialize view data iterators
                final DLViewDataCollection[] viewData = monitor.getViewData();
                m_dataIterators = new HashMap<>(viewData.length);
                for (final DLViewDataCollection vdc : viewData) {
                    final List<Iterator<DLLinePlotViewDataEntry>> dataIterators =
                        StreamSupport.stream(((DLLinePlotViewDataCollection)vdc).spliterator(), false) //
                            .map(vd -> vd.iterator()) //
                            .collect(Collectors.toList());
                    m_dataIterators.put(vdc.getSpec().id(), dataIterators);
                }
            }

            // Start timers if not yet started
            for (final DLJFreeChartLinePlotWithHistoryView view : m_views.values()) {
                view.startCurrentValueUpdate();
            }

            // Disable stop button if learning already stopped (or interrupted for some reason)
            if (monitor.hasStoppedEarly() || monitor.hasFinished()) {
                setHasStoppedButtonStatus(true);
            } else if (!monitor.isRunning()) {
                setHasStoppedButtonStatus(false);
            }

            // Update progress bars & labels
            final int numEpochs = monitor.getNumEpochs();
            final int numBatches = monitor.getNumBatchesPerEpoch();
            final int currEpoch = monitor.getCurrentEpoch() + 1;
            final int currBatch = monitor.getCurrentBatchInEpoch() + 1;

            m_epochProgressBar.setMaxProgress(numEpochs * numBatches);
            m_batchProgressBar.setMaxProgress(numBatches);

            m_epochProgressBar.setProgress((currEpoch - 1) * numBatches + currBatch);
            m_epochProgressBar.setProgressText(currEpoch, numEpochs);

            m_batchProgressBar.setProgress(currBatch);
            m_batchProgressBar.setProgressText(currBatch, numBatches);

            final LocalDateTime startTime = monitor.getStartDateTime();
            final LocalDateTime endTime =
                monitor.getEndDateTime() != null ? monitor.getEndDateTime() : LocalDateTime.now();
            final Duration elapsedTime = Duration.between(startTime, endTime);

            if (currEpoch != m_lastEpoch || currEpoch == monitor.getNumEpochs()) {
                m_epochProgressBar.setDuration(elapsedTime.dividedBy(currEpoch));
                m_lastEpoch = currEpoch;
            }

            if (currBatch != 0) {
                // TODO: this calculation is wrong if validation is performed. we need to subtract the time needed for
                // validation before dividing by the number of batches in such cases.
                m_batchProgressBar.setDuration(elapsedTime.dividedBy((currEpoch - 1) * (long)numBatches + currBatch));
            }

            m_startTime.setValue(formatStartTime(startTime));
            m_elapsedTime.setValue(formatElapsedTime(elapsedTime));

            // Update plots
            for (final DLViewSpec spec : m_viewSpecs) {
                final DLJFreeChartLinePlotWithHistoryView view = m_views.get(spec.id());
                view.setIsRunning(monitor.isRunning());
                if (m_dataIterators != null) {
                    final List<Iterator<DLLinePlotViewDataEntry>> iterators = m_dataIterators.get(spec.id());
                    for (int i = 0; i < iterators.size(); i++) {
                        view.update(((DLJFreeChartLinePlotViewSpec)spec).getLineLabel(i), iterators.get(i));
                    }
                }
            }

            // Update log output
            m_pythonStdOutOutputArea.setText(monitor.getStdOutOutput());
            m_pythonStdErrOutputArea.setText(monitor.getStdErrOutput());
        }

        public void reset() {
            // Stop timers if needed
            for (final DLJFreeChartLinePlotWithHistoryView v : m_views.values()) {
                v.stopCurrentValueUpdate();
            }
        }

        private void setHasStoppedButtonStatus(final boolean stoppedProperly) {
            if (stoppedProperly) {
                // Only change label if learning stopped properly (finished or early stopping)
                m_stopButton.getButton().setText("Learning stopped");
            }
            m_stopButton.getButton().setEnabled(false);
        }
    }

    /**
     * Simple helper to left align a button in a {@link GridBagLayout}.
     */
    private static class LeftAlignButton extends JPanel {
        private static final long serialVersionUID = 1L;

        private final JButton m_button;

        public LeftAlignButton(final String buttonText) {
            super(new GridBagLayout());

            m_button = new JButton(buttonText);
            m_button.setPreferredSize(new Dimension(180, 50));
            m_button.setSize(new Dimension(180, 50));
            m_button.setMinimumSize(new Dimension(180, 50));

            final GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.weightx = 0;
            add(m_button, gbc);

            gbc.gridx++;
            gbc.weightx = 1;
            add(new Box(0), gbc);
        }

        public JButton getButton() {
            return m_button;
        }
    }

    /**
     * Simple helper to left align a label with a changing value in a {@link GridBagLayout}.
     */
    private static class LeftAlignLabelWithValue extends JPanel {
        private static final long serialVersionUID = 1L;

        private final JLabel m_valueToDisplay = new JLabel();

        public LeftAlignLabelWithValue(final String label) {
            super(new GridBagLayout());

            final GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.weightx = 0;

            final JLabel startTimeLabel = new JLabel(label);
            startTimeLabel.setPreferredSize(new Dimension(85, 20));
            add(startTimeLabel, gbc);

            gbc.gridx++;
            add(m_valueToDisplay, gbc);

            // left align
            gbc.gridx++;
            gbc.weightx = 1;
            add(new Box(0), gbc);
        }

        public void setValue(final String value) {
            m_valueToDisplay.setText(value);
        }
    }
}
