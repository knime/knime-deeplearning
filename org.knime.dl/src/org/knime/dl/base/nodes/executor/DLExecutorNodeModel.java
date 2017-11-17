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
package org.knime.dl.base.nodes.executor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.append.AppendedColumnRow;
import org.knime.core.data.def.DefaultRow;
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
import org.knime.core.util.Pair;
import org.knime.core.util.UniqueNameGenerator;
import org.knime.dl.base.nodes.executor.DLExecutorInputConfig.DLDataTypeColumnFilter;
import org.knime.dl.base.portobjects.DLNetworkPortObject;
import org.knime.dl.base.portobjects.DLNetworkPortObjectSpec;
import org.knime.dl.core.DLException;
import org.knime.dl.core.DLMissingExtensionException;
import org.knime.dl.core.DLNetwork;
import org.knime.dl.core.DLNetworkSpec;
import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.core.data.convert.DLDataValueToTensorConverterFactory;
import org.knime.dl.core.data.convert.DLDataValueToTensorConverterRegistry;
import org.knime.dl.core.data.convert.DLTensorToDataCellConverterFactory;
import org.knime.dl.core.data.convert.DLTensorToDataCellConverterRegistry;
import org.knime.dl.core.execution.DLExecutableNetworkAdapter;
import org.knime.dl.core.execution.DLExecutionContext;
import org.knime.dl.core.execution.DLExecutionContextRegistry;
import org.knime.dl.core.execution.DLInvalidNetworkInputException;
import org.knime.dl.core.execution.DLKnimeNetworkExecutor;
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
		for (final DLTensorSpec layerDataSpec : networkSpec.getInputSpecs()) {
			// validate layer spec
			if (!DLUtils.Shapes.isFixed(layerDataSpec.getShape())) {
				throw new InvalidSettingsException("Input '" + layerDataSpec.getName()
						+ "' has an (at least partially) unknown shape. This is not supported.");
			}
			final DLExecutorInputConfig inputCfg = m_inputCfgs.get(layerDataSpec.getName());
			if (inputCfg == null) {
				throw new InvalidSettingsException(
						"Network input '" + layerDataSpec.getName() + "' is not yet configured.");
			}
			// get selected converter
			final DLDataValueToTensorConverterFactory<?, ?> converter = DLDataValueToTensorConverterRegistry
					.getInstance().getConverterFactory(inputCfg.getConverterModel().getStringArrayValue()[1])
					.orElseThrow(() -> new DLMissingExtensionException(
							"Converter '" + inputCfg.getConverterModel().getStringArrayValue()[0] + " ("
									+ inputCfg.getConverterModel().getStringArrayValue()[1] + ")' of input '"
									+ inputCfg.getInputTensorName()
									+ "' could not be found. Are you missing a KNIME extension?"));
			m_inputConverters.put(layerDataSpec, converter);

			if (m_lastConfiguredTableSpec != null) {
				// check if selected columns are still in input table:
				final DataColumnSpecFilterConfiguration filterConfig = inputCfg.getInputColumnsModel();
				// TODO: workaround
				((DLDataTypeColumnFilter) filterConfig.getFilter())
						.setFilterClasses(getAllowedInputColumnType(inputCfg));
				final String[] missingColumns = filterConfig.applyTo(inDataSpec).getRemovedFromIncludes();
				if (missingColumns.length != 0) {
					throw new InvalidSettingsException("Selected column '" + missingColumns[0] + "' of input '"
							+ layerDataSpec.getName() + "' is missing. Please reconfigure the node.");
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
			final DLTensorSpec layerDataSpec = DLUtils.Networks.findSpec(layerDataName, networkSpec)
					.orElseThrow(() -> new InvalidSettingsException("Selected output '" + layerDataName
							+ "' could not be found in the input deep learning network."));
			DLUtils.Shapes.getFixedShape(layerDataSpec.getShape())
					.orElseThrow(() -> new InvalidSettingsException("Selected output '" + layerDataName
							+ "' has an (at least partially) unknown shape. This is not supported."));
			final DLExecutorOutputConfig cfg = m_outputCfgs.get(layerDataName);
			// get selected converter
			final DLTensorToDataCellConverterFactory<?, ?> converter = DLTensorToDataCellConverterRegistry.getInstance()
					.getConverterFactory(cfg.getConverterModel().getStringArrayValue()[1])
					.orElseThrow(() -> new DLMissingExtensionException(
							"Converter '" + cfg.getConverterModel().getStringArrayValue()[0] + " ("
									+ cfg.getConverterModel().getStringArrayValue()[1] + ")' for output '"
									+ layerDataName + "' could not be found. Are you missing a KNIME extension?"));
			m_outputConverters.put(layerDataSpec, converter);
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
			final long count = converter.getDestCount(layerDataSpec);
			final String prefix = m_outputCfgs.get(layerDataSpec.getName()).getPrefixModel().getStringValue();
			for (int i = 0; i < count; i++) {
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

		if (inDataSpec.getNumColumns() == 0) {
			throw new IllegalStateException("Input table has no columns.");
		}

		final boolean inDataHasSize;
		final long inDataSize;
		if (rowInput instanceof DataTableRowInput) {
			inDataHasSize = true;
			inDataSize = ((DataTableRowInput) rowInput).getRowCount();
		} else {
			inDataHasSize = false;
			inDataSize = -1;
		}

		final int batchSize;
		if (inDataHasSize && inDataSize < m_generalCfg.getBatchSizeModel().getIntValue()) {
			batchSize = (int) inDataSize;
		} else {
			batchSize = m_generalCfg.getBatchSizeModel().getIntValue();
		}
		final boolean keepInputColumns = m_generalCfg.getKeepInputColumnsModel().getBooleanValue();

		final String[] selectedCtx = m_generalCfg.getExecutionContext();
		final DLExecutionContext<N> ctx = (DLExecutionContext<N>) DLExecutionContextRegistry.getInstance()
				.getExecutionContextsForNetworkType(network.getClass()).stream()
				.filter(inner -> inner.getIdentifier().equals(selectedCtx[1])).findFirst()
				.orElseThrow(() -> new DLMissingExtensionException(
						"There is no available execution back end of name '" + selectedCtx[0] + " (" + selectedCtx[1]
								+ ")' that supports the input network of type '" + network.getClass().getCanonicalName()
								+ "'. Are you missing a KNIME Deep Learning extension?"));

		final DLExecutableNetworkAdapter executableNetwork = ctx.executable(network, m_outputConverters.keySet());

		// assign input column indices to network inputs
		final List<Pair<DLTensorSpec, int[]>> inputs = new ArrayList<>(networkSpec.getInputSpecs().length);
		for (final Entry<DLTensorSpec, DLDataValueToTensorConverterFactory<?, ?>> input : m_inputConverters
				.entrySet()) {
			final DLExecutorInputConfig inputCfg = m_inputCfgs.get(input.getKey().getName());
			final DataColumnSpecFilterConfiguration filterConfig = inputCfg.getInputColumnsModel();
			// TODO: workaround
			((DLDataTypeColumnFilter) filterConfig.getFilter()).setFilterClasses(getAllowedInputColumnType(inputCfg));
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

		try (DLKnimeNetworkExecutor executor = new DLKnimeNetworkExecutor(executableNetwork, m_inputConverters,
				m_outputConverters)) {

			final DLNetworkExecutorOutputConsumer networkOutputConsumer = new DLNetworkExecutorOutputConsumer(
					m_outputConverters.keySet().stream().collect(Collectors.toList()));

			// TODO: optimize here!
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
					final Map<DLTensorSpec, List<DataValue>[]> temp = new HashMap<>();
					for (final Pair<DLTensorSpec, int[]> entry : inputs) {
						temp.put(entry.getFirst(), new ArrayList[batch.size()]);
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
							temp.get(entry.getFirst())[j] = cells;
						}
					}
					// execute
					executor.execute(temp, networkOutputConsumer, exec, batch.size());
					exec.checkCanceled();
					for (final List<DataValue>[] layer : temp.values()) {
						for (final List<DataValue> list : layer) {
							list.clear();
						}
					}
					// process output
					final DataCell[][] cells = networkOutputConsumer.collected();
					if (keepInputColumns) {
						for (int j = 0; j < batch.size(); j++) {
							rowOutput.push(new AppendedColumnRow(batch.get(j), cells[j]));
						}
					} else {
						for (int j = 0; j < batch.size(); j++) {
							rowOutput.push(new DefaultRow(batch.get(j).getKey(), cells[j]));
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
					message = "Error occured during execution of network model. See log for details.";
				}
				throw new RuntimeException(message, e);
			} finally {
				rowInput.close();
				rowOutput.close();
			}
		}
	}

	// TODO: workaround
	// when changing code here, also update
	// DLExecutorInputPanel#getAllowedInputColumnType
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

	private static class DLNetworkExecutorOutputConsumer implements Consumer<Map<DLTensorSpec, DataCell[][]>> {

		// TODO: performance (looping here and in #execute, copying)

		private final List<DataCell[]> m_tmpList = new ArrayList<>();

		private final List<DLTensorSpec> m_orderedOutputs;

		private DLNetworkExecutorOutputConsumer(final List<DLTensorSpec> orderedOutput) {
			m_orderedOutputs = orderedOutput;
		}

		@Override
		public void accept(final Map<DLTensorSpec, DataCell[][]> output) {
			m_tmpList.clear();
			final ArrayList<DataCell[][]> a = new ArrayList<>(m_orderedOutputs.size());
			for (final DLTensorSpec spec : m_orderedOutputs) {
				a.add(output.get(spec));
			}
			for (int i = 0; i < a.get(0).length; i++) { // iterate over batch
				final ArrayList<DataCell> b = new ArrayList<>();
				for (int j = 0; j < a.size(); j++) { // iterate over each output
					final DataCell[] c = a.get(j)[i];
					for (int h = 0; h < c.length; h++) { // iterate over output
															// cells
						b.add(c[h]);
					}
				}
				m_tmpList.add(b.toArray(new DataCell[0]));
			}
		}

		public DataCell[][] collected() {
			return m_tmpList.toArray(new DataCell[m_tmpList.size()][0]);
		}
	}
}
