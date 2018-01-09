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

import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;

/**
 * Note: all those config classes will certainly be generalized and extended in the future as they're probably viable
 * tools for saving/loading across all dl nodes. (e.g., they could implement a common super interface
 * 'DLNodeModelConfig' etc.)
 *
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
class DLExecutorGeneralConfig {

	private static final String CFG_KEY_ROOT = "general_settings";

	private static final String CFG_KEY_EXEC_CTX = "backend";

	private static final String CFG_KEY_BATCH_SIZE = "batch_size";

	private static final String CFG_KEY_KEEP_INPUT_COLS = "keep_input_columns";

	private final String[] m_executionContext;

	private final SettingsModelIntegerBounded m_smBatchSize;

	private final SettingsModelBoolean m_smKeepInputColumns;

	private final CopyOnWriteArrayList<ChangeListener> m_execCtxChangeListeners;

	DLExecutorGeneralConfig(final String defaultBackendName, final String defaultBackendId,
			final int defaultBatchSize) {
		m_executionContext = new String[] { defaultBackendName, defaultBackendId };
		m_smBatchSize = new SettingsModelIntegerBounded(CFG_KEY_BATCH_SIZE, defaultBatchSize, 1, Integer.MAX_VALUE);
		m_smKeepInputColumns = new SettingsModelBoolean(CFG_KEY_KEEP_INPUT_COLS, false);
		m_execCtxChangeListeners = new CopyOnWriteArrayList<>();
	}

	String[] getExecutionContext() {
		return m_executionContext.clone();
	}

	void setExecutionContext(final String newExecCtxName, final String newExecCtxId) {
		if (newExecCtxId != null && !newExecCtxId.equals(m_executionContext[1]) && newExecCtxName != null
		// TODO: remove hard-coded values
				&& !newExecCtxName.equals("<none>") && !newExecCtxName.equals(m_executionContext[0])) {
			m_executionContext[0] = newExecCtxName;
			m_executionContext[1] = newExecCtxId;
			notifyExecutionContextChangeListeners();
		}
	}

	SettingsModelIntegerBounded getBatchSizeModel() {
		return m_smBatchSize;
	}

	SettingsModelBoolean getKeepInputColumnsModel() {
		return m_smKeepInputColumns;
	}

	void addExecutionContextChangeListener(final ChangeListener l) {
		if (!m_execCtxChangeListeners.contains(l)) {
			m_execCtxChangeListeners.add(l);
		}
	}

	void removeExecutionContextChangeListener(final ChangeListener l) {
		m_execCtxChangeListeners.remove(l);
	}

	void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
		final NodeSettingsRO cfgSettings = settings.getNodeSettings(CFG_KEY_ROOT);
		if (cfgSettings.getStringArray(CFG_KEY_EXEC_CTX).length != m_executionContext.length) {
			throw new InvalidSettingsException("Saved settings for '" + CFG_KEY_EXEC_CTX
					+ "' are of invalid size. Expected element count is " + m_executionContext.length + ".");
		}
		m_smBatchSize.validateSettings(cfgSettings);
		m_smKeepInputColumns.validateSettings(cfgSettings);
	}

	void loadFromSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
		final NodeSettingsRO cfgSettings = settings.getNodeSettings(CFG_KEY_ROOT);
		final String[] newExecCtx = cfgSettings.getStringArray(CFG_KEY_EXEC_CTX);
		setExecutionContext(newExecCtx[0], newExecCtx[1]);
		m_smBatchSize.loadSettingsFrom(cfgSettings);
		m_smKeepInputColumns.loadSettingsFrom(cfgSettings);
	}

	void saveToSettings(final NodeSettingsWO settings) {
		final NodeSettingsWO cfgSettings = settings.addNodeSettings(CFG_KEY_ROOT);
		cfgSettings.addStringArray(CFG_KEY_EXEC_CTX, m_executionContext);
		m_smBatchSize.saveSettingsTo(cfgSettings);
		m_smKeepInputColumns.saveSettingsTo(cfgSettings);
	}

	private void notifyExecutionContextChangeListeners() {
		for (final ChangeListener l : m_execCtxChangeListeners) {
			l.stateChanged(new ChangeEvent(this));
		}
	}
}
