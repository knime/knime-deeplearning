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
 * History
 *   May 31, 2017 (marcel): created
 */
package org.knime.dl.keras.base.nodes.learner;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.knime.dl.util.DLUtils.Preconditions.checkNotNullOrEmpty;

import java.util.Arrays;
import java.util.List;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.filter.InputFilter;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;
import org.knime.dl.base.settings.AbstractConfig;
import org.knime.dl.base.settings.AbstractConfigEntry;
import org.knime.dl.base.settings.ConfigEntry;
import org.knime.dl.core.data.convert.DLDataValueToTensorConverterFactory;
import org.knime.dl.core.data.convert.DLDataValueToTensorConverterRegistry;
import org.knime.dl.keras.core.training.DLKerasLossFunction;

/**
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
final class DLKerasLearnerTargetConfig extends AbstractConfig {

	private static final String CFG_KEY_CONVERTER = "converter";

	private static final String CFG_KEY_INPUT_COL = "input_columns";

	private static final String CFG_KEY_LOSS_FUNC = "loss_function";

	private final String m_targetTensorName;

	private final DLKerasLearnerGeneralConfig m_generalConfig;

	@SuppressWarnings("rawtypes") // Java limitation
	DLKerasLearnerTargetConfig(final String targetTensorName, final DLKerasLearnerGeneralConfig generalCfg) {
		super(checkNotNullOrEmpty(targetTensorName));
		m_targetTensorName = targetTensorName;
		m_generalConfig = checkNotNull(generalCfg);
		put(new AbstractConfigEntry<DLDataValueToTensorConverterFactory>(CFG_KEY_CONVERTER,
				DLDataValueToTensorConverterFactory.class) {

			@Override
			protected void saveEntry(final NodeSettingsWO settings)
					throws InvalidSettingsException, UnsupportedOperationException {
				settings.addString(getEntryKey(), m_value.getIdentifier());
			}

			@Override
			protected void loadEntry(final NodeSettingsRO settings)
					throws InvalidSettingsException, IllegalStateException, UnsupportedOperationException {
				final String converterIdentifier = settings.getString(getEntryKey());
				m_value = DLDataValueToTensorConverterRegistry.getInstance().getConverterFactory(converterIdentifier)
						.orElseThrow(() -> new InvalidSettingsException("Target data converter '" + converterIdentifier
								+ "' could not be found. Are you missing a KNIME extension?"));
			}
		});
		put(new AbstractConfigEntry<DataColumnSpecFilterConfiguration>(CFG_KEY_INPUT_COL,
				DataColumnSpecFilterConfiguration.class,
				new DataColumnSpecFilterConfiguration(CFG_KEY_INPUT_COL, new DLDataTypeColumnFilter(DataValue.class))) {

			@Override
			protected void saveEntry(final NodeSettingsWO settings)
					throws InvalidSettingsException, UnsupportedOperationException {
				final NodeSettingsWO subSettings = settings.addNodeSettings(CFG_KEY_INPUT_COL);
				m_value.saveConfiguration(subSettings);
			}

			@Override
			protected void loadEntry(final NodeSettingsRO settings)
					throws InvalidSettingsException, IllegalStateException, UnsupportedOperationException {
				// no op. Separate routines for loading in model and dialog required. See below.
			}
		});
		put(new AbstractConfigEntry<DLKerasLossFunction>(CFG_KEY_LOSS_FUNC, DLKerasLossFunction.class) {

			@Override
			protected void saveEntry(final NodeSettingsWO settings)
					throws InvalidSettingsException, UnsupportedOperationException {
				final String identifier = m_value != null ? m_value.getClass().getCanonicalName() : "null";
				settings.addString(getEntryKey(), identifier);
			}

			@Override
			protected void loadEntry(final NodeSettingsRO settings)
					throws InvalidSettingsException, IllegalStateException, UnsupportedOperationException {
				final String identifier = settings.getString(getEntryKey());
				if (!identifier.equals("null")) {
					m_value = m_generalConfig.getTrainingContextEntry().getValue().createLossFunctions().stream()
							.filter(o -> o.getClass().getCanonicalName().equals(identifier)) //
							.findFirst() //
							.orElseThrow(() -> new InvalidSettingsException(
									"Loss function '" + identifier + "' of target '" + m_targetTensorName
											+ " could not be found. Are you missing a KNIME Deep Learning extension?"));
				}
			}
		});
	}

	String getTargetTensorName() {
		return m_targetTensorName;
	}

	DLKerasLearnerGeneralConfig getGeneralConfig() {
		return m_generalConfig;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	ConfigEntry<DLDataValueToTensorConverterFactory<?, ?>> getConverterEntry() {
		return (ConfigEntry) get(CFG_KEY_CONVERTER, DLDataValueToTensorConverterFactory.class);
	}

	ConfigEntry<DataColumnSpecFilterConfiguration> getInputColumnsEntry() {
		return get(CFG_KEY_INPUT_COL, DataColumnSpecFilterConfiguration.class);
	}

	ConfigEntry<DLKerasLossFunction> getLossFunctionEntry() {
		return get(CFG_KEY_LOSS_FUNC, DLKerasLossFunction.class);
	}

	void loadFromSettingsInModel(final NodeSettingsRO settings) throws InvalidSettingsException {
		if (settings.containsKey(m_targetTensorName)) {
			loadFromSettings(settings);
			final DataColumnSpecFilterConfiguration inputColumnConfig = getEntryValue(CFG_KEY_INPUT_COL,
					DataColumnSpecFilterConfiguration.class);
			final NodeSettingsRO inputColumnSettings = settings.getNodeSettings(getConfigKey())
					.getNodeSettings(CFG_KEY_INPUT_COL).getNodeSettings(CFG_KEY_INPUT_COL);
			inputColumnConfig.loadConfigurationInModel(inputColumnSettings);
		}
	}

	void loadFromSettingsInDialog(final NodeSettingsRO settings, final DataTableSpec spec)
			throws InvalidSettingsException {
		// we enforce inclusion by default
		final DataColumnSpecFilterConfiguration inputColumnConfig = getEntryValue(CFG_KEY_INPUT_COL,
				DataColumnSpecFilterConfiguration.class);
		inputColumnConfig.loadDefault(spec, null, true);
		if (settings.containsKey(m_targetTensorName)) {
			loadFromSettings(settings);
			final NodeSettingsRO inputColumnSettings = settings.getNodeSettings(getConfigKey())
					.getNodeSettings(CFG_KEY_INPUT_COL).getNodeSettings(CFG_KEY_INPUT_COL);
			inputColumnConfig.loadConfigurationInDialog(inputColumnSettings, spec);
		}
	}

	// TODO: this is a workaround
	static class DLDataTypeColumnFilter extends InputFilter<DataColumnSpec> {

		private Class<? extends DataValue>[] m_filterClasses;

		@SafeVarargs
		public DLDataTypeColumnFilter(final Class<? extends DataValue>... filterValueClasses) {
			setFilterClasses(filterValueClasses);
		}

		@Override
		public final boolean include(final DataColumnSpec cspec) {
			for (final Class<? extends DataValue> cl : m_filterClasses) {
				if (cspec.getType().isCompatible(cl)) {
					return true;
				}
			}
			return false;
		}

		Class<? extends DataValue>[] getFilterClasses() {
			return m_filterClasses;
		}

		@SafeVarargs
		final void setFilterClasses(final Class<? extends DataValue>... filterValueClasses) {
			if (filterValueClasses == null || filterValueClasses.length == 0) {
				throw new NullPointerException("Classes must not be null");
			}
			final List<Class<? extends DataValue>> list = Arrays.asList(filterValueClasses);
			if (list.contains(null)) {
				throw new NullPointerException("List of value classes must not contain null elements.");
			}
			m_filterClasses = filterValueClasses;
		}
	}
}
