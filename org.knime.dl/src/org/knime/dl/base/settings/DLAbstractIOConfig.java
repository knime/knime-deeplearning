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

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.dl.core.DLTensorId;

/**
 * Holds the general config as well as the tensor identifier. It also handles loading on an abstract level.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <C> the type of general config
 */
public abstract class DLAbstractIOConfig <C extends DLGeneralConfig<?>> extends AbstractConfig implements DLIOConfig<C> {

    /**
     * Config value for the null converter.
     */
    protected static final String CFG_VALUE_NULL_CONVERTER = "null";

    private final DLTensorId m_tensorId;

    private final String m_tensorName;

    private final C m_generalConfig;

    /**
     * @param tensorId the identifier of the tensor this config is concerned with. May be <code>null</code> for old
     *            configs where no tensor id is present. In this case <code>tensorName</code> must not be
     *            <code>null</code>.
     * @param tensorName the name of the tensor this config is concerned with. This argument is solely used to support
     *            older versions of this config (KNIME 3.5) that used the tensor name as config key. It may therefore be
     *            <code>null</code> for new configs. In this case <code>tensorId</code> must not be <code>null</code>.
     * @param generalCfg the general config held by this config
     */
    protected DLAbstractIOConfig(final DLTensorId tensorId, final String tensorName, final C generalCfg) {
        super(tensorId != null ? tensorId.getIdentifierString() : tensorName);
        m_tensorId = tensorId;
        m_tensorName = tensorName;
        m_generalConfig = checkNotNull(generalCfg);
    }

    public String getTensorNameOrId() {
        return m_tensorName != null ? m_tensorName : m_tensorId.getIdentifierString();
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
        if (settings.containsKey(getConfigKey()) || settings.containsKey(m_tensorName)) {
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
    public void loadFromSettingsInDialog(final NodeSettingsRO settings, final DataTableSpec spec)
        throws InvalidSettingsException {
        // Backward compatibility: Try to use tensor name if regular config key (tensor id) is not present.
        String configKey = null;
        if (settings.containsKey(getConfigKey())) {
            configKey = getConfigKey();
        } else if (settings.containsKey(m_tensorName)) {
            configKey = m_tensorName;
        }
        if (configKey != null) {
            loadFromSettings(settings);
            loadInDialog(settings.getNodeSettings(configKey), spec);
        }
    }

    /**
     * @param settings the settings for this config
     * @param spec of the input table
     * @throws InvalidSettingsException if the settings are not valid
     *
     */
    protected void loadInDialog(final NodeSettingsRO settings, final DataTableSpec spec) throws InvalidSettingsException {
        // no op
    }

    @Override
    protected boolean handleFailureToLoadConfig(final NodeSettingsRO settings, final Exception cause) {
        // Backward compatibility: Try to load config using tensor name instead of tensor id.
        // If the tensor id is null, we already used the tensor name for loading.
        if (m_tensorName != null && m_tensorId != null) {
            try {
                loadConfigFromSettings(settings, m_tensorName);
                return true;
            } catch (final InvalidSettingsException e) {
                // ignore, return false below
            }
        }
        return false;
    }
}
