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
 * History
 *   Feb 26, 2019 (marcel): created
 */
package org.knime.dl.python.prefs;

import static org.knime.python2.prefs.PythonPreferenceUtils.performActionOnWidgetInUiThread;

import javax.swing.event.ChangeEvent;

import org.apache.commons.lang3.SystemUtils;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.ShellListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.knime.python2.Conda.CondaEnvironmentIdentifier;
import org.knime.python2.config.AbstractCondaEnvironmentCreationObserver.CondaEnvironmentCreationStatus;
import org.knime.python2.config.AbstractCondaEnvironmentCreationObserver.CondaEnvironmentCreationStatusListener;
import org.knime.python2.config.CondaEnvironmentCreationDialog;

/**
 * This dialog is very similar to <code>CondaEnvironmentCreationPreferenceDialog</code> but creating a abstract class
 * would be hard and limit future changes where they may need to drift of further from each other.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
class DLCondaEnvironmentCreationPreferenceDialog extends Dialog implements CondaEnvironmentCreationDialog {

    private static final String CREATE_CPU_BUTTON_TEXT = "Create new CPU environment";

    private static final String RETRY_CPU_BUTTON_TEXT = "Retry creating new CPU environment";

    private static final String CREATE_GPU_BUTTON_TEXT = "Create new GPU environment";

    private static final String RETRY_GPU_BUTTON_TEXT = "Retry creating new GPU environment";

    private static final String ENVIRONMENT_NAME_PLACEHOLDER = "Collecting existing environment names...";

    private static final int DESCRIPTION_LABEL_WIDTH_HINTS = 300;

    private final DLCondaEnvironmentCreationObserver m_environmentCreator;

    // UI components: Initialized by #createContents().

    private final Shell m_shell;

    private Text m_environmentNameTextBox;

    private Label m_statusLabel;

    private StackLayout m_progressBarStackLayout;

    private ProgressBar m_indeterminateProgressBar;

    private ProgressBar m_determinateProgressBar;

    private Label m_errorTextBoxLabel;

    private Composite m_errorTextBoxContainer;

    private Text m_errorTextBox;

    private Button m_createOrRetryCPUButton;

    private Button m_createOrRetryGPUButton;

    private Button m_cancelButton;

    /**
     * Initialized when the {@link #switchToStartingOrRetryingState() create button is clicked}.
     */
    private volatile CondaEnvironmentCreationStatus m_status;

    /**
     * Initialized by {@link #registerExternalHooks()}.
     */
    private CondaEnvironmentCreationStatusListener m_statusChangeListener;

    private volatile boolean m_environmentCreationTerminated = false;

    public DLCondaEnvironmentCreationPreferenceDialog(final DLCondaEnvironmentCreationObserver environmentCreator,
        final Shell parent) {
        super(parent, SWT.NONE);
        m_environmentCreator = environmentCreator;
        m_shell = new Shell(parent, SWT.DIALOG_TRIM | SWT.SHEET | SWT.RESIZE);
        m_shell.setText("New Deep Learning Conda environment");
        createContents();
        m_shell.pack();
    }

    private void createContents() {
        m_shell.setLayout(new GridLayout());
        final Label descriptionText = new Label(m_shell, SWT.WRAP);
        descriptionText.setText("This will create a new preconfigured Conda environment for "
            + m_environmentCreator.getPythonVersion().getName() + " that contains all packages required by the KNIME "
            + m_environmentCreator.getLibrary().getName()
            + " integration. Creating the Conda environment may take several minutes and requires an active "
            + "internet connection.\n"
            + "On Linux and Windows you can choose between a 'GPU' environment that will run heavy calculations on a "
            + "NVIDIA GPU or a 'CPU' environment which will run everything on the CPU.");
        descriptionText.setFont(JFaceResources.getFontRegistry().getItalic(""));
        GridData gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        gridData.widthHint = DESCRIPTION_LABEL_WIDTH_HINTS;
        descriptionText.setLayoutData(gridData);

        final Label separator = new Label(m_shell, SWT.SEPARATOR | SWT.HORIZONTAL);
        separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        final Composite environmentNameContainer = new Composite(m_shell, SWT.NONE);
        environmentNameContainer.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));
        environmentNameContainer.setLayout(new GridLayout(2, false));

        // Environment name:

        final Label environmentNameTextBoxLabel = new Label(environmentNameContainer, SWT.NONE);
        environmentNameTextBoxLabel.setText("New environment's name");
        environmentNameTextBoxLabel.setLayoutData(new GridData());

        m_environmentNameTextBox = new Text(environmentNameContainer, SWT.BORDER);
        m_environmentNameTextBox.setEnabled(false);
        m_environmentNameTextBox.setText(ENVIRONMENT_NAME_PLACEHOLDER);
        m_environmentNameTextBox.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        // Progress monitoring widgets:

        final Composite installationMonitorContainer = new Composite(m_shell, SWT.NONE);
        installationMonitorContainer.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
        installationMonitorContainer.setLayout(new GridLayout());

        m_statusLabel = new Label(installationMonitorContainer, SWT.WRAP);
        m_statusLabel.setText("Please click '" + CREATE_CPU_BUTTON_TEXT + "' or '" + CREATE_GPU_BUTTON_TEXT
            + "' to start. You can specify a custom environment name using the text field above before starting.");
        gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        gridData.widthHint = DESCRIPTION_LABEL_WIDTH_HINTS;
        gridData.verticalIndent = 10;
        m_statusLabel.setLayoutData(gridData);

        final Composite progressBarContainer = new Composite(installationMonitorContainer, SWT.NONE);
        progressBarContainer.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));
        m_progressBarStackLayout = new StackLayout();
        progressBarContainer.setLayout(m_progressBarStackLayout);
        m_determinateProgressBar = new ProgressBar(progressBarContainer, SWT.SMOOTH);
        m_indeterminateProgressBar = new ProgressBar(progressBarContainer, SWT.SMOOTH | SWT.INDETERMINATE);
        m_progressBarStackLayout.topControl = m_determinateProgressBar;

        m_errorTextBoxLabel = new Label(installationMonitorContainer, SWT.NONE);
        m_errorTextBoxLabel.setText("Conda error log");
        gridData = new GridData();
        gridData.verticalIndent = 10;
        m_errorTextBoxLabel.setLayoutData(gridData);

        m_errorTextBoxContainer = new Composite(installationMonitorContainer, SWT.NONE);
        gridData = new GridData(GridData.FILL, GridData.FILL, true, true);
        gridData.heightHint = 80;
        m_errorTextBoxContainer.setLayoutData(gridData);
        m_errorTextBoxContainer.setLayout(new FillLayout());
        m_errorTextBox =
            new Text(m_errorTextBoxContainer, SWT.READ_ONLY | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL | SWT.BORDER);
        final Color red = new Color(m_errorTextBoxContainer.getDisplay(), 255, 0, 0);
        m_errorTextBox.setForeground(red);
        m_errorTextBox.addDisposeListener(e -> red.dispose());

        m_determinateProgressBar.setEnabled(false);
        m_errorTextBox.setEnabled(false);

        // Hide error log initially.
        m_errorTextBoxLabel.setVisible(false);
        ((GridData)m_errorTextBoxLabel.getLayoutData()).exclude = true;
        m_errorTextBoxContainer.setVisible(false);
        ((GridData)m_errorTextBoxContainer.getLayoutData()).exclude = true;

        // --

        final Composite buttonContainer = new Composite(m_shell, SWT.NONE);
        gridData = new GridData();
        gridData.horizontalAlignment = SWT.RIGHT;
        gridData.verticalIndent = 15;
        buttonContainer.setLayoutData(gridData);
        buttonContainer.setLayout(new RowLayout());
        m_createOrRetryCPUButton = new Button(buttonContainer, SWT.NONE);
        m_createOrRetryCPUButton.setText(CREATE_CPU_BUTTON_TEXT);
        m_createOrRetryCPUButton.setEnabled(false);
        m_createOrRetryGPUButton = new Button(buttonContainer, SWT.NONE);
        m_createOrRetryGPUButton.setText(CREATE_GPU_BUTTON_TEXT);
        m_createOrRetryGPUButton.setEnabled(false);
        m_cancelButton = new Button(buttonContainer, SWT.NONE);
        m_cancelButton.setText("Cancel");

        // Initialize environment name: we're calling Conda here, so we should do this asynchronously.
        new Thread(() -> {
            final String defaultEnvironmentName = m_environmentCreator.getDefaultEnvironmentName();
            switchToDefaultEnvironmentNameAvailableState(defaultEnvironmentName);
        }).start();

        // Internal hooks:

        m_createOrRetryCPUButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(final SelectionEvent e) {
                switchToStartingOrRetryingState(false);
            }

            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
                widgetSelected(e);
            }
        });
        m_createOrRetryGPUButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(final SelectionEvent e) {
                switchToStartingOrRetryingState(true);
            }

            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
                widgetSelected(e);
            }
        });
        m_cancelButton.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(final SelectionEvent e) {
                if (m_status != null && !m_environmentCreationTerminated) {
                    m_environmentCreator.cancelEnvironmentCreation(m_status);
                    // Shell will be closed by the environment status listener's cancellation handler.
                } else {
                    m_shell.close();
                }
            }

            @Override
            public void widgetDefaultSelected(final SelectionEvent e) {
                widgetSelected(e);
            }
        });
        m_shell.addShellListener(new ShellListener() {

            @Override
            public void shellIconified(final ShellEvent e) {
                // no-op
            }

            @Override
            public void shellDeiconified(final ShellEvent e) {
                // no-op
            }

            @Override
            public void shellDeactivated(final ShellEvent e) {
                // no-op
            }

            @Override
            public void shellClosed(final ShellEvent e) {
                if (m_status != null && !m_environmentCreationTerminated) {
                    m_environmentCreator.cancelEnvironmentCreation(m_status);
                    // Shell will be closed by the environment status listener's cancellation/finish handlers.
                    e.doit = false;
                }
            }

            @Override
            public void shellActivated(final ShellEvent e) {
                // no-op
            }
        });
    }

    @Override
    public void open() {
        try {
            m_shell.open();
            final Display display = getParent().getDisplay();
            while (!m_shell.isDisposed()) {
                if (!display.readAndDispatch()) {
                    display.sleep();
                }
            }
        } finally {
            unregisterExternalHooks();
        }
    }

    /**
     * May be followed by starting state.
     */
    private void switchToDefaultEnvironmentNameAvailableState(final String defaultEnvironmentName) {
        performActionOnWidgetInUiThread(m_environmentNameTextBox, () -> {
            m_environmentNameTextBox.setText(defaultEnvironmentName);
            m_environmentNameTextBox.setEnabled(true);
            m_environmentNameTextBox.setFocus();
            m_environmentNameTextBox.getParent().layout();
            m_createOrRetryCPUButton.setEnabled(true);
            enableCreateGPUButton();
            return null;
        }, false);
    }

    /**
     * May be followed by finished, canceled, or failed state.
     */
    private void switchToStartingOrRetryingState(final boolean gpu) {
        // If retrying.
        m_environmentCreationTerminated = false;
        m_statusLabel.setForeground(null);
        m_errorTextBox.setText("");
        unregisterExternalHooks();

        m_environmentNameTextBox.setEnabled(false);
        m_determinateProgressBar.setEnabled(true);
        m_progressBarStackLayout.topControl = m_indeterminateProgressBar;
        m_errorTextBox.setEnabled(true);
        m_createOrRetryCPUButton.setEnabled(false);
        m_createOrRetryGPUButton.setEnabled(false);
        String environmentName = m_environmentNameTextBox.getText();
        if (environmentName.isEmpty() || environmentName.equals(ENVIRONMENT_NAME_PLACEHOLDER)) {
            // While the creator can handle empty environments names, we should also sync the dialog to let the
            // user know which name we chose to overrule their empty text field input.
            environmentName = m_environmentCreator.getDefaultEnvironmentName();
            m_environmentNameTextBox.setText(environmentName);
        }
        m_shell.layout(true, true);
        m_status = new CondaEnvironmentCreationStatus();
        registerExternalHooks();
        m_environmentCreator.startEnvironmentCreation(environmentName, m_status, gpu);
    }

    /**
     * May be followed by starting state ("retry") or no state (terminal state).
     */
    private void switchToFailedState() {
        m_environmentCreationTerminated = true;
        performActionOnWidgetInUiThread(m_shell, () -> {
            if (!m_statusLabel.isDisposed()) {
                final Color red = new Color(m_statusLabel.getDisplay(), 255, 0, 0);
                m_statusLabel.setForeground(red);
                m_statusLabel.addDisposeListener(e -> red.dispose());
            }
            if (!m_determinateProgressBar.isDisposed()) {
                m_determinateProgressBar.setSelection(0);
                m_determinateProgressBar.setEnabled(false);
                m_progressBarStackLayout.topControl = m_determinateProgressBar;
            }
            if (!m_indeterminateProgressBar.isDisposed()) {
                m_indeterminateProgressBar.setEnabled(false);
            }
            // Prepare for retry.
            if (!m_environmentNameTextBox.isDisposed()) {
                m_environmentNameTextBox.setEnabled(true);
            }
            if (!m_createOrRetryCPUButton.isDisposed()) {
                m_createOrRetryCPUButton.setText(RETRY_CPU_BUTTON_TEXT);
                m_createOrRetryCPUButton.setEnabled(true);
            }
            if (!m_createOrRetryGPUButton.isDisposed()) {
                m_createOrRetryGPUButton.setText(RETRY_GPU_BUTTON_TEXT);
                enableCreateGPUButton();
            }
            m_shell.layout(true, true);
            return null;
        }, false);
    }

    private void enableCreateGPUButton() {
        if (!SystemUtils.IS_OS_MAC) {
            m_createOrRetryGPUButton.setEnabled(true);
        }
    }

    /**
     * Terminal state.
     */
    private void switchToFinishedState() {
        m_environmentCreationTerminated = true;
        performActionOnWidgetInUiThread(m_shell, () -> {
            m_shell.close();
            return null;
        }, false);
    }

    /**
     * Terminal state.
     */
    private void switchToCanceledState() {
        m_environmentCreationTerminated = true;
        performActionOnWidgetInUiThread(m_shell, () -> {
            m_shell.close();
            return null;
        }, false);
    }

    private void registerExternalHooks() {
        m_status.getStatusMessage().addChangeListener(this::updateStatusMessage);
        m_status.getProgress().addChangeListener(this::updateProgress);
        m_status.getErrorLog().addChangeListener(this::updateErrorLog);
        m_statusChangeListener = new CondaEnvironmentCreationStatusListener() {

            @Override
            public void condaEnvironmentCreationStarting(final CondaEnvironmentCreationStatus status) {
                // no-op
            }

            @Override
            public void condaEnvironmentCreationFinished(final CondaEnvironmentCreationStatus status,
                final CondaEnvironmentIdentifier createdEnvironment) {
                if (status == m_status) {
                    switchToFinishedState();
                }
            }

            @Override
            public void condaEnvironmentCreationCanceled(final CondaEnvironmentCreationStatus status) {
                if (status == m_status) {
                    switchToCanceledState();
                }
            }

            @Override
            public void condaEnvironmentCreationFailed(final CondaEnvironmentCreationStatus status,
                final String errorMessage) {
                if (status == m_status) {
                    switchToFailedState();
                }
            }
        };
        // Prepend to close dialog before installation tests on the preference page are triggered.
        m_environmentCreator.addEnvironmentCreationStatusListener(m_statusChangeListener, true);
    }

    private void unregisterExternalHooks() {
        if (m_status != null) {
            m_status.getStatusMessage().removeChangeListener(this::updateStatusMessage);
            m_status.getProgress().removeChangeListener(this::updateProgress);
            m_status.getErrorLog().removeChangeListener(this::updateErrorLog);
        }
        m_environmentCreator.removeEnvironmentCreationStatusListener(m_statusChangeListener);
    }

    private void updateStatusMessage(@SuppressWarnings("unused") final ChangeEvent e) {
        performActionOnWidgetInUiThread(m_statusLabel, () -> {
            m_statusLabel.setText(m_status.getStatusMessage().getStringValue());
            m_statusLabel.requestLayout();
            return null;
        }, true);
    }

    private void updateProgress(@SuppressWarnings("unused") final ChangeEvent e) {
        final int progress = m_status.getProgress().getIntValue();
        Control newVisibleProgressBar;
        if (progress < 100) {
            performActionOnWidgetInUiThread(m_determinateProgressBar, () -> {
                m_determinateProgressBar.setSelection(progress);
                m_determinateProgressBar.requestLayout();
                return null;
            }, true);
            newVisibleProgressBar = m_determinateProgressBar;
        } else {
            newVisibleProgressBar = m_indeterminateProgressBar;
        }
        if (m_progressBarStackLayout.topControl != newVisibleProgressBar) {
            m_progressBarStackLayout.topControl = newVisibleProgressBar;
            performActionOnWidgetInUiThread(m_shell, () -> {
                m_shell.layout(true, true);
                return null;
            }, true);
        }
    }

    private void updateErrorLog(@SuppressWarnings("unused") final ChangeEvent e) {
        performActionOnWidgetInUiThread(m_shell, () -> {
            if (!m_errorTextBox.isDisposed()) {
                m_errorTextBox.setText(m_status.getErrorLog().getStringValue());
                m_errorTextBox.requestLayout();
                if (!m_errorTextBoxContainer.isDisposed() && !m_errorTextBoxContainer.getVisible()) {
                    // Show error log if it is hidden (which is the initial state).
                    if (!m_errorTextBoxLabel.isDisposed()) {
                        m_errorTextBoxLabel.setVisible(true);
                        ((GridData)m_errorTextBoxLabel.getLayoutData()).exclude = false;
                    }
                    m_errorTextBoxContainer.setVisible(true);
                    final GridData errorTextBoxContainerGridData = (GridData)m_errorTextBoxContainer.getLayoutData();
                    errorTextBoxContainerGridData.exclude = false;
                    m_shell.layout(true, true);
                    // Manually resize shell to show error log as if it wasn't hidden in the first place.
                    final Point oldSize = m_shell.getSize();
                    final Point newSize =
                        m_shell.computeSize(oldSize.x, oldSize.y + errorTextBoxContainerGridData.heightHint, true);
                    m_shell.setSize(newSize);
                }
            }
            return null;
        }, true);
    }
}
