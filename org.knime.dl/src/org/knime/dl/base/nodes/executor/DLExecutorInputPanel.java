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
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.filter.NameFilterConfiguration.FilterResult;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterPanel;
import org.knime.dl.base.nodes.DialogComponentIdFromPrettyStringSelection;
import org.knime.dl.base.nodes.executor.DLExecutorInputConfig.DLDataTypeColumnFilter;
import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.core.data.convert.DLDataValueToTensorConverterFactory;
import org.knime.dl.core.data.convert.DLDataValueToTensorConverterRegistry;
import org.knime.dl.core.execution.DLExecutionContext;
import org.knime.dl.core.execution.DLExecutionContextRegistry;
import org.knime.dl.util.DLUtils;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
final class DLExecutorInputPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	private final DLExecutorInputConfig m_cfg;

	private final DLTensorSpec m_inputDataSpec;

	private final DialogComponentIdFromPrettyStringSelection m_dcConverter;

	private final DataColumnSpecFilterPanel m_dcInputColumns;

	private DataTableSpec m_lastTableSpec;

	DLExecutorInputPanel(final DLExecutorInputConfig cfg, final DLTensorSpec inputDataSpec,
			final DataTableSpec tableSpec) throws NotConfigurableException {
		super(new GridBagLayout());
		m_cfg = cfg;
		m_inputDataSpec = inputDataSpec;
		m_lastTableSpec = tableSpec;

		// construct panel:

		setBorder(BorderFactory.createTitledBorder("Input: " + m_inputDataSpec.getName()));
		final GridBagConstraints constr = new GridBagConstraints();
		constr.gridx = 0;
		constr.gridy = 0;
		constr.weightx = 1;
		constr.anchor = GridBagConstraints.WEST;
		constr.fill = GridBagConstraints.VERTICAL;
		// meta information
		final JPanel numNeurons = new JPanel();
		final GridBagConstraints numNeuronsConstr = new GridBagConstraints();
		numNeuronsConstr.insets = new Insets(5, 0, 5, 0);
		numNeurons.add(
				new JLabel("Number of neurons: "
						+ DLUtils.Shapes.getSize(DLUtils.Shapes.getFixedShape(m_inputDataSpec.getShape()).get())),
				numNeuronsConstr);
		add(numNeurons, constr);
		constr.gridy++;
		final JPanel shape = new JPanel();
		final GridBagConstraints shapeConstr = new GridBagConstraints();
		shapeConstr.insets = new Insets(5, 0, 5, 0);
		shape.add(new JLabel("Shape: " + m_inputDataSpec.getShape().toString()), shapeConstr);
		add(shape, constr);
		constr.gridy++;
		// converter selection
		m_dcConverter = new DialogComponentIdFromPrettyStringSelection(m_cfg.getConverterModel(), "Conversion", (e) -> {
			m_cfg.getConverterModel()
					.setStringArrayValue(((DialogComponentIdFromPrettyStringSelection) e.getSource()).getSelection());
		});
		add(m_dcConverter.getComponentPanel(), constr);
		constr.gridy++;
		// column selection
		final JPanel inputColumnsLabel = new JPanel();
		inputColumnsLabel.add(new JLabel("Input columns:"));
		add(inputColumnsLabel, constr);
		constr.gridy++;
		final JPanel inputColumnsFilter = new JPanel();
		m_dcInputColumns = new DataColumnSpecFilterPanel();
		inputColumnsFilter.add(m_dcInputColumns);
		add(inputColumnsFilter, constr);
		constr.gridy++;

		m_cfg.getGeneralConfig().addExecutionContextChangeListener(e -> {
			try {
				refreshAvailableConverters();
			} catch (final NotConfigurableException ex) {
				throw new IllegalStateException(ex.getMessage(), ex);
			}
		});
		m_cfg.getConverterModel().addChangeListener(e -> {
			try {
				refreshAllowedInputColumns();
			} catch (final NotConfigurableException ex) {
				throw new IllegalStateException(ex.getMessage(), ex);
			}
		});
	}

	DLExecutorInputConfig getConfig() {
		return m_cfg;
	}

	DataColumnSpecFilterPanel getInputColumns() {
		return m_dcInputColumns;
	}

	void loadFromSettings(final NodeSettingsRO settings, final PortObjectSpec[] specs) throws NotConfigurableException {
		m_lastTableSpec = (DataTableSpec) specs[DLExecutorNodeModel.IN_DATA_PORT_IDX];
		refreshAvailableConverters();
		try {
			m_cfg.loadFromSettingsInDialog(settings, m_lastTableSpec);
		} catch (final InvalidSettingsException e) {
			// ignore
		}
		refreshAllowedInputColumns();
	}

	void saveToSettings(final NodeSettingsWO settings) throws InvalidSettingsException {
		final long inputSize = DLUtils.Shapes.getSize(DLUtils.Shapes.getFixedShape(m_inputDataSpec.getShape()).get());
		m_dcInputColumns.saveConfiguration(m_cfg.getInputColumnsModel());
		m_cfg.saveToSettings(settings);
		// validate input: get user-selected columns and converter, ask converter for its output size given the input
		// columns (if possible) and compare to number of available input neurons
		final FilterResult filter = m_cfg.getInputColumnsModel().applyTo(m_lastTableSpec);
		final List<DataColumnSpec> includedColSpecs = Arrays.stream(filter.getIncludes())
				.collect(Collectors.mapping(col -> m_lastTableSpec.getColumnSpec(col), Collectors.toList()));
		final DLDataValueToTensorConverterFactory<? extends DataValue, ?> converter = DLDataValueToTensorConverterRegistry
				.getInstance().getConverterFactory(m_cfg.getConverterModel().getStringArrayValue()[1])
				.orElseThrow(() -> new InvalidSettingsException(
						"Converter '" + m_cfg.getConverterModel().getStringArrayValue()[0] + " ("
								+ m_cfg.getConverterModel().getStringArrayValue()[1]
								+ ")' could not be found. Are you missing a KNIME extension?"));
		final OptionalLong destSizeOpt = converter.getDestCount(includedColSpecs);
		if (destSizeOpt.isPresent()) {
			final long converterOutputSize = destSizeOpt.getAsLong();
			if (converterOutputSize > inputSize) {
				throw new InvalidSettingsException("Selected input columns provide more input elements ("
						+ converterOutputSize + ") than neurons available (" + inputSize + ") for network input '"
						+ m_inputDataSpec.getName() + "'. Try removing some columns from the selection.");
			}
			if (converterOutputSize < inputSize) {
				throw new InvalidSettingsException("Selected input columns do not provide enough input elements ("
						+ converterOutputSize + ") to populate all neurons (" + inputSize + ") of network input '"
						+ m_inputDataSpec.getName() + "'. Try adding some columns to the selection.");
			}
		} else {
			// we still can check if there are more input columns than input neurons since every column provides at
			// least one element
			if (includedColSpecs.size() > inputSize) {
				throw new InvalidSettingsException("More input columns selected (" + includedColSpecs.size()
						+ ") than neurons available (" + inputSize + ") for network input '" + m_inputDataSpec.getName()
						+ "'. Try removing some columns from the selection.");
			}
		}
	}

	void refreshAvailableConverters() throws NotConfigurableException {
		final DLExecutionContext<?> executionContext = DLExecutionContextRegistry.getInstance()
				.getExecutionContext(m_cfg.getGeneralConfig().getExecutionContext()[1])
				.orElseThrow(() -> new NotConfigurableException(
						"Execution back end '" + m_cfg.getGeneralConfig().getExecutionContext()[0] + " ("
								+ m_cfg.getGeneralConfig().getExecutionContext()[1] + ")' could not be found."));
		final HashSet<DataType> inputTypes = new HashSet<>();
		final HashSet<DLDataValueToTensorConverterFactory<?, ?>> converterFactories = new HashSet<>();
		final DLDataValueToTensorConverterRegistry converters = DLDataValueToTensorConverterRegistry.getInstance();
		// for each distinct column type in the input table, add the preferred
		// converter to the list of selectable
		// converters
		for (final DataColumnSpec inputColSpec : m_lastTableSpec) {
			if (inputTypes.add(inputColSpec.getType())) {
				final Optional<DLDataValueToTensorConverterFactory<?, ?>> converter = converters
						.getPreferredConverterFactory(inputColSpec.getType(),
								executionContext.getTensorFactory().getWritableBufferType(m_inputDataSpec));
				if (converter.isPresent()) {
					converterFactories.add(converter.get());
				}
			}
		}
		final List<DLDataValueToTensorConverterFactory<?, ?>> converterFactoriesSorted = converterFactories.stream()
				.sorted(Comparator.comparing(DLDataValueToTensorConverterFactory::getName))
				.collect(Collectors.toList());
		final String[] names = new String[converterFactoriesSorted.size()];
		final String[] ids = new String[converterFactoriesSorted.size()];
		for (int i = 0; i < converterFactoriesSorted.size(); i++) {
			final DLDataValueToTensorConverterFactory<?, ?> converter = converterFactoriesSorted.get(i);
			names[i] = "From " + converter.getName();
			ids[i] = converter.getIdentifier();
		}
		if (names.length == 0) {
			throw new NotConfigurableException(
					"No converters available for input '" + m_inputDataSpec.getName() + "'.");
		}
		m_dcConverter.replaceListItems(names, ids, null);
	}

	void refreshAllowedInputColumns() throws NotConfigurableException {
		m_dcInputColumns.loadConfiguration(m_cfg.getInputColumnsModel(), m_lastTableSpec);
		final Class<? extends DataValue> allowedColType = DLDataValueToTensorConverterRegistry.getInstance()
				.getConverterFactory(m_cfg.getConverterModel().getStringArrayValue()[1])
				.orElseThrow(() -> new NotConfigurableException(
						"Converter '" + m_cfg.getConverterModel().getStringArrayValue()[0] + " ("
								+ m_cfg.getConverterModel().getStringArrayValue()[1] + ")' could not be found."))
				.getSourceType();

		m_cfg.setInputColumnsModelFilter(new DLDataTypeColumnFilter(allowedColType));
		m_dcInputColumns.updateWithNewConfiguration(m_cfg.getInputColumnsModel());
		// FIXME (knime-core):
		// Strange behavior within DataColumnSpecFilterPanel (see
		// #toFilteredStringArray where m_filter is always
		// null because it doesn't get set in #updateWithNewConfiguration (only
		// in the super class).
		// Also see NameFilterPanel#loadConfiguration where
		// #getRemovedFromIncludeList and #getRemovedFromExcludeList
		// get added to the panel, which makes sense in general but not really
		// when updating the filter config).
	}
}
