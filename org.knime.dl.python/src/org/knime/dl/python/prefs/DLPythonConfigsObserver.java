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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.util.Version;
import org.knime.dl.python.core.DLPythonModuleDependencyRegistry;
import org.knime.dl.python.prefs.DLTestStatusChangeListenerCollection.DLPythonConfigsInstallationTestStatusChangeListener;
import org.knime.python2.Conda;
import org.knime.python2.Conda.CondaEnvironmentSpec;
import org.knime.python2.PythonCommand;
import org.knime.python2.PythonKernelTester;
import org.knime.python2.PythonKernelTester.PythonKernelTestResult;
import org.knime.python2.PythonModuleSpec;
import org.knime.python2.config.AbstractCondaEnvironmentCreationObserver.CondaEnvironmentCreationStatus;
import org.knime.python2.config.AbstractCondaEnvironmentCreationObserver.CondaEnvironmentCreationStatusListener;
import org.knime.python2.config.AbstractCondaEnvironmentsPanel;
import org.knime.python2.config.PythonEnvironmentType;
import org.knime.python2.config.PythonEnvironmentTypeConfig;
import org.knime.python2.config.SerializerConfig;
import org.knime.python2.extensions.serializationlibrary.SerializationLibraryExtensions;
import org.knime.python2.prefs.PythonPreferences;

import com.google.common.collect.Sets;

/**
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 */
final class DLPythonConfigsObserver {

    private static final String PLACEHOLDER_CONDA_ENV = "no environment available";

    private final DLTestStatusChangeListenerCollection m_changeListenerCollection;

    private final DLPythonConfigSelectionConfig m_configSelectionConfig;

    private final PythonEnvironmentTypeConfig m_environmentTypeConfig;

    private final DLCondaEnvironmentsConfig m_condaEnvironmentsConfig;

    private final DLCondaEnvironmentCreationObserver m_kerasEnvironmentCreator;

    private final DLCondaEnvironmentCreationObserver m_tf2EnvironmentCreator;

    private final DLManualEnvironmentsConfig m_manualEnvironmentsConfig;

    private final SerializerConfig m_serializerConfig;

    DLPythonConfigsObserver(final DLPythonConfigSelectionConfig configSelectionConfig,
        final PythonEnvironmentTypeConfig environmentTypeConfig,
        final DLCondaEnvironmentsConfig condaEnvironmentsConfig,
        final DLCondaEnvironmentCreationObserver kerasEnvironmentCreator,
        final DLCondaEnvironmentCreationObserver tf2EnvironmentCreator,
        final DLManualEnvironmentsConfig manualEnvironmentsConfig, final SerializerConfig serializerConfig) {
        m_configSelectionConfig = configSelectionConfig;
        m_environmentTypeConfig = environmentTypeConfig;
        m_condaEnvironmentsConfig = condaEnvironmentsConfig;
        m_kerasEnvironmentCreator = kerasEnvironmentCreator;
        m_tf2EnvironmentCreator = tf2EnvironmentCreator;
        m_manualEnvironmentsConfig = manualEnvironmentsConfig;
        m_serializerConfig = serializerConfig;
        m_changeListenerCollection = new DLTestStatusChangeListenerCollection();

        configSelectionConfig.getConfigSelection().addChangeListener(e -> testCurrentPreferences());

        environmentTypeConfig.getEnvironmentType().addChangeListener(e -> testCurrentPreferences());

        // Refresh and test entire Conda config on Conda directory change
        condaEnvironmentsConfig.getCondaDirectoryPath().addChangeListener(e -> refreshAndTestDLCondaConfig());

        // Test Keras Conda environment on change
        condaEnvironmentsConfig.getKerasConfig().getEnvironmentDirectory()
            .addChangeListener(e -> testDLPythonEnvironment(true, DLPythonLibrarySelection.KERAS));
        // Test TF2 Conda environment on change
        condaEnvironmentsConfig.getTF2Config().getEnvironmentDirectory()
            .addChangeListener(e -> testDLPythonEnvironment(true, DLPythonLibrarySelection.TF2));

        // Test manual config on change
        manualEnvironmentsConfig.getKerasConfig().getExecutablePath()
            .addChangeListener(e -> testDLPythonEnvironment(false, DLPythonLibrarySelection.KERAS));
        manualEnvironmentsConfig.getTF2Config().getExecutablePath()
            .addChangeListener(e -> testDLPythonEnvironment(false, DLPythonLibrarySelection.TF2));

        // Test everything if the serializer changes
        serializerConfig.getSerializer().addChangeListener(e -> testCurrentPreferences());

        // Disable Conda environment creation by default. Updated when Conda installation is tested
        kerasEnvironmentCreator.getIsEnvironmentCreationEnabled().setBooleanValue(false);
        tf2EnvironmentCreator.getIsEnvironmentCreationEnabled().setBooleanValue(false);

        // Handle finished Conda environment creation processes
        observeEnvironmentCreation(kerasEnvironmentCreator, DLPythonLibrarySelection.KERAS);
        observeEnvironmentCreation(tf2EnvironmentCreator, DLPythonLibrarySelection.TF2);
    }

