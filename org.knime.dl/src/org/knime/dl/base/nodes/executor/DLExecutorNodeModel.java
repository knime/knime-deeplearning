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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.OptionalLong;
import java.util.stream.Collectors;

import org.knime.core.data.DataColumnSpec;
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
import org.knime.dl.base.nodes.DLConfigurationUtility;
import org.knime.dl.base.nodes.DLTensorRole;
import org.knime.dl.base.portobjects.DLNetworkPortObject;
import org.knime.dl.base.portobjects.DLNetworkPortObjectSpec;
import org.knime.dl.base.settings.DLDataTypeColumnFilter;
import org.knime.dl.core.DLCanceledExecutionException;
import org.knime.dl.core.DLException;
import org.knime.dl.core.DLExecutionSpecCreator;
import org.knime.dl.core.DLInstallationTestTimeout;
import org.knime.dl.core.DLInstallationTestTimeoutException;
import org.knime.dl.core.DLMissingDependencyException;
import org.knime.dl.core.DLNetwork;
import org.knime.dl.core.DLNetworkSpec;
import org.knime.dl.core.DLNotCancelable;
import org.knime.dl.core.DLRowInputRowIterator;
import org.knime.dl.core.DLTensorId;
import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.core.data.convert.DLDataValueToTensorConverterFactory;
import org.knime.dl.core.data.convert.DLTensorToDataCellConverterFactory;
import org.knime.dl.core.execution.DLDefaultExecutionStatus;
import org.knime.dl.core.execution.DLExecutionContext;
import org.knime.dl.core.execution.DLExecutionStatus;
import org.knime.dl.core.execution.DLKnimeExecutionMonitor;
import org.knime.dl.core.execution.DLKnimeNetworkExecutionInputPreparer;
import org.knime.dl.core.execution.DLKnimeNetworkOutputConsumer;
import org.knime.dl.core.execution.DLNetworkExecutionSession;
import org.knime.dl.util.DLUtils;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
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
            if (!areNetworkSpecsCompatible(m_lastIncomingNetworkSpec, m_lastConfiguredNetworkSpec)) {
				throw new InvalidSettingsException("Input deep learning network changed. Please reconfigure the node.");
			}
