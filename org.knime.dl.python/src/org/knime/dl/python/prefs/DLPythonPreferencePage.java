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
package org.knime.dl.python.prefs;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.knime.core.node.NodeLogger;
import org.knime.python2.PythonKernelTester.PythonKernelTestResult;
import org.knime.python2.PythonVersion;
import org.knime.python2.config.AbstractPythonConfigsObserver.PythonConfigsInstallationTestStatusChangeListener;
import org.knime.python2.config.PythonConfigStorage;
import org.knime.python2.config.PythonEnvironmentType;
import org.knime.python2.config.PythonEnvironmentTypeConfig;
import org.knime.python2.config.SerializerConfig;
import org.knime.python2.prefs.PythonEnvironmentTypePreferencePanel;
import org.knime.python2.prefs.PythonPreferencePage;
import org.knime.python2.prefs.PythonPreferenceUtils;
import org.knime.python2.prefs.SerializerPreferencePanel;

/**
 * Preference page for configurations related to the deep-learning python setup.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 */
public class DLPythonPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

    private static final String INFO_MESSAGE =
        "See <a href=\"https://docs.knime.com/latest/deep_learning_installation_guide/index.html\">"
            + "this guide</a> for details on how to setup the KNIME Deep Learning Integration.";

    private ScrolledComposite m_containerScrolledView;

    private Composite m_container;

    private Group m_environmentConfigurationGroup;

    private StackLayout m_environmentConfigurationLayout;

    private DLCondaEnvironmentPreferencePanel m_condaEnvironmentPanel;

    private DLManualEnvironmetPreferencePanel m_manualEnvironmentPanel;

    private Config m_config;

    private SerializerPreferencePanel m_serializerPanel;

    private DLPythonConfigsObserver m_configObserver;

    @Override
    public void init(final IWorkbench workbench) {
        // Nothing to do
    }

    @Override
    protected Control createContents(final Composite parent) {
        createPageBody(parent);
        createInfoHeader(parent);
        m_config = new Config();

        // Config selection (Python or own dl preferences):
        @SuppressWarnings("unused")
        Object unused = new DLPythonConfigSelectionPanel(m_config.m_configSelection, m_container);

        m_environmentConfigurationGroup = new Group(m_container, SWT.NONE);
        m_environmentConfigurationGroup.setText("Deep Learning Python environment configuration");
        m_environmentConfigurationGroup.setLayout(new GridLayout());
        m_environmentConfigurationGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        // Environment type selection:
        @SuppressWarnings("unused") // Reference to object is not needed here; everything is done in its constructor.
        final Object unused1 =
            new PythonEnvironmentTypePreferencePanel(m_config.m_envType, m_environmentConfigurationGroup);
        final Label separator = new Label(m_environmentConfigurationGroup, SWT.SEPARATOR | SWT.HORIZONTAL);
        separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        final Composite environmentConfigurationPanel = new Composite(m_environmentConfigurationGroup, SWT.NONE);
        m_environmentConfigurationLayout = new StackLayout();
        environmentConfigurationPanel.setLayout(m_environmentConfigurationLayout);
        environmentConfigurationPanel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        // Conda environment configuration, including environment creation dialogs:
        final DLCondaEnvironmentCreationObserver pythonEnvironmentCreator =
            new DLCondaEnvironmentCreationObserver(m_config.m_condaEnv.getCondaDirectoryPath());

        m_condaEnvironmentPanel = new DLCondaEnvironmentPreferencePanel(m_config.m_condaEnv, pythonEnvironmentCreator,
            environmentConfigurationPanel);

        // Manual environment configuration:
        m_manualEnvironmentPanel =
            new DLManualEnvironmetPreferencePanel(m_config.m_manualEnv, environmentConfigurationPanel);

        m_serializerPanel = new SerializerPreferencePanel(m_config.m_serializer, m_container);

        // Load config
        m_config.load();
        updateConfigSelection();
        updateEnvironmentType();

        // Hooks
        m_config.m_envType.getEnvironmentType().addChangeListener(e -> updateEnvironmentType());
        m_config.m_configSelection.getConfigSelection().addChangeListener(e -> updateConfigSelection());

        m_configObserver = new DLPythonConfigsObserver(m_config.m_configSelection, m_config.m_envType,
            m_config.m_condaEnv, pythonEnvironmentCreator, m_config.m_manualEnv, m_config.m_serializer);

        m_configObserver.addConfigsTestStatusListener(new PythonConfigsInstallationTestStatusChangeListener() {

            @Override
            public void environmentInstallationTestStarting(final PythonEnvironmentType environmentType,
                final PythonVersion pythonVersion) {
                updateDisplayMinSize();
            }

            @Override
            public void environmentInstallationTestFinished(final PythonEnvironmentType environmentType,
                final PythonVersion pythonVersion, final PythonKernelTestResult testResult) {
                updateDisplayMinSize();
            }

            @Override
            public void condaInstallationTestStarting() {
                updateDisplayMinSize();
            }

            @Override
            public void condaInstallationTestFinished(final String errorMessage) {
                updateDisplayMinSize();
            }
        });

        // Initial installation tests
        m_configObserver.testCurrentPreferences();

        return m_containerScrolledView;
    }

    private void createInfoHeader(final Composite parent) {
        final Link startScriptInfo = new Link(m_container, SWT.NONE);
        startScriptInfo.setLayoutData(new GridData());
        startScriptInfo.setText(INFO_MESSAGE);
        final Color gray = new Color(parent.getDisplay(), 100, 100, 100);
        startScriptInfo.setForeground(gray);
        startScriptInfo.addDisposeListener(e -> gray.dispose());
        startScriptInfo.setFont(JFaceResources.getFontRegistry().getItalic(""));
        startScriptInfo.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(final SelectionEvent e) {
                try {
                    PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(e.text));
                } catch (PartInitException | MalformedURLException ex) {
                    NodeLogger.getLogger(PythonPreferencePage.class).error(ex);
                }
            }
        });
    }

    private void createPageBody(final Composite parent) {
        m_containerScrolledView = new ScrolledComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL);
        m_container = new Composite(m_containerScrolledView, SWT.NONE);
        final GridLayout gridLayout = new GridLayout();
        gridLayout.marginWidth = 0;
        gridLayout.marginHeight = 0;
        m_container.setLayout(gridLayout);

        m_containerScrolledView.setContent(m_container);
        m_containerScrolledView.setExpandHorizontal(true);
        m_containerScrolledView.setExpandVertical(true);
    }

    private void updateEnvironmentType() {
        final PythonEnvironmentType environmentType =
            PythonEnvironmentType.fromId(m_config.m_envType.getEnvironmentType().getStringValue());
        if (PythonEnvironmentType.CONDA.equals(environmentType)) {
            m_environmentConfigurationLayout.topControl = m_condaEnvironmentPanel.getPanel();
        } else if (PythonEnvironmentType.MANUAL.equals(environmentType)) {
            m_environmentConfigurationLayout.topControl = m_manualEnvironmentPanel.getPanel();
        } else {
            throw new IllegalStateException(
                "Selected Python environment type is neither Conda nor manual. This is an implementation error.");
        }
        updateDisplayMinSize();
    }

    private void updateConfigSelection() {
        final DLPythonConfigSelection configSelection =
            DLPythonConfigSelection.fromId(m_config.m_configSelection.getConfigSelection().getStringValue());
        if (DLPythonConfigSelection.PYTHON.equals(configSelection)) {
            enableEnvConfig(false);
        } else if (DLPythonConfigSelection.DL.equals(configSelection)) {
            enableEnvConfig(true);
        } else {
            throw new IllegalStateException(
                "Selected config selection is neither Python nor deep learning. This is an implementation error.");
        }
    }

    private void enableEnvConfig(final boolean enabled) {
        enable(m_environmentConfigurationGroup, enabled);
        enable(m_serializerPanel.getPanel(), enabled);
    }

    private static void enable(final Control control, final boolean enabled) {
        control.setEnabled(enabled);
        if (control instanceof Composite) {
            for (final Control c : ((Composite)control).getChildren()) {
                enable(c, enabled);
            }
        }
    }

    private void updateDisplayMinSize() {
        PythonPreferenceUtils.performActionOnWidgetInUiThread(getControl(), () -> {
            m_container.layout(true, true);
            m_containerScrolledView.setMinSize(m_container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
            return null;
        }, false);
    }

    @Override
    public boolean performOk() {
        m_config.save();
        return true;
    }

    @Override
    protected void performApply() {
        m_config.save();
        m_configObserver.testCurrentPreferences();
    }

    @Override
    protected void performDefaults() {
        m_config.loadDefaults();
    }

    private static final class Config {

        private final DLPythonConfigSelectionConfig m_configSelection = new DLPythonConfigSelectionConfig();

        private final PythonEnvironmentTypeConfig m_envType = new PythonEnvironmentTypeConfig();

        private final DLCondaEnvironmentConfig m_condaEnv = new DLCondaEnvironmentConfig();

        private final DLManualEnvironmentConfig m_manualEnv = new DLManualEnvironmentConfig();

        private final SerializerConfig m_serializer = new SerializerConfig();

        private void save() {
            final PythonConfigStorage currentPrefs = DLPythonPreferences.CURRENT;
            m_configSelection.saveConfigTo(currentPrefs);
            m_envType.saveConfigTo(currentPrefs);
            m_condaEnv.saveConfigTo(currentPrefs);
            m_manualEnv.saveConfigTo(currentPrefs);
            m_serializer.saveConfigTo(currentPrefs);
        }

        private void load() {
            final PythonConfigStorage currentPrefs = DLPythonPreferences.CURRENT;
            m_configSelection.loadConfigFrom(currentPrefs);
            m_envType.loadConfigFrom(currentPrefs);
            m_condaEnv.loadConfigFrom(currentPrefs);
            m_manualEnv.loadConfigFrom(currentPrefs);
            m_serializer.loadConfigFrom(currentPrefs);
        }

        private void loadDefaults() {
            m_configSelection.loadDefaults();
            m_envType.getEnvironmentType().setStringValue(PythonEnvironmentTypeConfig.DEFAULT_ENVIRONMENT_TYPE);
            m_condaEnv.loadDefaults();
            m_manualEnv.loadDefaults();
            m_serializer.getSerializer().setStringValue(SerializerConfig.DEFAULT_SERIALIZER);
            m_serializer.getSerializerError().setStringValue("");
        }
    }
}
