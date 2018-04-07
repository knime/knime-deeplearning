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
 *   Jul 10, 2017 (marcel): created
 */
package org.knime.dl.keras.base.nodes.learner;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalLong;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterPanel;
import org.knime.dl.base.nodes.DLConverterRefresher;
import org.knime.dl.base.nodes.DLConverterRefresher.DLNoConverterAvailableException;
import org.knime.dl.base.nodes.DialogComponentObjectSelection;
import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.core.data.convert.DLDataValueToTensorConverterFactory;
import org.knime.dl.core.training.DLTrainingContext;
import org.knime.dl.util.DLUtils;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("serial")
final class DLKerasLearnerInputPanel extends JPanel {

	private static final NodeLogger LOGGER = NodeLogger.getLogger(DLKerasLearnerNodeDialog.class);

	private final DLKerasLearnerInputConfig m_cfg;

	private final DLTensorSpec m_inputTensorSpec;

	private final DialogComponentObjectSelection<DLDataValueToTensorConverterFactory<?, ?>> m_dcConverter;

	private final DataColumnSpecFilterPanel m_dcInputColumns;

	private DataTableSpec m_lastTableSpec;

	DLKerasLearnerInputPanel(final DLKerasLearnerInputConfig cfg, final DLTensorSpec inputDataSpec,
			final DataTableSpec tableSpec) {
		super(new GridBagLayout());
		m_cfg = cfg;
		m_inputTensorSpec = inputDataSpec;
		m_lastTableSpec = tableSpec;

		// construct panel:

		setBorder(BorderFactory.createTitledBorder("Training input: " + m_inputTensorSpec.getName()));
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
		numNeurons.add(new JLabel("Number of neurons: " + DLUtils.Shapes.getSizeAsString(m_inputTensorSpec.getShape())),
				numNeuronsConstr);
		add(numNeurons, constr);
		constr.gridy++;
		final JPanel shape = new JPanel();
		final GridBagConstraints shapeConstr = new GridBagConstraints();
		shapeConstr.insets = new Insets(5, 0, 5, 0);
		shape.add(new JLabel("Shape: " + m_inputTensorSpec.getShape().toString()), shapeConstr);
		add(shape, constr);
		constr.gridy++;
		// converter selection
		m_dcConverter = new DialogComponentObjectSelection<>(m_cfg.getConverterEntry(), c -> "From " + c.getName(),
				"Conversion");
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

		m_cfg.getGeneralConfig().getTrainingContextEntry().addValueChangeListener((entry, oldValue) -> {
			try {
				refreshAvailableConverters(m_lastTableSpec);
			} catch (final NotConfigurableException ex) {
				throw new IllegalStateException(ex.getMessage(), ex);
			}
		});

		m_cfg.getConverterEntry()
				.addValueChangeListener((entry, oldValue) -> refreshAllowedInputColumns(m_lastTableSpec));
	}

