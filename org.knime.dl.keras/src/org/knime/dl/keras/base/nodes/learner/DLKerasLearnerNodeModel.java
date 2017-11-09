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
 *   May 2, 2017 (dietzc): created
 */
package org.knime.dl.keras.base.nodes.learner;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.streamable.DataTableRowInput;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.OutputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.PortInput;
import org.knime.core.node.streamable.PortObjectInput;
import org.knime.core.node.streamable.PortObjectOutput;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.RowInput;
import org.knime.core.node.streamable.StreamableOperator;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;
import org.knime.core.util.Pair;
import org.knime.dl.base.portobjects.DLNetworkPortObject;
import org.knime.dl.base.portobjects.DLNetworkPortObjectSpec;
import org.knime.dl.core.DLException;
import org.knime.dl.core.DLMissingDependencyException;
import org.knime.dl.core.DLNetwork;
import org.knime.dl.core.DLNetworkSpec;
import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.core.data.convert.DLDataValueToTensorConverterFactory;
import org.knime.dl.core.execution.DLInvalidNetworkInputException;
import org.knime.dl.core.training.DLKnimeNetworkLearner;
import org.knime.dl.core.training.DLLossFunction;
import org.knime.dl.core.training.DLTrainingContextRegistry;
import org.knime.dl.keras.base.portobjects.DLKerasNetworkPortObject;
import org.knime.dl.keras.base.portobjects.DLKerasNetworkPortObjectSpec;
import org.knime.dl.keras.core.DLKerasNetwork;
import org.knime.dl.keras.core.DLKerasNetworkSpec;
import org.knime.dl.keras.core.training.DLKerasCallback;
import org.knime.dl.keras.core.training.DLKerasDefaultTrainingConfig;
import org.knime.dl.keras.core.training.DLKerasLossFunction;
import org.knime.dl.keras.core.training.DLKerasOptimizer;
import org.knime.dl.keras.core.training.DLKerasTrainableNetworkAdapter;
import org.knime.dl.keras.core.training.DLKerasTrainingConfig;
import org.knime.dl.keras.core.training.DLKerasTrainingContext;
import org.knime.dl.util.DLUtils;

import com.google.common.base.Strings;

