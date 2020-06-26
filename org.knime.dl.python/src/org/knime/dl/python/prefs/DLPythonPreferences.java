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

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.python2.PythonCommand;
import org.knime.python2.PythonModuleSpec;
import org.knime.python2.config.PythonConfigStorage;
import org.knime.python2.config.PythonEnvironmentType;
import org.knime.python2.config.PythonEnvironmentTypeConfig;
import org.knime.python2.config.SerializerConfig;
import org.knime.python2.extensions.serializationlibrary.SerializationLibraryExtensions;
import org.knime.python2.prefs.PythonPreferences;
import org.knime.python2.prefs.PythonPreferencesInitializer;
import org.osgi.service.prefs.BackingStoreException;

/**
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 */
public class DLPythonPreferences {

    /** The current dl python configuration */
    static final PythonConfigStorage CURRENT = new InstanceScopeConfigStorage();

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

    /**
     * Add a listener which gets notified if a setting changes.
     *
     * @param listener the listener
     */
    public static void addPreferencesChangeListener(final IPreferenceChangeListener listener) {
        InstanceScopeConfigStorage.getInstanceScopePreferences().addPreferenceChangeListener(listener);
    }

    /**
     * Remove a listener that was added with {@link #addPreferencesChangeListener(IPreferenceChangeListener)}.
     *
     * @param listener the listener to remove
     */
    public static void removePreferencesChangeListener(final IPreferenceChangeListener listener) {
        InstanceScopeConfigStorage.getInstanceScopePreferences().removePreferenceChangeListener(listener);
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

    public static final class InstanceScopeConfigStorage implements PythonConfigStorage {

        private static final String QUALIFIER = "org.knime.dl.python";

        static final IEclipsePreferences getInstanceScopePreferences() {
            return InstanceScope.INSTANCE.getNode(QUALIFIER);
        }

        @Override
        public void saveBooleanModel(final SettingsModelBoolean model) {
            getInstanceScopePreferences().putBoolean(model.getConfigName(), model.getBooleanValue());
            flush();
        }

        @Override
        public void loadBooleanModel(final SettingsModelBoolean model) {
            final boolean value = Platform.getPreferencesService().getBoolean(QUALIFIER, model.getConfigName(),
                model.getBooleanValue(), null);
            model.setBooleanValue(value);
        }

        @Override
        public void saveIntegerModel(final SettingsModelInteger model) {
            getInstanceScopePreferences().putInt(model.getKey(), model.getIntValue());
            flush();
        }

        @Override
        public void loadIntegerModel(final SettingsModelInteger model) {
            final int value =
                Platform.getPreferencesService().getInt(QUALIFIER, model.getKey(), model.getIntValue(), null);
            model.setIntValue(value);
        }

        @Override
        public void saveStringModel(final SettingsModelString model) {
            getInstanceScopePreferences().put(model.getKey(), model.getStringValue());
            flush();
        }

        @Override
        public void loadStringModel(final SettingsModelString model) {
            final String value =
                Platform.getPreferencesService().getString(QUALIFIER, model.getKey(), model.getStringValue(), null);
            model.setStringValue(value);
        }

        private static void flush() {
            try {
                getInstanceScopePreferences().flush();
            } catch (BackingStoreException ex) {
                NodeLogger.getLogger(PythonPreferencesInitializer.class)
                    .error("Could not save Python preferences entry: " + ex.getMessage(), ex);
            }
        }
    }
}
