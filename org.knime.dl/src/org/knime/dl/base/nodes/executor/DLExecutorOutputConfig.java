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
 * History
 *   Jun 6, 2017 (marcel): created
 */
package org.knime.dl.base.nodes.executor;

import static org.knime.dl.util.DLUtils.Preconditions.checkNotNullOrEmpty;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;
import org.knime.dl.base.settings.ConfigEntry;
import org.knime.dl.base.settings.DLAbstractIOConfig;
import org.knime.dl.base.settings.SettingsModelConfigEntries;
import org.knime.dl.base.settings.SettingsModelConfigEntry;
import org.knime.dl.core.data.convert.DLTensorToDataCellConverterFactory;
import org.knime.dl.core.data.convert.DLTensorToDataCellConverterRegistry;

/**
 * 
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class DLExecutorOutputConfig extends DLAbstractIOConfig<DLExecutorGeneralConfig> {

	private static final String CFG_KEY_CONVERTER = "output_converter";

	private static final String CFG_KEY_OUTPUT_PREFIX = "output_prefix";


	DLExecutorOutputConfig(final String outputTensorName, final DLExecutorGeneralConfig generalConfig) {
	    super(checkNotNullOrEmpty(outputTensorName), generalConfig);
		put(new SettingsModelConfigEntry<>(
		        CFG_KEY_CONVERTER, DLTensorToDataCellConverterFactory.class,
		        s -> new SettingsModelStringArray(s, null),
		        this::entryToSettingsModel,
		        this::settingsModelToValue));
		put(SettingsModelConfigEntries.createStringConfigEntry(CFG_KEY_OUTPUT_PREFIX, outputTensorName + "_"));
	}
	
	@SuppressWarnings("rawtypes")
    private SettingsModelStringArray entryToSettingsModel(ConfigEntry<DLTensorToDataCellConverterFactory> entry) {
	    DLTensorToDataCellConverterFactory<?, ?> cf = entry.getValue();
	    return new SettingsModelStringArray(entry.getEntryKey(), new String[] {cf.getName(), cf.getIdentifier()});
	}
	
	private DLTensorToDataCellConverterFactory<?, ?> settingsModelToValue(SettingsModelStringArray sm) throws InvalidSettingsException {
	    String[] array = sm.getStringArrayValue();
	    return DLTensorToDataCellConverterRegistry.getInstance().getConverterFactory(array[1])
	            .orElseThrow(() -> new InvalidSettingsException("Converter '" + array[0] + " ("
	                    + array[1] + ") for network output '" + getTensorName()
	                    + "' could not be found. Are you missing a KNIME extension?"));
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
    ConfigEntry<DLTensorToDataCellConverterFactory<?, ?>> getConverterEntry() {
	    return (ConfigEntry) get(CFG_KEY_CONVERTER, DLTensorToDataCellConverterFactory.class);
	}
	
	ConfigEntry<String> getPrefixEntry() {
	    return get(CFG_KEY_OUTPUT_PREFIX, String.class);
	}

}