    /** Check the currently configured preferences. All of them */
    public void testCurrentPreferences() {
        if (isPythonEnvironmentSelected()) {
            //
            // Using the python config
            //
            clearDLEnvInfoAndError();
            testDefaultPythonEnvironment();
        } else if (isDlEnvironmentSelected()) {
            //
            // Using the special DL config
            //
            clearDefaultEnvInfoAndError();
            final PythonEnvironmentType environmentType =
                PythonEnvironmentType.fromId(m_environmentTypeConfig.getEnvironmentType().getStringValue());
            if (PythonEnvironmentType.CONDA.equals(environmentType)) {
                // CONDA
                refreshAndTestDLCondaConfig();
            } else if (PythonEnvironmentType.MANUAL.equals(environmentType)) {
                // MANUAL
                testDLPythonEnvironment(false, DLPythonLibrarySelection.KERAS);
                testDLPythonEnvironment(false, DLPythonLibrarySelection.TF2);
            } else {
                throw new IllegalStateException("Selected environment type '" + environmentType.getName()
                    + "' is neither " + "conda nor manual. This is an implementation error.");
            }
        } else {
            throw new IllegalStateException("Selected config'" + DLPythonConfigSelection
                .fromId(m_configSelectionConfig.getConfigSelection().getStringValue()).getName() + "' is neither "
                + "python nor deep learning. This is an implementation error.");
        }
    }

    /**
     * @param listener A listener which will be notified about changes in the status of any installation test initiated
     *            by this instance.
     */
    public void addConfigsTestStatusListener(final DLPythonConfigsInstallationTestStatusChangeListener listener) {
        m_changeListenerCollection.addConfigsTestStatusListener(listener);
    }

    /** @return true if the default python configuration should be used */
    private boolean isPythonEnvironmentSelected() {
        return DLPythonConfigSelection.PYTHON.getId() //
            .equals(m_configSelectionConfig.getConfigSelection().getStringValue());
    }

    /** @return true if a special dl configuration should be used */
    private boolean isDlEnvironmentSelected() {
        return DLPythonConfigSelection.DL.getId() //
            .equals(m_configSelectionConfig.getConfigSelection().getStringValue());
    }

    /** Test that conda is usable, get environments (and notify the UI) and test the selected environments */
    private void refreshAndTestDLCondaConfig() {
        new Thread(() -> {
            // Test the conda installation
            final Conda conda;
            try {
                conda = testDLCondaInstallation();
            } catch (final Exception ex) {
                return;
            }
            // Get the available environments
            final List<CondaEnvironmentSpec> availableEnvironments;
            try {
                availableEnvironments = getAvailableCondaEnvironments(conda, true, true);
            } catch (final Exception ex) {
                return;
            }

            // Test the configuration
            try {
                setAvailableCondaEnvironments(availableEnvironments);
                testDLPythonEnvironment(true, DLPythonLibrarySelection.KERAS);
                testDLPythonEnvironment(true, DLPythonLibrarySelection.TF2);
            } catch (Exception ex) {
                // Ignore, we still want to configure and test the second environment.
            }
        }).start();
    }

