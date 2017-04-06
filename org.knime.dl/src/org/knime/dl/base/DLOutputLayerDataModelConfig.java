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
package org.knime.dl.base;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.knime.dl.util.DLUtils.Preconditions.checkNotNullOrEmpty;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.dl.core.DLLayerDataSpec;
import org.knime.dl.core.backend.DLBackend;
import org.knime.dl.core.backend.DLBackendRegistry;
import org.knime.dl.core.data.convert.output.DLLayerDataToDataCellConverterRegistry;

/**
 *
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
public class DLOutputLayerDataModelConfig {

	private static final String CFG_KEY_CONVERTER = "output_converter";

	private static final String CFG_KEY_OUTPUT_PREFIX = "output_prefix";

	private final String m_outputLayerDataName;

	private final SettingsModelString m_backendModel;

	private final SettingsModelString m_converterModel;

	private final SettingsModelString m_prefixModel;

	private final CopyOnWriteArrayList<ChangeListener> m_convertersChangeListeners;

	public DLOutputLayerDataModelConfig(final String outputLayerDataName, final SettingsModelString backendModel) {
		m_outputLayerDataName = checkNotNullOrEmpty(outputLayerDataName);
		m_backendModel = checkNotNull(backendModel);
		m_converterModel = new SettingsModelString(CFG_KEY_CONVERTER, "<none>");
		m_prefixModel = new SettingsModelString(CFG_KEY_OUTPUT_PREFIX, outputLayerDataName + "_");
		m_convertersChangeListeners = new CopyOnWriteArrayList<>();
		m_backendModel.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(final ChangeEvent e) {
				onBackendChanged();
			}
		});
	}

	// must equal layer data name, this is API
	public String getConfigKey() {
		return m_outputLayerDataName;
	}

	public String getOutputLayerDataName() {
		return m_outputLayerDataName;
	}

	public SettingsModelString getBackendModel() {
		return m_backendModel;
	}

	public SettingsModelString getConverterModel() {
		return m_converterModel;
	}

	public SettingsModelString getPrefixModel() {
		return m_prefixModel;
	}

	// TODO: what to do when there are no available/preferred converters?
	// exception? empty list/optional?
	public List<String> getAvailableConverters(final DLLayerDataSpec spec) {
		DLBackendRegistry.getInstance();
		final Optional<DLBackend> backend = DLBackendRegistry.getBackend(m_backendModel.getStringValue());
		if (!backend.isPresent()) {
			// TODO
			throw new RuntimeException("Backend not present!");
		}

		final DLLayerDataToDataCellConverterRegistry converters = DLLayerDataToDataCellConverterRegistry.getInstance();
		// now we can ask all the converters for readable buffers... compatible
		// to this one...

		return converters.getConverterFactories(backend.get().getReadableBufferType(spec)).stream()
				.map((c) -> c.getIdentifier()).collect(Collectors.toList());
	}

	public String getPreferredConverter(final DLLayerDataSpec spec) {
		return getAvailableConverters(spec).get(0);
	}

	public void addAvailableConvertersChangeListener(final ChangeListener l) {
		if (!m_convertersChangeListeners.contains(l)) {
			m_convertersChangeListeners.add(l);
		}
	}

	public void removeAvailableConvertersChangeListener(final ChangeListener l) {
		m_convertersChangeListeners.remove(l);
	}

	public void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
		final NodeSettingsRO cfgSettings = settings.getNodeSettings(m_outputLayerDataName);
		m_converterModel.validateSettings(cfgSettings);
		m_prefixModel.validateSettings(cfgSettings);
	}

	public void loadFromSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
		final NodeSettingsRO cfgSettings = settings.getNodeSettings(m_outputLayerDataName);
		m_converterModel.loadSettingsFrom(cfgSettings);
		m_prefixModel.loadSettingsFrom(cfgSettings);
	}

	public void saveToSettings(final NodeSettingsWO settings) {
		final NodeSettingsWO cfgSettings = settings.addNodeSettings(m_outputLayerDataName);
		m_converterModel.saveSettingsTo(cfgSettings);
		m_prefixModel.saveSettingsTo(cfgSettings);
	}

	private void onBackendChanged() {
		for (final ChangeListener l : m_convertersChangeListeners) {
			l.stateChanged(new ChangeEvent(this));
		}
	}
}
