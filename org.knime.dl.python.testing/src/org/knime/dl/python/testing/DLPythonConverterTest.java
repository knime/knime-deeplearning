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
 *   Jun 28, 2017 (marcel): created
 */
package org.knime.dl.python.testing;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.node.ExecutionContext;
import org.knime.dl.core.DLAbstractExecutableNetworkSpec;
import org.knime.dl.core.DLAbstractNetworkSpec;
import org.knime.dl.core.DLDefaultFixedLayerDataShape;
import org.knime.dl.core.DLDefaultLayerData;
import org.knime.dl.core.DLDefaultLayerDataSpec;
import org.knime.dl.core.DLExecutableNetwork;
import org.knime.dl.core.DLExecutableNetworkSpec;
import org.knime.dl.core.DLLayerData;
import org.knime.dl.core.DLLayerDataSpec;
import org.knime.dl.core.DLNetwork;
import org.knime.dl.core.DLNetworkSpec;
import org.knime.dl.core.backend.DLBackend;
import org.knime.dl.core.data.DLReadableBuffer;
import org.knime.dl.core.data.DLReadableDoubleBuffer;
import org.knime.dl.core.data.DLReadableFloatBuffer;
import org.knime.dl.core.data.convert.input.DLDataValueToLayerDataConverter;
import org.knime.dl.core.data.convert.input.DLDataValueToLayerDataConverterFactory;
import org.knime.dl.core.data.convert.input.DLDataValueToLayerDataConverterRegistry;
import org.knime.dl.core.data.convert.output.DLLayerDataToDataCellConverter;
import org.knime.dl.core.data.convert.output.DLLayerDataToDataCellConverterFactory;
import org.knime.dl.core.data.convert.output.DLLayerDataToDataCellConverterRegistry;
import org.knime.dl.core.data.writables.DLWritableBuffer;
import org.knime.dl.core.data.writables.DLWritableDoubleBuffer;
import org.knime.dl.core.data.writables.DLWritableFloatBuffer;
import org.knime.dl.core.execution.DLDefaultLayerDataInput;
import org.knime.dl.core.execution.DLDefaultLayerDataOutput;
import org.knime.dl.core.execution.DLFromKnimeNetworkExecutor;
import org.knime.dl.core.execution.DLLayerDataInput;
import org.knime.dl.core.execution.DLLayerDataOutput;
import org.knime.dl.core.execution.DLNetworkExecutor;
import org.knime.dl.core.io.DLNetworkReader;
import org.knime.dl.python.core.data.DLPythonDataBuffer;
import org.knime.dl.python.core.data.DLPythonDoubleBuffer;
import org.knime.dl.python.core.data.DLPythonFloatBuffer;
import org.knime.dl.python.core.data.DLPythonIntBuffer;
import org.knime.dl.python.core.data.DLPythonLongBuffer;
import org.knime.dl.util.DLUtils;