    private void testDefaultPythonEnvironment() {
        final PythonCommand pythonCommand = PythonPreferences.getPython3CommandPreference();
        Collection<PythonModuleSpec> serializerModules = PythonPreferences.getCurrentlyRequiredSerializerModules();

        final Collection<PythonModuleSpec> additionalRequiredModules = new ArrayList<>();
        // Serializer modules
        additionalRequiredModules.addAll(serializerModules);
        // Deep learning modules
        // Note: We still use the PythonModuleDependencyRegistry because there are only separate environments
        // for Keras and TensorFlow 2 yet. (Not for ONNX)
        final Collection<PythonModuleSpec> additionalOptionalModules =
            DLPythonModuleDependencyRegistry.getInstance().getPythonDependenciesModules();

        // Start the installation test in a new thread
        new Thread(() -> {
            m_changeListenerCollection.onEnvironmentInstallationTestStarting(null, null);
            m_configSelectionConfig.getPythonInstallationInfo().setStringValue("Testing Python environment...");
            m_configSelectionConfig.getPythonInstallationWarning().setStringValue("");
            m_configSelectionConfig.getPythonInstallationError().setStringValue("");
            final PythonKernelTestResult testResult = PythonKernelTester.testPython3Installation(pythonCommand,
                additionalRequiredModules, additionalOptionalModules, true);
            setDefaultPythonTestResult(testResult);
            m_changeListenerCollection.onEnvironmentInstallationTestFinished(null, null, testResult);
        }).start();
    }

    /** Set the test result of testing the default Python environment */
    private void setDefaultPythonTestResult(final PythonKernelTestResult testResult) {
        if (isPythonEnvironmentSelected()) {
            m_configSelectionConfig.getPythonInstallationInfo().setStringValue(testResult.getVersion());
            String errorLog = testResult.getErrorLog();
            if (errorLog != null && !errorLog.isEmpty()) {
                errorLog += "\nNote: You can create a new Python Conda environment that contains all packages\n"
                    + "required by the KNIME Deep Learning integration by selecting 'Use special Deep Learning "
                    + "configuration' and clicking on the '"
                    + AbstractCondaEnvironmentsPanel.CREATE_NEW_ENVIRONMENT_BUTTON_TEXT + "' button.";
            }
            final String warningLog = testResult.getWarningLog();
            m_configSelectionConfig.getPythonInstallationError().setStringValue(errorLog);
            m_configSelectionConfig.getPythonInstallationWarning().setStringValue(warningLog);
        } else {
            m_configSelectionConfig.getPythonInstallationInfo().setStringValue("");
        }
    }

    private void testDLPythonEnvironment(final boolean isConda, final DLPythonLibrarySelection library) {
        // Conda or manual
        final PythonEnvironmentType environmentType;
        final DLPythonEnvironmentsConfig environmentsConfig;
        final DLPythonEnvironmentConfig environmentConfig;
        final String environmentCreationInfo;
        if (isConda) {
            environmentType = PythonEnvironmentType.CONDA;
            environmentsConfig = m_condaEnvironmentsConfig;
            environmentCreationInfo = "\nNote: You can create a new Python Conda environment that "
                + "contains all packages\nrequired by the KNIME Deep Learning integration by clicking the '"
                + AbstractCondaEnvironmentsPanel.CREATE_NEW_ENVIRONMENT_BUTTON_TEXT + "' button\nabove.";
        } else {
            environmentType = PythonEnvironmentType.MANUAL;
            environmentsConfig = m_manualEnvironmentsConfig;
            environmentCreationInfo = "\nNote: An easy way to create a new Python Conda environment that "
                + "contains all packages\nrequired by the KNIME Deep Learning integration can be found on the '"
                + PythonEnvironmentType.CONDA.getName() + "' tab of this preference page.";
        }

        // Deep learning modules
        final Collection<PythonModuleSpec> additionalOptionalModules;
        if (DLPythonLibrarySelection.KERAS.equals(library)) {
            environmentConfig = environmentsConfig.getKerasConfig();
            additionalOptionalModules = Sets.newHashSet(new PythonModuleSpec("keras"),
                new PythonModuleSpec("tensorflow", new Version(1, 0, 0), true, new Version(2, 0, 0), false),
                new PythonModuleSpec("h5py"));
        } else if (DLPythonLibrarySelection.TF2.equals(library)) {
            environmentConfig = environmentsConfig.getTF2Config();
            additionalOptionalModules = Sets.newHashSet(
                new PythonModuleSpec("tensorflow", new Version(2, 2, 0), true, new Version(3, 0, 0), false));
        } else {
            throw new IllegalStateException(
                "Library is neither Keras nor TensorFlow 2. This is an implementation error.");
        }

        // If the placeholder is selected we ask the user to create a new environment
        // And test nothing
        if (isConda && isPlaceholderEnvironmentSelected((DLCondaEnvironmentConfig)environmentConfig)) {
            environmentConfig.getPythonInstallationInfo().setStringValue("");
            environmentConfig.getPythonInstallationError().setStringValue(
                "No environment avaiable. Please create a new one to be able to use the Deep Learning integration."
                    + environmentCreationInfo);
            return;
        }

        // Serializer modules
        final Collection<PythonModuleSpec> additionalRequiredModules = new ArrayList<>();
        additionalRequiredModules.addAll(SerializationLibraryExtensions
            .getSerializationLibraryFactory(m_serializerConfig.getSerializer().getStringValue())
            .getRequiredExternalModules());

        // Start the installation test in a new thread
        new Thread(() -> {
            m_changeListenerCollection.onEnvironmentInstallationTestStarting(environmentType, library);
            environmentConfig.getPythonInstallationInfo().setStringValue("Testing Python environment...");
            environmentConfig.getPythonInstallationWarning().setStringValue("");
            environmentConfig.getPythonInstallationError().setStringValue("");
            final PythonCommand pythonCommand = environmentConfig.getPythonCommand();
            final PythonKernelTestResult testResult = PythonKernelTester.testPython3Installation(pythonCommand,
                additionalRequiredModules, additionalOptionalModules, true);
            setDLPythonTestResult(environmentConfig, environmentCreationInfo, testResult);
            m_changeListenerCollection.onEnvironmentInstallationTestFinished(environmentType, library, testResult);
        }).start();
    }

