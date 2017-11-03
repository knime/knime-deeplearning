/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
public abstract class AbstractConfigEntry<T> implements ConfigEntry<T> {

	private static final String CFG_KEY_ENABLED = "enabled";

	private final String m_key;

	private final Class<T> m_entryType;

	private final CopyOnWriteArrayList<BiConsumer<ConfigEntry<T>, T>> m_valueChangeOrLoadListeners //
			= new CopyOnWriteArrayList<>();

	private final CopyOnWriteArrayList<BiConsumer<ConfigEntry<T>, Boolean>> m_enabledChangeOrLoadListeners //
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
	public void setValue(final T value) {
		if (!Objects.deepEquals(m_value, value)) {
			final T oldValue = m_value;
			m_value = value;
			valueChangedOrLoaded(oldValue);
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
			enabledChangedOrLoaded(!enabled);
		}
	}

	@Override
	public final void saveSettingsTo(final NodeSettingsWO settings)
			throws InvalidSettingsException, UnsupportedOperationException {
		final NodeSettingsWO subSettings = checkNotNull(settings).addNodeSettings(m_key);
		saveEntry(subSettings);
		subSettings.addBoolean(CFG_KEY_ENABLED, m_enabled);
	}

	@Override
	public final void loadSettingsFrom(final NodeSettingsRO settings)
			throws InvalidSettingsException, UnsupportedOperationException {
		final NodeSettingsRO subSettings = checkNotNull(settings).getNodeSettings(m_key);
		final boolean oldEnabled = m_enabled;
		m_enabled = subSettings.getBoolean(CFG_KEY_ENABLED);
		enabledChangedOrLoaded(oldEnabled);
		final T oldValue = m_value;
		loadEntry(subSettings);
		valueChangedOrLoaded(oldValue);
	}

	@Override
	public void addValueChangeOrLoadListener(final BiConsumer<ConfigEntry<T>, T> listener) {
		if (!m_valueChangeOrLoadListeners.contains(listener)) {
			m_valueChangeOrLoadListeners.add(listener);
		}
	}

	@Override
	public void removeValueChangeOrLoadListener(final BiConsumer<ConfigEntry<T>, T> listener) {
		m_valueChangeOrLoadListeners.remove(listener);
	}

	@Override
	public void addEnabledChangeOrLoadListener(final BiConsumer<ConfigEntry<T>, Boolean> listener) {
		if (!m_enabledChangeOrLoadListeners.contains(listener)) {
			m_enabledChangeOrLoadListeners.add(listener);
		}
	}

	@Override
	public void removeEnabledChangeOrLoadListener(final BiConsumer<ConfigEntry<T>, Boolean> listener) {
		m_enabledChangeOrLoadListeners.remove(listener);
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

	private final void valueChangedOrLoaded(final T oldValue) {
		for (final BiConsumer<ConfigEntry<T>, T> listener : m_valueChangeOrLoadListeners) {
			listener.accept(this, oldValue);
		}
	}

	private final void enabledChangedOrLoaded(final boolean oldEnabled) {
		for (final BiConsumer<ConfigEntry<T>, Boolean> listener : m_enabledChangeOrLoadListeners) {
			listener.accept(this, oldEnabled);
		}
	}
}
