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

import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;
import org.knime.python2.Conda;
import org.knime.python2.PythonCommand;
import org.knime.python2.config.AbstractPythonEnvironmentConfig;
import org.knime.python2.config.PythonConfigStorage;
import org.knime.python2.prefs.PythonPreferences;

/**
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 */
public class DLCondaEnvironmentConfig extends AbstractPythonEnvironmentConfig {

    private static final String CFG_KEY_CONDA_DIRECTORY_PATH = "condaDirectoryPath";

    private static final String CFG_KEY_CONDA_ENV_NAME = "condaEnvironmentName";

    private static final String CFG_KEY_DUMMY = "dummy";

    private static final String DEFAULT_ENVIRONMENT_NAME = "<no environment>";

    private final SettingsModelString m_environmentName;

    private final SettingsModelString m_condaDirectory;

    private final SettingsModelStringArray m_availableEnvironments =
        new SettingsModelStringArray(CFG_KEY_DUMMY, new String[]{DEFAULT_ENVIRONMENT_NAME});

    private final SettingsModelString m_condaInstallationInfo = new SettingsModelString(CFG_KEY_DUMMY, "");

    private final SettingsModelString m_condaInstallationError = new SettingsModelString(CFG_KEY_DUMMY, "");

    /**
     * Create a new conda environment config for deep learning.
     */
    DLCondaEnvironmentConfig() {
        // Configuration
        m_condaDirectory =
            new SettingsModelString(CFG_KEY_CONDA_DIRECTORY_PATH, PythonPreferences.getCondaInstallationPath());
        m_environmentName = new SettingsModelString(CFG_KEY_CONDA_ENV_NAME, DEFAULT_ENVIRONMENT_NAME);
    }

    @Override
    public PythonCommand getPythonCommand() {
        return Conda.createPythonCommand(m_condaDirectory.getStringValue(), m_environmentName.getStringValue());
    }

    /**
     * @return The path to the conda directory
     */
    public SettingsModelString getCondaDirectoryPath() {
        return m_condaDirectory;
    }

    /**
     * @return The name of the Python Conda environment.
     */
    public SettingsModelString getEnvironmentName() {
        return m_environmentName;
    }

    /**
     * @return The list of currently available Python Conda environments. Not meant for saving/loading.
     */
    public SettingsModelStringArray getAvailableEnvironmentNames() {
        return m_availableEnvironments;
    }

    /**
     * @return The installation status message of the local Conda installation. Not meant for saving/loading.
     */
    public SettingsModelString getCondaInstallationInfo() {
        return m_condaInstallationInfo;
    }

    /**
     * @return The installation error message of the local Conda installation. Not meant for saving/loading.
     */
    public SettingsModelString getCondaInstallationError() {
        return m_condaInstallationError;
    }

    @Override
    public void saveConfigTo(final PythonConfigStorage storage) {
        storage.saveStringModel(m_condaDirectory);
        storage.saveStringModel(m_environmentName);
    }

    @Override
    public void loadConfigFrom(final PythonConfigStorage storage) {
        storage.loadStringModel(m_condaDirectory);
        storage.loadStringModel(m_environmentName);
    }
}
