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
package org.knime.dl.keras.base.nodes.learner;

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
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterPanel;
import org.knime.dl.base.nodes.DialogComponentObjectSelection;
import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.core.data.convert.DLDataValueToTensorConverterFactory;
import org.knime.dl.core.data.convert.DLDataValueToTensorConverterRegistry;
import org.knime.dl.core.training.DLLossFunction;
import org.knime.dl.core.training.DLTrainingContext;
import org.knime.dl.keras.core.training.DLKerasLossFunction;
import org.knime.dl.keras.core.training.DLKerasTrainingContext;
import org.knime.dl.util.DLUtils;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
final class DLKerasLearnerTargetPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	private final DLKerasLearnerTargetConfig m_cfg;

	private final DLTensorSpec m_outputDataSpec;

	private final DialogComponentObjectSelection<DLDataValueToTensorConverterFactory<?, ?>> m_dcConverter;

	private final DataColumnSpecFilterPanel m_dcInputColumns;

	private final DialogComponentObjectSelection<DLKerasLossFunction> m_dcLossFunction;

	private DataTableSpec m_lastTableSpec;

	DLKerasLearnerTargetPanel(final DLKerasLearnerTargetConfig cfg, final DLTensorSpec outputDataSpec,
			final DataTableSpec tableSpec) throws NotConfigurableException {
		super(new GridBagLayout());
		m_cfg = cfg;
		m_outputDataSpec = outputDataSpec;
		m_lastTableSpec = tableSpec;

		// construct panel:

		setBorder(BorderFactory.createTitledBorder("Training target: " + m_outputDataSpec.getName()));
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
						+ DLUtils.Shapes.getSize(DLUtils.Shapes.getFixedShape(m_outputDataSpec.getShape()).get())),
				numNeuronsConstr);
		add(numNeurons, constr);
		constr.gridy++;
		final JPanel shape = new JPanel();
		final GridBagConstraints shapeConstr = new GridBagConstraints();
		shapeConstr.insets = new Insets(5, 0, 5, 0);
		shape.add(new JLabel("Shape: " + m_outputDataSpec.getShape().toString()), shapeConstr);
		add(shape, constr);
		constr.gridy++;
		// converter selection
		m_dcConverter = new DialogComponentObjectSelection<>(m_cfg.getConverterEntry(), c -> "From " + c.getName(),
				"Conversion");
		add(m_dcConverter.getComponentPanel(), constr);
		constr.gridy++;
		// column selection
		final JPanel inputColumnsLabel = new JPanel();
		inputColumnsLabel.add(new JLabel("Target columns:"));
		add(inputColumnsLabel, constr);
		constr.gridy++;
		final JPanel inputColumnsFilter = new JPanel();
		m_dcInputColumns = new DataColumnSpecFilterPanel();
		inputColumnsFilter.add(m_dcInputColumns);
		add(inputColumnsFilter, constr);
		constr.gridy++;
		// loss function selection
		m_dcLossFunction = new DialogComponentObjectSelection<>(m_cfg.getLossFunctionEntry(), DLLossFunction::getName,
				"Loss function");
		add(m_dcLossFunction.getComponentPanel(), constr);
		constr.gridy++;

		m_cfg.getGeneralConfig().getTrainingContextEntry().addValueChangeOrLoadListener((entry, oldValue) -> {
			try {
				refreshAvailableConverters();
				refreshAvailableLossFunctions();
			} catch (final NotConfigurableException ex) {
				throw new IllegalStateException(ex.getMessage(), ex);
			}
		});
		m_cfg.getConverterEntry().addValueChangeOrLoadListener((entry, oldValue) -> refreshAllowedInputColumns());
	}

	DLKerasLearnerTargetConfig getConfig() {
		return m_cfg;
	}

	DataColumnSpecFilterPanel getInputColumns() {
		return m_dcInputColumns;
	}

	void saveToSettings(final NodeSettingsWO settings) throws InvalidSettingsException {
		final long inputSize = DLUtils.Shapes.getSize(DLUtils.Shapes.getFixedShape(m_outputDataSpec.getShape())
				.orElseThrow(() -> new InvalidSettingsException("Target '" + m_outputDataSpec.getName()
						+ "' has an (at least partially) unknown shape. This is not supported.")));
		m_dcInputColumns.saveConfiguration(m_cfg.getInputColumnsEntry().getValue());
		m_cfg.saveToSettings(settings);
		final FilterResult filter = m_cfg.getInputColumnsEntry().getValue().applyTo(m_lastTableSpec);
		if (filter.getIncludes().length > inputSize) {
			throw new InvalidSettingsException("More target data columns selected (" + filter.getIncludes().length
					+ ") than output neurons available (" + inputSize + ") for target '" + m_outputDataSpec.getName()
					+ "'. Try removing some columns from the selection.");
		}
	}

	void loadFromSettings(final NodeSettingsRO settings, final PortObjectSpec[] specs) throws NotConfigurableException {
		m_lastTableSpec = (DataTableSpec) specs[DLKerasLearnerNodeModel.IN_DATA_PORT_IDX];
		refreshAvailableConverters();
		try {
			m_cfg.loadFromSettingsInDialog(settings, m_lastTableSpec);
		} catch (final InvalidSettingsException e) {
			// ignore
		}
		refreshAllowedInputColumns();
		refreshAvailableLossFunctions();
	}

	void refreshAvailableConverters() throws NotConfigurableException {
		final DLTrainingContext<?, ?> trainingContext = m_cfg.getGeneralConfig().getTrainingContextEntry().getValue();
		final HashSet<DataType> inputTypes = new HashSet<>();
		final HashSet<DLDataValueToTensorConverterFactory<?, ?>> converterFactories = new HashSet<>();
		final DLDataValueToTensorConverterRegistry converters = DLDataValueToTensorConverterRegistry.getInstance();
		// for each distinct column type in the input table, add the preferred converter to the list of selectable
		// converters
		for (final DataColumnSpec inputColSpec : m_lastTableSpec) {
			if (inputTypes.add(inputColSpec.getType())) {
				final Optional<DLDataValueToTensorConverterFactory<?, ?>> converter = converters
						.getPreferredConverterFactory(inputColSpec.getType(),
								trainingContext.getTensorFactory().getWritableBufferType(m_outputDataSpec));
				if (converter.isPresent()) {
					converterFactories.add(converter.get());
				}
			}
		}
		if (converterFactories.isEmpty()) {
			throw new NotConfigurableException(
					"No converters available for input '" + m_outputDataSpec.getName() + "'.");
		}
		final List<DLDataValueToTensorConverterFactory<?, ?>> converterFactoriesSorted = converterFactories.stream()
				.sorted(Comparator.comparing(DLDataValueToTensorConverterFactory::getName))
				.collect(Collectors.toList());
		m_dcConverter.replaceListItems(converterFactoriesSorted, null);
	}

	void refreshAllowedInputColumns() {
		m_dcInputColumns.loadConfiguration(m_cfg.getInputColumnsEntry().getValue(), m_lastTableSpec);
		final Class<? extends DataValue> allowedColType = m_cfg.getConverterEntry().getValue().getSourceType();

		m_cfg.getInputColumnsEntry().setValue(new DataColumnSpecFilterConfiguration(
				DLKerasLearnerInputConfig.CFG_KEY_INPUT_COL, new DLDataTypeColumnFilter(allowedColType)));
		m_dcInputColumns.updateWithNewConfiguration(m_cfg.getInputColumnsEntry().getValue());
		// FIXME (knime-core):
		// Strange behavior within DataColumnSpecFilterPanel (see #toFilteredStringArray where m_filter is always
		// null because it doesn't get set in #updateWithNewConfiguration (only in the super class).
		// Also see NameFilterPanel#loadConfiguration where #getRemovedFromIncludeList and #getRemovedFromExcludeList
		// get added to the panel, which makes sense in general but not really when updating the filter config).
	}

	void refreshAvailableLossFunctions() throws NotConfigurableException {
		final DLKerasTrainingContext<?> trainingContext = m_cfg.getGeneralConfig().getTrainingContextEntry().getValue();
		final List<DLKerasLossFunction> availableLossFunctions = trainingContext.createLossFunctions() //
				.stream() //
				.sorted(Comparator.comparing(DLKerasLossFunction::getName)) //
				.collect(Collectors.toList());
		if (availableLossFunctions.isEmpty()) {
			throw new NotConfigurableException("No loss functions available for output '" + m_outputDataSpec.getName()
					+ "' (with training context '" + trainingContext.getName() + "').");
		}
		final DLKerasLossFunction selectedLossFunction = m_cfg.getLossFunctionEntry().getValue() != null
				? m_cfg.getLossFunctionEntry().getValue()
				: availableLossFunctions.get(0);
		for (int i = availableLossFunctions.size() - 1; i >= 0; i--) {
			if (availableLossFunctions.get(i).getClass() == selectedLossFunction.getClass()) {
				availableLossFunctions.remove(i);
				availableLossFunctions.add(i, selectedLossFunction);
			}
		}
		m_dcLossFunction.replaceListItems(availableLossFunctions, selectedLossFunction);
	}
}
