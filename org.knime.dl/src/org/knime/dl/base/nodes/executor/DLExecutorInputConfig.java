/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
package org.knime.dl.base.nodes.executor;

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
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;
import org.knime.core.node.util.filter.InputFilter;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;

/**
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
class DLExecutorInputConfig {

	private static final String CFG_KEY_CONVERTER = "converter";

	private static final String CFG_KEY_INPUT_COL = "input_columns";

	private final String m_inputTensorName;

	private final DLExecutorGeneralConfig m_generalConfig;

	private final SettingsModelStringArray m_smConverter;

	private DataColumnSpecFilterConfiguration m_smInputCol;

	DLExecutorInputConfig(final String inputTensorName, final DLExecutorGeneralConfig generalCfg) {
		m_inputTensorName = checkNotNullOrEmpty(inputTensorName);
		m_generalConfig = checkNotNull(generalCfg);
		m_smConverter = new SettingsModelStringArray(CFG_KEY_CONVERTER, new String[2]);
		m_smInputCol =
				new DataColumnSpecFilterConfiguration(CFG_KEY_INPUT_COL, new DLDataTypeColumnFilter(DataValue.class));
	}

	/**
	 * Equivalent to {@link #getInputTensorName()}.
	 */
	String getConfigKey() {
		return m_inputTensorName;
	}

	String getInputTensorName() {
		return m_inputTensorName;
	}

	DLExecutorGeneralConfig getGeneralConfig() {
		return m_generalConfig;
	}

	SettingsModelStringArray getConverterModel() {
		return m_smConverter;
	}

	DataColumnSpecFilterConfiguration getInputColumnsModel() {
		return m_smInputCol;
	}

	void setInputColumnsModelFilter(final DLDataTypeColumnFilter inputColumnsFilter) {
		m_smInputCol = new DataColumnSpecFilterConfiguration(CFG_KEY_INPUT_COL, inputColumnsFilter);
	}

	NodeSettingsRO validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
		final NodeSettingsRO cfgSettings = settings.getNodeSettings(m_inputTensorName);
		m_smConverter.validateSettings(cfgSettings);
		return cfgSettings;
	}

	void loadFromSettingsInModel(final NodeSettingsRO settings) throws InvalidSettingsException {
		final NodeSettingsRO child = settings.getNodeSettings(m_inputTensorName);
		if (settings.containsKey(m_inputTensorName)) {
			m_smConverter.loadSettingsFrom(child);
			m_smInputCol.loadConfigurationInModel(child);
		}
	}

	void loadFromSettingsInDialog(final NodeSettingsRO settings, final DataTableSpec spec)
			throws InvalidSettingsException {
		// we enforce inclusion by default
		m_smInputCol.loadDefault(spec, null, true);
		final NodeSettingsRO child = settings.getNodeSettings(m_inputTensorName);
		if (settings.containsKey(m_inputTensorName)) {
			m_smConverter.loadSettingsFrom(child);
			m_smInputCol.loadConfigurationInDialog(child, spec);
		}
	}

	void saveToSettings(final NodeSettingsWO settings) {
		final NodeSettingsWO cfgSettings = settings.addNodeSettings(m_inputTensorName);
		m_smConverter.saveSettingsTo(cfgSettings);
		m_smInputCol.saveConfiguration(cfgSettings);
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
				throw new NullPointerException("List of value classes must not " + "contain null elements.");
			}
			m_filterClasses = filterValueClasses;
		}
	}
}
