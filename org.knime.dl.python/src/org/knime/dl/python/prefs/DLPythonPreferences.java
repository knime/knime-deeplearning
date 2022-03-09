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

import java.util.Collection;

import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.knime.conda.prefs.CondaPreferences;
import org.knime.core.node.NodeLogger;
import org.knime.python2.PythonCommand;
import org.knime.python2.PythonModuleSpec;
import org.knime.python2.config.PythonConfigStorage;
import org.knime.python2.config.PythonEnvironmentType;
import org.knime.python2.config.PythonEnvironmentTypeConfig;
import org.knime.python2.config.SerializerConfig;
import org.knime.python2.extensions.serializationlibrary.SerializationLibraryExtensions;
import org.knime.python2.prefs.PreferenceStorage;
import org.knime.python2.prefs.PreferenceWrappingConfigStorage;
import org.knime.python2.prefs.PythonPreferences;
import org.osgi.service.prefs.BackingStoreException;

/**
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 */
public final class DLPythonPreferences {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(DLPythonPreferences.class);

    private static final String QUALIFIER = "org.knime.dl.python";

    private static final PreferenceStorage DEFAULT_SCOPE_PREFERENCES =
        new PreferenceStorage(QUALIFIER, DefaultScope.INSTANCE);

    private static final PreferenceStorage CURRENT_SCOPE_PREFERENCES =
        new PreferenceStorage(QUALIFIER, InstanceScope.INSTANCE, DefaultScope.INSTANCE);

    /**
     * Accessed by preference page.
     */
    static final PythonConfigStorage CURRENT = new PreferenceWrappingConfigStorage(CURRENT_SCOPE_PREFERENCES);

    /**
     * Accessed by preference page and preferences initializer.
     */
    static final PythonConfigStorage DEFAULT = new PreferenceWrappingConfigStorage(DEFAULT_SCOPE_PREFERENCES);

    private DLPythonPreferences() {
    }

    /**
     * @return the config selection which should be used (python v. dl)
     */
    public static DLPythonConfigSelection getConfigSelectionPreference() {
        final DLPythonConfigSelectionConfig configSelection = new DLPythonConfigSelectionConfig();
        configSelection.loadConfigFrom(CURRENT);
        return DLPythonConfigSelection.fromId(configSelection.getConfigSelection().getStringValue());
    }

    /**
     * @return <code>true</code> if the Python configuration should be used
     */
    public static boolean usePythonPreferences() {
        return DLPythonConfigSelection.PYTHON.equals(getConfigSelectionPreference());
    }

    /**
     * @return the config selection which library should be used for the DL Python nodes
     */
    public static DLPythonLibrarySelection getLibrarySelectionPreference() {
        final DLPythonLibrarySelectionConfig config = new DLPythonLibrarySelectionConfig();
        config.loadConfigFrom(CURRENT);
        return DLPythonLibrarySelection.fromId(config.getLibrarySelection().getStringValue());
    }

    /**
     * @return the currently selected Python environment type (Conda v. manual)
     */
    public static PythonEnvironmentType getEnvironmentTypePreference() {
        if (usePythonPreferences()) {
            return PythonPreferences.getEnvironmentTypePreference();
        } else {
            final PythonEnvironmentTypeConfig environmentTypeConfig = new PythonEnvironmentTypeConfig();
            environmentTypeConfig.loadConfigFrom(CURRENT);
            return PythonEnvironmentType.fromId(environmentTypeConfig.getEnvironmentType().getStringValue());
        }
    }

    /**
     * @return the currently selected default Python command. Use {@link #getPythonKerasCommandPreference()} or
     *         {@link #getPythonTF2CommandPreference()} to get the commands for Keras or TensorFlow 2.
     */
    public static PythonCommand getPythonCommandPreference() {
        return getPythonCommandFor(getLibrarySelectionPreference());
    }

    /**
     * @return the currently selected Python command for a Keras environment
     */
    public static PythonCommand getPythonKerasCommandPreference() {
        return getPythonCommandFor(DLPythonLibrarySelection.KERAS);
    }

    /**
     * @return the currently selected Python command for a TensorFlow 2 environment
     */
    public static PythonCommand getPythonTF2CommandPreference() {
        return getPythonCommandFor(DLPythonLibrarySelection.TF2);
    }

    /**
     * @return the currently selected serialization library
     */
    public static String getSerializerPreference() {
        if (usePythonPreferences()) {
            return PythonPreferences.getSerializerPreference();
        } else {
            final SerializerConfig serializerConfig = new SerializerConfig();
            serializerConfig.loadConfigFrom(CURRENT);
            return serializerConfig.getSerializer().getStringValue();
        }
    }

    /**
     * @return the required modules of the currently selected serialization library
     */
    public static Collection<PythonModuleSpec> getCurrentlyRequiredSerializerModules() {
        return SerializationLibraryExtensions.getSerializationLibraryFactory(getSerializerPreference())
            .getRequiredExternalModules();
    }

