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

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;
import org.knime.dl.core.data.convert.DLDataValueToTensorConverterFactory;

/**
 * Handles loading of the column filter configuration.
 * 
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <C> the type of {@link DLGeneralConfig} e.g. learner or executor
 */
public abstract class DLAbstractInputConfig<C extends DLGeneralConfig<?>> extends DLAbstractIOConfig<C> implements DLInputConfig<C> {

    /**
     * Config key for the converter
     */
    protected static final String CFG_KEY_CONVERTER = "converter";
    /**
     * Config key for the input columns filter configuration
     */
    public static final String CFG_KEY_INPUT_COL = "input_columns";

    /**
     * @param tensorName
     * @param generalCfg
     */
    public DLAbstractInputConfig(String tensorName, C generalCfg) {
        super(tensorName, generalCfg);
    }

    private DataColumnSpecFilterConfiguration getInputColumnConfig() {
        return getEntryValue(CFG_KEY_INPUT_COL, DataColumnSpecFilterConfiguration.class);
    }

    @Override
    public void loadInModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        getInputColumnConfig().loadConfigurationInModel(findInputColConfigParent(settings));
    }

    @Override
    public void loadInDialog(final NodeSettingsRO settings, final DataTableSpec spec) throws InvalidSettingsException {
        DataColumnSpecFilterConfiguration inputColConfig = getInputColumnConfig();
        // we enforce inclusion by default
        inputColConfig.loadDefault(spec, null, true);
        inputColConfig.loadConfigurationInDialog(findInputColConfigParent(settings), spec);
    }
    
    private static NodeSettingsRO findInputColConfigParent(NodeSettingsRO settings) throws InvalidSettingsException {
        NodeSettingsRO parent = settings;
        NodeSettingsRO child = settings.getNodeSettings(CFG_KEY_INPUT_COL);
        while (child.containsKey(CFG_KEY_INPUT_COL)) {
            parent = child;
            child = parent.getNodeSettings(CFG_KEY_INPUT_COL);
        }
        return parent;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public ConfigEntry<DLDataValueToTensorConverterFactory<?, ?>> getConverterEntry() {
        return (ConfigEntry) get(CFG_KEY_CONVERTER, DLDataValueToTensorConverterFactory.class);
    }

    @Override
    public ConfigEntry<DataColumnSpecFilterConfiguration> getInputColumnsEntry() {
        return get(CFG_KEY_INPUT_COL, DataColumnSpecFilterConfiguration.class);
    }

}