//			else if (!m_lastConfiguredTableSpec.equals(m_lastIncomingTableSpec)) {
//			    throw new InvalidSettingsException("Input table changed. Please reconfigure the node.");
//			}
		} else if (m_initialLoaded) {
			// loaded from saved workflow
			m_lastConfiguredNetworkSpec = m_lastIncomingNetworkSpec;
			m_lastConfiguredTableSpec = m_lastIncomingTableSpec;
		}

		try {
			configureGeneral(networkType);
			configureInputs(networkSpec, inDataSpec);
			configureOutputs(networkSpec);
		} catch (final Exception e) {
			throw new InvalidSettingsException(e.getMessage(), e);
		}

		// TODO remove temporal dependency between configureOutputs and createOutputSpec
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
	    try {
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
	    } catch (final InvalidSettingsException e) {
	        throw new RuntimeException(e.getMessage(), e);
	    }
	}

	@Override
	protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {

		m_inputCfgs.clear();
		final NodeSettingsRO inputSettings = settings.getNodeSettings(CFG_KEY_INPUTS);
		for (final String layerName : inputSettings) {
			final DLExecutorInputConfig inputCfg = createInputTensorModelConfig(layerName, m_generalCfg);
			m_inputCfgs.put(layerName, inputCfg);
		}

		m_outputCfgs.clear();
		final NodeSettingsRO outputSettings = settings.getNodeSettings(CFG_KEY_OUTPUTS);
		for (final String layerName : outputSettings) {
			final DLExecutorOutputConfig outputCfg = createOutputTensorModelConfig(layerName, m_generalCfg);
			m_outputCfgs.put(layerName, outputCfg);
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

    private boolean areNetworkSpecsCompatible(final DLNetworkSpec newSpec, final DLNetworkSpec oldSpec) {
        // Network types must be the same.
        if (!newSpec.getClass().equals(oldSpec.getClass())) {
            return false;
        }
        // Inputs must be the same.
        if (!Sets.symmetricDifference(new HashSet<>(Arrays.asList(newSpec.getInputSpecs())),
            new HashSet<>(Arrays.asList(oldSpec.getInputSpecs()))).isEmpty()) {
            return false;
        }
        // Selected outputs must still be available.
        for (final String tensorName : m_smOutputOrder.getStringArrayValue()) {
            DLTensorSpec newTensorSpec = null;
            try {
                newTensorSpec = getOutputOrHiddenTensorSpec(tensorName, newSpec);
            } catch (final InvalidSettingsException e) {
                return false;
            }
            DLTensorSpec oldTensorSpec = null;
            try {
                oldTensorSpec = getOutputOrHiddenTensorSpec(tensorName, oldSpec);
            } catch (final InvalidSettingsException e) {
                continue; // This should not happen. But if it does, the new spec is valid.
            }
            if (!newTensorSpec.equals(oldTensorSpec)) {
                return false;
            }
        }

        return true;
    }

    private DLTensorSpec getOutputOrHiddenTensorSpec(final String tensorName, final DLNetworkSpec networkSpec)
        throws InvalidSettingsException {
        return DLUtils.Networks.findSpec(tensorName, networkSpec.getHiddenOutputSpecs(), networkSpec.getOutputSpecs())
            .orElseThrow(() -> new InvalidSettingsException(
                "Selected output '" + tensorName + "' could not be found in the input deep learning network."));
    }

	private void configureGeneral(final Class<? extends DLNetwork> networkType)
			throws DLMissingDependencyException, InvalidSettingsException {
		DLExecutionContext<?> backend = m_generalCfg.getContextEntry().getValue();
		if (backend == null) {
		    final List<DLExecutionContext<?>> availableBackends = DLExecutorGeneralConfig
		            .getAvailableExecutionContexts(networkType).stream()
		            .sorted(Comparator.comparing(DLExecutionContext::getName))
		            .collect(Collectors.toList());
		    if (availableBackends.isEmpty()) {
                throw new DLMissingDependencyException("No compatible training back end available. "
                        + "Are you missing a KNIME Deep Learning extension?");
            }
            backend = availableBackends.get(0);
            m_generalCfg.getContextEntry().setValue(backend);
		}
        if (!networkType.isAssignableFrom(backend.getNetworkType())) {
            throw new InvalidSettingsException(
                "Selected back end is not compatible to the input deep learning network. Please reconfigure the node.");
        }
	}

	private void configureInputs(final DLNetworkSpec networkSpec, final DataTableSpec inDataSpec)
			throws InvalidSettingsException {
		m_inputConverters = new LinkedHashMap<>(m_inputCfgs.size());
		for (final DLTensorSpec tensorSpec : networkSpec.getInputSpecs()) {
			// validate layer spec
			final DLExecutorInputConfig inputCfg = m_inputCfgs.get(tensorSpec.getName());
			if (inputCfg == null) {
				throw new InvalidSettingsException(
						"Network input '" + tensorSpec.getName() + "' is not yet configured.");
			}
			// get selected converter
			final DLDataValueToTensorConverterFactory<?, ?> converter = DLConfigurationUtility.configureInput(inputCfg, tensorSpec,
			    m_generalCfg.getContextEntry().getValue(), inDataSpec, m_lastConfiguredTableSpec, DLTensorRole.INPUT);
			m_inputConverters.put(tensorSpec, converter);
		}
	}

    private void configureOutputs(final DLNetworkSpec networkSpec)
        throws InvalidSettingsException {
		if (m_outputCfgs.size() == 0) {
			throw new InvalidSettingsException("No network output was selected.");
		}
		m_outputConverters = new LinkedHashMap<>(m_outputCfgs.size());
		for (final String tensorName : m_smOutputOrder.getStringArrayValue()) {
            final DLTensorSpec tensorSpec = getOutputOrHiddenTensorSpec(tensorName, networkSpec);
			if (!DLUtils.Shapes.isKnown(tensorSpec.getShape())) {
				throw new InvalidSettingsException(
						"Selected output '" + tensorName + "' has an unknown shape. This is not supported.");
			}
			final DLExecutorOutputConfig cfg = m_outputCfgs.get(tensorName);
			// get selected converter
			final DLTensorToDataCellConverterFactory<?, ?> converter = cfg.getConverterEntry().getValue();
			m_outputConverters.put(tensorSpec, converter);
		}
	}

	private DataTableSpec createOutputSpec(final DataTableSpec inDataSpec) {
		final boolean keepInputColumns = m_generalCfg.getKeepInputColumnsEntry().getValue();
		final ArrayList<DataColumnSpec> outputSpecs = new ArrayList<>();
		final UniqueNameGenerator nameGenerator = new UniqueNameGenerator(keepInputColumns ? inDataSpec : null);
		for (final Entry<DLTensorSpec, DLTensorToDataCellConverterFactory<?, ?>> output : m_outputConverters
				.entrySet()) {
			final DLTensorSpec tensorSpec = output.getKey();
			final DLTensorToDataCellConverterFactory<?, ?> converter = output.getValue();
			final OptionalLong count = converter.getDestCount(tensorSpec);
			final String prefix = m_outputCfgs.get(tensorSpec.getName()).getPrefixEntry().getValue();
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

        final DLExecutionContext<N> ctx = (DLExecutionContext<N>)m_generalCfg.getContextEntry().getValue();
        try {
            ctx.checkAvailability(false, DLInstallationTestTimeout.getInstallationTestTimeout(),
                DLNotCancelable.INSTANCE);
        } catch (final DLMissingDependencyException | DLInstallationTestTimeoutException
                | DLCanceledExecutionException e) {
            throw new InvalidSettingsException("Selected back end '" + ctx.getName() + "' is not available anymore. "
                + "Please check your local installation.\nDetails: " + e.getMessage());
        }

		final int batchSize = m_generalCfg.getBatchSizeEntry().getValue();
		final boolean isPredefinedBatchSize = Arrays.stream(networkSpec.getInputSpecs())
				.anyMatch(s -> s.getBatchSize().isPresent());

		final boolean keepInputColumns = m_generalCfg.getKeepInputColumnsEntry().getValue();

		// assign input column indices to network inputs
		final LinkedHashMap<DLTensorId, int[]> columnsForTensorId = new LinkedHashMap<>(m_inputConverters.size());
		final LinkedHashMap<DLTensorId, DLDataValueToTensorConverterFactory<?, ?>> inputConverterForTensorId = new LinkedHashMap<>(
				m_inputConverters.size());

		fillInputSpecificMaps(inDataSpec, columnsForTensorId, inputConverterForTensorId);

		final LinkedHashMap<DLTensorId, DLTensorToDataCellConverterFactory<?, ?>> outputConverterForTensorId =
            createOutputConverterMap();

		try (final DLRowInputRowIterator rowIterator = new DLRowInputRowIterator(rowInput, columnsForTensorId);
				final DLKnimeNetworkExecutionInputPreparer inputPreparer = new DLKnimeNetworkExecutionInputPreparer(
						rowIterator, batchSize, isPredefinedBatchSize, inputConverterForTensorId);
				final DLKnimeNetworkOutputConsumer outputConsumer = new DLKnimeNetworkOutputConsumer(rowOutput,
						inputPreparer.getBaseRows()::remove, keepInputColumns, outputConverterForTensorId, exec);
				final DLNetworkExecutionSession session = ctx.createExecutionSession(network,
						DLExecutionSpecCreator.createExecutionSpecs(rowIterator.peek(), ctx.getTensorFactory(),
								batchSize, columnsForTensorId, m_inputConverters),
						outputConverterForTensorId.keySet(), inputPreparer, outputConsumer)) {
			final DLKnimeExecutionMonitor monitor = createExecutionMonitor(exec, inputPreparer);
			session.run(monitor);
		} catch (final CanceledExecutionException | DLCanceledExecutionException e) {
			throw e;
		} catch (final Exception e) {
			handleGeneralException(e);
		}
	}

    private LinkedHashMap<DLTensorId, DLTensorToDataCellConverterFactory<?, ?>> createOutputConverterMap() {
        final LinkedHashMap<DLTensorId, DLTensorToDataCellConverterFactory<?, ?>> outputConverterForTensorId = new LinkedHashMap<>(
				m_outputConverters.size());
		for (final Entry<DLTensorSpec, DLTensorToDataCellConverterFactory<?, ?>> entry : m_outputConverters
				.entrySet()) {
			outputConverterForTensorId.put(entry.getKey().getIdentifier(), entry.getValue());
		}
        return outputConverterForTensorId;
    }

    private void fillInputSpecificMaps(final DataTableSpec inDataSpec,
        final LinkedHashMap<DLTensorId, int[]> columnsForTensorId,
        final LinkedHashMap<DLTensorId, DLDataValueToTensorConverterFactory<?, ?>> inputConverterForTensorId) {
        for (final Entry<DLTensorSpec, DLDataValueToTensorConverterFactory<?, ?>> entry : m_inputConverters
				.entrySet()) {
			final DLTensorSpec spec = entry.getKey();
			final DLExecutorInputConfig inputCfg = m_inputCfgs.get(spec.getName());
			final DataColumnSpecFilterConfiguration filterConfig = inputCfg.getInputColumnsEntry().getValue();
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
    }

    private static void handleGeneralException(final Exception e) throws CanceledExecutionException {
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

    private static DLKnimeExecutionMonitor createExecutionMonitor(final ExecutionContext exec,
        final DLKnimeNetworkExecutionInputPreparer inputPreparer) {
        final DLExecutionStatus status = createExecutionStatus(inputPreparer);
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
        return monitor;
    }

	private static DLExecutionStatus createExecutionStatus(final DLKnimeNetworkExecutionInputPreparer inputPreparer) {
	    int numBatches = -1;
        try {
            numBatches = (int) inputPreparer.getNumBatches();
        } catch (final UnsupportedOperationException ex) {
            // ignore - we now know that we don't know the number of batches
        }
        return numBatches != -1 ? new DLDefaultExecutionStatus(numBatches)
                : new DLDefaultExecutionStatus();
	}

	// workaround; when changing code here, also update DLExecutorInputPanel#getAllowedInputColumnType
	private static Class<? extends DataValue> getAllowedInputColumnType(final DLExecutorInputConfig inputCfg) {
		final DLDataValueToTensorConverterFactory<? extends DataValue, ?> conv = inputCfg.getConverterEntry().getValue();
		return conv.getSourceType();
	}
}
