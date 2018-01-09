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

import static com.google.common.base.Preconditions.checkNotNull;
import static org.knime.dl.util.DLUtils.Preconditions.checkNotNullOrEmpty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.util.Pair;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public abstract class AbstractConfig implements Config {

	private final String m_key;

	private final LinkedHashMap<String, ConfigEntry<?>> m_entries = new LinkedHashMap<>();

	protected AbstractConfig(final String configKey) {
		m_key = checkNotNullOrEmpty(configKey);
	}

	@Override
	public String getConfigKey() {
		return m_key;
	}

	@Override
	public boolean getAllEnabled() {
		return m_entries.values().stream().allMatch(ConfigEntry::getEnabled);
	}

	@Override
	public void setAllEnabled(final boolean enabled) {
		for (final ConfigEntry<?> entry : m_entries.values()) {
			entry.setEnabled(enabled);
		}
	}

	@Override
	public int size() {
		return m_entries.size();
	}

	@Override
	public boolean isEmpty() {
		return m_entries.isEmpty();
	}

	@Override
	public boolean containsKey(final Object key) {
		return m_entries.containsKey(key);
	}

	@Override
	public boolean containsValue(final Object value) {
		return m_entries.containsValue(value);
	}

	@Override
	public ConfigEntry<?> get(final Object key) {
		return m_entries.get(key);
	}

	@Override
	public <T> ConfigEntry<T> get(final String key, final Class<T> entryType) {
		checkNotNull(key);
		checkNotNull(entryType);
		final ConfigEntry<?> entry = get(key);
		if (entry == null) {
			return null;
		}
		if (!entryType.equals(entry.getEntryType())) {
			throw new IllegalArgumentException("Requested entry type " + entryType.getSimpleName()
					+ " is incompatible to type " + entry.getEntryType().getSimpleName() + " of entry '" + key + "'.");
		}
		@SuppressWarnings("unchecked") // we ensured type safety
		final ConfigEntry<T> typedEntry = (ConfigEntry<T>) entry;
		return typedEntry;
	}

	@Override
	public <T> T getEntryValue(final String key, final Class<T> entryType) {
		final ConfigEntry<T> entry = get(key, entryType);
		return entry != null ? entry.getValue() : null;
	}

	@Override
	public ConfigEntry<?> put(final String key, final ConfigEntry<?> value) {
		return m_entries.put(key, value);
	}

	@Override
	public ConfigEntry<?> put(final ConfigEntry<?> entry) {
		checkNotNull(entry);
		return put(entry.getEntryKey(), entry);
	}

	@Override
	public <T> void setEntryValue(final String key, final Class<T> entryType, final T value) {
		ConfigEntry<T> entry = get(key, entryType);
		if (entry == null) {
			entry = new DefaultConfigEntry<>(key, entryType);
			put(key, entry);
		}
		entry.setValue(value);
	}

	@Override
	public ConfigEntry<?> remove(final Object key) {
		return m_entries.remove(key);
	}

	@Override
	public void putAll(final Map<? extends String, ? extends ConfigEntry<?>> m) {
		m_entries.putAll(m);
	}

	@Override
	public void clear() {
		m_entries.clear();
	}

	@Override
	public Set<String> keySet() {
		return m_entries.keySet();
	}

	@Override
	public Collection<ConfigEntry<?>> values() {
		return m_entries.values();
	}

	@Override
	public Set<Entry<String, ConfigEntry<?>>> entrySet() {
		return m_entries.entrySet();
	}

	@Override
	public final void saveToSettings(final NodeSettingsWO settings) throws InvalidSettingsException {
		final NodeSettingsWO config = settings.addNodeSettings(m_key);
		saveConfig(config);
	}

	@Override
	public final void loadFromSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
		final NodeSettingsRO config = settings.getNodeSettings(m_key);
		loadConfig(config);
	}

	@Override
	public int hashCode() {
		return super.hashCode() * 37 + m_key.hashCode();
	}

	@Override
	public boolean equals(final Object o) {
		return super.equals(o) && ((AbstractConfig) o).m_key.equals(m_key);
	}

	@Override
	public String toString() {
		return !isEmpty()
				? "config '" + m_key + "' of size " + size() + ":\n"
						+ values().stream().map(ConfigEntry::toString).collect(Collectors.joining("\n"))
				: "empty config '" + m_key + "'";
	}

	protected void saveConfig(final NodeSettingsWO settings) throws InvalidSettingsException {
		ArrayList<Pair<ConfigEntry<?>, Exception>> entriesFailedToSave = null;
		for (final ConfigEntry<?> entry : values()) {
			try {
				entry.saveSettingsTo(settings);
			} catch (final Exception e) {
				if (entriesFailedToSave == null) {
					entriesFailedToSave = new ArrayList<>();
				}
				entriesFailedToSave.add(new Pair<>(entry, e));
			}
		}
		if (entriesFailedToSave != null) {
			throw new DLInvalidSettingsException("Failed to save entries of config '" + m_key + "'.",
					entriesFailedToSave);
		}
	}

	protected void loadConfig(final NodeSettingsRO settings) throws InvalidSettingsException {
		ArrayList<Pair<ConfigEntry<?>, Throwable>> entriesFailedToLoad = null;
		for (final ConfigEntry<?> entry : values()) {
			try {
				entry.loadSettingsFrom(settings);
			} catch (final Exception e) {
				if (entriesFailedToLoad == null) {
					entriesFailedToLoad = new ArrayList<>();
				}
				entriesFailedToLoad.add(new Pair<>(entry, e));
			}
		}
		if (entriesFailedToLoad != null) {
			throw new DLInvalidSettingsException("Failed to load entries of config '" + m_key + "'.",
					entriesFailedToLoad);
		}
	}
}
