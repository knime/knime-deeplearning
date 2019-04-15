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
import org.knime.python2.PythonVersion;
import org.knime.python2.config.CondaEnvironmentCreationObserver;
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

    private StackLayout m_environmentConfigurationLayout;

    private DLCondaEnvironmentPreferencePanel m_condaEnvironmentPanel;

    private DLManualEnvironmetPreferencePanel m_manualEnvironmentPanel;

    private Config m_config;

    @Override
    public void init(final IWorkbench workbench) {
        // Nothing to do
    }

    @Override
    protected Control createContents(final Composite parent) {
        createPageBody(parent);
        createInfoHeader(parent);
        m_config = new Config();

        // TODO Radio button for using the python configuration (should be activate by default)

        // Environment configuration:

        final Group environmentConfigurationGroup = new Group(m_container, SWT.NONE);
        environmentConfigurationGroup.setText("Python environment configuration");
        environmentConfigurationGroup.setLayout(new GridLayout());
        environmentConfigurationGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        // Environment type selection:

        @SuppressWarnings("unused") // Reference to object is not needed here; everything is done in its constructor.
        final Object unused1 =
            new PythonEnvironmentTypePreferencePanel(m_config.m_envType, environmentConfigurationGroup);
        final Label separator = new Label(environmentConfigurationGroup, SWT.SEPARATOR | SWT.HORIZONTAL);
        separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        final Composite environmentConfigurationPanel = new Composite(environmentConfigurationGroup, SWT.NONE);
        m_environmentConfigurationLayout = new StackLayout();
        environmentConfigurationPanel.setLayout(m_environmentConfigurationLayout);
        environmentConfigurationPanel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        // Conda environment configuration, including environment creation dialogs:

        final CondaEnvironmentCreationObserver python3EnvironmentCreator =
            new CondaEnvironmentCreationObserver(PythonVersion.PYTHON3, m_config.m_condaEnv.getCondaDirectoryPath());

        m_condaEnvironmentPanel = new DLCondaEnvironmentPreferencePanel(m_config.m_condaEnv, python3EnvironmentCreator,
            environmentConfigurationPanel);

        // Manual environment configuration:

        m_manualEnvironmentPanel =
            new DLManualEnvironmetPreferencePanel(m_config.m_manualEnv, environmentConfigurationPanel);

        // Serializer selection:

        @SuppressWarnings("unused") // Reference to object is not needed here; everything is done in its constructor.
        Object unused2 = new SerializerPreferencePanel(m_config.m_serializer, m_container);

        // Load config
        m_config.load();
        displayPanelForEnvironmentType(m_config.m_envType.getEnvironmentType().getStringValue());

        // Hooks

        m_config.m_envType.getEnvironmentType().addChangeListener(
            e -> displayPanelForEnvironmentType(m_config.m_envType.getEnvironmentType().getStringValue()));

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

    private void displayPanelForEnvironmentType(final String environmentTypeId) {
        final PythonEnvironmentType environmentType = PythonEnvironmentType.fromId(environmentTypeId);
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

    private void updateDisplayMinSize() {
        PythonPreferenceUtils.performActionOnWidgetInUiThread(getControl(), () -> {
            m_container.layout(true, true);
            m_containerScrolledView.setMinSize(m_container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
            return null;
        }, false);
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

    @Override
    public boolean performOk() {
        m_config.save();
        return true;
    }

    @Override
    protected void performApply() {
        m_config.save();
        // TODO test current preferences
        //m_configObserver.testCurrentPreferences();
    }

    @Override
    protected void performDefaults() {
        m_config.loadDefaults();
    }

    private static final class Config {

        private final PythonEnvironmentTypeConfig m_envType = new PythonEnvironmentTypeConfig();

        private final DLCondaEnvironmentConfig m_condaEnv = new DLCondaEnvironmentConfig();

        private final DLManualEnvironmentConfig m_manualEnv = new DLManualEnvironmentConfig();

        private final SerializerConfig m_serializer = new SerializerConfig();

        private void save() {
            final PythonConfigStorage currentPrefs = DLPythonPreferences.CURRENT;
            m_envType.saveConfigTo(currentPrefs);
            m_condaEnv.saveConfigTo(currentPrefs);
            m_manualEnv.saveConfigTo(currentPrefs);
            m_serializer.saveConfigTo(currentPrefs);
        }

        private void load() {
            final PythonConfigStorage currentPrefs = DLPythonPreferences.CURRENT;
            m_envType.loadConfigFrom(currentPrefs);
            m_condaEnv.loadConfigFrom(currentPrefs);
            m_manualEnv.loadConfigFrom(currentPrefs);
            m_serializer.loadConfigFrom(currentPrefs);
        }

        private void loadDefaults() {
            m_envType.getEnvironmentType().setStringValue(PythonEnvironmentTypeConfig.DEFAULT_ENVIRONMENT_TYPE);
            m_condaEnv.loadDefaults();
            m_manualEnv.loadDefaults();
            m_serializer.getSerializer().setStringValue(SerializerConfig.DEFAULT_SERIALIZER);
            m_serializer.getSerializerError().setStringValue("");
        }
    }
}
