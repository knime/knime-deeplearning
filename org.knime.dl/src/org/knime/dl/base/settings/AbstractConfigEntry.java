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

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public abstract class AbstractConfigEntry<T> implements ConfigEntry<T> {

	private static final String CFG_KEY_ENABLED = "enabled";

	private final String m_key;

	private final Class<T> m_entryType;

	private final CopyOnWriteArrayList<Consumer<ConfigEntry<T>>> m_loadListeners //
			= new CopyOnWriteArrayList<>();

	private final CopyOnWriteArrayList<BiConsumer<ConfigEntry<T>, T>> m_valueChangeListeners //
			= new CopyOnWriteArrayList<>();

	private final CopyOnWriteArrayList<Consumer<ConfigEntry<T>>> m_enableChangeListeners //
			= new CopyOnWriteArrayList<>();

	private final CopyOnWriteArrayList<Function<ConfigEntry<T>, Boolean>> m_loadPredicates //
			= new CopyOnWriteArrayList<>();

	protected T m_value;

	protected boolean m_enabled = true;

	protected AbstractConfigEntry(final String entryKey, final Class<T> entryType) {
		m_key = checkNotNullOrEmpty(entryKey);
		m_entryType = checkNotNull(entryType);
	}

	/**
	 * @param value may be null
	 */
	protected AbstractConfigEntry(final String entryKey, final Class<T> entryType, final T value) {
		this(entryKey, entryType);
		m_value = value;
	}

	protected AbstractConfigEntry(final String entryKey, final Class<T> entryType, final T value,
			final boolean enabled) {
		this(entryKey, entryType, value);
		m_enabled = enabled;
	}

	@Override
	public String getEntryKey() {
		return m_key;
	}

	@Override
	public Class<T> getEntryType() {
		return m_entryType;
	}

	@Override
	public T getValue() {
		return m_value;
	}

	@Override
	public void setValue(final T value, final boolean forceUpdate) {
		if (!Objects.deepEquals(m_value, value) || forceUpdate) {
			final T oldValue = m_value;
			m_value = value;
			onValueChanged(oldValue);
		}
	}

	@Override
	public boolean getEnabled() {
		return m_enabled;
	}

	@Override
	public void setEnabled(final boolean enabled) {
		if (enabled != m_enabled) {
			m_enabled = enabled;
			onEnabledChanged();
		}
	}

	@Override
	public final void saveSettingsTo(final NodeSettingsWO settings)
			throws InvalidSettingsException, UnsupportedOperationException {
		final NodeSettingsWO subSettings = checkNotNull(settings).addNodeSettings(m_key);
		subSettings.addBoolean(CFG_KEY_ENABLED, m_enabled);
		saveEntry(subSettings);
	}

	@Override
	public final void loadSettingsFrom(final NodeSettingsRO settings)
			throws InvalidSettingsException, UnsupportedOperationException {
		final T oldValue = m_value;
		try {
			final NodeSettingsRO subSettings = checkNotNull(settings).getNodeSettings(m_key);
			m_enabled = subSettings.getBoolean(CFG_KEY_ENABLED);
			loadEntry(subSettings);
		} catch (final Exception e) {
			// Config entry could not be found in settings. Give deriving classes a chance to fall back to a default
			// value or the like.
			if (!handleFailureToLoadConfigEntry(settings, e)) {
				throw new InvalidSettingsException(e.getMessage(), e);
			}
		}
		if (checkLoadPredicates()) {
			onLoaded();
		} else {
			m_value = oldValue;
		}
	}

	private boolean checkLoadPredicates() {
		for (final Function<ConfigEntry<T>, Boolean> func : m_loadPredicates) {
			if (!func.apply(this)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void addLoadListener(final Consumer<ConfigEntry<T>> listener) {
		if (!m_loadListeners.contains(listener)) {
			m_loadListeners.add(listener);
		}
	}

	@Override
	public void removeLoadListener(final Consumer<ConfigEntry<T>> listener) {
		m_loadListeners.remove(listener);
	}

	@Override
	public void addValueChangeListener(final BiConsumer<ConfigEntry<T>, T> listener) {
		if (!m_valueChangeListeners.contains(listener)) {
			m_valueChangeListeners.add(listener);
		}
	}

	@Override
	public void removeValueChangeListener(final BiConsumer<ConfigEntry<T>, T> listener) {
		m_valueChangeListeners.remove(listener);
	}

	@Override
	public void removeLoadPredicate(final Function<ConfigEntry<T>, Boolean> listener) {
		m_loadPredicates.remove(listener);
	}

	@Override
	public void addEnableChangeListener(final Consumer<ConfigEntry<T>> listener) {
		if (!m_enableChangeListeners.contains(listener)) {
			m_enableChangeListeners.add(listener);
		}
	}

	@Override
	public void addLoadPredicate(final Function<ConfigEntry<T>, Boolean> listener) {
		if (!m_loadPredicates.contains(listener)) {
			m_loadPredicates.add(listener);
		}
	}

	@Override
	public void removeEnableChangeListener(final Consumer<ConfigEntry<T>> listener) {
		m_enableChangeListeners.remove(listener);
	}

	@Override
	public String toString() {
		return "config entry: " + m_key + ": " + Objects.toString(m_value);
	}

	protected void saveEntry(final NodeSettingsWO settings)
			throws InvalidSettingsException, UnsupportedOperationException {
		if (Boolean.class.equals(m_entryType)) {
			settings.addBoolean(m_key, (Boolean) m_value);
		} else if (Double.class.equals(m_entryType)) {
			settings.addDouble(m_key, (Double) m_value);
		} else if (Integer.class.equals(m_entryType)) {
			settings.addInt(m_key, (Integer) m_value);
		} else if (Long.class.equals(m_entryType)) {
			settings.addLong(m_key, (Long) m_value);
		} else if (String.class.equals(m_entryType)) {
			settings.addString(m_key, (String) m_value);
		} else if (String[].class.equals(m_entryType)) {
			settings.addStringArray(m_key, (String[]) m_value);
		} else if (org.knime.dl.base.settings.Config.class.isAssignableFrom(m_entryType)) {
			((org.knime.dl.base.settings.Config) m_value).saveToSettings(settings);
		} else {
			try {
				final Method[] methods = settings.getClass().getMethods();
				for (final Method method : methods) {
					if (method.getParameterCount() == 2) {
						final Class<?>[] parameterTypes = method.getParameterTypes();
						if (parameterTypes[0].equals(String.class) && parameterTypes[1].equals(m_entryType)
								&& method.getName().startsWith("add")) {
							method.invoke(settings, m_key, m_value);
							return;
						}
					}
				}
			} catch (final Throwable t) {
				// ignore
			}
			throw new UnsupportedOperationException("Cannot save config entry '" + m_key
					+ "' to KNIME settings. Entry type " + m_entryType + " is not supported.");
		}
	}

	@SuppressWarnings("unchecked") // we ensure type safety
	protected void loadEntry(final NodeSettingsRO settings)
			throws InvalidSettingsException, IllegalStateException, UnsupportedOperationException {
		if (Boolean.class.equals(m_entryType)) {
			m_value = (T) new Boolean(settings.getBoolean(m_key));
		} else if (Double.class.equals(m_entryType)) {
			m_value = (T) new Double(settings.getDouble(m_key));
		} else if (Integer.class.equals(m_entryType)) {
			m_value = (T) new Integer(settings.getInt(m_key));
		} else if (Long.class.equals(m_entryType)) {
			m_value = (T) new Long(settings.getLong(m_key));
		} else if (String.class.equals(m_entryType)) {
			m_value = (T) settings.getString(m_key);
		} else if (String[].class.equals(m_entryType)) {
			m_value = (T) settings.getStringArray(m_key);
		} else if (org.knime.dl.base.settings.Config.class.isAssignableFrom(m_entryType)) {
			if (m_value == null) {
				throw new IllegalStateException(
						"Cannot load config entry '" + m_key + "' from KNIME settings. Entry type " + m_entryType
								+ " requires a pre-allocated default value to populate.");
			}
			((org.knime.dl.base.settings.Config) m_value).loadFromSettings(settings);
		} else {
			try {
				final Method[] methods = settings.getClass().getMethods();
				for (final Method method : methods) {
					if (method.getParameterCount() == 1 && method.getParameterTypes()[0].equals(String.class)
							&& method.getReturnType().equals(m_entryType) && method.getName().startsWith("get")) {
						m_value = (T) method.invoke(settings, m_key);
						return;

					}
				}
			} catch (final Throwable t) {
				// ignore
			}
			throw new UnsupportedOperationException("Cannot load config entry '" + m_key
					+ "' from KNIME settings. Entry type " + m_entryType + " is not supported.");
		}
	}

	/**
	 * This method is called by {@link #loadSettingsFrom(NodeSettingsRO)} if loading the config entry failed. It allows
	 * deriving classes to handle such cases e.g. by falling back to a default value and/or enabled state.
	 *
	 * @param settings the provided node settings from which loading the config entry failed
	 * @param cause the exception that made loading the config entry fail. Usually, it is one of the following:
	 *            <ul>
	 *            <li><code>InvalidSettingsException</code>: if the entry's key cannot be found in the given
	 *            settings</li>
	 *            <li><code>ClassCastException</code>: if the type of the entry's saved value and the entry's actual
	 *            type are incompatible</li>
	 *            <li><code>IllegalStateException</code>: if loading the config entry requires a pre-allocated default
	 *            value to populate which is missing</li>
	 *            <li><code>UnsupportedOperationException</code>: if the entry type is not supported</li>
	 *            </ul>
	 * @return <code>true</code> if the failure could be handled. Returning <code>false</code> will cause an
	 *         {@link InvalidSettingsException} to be thrown.
	 */
	protected boolean handleFailureToLoadConfigEntry(final NodeSettingsRO settings, final Exception cause) {
		return false;
	}

	private final void onLoaded() {
		for (final Consumer<ConfigEntry<T>> listener : m_loadListeners) {
			listener.accept(this);
		}
	}

	private final void onValueChanged(final T oldValue) {
		for (final BiConsumer<ConfigEntry<T>, T> listener : m_valueChangeListeners) {
			listener.accept(this, oldValue);
		}
	}

	private final void onEnabledChanged() {
		for (final Consumer<ConfigEntry<T>> listener : m_enableChangeListeners) {
			listener.accept(this);
		}
	}
}