    private void setDLPythonTestResult(final DLPythonEnvironmentConfig environmentConfig,
        final String environmentCreationInfo, final PythonKernelTestResult testResult) {
        if (isDlEnvironmentSelected()) {
            environmentConfig.getPythonInstallationInfo().setStringValue(testResult.getVersion());
            String errorLog = testResult.getErrorLog();
            if (errorLog != null && !errorLog.isEmpty()) {
                errorLog += environmentCreationInfo;
            }
            final String warningLog = testResult.getWarningLog();
            environmentConfig.getPythonInstallationError().setStringValue(errorLog);
            environmentConfig.getPythonInstallationWarning().setStringValue(warningLog);
        } else {
            environmentConfig.getPythonInstallationInfo().setStringValue("");
        }
    }

    private static boolean isPlaceholderEnvironmentSelected(final DLCondaEnvironmentConfig config) {
        return PLACEHOLDER_CONDA_ENV.equals(config.getEnvironmentDirectory().getStringValue());
    }

    /** Test the conda installation and set the info/error message */
    private Conda testDLCondaInstallation() throws Exception {
        final SettingsModelString condaInfoMessage = m_condaEnvironmentsConfig.getCondaInstallationInfo();
        final SettingsModelString condaErrorMessage = m_condaEnvironmentsConfig.getCondaInstallationError();
        try {
            condaInfoMessage.setStringValue("Testing Conda installation...");
            condaErrorMessage.setStringValue("");
            m_changeListenerCollection.onCondaInstallationTestStarting();
            final Conda conda = new Conda(m_condaEnvironmentsConfig.getCondaDirectoryPath().getStringValue());
            String condaVersionString = conda.getVersionString();
            try {
                condaVersionString =
                    "Conda version: " + Conda.condaVersionStringToVersion(condaVersionString).toString();
            } catch (final IllegalArgumentException ex) {
                // Ignore and use raw version string.
            }
            condaInfoMessage.setStringValue(condaVersionString);
            condaErrorMessage.setStringValue("");
            m_kerasEnvironmentCreator.getIsEnvironmentCreationEnabled().setBooleanValue(true);
            m_tf2EnvironmentCreator.getIsEnvironmentCreationEnabled().setBooleanValue(true);
            m_changeListenerCollection.onCondaInstallationTestFinished("");
            return conda;
        } catch (final Exception ex) {
            condaInfoMessage.setStringValue("");
            condaErrorMessage.setStringValue(ex.getMessage());
            clearAvailableCondaEnvironments();
            setCondaEnvironmentStatusMessages("", "", "", true, true);
            m_kerasEnvironmentCreator.getIsEnvironmentCreationEnabled().setBooleanValue(false);
            m_tf2EnvironmentCreator.getIsEnvironmentCreationEnabled().setBooleanValue(false);
            m_changeListenerCollection.onCondaInstallationTestFinished(ex.getMessage());
            throw ex;
        }
    }