    /** @return the python command for the given environment selection */
    private static PythonCommand getPythonCommandFor(final DLPythonLibrarySelection envSelection) {
        if (usePythonPreferences()) {
            return PythonPreferences.getPython3CommandPreference();
        } else {
            final DLPythonEnvironmentsConfig envsConfig = getEnvironmentsConfig();
            if (DLPythonLibrarySelection.KERAS.equals(envSelection)) {
                return envsConfig.getKerasConfig().getPythonCommand();
            } else if (DLPythonLibrarySelection.TF2.equals(envSelection)) {
                return envsConfig.getTF2Config().getPythonCommand();
            } else {
                throw new IllegalStateException("Deep learning Python environment is neither Keras nor TensorFlow 2. "
                    + "This is an implementation error");
            }
        }
    }

    /** @return the selected environment configs (conda or manual). Only valid if DL is selected */
    private static DLPythonEnvironmentsConfig getEnvironmentsConfig() {
        final PythonEnvironmentType envType = getEnvironmentTypePreference();
        final DLPythonEnvironmentsConfig envsConfig;
        if (PythonEnvironmentType.CONDA.equals(envType)) {
            envsConfig = new DLCondaEnvironmentsConfig();
        } else if (PythonEnvironmentType.MANUAL.equals(envType)) {
            envsConfig = new DLManualEnvironmentsConfig();
        } else {
            throw new IllegalStateException("Selected deep learning Python environment is neither Conda nor manual. "
                + "This is an implementation error.");
        }
        envsConfig.loadConfigFrom(CURRENT);
        return envsConfig;
    }

    /**
     * Get the path to the Conda installation directory. If it is configured in the deep learning preferences this will
     * be used. Otherwise {@link CondaPreferences#getCondaInstallationDirectory()} will be used.
     *
     * @return the path to the Conda installation
     * @deprecated use {@link CondaPreferences#getCondaInstallationDirectory()}.
     */
    @Deprecated
    public static String getCondaInstallationPath() {
        /* CASES:
         * - Using Python prefs (#usePythonPreferences) or MANUAL: Use from Conda prefs + delete in DL prefs + INFO log  -- happens once
         * - Saved in DL prefs but equal to Conda prefs: Use from Conda prefs + delete in DL prefs + WARN log  -- happens once
         * - Saved in DL prefs and not equal to Conda prefs: Use from DL prefs + WARN log  -- happens each time this is called
         * - Set in DL default prefs (on KNIME Executor): Use from DL default prefs + WARN log  -- happens each time this is called
         * - Else: Use from Conda prefs
         */

        final String condaDirPrefKey = "condaDirectoryPath";

        final String condaDir = CondaPreferences.getCondaInstallationDirectory();

        if (!usePythonPreferences() && PythonEnvironmentType.CONDA.equals(getEnvironmentTypePreference())) {
            // Look in the instance scope
            // If present: The user has configured it in a previous version of the AP
            final String condaDirDLInstance = CURRENT_SCOPE_PREFERENCES.readString(condaDirPrefKey, null);
            if (condaDirDLInstance != null) {
                if (condaDirDLInstance.equals(condaDir)) {
                    // Delete the "condaDirectoryPath" preference
                    // It is equal to the preference on the Conda preference page and should be configured there
                    deleteCondaInstallationPathPref();
                    LOGGER.warn(
                        "The 'condaDirectoryPath' preference on the Python Deep Learning preference page is deprecated. "
                            + "The configured value was equal to the value on the Conda preference page. "
                            + "Therefore, the preference was deleted and the preference from the Conda preference page is used.");
                    return condaDir;
                }

                // The paths are not equal
                LOGGER.warn(
                    "The 'condaDirectoryPath' preference on the Python Deep Learning preference page is deprecated. " //
                        + "The configured value is '" + condaDirDLInstance + "'. " //
                        + "This Conda installation in this directory will be used in Deep Learning nodes. " //
                        + "The Conda installation path configured on the Conda preference page is '" + condaDir + "'. " //
                        + "If '" + condaDirDLInstance + "' points to the correct Conda installation directory, " //
                        + "please go to the Conda preference page and configure the directory there. " //
                        + "If '" + condaDir + "' points to the correct Conda installation directory, " //
                        + "please go to the Python Deep Learning preference page and follow the instructions there.");
                return condaDirDLInstance;
            }

            // Look in the default scope
            // We have not added it. If it is present it was added with preferences.epf on the Executor
            final String condaDirDLDefault = DEFAULT_SCOPE_PREFERENCES.readString(condaDirPrefKey, null);
            if (condaDirDLDefault != null) {
                LOGGER.warn(
                    "Using 'org.knime.dl.python/condaDirectoryPath' to configure the conda installation directory is deprecated. "
                        + "Please use 'org.knime.conda/condaDirectoryPath'.");
                return condaDirDLDefault;
            }
        } else {
            // Delete the "condaDirectoryPath" preference
            // It is not used and cannot be changed anymore. It should not be used in the future
            InstanceScope.INSTANCE.getNode(QUALIFIER).remove("condaDirectoryPath");
            LOGGER
                .info("The 'condaDirectoryPath' preference on the Python Deep Learning preference page is deprecated. "
                    + "Since the current configuration does not use it, it was deleted from the preferences.");
        }

        return condaDir;
    }

    /** Delete the condaDirectoryPath preference from the DL Python preferences */
    static void deleteCondaInstallationPathPref() {
        final IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(QUALIFIER);
        prefs.remove("condaDirectoryPath");
        try {
            prefs.flush();
        } catch (final BackingStoreException ex) {
            LOGGER.warn(
                "Failed to flush the Python Deep Learning preferences with the deleted path to conda installation.",
                ex);
        }
    }
}