/**
 *
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
public class DLPythonConverterTest {

    @Test
    public void testFooToBar() throws Exception {
        // converters:
        DLDataValueToLayerDataConverterRegistry.getInstance()
            .registerConverter(new DLFooDataValueToFloatLayerConverterFactory());
        DLLayerDataToDataCellConverterRegistry.getInstance()
            .registerConverter(new DLDoubleBufferToBarDataCellConverterFactory());

        // network:

        final DLBazBackend backend = new DLBazBackend();
        final DLNetwork network = backend.createReader().readNetwork(null /* dummy */);
        final DLExecutableBazNetwork execNetwork = backend.toExecutableNetwork(network);
        final DLExecutableNetworkSpec execNetworkSpec = execNetwork.getSpec();

        // input data:

        final Random rng = new Random(543653);
        final HashMap<String, DataCell[]> inputCells = new HashMap<>(network.getSpec().getInputSpecs().length);
        final FooDataCell[] input0Cells = new FooDataCell[1];
        for (int i = 0; i < input0Cells.length; i++) {
            final float[] arr = new float[10 * 10];
            for (int j = 0; j < arr.length; j++) {
                arr[j] = rng.nextFloat() * rng.nextInt(Short.MAX_VALUE);
            }
            input0Cells[i] = new FooDataCell(arr);
        }
        inputCells.put(network.getSpec().getInputSpecs()[0].getName(), input0Cells);

        // "configure":

        // input converters:
        final HashMap<DLLayerDataSpec, DLDataValueToLayerDataConverterFactory<?, ?>> inputConverters =
            new HashMap<>(execNetworkSpec.getInputSpecs().length);
        for (final DLLayerDataSpec inputSpec : execNetworkSpec.getInputSpecs()) {
            final DLDataValueToLayerDataConverterFactory<?, ?> converter =
                DLDataValueToLayerDataConverterRegistry.getInstance()
                    .getPreferredConverterFactory(FooDataCell.TYPE, backend.getWritableBufferType(inputSpec)).get();
            inputConverters.put(inputSpec, converter);
        }
        // output converters:
        final Map<DLLayerDataSpec, DLLayerDataToDataCellConverterFactory<?, ?>> outputConverters = new HashMap<>(
            execNetworkSpec.getOutputSpecs().length + execNetworkSpec.getIntermediateOutputSpecs().length);
        for (final DLLayerDataSpec outputSpec : execNetworkSpec.getOutputSpecs()) {
            final DLLayerDataToDataCellConverterFactory<?, ?> converter = DLLayerDataToDataCellConverterRegistry
                .getInstance().getConverterFactories(backend.getReadableBufferType(outputSpec)).stream()
                .filter(c -> c.getDestType().equals(BarDataCell.class)).findFirst().get();
            outputConverters.put(outputSpec, converter);
        }
        for (final DLLayerDataSpec outputSpec : execNetworkSpec.getIntermediateOutputSpecs()) {
            final DLLayerDataToDataCellConverterFactory<?, ?> converter = DLLayerDataToDataCellConverterRegistry
                .getInstance().getConverterFactories(backend.getReadableBufferType(outputSpec)).stream()
                .filter(c -> c.getDestType().equals(BarDataCell.class)).findFirst().get();
            outputConverters.put(outputSpec, converter);
        }

        final DLFromKnimeNetworkExecutor knimeExec =
            new DLFromKnimeNetworkExecutor(execNetwork, inputConverters, outputConverters);

        // "execute":

        // assign inputs to 'network input ports'/specs:
        final Map<DLLayerDataSpec, Iterable<DataValue>[]> inputs = new HashMap<>(inputConverters.size());
        for (final Entry<String, DataCell[]> input : inputCells.entrySet()) {
            final Optional<DLLayerDataSpec> inputSpec = Arrays.stream(network.getSpec().getInputSpecs())
                .filter(i -> i.getName().equals(input.getKey())).findFirst();
            final List<DataCell> val = Arrays.asList(input.getValue());
            inputs.put(inputSpec.get(), new Iterable[]{val});
        }

        final HashMap<DLLayerDataSpec, DataCell[]> outputs = new HashMap<>(outputConverters.size());

        knimeExec.execute(inputs, new Consumer<Map<DLLayerDataSpec, DataCell[][]>>() {

            @Override
            public void accept(final Map<DLLayerDataSpec, DataCell[][]> output) {
                for (final Entry<DLLayerDataSpec, DataCell[][]> o : output.entrySet()) {
                    DataCell[] dataCells = outputs.get(o.getKey());
                    if (dataCells == null) {
                        dataCells = o.getValue()[0];
                    } else {
                        dataCells = ArrayUtils.addAll(dataCells, o.getValue()[0]);
                    }
                    outputs.put(o.getKey(), dataCells);
                }
            }
        }, null, 1);

        // check if conversion succeeded:
        Assert.assertEquals(outputs.size(), outputConverters.size());
        for (final Entry<DLLayerDataSpec, DLLayerDataToDataCellConverterFactory<?, ?>> outputSpecPair : outputConverters
            .entrySet()) {
            final Iterable<DataValue> inputsForSpec = inputs.get(execNetworkSpec.getInputSpecs()[0])[0];
            final DataCell[] outputsForSpec = outputs.get(outputSpecPair.getKey());
            int i = 0;
            for (final DataValue input : inputsForSpec) {
                final DataCell output = outputsForSpec[i++];
                final float[] in = ((FooDataCell)input).getFloatArray();
                Assert.assertTrue(output instanceof BarDataCell);
                final double[] out = ((BarDataCell)output).getDoubleArray();
                for (int j = 0; j < out.length; j++) {
                    Assert.assertEquals(out[j], in[j] * 5.0, 0.0);
                }
            }
        }
    }

    private static class DLBazNetwork implements DLNetwork {

        private final DLNetworkSpec m_spec;

        private DLBazNetwork(final DLNetworkSpec spec) {
            m_spec = spec;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DLNetworkSpec getSpec() {
            return m_spec;
        }
    }

    // our network wants floats and returns doubles
    private static class DLExecutableBazNetwork implements DLExecutableNetwork {

        private final DLBazBackend m_backend;

        private final DLExecutableNetworkSpec m_spec;

        private final HashMap<DLLayerDataSpec, DLLayerDataInput<?>> m_inputs;

        private final HashMap<DLLayerDataSpec, DLLayerDataOutput<?>> m_outputs;

        private DLBazNetworkExecutor m_executor;

        private DLExecutableBazNetwork(final DLBazBackend backend, final DLExecutableNetworkSpec spec) {
            m_backend = backend;
            m_spec = spec;
            m_inputs = new HashMap<>(spec.getInputSpecs().length);
            m_outputs = new HashMap<>(spec.getOutputSpecs().length + spec.getIntermediateOutputSpecs().length);
        }

        @Override
        public DLBazBackend getBackend() {
            return m_backend;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DLExecutableNetworkSpec getSpec() {
            return m_spec;
        }

        @Override
        public DLLayerDataInput<?> getInputForSpec(final DLLayerDataSpec spec, final int batchSize) {
            DLLayerDataInput<?> input = m_inputs.get(spec);
            if (input == null || input.getBatchSize() != batchSize) {
                final DLLayerData[] batch = new DLLayerData[batchSize];
                for (int i = 0; i < batchSize; i++) {
                    batch[i] = getBackend().createLayerData(spec);
                }
                input = new DLDefaultLayerDataInput<>(batch);
                m_inputs.put(spec, input);
            }
            return input;
        }

        private DLLayerDataOutput<?> getOutputForSpec(final DLLayerDataSpec spec, final int batchSize) {
            DLLayerDataOutput<?> output = m_outputs.get(spec);
            if (output == null || output.getBatchSize() != batchSize) {
                final DLLayerData[] batch = new DLLayerData[batchSize];
                for (int i = 0; i < batchSize; i++) {
                    batch[i] = getBackend().createLayerData(spec);
                }
                output = new DLDefaultLayerDataOutput(batch);
                m_outputs.put(spec, output);
            }
            return output;
        }

        @Override
        public void execute(final Set<DLLayerDataSpec> selectedOutputs,
            final Consumer<Map<DLLayerDataSpec, DLLayerDataOutput<?>>> outputConsumer)
            throws RuntimeException, IllegalStateException {
            if (m_executor == null) {
                m_executor = new DLBazNetworkExecutor(getBackend());
            }
            // TODO: assert that all inputs are populated
            final int outputBatchSize = (int)m_inputs.values().stream().findFirst().get().getBatchSize();
            final HashMap<DLLayerDataSpec, DLLayerDataOutput<?>> outputs = new HashMap<>(selectedOutputs.size());
            for (final DLLayerDataSpec output : selectedOutputs) {
                outputs.put(output, getOutputForSpec(output, outputBatchSize));
            }
            m_executor.execute(this, m_inputs, outputs);
            outputConsumer.accept(outputs);
        }

        @Override
        public void close() throws Exception {
            if (m_executor != null) {
                m_executor.close();
                m_executor = null;
            }
        }
    }

    private static class DLBazBackend implements DLBackend {

        /**
         * {@inheritDoc}
         */
        @Override
        public String getName() {
            return "Baz";
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String getIdentifier() {
            return "org.knime.dl.python.testing.DLPythonConverterTest.Baz";
        }

        @Override
        public DLLayerData<DLPythonDataBuffer<?>> createLayerData(final DLLayerDataSpec spec)
            throws IllegalArgumentException {
            final long[] shape =
                DLUtils.Shapes.getFixedShape(spec.getShape()).orElseThrow(() -> new IllegalArgumentException(
                    "Layer data spec does not provide a shape. Layer data cannot be created."));
            DLPythonDataBuffer<?> data;
            final Class<?> t = spec.getElementType();
            final long size = DLUtils.Shapes.getSize(shape);
            if (t.equals(double.class)) {
                data = new DLPythonDoubleBuffer(size);
            } else if (t.equals(float.class)) {
                data = new DLPythonFloatBuffer(size);
            } else if (t.equals(int.class)) {
                data = new DLPythonIntBuffer(size);
            } else if (t.equals(long.class)) {
                data = new DLPythonLongBuffer(size);
            } else {
                throw new IllegalArgumentException("No matching layer data type.");
            }
            return new DLDefaultLayerData<>(spec, data);
        }

        @Override
        public Class<? extends DLReadableBuffer> getReadableBufferType(final DLLayerDataSpec spec) {
            final Class<?> t = spec.getElementType();
            if (t.equals(double.class)) {
                return DLReadableDoubleBuffer.class;
            } else if (t.equals(float.class)) {
                return DLReadableFloatBuffer.class;
            } else {
                throw new IllegalArgumentException("No matching buffer type.");
            }
        }

        @Override
        public Class<? extends DLWritableBuffer> getWritableBufferType(final DLLayerDataSpec spec) {
            final Class<?> t = spec.getElementType();
            if (t.equals(double.class)) {
                return DLWritableDoubleBuffer.class;
            } else if (t.equals(float.class)) {
                return DLWritableFloatBuffer.class;
            } else {
                throw new IllegalArgumentException("No matching buffer type.");
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DLNetworkReader<?> createReader() {
            return new DLBazNetworkReader(this);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DLNetworkExecutor<?> createExecutor() throws Exception {
            return new DLBazNetworkExecutor(this);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DLExecutableBazNetwork toExecutableNetwork(final DLNetwork network) {
            if (!(network instanceof DLBazNetwork)) {
                throw new IllegalArgumentException("Input must be a Keras network.");
            }
            final DLNetworkSpec networkSpec = network.getSpec();
            final DLExecutableNetworkSpec execNetworkSpec = new DLAbstractExecutableNetworkSpec(
                networkSpec.getInputSpecs(), networkSpec.getIntermediateOutputSpecs(), networkSpec.getOutputSpecs()) {
            };
            return new DLExecutableBazNetwork(this, execNetworkSpec);
        }
    }

    private static class DLBazNetworkReader implements DLNetworkReader<DLBazNetwork> {

        private DLBazNetworkReader(final DLBazBackend backend) {
            // dummy
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DLBazNetwork readNetwork(final URL source) throws IllegalArgumentException, IOException {
            final DLLayerDataSpec[] inputSpecs = new DLLayerDataSpec[1];
            inputSpecs[0] =
                new DLDefaultLayerDataSpec("in0", new DLDefaultFixedLayerDataShape(new long[]{10, 10}), float.class);
            final DLLayerDataSpec[] intermediateSpecs = new DLLayerDataSpec[0];
            // intermediate stays empty
            final DLLayerDataSpec[] outputSpecs = new DLLayerDataSpec[1];
            outputSpecs[0] =
                new DLDefaultLayerDataSpec("out0", new DLDefaultFixedLayerDataShape(new long[]{10, 10}), double.class);
            final DLNetworkSpec spec = new DLAbstractNetworkSpec(inputSpecs, intermediateSpecs, outputSpecs) {

                @Override
                protected void hashCodeInternal(final HashCodeBuilder b) {
                    // no op
                }

                @Override
                protected boolean equalsInternal(final DLNetworkSpec other) {
                    // no op
                    return true;
                }
            };
            return new DLBazNetwork(spec);
        }
    }

    private static class DLBazNetworkExecutor implements DLNetworkExecutor<DLExecutableBazNetwork> {

        private final DLBazBackend m_backend;

        private DLBazNetworkExecutor(final DLBazBackend backend) {
            m_backend = backend;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void execute(final DLExecutableBazNetwork network,
            final Map<DLLayerDataSpec, DLLayerDataInput<?>> inputs,
            final Map<DLLayerDataSpec, DLLayerDataOutput<?>> outputs) throws RuntimeException {
            // we fake some network activity here: unwrap floats, calc some
            // stuff, create doubles
            for (final Entry<DLLayerDataSpec, DLLayerDataInput<?>> input : inputs.entrySet()) {

                // TODO: replace these old tests (e.g. check buffer type vs.
                // buffer type from spec (via backend)):
                // Assert.assertEquals(input.getKey().getElementType(),
                // input.getValue().getData().getElementType());
                // Assert.assertEquals(input.getKey().getElementType(),
                // float.class);
                // Assert.assertTrue(input.getValue().getData() instanceof
                // DLFloatBuffer);

                final DLPythonFloatBuffer buffer = (DLPythonFloatBuffer)input.getValue().getBatch()[0].getBuffer();
                final float[] inArr = buffer.getStorageForReading(0, buffer.size());
                final double[] outArr = new double[inArr.length];
                for (int i = 0; i < inArr.length; i++) {
                    outArr[i] = inArr[i] * 5.0;
                }
                final DLLayerDataSpec outSpec = outputs.keySet().stream().findFirst().get();
                ((DLWritableDoubleBuffer)outputs.get(outSpec).getBatch()[0].getBuffer()).putAll(outArr);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void close() throws Exception {
        }
    }

    public class DLFooDataValueToFloatLayerConverterFactory
        implements DLDataValueToLayerDataConverterFactory<FooDataValue, DLWritableFloatBuffer> {

        @Override
        public String getName() {
            return "FooDataValue to FloatBuffe";
        }

        @Override
        public Class<FooDataValue> getSourceType() {
            return FooDataValue.class;
        }

        @Override
        public Class<DLWritableFloatBuffer> getBufferType() {
            return DLWritableFloatBuffer.class;
        }

        @Override
        public DLDataValueToLayerDataConverter<FooDataValue, DLWritableFloatBuffer> createConverter() {
            return new DLDataValueToLayerDataConverter<FooDataValue, DLWritableFloatBuffer>() {

                @Override
                public void convert(final Iterable<FooDataValue> input,
                    final DLLayerData<DLWritableFloatBuffer> output) {
                    final DLWritableFloatBuffer buf = output.getBuffer();
                    buf.putAll(input.iterator().next().getFloatArray());
                }
            };
        }
    }

    public class DLDoubleBufferToBarDataCellConverterFactory
        implements DLLayerDataToDataCellConverterFactory<DLReadableDoubleBuffer, BarDataCell> {

        @Override
        public String getName() {
            return "To BarDataCell";
        }

        @Override
        public Class<DLReadableDoubleBuffer> getBufferType() {
            return DLReadableDoubleBuffer.class;
        }

        @Override
        public Class<BarDataCell> getDestType() {
            return BarDataCell.class;
        }

        @Override
        public DLLayerDataToDataCellConverter<DLReadableDoubleBuffer, BarDataCell> createConverter() {
            return new DLLayerDataToDataCellConverter<DLReadableDoubleBuffer, BarDataCell>() {

                @Override
                public void convert(final ExecutionContext exec, final DLLayerData<DLReadableDoubleBuffer> input,
                    final Consumer<BarDataCell> out) {
                    final DLReadableDoubleBuffer buf = input.getBuffer();
                    out.accept(new BarDataCell(buf.toDoubleArray()));
                }
            };
        }

        @Override
        public long getDestCount(final DLLayerDataSpec spec) {
            return 1;
        }
    }

    @SuppressWarnings("serial")
    private static class FooDataCell extends DataCell implements FooDataValue {

        private static final DataType TYPE = DataType.getType(FooDataCell.class);

        private final float[] m_floats;

        private FooDataCell(final float[] floats) {
            m_floats = floats;
        }

        @Override
        public float[] getFloatArray() {
            return m_floats;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return Arrays.toString(m_floats);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected boolean equalsDataCell(final DataCell dc) {
            return Arrays.equals(m_floats, ((FooDataCell)dc).m_floats);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return m_floats.hashCode();
        }
    }

    private static interface FooDataValue extends DataValue {
        float[] getFloatArray();
    }

    @SuppressWarnings("serial")
    private static class BarDataCell extends DataCell {

        private static final DataType TYPE = DataType.getType(BarDataCell.class);

        private final double[] m_doubles;

        private BarDataCell(final double[] doubles) {
            m_doubles = doubles;
        }

        private double[] getDoubleArray() {
            return m_doubles;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return Arrays.toString(m_doubles);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected boolean equalsDataCell(final DataCell dc) {
            return Arrays.equals(m_doubles, ((BarDataCell)dc).m_doubles);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return m_doubles.hashCode();
        }
    }
}
