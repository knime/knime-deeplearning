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

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public abstract class AbstractConfigEntry<T> implements ConfigEntry<T> {

    protected final String m_key;
    protected final Class<T> m_entryType;
    private final CopyOnWriteArrayList<Consumer<ConfigEntry<T>>> m_loadListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<BiConsumer<ConfigEntry<T>, T>> m_valueChangeListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Consumer<ConfigEntry<T>>> m_enableChangeListeners = new CopyOnWriteArrayList<>();
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
    public void addEnableChangeListener(final Consumer<ConfigEntry<T>> listener) {
    	if (!m_enableChangeListeners.contains(listener)) {
    		m_enableChangeListeners.add(listener);
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

    protected final void onLoaded() {
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