	void saveToSettings(final NodeSettingsWO settings) throws InvalidSettingsException {
		final OptionalLong inputSizeOpt = DLUtils.Shapes.getFixedSize(m_inputTensorSpec.getShape());
		if (inputSizeOpt.isPresent()) {
			final long inputSize = inputSizeOpt.getAsLong();
			// validate input: get user-selected columns and converter, ask
			// converter for its output size given the input
			// columns (if possible) and compare to number of available input
			// neurons
			final Set<DataColumnSpec> includedColSpecs = m_dcInputColumns.getIncludeList();
			final DLDataValueToTensorConverterFactory<? extends DataValue, ?> converter = m_cfg.getConverterEntry()
					.getValue();
			final OptionalLong converterOutputSizeOpt = converter.getDestCount(new ArrayList<>(includedColSpecs));
			if (converterOutputSizeOpt.isPresent()) {
				final long converterOutputSize = converterOutputSizeOpt.getAsLong();
				if (converterOutputSize > inputSize) {
					throw new InvalidSettingsException("Selected input columns provide more input elements ("
							+ converterOutputSize + ") than neurons available (" + inputSize + ") for network input '"
							+ m_inputTensorSpec.getName() + "'. Try removing some columns from the selection.");
				}
				if (converterOutputSize < inputSize) {
					throw new InvalidSettingsException("Selected input columns do not provide enough input elements ("
							+ converterOutputSize + ") to populate all neurons (" + inputSize + ") of network input '"
							+ m_inputTensorSpec.getName() + "'. Try adding some columns to the selection.");
				}
			} else {
				// we still can check if there are more input columns than input
				// neurons since every column provides at
				// least one element
				if (includedColSpecs.size() > inputSize) {
					throw new InvalidSettingsException("More input columns selected (" + includedColSpecs.size()
							+ ") than neurons available (" + inputSize + ") for network input '"
							+ m_inputTensorSpec.getName() + "'. Try removing some columns from the selection.");
				}
			}
		}

		m_dcInputColumns.saveConfiguration(m_cfg.getInputColumnsEntry().getValue());
		m_cfg.saveToSettings(settings);
	}

	void loadFromSettings(final NodeSettingsRO settings, final PortObjectSpec[] specs) throws NotConfigurableException {
		m_lastTableSpec = (DataTableSpec) specs[DLKerasLearnerNodeModel.IN_DATA_PORT_IDX];
		try {
			m_cfg.loadFromSettingsInDialog(settings, m_lastTableSpec);
		} catch (final InvalidSettingsException e) {
			// ignore
			LOGGER.debug(e.getMessage() != null ? e.getMessage() : "Trying to restore from invalid settings.", e);
		}
		m_dcInputColumns.loadConfiguration(m_cfg.getInputColumnsEntry().getValue(), m_lastTableSpec);
		refreshAvailableConverters(m_lastTableSpec);
		refreshAllowedInputColumns(m_lastTableSpec);
	}

	private void refreshAvailableConverters(final DataTableSpec dataTableSpec) throws NotConfigurableException {
		final DLTrainingContext<?, ?> trainingContext = m_cfg.getGeneralConfig().getTrainingContextEntry().getValue();
		DLConverterRefresher converterRefresher;
		try {
			final Comparator<DLDataValueToTensorConverterFactory<?, ?>> nameComparator = Comparator
					.comparing(DLDataValueToTensorConverterFactory::getName);
			converterRefresher = new DLConverterRefresher(dataTableSpec,
					trainingContext.getTensorFactory().getWritableBufferType(m_inputTensorSpec), m_inputTensorSpec,
					false, nameComparator);
		} catch (final DLNoConverterAvailableException e) {
			throw new NotConfigurableException(e.getLongMessage());
		}
		final List<DLDataValueToTensorConverterFactory<?, ?>> converterFactories = converterRefresher.getConverters();
		m_dcConverter.replaceListItems(converterFactories, null);
	}

	private void refreshAllowedInputColumns(final DataTableSpec dataTableSpec) {
		final Class<? extends DataValue> allowedColType = m_cfg.getConverterEntry().getValue().getSourceType();
		if (dataTableSpec.containsCompatibleType(allowedColType)) {
			// We need to save and reload the current configuration to take user actions into account that were taken
			// since the dialog was opened. Else those would be overridden by the initial configuration.
			m_dcInputColumns.saveConfiguration(m_cfg.getInputColumnsEntry().getValue());
			m_dcInputColumns.loadConfiguration(m_cfg.getInputColumnsEntry().getValue(), dataTableSpec);
			final DataColumnSpecFilterConfiguration filterConfig = new DataColumnSpecFilterConfiguration(
					DLKerasLearnerInputConfig.CFG_KEY_INPUT_COL, new DLDataTypeColumnFilter(allowedColType));
			m_cfg.getInputColumnsEntry().setValue(filterConfig, true);
			m_dcInputColumns.updateWithNewConfiguration(filterConfig);
		}
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
