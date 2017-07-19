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
 *   Jun 13, 2017 (marcel): created
 */
package org.knime.dl.base.nodes.executor;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * Note: all those config classes will certainly be generalized and extended in the future as they're probably viable
 * tools for saving/loading across all dl nodes. (e.g., they could implement a common super interface
 * 'DLNodeModelConfig' etc.)
 *
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
class DLGeneralModelConfig {

    private static final String CFG_KEY_ROOT = "general_settings";

    private static final String CFG_KEY_BACKEND = "backend";

    private static final String CFG_KEY_BATCH_SIZE = "batch_size";

    private static final String CFG_KEY_KEEP_INPUT_COLS = "keep_input_columns";

    private final SettingsModelString m_backend;

    private final SettingsModelIntegerBounded m_batchSize;

    private final SettingsModelBoolean m_keepInputColumns;

    DLGeneralModelConfig(final String defaultBackend, final int defaultBatchSize) {
        m_keepInputColumns = new SettingsModelBoolean(CFG_KEY_KEEP_INPUT_COLS, false);
        m_backend = new SettingsModelString(CFG_KEY_BACKEND, defaultBackend);
        m_batchSize = new SettingsModelIntegerBounded(CFG_KEY_BATCH_SIZE, defaultBatchSize, 1, Integer.MAX_VALUE);
    }

    SettingsModelString getBackendModel() {
        return m_backend;
    }

    SettingsModelIntegerBounded getBatchSizeModel() {
        return m_batchSize;
    }

    SettingsModelBoolean getKeepInputColumns() {
        return m_keepInputColumns;
    }

    void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        final NodeSettingsRO cfgSettings = settings.getNodeSettings(CFG_KEY_ROOT);
        m_backend.validateSettings(cfgSettings);
        m_batchSize.validateSettings(cfgSettings);
        m_keepInputColumns.validateSettings(cfgSettings);
    }

    void loadFromSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        final NodeSettingsRO cfgSettings = settings.getNodeSettings(CFG_KEY_ROOT);
        m_backend.loadSettingsFrom(cfgSettings);
        m_batchSize.loadSettingsFrom(cfgSettings);
        m_keepInputColumns.loadSettingsFrom(cfgSettings);
    }

    void saveToSettings(final NodeSettingsWO settings) {
        final NodeSettingsWO cfgSettings = settings.addNodeSettings(CFG_KEY_ROOT);
        m_backend.saveSettingsTo(cfgSettings);
        m_batchSize.saveSettingsTo(cfgSettings);
        m_keepInputColumns.saveSettingsTo(cfgSettings);
    }
}