    /** Clear the list of available conda environments */
    private void clearAvailableCondaEnvironments() {
        setAvailableCondaEnvironments(Collections.emptyList());
    }

    /** Clear all errors and infos for the default Python environment selection */
    private void clearDefaultEnvInfoAndError() {
        m_configSelectionConfig.getPythonInstallationInfo().setStringValue("");
        m_configSelectionConfig.getPythonInstallationWarning().setStringValue("");
        m_configSelectionConfig.getPythonInstallationError().setStringValue("");
    }

    /** Clear all errors and infos for the special DL environment selection */
    private void clearDLEnvInfoAndError() {
        // Conda path
        m_condaEnvironmentsConfig.getCondaInstallationInfo().setStringValue("");
        m_condaEnvironmentsConfig.getCondaInstallationError().setStringValue("");
        // Conda Keras
        m_condaEnvironmentsConfig.getKerasConfig().getPythonInstallationInfo().setStringValue("");
        m_condaEnvironmentsConfig.getKerasConfig().getPythonInstallationWarning().setStringValue("");
        m_condaEnvironmentsConfig.getKerasConfig().getPythonInstallationError().setStringValue("");
        // Conda TF2
        m_condaEnvironmentsConfig.getTF2Config().getPythonInstallationInfo().setStringValue("");
        m_condaEnvironmentsConfig.getTF2Config().getPythonInstallationWarning().setStringValue("");
        m_condaEnvironmentsConfig.getTF2Config().getPythonInstallationError().setStringValue("");
        // Manual Keras
        m_manualEnvironmentsConfig.getKerasConfig().getPythonInstallationInfo().setStringValue("");
        m_manualEnvironmentsConfig.getKerasConfig().getPythonInstallationWarning().setStringValue("");
        m_manualEnvironmentsConfig.getKerasConfig().getPythonInstallationError().setStringValue("");
        // Manual TF2
        m_manualEnvironmentsConfig.getTF2Config().getPythonInstallationInfo().setStringValue("");
        m_manualEnvironmentsConfig.getTF2Config().getPythonInstallationWarning().setStringValue("");
        m_manualEnvironmentsConfig.getTF2Config().getPythonInstallationError().setStringValue("");
    }

    /** Set the given messages for conda environment selections */
    private void setCondaEnvironmentStatusMessages(final String infoMessage, final String warningMessage,
        final String errorMessage, final boolean setForKeras, final boolean setForTF2) {
        if (setForKeras) {
            // Keras
            m_condaEnvironmentsConfig.getKerasConfig().getPythonInstallationInfo().setStringValue(infoMessage);
            m_condaEnvironmentsConfig.getKerasConfig().getPythonInstallationWarning().setStringValue(warningMessage);
            m_condaEnvironmentsConfig.getKerasConfig().getPythonInstallationError().setStringValue(errorMessage);
        }
        if (setForTF2) {
            // TF2
            m_condaEnvironmentsConfig.getTF2Config().getPythonInstallationInfo().setStringValue(infoMessage);
            m_condaEnvironmentsConfig.getTF2Config().getPythonInstallationWarning().setStringValue(warningMessage);
            m_condaEnvironmentsConfig.getTF2Config().getPythonInstallationError().setStringValue(errorMessage);
        }
    }

    /** Get a list of conda environments in the given conda installation */
    private List<CondaEnvironmentSpec> getAvailableCondaEnvironments(final Conda conda,
        final boolean updateKerasStatusMessage, final boolean updateTF2StatusMessage) throws Exception {
        try {
            setCondaEnvironmentStatusMessages("Collecting available environments...", "", "", updateKerasStatusMessage,
                updateTF2StatusMessage);
            return conda.getEnvironments();
        } catch (final Exception ex) {
            m_condaEnvironmentsConfig.getCondaInstallationError().setStringValue(ex.getMessage());
            final String environmentsNotDetectedMessage = "Available environments could not be detected.";
            clearAvailableCondaEnvironments();
            setCondaEnvironmentStatusMessages("", "", environmentsNotDetectedMessage, true, true);
            throw ex;
        }
    }

