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
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
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
import org.knime.core.node.defaultnodesettings.SettingsModelString;
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
import org.knime.core.util.Pair;
import org.knime.dl.base.portobjects.DLNetworkPortObject;
import org.knime.dl.base.portobjects.DLNetworkPortObjectSpec;
import org.knime.dl.core.DLExecutableNetwork;
import org.knime.dl.core.DLLayerDataSpec;
import org.knime.dl.core.DLNetwork;
import org.knime.dl.core.DLNetworkSpec;
import org.knime.dl.core.backend.DLBackend;
import org.knime.dl.core.backend.DLBackendRegistry;
import org.knime.dl.core.backend.DLProfile;
import org.knime.dl.core.data.convert.input.DLDataValueToLayerDataConverterFactory;
import org.knime.dl.core.data.convert.input.DLDataValueToLayerDataConverterRegistry;
import org.knime.dl.core.data.convert.output.DLLayerDataToDataCellConverterFactory;
import org.knime.dl.core.data.convert.output.DLLayerDataToDataCellConverterRegistry;
import org.knime.dl.core.execution.DLFromKnimeNetworkExecutor;
import org.knime.dl.util.DLUtils;

/**
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
final class DLExecutorNodeModel extends NodeModel {

    static final int IN_NETWORK_PORT_IDX = 0;

    static final int IN_DATA_PORT_IDX = 1;

    static final int OUT_DATA_PORT_IDX = 0;

    static final String CFG_KEY_INPUTS = "inputs";

    static final String CFG_KEY_OUTPUTS = "outputs";

    static final String CFG_KEY_OUTPUTS_ORDER = "outputs_ordered";

    private static final NodeLogger LOGGER = NodeLogger.getLogger(DLExecutorNodeModel.class);

    static DLGeneralModelConfig createGeneralModelConfig() {
        return new DLGeneralModelConfig("<none>", 100);
    }

    static DLInputLayerDataModelConfig createInputLayerDataModelConfig(final String configKey,
        final SettingsModelString smBackend) {
        return new DLInputLayerDataModelConfig(configKey, smBackend);
    }

    static DLOutputLayerDataModelConfig createOutputLayerDataModelConfig(final String configKey,
        final SettingsModelString smBackend) {
        return new DLOutputLayerDataModelConfig(configKey, smBackend);
    }

    static SettingsModelStringArray createOutputOrderSettingsModel(final int outputsCount) {
        return new SettingsModelStringArray(CFG_KEY_OUTPUTS_ORDER, new String[outputsCount]);
    }

    private final DLGeneralModelConfig m_generalCfg;

    private final HashMap<String, DLInputLayerDataModelConfig> m_inputCfgs;

    private final HashMap<String, DLOutputLayerDataModelConfig> m_outputCfgs;

    private SettingsModelStringArray m_outputOrder;

    private DLBackend m_backend;

    private LinkedHashMap<DLLayerDataSpec, DLDataValueToLayerDataConverterFactory<?, ?>> m_inputConverters;

    private LinkedHashMap<DLLayerDataSpec, DLLayerDataToDataCellConverterFactory<?, ?>> m_outputConverters;

    private DLNetworkSpec m_lastIncomingNetworkSpec;

    private DLNetworkSpec m_lastConfiguredNetworkSpec;

    private boolean m_initialLoaded;

    DLExecutorNodeModel() {
        super(new PortType[]{DLNetworkPortObject.TYPE, BufferedDataTable.TYPE}, new PortType[]{BufferedDataTable.TYPE});
        m_generalCfg = createGeneralModelConfig();
        m_inputCfgs = new HashMap<>();
        m_outputCfgs = new HashMap<>();
        m_lastIncomingNetworkSpec = null;
        m_lastConfiguredNetworkSpec = null;
        m_initialLoaded = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputPortRole[] getInputPortRoles() {
        return new InputPortRole[]{InputPortRole.NONDISTRIBUTED_NONSTREAMABLE, InputPortRole.NONDISTRIBUTED_STREAMABLE};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OutputPortRole[] getOutputPortRoles() {
        return new OutputPortRole[]{OutputPortRole.NONDISTRIBUTED};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo,
        final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        return new StreamableOperator() {

            @Override
            public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext exec)
                throws Exception {
                final PortObject portObject = ((PortObjectInput)inputs[IN_NETWORK_PORT_IDX]).getPortObject();
                final RowInput rowInput = (RowInput)inputs[IN_DATA_PORT_IDX];
                final RowOutput rowOutput = (RowOutput)outputs[OUT_DATA_PORT_IDX];

                executeInternal(portObject, rowInput, rowOutput, exec);
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        if (inSpecs[DLExecutorNodeModel.IN_NETWORK_PORT_IDX] == null) {
            throw new InvalidSettingsException("Input deep learning network port object is missing.");
        }
        if (inSpecs[DLExecutorNodeModel.IN_DATA_PORT_IDX] == null) {
            throw new InvalidSettingsException("Input data table is missing.");
        }
        if (!DLNetworkPortObject.TYPE.acceptsPortObjectSpec(inSpecs[DLExecutorNodeModel.IN_NETWORK_PORT_IDX])) {
            throw new InvalidSettingsException("Input port object is not a valid deep learning network port object.");
        }

        final DLNetworkPortObjectSpec portObjectSpec = ((DLNetworkPortObjectSpec)inSpecs[IN_NETWORK_PORT_IDX]);
        final DLNetworkSpec networkSpec = portObjectSpec.getNetworkSpec();
        final DataTableSpec inDataSpec = ((DataTableSpec)inSpecs[IN_DATA_PORT_IDX]);

        if (networkSpec == null) {
            throw new InvalidSettingsException("Input port object's deep learning network specs are missing.");
        }
        if (portObjectSpec.getProfile() == null) {
            throw new InvalidSettingsException("Input port object's deep learning profile is missing.");
        }
        if (networkSpec.getInputSpecs().length == 0) {
            LOGGER.warn("Input deep learning network has no input specs.");
        }
        if (networkSpec.getOutputSpecs().length == 0 && networkSpec.getIntermediateOutputSpecs().length == 0) {
            LOGGER.warn("Input deep learning network has no output specs.");
        }
        if (portObjectSpec.getProfile().size() == 0) {
            throw new InvalidSettingsException("Input deep learning network has no associated back end.");
        }

        m_lastIncomingNetworkSpec = networkSpec;

        if (m_lastConfiguredNetworkSpec != null) {
            if (!m_lastConfiguredNetworkSpec.equals(m_lastIncomingNetworkSpec)) {
                throw new InvalidSettingsException("Input deep learning network changed. Please reconfigure the node.");
            }
        } else if (m_initialLoaded) {
            // loaded from saved workflow
            m_lastConfiguredNetworkSpec = m_lastIncomingNetworkSpec;
        }

        configureGeneral(portObjectSpec.getProfile());
        configureInputs(networkSpec);
        configureOutputs(networkSpec);

        final DataTableSpec outDataSpec = createOutputSpec(inDataSpec);
        return new PortObjectSpec[]{outDataSpec};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        final PortObject portObject = inObjects[IN_NETWORK_PORT_IDX];
        final BufferedDataTable inData = (BufferedDataTable)inObjects[IN_DATA_PORT_IDX];
        final DataTableSpec inDataSpec = inData.getDataTableSpec();

        final RowInput rowInput = new DataTableRowInput(inData);
        final BufferedDataTableRowOutput rowOutput =
            new BufferedDataTableRowOutput(exec.createDataContainer(createOutputSpec(inDataSpec)));

        executeInternal(portObject, rowInput, rowOutput, exec);

        return new PortObject[]{rowOutput.getDataTable()};
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // no op
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // no op
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_generalCfg.saveToSettings(settings);

        final NodeSettingsWO inputSettings = settings.addNodeSettings(CFG_KEY_INPUTS);
        for (final DLInputLayerDataModelConfig inputCfg : m_inputCfgs.values()) {
            inputCfg.saveToSettings(inputSettings);
        }

        final NodeSettingsWO outputSettings = settings.addNodeSettings(CFG_KEY_OUTPUTS);
        for (final DLOutputLayerDataModelConfig outputCfg : m_outputCfgs.values()) {
            outputCfg.saveToSettings(outputSettings);
        }

        if (m_outputOrder != null) {
            m_outputOrder.saveSettingsTo(settings);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_generalCfg.validateSettings(settings);

        m_inputCfgs.clear();
        final NodeSettingsRO inputSettings = settings.getNodeSettings(CFG_KEY_INPUTS);
        for (final String layerName : inputSettings) {
            final DLInputLayerDataModelConfig inputCfg =
                createInputLayerDataModelConfig(layerName, m_generalCfg.getBackendModel());
            m_inputCfgs.put(layerName, inputCfg);
            inputCfg.validateSettings(inputSettings);
        }

        m_outputCfgs.clear();
        final NodeSettingsRO outputSettings = settings.getNodeSettings(CFG_KEY_OUTPUTS);
        for (final String layerName : outputSettings) {
            final DLOutputLayerDataModelConfig outputCfg =
                createOutputLayerDataModelConfig(layerName, m_generalCfg.getBackendModel());
            m_outputCfgs.put(layerName, outputCfg);
            outputCfg.validateSettings(outputSettings);
        }

        m_outputOrder = createOutputOrderSettingsModel(m_outputCfgs.size());
        m_outputOrder.validateSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_generalCfg.loadFromSettings(settings);
        final NodeSettingsRO inputSettings = settings.getNodeSettings(CFG_KEY_INPUTS);
        for (final DLInputLayerDataModelConfig inputCfg : m_inputCfgs.values()) {
            inputCfg.loadFromSettings(inputSettings);
        }

        final NodeSettingsRO outputSettings = settings.getNodeSettings(CFG_KEY_OUTPUTS);
        for (final DLOutputLayerDataModelConfig outputCfg : m_outputCfgs.values()) {
            outputCfg.loadFromSettings(outputSettings);
        }

        m_outputOrder.loadSettingsFrom(settings);

        m_lastConfiguredNetworkSpec = m_lastIncomingNetworkSpec;
        m_initialLoaded = true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_backend = null;
    }

    private void configureGeneral(final DLProfile profile) {
        final DLBackend backend =
            DLBackendRegistry.getBackend(m_generalCfg.getBackendModel().getStringValue()).orElseGet(() -> {
                try {
                    return DLBackendRegistry.getPreferredBackend(profile);
                } catch (final Exception ex) {
                    throw new IllegalStateException("There is no available back end that supports the input network.");
                }
            });
        m_backend = backend;
    }

    private void configureInputs(final DLNetworkSpec networkSpec) throws InvalidSettingsException {
        m_inputConverters = new LinkedHashMap<>(m_inputCfgs.size());
        for (final DLLayerDataSpec layerDataSpec : networkSpec.getInputSpecs()) {
            // validate layer spec
            if (!DLUtils.Shapes.isFixed(layerDataSpec.getShape())) {
                throw new InvalidSettingsException("Input '" + layerDataSpec.getName()
                    + "' has an (at least partially) unknown shape. This is not supported.");
            }
            final DLInputLayerDataModelConfig inputCfg = m_inputCfgs.get(layerDataSpec.getName());
            if (inputCfg == null) {
                throw new InvalidSettingsException(
                    "Network input '" + layerDataSpec.getName() + "' is not yet configured.");
            }
            // get selected converter
            final DLDataValueToLayerDataConverterFactory<?, ?> converter = DLDataValueToLayerDataConverterRegistry
                .getInstance().getConverterFactory(inputCfg.getConverterModel().getStringValue()).orElseThrow(
                    () -> new InvalidSettingsException("Converter '" + inputCfg.getConverterModel().getStringValue()
                        + "' of input '" + inputCfg.getInputLayerDataName() + "' could not be found."));
            m_inputConverters.put(layerDataSpec, converter);
        }
    }

    private void configureOutputs(final DLNetworkSpec networkSpec) throws InvalidSettingsException {
        if (m_outputCfgs.size() == 0) {
            throw new InvalidSettingsException("No network output was selected.");
        }
        m_outputConverters = new LinkedHashMap<>(m_outputCfgs.size());
        for (final String layerDataName : m_outputOrder.getStringArrayValue()) {
            // validate layer spec
            final DLLayerDataSpec layerDataSpec =
                DLUtils.Networks.findSpec(layerDataName, networkSpec).orElseThrow(() -> new InvalidSettingsException(
                    "Selected output '" + layerDataName + "' could not be found in the input deep learning network."));
            // TODO
            final long[] shape = DLUtils.Shapes.getFixedShape(layerDataSpec.getShape())
                .orElseThrow(() -> new InvalidSettingsException("Selected output '" + layerDataName
                    + "' has an (at least partially) unknown shape. This is not supported."));
            if (shape.length > 1) {
                throw new InvalidSettingsException("Selected output '" + layerDataName
                    + "' has a shape with dimensionality > 1. This is not yet supported.");
            }
            final DLOutputLayerDataModelConfig cfg = m_outputCfgs.get(layerDataName);
            // get selected converter
            final String converterName = cfg.getConverterModel().getStringValue();
            final DLLayerDataToDataCellConverterFactory<?, ?> converter = DLLayerDataToDataCellConverterRegistry
                .getInstance().getConverterFactory(converterName).orElseThrow(() -> new InvalidSettingsException(
                    "Converter '" + converterName + "' for output '" + layerDataName + "' could not be found."));
            m_outputConverters.put(layerDataSpec, converter);
        }
    }

    private DataTableSpec createOutputSpec(final DataTableSpec inDataSpec) throws InvalidSettingsException {
        final ArrayList<DataColumnSpec> outputSpecs = new ArrayList<>();
        for (final Entry<DLLayerDataSpec, DLLayerDataToDataCellConverterFactory<?, ?>> output : m_outputConverters
            .entrySet()) {
            final DLLayerDataSpec layerDataSpec = output.getKey();
            final DLLayerDataToDataCellConverterFactory<?, ?> converter = output.getValue();
            final long count = converter.getDestCount(layerDataSpec);
            final String prefix = m_outputCfgs.get(layerDataSpec.getName()).getPrefixModel().getStringValue();
            for (int i = 0; i < count; i++) {
                outputSpecs.add(
                    new DataColumnSpecCreator(prefix + Integer.toString(i), DataType.getType(converter.getDestType()))
                        .createSpec());
            }
        }
        final DataTableSpec outDataSpec = new DataTableSpec(outputSpecs.toArray(new DataColumnSpec[0]));
        if (m_generalCfg.getKeepInputColumns().getBooleanValue()) {
            return new DataTableSpec(inDataSpec, outDataSpec);
        }
        return outDataSpec;
    }

    private void executeInternal(final PortObject portObject, final RowInput rowInput, final RowOutput rowOutput,
        final ExecutionContext exec) throws Exception {
        if (!(portObject instanceof DLNetworkPortObject)) {
            throw new RuntimeException("Unsupported deep learning network type at input port.");
        }

        final DLNetwork network = ((DLNetworkPortObject)portObject).getNetwork();
        final DLNetworkSpec networkSpec = network.getSpec();
        final DataTableSpec inDataSpec = rowInput.getDataTableSpec();

        if (inDataSpec.getNumColumns() == 0) {
            throw new RuntimeException("Input table has no columns.");
        }

        final boolean inDataHasSize;
        final long inDataSize;
        if (rowInput instanceof DataTableRowInput) {
            inDataHasSize = true;
            inDataSize = ((DataTableRowInput)rowInput).getRowCount();
        } else {
            inDataHasSize = false;
            inDataSize = -1;
        }

        final int batchSize;
        if (inDataHasSize && inDataSize < m_generalCfg.getBatchSizeModel().getIntValue()) {
            batchSize = (int)inDataSize;
        } else {
            batchSize = m_generalCfg.getBatchSizeModel().getIntValue();
        }
        final boolean keepInputColumns = m_generalCfg.getKeepInputColumns().getBooleanValue();

        final DLExecutableNetwork executableNetwork = m_backend.toExecutableNetwork(network);
        // assign input column indices to network inputs
        final List<Pair<DLLayerDataSpec, int[]>> inputs = new ArrayList<>(networkSpec.getInputSpecs().length);
        for (final Entry<DLLayerDataSpec, DLDataValueToLayerDataConverterFactory<?, ?>> input : m_inputConverters
            .entrySet()) {
            final DLInputLayerDataModelConfig inputCfg = m_inputCfgs.get(input.getKey().getName());
            final int[] indices =
                Arrays.stream(inputCfg.getInputColumnsModel().applyTo(inDataSpec).getIncludes()).mapToInt(c -> {
                    final int idx = inDataSpec.findColumnIndex(c);
                    if (idx == -1) {
                        throw new IllegalStateException(
                            "Selected input column '" + c + "' could not be found in the input table.");
                    }
                    return idx;
                }).toArray();
            inputs.add(new Pair<>(input.getKey(), indices));
        }

        try (DLFromKnimeNetworkExecutor executor =
            new DLFromKnimeNetworkExecutor(executableNetwork, m_inputConverters, m_outputConverters)) {

            final DLNetworkExecutorOutputConsumer networkOutputConsumer =
                new DLNetworkExecutorOutputConsumer(m_outputConverters.keySet().stream().collect(Collectors.toList()));

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
                        // process the last, incomplete, batch
                    }
                    exec.checkCanceled();
                    // gather input
                    final Map<DLLayerDataSpec, List<DataValue>[]> temp = new HashMap<>();
                    for (final Pair<DLLayerDataSpec, int[]> entry : inputs) {
                        temp.put(entry.getFirst(), new ArrayList[batch.size()]);
                        for (int j = 0; j < batch.size(); j++) {
                            final ArrayList<DataValue> cells = new ArrayList<>(entry.getSecond().length);
                            for (final int c : entry.getSecond()) {
                                final DataCell cell = batch.get(j).getCell(c);
                                // TODO: we could also allow some missing value handling settings in the dialog.
                                if (cell.isMissing()) {
                                    throw new RuntimeException(
                                        "Missing cell in input row " + batch.get(j).getKey() + ".");
                                }
                                cells.add(cell);
                            }
                            temp.get(entry.getFirst())[j] = cells;
                        }
                    }
                    // execute
                    try {
                        executor.execute(temp, networkOutputConsumer, exec, batch.size());
                    } catch (final Exception ex) {
                        throw new IllegalStateException(ex.getMessage(), ex);
                    }
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
                        exec.setProgress((double)currRowIdx / inDataSize,
                            "Processing row " + row.getKey().toString() + "...");
                    }
                    batch.clear();
                }
            } catch (final Exception e) {
                executor.close();
                LOGGER.debug("Error occured during execution of network model: " + e.getMessage(), e);
                throw new RuntimeException(e.getMessage(), e);
            } finally {
                if (executableNetwork != null) {
                    executableNetwork.close();
                }
                rowInput.close();
                rowOutput.close();
            }
        }
    }

    private static class DLNetworkExecutorOutputConsumer implements Consumer<Map<DLLayerDataSpec, DataCell[][]>> {

        // TODO: performance (looping here and in #execute, copying)

        private final List<DataCell[]> m_tmpList = new ArrayList<>();

        private final List<DLLayerDataSpec> m_orderedOutputs;

        private DLNetworkExecutorOutputConsumer(final List<DLLayerDataSpec> orderedOutput) {
            m_orderedOutputs = orderedOutput;
        }

        @Override
        public void accept(final Map<DLLayerDataSpec, DataCell[][]> output) {
            m_tmpList.clear();
            final ArrayList<DataCell[][]> a = new ArrayList<>(m_orderedOutputs.size());
            for (final DLLayerDataSpec spec : m_orderedOutputs) {
                a.add(output.get(spec));
            }
            for (int i = 0; i < a.get(0).length; i++) { // iterate over batch
                final ArrayList<DataCell> b = new ArrayList<>();
                for (int j = 0; j < a.size(); j++) { // iterate over each output
                    final DataCell[] c = a.get(j)[i];
                    for (int h = 0; h < c.length; h++) { // iterate over output cells
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
