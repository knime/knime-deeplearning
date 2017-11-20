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
import java.util.function.BiFunction;
import java.util.function.Function;

import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelLong;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public class ConfigUtil {

	private ConfigUtil() {
	}

	public static ConfigEntry<?> fromSettingsModel(final SettingsModel settingsModel, final String configName) {
		if (settingsModel instanceof SettingsModelBoolean) {
			return fromSettingsModelBoolean((SettingsModelBoolean) settingsModel, configName);
		}
		if (settingsModel instanceof SettingsModelDouble) {
			return fromSettingsModelDouble((SettingsModelDouble) settingsModel, configName);
		}
		if (settingsModel instanceof SettingsModelInteger) {
			return fromSettingsModelInteger((SettingsModelInteger) settingsModel, configName);
		}
		if (settingsModel instanceof SettingsModelLong) {
			return fromSettingsModelLong((SettingsModelLong) settingsModel, configName);
		}
		if (settingsModel instanceof SettingsModelString) {
			return fromSettingsModelString((SettingsModelString) settingsModel, configName);
		}
		if (settingsModel instanceof SettingsModelStringArray) {
			return fromSettingsModelStringArray((SettingsModelStringArray) settingsModel, configName);
		}
		throw new IllegalArgumentException(
				"Cannot convert settings model to config entry. No converter available for settings model type "
						+ settingsModel.getClass().getSimpleName() + ".");
	}

	public static ConfigEntry<Boolean> fromSettingsModelBoolean(final SettingsModelBoolean settingsModel,
			final String configName) {
		final ConfigEntry<Boolean> configEntry = new DefaultConfigEntry<>(configName, Boolean.class,
				settingsModel.getBooleanValue());
		configEntry.setEnabled(settingsModel.isEnabled());
		keepInSync(configEntry, settingsModel, SettingsModelBoolean::getBooleanValue,
				SettingsModelBoolean::setBooleanValue);
		return configEntry;
	}

	public static ConfigEntry<Double> fromSettingsModelDouble(final SettingsModelDouble settingsModel,
			final String configName) {
		final ConfigEntry<Double> configEntry = new DefaultConfigEntry<>(configName, Double.class,
				settingsModel.getDoubleValue());
		configEntry.setEnabled(settingsModel.isEnabled());
		keepInSync(configEntry, settingsModel, SettingsModelDouble::getDoubleValue,
				SettingsModelDouble::setDoubleValue);
		return configEntry;
	}

	public static ConfigEntry<Integer> fromSettingsModelInteger(final SettingsModelInteger settingsModel,
			final String configName) {
		final ConfigEntry<Integer> configEntry = new DefaultConfigEntry<>(configName, Integer.class,
				settingsModel.getIntValue());
		configEntry.setEnabled(settingsModel.isEnabled());
		keepInSync(configEntry, settingsModel, SettingsModelInteger::getIntValue, SettingsModelInteger::setIntValue);
		return configEntry;
	}

	public static ConfigEntry<Long> fromSettingsModelLong(final SettingsModelLong settingsModel,
			final String configName) {
		final ConfigEntry<Long> configEntry = new DefaultConfigEntry<>(configName, Long.class,
				settingsModel.getLongValue());
		configEntry.setEnabled(settingsModel.isEnabled());
		keepInSync(configEntry, settingsModel, SettingsModelLong::getLongValue, SettingsModelLong::setLongValue);
		return configEntry;
	}

	public static ConfigEntry<String> fromSettingsModelString(final SettingsModelString settingsModel,
			final String configName) {
		final ConfigEntry<String> configEntry = new DefaultConfigEntry<>(configName, String.class,
				settingsModel.getStringValue());
		configEntry.setEnabled(settingsModel.isEnabled());
		keepInSync(configEntry, settingsModel, SettingsModelString::getStringValue,
				SettingsModelString::setStringValue);
		return configEntry;
	}

	public static ConfigEntry<String[]> fromSettingsModelStringArray(final SettingsModelStringArray settingsModel,
			final String configName) {
		final ConfigEntry<String[]> configEntry = new DefaultConfigEntry<>(configName, String[].class,
				settingsModel.getStringArrayValue());
		configEntry.setEnabled(settingsModel.isEnabled());
		keepInSync(configEntry, settingsModel, SettingsModelStringArray::getStringArrayValue,
				SettingsModelStringArray::setStringArrayValue);
		return configEntry;
	}

	@SuppressWarnings("unchecked") // we ensure type safety
	public static SettingsModel toSettingsModel(final ConfigEntry<?> entry) {
		final Class<?> entryType = entry.getEntryType();
		if (Boolean.class.equals(entryType)) {
			return toSettingsModelBoolean((ConfigEntry<Boolean>) entry);
		}
		if (Double.class.equals(entryType)) {
			return toSettingsModelDouble((ConfigEntry<Double>) entry);
		}
		if (Integer.class.equals(entryType)) {
			return toSettingsModelInteger((ConfigEntry<Integer>) entry);
		}
		if (Long.class.equals(entryType)) {
			return toSettingsModelLong((ConfigEntry<Long>) entry);
		}
		if (String.class.equals(entryType)) {
			return toSettingsModelString((ConfigEntry<String>) entry);
		}
		if (String[].class.equals(entryType)) {
			return toSettingsModelStringArray((ConfigEntry<String[]>) entry);
		}
		throw new IllegalArgumentException("Cannot convert config entry '" + entry.getEntryKey()
				+ "' to settings model. No converter available for entry type " + entry.getEntryType() + ".");
	}

	public static SettingsModelBoolean toSettingsModelBoolean(final ConfigEntry<Boolean> entry) {
		return toSettingsModel(entry, SettingsModelBoolean::new, SettingsModelBoolean::getBooleanValue,
				SettingsModelBoolean::setBooleanValue);
	}

	public static SettingsModelDouble toSettingsModelDouble(final ConfigEntry<Double> entry) {
		return toSettingsModel(entry, SettingsModelDouble::new, SettingsModelDouble::getDoubleValue,
				SettingsModelDouble::setDoubleValue);
	}

	public static SettingsModelDoubleBounded toSettingsModelDoubleBounded(final ConfigEntry<Double> entry,
			final double minValue, final double maxValue) {
		final BiFunction<String, Double, SettingsModelDoubleBounded> settingsModelCreator = (key,
				value) -> new SettingsModelDoubleBounded(key, value, minValue, maxValue);
		return toSettingsModel(entry, settingsModelCreator, SettingsModelDoubleBounded::getDoubleValue,
				SettingsModelDoubleBounded::setDoubleValue);
	}

	public static SettingsModelInteger toSettingsModelInteger(final ConfigEntry<Integer> entry) {
		return toSettingsModel(entry, SettingsModelInteger::new, SettingsModelInteger::getIntValue,
				SettingsModelInteger::setIntValue);
	}

	public static SettingsModelIntegerBounded toSettingsModelIntegerBounded(final ConfigEntry<Integer> entry,
			final int minValue, final int maxValue) {
		final BiFunction<String, Integer, SettingsModelIntegerBounded> settingsModelCreator = (key,
				value) -> new SettingsModelIntegerBounded(key, value, minValue, maxValue);
		return toSettingsModel(entry, settingsModelCreator, SettingsModelIntegerBounded::getIntValue,
				SettingsModelIntegerBounded::setIntValue);
	}

	public static SettingsModelLong toSettingsModelLong(final ConfigEntry<Long> entry) {
		return toSettingsModel(entry, SettingsModelLong::new, SettingsModelLong::getLongValue,
				SettingsModelLong::setLongValue);
	}

	public static SettingsModelString toSettingsModelString(final ConfigEntry<String> entry) {
		return toSettingsModel(entry, SettingsModelString::new, SettingsModelString::getStringValue,
				SettingsModelString::setStringValue);
	}

	public static SettingsModelStringArray toSettingsModelStringArray(final ConfigEntry<String[]> entry) {
		return toSettingsModel(entry, SettingsModelStringArray::new, SettingsModelStringArray::getStringArrayValue,
				SettingsModelStringArray::setStringArrayValue);
	}

	public static <T, SM extends SettingsModel> SM toSettingsModel(final ConfigEntry<T> entry,
			final BiFunction<String, T, SM> settingsModelCreator, final Function<SM, T> settingsModelValueGetter,
			final BiConsumer<SM, T> settingsModelValueSetter) {
		final SM settingsModel = settingsModelCreator.apply(entry.getEntryKey(), entry.getValue());
		settingsModel.setEnabled(entry.getEnabled());
		keepInSync(entry, settingsModel, settingsModelValueGetter, settingsModelValueSetter);
		return settingsModel;
	}

	private static <T, SM extends SettingsModel> void keepInSync(final ConfigEntry<T> entry, final SM settingsModel,
			final Function<SM, T> settingsModelValueGetter, final BiConsumer<SM, T> settingsModelValueSetter) {
		entry.addLoadListener(e -> {
			settingsModel.setEnabled(e.getEnabled());
			settingsModelValueSetter.accept(settingsModel, e.getValue());
		});
		entry.addEnableChangeListener(e -> settingsModel.setEnabled(e.getEnabled()));
		entry.addValueChangeListener((e, oldValue) -> settingsModelValueSetter.accept(settingsModel, e.getValue()));
		settingsModel.addChangeListener(e -> {
			entry.setEnabled(settingsModel.isEnabled());
			entry.setValue(settingsModelValueGetter.apply(settingsModel));
		});
	}
}
