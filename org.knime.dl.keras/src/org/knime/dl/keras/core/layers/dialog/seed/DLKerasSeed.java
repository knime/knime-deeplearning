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
package org.knime.dl.keras.core.layers.dialog.seed;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.dl.keras.core.layers.dialog.AbstractOptionalWidgetType;
import org.knime.dl.python.util.DLPythonUtils;

/**
 * Class representing a random seed.
 * 
 * @author Knime, KNIME GmbH, Konstanz, Germany
 */
public class DLKerasSeed extends AbstractOptionalWidgetType {

    /** */
    public static final String SETTINGS_KEY_IS_ENABLED = "DLKerasSeed.IsEnabled";

    private static final long DEFAULT_SEED = 123456789L;

    private long m_seed;

    private final boolean m_isOptional;

    /**
     * Constructor
     * 
     * @param seed the initial seed
     */
    public DLKerasSeed(final long seed, final boolean defaultEnabled, final boolean isOptional) {
        super(defaultEnabled);
        m_seed = seed;
        m_isOptional = isOptional;
    }

    /**
     * Constructor using the default seed 123456789.
     */
    public DLKerasSeed(final boolean defaultEnabled, final boolean isOptional) {
        super(defaultEnabled);
        m_seed = DEFAULT_SEED;
        m_isOptional = isOptional;
    }

    /**
     * @return the seed value
     */
    public long getSeed() {
        return m_seed;
    }

    /**
     * @return return whether optional or not
     */
    public boolean isOptional() {
        return m_isOptional;
    }

    /**
     * @return the Python representation of this seed
     */
    public String toPytonSeed() {
        if (!isEnabled()) {
            return DLPythonUtils.NONE;
        }
        return String.valueOf(m_seed);
    }

    /**
     * Convenience method to saves the specified seed to the specified NodeSettings.
     * 
     * @param seed the seed to save
     * @param settings the settings to write to
     */
    public static void saveTo(DLKerasSeed seed, NodeSettingsWO settings, String key) {
        settings.addLong(key, seed.getSeed());
        settings.addBoolean(SETTINGS_KEY_IS_ENABLED, seed.isEnabled());
    }

    /**
     * Convenience method to load the seed from the specified NodeSettings.
     * 
     * @param settings the settings to load from
     * @return the loaded seed if existing, else null
     * @throws InvalidSettingsException
     */
    public static DLKerasSeed loadFrom(NodeSettingsRO settings, String key) throws InvalidSettingsException {
        boolean isEnabled =
            settings.containsKey(SETTINGS_KEY_IS_ENABLED) ? settings.getBoolean(SETTINGS_KEY_IS_ENABLED) : true;
        DLKerasSeed ks = new DLKerasSeed(settings.getLong(key), isEnabled, false);
        return ks;
    }
}
