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
 *   May 2, 2017 (dietzc): created
 */
package org.knime.dl.base.nodes.executor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.OptionalLong;
import java.util.stream.Collectors;

import org.knime.core.data.DataColumnSpec;
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
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.streamable.BufferedDataTableRowOutput;
import org.knime.core.node.streamable.DataTableRowInput;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.OutputPortRole;
import org.knime.core.node.streamable.PartitionInfo;
import org.knime.core.node.streamable.PortInput;
import org.knime.core.node.streamable.PortObjectInput;
import org.knime.core.node.streamable.PortOutput;
import org.knime.core.node.streamable.RowInput;
import org.knime.core.node.streamable.RowOutput;
import org.knime.core.node.streamable.StreamableOperator;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;
import org.knime.core.util.UniqueNameGenerator;
import org.knime.dl.base.nodes.executor.DLExecutorInputConfig.DLDataTypeColumnFilter;
import org.knime.dl.base.portobjects.DLNetworkPortObject;
import org.knime.dl.base.portobjects.DLNetworkPortObjectSpec;
import org.knime.dl.core.DLCanceledExecutionException;
import org.knime.dl.core.DLException;
import org.knime.dl.core.DLFixedTensorShape;
import org.knime.dl.core.DLMissingExtensionException;
import org.knime.dl.core.DLNetwork;
import org.knime.dl.core.DLNetworkSpec;
import org.knime.dl.core.DLRowInputRowIterator;
import org.knime.dl.core.DLTensorFactory;
import org.knime.dl.core.DLTensorId;
import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.core.ExecutionSpecCreator;
import org.knime.dl.core.data.convert.DLDataValueToTensorConverterFactory;
import org.knime.dl.core.data.convert.DLDataValueToTensorConverterRegistry;
import org.knime.dl.core.data.convert.DLTensorToDataCellConverterFactory;
import org.knime.dl.core.data.convert.DLTensorToDataCellConverterRegistry;
import org.knime.dl.core.execution.DLDefaultExecutionStatus;
import org.knime.dl.core.execution.DLExecutionContext;
import org.knime.dl.core.execution.DLExecutionContextRegistry;
import org.knime.dl.core.execution.DLExecutionStatus;
import org.knime.dl.core.execution.DLKnimeExecutionMonitor;
import org.knime.dl.core.execution.DLKnimeNetworkExecutionInputPreparer;
import org.knime.dl.core.execution.DLKnimeNetworkOutputConsumer;
import org.knime.dl.core.execution.DLNetworkExecutionSession;
import org.knime.dl.util.DLUtils;

import com.google.common.base.Strings;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
final class DLExecutorNodeModel extends NodeModel {

	static final int IN_NETWORK_PORT_IDX = 0;

	static final int IN_DATA_PORT_IDX = 1;

	static final int OUT_DATA_PORT_IDX = 0;

	static final String CFG_KEY_INPUTS = "inputs";

	static final String CFG_KEY_OUTPUTS = "outputs";

	static final String CFG_KEY_OUTPUTS_ORDER = "outputs_ordered";

	private static final NodeLogger LOGGER = NodeLogger.getLogger(DLExecutorNodeModel.class);

	static DLExecutorGeneralConfig createGeneralModelConfig() {
		return new DLExecutorGeneralConfig("<none>", null, 100);
	}

	static DLExecutorInputConfig createInputTensorModelConfig(final String configKey,
			final DLExecutorGeneralConfig generalCfg) {
		return new DLExecutorInputConfig(configKey, generalCfg);
	}

	static DLExecutorOutputConfig createOutputTensorModelConfig(final String configKey,
			final DLExecutorGeneralConfig generalCfg) {
		return new DLExecutorOutputConfig(configKey, generalCfg);
	}

	static SettingsModelStringArray createOutputOrderSettingsModel(final int outputsCount) {
		return new SettingsModelStringArray(CFG_KEY_OUTPUTS_ORDER, new String[outputsCount]);
	}

	private final DLExecutorGeneralConfig m_generalCfg;

	private final HashMap<String, DLExecutorInputConfig> m_inputCfgs;