/**
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
final class DLKerasLearnerNodeModel extends NodeModel {

	static final int IN_NETWORK_PORT_IDX = 0;

	static final int IN_DATA_PORT_IDX = 1;

	static final int OUT_NETWORK_PORT_IDX = 0;

	static final String CFG_KEY_TRAINING = "training";

	static final String CFG_KEY_TARGET = "target";

	private static final NodeLogger LOGGER = NodeLogger.getLogger(DLKerasLearnerNodeModel.class);

	static DLKerasLearnerGeneralConfig createGeneralModelConfig() {
		return new DLKerasLearnerGeneralConfig();
	}

	static DLKerasLearnerInputConfig createInputTensorModelConfig(final String inputTensorName,
			final DLKerasLearnerGeneralConfig generalCfg) {
		return new DLKerasLearnerInputConfig(inputTensorName, generalCfg);
	}

	static DLKerasLearnerTargetConfig createOutputTensorModelConfig(final String outputTensorName,
			final DLKerasLearnerGeneralConfig generalCfg) {
		return new DLKerasLearnerTargetConfig(outputTensorName, generalCfg);
	}

	private final DLKerasLearnerGeneralConfig m_generalCfg;

	private final HashMap<String, DLKerasLearnerInputConfig> m_inputCfgs;

	private final HashMap<String, DLKerasLearnerTargetConfig> m_outputCfgs;

	private LinkedHashMap<DLTensorSpec, DLDataValueToTensorConverterFactory<?, ?>> m_inputConverters;

	private LinkedHashMap<DLTensorSpec, DLDataValueToTensorConverterFactory<?, ?>> m_outputConverters;

	private DLNetworkSpec m_lastIncomingNetworkSpec;

	private DLNetworkSpec m_lastConfiguredNetworkSpec;

	private DataTableSpec m_lastIncomingTableSpec;

	private DataTableSpec m_lastConfiguredTableSpec;

	private boolean m_initialLoaded;

	DLKerasLearnerNodeModel() {
		super(new PortType[] { DLKerasNetworkPortObject.TYPE, BufferedDataTable.TYPE },
				new PortType[] { DLKerasNetworkPortObject.TYPE });
		m_generalCfg = createGeneralModelConfig();
		m_inputCfgs = new HashMap<>();
		m_outputCfgs = new HashMap<>();
	}

	@Override
	public InputPortRole[] getInputPortRoles() {
		return new InputPortRole[] { InputPortRole.NONDISTRIBUTED_NONSTREAMABLE,
				InputPortRole.NONDISTRIBUTED_STREAMABLE };
	}

	@Override
	public OutputPortRole[] getOutputPortRoles() {
		return new OutputPortRole[] { OutputPortRole.NONDISTRIBUTED };
	}

	@Override
	public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo,
			final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
		return new StreamableOperator() {

			@Override
			public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext exec)
					throws Exception {
				final PortObject inPortObject = ((PortObjectInput) inputs[IN_NETWORK_PORT_IDX]).getPortObject();
				final RowInput rowInput = (RowInput) inputs[IN_DATA_PORT_IDX];

				final PortObject outPortObject = executeInternal(inPortObject, rowInput, exec);

				((PortObjectOutput) outputs[OUT_NETWORK_PORT_IDX]).setPortObject(outPortObject);
			}
		};
	}

	@Override
	protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
		if (inSpecs[DLKerasLearnerNodeModel.IN_NETWORK_PORT_IDX] == null) {
			throw new InvalidSettingsException("Input deep learning network is missing.");
		}
		if (inSpecs[DLKerasLearnerNodeModel.IN_DATA_PORT_IDX] == null) {
			throw new InvalidSettingsException("Input data table is missing.");
		}
		if (!DLKerasNetworkPortObject.TYPE
				.acceptsPortObjectSpec(inSpecs[DLKerasLearnerNodeModel.IN_NETWORK_PORT_IDX])) {
			throw new InvalidSettingsException(
					"Input port object is not a valid Keras deep learning network port object.");
		}

		final DLKerasNetworkPortObjectSpec inPortObjectSpec = ((DLKerasNetworkPortObjectSpec) inSpecs[IN_NETWORK_PORT_IDX]);
		final DLKerasNetworkSpec inNetworkSpec = inPortObjectSpec.getNetworkSpec();
		final Class<? extends DLNetwork> inNetworkType = inPortObjectSpec.getNetworkType();
		final DataTableSpec inDataSpec = ((DataTableSpec) inSpecs[IN_DATA_PORT_IDX]);

		if (inNetworkSpec == null) {
			throw new InvalidSettingsException("Input port object's deep learning network specs are missing.");
		}

		if (inNetworkSpec.getInputSpecs().length == 0) {
			LOGGER.warn("Input deep learning network has no input specs.");
		}
		if (inNetworkSpec.getOutputSpecs().length == 0) {
			LOGGER.warn("Input deep learning network has no output specs.");
		}

		m_lastIncomingNetworkSpec = inNetworkSpec;
		m_lastIncomingTableSpec = inDataSpec;

		if (m_lastConfiguredNetworkSpec != null && m_lastConfiguredTableSpec != null) {
			if (!m_lastConfiguredNetworkSpec.equals(m_lastIncomingNetworkSpec)) {
				throw new InvalidSettingsException("Input deep learning network changed. Please reconfigure the node.");
			}
		} else if (m_initialLoaded) {
			// loaded from saved workflow
			m_lastConfiguredNetworkSpec = m_lastIncomingNetworkSpec;
			m_lastConfiguredTableSpec = m_lastIncomingTableSpec;
		}

		try {
			configureGeneral(inNetworkType);
			configureInputs(inNetworkSpec, inDataSpec);
			configureOutputs(inNetworkSpec, inDataSpec);
		} catch (final Exception e) {
			throw new InvalidSettingsException(e.getMessage(), e);
		}

		final DLNetworkPortObjectSpec outDataSpec = createOutputSpec(inPortObjectSpec);
		return new PortObjectSpec[] { outDataSpec };
	}

	@Override
	protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
		final PortObject inPortObject = inObjects[IN_NETWORK_PORT_IDX];
		final RowInput rowInput = new DataTableRowInput((BufferedDataTable) inObjects[IN_DATA_PORT_IDX]);

		final DLKerasNetworkPortObject outPortObject = executeInternal(inPortObject, rowInput, exec);

		return new PortObject[] { outPortObject };
	}

	@Override
	protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		// no op
	}

	@Override
	protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		// no op
	}

	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
		try {
			m_generalCfg.saveToSettings(settings);

			final NodeSettingsWO inputSettings = settings.addNodeSettings(CFG_KEY_TRAINING);
			for (final DLKerasLearnerInputConfig inputCfg : m_inputCfgs.values()) {
				inputCfg.saveToSettings(inputSettings);
			}

			final NodeSettingsWO outputSettings = settings.addNodeSettings(CFG_KEY_TARGET);
			for (final DLKerasLearnerTargetConfig outputCfg : m_outputCfgs.values()) {
				outputCfg.saveToSettings(outputSettings);
			}
		} catch (final InvalidSettingsException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	@Override
	protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
		m_inputCfgs.clear();
		final NodeSettingsRO inputSettings = settings.getNodeSettings(CFG_KEY_TRAINING);
		for (final String layerName : inputSettings) {
			final DLKerasLearnerInputConfig inputCfg = createInputTensorModelConfig(layerName, m_generalCfg);
			m_inputCfgs.put(layerName, inputCfg);
		}

		m_outputCfgs.clear();
		final NodeSettingsRO outputSettings = settings.getNodeSettings(CFG_KEY_TARGET);
		for (final String layerName : outputSettings) {
			final DLKerasLearnerTargetConfig outputCfg = createOutputTensorModelConfig(layerName, m_generalCfg);
			m_outputCfgs.put(layerName, outputCfg);
		}
	}

	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
		m_generalCfg.loadFromSettings(settings);

		final NodeSettingsRO inputSettings = settings.getNodeSettings(CFG_KEY_TRAINING);
		for (final DLKerasLearnerInputConfig inputCfg : m_inputCfgs.values()) {
			inputCfg.loadFromSettingsInModel(inputSettings);
		}

		final NodeSettingsRO outputSettings = settings.getNodeSettings(CFG_KEY_TARGET);
		for (final DLKerasLearnerTargetConfig outputCfg : m_outputCfgs.values()) {
			outputCfg.loadFromSettingsInModel(outputSettings);
		}

		m_lastConfiguredNetworkSpec = m_lastIncomingNetworkSpec;
		m_lastConfiguredTableSpec = m_lastIncomingTableSpec;
		m_initialLoaded = true;
	}

	@Override
	protected void reset() {
		// no op
	}

	private void configureGeneral(final Class<? extends DLNetwork> networkType) throws Exception {
		final DLKerasTrainingContext<?> backend = m_generalCfg.getTrainingContextEntry().getValue();
		if (backend == null) {
			if (DLTrainingContextRegistry.getInstance().getTrainingContextsForNetworkType(networkType).isEmpty()) {
				throw new DLMissingDependencyException("No compatible training back end available. "
						+ "Are you missing a KNIME Deep Learning extension?");
			}
			throw new InvalidSettingsException("No training back end selected. Please configure the node.");
		}
		if (!backend.getNetworkType().isAssignableFrom(networkType)) {
			throw new InvalidSettingsException(
					"Selected training back end is not compatible to the input deep learning network. "
							+ "Please reconfigure the node.");
		}
		final DLKerasOptimizer optimizer = m_generalCfg.getOptimizerEntry().getValue();
		if (optimizer == null) {
			throw new InvalidSettingsException("No optimizer selected. Please configure the node.");
		}
	}

	private void configureInputs(final DLNetworkSpec networkSpec, final DataTableSpec inDataSpec)
			throws InvalidSettingsException {
		m_inputConverters = new LinkedHashMap<>(m_inputCfgs.size());
		for (final DLTensorSpec layerDataSpec : networkSpec.getInputSpecs()) {
			// validate layer spec
			if (!DLUtils.Shapes.isFixed(layerDataSpec.getShape())) {
				throw new InvalidSettingsException("Input '" + layerDataSpec.getName()
						+ "' has an (at least partially) unknown shape. This is not supported.");
			}
			final DLKerasLearnerInputConfig inputCfg = m_inputCfgs.get(layerDataSpec.getName());
			if (inputCfg == null) {
				throw new InvalidSettingsException(
						"Network input '" + layerDataSpec.getName() + "' is not yet configured.");
			}
			// get selected converter
			final DLDataValueToTensorConverterFactory<?, ?> converter = inputCfg.getConverterEntry().getValue();
			if (converter == null) {
				throw new InvalidSettingsException("No converter selected for input '" + layerDataSpec.getName()
						+ "'. Please configure the node.");
			}
			m_inputConverters.put(layerDataSpec, converter);
			if (m_lastConfiguredTableSpec != null) {
				// check if selected columns are still in input table:
				final DataColumnSpecFilterConfiguration filterConfig = inputCfg.getInputColumnsEntry().getValue();
				// TODO: workaround
				((org.knime.dl.keras.base.nodes.learner.DLKerasLearnerInputConfig.DLDataTypeColumnFilter) filterConfig
						.getFilter()).setFilterClasses(converter.getSourceType());
				final String[] missingColumns = filterConfig.applyTo(inDataSpec).getRemovedFromIncludes();
				if (missingColumns.length != 0) {
					throw new InvalidSettingsException("Selected column '" + missingColumns[0] + "' of input '"
							+ layerDataSpec.getName() + "' is missing. Please reconfigure the node.");
				}
			}
		}
	}

	private void configureOutputs(final DLNetworkSpec networkSpec, final DataTableSpec inDataSpec)
			throws InvalidSettingsException {
		m_outputConverters = new LinkedHashMap<>(m_outputCfgs.size());
		for (final DLTensorSpec layerDataSpec : networkSpec.getOutputSpecs()) {
			// validate layer spec
			if (!DLUtils.Shapes.isFixed(layerDataSpec.getShape())) {
				throw new InvalidSettingsException("Output '" + layerDataSpec.getName()
						+ "' has an (at least partially) unknown shape. This is not supported.");
			}
			final DLKerasLearnerTargetConfig outputCfg = m_outputCfgs.get(layerDataSpec.getName());
			if (outputCfg == null) {
				throw new InvalidSettingsException(
						"Network output '" + layerDataSpec.getName() + "' is not yet configured.");
			}
			// get selected converter
			final DLDataValueToTensorConverterFactory<?, ?> converter = outputCfg.getConverterEntry().getValue();
			if (converter == null) {
				throw new InvalidSettingsException("No converter selected for output '" + layerDataSpec.getName()
						+ "'. Please configure the node.");
			}
			m_outputConverters.put(layerDataSpec, converter);
			if (m_lastConfiguredTableSpec != null) {
				// check if selected columns are still in input table:
				final DataColumnSpecFilterConfiguration filterConfig = outputCfg.getInputColumnsEntry().getValue();
				// TODO: workaround
				((org.knime.dl.keras.base.nodes.learner.DLKerasLearnerTargetConfig.DLDataTypeColumnFilter) filterConfig
						.getFilter()).setFilterClasses(converter.getSourceType());
				final String[] missingColumns = filterConfig.applyTo(inDataSpec).getRemovedFromIncludes();
				if (missingColumns.length != 0) {
					throw new InvalidSettingsException("Selected column '" + missingColumns[0] + "' of output '"
							+ layerDataSpec.getName() + "' is missing. Please reconfigure the node.");
				}
			}
			final DLLossFunction lossFunction = outputCfg.getLossFunctionEntry().getValue();
			if (lossFunction == null) {
				throw new InvalidSettingsException("No loss function selected for output '" + layerDataSpec.getName()
						+ "'. Please configure the node.");
			}
		}
	}

	private DLNetworkPortObjectSpec createOutputSpec(final DLNetworkPortObjectSpec inPortObjectSpec)
			throws InvalidSettingsException {
		// TODO: create new network spec with updated training config
		return inPortObjectSpec;
	}

	@SuppressWarnings("unchecked")
	private <N extends DLKerasNetwork> DLKerasNetworkPortObject executeInternal(final PortObject portObject,
			final RowInput rowInput, final ExecutionContext exec) throws Exception {
		final N network = (N) ((DLNetworkPortObject) portObject).getNetwork();
		final DLKerasNetworkSpec networkSpec = network.getSpec();
		final DataTableSpec inDataSpec = rowInput.getDataTableSpec();

		final boolean inDataHasSize;
		final long inDataSize;
		if (rowInput instanceof DataTableRowInput) {
			inDataHasSize = true;
			inDataSize = ((DataTableRowInput) rowInput).getRowCount();
		} else {
			inDataHasSize = false;
			inDataSize = -1;
		}

		// final int batchSize;
		// if (inDataHasSize && inDataSize < m_generalCfg.getBatchSizeEntry().getValue()) {
		// batchSize = (int) inDataSize;
		// } else {
		// batchSize = m_generalCfg.getBatchSizeEntry().getValue();
		// }
		if (inDataSize == -1) {
			throw new RuntimeException("Streaming is not yet supported."); // TODO: NYI
		}

		if (inDataSpec.getNumColumns() == 0 || inDataSize == 0) {
			setWarningMessage("Input table is empty. Output network equals input network.");
			return (DLKerasNetworkPortObject) portObject;
		}

		final int batchSize = (int) inDataSize; // TODO: HACK

		final DLKerasTrainingContext<N> ctx = (DLKerasTrainingContext<N>) m_generalCfg.getTrainingContextEntry()
				.getValue();

		final Integer trainingBatchSize = m_generalCfg.getBatchSizeEntry().getValue();
		final Integer epochs = m_generalCfg.getEpochsEntry().getValue();
		final DLKerasOptimizer optimizer = m_generalCfg.getOptimizerEntry().getValue();
		final Map<DLTensorSpec, DLKerasLossFunction> lossFunctions = new HashMap<>();
		for (final DLTensorSpec outputSpec : networkSpec.getOutputSpecs()) {
			final DLKerasLossFunction lossFunction = m_outputCfgs.get(outputSpec.getName()).getLossFunctionEntry()
					.getValue();
			lossFunctions.put(outputSpec, lossFunction);
		}
		final ArrayList<DLKerasCallback> callbacks = new ArrayList<>(3);
		if (m_generalCfg.getTerminateOnNaNEntry().getEnabled()) {
			callbacks.add(m_generalCfg.getTerminateOnNaNEntry().getValue());
		}
		if (m_generalCfg.getEarlyStoppingEntry().getEnabled()) {
			callbacks.add(m_generalCfg.getEarlyStoppingEntry().getValue());
		}
		if (m_generalCfg.getReduceLROnPlateauEntry().getEnabled()) {
			callbacks.add(m_generalCfg.getReduceLROnPlateauEntry().getValue());
		}
		final DLKerasTrainingConfig trainingConfig = new DLKerasDefaultTrainingConfig(batchSize, epochs, optimizer,
				lossFunctions, callbacks);

		final DLKerasTrainableNetworkAdapter trainableNetwork = ctx.trainable(network, trainingConfig);

		// assign input column indices to network inputs
		final List<Pair<DLTensorSpec, int[]>> inputs = new ArrayList<>(networkSpec.getInputSpecs().length);
		for (final Entry<DLTensorSpec, DLDataValueToTensorConverterFactory<?, ?>> input : m_inputConverters
				.entrySet()) {
			final DLKerasLearnerInputConfig inputCfg = m_inputCfgs.get(input.getKey().getName());
			final DataColumnSpecFilterConfiguration filterConfig = inputCfg.getInputColumnsEntry().getValue();
			// TODO: workaround
			((org.knime.dl.keras.base.nodes.learner.DLKerasLearnerInputConfig.DLDataTypeColumnFilter) filterConfig
					.getFilter()).setFilterClasses(input.getValue().getSourceType());
			final int[] indices = Arrays.stream(filterConfig.applyTo(inDataSpec).getIncludes()).mapToInt(c -> {
				final int idx = inDataSpec.findColumnIndex(c);
				if (idx == -1) {
					throw new IllegalStateException(
							"Selected input column '" + c + "' could not be found in the input table.");
				}
				return idx;
			}).toArray();
			inputs.add(new Pair<>(input.getKey(), indices));
		}

		// assign input column indices to network targets
		final List<Pair<DLTensorSpec, int[]>> targets = new ArrayList<>(networkSpec.getOutputSpecs().length);
		for (final Entry<DLTensorSpec, DLDataValueToTensorConverterFactory<?, ?>> output : m_outputConverters
				.entrySet()) {
			final DLKerasLearnerTargetConfig outputCfg = m_outputCfgs.get(output.getKey().getName());
			final DataColumnSpecFilterConfiguration filterConfig = outputCfg.getInputColumnsEntry().getValue();
			// TODO: workaround
			((org.knime.dl.keras.base.nodes.learner.DLKerasLearnerTargetConfig.DLDataTypeColumnFilter) filterConfig
					.getFilter()).setFilterClasses(output.getValue().getSourceType());
			final int[] indices = Arrays.stream(filterConfig.applyTo(inDataSpec).getIncludes()).mapToInt(c -> {
				final int idx = inDataSpec.findColumnIndex(c);
				if (idx == -1) {
					throw new IllegalStateException(
							"Selected input column '" + c + "' could not be found in the input table.");
				}
				return idx;
			}).toArray();
			targets.add(new Pair<>(output.getKey(), indices));
		}

		try (final DLKnimeNetworkLearner learner = new DLKnimeNetworkLearner(trainableNetwork, m_inputConverters,
				m_outputConverters)) {

			boolean moreRows = true;
			long currRowIdx = 0;
			DataRow row;
			final List<DataRow> batch = new ArrayList<>(batchSize);

			try {
				while (moreRows) {
					while ((row = rowInput.poll()) != null) {
						// collect batch
						batch.add(row);
						currRowIdx++;
						if (currRowIdx % batchSize == 0) {
							break;
						}
					}
					if (row == null) {
						moreRows = false;
						if (batch.size() == 0) {
							break;
						}
						// process the last, incomplete batch
					}
					exec.checkCanceled();
					// gather input
					// TODO
					final Map<DLTensorSpec, List<DataValue>[]> temp1 = new HashMap<>();
					for (final Pair<DLTensorSpec, int[]> entry : inputs) {
						temp1.put(entry.getFirst(), new ArrayList[batch.size()]);
						for (int j = 0; j < batch.size(); j++) {
							final ArrayList<DataValue> cells = new ArrayList<>(entry.getSecond().length);
							for (final int c : entry.getSecond()) {
								final DataCell cell = batch.get(j).getCell(c);
								// TODO: we could also allow some missing value
								// handling settings in the dialog.
								if (cell.isMissing()) {
									throw new DLInvalidNetworkInputException(
											"Missing cell in input row '" + batch.get(j).getKey() + "', column '"
													+ inDataSpec.getColumnSpec(c).getName() + "'.");
								}
								cells.add(cell);
							}
							temp1.get(entry.getFirst())[j] = cells;
						}
					}
					// TODO: merge both batch traversals
					final Map<DLTensorSpec, List<DataValue>[]> temp2 = new HashMap<>();
					for (final Pair<DLTensorSpec, int[]> entry : targets) {
						temp2.put(entry.getFirst(), new ArrayList[batch.size()]);
						for (int j = 0; j < batch.size(); j++) {
							final ArrayList<DataValue> cells = new ArrayList<>(entry.getSecond().length);
							for (final int c : entry.getSecond()) {
								final DataCell cell = batch.get(j).getCell(c);
								// TODO: we could also allow some missing value
								// handling settings in the dialog.
								if (cell.isMissing()) {
									throw new DLInvalidNetworkInputException(
											"Missing cell in input row '" + batch.get(j).getKey() + "', column '"
													+ inDataSpec.getColumnSpec(c).getName() + "'.");
								}
								cells.add(cell);
							}
							temp2.get(entry.getFirst())[j] = cells;
						}
					}
					// execute
					learner.train(temp1, temp2, exec, batch.size());
					exec.checkCanceled();
					for (final List<DataValue>[] layer : temp1.values()) {
						for (final List<DataValue> list : layer) {
							list.clear();
						}
					}
					for (final List<DataValue>[] layer : temp2.values()) {
						for (final List<DataValue> list : layer) {
							list.clear();
						}
					}

					if (inDataHasSize && row != null) {
						exec.setProgress((double) currRowIdx / inDataSize,
								"Processing row " + row.getKey().toString() + "...");
					}
					batch.clear();
				}
			} catch (final Exception e) {
				String message;
				if (e instanceof DLException) {
					message = e.getMessage();
				} else {
					if (!Strings.isNullOrEmpty(e.getMessage())) {
						LOGGER.error(e.getMessage());
					}
					message = "Error occured during training of network model. See log for details.";
				}
				throw new RuntimeException(message, e);
			} finally {
				rowInput.close();
			}

			return trainableNetwork.getNetwork().getTrainedNetwork(exec);
		}
	}
}
