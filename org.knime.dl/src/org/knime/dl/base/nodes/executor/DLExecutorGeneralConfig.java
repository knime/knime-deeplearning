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
 *   Jun 13, 2017 (marcel): created
 */
package org.knime.dl.base.nodes.executor;

import java.util.Collection;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.dl.base.settings.AbstractConfig;
import org.knime.dl.base.settings.AbstractConfigEntry;
import org.knime.dl.base.settings.ConfigEntry;
import org.knime.dl.base.settings.DLGeneralConfig;
import org.knime.dl.base.settings.SettingsModelConfigEntries;
import org.knime.dl.core.DLNetwork;
import org.knime.dl.core.execution.DLExecutionContext;
import org.knime.dl.core.execution.DLExecutionContextRegistry;

/**
 * Note: all those config classes will certainly be generalized and extended in the future as they're probably viable
 * tools for saving/loading across all dl nodes. (e.g., they could implement a common super interface
 * 'DLNodeModelConfig' etc.)
 *
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
@Deprecated
class DLExecutorGeneralConfig extends AbstractConfig implements DLGeneralConfig<DLExecutionContext<?, ?>> {

	private static final String CFG_KEY_ROOT = "general_settings";

	private static final String CFG_KEY_EXEC_CTX = "backend";

	private static final String CFG_KEY_BATCH_SIZE = "batch_size";

	private static final String CFG_KEY_KEEP_INPUT_COLS = "keep_input_columns";


	@SuppressWarnings("rawtypes") // java limitation
    DLExecutorGeneralConfig(final String defaultBackendName, final String defaultBackendId,
			final int defaultBatchSize) {
	    super(CFG_KEY_ROOT);
	    put(new AbstractConfigEntry<DLExecutionContext>(CFG_KEY_EXEC_CTX, DLExecutionContext.class) {

            @Override
            public void saveSettingsTo(final NodeSettingsWO settings)
                throws InvalidSettingsException {
                final String[] ctx = new String[2];
                if (m_value == null) {
                    ctx[0] = defaultBackendName;
                    ctx[1] = defaultBackendId;
                } else {
                    ctx[0] = m_value.getName();
                    ctx[1] = m_value.getIdentifier();
                }
                settings.addStringArray(getEntryKey(), ctx);
            }

            @Override
            public void loadSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
                String[] newExecCtx = settings.getStringArray(getEntryKey());
                if (newExecCtx[1] != null) {
                    m_value = DLExecutionContextRegistry.getInstance().getExecutionContext(newExecCtx[1])
                        .orElseThrow(() -> new InvalidSettingsException("Executor back end '" + newExecCtx[0]
                                + "' ("
                                + newExecCtx[1]
                                        + ") could not be found. Are you missing a KNIME Deep Learning extension?"));
                }
            }

        });
		put(SettingsModelConfigEntries.createIntegerBoundedConfigEntry(CFG_KEY_BATCH_SIZE, defaultBatchSize,
		    1, Integer.MAX_VALUE));
		put(SettingsModelConfigEntries.createBooleanConfigEntry(CFG_KEY_KEEP_INPUT_COLS, false));
	}

	ConfigEntry<Integer> getBatchSizeEntry() {
	    return get(CFG_KEY_BATCH_SIZE, Integer.class);
	}

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public ConfigEntry<DLExecutionContext<?, ?>> getContextEntry() {
        return (ConfigEntry) get(CFG_KEY_EXEC_CTX, DLExecutionContext.class);
    }

    public ConfigEntry<Boolean> getKeepInputColumnsEntry() {
        return get(CFG_KEY_KEEP_INPUT_COLS, Boolean.class);
    }


    static Collection<DLExecutionContext<?, ?>> // NOSONAR Internal API. Types will be checked at a later point in time.
    getAvailableExecutionContexts(final Class<? extends DLNetwork> networkType) {
        return DLExecutionContextRegistry.getInstance().getExecutionContextsForNetworkType(networkType);
    }

}
