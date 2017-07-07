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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.util.Pair;
import org.knime.dl.base.DLGeneralModelConfig;
import org.knime.dl.base.DLInputLayerDataModelConfig;
import org.knime.dl.base.DLOutputLayerDataModelConfig;
import org.knime.dl.base.portobjects.DLNetworkPortObject;
import org.knime.dl.base.portobjects.DLNetworkPortObjectSpec;
import org.knime.dl.core.DLExecutableNetwork;
import org.knime.dl.core.DLLayerDataSpec;
import org.knime.dl.core.DLNetwork;
import org.knime.dl.core.DLNetworkSpec;
import org.knime.dl.core.backend.DLBackend;
import org.knime.dl.core.backend.DLBackendRegistry;
import org.knime.dl.core.data.convert.input.DLDataValueToLayerConverterFactory;
import org.knime.dl.core.data.convert.input.DLDataValueToLayerDataConverterRegistry;
import org.knime.dl.core.data.convert.output.DLLayerDataToDataCellConverterFactory;
import org.knime.dl.core.data.convert.output.DLLayerDataToDataCellConverterRegistry;
import org.knime.dl.core.execution.DLFromKnimeNetworkExecutor;

/**
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
final class DLExecutorNodeModel extends NodeModel {

    static final int IN_NETWORK_PORT_IDX = 0;

    static final int IN_DATA_PORT_IDX = 1;

    static final int OUT_DATA_PORT_IDX = 0;

    static final String CFG_KEY_INPUTS = "inputs";

    static final String CFG_KEY_INPUT_ARRAY = "input_data";

    static final String CFG_KEY_OUTPUTS = "outputs";

    static final String CFG_KEY_OUTPUTS_ARRAY = "outputs_data";

    static final String CFG_KEY_ORDER = "output_order";

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
        return new SettingsModelStringArray("outputs_ordered", new String[outputsCount]);
    }

    private final DLGeneralModelConfig m_generalCfg;

    private final HashMap<String, DLInputLayerDataModelConfig> m_inputCfgs;

    private final HashMap<String, DLOutputLayerDataModelConfig> m_outputCfgs;

    private SettingsModelStringArray m_outputOrder;

    DLExecutorNodeModel() {
        super(new PortType[]{DLNetworkPortObject.TYPE, BufferedDataTable.TYPE}, new PortType[]{BufferedDataTable.TYPE});
        m_generalCfg = createGeneralModelConfig();
        m_inputCfgs = new HashMap<>();
        m_outputCfgs = new HashMap<>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        if (!(inSpecs[IN_NETWORK_PORT_IDX] instanceof DLNetworkPortObjectSpec)) {
            throw new InvalidSettingsException("Unsupported deep learning network type at input port.");
        }
        final DLNetworkSpec networkSpec = ((DLNetworkPortObjectSpec)inSpecs[IN_NETWORK_PORT_IDX]).getNetworkSpec();
        final DataTableSpec inDataSpec = ((DataTableSpec)inSpecs[IN_DATA_PORT_IDX]);

        DataTableSpec outDataSpec = new DataTableSpec(createOutputSpec(networkSpec));

        if (m_generalCfg.getAppendColumns().getBooleanValue()) {
            outDataSpec = new DataTableSpec(inDataSpec, outDataSpec);
        }
        return new PortObjectSpec[]{outDataSpec};
    }

    private DataColumnSpec[] createOutputSpec(final DLNetworkSpec inNetworkSpec) throws InvalidSettingsException {
        if (m_outputCfgs.size() == 0) {
            throw new InvalidSettingsException("No network output was selected. Output table will equal input table.");
        }
        final ArrayList<DataColumnSpec> appendColumnSpecs = new ArrayList<>();
        DLBackendRegistry.getBackend(m_generalCfg.getBackendModel().getStringValue())
            .orElseThrow(() -> new InvalidSettingsException(
                "Selected back end '" + m_generalCfg.getBackendModel().getStringValue() + "' could not be found."));
        for (final String outputLayerDataSpecName : m_outputOrder.getStringArrayValue()) {
            for (final DLOutputLayerDataModelConfig outputCfg : m_outputCfgs.values()) {
                if (!outputCfg.getOutputLayerDataName().equals(outputLayerDataSpecName)) {
                    continue;
                }
                final DLLayerDataToDataCellConverterFactory<?, ? extends DataCell> converterFactory =
                    DLLayerDataToDataCellConverterRegistry.getInstance()
                        .getConverterFactory(outputCfg.getConverterModel().getStringValue())
                        .orElseThrow(() -> new InvalidSettingsException(
                            "Converter '" + outputCfg.getConverterModel().getStringValue() + "' of output '"
                                + outputCfg.getOutputLayerDataName() + "' could not be found."));
                Optional<DLLayerDataSpec> outSpec = Arrays.stream(inNetworkSpec.getOutputSpecs())
                    .filter(ods -> ods.getName().equals(outputCfg.getOutputLayerDataName())).findFirst();
                if (!outSpec.isPresent()) {
                    outSpec = Arrays.stream(inNetworkSpec.getIntermediateOutputSpecs())
                        .filter(ods -> ods.getName().equals(outputCfg.getOutputLayerDataName())).findFirst();
                    if (!outSpec.isPresent()) {
                        throw new InvalidSettingsException("Selected output '" + outputCfg.getOutputLayerDataName()
                            + "' could not be found in the input deep learning network.");
                    }
                }
                if (!outSpec.get().hasShape()) {
                    throw new InvalidSettingsException("Selected output '" + outputCfg.getOutputLayerDataName()
                        + "' has an unknown shape. This is not supported.");
                }
                final long[] shape = outSpec.get().getShape();
                if (shape.length > 1) {
                    throw new InvalidSettingsException("Selected output '" + outputCfg.getOutputLayerDataName()
                        + "' has a shape with dimensionality > 1. This is not yet supported.");
                }

                final int count = converterFactory.getDestCount(outSpec.get());
                final String prefix = outputCfg.getPrefixModel().getStringValue();
                for (int i = 0; i < count; i++) {
                    appendColumnSpecs.add(new DataColumnSpecCreator(prefix + Integer.toString(i),
                        DataType.getType(converterFactory.getDestType())).createSpec());
                }
            }
        }
        return appendColumnSpecs.toArray(new DataColumnSpec[appendColumnSpecs.size()]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        if (!(inObjects[IN_NETWORK_PORT_IDX] instanceof DLNetworkPortObject)) {
            throw new RuntimeException("Unsupported deep learning network type at input port.");
        }
        final DLNetworkPortObject portObject = (DLNetworkPortObject)inObjects[IN_NETWORK_PORT_IDX];
        final DLNetwork network = portObject.getNetwork();
        final DLNetworkSpec networkSpec = portObject.getNetwork().getSpec();
        final BufferedDataTable inData = (BufferedDataTable)inObjects[IN_DATA_PORT_IDX];
        final DataTableSpec inDataSpec = inData.getDataTableSpec();

        final DLBackend backend = DLBackendRegistry.getBackend(m_generalCfg.getBackendModel().getStringValue())
            .orElseThrow(() -> new InvalidSettingsException(
                "Selected back end '" + m_generalCfg.getBackendModel().getStringValue() + "' could not be found."));

        // only int for now
        final int batchSize = Math.min(m_generalCfg.getBatchSizeModel().getIntValue(), (int)inData.size());

        final DLExecutableNetwork executableNetwork = backend.toExecutableNetwork(network);
        final HashMap<DLLayerDataSpec, DLDataValueToLayerConverterFactory<?, ?>> inputLayerDataConverters =
            new HashMap<>();

        // handle inputs
        final List<Pair<DLLayerDataSpec, int[]>> inputs = new ArrayList<>(networkSpec.getInputSpecs().length);
        for (final DLLayerDataSpec layerName : networkSpec.getInputSpecs()) {

            final DLInputLayerDataModelConfig inputCfg = m_inputCfgs.get(layerName.getName());
            if (inputCfg == null) {
                new InvalidSettingsException("Unexpected network input '" + layerName + "'.");
            }

            for (final DLLayerDataSpec candidate : networkSpec.getInputSpecs()) {
                if (candidate.getName().equals(layerName.getName())) {
                    final List<Integer> inColIndices =
                        Arrays.stream(inputCfg.getInputColumnsModel().applyTo(inDataSpec).getIncludes()).map(cn -> {
                            final int idx = inDataSpec.findColumnIndex(cn);
                            if (idx == -1) {
                                throw new IllegalStateException(
                                    "Selected input column '" + cn + "' could not be found in the input table.");
                            }
                            return idx;
                        }).collect(Collectors.toList());
                    final int[] indices = new int[inColIndices.size()];
                    for (int i = 0; i < indices.length; i++) {
                        indices[i] = inColIndices.get(i);
                    }
                    inputs.add(new Pair<>(candidate, indices));
                    inputLayerDataConverters.put(candidate, DLDataValueToLayerDataConverterRegistry.getInstance()
                        .getConverterFactory(inputCfg.getConverterModel().getStringValue()));
                    break;
                }
            }
        }

        // Handle outputs
        final String[] orderedNames = m_outputOrder.getStringArrayValue();
        final List<DLLayerDataSpec> orderedOutputs = new ArrayList<>();
        // TODO more efficient...
        outer: for (final String name : orderedNames) {
            for (final DLLayerDataSpec out : networkSpec.getOutputSpecs()) {
                if (out.getName().equals(name)) {
                    orderedOutputs.add(out);
                    continue outer;
                }
            }
            for (final DLLayerDataSpec out : networkSpec.getIntermediateOutputSpecs()) {
                if (out.getName().equals(name)) {
                    orderedOutputs.add(out);
                    break;
                }
            }
        }

        final HashMap<DLLayerDataSpec, DLLayerDataToDataCellConverterFactory<?, ?>> outputLayerDataConverters =
            new HashMap<>();
        for (final DLOutputLayerDataModelConfig outputCfg : m_outputCfgs.values()) {

            Optional<DLLayerDataSpec> outputSpecOptional = Arrays.stream(networkSpec.getOutputSpecs())
                .filter(ods -> ods.getName().equals(outputCfg.getOutputLayerDataName())).findFirst();
            if (!outputSpecOptional.isPresent()) {
                outputSpecOptional = Arrays.stream(networkSpec.getIntermediateOutputSpecs())
                    .filter(ods -> ods.getName().equals(outputCfg.getOutputLayerDataName())).findFirst();
                if (!outputSpecOptional.isPresent()) {
                    throw new InvalidSettingsException("Selected output '" + outputCfg.getOutputLayerDataName()
                        + "' could not be found in the input deep learning network.");
                }
            }
            final DLLayerDataSpec outputSpec = outputSpecOptional.get();
            final String converterId = outputCfg.getConverterModel().getStringValue();
            final DLLayerDataToDataCellConverterFactory<?, ? extends DataCell> converter =
                DLLayerDataToDataCellConverterRegistry.getInstance().getConverterFactory(converterId)
                    .orElseThrow(() -> new InvalidSettingsException("Selected converter '" + converterId
                        + "' for output '" + outputSpec.getName() + "' could not be created."));
            outputLayerDataConverters.put(outputSpec, converter);
        }

        final boolean appendResults = m_generalCfg.getAppendColumns().getBooleanValue();

        final DataColumnSpec[] outColumnSpecs = createOutputSpec(networkSpec);
        DataTableSpec outDataSpec = new DataTableSpec(outColumnSpecs);
        if (appendResults) {
            outDataSpec = new DataTableSpec(inDataSpec, outDataSpec);
        }

        final BufferedDataContainer outData = exec.createDataContainer(outDataSpec);

        try (DLFromKnimeNetworkExecutor executor =
            new DLFromKnimeNetworkExecutor(executableNetwork, inputLayerDataConverters, outputLayerDataConverters)) {

            final DLNetworkExecutorOutputConsumer networkOutputConsumer =
                new DLNetworkExecutorOutputConsumer(orderedOutputs);

            long currRowIdx = 0;
            final List<DataRow> batch = new ArrayList<>();
            try {
                for (final DataRow row : inData) {
                    // collecting our batch our batch
                    batch.add(row);
                    currRowIdx++;

                    if (currRowIdx % batchSize == 0 || currRowIdx == inData.size()) {

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

                        executor.execute(temp, networkOutputConsumer, exec, batch.size());

                        for (final List<DataValue>[] layer : temp.values()) {
                            for (final List<DataValue> list : layer) {
                                list.clear();
                            }
                        }

                        final DataCell[][] cells = networkOutputConsumer.collected();
                        if (appendResults) {
                            for (int j = 0; j < batch.size(); j++) {
                                outData.addRowToTable(new AppendedColumnRow(batch.get(j), cells[j]));
                            }
                        } else {
                            for (int j = 0; j < batch.size(); j++) {
                                outData.addRowToTable(new DefaultRow(batch.get(j).getKey(), cells[j]));
                            }
                        }

                        exec.setProgress((double)currRowIdx / inData.size(),
                            "Processing row " + row.getKey().toString() + "...");

                        batch.clear();
                    }
                }
            } catch (final Exception e) {
                executor.close();
                // TODO better exception handling?
                LOGGER.debug("Error occured during execution of network model", e);
                throw new RuntimeException(e);
            }
        }
        outData.close();

        return new PortObject[]{outData.getTable()};
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
        NodeSettings tmp = new NodeSettings("tmp");
        for (final DLInputLayerDataModelConfig inputCfg : m_inputCfgs.values()) {
            final NodeSettings child = new NodeSettings(inputCfg.getInputLayerDataName());
            inputCfg.saveToSettings(child);
            tmp.addNodeSettings(child);
        }
        saveAsBytesArray(tmp, inputSettings, CFG_KEY_INPUT_ARRAY);

        final NodeSettingsWO outputSettings = settings.addNodeSettings(CFG_KEY_OUTPUTS);
        tmp = new NodeSettings("tmp");
        for (final DLOutputLayerDataModelConfig outputCfg : m_outputCfgs.values()) {
            final NodeSettings child = new NodeSettings(outputCfg.getOutputLayerDataName());
            outputCfg.saveToSettings(child);
            tmp.addNodeSettings(child);
        }
        saveAsBytesArray(tmp, outputSettings, CFG_KEY_OUTPUTS_ARRAY);

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

        final NodeSettingsRO inputSettings = settings.getNodeSettings(CFG_KEY_INPUTS);
        NodeSettings tmp = loadFromBytesArray(inputSettings, CFG_KEY_INPUT_ARRAY);
        if (m_inputCfgs.size() == 0) {
            for (final String layerName : tmp) {
                m_inputCfgs.put(layerName, createInputLayerDataModelConfig(layerName, m_generalCfg.getBackendModel()));
                m_inputCfgs.get(layerName).validateSettings(tmp.getNodeSettings(layerName));
            }
        }

        m_outputCfgs.clear();
        final NodeSettingsRO outputSettings = settings.getNodeSettings(CFG_KEY_OUTPUTS);
        tmp = loadFromBytesArray(outputSettings, CFG_KEY_OUTPUTS_ARRAY);
        for (final String key : tmp) {
            final DLOutputLayerDataModelConfig outputCfg =
                createOutputLayerDataModelConfig(key, m_generalCfg.getBackendModel());
            m_outputCfgs.put(key, outputCfg);
            outputCfg.validateSettings(tmp.getNodeSettings(key));
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
        NodeSettings tmp = loadFromBytesArray(inputSettings, CFG_KEY_INPUT_ARRAY);
        for (final DLInputLayerDataModelConfig inputCfg : m_inputCfgs.values()) {
            inputCfg.loadFromSettings(tmp.getNodeSettings(inputCfg.getInputLayerDataName()));
        }

        final NodeSettingsRO outputSettings = settings.getNodeSettings(CFG_KEY_OUTPUTS);
        tmp = loadFromBytesArray(outputSettings, CFG_KEY_OUTPUTS_ARRAY);
        for (final DLOutputLayerDataModelConfig outputCfg : m_outputCfgs.values()) {
            outputCfg.loadFromSettings(tmp.getNodeSettings(outputCfg.getOutputLayerDataName()));
        }
        m_outputOrder.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // no op a.t.m. - this might change as we're supporting more back ends,
        // TODO
    }

    static void saveAsBytesArray(final NodeSettings tmp, final NodeSettingsWO outputSettings, final String key) {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(bytes)) {
            tmp.writeToFile(oos);
            outputSettings.addByteArray(key, bytes.toByteArray());
        } catch (final IOException e) {
            // Noop
        }
    }

    static NodeSettings loadFromBytesArray(final NodeSettingsRO inputSettings, final String key)
        throws InvalidSettingsException {
        final byte[] array = inputSettings.getByteArray(key);
        try (ByteArrayInputStream bytes = new ByteArrayInputStream(array);
                ObjectInputStream stream = new ObjectInputStream(bytes)) {
            return NodeSettings.readFromFile(stream);
        } catch (final IOException e) {
            throw new InvalidSettingsException(e);
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
