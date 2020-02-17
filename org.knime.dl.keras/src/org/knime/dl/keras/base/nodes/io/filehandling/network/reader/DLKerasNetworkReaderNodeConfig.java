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
 *   Feb 17, 2020 (benjamin): created
 */
package org.knime.dl.keras.base.nodes.io.filehandling.network.reader;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.filehandling.core.node.portobject.reader.PortObjectReaderNodeConfig;

/**
 * Config of the Keras network reader.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 */
final class DLKerasNetworkReaderNodeConfig extends PortObjectReaderNodeConfig {

    private static final String[] FILE_SUFFIXES = {".h5", ".json", ".yaml", ".yml"};

    private static final String CFG_KERAS_BACKEND = "keras_backend";

    private final SettingsModelString m_kerasBackend;

    DLKerasNetworkReaderNodeConfig() {
        super(FILE_SUFFIXES);
        m_kerasBackend = new SettingsModelString(CFG_KERAS_BACKEND, "");
    }

    SettingsModelString getKerasBackend() {
        return m_kerasBackend;
    }

    @Override
    protected void validateConfigurationForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.validateConfigurationForModel(settings);
        m_kerasBackend.validateSettings(settings);
    }

    @Override
    protected void saveConfigurationForModel(final NodeSettingsWO settings) {
        super.saveConfigurationForModel(settings);
        m_kerasBackend.saveSettingsTo(settings);
    }

    @Override
    protected void loadConfigurationForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        super.loadConfigurationForModel(settings);
        m_kerasBackend.loadSettingsFrom(settings);
    }

    @Override
    protected void saveConfigurationForDialog(final NodeSettingsWO settings) throws InvalidSettingsException {
        super.saveConfigurationForDialog(settings);
        m_kerasBackend.saveSettingsTo(settings);
    }

    @Override
    protected void loadConfigurationForDialog(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        super.loadConfigurationForDialog(settings, specs);
        try {
            m_kerasBackend.loadSettingsFrom(settings);
        } catch (final InvalidSettingsException e) {
            // Nothing to do
        }
    }
}
