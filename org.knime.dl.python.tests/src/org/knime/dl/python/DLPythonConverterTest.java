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
 *   Jun 28, 2017 (marcel): created
 */
package org.knime.dl.python;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataValue;
import org.knime.dl.core.DLCanceledExecutionException;
import org.knime.dl.core.DLDefaultFixedTensorShape;
import org.knime.dl.core.DLDefaultTensorId;
import org.knime.dl.core.DLDefaultTensorSpec;
import org.knime.dl.core.DLDimensionOrder;
import org.knime.dl.core.DLInvalidNetworkInputException;
import org.knime.dl.core.DLInvalidNetworkOutputException;
import org.knime.dl.core.DLNetworkInputPreparer;
import org.knime.dl.core.DLTensor;
import org.knime.dl.core.DLTensorId;
import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.core.data.DLReadableBuffer;
import org.knime.dl.core.data.DLWritableBuffer;
import org.knime.dl.core.data.convert.DLDataValueToTensorConverter;
import org.knime.dl.core.data.convert.DLDataValueToTensorConverterFactory;
import org.knime.dl.core.data.convert.DLDataValueToTensorConverterRegistry;
import org.knime.dl.core.data.convert.DLTensorToDataCellConverter;
import org.knime.dl.core.data.convert.DLTensorToDataCellConverterFactory;
import org.knime.dl.core.data.convert.DLTensorToDataCellConverterRegistry;
import org.knime.dl.core.execution.DLNetworkExecutionSession;
import org.knime.dl.core.execution.DLNetworkOutputConsumer;
import org.knime.dl.testing.DLTestExecutionMonitor;
import org.knime.dl.testing.backend.DLTestingBackendExecutionContext;
import org.knime.dl.testing.backend.DLTestingBackendFactories.DLDoubleBufferToDoubleDataCellDataCellConverterFactory;
import org.knime.dl.testing.backend.DLTestingBackendFactories.DLTestingDataValueToFloatTensorConverterFactory;
import org.knime.dl.testing.backend.DLTestingBackendNetwork;
import org.knime.dl.testing.backend.DLTestingBackendNetworkSpec;
import org.knime.dl.testing.backend.DLTestingDataCells.DoubleDataCell;
import org.knime.dl.testing.backend.DLTestingDataCells.FloatDataCell;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public class DLPythonConverterTest {

    @Test
    public void testFooToBar() throws Exception {
        // register converters:
        DLDataValueToTensorConverterRegistry.getInstance()
        .registerConverter(new DLTestingDataValueToFloatTensorConverterFactory());
        DLTensorToDataCellConverterRegistry.getInstance()
        .registerConverter(new DLDoubleBufferToDoubleDataCellDataCellConverterFactory());

        // network:

        final DLTensorSpec[] inputSpecs = new DLTensorSpec[1];
        inputSpecs[0] = new DLDefaultTensorSpec(new DLDefaultTensorId("in0"), "in0", 1,
            new DLDefaultFixedTensorShape(new long[] { 10, 10 }), float.class, DLDimensionOrder.TDHWC);
        // intermediate outputs stay empty
        final DLTensorSpec[] intermediateOutputSpecs = new DLTensorSpec[0];
        final DLTensorSpec[] outputSpecs = new DLTensorSpec[1];
        outputSpecs[0] = new DLDefaultTensorSpec(new DLDefaultTensorId("out0"), "out0", 1,
            new DLDefaultFixedTensorShape(new long[] { 10, 10 }), double.class, DLDimensionOrder.TDHWC);
        final DLTestingBackendNetworkSpec networkSpec =
                new DLTestingBackendNetworkSpec(inputSpecs, intermediateOutputSpecs, outputSpecs);
        final DLTestingBackendNetwork network = new DLTestingBackendNetwork(networkSpec);

        // input data:

        final Random rng = new Random(543653);
        final HashMap<String, DataCell[]> inputCells = new HashMap<>(network.getSpec().getInputSpecs().length);
        final FloatDataCell[] input0Cells = new FloatDataCell[1];
        for (int i = 0; i < input0Cells.length; i++) {
            final float[] arr = new float[10 * 10];
            for (int j = 0; j < arr.length; j++) {
                arr[j] = rng.nextFloat() * rng.nextInt(Short.MAX_VALUE);
            }
            input0Cells[i] = new FloatDataCell(arr);
        }
        inputCells.put(network.getSpec().getInputSpecs()[0].getName(), input0Cells);

        // "configure":

        final DLTestingBackendExecutionContext ctx = new DLTestingBackendExecutionContext();

        // input converters:
        final LinkedHashSet<DLTensorSpec> executionInputSpecs = new LinkedHashSet<>();
        final HashMap<DLTensorId, DLDataValueToTensorConverterFactory<?, ?>> inputConverters = new HashMap<>(
                networkSpec.getInputSpecs().length);
        for (final DLTensorSpec inputSpec : networkSpec.getInputSpecs()) {
            executionInputSpecs.add(inputSpec);
            final DLDataValueToTensorConverterFactory<?, ?> converter = DLDataValueToTensorConverterRegistry
                    .getInstance().getPreferredConverterFactory(FloatDataCell.TYPE,
                        ctx.getTensorFactory().getWritableBufferType(inputSpec))
                    .get();
            inputConverters.put(inputSpec.getIdentifier(), converter);
        }
        // output converters:
        final Map<DLTensorId, DLTensorToDataCellConverterFactory<?, ?>> outputConverters = new HashMap<>(
                networkSpec.getOutputSpecs().length + networkSpec.getHiddenOutputSpecs().length);
        for (final DLTensorSpec outputSpec : networkSpec.getOutputSpecs()) {
            final DLTensorToDataCellConverterFactory<?, ?> converter = DLTensorToDataCellConverterRegistry.getInstance()
                    .getFactoriesForSourceType(ctx.getTensorFactory().getReadableBufferType(outputSpec), outputSpec)
                    .stream().filter(c -> {
                        return c.getDestType().getCellClass() == DoubleDataCell.class;
                    }).findFirst().get();
            outputConverters.put(outputSpec.getIdentifier(), converter);
        }
        for (final DLTensorSpec outputSpec : networkSpec.getHiddenOutputSpecs()) {
            final DLTensorToDataCellConverterFactory<?, ?> converter = DLTensorToDataCellConverterRegistry.getInstance()
                    .getFactoriesForSourceType(ctx.getTensorFactory().getReadableBufferType(outputSpec), outputSpec)
                    .stream().filter(c -> c.getDestType().equals(DoubleDataCell.class)).findFirst().get();
            outputConverters.put(outputSpec.getIdentifier(), converter);
        }

        // "execute":

        // assign inputs to 'network input ports'/specs:
        final Map<DLTensorId, Iterable<DataValue>> inputs = new HashMap<>(inputConverters.size());
        for (final Entry<String, DataCell[]> input : inputCells.entrySet()) {
            final Optional<DLTensorSpec> inputSpec = Arrays.stream(network.getSpec().getInputSpecs())
                    .filter(i -> i.getName().equals(input.getKey())).findFirst();
            final List<DataValue> val = Arrays.asList(input.getValue());
            inputs.put(inputSpec.get().getIdentifier(), val);
        }

        // pre-allocate output map
        final HashMap<DLTensorId, DataCell[]> outputs = new HashMap<>(outputConverters.size());

        try (final DLNetworkExecutionSession session = ctx.createExecutionSession(null, network,
            executionInputSpecs, outputConverters.keySet(), new DLNetworkInputPreparer() {
                private boolean hasNext = true;

                @Override
                public boolean hasNext() {
                    return hasNext;
                };

                @Override
                public void prepareNext(Map<DLTensorId, DLTensor<? extends DLWritableBuffer>> input)
                    throws DLCanceledExecutionException, DLInvalidNetworkInputException {
                    for (Entry<DLTensorId, DLTensor<? extends DLWritableBuffer>> entry : input.entrySet()) {
                        final DLTensorId identifier = entry.getKey();
                        final DLTensor<? extends DLWritableBuffer> tensor = entry.getValue();
                        final DLDataValueToTensorConverter converter =
                            inputConverters.get(identifier).createConverter();
                        converter.convert(inputs.get(identifier), tensor);
                    }
                    hasNext = false;
                }

            @Override
            public void close() throws Exception {
                // no op
            }
        }, new DLNetworkOutputConsumer() {

            @Override
            public void accept(final Map<DLTensorId, DLTensor<? extends DLReadableBuffer>> output)
                    throws DLCanceledExecutionException, DLInvalidNetworkOutputException {
                for (final Entry<DLTensorId, DLTensor<? extends DLReadableBuffer>> entry : output.entrySet()) {
                    DLTensorId identifier = entry.getKey();
                    DLTensor<? extends DLReadableBuffer> tensor = entry.getValue();
                    DLTensorToDataCellConverter converter = outputConverters.get(identifier).createConverter();
                    DataCell[] dataCells = outputs.computeIfAbsent(entry.getKey(), k -> new DataCell[1]);
                    converter.convert(tensor, dataCells, null);
                }

                // check if conversion succeeded
                for (final Entry<DLTensorId, DLTensorToDataCellConverterFactory<?, ?>> outputSpecPair : outputConverters
                        .entrySet()) {
                    final Iterable<DataValue> inputsForSpec = inputs.get(networkSpec.getInputSpecs()[0]);
                    final DataCell[] outputsForSpec = outputs.get(outputSpecPair.getKey());
                    int i = 0;
                    for (final DataValue input : inputsForSpec) {
                        final DataCell o = outputsForSpec[i++];
                            final float[] in = ((FloatDataCell)input).getFloatArray();
                            Assert.assertTrue(o instanceof DoubleDataCell);
                            final double[] out = ((DoubleDataCell)o).getDoubleArray();
                        for (int j = 0; j < out.length; j++) {
                            Assert.assertEquals(out[j], in[j] * 5.0, 0.0);
                        }
                    }
                }
            }

            @Override
            public void close() throws Exception {
                // no op
            }
        })) {
            session.run(new DLTestExecutionMonitor());
        }
    }
}
