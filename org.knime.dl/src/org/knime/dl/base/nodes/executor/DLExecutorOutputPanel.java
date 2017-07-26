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
 *   Jul 10, 2017 (marcel): created
 */
package org.knime.dl.base.nodes.executor;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.dl.core.DLLayerDataSpec;
import org.knime.dl.core.backend.DLBackend;
import org.knime.dl.core.backend.DLBackendRegistry;
import org.knime.dl.core.data.DLReadableBuffer;
import org.knime.dl.core.data.convert.output.DLLayerDataToDataCellConverterFactory;
import org.knime.dl.core.data.convert.output.DLLayerDataToDataCellConverterRegistry;

/**
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
@SuppressWarnings("serial")
final class DLExecutorOutputPanel extends JPanel {

	private static final NodeLogger LOGGER = NodeLogger.getLogger(DLExecutorNodeModel.class);

	private final DLLayerDataSpec m_outputDataSpec;

	private final DLExecutorOutputConfig m_cfg;

	private final GridBagConstraints m_constr;

	private final DialogComponentIdFromPrettyStringSelection m_dcConverter;

	private final CopyOnWriteArrayList<ChangeListener> m_removeListeners;

	DLExecutorOutputPanel(final DLLayerDataSpec outputDataSpec, final DLExecutorOutputConfig cfg)
			throws NotConfigurableException {
		super(new GridBagLayout());
		m_outputDataSpec = outputDataSpec;
		m_cfg = cfg;
		m_dcConverter = new DialogComponentIdFromPrettyStringSelection(m_cfg.getConverterModel(), "Conversion");
		try {
			refreshConverters();
		} catch (final Exception e) {
			throw new NotConfigurableException(e.getMessage(), e);
		}
		m_cfg.addBackendChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(final ChangeEvent e) {
				refreshConverters();
			}
		});
		m_constr = new GridBagConstraints();
		m_constr.gridx = 0;
		m_constr.gridy = 0;
		m_constr.weightx = 1;
		m_constr.anchor = GridBagConstraints.WEST;
		m_constr.fill = GridBagConstraints.VERTICAL;
		m_removeListeners = new CopyOnWriteArrayList<>();
		constructPanel();
	}

	DLExecutorOutputConfig getConfig() {
		return m_cfg;
	}

	void addRemoveListener(final ChangeListener l) {
		if (!m_removeListeners.contains(l)) {
			m_removeListeners.add(l);
		}
	}

	void removeRemoveListener(final ChangeListener l) {
		m_removeListeners.remove(l);
	}

	void loadFromSettings(final NodeSettingsRO settings, final PortObjectSpec[] specs) throws NotConfigurableException {
		try {
			m_cfg.loadFromSettings(settings);
			m_dcConverter.loadSettingsFrom(settings, specs);
		} catch (final InvalidSettingsException e) {
			// default settings
		}
	}

	void saveToSettings(final NodeSettingsWO settings) {
		m_cfg.saveToSettings(settings);
	}

	private void constructPanel() {
		setBorder(BorderFactory.createTitledBorder("Output: " + m_outputDataSpec.getName()));
		// meta information
		final JPanel shape = new JPanel();
		final GridBagConstraints shapeConstr = new GridBagConstraints();
		shapeConstr.insets = new Insets(5, 0, 5, 0);
		shape.add(new JLabel("Shape: " + m_outputDataSpec.getShape().toString()), shapeConstr);
		add(shape, m_constr);
		// 'remove' button, see bottom for click event handling
		final JButton outputRemoveBtn = new JButton("remove");
		final GridBagConstraints outputRemoveBtnConstr = (GridBagConstraints) m_constr.clone();
		outputRemoveBtnConstr.weightx = 1;
		outputRemoveBtnConstr.anchor = GridBagConstraints.EAST;
		outputRemoveBtnConstr.fill = GridBagConstraints.NONE;
		outputRemoveBtnConstr.insets = new Insets(0, 0, 0, 5);
		add(outputRemoveBtn, outputRemoveBtnConstr);
		m_constr.gridy++;
		// converter selection
		add(m_dcConverter.getComponentPanel(), m_constr);
		m_constr.gridy++;
		// prefix text input
		final DialogComponentString dcPrefix =
				new DialogComponentString(m_cfg.getPrefixModel(), "Output columns prefix");
		add(dcPrefix.getComponentPanel(), m_constr);
		m_constr.gridy++;
		// 'remove' button click event: remove output
		outputRemoveBtn.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {
				onRemove();
			}
		});
	}

	private void refreshConverters() {
		final String[][] newConverters = getAvailableConverters();
		if (newConverters.length == 0) {
			final String msg = "No converters available for output '" + m_outputDataSpec.getName() + "'.";
			LOGGER.error(msg);
			throw new IllegalStateException(msg);
		}
		m_dcConverter.replaceListItems(newConverters[0], newConverters[1], null);
	}

	private void onRemove() {
		for (final ChangeListener l : m_removeListeners) {
			l.stateChanged(new ChangeEvent(this));
		}
	}

	private String[][] getAvailableConverters() {
		DLBackendRegistry.getInstance();
		final Optional<DLBackend> backend = DLBackendRegistry.getBackend(m_cfg.getBackendModel().getStringValue());
		if (!backend.isPresent()) {
			throw new RuntimeException(
					"Backend '" + m_cfg.getBackendModel().getStringValue() + "' could not be found.");
		}
		final Class<? extends DLReadableBuffer> bufferType = backend.get().getReadableBufferType(m_outputDataSpec);
		final List<DLLayerDataToDataCellConverterFactory<?, ? extends DataCell>> converterFactories =
				DLLayerDataToDataCellConverterRegistry.getInstance().getPreferredFactoriesForSourceType(bufferType,
						m_outputDataSpec);
		converterFactories.sort(Comparator.comparing(DLLayerDataToDataCellConverterFactory::getName));
		final String[] names = new String[converterFactories.size()];
		final String[] ids = new String[converterFactories.size()];
		for (int i = 0; i < converterFactories.size(); i++) {
			final DLLayerDataToDataCellConverterFactory<?, ? extends DataCell> converter = converterFactories.get(i);
			names[i] = "To " + converter.getName();
			ids[i] = converter.getIdentifier();
		}
		return new String[][] { names, ids };
	}
}
