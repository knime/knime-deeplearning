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

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;

/**
 * Holds the general config as well as the tensor name.
 * It also handles loading on an abstract level.
 * 
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <C> the type of general config
 */
public abstract class DLAbstractIOConfig <C extends DLGeneralConfig<?>> extends AbstractConfig implements DLIOConfig<C> {
    
    /**
     * Config value for the null converter.
     */
    protected static final String CFG_VALUE_NULL_CONVERTER = "null";
    
    private final String m_tensorName;

    private final C m_generalConfig;
    
    /**
     * @param tensorName the name of the tensor this config is concerned with
     * @param generalCfg the general config held by this config
     */
    protected DLAbstractIOConfig(final String tensorName, final C generalCfg) {
        super(checkNotNullOrEmpty(tensorName));
        m_tensorName = tensorName;
        m_generalConfig = checkNotNull(generalCfg);
    }
    
    /**
     * @return the name of the tensor
     */
    public final String getTensorName() {
        return m_tensorName;
    }
    
    @Override
    public final C getGeneralConfig() {
        return m_generalConfig;
    }
    
    /**
     * @param settings the in or output settings
     * @throws InvalidSettingsException if the settings are not valid
     */
    public void loadFromSettingsInModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        if (settings.containsKey(getTensorName())) {
            loadFromSettings(settings);
            loadInModel(settings.getNodeSettings(getConfigKey()));
        }
    }
    
    /**
     * @param settings the settings for this config
     * @throws InvalidSettingsException if the settings are not valid
     * 
     */
    protected void loadInModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        // no op
    }
    
    @Override
    public void loadFromSettingsInDialog(final NodeSettingsRO settings, final DataTableSpec spec) throws InvalidSettingsException {
        if (settings.containsKey(getTensorName())) {
            loadFromSettings(settings);
            loadInDialog(settings.getNodeSettings(getConfigKey()), spec);
        }
    }
    
    /**
     * @param settings the settings for this config
     * @param spec of the input table
     * @throws InvalidSettingsException if the settings are not valid 
     * 
     */
    protected void loadInDialog(final NodeSettingsRO settings, DataTableSpec spec) throws InvalidSettingsException {
        // no op
    }
    
}
