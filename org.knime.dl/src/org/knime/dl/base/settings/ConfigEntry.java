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
public interface ConfigEntry<T> {

	String getEntryKey();

	Class<T> getEntryType();

	T getValue();

	default void setValue(T value) throws IllegalArgumentException {
		setValue(value, false);
	}

	void setValue(T value, boolean forceUpdate) throws IllegalArgumentException;

	boolean getEnabled();

	void setEnabled(boolean enabled);

	void saveSettingsTo(NodeSettingsWO settings) throws InvalidSettingsException, UnsupportedOperationException;

	void loadSettingsFrom(NodeSettingsRO settings)
			throws InvalidSettingsException, IllegalStateException, UnsupportedOperationException;

	void addLoadPredicate(Function<ConfigEntry<T>, Boolean> listener);

	void addLoadListener(Consumer<ConfigEntry<T>> listener);

	void removeLoadListener(Consumer<ConfigEntry<T>> listener);

	void addValueChangeListener(BiConsumer<ConfigEntry<T>, T> listener);

	void removeValueChangeListener(BiConsumer<ConfigEntry<T>, T> listener);

	void addEnableChangeListener(Consumer<ConfigEntry<T>> listener);

	void removeEnableChangeListener(Consumer<ConfigEntry<T>> listener);

	void removeLoadPredicate(Function<ConfigEntry<T>, Boolean> listener);

	@Override
	int hashCode();

	@Override
	boolean equals(Object obj);

	@Override
	String toString();

}