    /** Set the given environments to be available for both conda environment selections */
    private void setAvailableCondaEnvironments(List<CondaEnvironmentSpec> availableEnvironments) {
        if (availableEnvironments.isEmpty()) {
            availableEnvironments =
                Arrays.asList(new CondaEnvironmentSpec(PLACEHOLDER_CONDA_ENV, PLACEHOLDER_CONDA_ENV));
        }
        // Keras
        m_condaEnvironmentsConfig.getKerasConfig().getAvailableEnvironments()
            .setValue(availableEnvironments.toArray(new CondaEnvironmentSpec[0]));
        if (availableEnvironments.stream().noneMatch(env -> Objects.equals(env.getDirectoryPath(),
            m_condaEnvironmentsConfig.getKerasConfig().getEnvironmentDirectory().getStringValue()))) {
            m_condaEnvironmentsConfig.getKerasConfig().getEnvironmentDirectory()
                .setStringValue(availableEnvironments.get(0).getDirectoryPath());
        }
        // TF2
        m_condaEnvironmentsConfig.getTF2Config().getAvailableEnvironments()
            .setValue(availableEnvironments.toArray(new CondaEnvironmentSpec[0]));
        if (availableEnvironments.stream().noneMatch(env -> Objects.equals(env.getDirectoryPath(),
            m_condaEnvironmentsConfig.getTF2Config().getEnvironmentDirectory().getStringValue()))) {
            m_condaEnvironmentsConfig.getTF2Config().getEnvironmentDirectory()
                .setStringValue(availableEnvironments.get(0).getDirectoryPath());
        }
    }

    /** Get the conda config for the given library */
    private DLCondaEnvironmentConfig getCondaConfigForLibrary(final DLPythonLibrarySelection library) {
        if (DLPythonLibrarySelection.KERAS.equals(library)) {
            return m_condaEnvironmentsConfig.getKerasConfig();
        } else if (DLPythonLibrarySelection.TF2.equals(library)) {
            return m_condaEnvironmentsConfig.getTF2Config();
        } else {
            throw new IllegalStateException(
                "Library is neither Keras nor TensorFlow 2. This is an implementation error.");
        }
    }

    /** Add a status listener which tests the python environment if one got created */
    private void observeEnvironmentCreation(final DLCondaEnvironmentCreationObserver creationStatus,
        final DLPythonLibrarySelection library) {
        creationStatus.addEnvironmentCreationStatusListener(new CondaEnvironmentCreationStatusListener() {

            @Override
            public void condaEnvironmentCreationStarting(final CondaEnvironmentCreationStatus status) {
                // no-op
            }

            @Override
            public void condaEnvironmentCreationFinished(final CondaEnvironmentCreationStatus status,
                final CondaEnvironmentSpec createdEnvironment) {
                final Conda conda;
                try {
                    conda = testDLCondaInstallation();
                } catch (final Exception ex) {
                    return;
                }
                final List<CondaEnvironmentSpec> availableEnvironments;
                try {
                    availableEnvironments = getAvailableCondaEnvironments(conda,
                        DLPythonLibrarySelection.KERAS.equals(library), DLPythonLibrarySelection.TF2.equals(library));
                } catch (final Exception ex) {
                    return;
                }
                try {
                    setAvailableCondaEnvironments(availableEnvironments);
                    // Both lists of conda environments were updated but for the other library the selection did not change
                    // Therefore, we do not have to check it
                    final DLCondaEnvironmentConfig environmentConfig = getCondaConfigForLibrary(library);
                    environmentConfig.getEnvironmentDirectory().setStringValue(createdEnvironment.getDirectoryPath());
                    testDLPythonEnvironment(true, library);
                } catch (Exception ex) {
                    // Ignore, we still want to configure and test the second environment.
                }
            }

            @Override
            public void condaEnvironmentCreationCanceled(final CondaEnvironmentCreationStatus status) {
                // no-op
            }

            @Override
            public void condaEnvironmentCreationFailed(final CondaEnvironmentCreationStatus status,
                final String errorMessage) {
                // no-op
            }
        }, false);
    }
}
