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

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.knime.python2.PythonVersion;
import org.knime.python2.config.AbstractPythonConfigPanel;
import org.knime.python2.prefs.CondaEnvironmentSelectionBox;
import org.knime.python2.prefs.StatusDisplayingFilePathEditor;

/**
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 */
final class DLCondaEnvironmentPreferencePanel extends AbstractPythonConfigPanel<DLCondaEnvironmentsConfig, Composite> {

    DLCondaEnvironmentPreferencePanel(final DLCondaEnvironmentsConfig config,
        final DLCondaEnvironmentCreationObserver kerasEnvironmentCreator,
        final DLCondaEnvironmentCreationObserver tf2EnvironmentCreator, final Composite parent) {
        super(config, parent);
        final Composite panel = getPanel();

        createCondaDirectoryPathPanel(config, panel);
        createKerasEnvironmentSelectionPanel(config.getKerasConfig(), kerasEnvironmentCreator, panel);
        createTF2EnvironmentSelectionPanel(config.getTF2Config(), tf2EnvironmentCreator, panel);
    }

    @Override
    protected Composite createPanel(final Composite parent) {
        final Composite panel = new Composite(parent, SWT.NONE);
        panel.setLayout(new GridLayout());
        return panel;
    }

    private static void createKerasEnvironmentSelectionPanel(final DLCondaEnvironmentConfig kerasConfig,
        final DLCondaEnvironmentCreationObserver kerasEnvironmentCreator, final Composite panel) {
        createEnvironmentSelectionPanel(DLPythonLibrarySelection.KERAS, kerasConfig, kerasEnvironmentCreator, panel);
    }

    private static void createTF2EnvironmentSelectionPanel(final DLCondaEnvironmentConfig tf2Config,
        final DLCondaEnvironmentCreationObserver tf2EnvironmentCreator, final Composite panel) {
        createEnvironmentSelectionPanel(DLPythonLibrarySelection.TF2, tf2Config, tf2EnvironmentCreator, panel);
    }

    private static void createEnvironmentSelectionPanel(final DLPythonLibrarySelection envType,
        final DLCondaEnvironmentConfig config, final DLCondaEnvironmentCreationObserver pythonEnvironmentCreator,
        final Composite panel) {
        final CondaEnvironmentSelectionBox environmentSelection =
            new CondaEnvironmentSelectionBox(PythonVersion.PYTHON3, config.getEnvironmentDirectory(),
                config.getAvailableEnvironments(), envType.getName(),
                "Name of the " + envType.getName() + " Conda environment", config.getPythonInstallationInfo(),
                config.getPythonInstallationWarning(), config.getPythonInstallationError(), pythonEnvironmentCreator,
                panel, shell -> new DLCondaEnvironmentCreationPreferenceDialog(pythonEnvironmentCreator, shell).open());
        final GridData gridData = new GridData();
        gridData.horizontalAlignment = SWT.FILL;
        gridData.horizontalIndent = 20;
        gridData.grabExcessHorizontalSpace = true;
        environmentSelection.setLayoutData(gridData);
    }

    private static void createCondaDirectoryPathPanel(final DLCondaEnvironmentsConfig config, final Composite panel) {
        final StatusDisplayingFilePathEditor directoryPathEditor = new StatusDisplayingFilePathEditor(
            config.getCondaDirectoryPath(), false, "Conda", "Path to the Conda installation directory",
            config.getCondaInstallationInfo(), config.getCondaInstallationError(), panel);
        final GridData gridData = new GridData();
        gridData.grabExcessHorizontalSpace = true;
        gridData.horizontalAlignment = SWT.FILL;
        directoryPathEditor.setLayoutData(gridData);
    }
}
