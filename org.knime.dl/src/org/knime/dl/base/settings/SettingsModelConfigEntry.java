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
package org.knime.dl.base.settings;

import java.util.function.Function;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.dl.util.DLThrowingLambdas.DLThrowingFunction;

/**
 * Instances of this class save and load their configuration using {@link SettingsModel}s
 * to allow for backwards compatibility.
 * Note that for each save and each load new SettingsModel objects are created to avoid the necessity
 * of keeping the SettingsModel and the ConfigEntry in sync.
 * 
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <T> the type of object the config entry is holding
 * @param <S> the type of {@link SettingsModel} that is used to serialize the {@link ConfigEntry}
 */
public final class SettingsModelConfigEntry<T, S extends SettingsModel> extends AbstractConfigEntry<T> {

    private final DLThrowingFunction<ConfigEntry<T>, S, InvalidSettingsException> m_valueToSettingsModel;

    private final DLThrowingFunction<S, T, InvalidSettingsException> m_settingsModelToValue;

    private final Function<String, S> m_settingsModelCreator;

    /**
     * @param entryKey the config key for this entry
     * @param entryType the type of object this {@link ConfigEntry} holds
     * @param settingsModelCreator creates a SettingsModel from a config key to load the configuration
     * @param entryToSettingsModel creates a SettingsModel from a {@link ConfigEntry} to save the configuration
     * @param settingsModelToValue creates the value from the loaded SettingsModel
     * @param value initial value of the entry
     * @param enabled initial enabled status of the entry
     */
    public SettingsModelConfigEntry(String entryKey, Class<T> entryType, Function<String, S> settingsModelCreator,
        DLThrowingFunction<ConfigEntry<T>, S, InvalidSettingsException> entryToSettingsModel,
        DLThrowingFunction<S, T, InvalidSettingsException> settingsModelToValue, T value, boolean enabled) {
        super(entryKey, entryType, value, enabled);
        m_valueToSettingsModel = entryToSettingsModel;
        m_settingsModelToValue = settingsModelToValue;
        m_settingsModelCreator = settingsModelCreator;
    }

    /**
     * @param entryKey the config key for this entry
     * @param entryType the type of object this {@link ConfigEntry} holds
     * @param settingsModelCreator creates a SettingsModel from a config key to load the configuration
     * @param entryToSettingsModel creates a SettingsModel from a {@link ConfigEntry} to save the configuration
     * @param settingsModelToValue creates the value from the loaded SettingsModel
     * @param value initial value of the entry
     */
    public SettingsModelConfigEntry(String entryKey, Class<T> entryType, Function<String, S> settingsModelCreator,
        DLThrowingFunction<ConfigEntry<T>, S, InvalidSettingsException> entryToSettingsModel,
        DLThrowingFunction<S, T, InvalidSettingsException> settingsModelToValue, T value) {
        super(entryKey, entryType, value);
        m_valueToSettingsModel = entryToSettingsModel;
        m_settingsModelToValue = settingsModelToValue;
        m_settingsModelCreator = settingsModelCreator;
    }

    /**
     * @param entryKey the config key for this entry
     * @param entryType the type of object this {@link ConfigEntry} holds
     * @param settingsModelCreator creates a SettingsModel from a config key to load the configuration
     * @param entryToSettingsModel creates a SettingsModel from a {@link ConfigEntry} to save the configuration
     * @param settingsModelToValue creates the value from the loaded SettingsModel
     */
    public SettingsModelConfigEntry(String entryKey, Class<T> entryType, Function<String, S> settingsModelCreator,
        DLThrowingFunction<ConfigEntry<T>, S, InvalidSettingsException> entryToSettingsModel,
        DLThrowingFunction<S, T, InvalidSettingsException> settingsModelToValue) {
        super(entryKey, entryType);
        m_valueToSettingsModel = entryToSettingsModel;
        m_settingsModelToValue = settingsModelToValue;
        m_settingsModelCreator = settingsModelCreator;
    }

    @Override
    public void saveSettingsTo(NodeSettingsWO settings) throws InvalidSettingsException {
        S settingsModel = m_valueToSettingsModel.apply(this);
        settingsModel.setEnabled(getEnabled());
        settingsModel.saveSettingsTo(settings);
    }

    @Override
    public void loadSettingsFrom(NodeSettingsRO settings) throws InvalidSettingsException {
        S settingsModel = m_settingsModelCreator.apply(getEntryKey());
        settingsModel.loadSettingsFrom(settings);
        m_value = m_settingsModelToValue.apply(settingsModel);
        setEnabled(settingsModel.isEnabled());
        onLoaded();
    }

}