	private final HashMap<String, DLExecutorOutputConfig> m_outputCfgs;

	private SettingsModelStringArray m_smOutputOrder;

	private LinkedHashMap<DLTensorSpec, DLDataValueToTensorConverterFactory<?, ?>> m_inputConverters;

	private LinkedHashMap<DLTensorSpec, DLTensorToDataCellConverterFactory<?, ?>> m_outputConverters;

	private DLNetworkSpec m_lastIncomingNetworkSpec;

	private DLNetworkSpec m_lastConfiguredNetworkSpec;

	private DataTableSpec m_lastIncomingTableSpec;

	private DataTableSpec m_lastConfiguredTableSpec;

	private boolean m_initialLoaded;

	DLExecutorNodeModel() {
		super(new PortType[] { DLNetworkPortObject.TYPE, BufferedDataTable.TYPE },
				new PortType[] { BufferedDataTable.TYPE });
		m_generalCfg = createGeneralModelConfig();
		m_inputCfgs = new HashMap<>();
		m_outputCfgs = new HashMap<>();
		m_lastIncomingNetworkSpec = null;
		m_lastConfiguredNetworkSpec = null;
		m_lastIncomingTableSpec = null;
		m_lastConfiguredTableSpec = null;
		m_initialLoaded = false;
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
				final PortObject portObject = ((PortObjectInput) inputs[IN_NETWORK_PORT_IDX]).getPortObject();
				final RowInput rowInput = (RowInput) inputs[IN_DATA_PORT_IDX];
				final RowOutput rowOutput = (RowOutput) outputs[OUT_DATA_PORT_IDX];

				executeInternal(portObject, rowInput, rowOutput, exec);
			}
		};
	}

	@Override
	protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
		if (inSpecs[DLExecutorNodeModel.IN_NETWORK_PORT_IDX] == null) {
			throw new InvalidSettingsException("Input deep learning network is missing.");
		}
		if (inSpecs[DLExecutorNodeModel.IN_DATA_PORT_IDX] == null) {
			throw new InvalidSettingsException("Input data table is missing.");
		}
		if (!DLNetworkPortObject.TYPE.acceptsPortObjectSpec(inSpecs[DLExecutorNodeModel.IN_NETWORK_PORT_IDX])) {
			throw new InvalidSettingsException("Input port object is not a valid deep learning network port object.");
		}

		final DLNetworkPortObjectSpec portObjectSpec = ((DLNetworkPortObjectSpec) inSpecs[IN_NETWORK_PORT_IDX]);
		final DLNetworkSpec networkSpec = portObjectSpec.getNetworkSpec();
		final Class<? extends DLNetwork> networkType = portObjectSpec.getNetworkType();
		final DataTableSpec inDataSpec = ((DataTableSpec) inSpecs[IN_DATA_PORT_IDX]);

		if (networkSpec == null) {
			throw new InvalidSettingsException("Input port object's deep learning network specs are missing.");
		}

		if (networkSpec.getInputSpecs().length == 0) {
			LOGGER.warn("Input deep learning network has no input specs.");
		}
		if (networkSpec.getOutputSpecs().length == 0 && networkSpec.getHiddenOutputSpecs().length == 0) {
			LOGGER.warn("Input deep learning network has no output specs.");
		}

		m_lastIncomingNetworkSpec = networkSpec;
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
			configureGeneral(networkSpec, networkType);
			configureInputs(networkSpec, inDataSpec);
			configureOutputs(networkSpec);
		} catch (final Exception e) {
			throw new InvalidSettingsException(e.getMessage(), e);
		}

		final DataTableSpec outDataSpec = createOutputSpec(inDataSpec);
		return new PortObjectSpec[] { outDataSpec };
	}

	@Override
	protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
		final PortObject portObject = inObjects[IN_NETWORK_PORT_IDX];
		final BufferedDataTable inData = (BufferedDataTable) inObjects[IN_DATA_PORT_IDX];
		final DataTableSpec inDataSpec = inData.getDataTableSpec();

		final RowInput rowInput = new DataTableRowInput(inData);
		final BufferedDataTableRowOutput rowOutput = new BufferedDataTableRowOutput(
				exec.createDataContainer(createOutputSpec(inDataSpec)));

		executeInternal(portObject, rowInput, rowOutput, exec);

		return new PortObject[] { rowOutput.getDataTable() };
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
		m_generalCfg.saveToSettings(settings);

		final NodeSettingsWO inputSettings = settings.addNodeSettings(CFG_KEY_INPUTS);
		for (final DLExecutorInputConfig inputCfg : m_inputCfgs.values()) {
			inputCfg.saveToSettings(inputSettings);
		}

		final NodeSettingsWO outputSettings = settings.addNodeSettings(CFG_KEY_OUTPUTS);
		for (final DLExecutorOutputConfig outputCfg : m_outputCfgs.values()) {
			outputCfg.saveToSettings(outputSettings);
		}

		if (m_smOutputOrder != null) {
			m_smOutputOrder.saveSettingsTo(settings);
		}
	}

	@Override
	protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
		m_generalCfg.validateSettings(settings);

		m_inputCfgs.clear();
		final NodeSettingsRO inputSettings = settings.getNodeSettings(CFG_KEY_INPUTS);
		for (final String layerName : inputSettings) {
			final DLExecutorInputConfig inputCfg = createInputTensorModelConfig(layerName, m_generalCfg);
			m_inputCfgs.put(layerName, inputCfg);
			inputCfg.validateSettings(inputSettings);
		}

		m_outputCfgs.clear();
		final NodeSettingsRO outputSettings = settings.getNodeSettings(CFG_KEY_OUTPUTS);
		for (final String layerName : outputSettings) {
			final DLExecutorOutputConfig outputCfg = createOutputTensorModelConfig(layerName, m_generalCfg);
			m_outputCfgs.put(layerName, outputCfg);
			outputCfg.validateSettings(outputSettings);
		}

		m_smOutputOrder = createOutputOrderSettingsModel(m_outputCfgs.size());
		m_smOutputOrder.validateSettings(settings);
	}

	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
		m_generalCfg.loadFromSettings(settings);
		final NodeSettingsRO inputSettings = settings.getNodeSettings(CFG_KEY_INPUTS);
		for (final DLExecutorInputConfig inputCfg : m_inputCfgs.values()) {
			inputCfg.loadFromSettingsInModel(inputSettings);
		}

		final NodeSettingsRO outputSettings = settings.getNodeSettings(CFG_KEY_OUTPUTS);
		for (final DLExecutorOutputConfig outputCfg : m_outputCfgs.values()) {
			outputCfg.loadFromSettings(outputSettings);
		}

		m_smOutputOrder.loadSettingsFrom(settings);

		m_lastConfiguredNetworkSpec = m_lastIncomingNetworkSpec;
		m_lastConfiguredTableSpec = m_lastIncomingTableSpec;
		m_initialLoaded = true;
	}

	@Override
	protected void reset() {
		// no op
	}

	private void configureGeneral(final DLNetworkSpec networkSpec, final Class<? extends DLNetwork> networkType)
			throws Exception {
		final String[] selectedBackend = m_generalCfg.getExecutionContext();
		final DLExecutionContext<?> backend = DLExecutionContextRegistry.getInstance()
				.getExecutionContext(selectedBackend[1]).orElseThrow(() -> {
					if (DLExecutionContextRegistry.getInstance().getExecutionContextsForNetworkType(networkType)
							.isEmpty()) {
						return new DLMissingExtensionException(
								"No compatible execution back end available. Are you missing a KNIME Deep Learning extension?");
					} else {
						return new InvalidSettingsException(
								"No execution back end selected. Please configure the node.");
					}
				});
		if (!backend.getNetworkType().isAssignableFrom(networkType)) {
			throw new InvalidSettingsException(
					"Selected back end is not compatible to the input deep learning network. Please reconfigure the node.");
		}
	}

	private void configureInputs(final DLNetworkSpec networkSpec, final DataTableSpec inDataSpec)
			throws DLMissingExtensionException, InvalidSettingsException {
		m_inputConverters = new LinkedHashMap<>(m_inputCfgs.size());
		for (final DLTensorSpec tensorSpec : networkSpec.getInputSpecs()) {
			// validate layer spec
			if (!DLUtils.Shapes.isKnown(tensorSpec.getShape())) {
				throw new InvalidSettingsException(
						"Input '" + tensorSpec.getName() + "' has an unknown shape. This is not supported.");
			}
			final DLExecutorInputConfig inputCfg = m_inputCfgs.get(tensorSpec.getName());
			if (inputCfg == null) {
				throw new InvalidSettingsException(
						"Network input '" + tensorSpec.getName() + "' is not yet configured.");
			}
			// get selected converter
			final DLDataValueToTensorConverterFactory<?, ?> converter = DLDataValueToTensorConverterRegistry
					.getInstance().getConverterFactory(inputCfg.getConverterModel().getStringArrayValue()[1])
					.orElseThrow(() -> new DLMissingExtensionException(
							"Converter '" + inputCfg.getConverterModel().getStringArrayValue()[0] + " ("
									+ inputCfg.getConverterModel().getStringArrayValue()[1] + ")' of input '"
									+ inputCfg.getInputTensorName()
									+ "' could not be found. Are you missing a KNIME extension?"));
			m_inputConverters.put(tensorSpec, converter);

			if (m_lastConfiguredTableSpec != null) {
				// check if selected columns are still in input table:
				final DataColumnSpecFilterConfiguration filterConfig = inputCfg.getInputColumnsModel();
				// workaround
				((DLDataTypeColumnFilter) filterConfig.getFilter())
						.setFilterClasses(getAllowedInputColumnType(inputCfg));
				final String[] missingColumns = filterConfig.applyTo(inDataSpec).getRemovedFromIncludes();
				if (missingColumns.length != 0) {
					throw new InvalidSettingsException("Selected column '" + missingColumns[0] + "' of input '"
							+ tensorSpec.getName() + "' is missing. Please reconfigure the node.");
				}
			}
		}
	}

	private void configureOutputs(final DLNetworkSpec networkSpec)
			throws DLMissingExtensionException, InvalidSettingsException {
		if (m_outputCfgs.size() == 0) {
			throw new InvalidSettingsException("No network output was selected.");
		}
		m_outputConverters = new LinkedHashMap<>(m_outputCfgs.size());
		for (final String layerDataName : m_smOutputOrder.getStringArrayValue()) {
			// validate layer spec
			final DLTensorSpec tensorSpec = DLUtils.Networks.findSpec(layerDataName, networkSpec)
					.orElseThrow(() -> new InvalidSettingsException("Selected output '" + layerDataName
							+ "' could not be found in the input deep learning network."));
			if (!DLUtils.Shapes.isKnown(tensorSpec.getShape())) {
				throw new InvalidSettingsException(
						"Selected output '" + layerDataName + "' has an unknown shape. This is not supported.");
			}
			final DLExecutorOutputConfig cfg = m_outputCfgs.get(layerDataName);
			// get selected converter
			final DLTensorToDataCellConverterFactory<?, ?> converter = DLTensorToDataCellConverterRegistry.getInstance()
					.getConverterFactory(cfg.getConverterModel().getStringArrayValue()[1])
					.orElseThrow(() -> new DLMissingExtensionException(
							"Converter '" + cfg.getConverterModel().getStringArrayValue()[0] + " ("
									+ cfg.getConverterModel().getStringArrayValue()[1] + ")' for output '"
									+ layerDataName + "' could not be found. Are you missing a KNIME extension?"));
			m_outputConverters.put(tensorSpec, converter);
		}
	}

	private DataTableSpec createOutputSpec(final DataTableSpec inDataSpec) {
		final boolean keepInputColumns = m_generalCfg.getKeepInputColumnsModel().getBooleanValue();
		final ArrayList<DataColumnSpec> outputSpecs = new ArrayList<>();
		final UniqueNameGenerator nameGenerator = new UniqueNameGenerator(keepInputColumns ? inDataSpec : null);
		for (final Entry<DLTensorSpec, DLTensorToDataCellConverterFactory<?, ?>> output : m_outputConverters
				.entrySet()) {
			final DLTensorSpec layerDataSpec = output.getKey();
			final DLTensorToDataCellConverterFactory<?, ?> converter = output.getValue();
			final OptionalLong count = converter.getDestCount(layerDataSpec);
			final String prefix = m_outputCfgs.get(layerDataSpec.getName()).getPrefixModel().getStringValue();
			if (!count.isPresent()) {
				// We can't output a tableSpec if we don't know the number of produced columns for any of the output
				// converters.
				return null;
			}
			for (int i = 0; i < count.getAsLong(); i++) {
				outputSpecs.add(nameGenerator.newColumn(prefix + Integer.toString(i), converter.getDestType()));
			}
		}
		final DataTableSpec outDataSpec = new DataTableSpec(outputSpecs.toArray(new DataColumnSpec[0]));
		return keepInputColumns ? new DataTableSpec(inDataSpec, outDataSpec) : outDataSpec;
	}

	@SuppressWarnings("unchecked")
	private <N extends DLNetwork> void executeInternal(final PortObject portObject, final RowInput rowInput,
			final RowOutput rowOutput, final ExecutionContext exec) throws Exception {
		final N network = (N) ((DLNetworkPortObject) portObject).getNetwork();
		final DLNetworkSpec networkSpec = network.getSpec();
		final DataTableSpec inDataSpec = rowInput.getDataTableSpec();
		if ((rowInput instanceof DataTableRowInput && ((DataTableRowInput) rowInput).getRowCount() == 0)
				|| inDataSpec.getNumColumns() == 0) {
			setWarningMessage("Input table is empty. Node created an empty output table.");
			rowInput.close();
			rowOutput.close();
			return;
		}

		final String[] selectedCtx = m_generalCfg.getExecutionContext();
		final DLExecutionContext<N> ctx = (DLExecutionContext<N>) DLExecutionContextRegistry.getInstance()
				.getExecutionContextsForNetworkType(network.getClass()).stream()
				.filter(inner -> inner.getIdentifier().equals(selectedCtx[1])).findFirst()
				.orElseThrow(() -> new DLMissingExtensionException(
						"There is no available execution back end of name '" + selectedCtx[0] + " (" + selectedCtx[1]
								+ ")' that supports the input network of type '" + network.getClass().getCanonicalName()
								+ "'. Are you missing a KNIME Deep Learning extension?"));

		final int batchSize = m_generalCfg.getBatchSizeModel().getIntValue();
		final boolean isPredefinedBatchSize = Arrays.stream(networkSpec.getInputSpecs())
				.anyMatch(s -> s.getBatchSize().isPresent());

		final boolean keepInputColumns = m_generalCfg.getKeepInputColumnsModel().getBooleanValue();

		// assign input column indices to network inputs
		final LinkedHashMap<DLTensorId, int[]> columnsForTensorId = new LinkedHashMap<>(m_inputConverters.size());
		final LinkedHashMap<DLTensorId, DLDataValueToTensorConverterFactory<?, ?>> inputConverterForTensorId = new LinkedHashMap<>(
				m_inputConverters.size());

		for (final Entry<DLTensorSpec, DLDataValueToTensorConverterFactory<?, ?>> entry : m_inputConverters
				.entrySet()) {
			final DLTensorSpec spec = entry.getKey();
			final DLExecutorInputConfig inputCfg = m_inputCfgs.get(spec.getName());
			final DataColumnSpecFilterConfiguration filterConfig = inputCfg.getInputColumnsModel();
			((DLDataTypeColumnFilter) filterConfig.getFilter()).setFilterClasses(getAllowedInputColumnType(inputCfg));
			final int[] indices = Arrays.stream(filterConfig.applyTo(inDataSpec).getIncludes()).mapToInt(c -> {
				final int idx = inDataSpec.findColumnIndex(c);
				if (idx == -1) {
					throw new IllegalStateException(
							"Selected input column '" + c + "' could not be found in the input table.");
				}
				return idx;
			}).toArray();
			columnsForTensorId.put(spec.getIdentifier(), indices);
			inputConverterForTensorId.put(spec.getIdentifier(), entry.getValue());
		}

		final LinkedHashMap<DLTensorId, DLTensorToDataCellConverterFactory<?, ?>> outputConverterForTensorId = new LinkedHashMap<>(
				m_outputConverters.size());
		for (final Entry<DLTensorSpec, DLTensorToDataCellConverterFactory<?, ?>> entry : m_outputConverters
				.entrySet()) {
			outputConverterForTensorId.put(entry.getKey().getIdentifier(), entry.getValue());
		}

		try (final DLRowInputRowIterator rowIterator = new DLRowInputRowIterator(rowInput, columnsForTensorId);
				final DLKnimeNetworkExecutionInputPreparer inputPreparer = new DLKnimeNetworkExecutionInputPreparer(
						rowIterator, batchSize, isPredefinedBatchSize, inputConverterForTensorId);
				final DLKnimeNetworkOutputConsumer outputConsumer = new DLKnimeNetworkOutputConsumer(rowOutput,
						inputPreparer.getBaseRows()::remove, keepInputColumns, outputConverterForTensorId, exec);
				final DLNetworkExecutionSession session = ctx.createExecutionSession(network,
						ExecutionSpecCreator.createExecutionSpecs(
								rowIterator.peek(), ctx.getTensorFactory(), batchSize,
								columnsForTensorId, m_inputConverters),
						outputConverterForTensorId.keySet(), inputPreparer, outputConsumer)) {
			int numBatches = -1;
			try {
				numBatches = (int) inputPreparer.getNumBatches();
			} catch (final UnsupportedOperationException ex) {
				// ignore - we now know that we don't know the number of batches
			}
			final DLExecutionStatus status = numBatches != -1 ? new DLDefaultExecutionStatus(numBatches)
					: new DLDefaultExecutionStatus();
			final DLKnimeExecutionMonitor monitor = new DLKnimeExecutionMonitor(exec, status);
			monitor.getExecutionStatus().batchEnded().addListener((src, v) -> {
				final int currBatch = status.getCurrentBatch() + 1;
				if (status.getNumBatches().isPresent()) {
					final int numBatch = status.getNumBatches().getAsInt();
					monitor.setProgress(currBatch / (double) numBatch,
							"Processing batch " + currBatch + " of " + numBatch + "...");
				} else {
					monitor.setMessage("Processing batch " + currBatch + "...");
				}
			});
			session.run(monitor);
		} catch (final CanceledExecutionException | DLCanceledExecutionException e) {
			throw e;
		} catch (final Exception e) {
			final Throwable cause = e.getCause();
			if (cause != null) {
				if (cause instanceof CanceledExecutionException) {
					throw (CanceledExecutionException) cause;
				} else if (cause instanceof DLCanceledExecutionException) {
					throw new CanceledExecutionException(e.getMessage());
				}
			}
			String message;
			if (e instanceof DLException) {
				message = e.getMessage();
			} else {
				if (!Strings.isNullOrEmpty(e.getMessage())) {
					LOGGER.error(e.getMessage());
				}
				message = "An error occured during execution of the deep learning network. See log for details.";
			}
			throw new RuntimeException(message, e);
		}
	}

	// workaround; when changing code here, also update DLExecutorInputPanel#getAllowedInputColumnType
	private Class<? extends DataValue> getAllowedInputColumnType(final DLExecutorInputConfig inputCfg)
			throws DLMissingExtensionException {
		final DLDataValueToTensorConverterFactory<? extends DataValue, ?> conv = DLDataValueToTensorConverterRegistry
				.getInstance().getConverterFactory(inputCfg.getConverterModel().getStringArrayValue()[1])
				.orElseThrow(() -> new DLMissingExtensionException(
						"Converter '" + inputCfg.getConverterModel().getStringArrayValue()[0] + " ("
								+ inputCfg.getConverterModel().getStringArrayValue()[1]
								+ ")' could not be found. Are you missing a KNIME extension?"));
		return conv.getSourceType();
	}
}
