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
import java.util.ArrayList;
import java.util.List;

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
import org.knime.python2.config.PythonConfig;
import org.knime.python2.config.PythonEnvironmentType;
import org.knime.python2.config.PythonEnvironmentTypeConfig;
import org.knime.python2.prefs.PythonEnvironmentTypePreferencePanel;
import org.knime.python2.prefs.PythonPreferencePage;

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

    private PythonEnvironmentTypeConfig m_environmentTypeConfig;

    private List<PythonConfig> m_configs;

    private StackLayout m_environmentConfigurationLayout;

    private DLCondaEnvironmentPreferencePanel m_condaEnvironmentPanel;

    private DLManualEnvironmetPreferencePanel m_manualEnvironmentPanel;

    @Override
    public void init(final IWorkbench workbench) {
        // Nothing to do
    }

    @Override
    protected Control createContents(final Composite parent) {
        createPageBody(parent);
        createInfoHeader(parent);

        m_configs = new ArrayList<>(5);

        // TODO Radio button for using the python configuration (should be activate by default)

        // Environment configuration:

        final Group environmentConfigurationGroup = new Group(m_container, SWT.NONE);
        environmentConfigurationGroup.setText("Python environment configuration");
        environmentConfigurationGroup.setLayout(new GridLayout());
        environmentConfigurationGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        // Environment type selection:

        m_environmentTypeConfig = new PythonEnvironmentTypeConfig();
        m_configs.add(m_environmentTypeConfig);
        @SuppressWarnings("unused") // Reference to object is not needed here; everything is done in its constructor.
        final Object unused1 =
            new PythonEnvironmentTypePreferencePanel(m_environmentTypeConfig, environmentConfigurationGroup);
        final Label separator = new Label(environmentConfigurationGroup, SWT.SEPARATOR | SWT.HORIZONTAL);
        separator.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        final Composite environmentConfigurationPanel = new Composite(environmentConfigurationGroup, SWT.NONE);
        m_environmentConfigurationLayout = new StackLayout();
        environmentConfigurationPanel.setLayout(m_environmentConfigurationLayout);
        environmentConfigurationPanel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        // Conda environment configuration, including environment creation dialogs:

        final DLCondaEnvironmentConfig condaEnvironmentsConfig = new DLCondaEnvironmentConfig();
        m_configs.add(condaEnvironmentsConfig);
        final CondaEnvironmentCreationObserver python3EnvironmentCreator = new CondaEnvironmentCreationObserver(
            PythonVersion.PYTHON3, condaEnvironmentsConfig.getCondaDirectoryPath());

        m_condaEnvironmentPanel = new DLCondaEnvironmentPreferencePanel(condaEnvironmentsConfig,
            python3EnvironmentCreator, environmentConfigurationPanel);

        // Manual environment configuration:

        final DLManualEnvironmentConfig manualEnvironmentConfig = new DLManualEnvironmentConfig();
        m_configs.add(manualEnvironmentConfig);
        m_manualEnvironmentPanel =
            new DLManualEnvironmetPreferencePanel(manualEnvironmentConfig, environmentConfigurationPanel);


        // Hooks

        m_environmentTypeConfig.getEnvironmentType().addChangeListener(
            e -> displayPanelForEnvironmentType(m_environmentTypeConfig.getEnvironmentType().getStringValue()));

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
        // saveConfigurations();
        return true;
    }

    @Override
    protected void performApply() {
        // TODO
        // saveConfigurations();
        //m_configObserver.testCurrentPreferences();
    }

    // TODO defaults, load, save
}
