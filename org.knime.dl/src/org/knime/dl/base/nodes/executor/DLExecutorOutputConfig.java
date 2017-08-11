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
 *   Jun 6, 2017 (marcel): created
 */
package org.knime.dl.base.nodes.executor;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.knime.dl.util.DLUtils.Preconditions.checkNotNullOrEmpty;

import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
class DLExecutorOutputConfig {

	private static final String CFG_KEY_CONVERTER = "output_converter";

	private static final String CFG_KEY_OUTPUT_PREFIX = "output_prefix";

	private final String m_outputLayerDataName;

	private final SettingsModelString m_smExecutionContext;

	private final SettingsModelString m_smConverter;

	private final SettingsModelString m_smPrefix;

	private final CopyOnWriteArrayList<ChangeListener> m_backendChangeListeners;

	DLExecutorOutputConfig(final String outputLayerDataName, final SettingsModelString executionContextModel) {
		m_outputLayerDataName = checkNotNullOrEmpty(outputLayerDataName);
		m_smExecutionContext = checkNotNull(executionContextModel);
		m_smConverter = new SettingsModelString(CFG_KEY_CONVERTER, null);
		m_smPrefix = new SettingsModelString(CFG_KEY_OUTPUT_PREFIX, outputLayerDataName + "_");
		m_backendChangeListeners = new CopyOnWriteArrayList<>();
		m_smExecutionContext.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(final ChangeEvent e) {
				onBackendChanged();
			}
		});
	}

	/**
	 * Equivalent to {@link #getOutputLayerDataName()}.
	 */
	String getConfigKey() {
		return m_outputLayerDataName;
	}

	String getOutputLayerDataName() {
		return m_outputLayerDataName;
	}

	SettingsModelString getExecutionContextModel() {
		return m_smExecutionContext;
	}

	SettingsModelString getConverterModel() {
		return m_smConverter;
	}

	SettingsModelString getPrefixModel() {
		return m_smPrefix;
	}

	void addBackendChangeListener(final ChangeListener l) {
		if (!m_backendChangeListeners.contains(l)) {
			m_backendChangeListeners.add(l);
		}
	}

	void removeBackendChangeListener(final ChangeListener l) {
		m_backendChangeListeners.remove(l);
	}

	void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
		final NodeSettingsRO cfgSettings = settings.getNodeSettings(m_outputLayerDataName);
		m_smConverter.validateSettings(cfgSettings);
		m_smPrefix.validateSettings(cfgSettings);
	}

	void loadFromSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
		final NodeSettingsRO cfgSettings = settings.getNodeSettings(m_outputLayerDataName);
		m_smConverter.loadSettingsFrom(cfgSettings);
		m_smPrefix.loadSettingsFrom(cfgSettings);
	}

	void saveToSettings(final NodeSettingsWO settings) {
		final NodeSettingsWO cfgSettings = settings.addNodeSettings(m_outputLayerDataName);
		m_smConverter.saveSettingsTo(cfgSettings);
		m_smPrefix.saveSettingsTo(cfgSettings);
	}

	private void onBackendChanged() {
		for (final ChangeListener l : m_backendChangeListeners) {
			l.stateChanged(new ChangeEvent(this));
		}
	}
}
