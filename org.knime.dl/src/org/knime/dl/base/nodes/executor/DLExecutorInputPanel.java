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
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.filter.NameFilterConfiguration.FilterResult;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterPanel;
import org.knime.dl.base.nodes.executor.DLExecutorInputConfig.DLDataTypeColumnFilter;
import org.knime.dl.core.DLLayerDataSpec;
import org.knime.dl.core.data.convert.DLDataValueToLayerDataConverterFactory;
import org.knime.dl.core.data.convert.DLDataValueToLayerDataConverterRegistry;
import org.knime.dl.core.execution.DLExecutionContext;
import org.knime.dl.core.execution.DLExecutionContextRegistry;
import org.knime.dl.util.DLUtils;

/**
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
final class DLExecutorInputPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	private static final NodeLogger LOGGER = NodeLogger.getLogger(DLExecutorNodeModel.class);

	private final DLLayerDataSpec m_inputDataSpec;

	private final DLExecutorInputConfig m_cfg;

	private final DialogComponentIdFromPrettyStringSelection m_dcConverter;

	private final DataColumnSpecFilterPanel m_dcInputColumns;

	private final GridBagConstraints m_constr;

	private DataTableSpec m_lastTableSpec;

	DLExecutorInputPanel(final DLLayerDataSpec inputDataSpec, final DLExecutorInputConfig cfg,
			final DataTableSpec tableSpec) throws NotConfigurableException {
		super(new GridBagLayout());
		m_inputDataSpec = inputDataSpec;
		m_lastTableSpec = tableSpec;
		m_cfg = cfg;
		m_dcConverter = new DialogComponentIdFromPrettyStringSelection(m_cfg.getConverterModel(), "Conversion");
		m_dcInputColumns = new DataColumnSpecFilterPanel();
		try {
			initializeComponents();
		} catch (final Exception e) {
			throw new NotConfigurableException(e.getMessage(), e);
		}
		m_cfg.addBackendChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(final ChangeEvent e) {
				try {
					refreshConverters();
				} catch (final InvalidSettingsException ex) {
					throw new IllegalStateException(ex.getMessage(), ex);
				}
			}
		});
		m_cfg.addConverterChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(final ChangeEvent e) {
				refreshInputColumns();
			}
		});
		m_constr = new GridBagConstraints();
		m_constr.gridx = 0;
		m_constr.gridy = 0;
		m_constr.weightx = 1;
		m_constr.anchor = GridBagConstraints.WEST;
		m_constr.fill = GridBagConstraints.VERTICAL;
		constructPanel();
	}

	DLExecutorInputConfig getConfig() {
		return m_cfg;
	}

	DataColumnSpecFilterPanel getInputColumns() {
		return m_dcInputColumns;
	}

	void loadFromSettings(final NodeSettingsRO settings, final PortObjectSpec[] specs)
			throws InvalidSettingsException, NotConfigurableException {
		m_lastTableSpec = (DataTableSpec) specs[DLExecutorNodeModel.IN_DATA_PORT_IDX];
		m_cfg.loadFromSettingsInDialog(settings, m_lastTableSpec);
		refreshConverters();
		refreshInputColumns();
	}

	void saveToSettings(final NodeSettingsWO settings) throws InvalidSettingsException {
		final long inputSize = DLUtils.Shapes.getSize(DLUtils.Shapes.getFixedShape(m_inputDataSpec.getShape()).get());
		m_dcInputColumns.saveConfiguration(m_cfg.getInputColumnsModel());
		m_cfg.saveToSettings(settings);
		final FilterResult filter = m_cfg.getInputColumnsModel().applyTo(m_lastTableSpec);
		if (filter.getIncludes().length > inputSize) {
			throw new InvalidSettingsException(
					"More columns selected (" + filter.getIncludes().length + ") than input neurons available ("
							+ inputSize + ") for input '" + m_inputDataSpec.getName() + "'.");
		}
	}

	private void initializeComponents() throws InvalidSettingsException {
		refreshConverters();
		m_dcInputColumns.loadConfiguration(m_cfg.getInputColumnsModel(), m_lastTableSpec);
		refreshInputColumns();
	}

	private void constructPanel() {
		setBorder(BorderFactory.createTitledBorder("Input: " + m_inputDataSpec.getName()));
		// meta information
		final JPanel numNeurons = new JPanel();
		final GridBagConstraints numNeuronsConstr = new GridBagConstraints();
		numNeuronsConstr.insets = new Insets(5, 0, 5, 0);
		numNeurons.add(
				new JLabel("Number of neurons: "
						+ DLUtils.Shapes.getSize(DLUtils.Shapes.getFixedShape(m_inputDataSpec.getShape()).get())),
				numNeuronsConstr);
		add(numNeurons, m_constr);
		m_constr.gridy++;
		final JPanel shape = new JPanel();
		final GridBagConstraints shapeConstr = new GridBagConstraints();
		shapeConstr.insets = new Insets(5, 0, 5, 0);
		shape.add(new JLabel("Shape: " + m_inputDataSpec.getShape().toString()), shapeConstr);
		add(shape, m_constr);
		m_constr.gridy++;
		// converter selection
		add(m_dcConverter.getComponentPanel(), m_constr);
		m_constr.gridy++;
		// column selection
		final JPanel inputColumnsLabel = new JPanel();
		inputColumnsLabel.add(new JLabel("Input columns:"));
		add(inputColumnsLabel, m_constr);
		m_constr.gridy++;
		final JPanel inputColumnsFilter = new JPanel();
		inputColumnsFilter.add(m_dcInputColumns);
		add(inputColumnsFilter, m_constr);
		m_constr.gridy++;
	}

	private void refreshConverters() throws InvalidSettingsException {
		final String[][] newConverters = getAvailableConverters();
		if (newConverters[0].length == 0) {
			final String msg = "No converters available for output '" + m_inputDataSpec.getName() + "'.";
			LOGGER.error(msg);
			throw new InvalidSettingsException(msg);
		}
		m_dcConverter.replaceListItems(newConverters[0], newConverters[1], null);
	}

	private void refreshInputColumns() {
		final Class<? extends DataValue> allowedColType = getAllowedInputColumnType();
		m_cfg.setInputColumnsModelFilter(new DLDataTypeColumnFilter(allowedColType));
		m_dcInputColumns.updateWithNewConfiguration(m_cfg.getInputColumnsModel());
		// FIXME (knime-core):
		// Strange behavior within DataColumnSpecFilterPanel (see #toFilteredStringArray where m_filter is always
		// null because it doesn't get set in #updateWithNewConfiguration (only in the super class).
		// Also see NameFilterPanel#loadConfiguration where #getRemovedFromIncludeList and #getRemovedFromExcludeList
		// get added to the panel, which makes sense in general but not really when updating the filter config)
	}

	private String[][] getAvailableConverters() throws InvalidSettingsException {
		final DLExecutionContext<?> executionContext = DLExecutionContextRegistry.getInstance()
				.getExecutionContext(m_cfg.getExecutionContextModel().getStringValue()).orElseThrow(() -> {
					final String msg = "Execution back end '" + m_cfg.getExecutionContextModel().getStringValue()
							+ "' could not be found.";
					LOGGER.error(msg);
					return new InvalidSettingsException(msg);
				});
		final HashSet<DataType> inputTypes = new HashSet<>();
		final HashSet<DLDataValueToLayerDataConverterFactory<?, ?>> converterFactories = new HashSet<>();
		final DLDataValueToLayerDataConverterRegistry converters =
				DLDataValueToLayerDataConverterRegistry.getInstance();
		// for each distinct column type in the input table, add the preferred converter to the list of selectable
		// converters
		for (final DataColumnSpec inputColSpec : m_lastTableSpec) {
			if (inputTypes.add(inputColSpec.getType())) {
				final Optional<DLDataValueToLayerDataConverterFactory<?, ?>> converter =
						converters.getPreferredConverterFactory(inputColSpec.getType(),
								executionContext.getLayerDataFactory().getWritableBufferType(m_inputDataSpec));
				if (converter.isPresent()) {
					converterFactories.add(converter.get());
				}
			}
		}
		final List<DLDataValueToLayerDataConverterFactory<?, ?>> converterFactoriesSorted = converterFactories.stream()
				.sorted(Comparator.comparing(DLDataValueToLayerDataConverterFactory::getName))
				.collect(Collectors.toList());
		final String[] names = new String[converterFactoriesSorted.size()];
		final String[] ids = new String[converterFactoriesSorted.size()];
		final int i = 0;
		for (final DLDataValueToLayerDataConverterFactory<?, ?> converter : converterFactoriesSorted) {
			names[i] = "From " + converter.getName();
			ids[i] = converter.getIdentifier();
		}
		return new String[][] { names, ids };
	}

	// if changing code here, also update DLExecutorNodeModel#getAllowedInputColumnType
	private Class<? extends DataValue> getAllowedInputColumnType() {
		final Optional<DLDataValueToLayerDataConverterFactory<? extends DataValue, ?>> conv =
				DLDataValueToLayerDataConverterRegistry.getInstance()
						.getConverterFactory(m_cfg.getConverterModel().getStringValue());
		if (!conv.isPresent()) {
			final String msg = "Converter '" + m_cfg.getConverterModel().getStringValue() + "' could not be found.";
			LOGGER.error(msg);
			throw new IllegalStateException(msg);
		}
		return conv.get().getSourceType();
	}
}
