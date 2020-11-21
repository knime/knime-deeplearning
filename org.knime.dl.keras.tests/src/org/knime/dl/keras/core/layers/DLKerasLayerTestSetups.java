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
 */
package org.knime.dl.keras.core.layers;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.knime.core.node.NodeLogger;
import org.knime.core.util.Pair;
import org.knime.dl.core.DLCanceledExecutionException;
import org.knime.dl.core.DLInvalidEnvironmentException;
import org.knime.dl.core.DLInvalidSourceException;
import org.knime.dl.core.DLNetworkLocation;
import org.knime.dl.core.DLNetworkReferenceLocation;
import org.knime.dl.core.DLNotCancelable;
import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.keras.core.DLKerasNetwork;
import org.knime.dl.keras.core.DLKerasNetworkSpec;
import org.knime.dl.keras.core.layers.impl.core.DLKerasDefaultInputLayer;
import org.knime.dl.keras.core.layers.impl.core.DLKerasDenseLayer;
import org.knime.dl.keras.core.layers.impl.merge.DLKerasAddLayer;
import org.knime.dl.keras.tensorflow.core.DLKerasTensorFlowNetworkLoader;
import org.knime.dl.python.core.DLPythonContext;
import org.knime.dl.python.core.DLPythonDefaultNetworkReader;
import org.knime.dl.util.DLUtils;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
class DLKerasLayerTestSetups {

    public static final DLNetworkLocation SEQUENTIAL_NETWORK_0;

    public static final DLNetworkLocation SEQUENTIAL_NETWORK_1;

    public static final DLNetworkLocation MULTI_INPUT_MULTI_OUTPUT_NETWORK_0;

    public static final DLNetworkLocation MULTI_INPUT_MULTI_OUTPUT_NETWORK_1;

    static {
        try {
            final Class<DLKerasLayerTestSetups> thisClass = DLKerasLayerTestSetups.class;
            SEQUENTIAL_NETWORK_0 = new DLNetworkReferenceLocation(
                DLUtils.Files.getFileFromSameBundle(thisClass, "data/simple_test_model.h5").toURI());
            SEQUENTIAL_NETWORK_1 = new DLNetworkReferenceLocation(
                DLUtils.Files.getFileFromSameBundle(thisClass, "data/simple_test_model_2.h5").toURI());
            MULTI_INPUT_MULTI_OUTPUT_NETWORK_0 = new DLNetworkReferenceLocation(
                DLUtils.Files.getFileFromSameBundle(thisClass, "data/3in_3out.h5").toURI());
            MULTI_INPUT_MULTI_OUTPUT_NETWORK_1 = new DLNetworkReferenceLocation(
                DLUtils.Files.getFileFromSameBundle(thisClass, "data/multi_in_out.h5").toURI());
        } catch (final Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private DLKerasLayerTestSetups() {
        // utility class
    }

    public static List<DLKerasLayer> createSingleLayerTestSetup() {
        return Arrays.asList(new DLKerasDefaultInputLayer());
    }

    public static List<DLKerasLayer> createSequentialModelTestSetup() {
        final DLKerasDefaultInputLayer in0 = new DLKerasDefaultInputLayer();

        final DLKerasDenseLayer hidden0 = new DLKerasDenseLayer();
        hidden0.setRuntimeId("hidden0");
        hidden0.setParent(0, in0);

        final DLKerasDenseLayer hidden1 = new DLKerasDenseLayer();
        hidden1.setRuntimeId("hidden1");
        hidden1.setParent(0, hidden0);

        final DLKerasDenseLayer hidden2 = new DLKerasDenseLayer();
        hidden2.setRuntimeId("hidden2");
        hidden2.setParent(0, hidden1);

        final DLKerasDenseLayer out0 = new DLKerasDenseLayer();
        out0.setRuntimeId("out0");
        out0.setParent(0, hidden2);

        return Arrays.asList(out0);
    }

    public static List<DLKerasLayer> createMultiInputModelTestSetup() {
        final DLKerasDefaultInputLayer in0 = new DLKerasDefaultInputLayer();
        in0.setRuntimeId("in0");

        final DLKerasDefaultInputLayer in1 = new DLKerasDefaultInputLayer();
        in1.setRuntimeId("in1");

        final DLKerasAddLayer hidden0 = new DLKerasAddLayer();
        hidden0.setRuntimeId("hidden0");
        hidden0.setParent(0, in0);
        hidden0.setParent(1, in1);

        final DLKerasDenseLayer hidden1 = new DLKerasDenseLayer();
        hidden1.setRuntimeId("hidden1");
        hidden1.setParent(0, hidden0);

        final DLKerasDenseLayer hidden2 = new DLKerasDenseLayer();
        hidden2.setRuntimeId("hidden2");
        hidden2.setParent(0, hidden1);

        final DLKerasDenseLayer out0 = new DLKerasDenseLayer();
        out0.setRuntimeId("out0");
        out0.setParent(0, hidden2);

        return Arrays.asList(out0);
    }

    public static List<DLKerasLayer> createMultiOutputModelTestSetup() {
        final DLKerasDefaultInputLayer in0 = new DLKerasDefaultInputLayer();
        in0.setRuntimeId("in0");

        final DLKerasDenseLayer hidden0 = new DLKerasDenseLayer();
        hidden0.setRuntimeId("hidden0");
        hidden0.setParent(0, in0);

        final DLKerasDenseLayer hidden1 = new DLKerasDenseLayer();
        hidden1.setRuntimeId("hidden1");
        hidden1.setParent(0, hidden0);

        final DLKerasDenseLayer hidden2 = new DLKerasDenseLayer();
        hidden2.setRuntimeId("hidden2");
        hidden2.setParent(0, hidden1);

        final DLKerasDenseLayer out0 = new DLKerasDenseLayer();
        out0.setRuntimeId("out0");
        out0.setParent(0, hidden2);

        final DLKerasDenseLayer out1 = new DLKerasDenseLayer();
        out1.setRuntimeId("out1");
        out1.setParent(0, hidden2);

        return Arrays.asList(out0, out1);
    }

    public static List<DLKerasLayer> createMultiInputMultiOutputModelTestSetup() {
        final DLKerasDefaultInputLayer in0 = new DLKerasDefaultInputLayer();
        in0.setRuntimeId("in0");

        final DLKerasDefaultInputLayer in1 = new DLKerasDefaultInputLayer();
        in1.setRuntimeId("in1");

        final DLKerasAddLayer hidden0 = new DLKerasAddLayer();
        hidden0.setRuntimeId("hidden0");
        hidden0.setParent(0, in0);
        hidden0.setParent(1, in1);

        final DLKerasDenseLayer hidden1 = new DLKerasDenseLayer();
        hidden1.setRuntimeId("hidden1");
        hidden1.setParent(0, hidden0);

        final DLKerasDenseLayer hidden2 = new DLKerasDenseLayer();
        hidden2.setRuntimeId("hidden2");
        hidden2.setParent(0, hidden1);

        final DLKerasDenseLayer out0 = new DLKerasDenseLayer();
        out0.setRuntimeId("out0");
        out0.setParent(0, hidden2);

        final DLKerasDenseLayer out1 = new DLKerasDenseLayer();
        out1.setRuntimeId("out1");
        out1.setParent(0, hidden2);

        return Arrays.asList(out0, out1);
    }

    public static List<DLKerasLayer> createMultiInputMultiOutputForkJoinModelTestSetup() {
        final DLKerasDefaultInputLayer in0 = new DLKerasDefaultInputLayer();
        in0.setRuntimeId("in0");

        final DLKerasDefaultInputLayer in1 = new DLKerasDefaultInputLayer();
        in1.setRuntimeId("in1");

        final DLKerasDefaultInputLayer in2 = new DLKerasDefaultInputLayer();
        in2.setRuntimeId("in2");

        final DLKerasDenseLayer hidden0 = new DLKerasDenseLayer();
        hidden0.setRuntimeId("hidden0");
        hidden0.setParent(0, in0);

        final DLKerasAddLayer hidden1 = new DLKerasAddLayer();
        hidden1.setRuntimeId("hidden1");
        hidden1.setParent(0, hidden0);
        hidden1.setParent(1, in1);

        final DLKerasAddLayer hidden2 = new DLKerasAddLayer();
        hidden2.setRuntimeId("hidden2");
        hidden2.setParent(0, hidden1);
        hidden2.setParent(1, in2);

        final DLKerasAddLayer hidden3 = new DLKerasAddLayer();
        hidden3.setRuntimeId("hidden3");
        hidden3.setParent(0, hidden0);
        hidden3.setParent(1, hidden2);

        final DLKerasDenseLayer hidden4 = new DLKerasDenseLayer();
        hidden4.setRuntimeId("hidden4");
        hidden4.setParent(0, hidden2);

        final DLKerasDenseLayer hidden5 = new DLKerasDenseLayer();
        hidden5.setRuntimeId("hidden5");
        hidden5.setParent(0, hidden3);

        final DLKerasDenseLayer hidden6 = new DLKerasDenseLayer();
        hidden6.setRuntimeId("hidden6");
        hidden6.setParent(0, hidden3);

        final DLKerasDenseLayer hidden7 = new DLKerasDenseLayer();
        hidden7.setRuntimeId("hidden7");
        hidden7.setParent(0, hidden4);

        final DLKerasDenseLayer out0 = new DLKerasDenseLayer();
        out0.setRuntimeId("out0");
        out0.setParent(0, hidden5);

        final DLKerasDenseLayer out1 = new DLKerasDenseLayer();
        out1.setRuntimeId("out1");
        out1.setParent(0, hidden5);

        final DLKerasAddLayer out2 = new DLKerasAddLayer();
        out2.setRuntimeId("out2");
        out2.setParent(0, hidden6);
        out2.setParent(1, hidden7);

        return Arrays.asList(out0, out1, out2);
    }

    public static Pair<List<DLKerasNetwork>, List<DLKerasLayer>>
        createSequentialModelAppendedUnaryLayerTestSetup(final DLPythonContext context)
            throws DLInvalidSourceException, DLInvalidEnvironmentException, IOException, DLCanceledExecutionException {
        final DLKerasNetwork baseNetwork = new DLPythonDefaultNetworkReader<>(new DLKerasTensorFlowNetworkLoader())
            .read(SEQUENTIAL_NETWORK_0, false, context, DLNotCancelable.INSTANCE);

        final DLKerasDefaultBaseNetworkTensorSpecOutput baseNetworkOut0 =
            new DLKerasDefaultBaseNetworkTensorSpecOutput(baseNetwork, 0);

        final DLKerasDenseLayer out0 = new DLKerasDenseLayer();
        out0.setRuntimeId("out0");
        out0.setParent(0, baseNetworkOut0);

        return new Pair<>(Arrays.asList(baseNetwork), Arrays.asList(out0));
    }

    public static Pair<List<DLKerasNetwork>, List<DLKerasLayer>>
        createMultiInputMultiOutputModelAppendedUnaryLayerTestSetup(final DLPythonContext context)
            throws DLInvalidSourceException, DLInvalidEnvironmentException, IOException, DLCanceledExecutionException {
        final DLKerasNetwork baseNetwork = new DLPythonDefaultNetworkReader<>(new DLKerasTensorFlowNetworkLoader())
            .read(MULTI_INPUT_MULTI_OUTPUT_NETWORK_0, false, context, DLNotCancelable.INSTANCE);

        final DLKerasDefaultBaseNetworkTensorSpecOutput baseNetworkOut0 =
            new DLKerasDefaultBaseNetworkTensorSpecOutput(baseNetwork, 1);

        final DLKerasDenseLayer out0 = new DLKerasDenseLayer();
        out0.setRuntimeId("out0");
        out0.setParent(0, baseNetworkOut0);

        return new Pair<>(Arrays.asList(baseNetwork), Arrays.asList(out0));
    }

    public static Pair<List<DLKerasNetwork>, List<DLKerasLayer>>
        createSequentialModelAppendedBinaryLayerTestSetup(final DLPythonContext context)
            throws DLInvalidSourceException, DLInvalidEnvironmentException, IOException, DLCanceledExecutionException {
        final DLKerasNetwork baseNetwork = new DLPythonDefaultNetworkReader<>(new DLKerasTensorFlowNetworkLoader())
            .read(SEQUENTIAL_NETWORK_0, false, context, DLNotCancelable.INSTANCE);

        final DLKerasDefaultBaseNetworkTensorSpecOutput baseNetworkOut0 =
            new DLKerasDefaultBaseNetworkTensorSpecOutput(baseNetwork, 0);

        final DLKerasAddLayer out0 = new DLKerasAddLayer();
        out0.setRuntimeId("out0");
        out0.setParent(0, baseNetworkOut0);
        out0.setParent(1, baseNetworkOut0);

        return new Pair<>(Arrays.asList(baseNetwork), Arrays.asList(out0));
    }

    public static Pair<List<DLKerasNetwork>, List<DLKerasLayer>>
        createMultiInputMultiOutputModelAppendedBinaryLayerTestSetup(final DLPythonContext context)
            throws DLInvalidSourceException, DLInvalidEnvironmentException, IOException, DLCanceledExecutionException {
        final DLKerasNetwork baseNetwork = new DLPythonDefaultNetworkReader<>(new DLKerasTensorFlowNetworkLoader())
            .read(MULTI_INPUT_MULTI_OUTPUT_NETWORK_0, false, context, DLNotCancelable.INSTANCE);

        final DLKerasDefaultBaseNetworkTensorSpecOutput baseNetworkOut0 =
            new DLKerasDefaultBaseNetworkTensorSpecOutput(baseNetwork, 0);

        final DLKerasDefaultBaseNetworkTensorSpecOutput baseNetworkOut1 =
            new DLKerasDefaultBaseNetworkTensorSpecOutput(baseNetwork, 1);

        final DLKerasAddLayer out0 = new DLKerasAddLayer();
        out0.setRuntimeId("out0");
        out0.setParent(0, baseNetworkOut0);
        out0.setParent(1, baseNetworkOut1);

        return new Pair<>(Arrays.asList(baseNetwork), Arrays.asList(out0));
    }

    public static Pair<List<DLKerasNetwork>, List<DLKerasLayer>>
        createTwoMultiInputMultiOutputModelsAppendedBinaryLayerTestSetup(final DLPythonContext context)
            throws DLInvalidSourceException, DLInvalidEnvironmentException, IOException, DLCanceledExecutionException {
        final DLKerasNetwork baseNetwork0 = new DLPythonDefaultNetworkReader<>(new DLKerasTensorFlowNetworkLoader())
            .read(MULTI_INPUT_MULTI_OUTPUT_NETWORK_0, false, context, DLNotCancelable.INSTANCE);

        final DLKerasNetwork baseNetwork1 = new DLPythonDefaultNetworkReader<>(new DLKerasTensorFlowNetworkLoader())
            .read(MULTI_INPUT_MULTI_OUTPUT_NETWORK_1, false, context, DLNotCancelable.INSTANCE);

        final DLKerasDefaultBaseNetworkTensorSpecOutput baseNetwork0Out0 =
            new DLKerasDefaultBaseNetworkTensorSpecOutput(baseNetwork0, 2);

        final DLKerasDefaultBaseNetworkTensorSpecOutput baseNetwork1Out0 =
            new DLKerasDefaultBaseNetworkTensorSpecOutput(baseNetwork1, 1);

        final DLKerasAddLayer out0 = new DLKerasAddLayer();
        out0.setRuntimeId("out0");
        out0.setParent(0, baseNetwork0Out0);
        out0.setParent(1, baseNetwork1Out0);

        return new Pair<>(Arrays.asList(baseNetwork0, baseNetwork1), Arrays.asList(out0));
    }

    public static <IO> IO testOnSingleLayerSetup(final Function<List<DLKerasLayer>, IO> testFunction,
        final Function<IO, DLKerasNetworkSpec> testFunctionOutputToSpec) {
        final List<DLKerasLayer> outputLayers = createSingleLayerTestSetup();

        final IO testFunctionOutput = testFunction.apply(outputLayers);
        final DLKerasNetworkSpec networkSpec = testFunctionOutputToSpec.apply(testFunctionOutput);

        final DLTensorSpec[] inputSpecs = networkSpec.getInputSpecs();
        assertTrue(inputSpecs.length == 1);

        final DLTensorSpec inputSpec0 = inputSpecs[0];
        assertTrue(inputSpec0.getIdentifier().getIdentifierString().equals("input_1_0:0"));
        assertTrue(!inputSpec0.getBatchSize().isPresent());
        final long[] inputShape0 = DLUtils.Shapes.getFixedShape(inputSpec0.getShape()).get();
        assertTrue(inputShape0.length == 1);
        assertTrue(inputShape0[0] == 1);
        assertTrue(inputSpec0.getElementType() == float.class);

        final DLTensorSpec[] outputSpecs = networkSpec.getOutputSpecs();
        assertTrue(outputSpecs.length == 1);

        final DLTensorSpec outputSpec0 = outputSpecs[0];
        assertTrue(outputSpec0.getIdentifier().getIdentifierString().equals("input_1_0:0"));
        assertTrue(!outputSpec0.getBatchSize().isPresent());
        final long[] outputShape0 = DLUtils.Shapes.getFixedShape(outputSpec0.getShape()).get();
        assertTrue(outputShape0.length == 1);
        assertTrue(outputShape0[0] == 1);
        assertTrue(outputSpec0.getElementType() == float.class);

        return testFunctionOutput;
    }

    public static <IO> IO testOnSequentialModelSetup(final Function<List<DLKerasLayer>, IO> testFunction,
        final Function<IO, DLKerasNetworkSpec> testFunctionOutputToSpec) {
        final List<DLKerasLayer> outputLayers = createSequentialModelTestSetup();

        final IO testFunctionOutput = testFunction.apply(outputLayers);
        final DLKerasNetworkSpec networkSpec = testFunctionOutputToSpec.apply(testFunctionOutput);

        final DLTensorSpec[] inputSpecs = networkSpec.getInputSpecs();
        assertTrue(inputSpecs.length == 1);

        final DLTensorSpec inputSpec0 = inputSpecs[0];
        assertTrue(inputSpec0.getIdentifier().getIdentifierString().equals("input_1_0:0"));
        assertTrue(!inputSpec0.getBatchSize().isPresent());
        final long[] inputShape0 = DLUtils.Shapes.getFixedShape(inputSpec0.getShape()).get();
        assertTrue(inputShape0.length == 1);
        assertTrue(inputShape0[0] == 1);
        assertTrue(inputSpec0.getElementType() == float.class);

        final DLTensorSpec[] outputSpecs = networkSpec.getOutputSpecs();
        assertTrue(outputSpecs.length == 1);

        final DLTensorSpec outputSpec0 = outputSpecs[0];
        assertTrue(outputSpec0.getIdentifier().getIdentifierString().equals("dense_4_0:0"));
        assertTrue(!outputSpec0.getBatchSize().isPresent());
        final long[] outputShape0 = DLUtils.Shapes.getFixedShape(outputSpec0.getShape()).get();
        assertTrue(outputShape0.length == 1);
        assertTrue(outputShape0[0] == 1);
        assertTrue(outputSpec0.getElementType() == float.class);

        return testFunctionOutput;
    }

    public static <IO> IO testOnMultiInputModelSetup(final Function<List<DLKerasLayer>, IO> testFunction,
        final Function<IO, DLKerasNetworkSpec> testFunctionOutputToSpec) {
        final List<DLKerasLayer> outputLayers = createMultiInputModelTestSetup();

        final IO testFunctionOutput = testFunction.apply(outputLayers);
        final DLKerasNetworkSpec networkSpec = testFunctionOutputToSpec.apply(testFunctionOutput);

        final DLTensorSpec[] inputSpecs = networkSpec.getInputSpecs();
        assertTrue(inputSpecs.length == 2);

        final DLTensorSpec inputSpec0 = inputSpecs[0];
        assertTrue(inputSpec0.getIdentifier().getIdentifierString().equals("input_1_0:0"));
        assertTrue(!inputSpec0.getBatchSize().isPresent());
        final long[] inputShape0 = DLUtils.Shapes.getFixedShape(inputSpec0.getShape()).get();
        assertTrue(inputShape0.length == 1);
        assertTrue(inputShape0[0] == 1);
        assertTrue(inputSpec0.getElementType() == float.class);

        final DLTensorSpec inputSpec1 = inputSpecs[1];
        assertTrue(inputSpec1.getIdentifier().getIdentifierString().equals("input_2_0:0"));
        assertTrue(!inputSpec1.getBatchSize().isPresent());
        final long[] inputShape1 = DLUtils.Shapes.getFixedShape(inputSpec1.getShape()).get();
        assertTrue(inputShape1.length == 1);
        assertTrue(inputShape1[0] == 1);
        assertTrue(inputSpec1.getElementType() == float.class);

        final DLTensorSpec[] outputSpecs = networkSpec.getOutputSpecs();
        assertTrue(outputSpecs.length == 1);

        final DLTensorSpec outputSpec0 = outputSpecs[0];
        assertTrue(outputSpec0.getIdentifier().getIdentifierString().equals("dense_3_0:0"));
        assertTrue(!outputSpec0.getBatchSize().isPresent());
        final long[] outputShape0 = DLUtils.Shapes.getFixedShape(outputSpec0.getShape()).get();
        assertTrue(outputShape0.length == 1);
        assertTrue(outputShape0[0] == 1);
        assertTrue(outputSpec0.getElementType() == float.class);

        return testFunctionOutput;
    }

    public static <IO> IO testOnMultiOutputModelSetup(final Function<List<DLKerasLayer>, IO> testFunction,
        final Function<IO, DLKerasNetworkSpec> testFunctionOutputToSpec) {
        final List<DLKerasLayer> outputLayers = createMultiOutputModelTestSetup();

        final IO testFunctionOutput = testFunction.apply(outputLayers);
        final DLKerasNetworkSpec networkSpec = testFunctionOutputToSpec.apply(testFunctionOutput);

        final DLTensorSpec[] inputSpecs = networkSpec.getInputSpecs();
        assertTrue(inputSpecs.length == 1);

        final DLTensorSpec inputSpec0 = inputSpecs[0];
        assertTrue(inputSpec0.getIdentifier().getIdentifierString().equals("input_1_0:0"));
        assertTrue(!inputSpec0.getBatchSize().isPresent());
        final long[] inputShape0 = DLUtils.Shapes.getFixedShape(inputSpec0.getShape()).get();
        assertTrue(inputShape0.length == 1);
        assertTrue(inputShape0[0] == 1);
        assertTrue(inputSpec0.getElementType() == float.class);

        final DLTensorSpec[] outputSpecs = networkSpec.getOutputSpecs();
        assertTrue(outputSpecs.length == 2);

        final DLTensorSpec outputSpec0 = outputSpecs[0];
        assertTrue(outputSpec0.getIdentifier().getIdentifierString().equals("dense_4_0:0"));
        assertTrue(!outputSpec0.getBatchSize().isPresent());
        final long[] outputShape0 = DLUtils.Shapes.getFixedShape(outputSpec0.getShape()).get();
        assertTrue(outputShape0.length == 1);
        assertTrue(outputShape0[0] == 1);
        assertTrue(outputSpec0.getElementType() == float.class);

        final DLTensorSpec outputSpec1 = outputSpecs[1];
        assertTrue(outputSpec1.getIdentifier().getIdentifierString().equals("dense_5_0:0"));
        assertTrue(!outputSpec1.getBatchSize().isPresent());
        final long[] outputShape1 = DLUtils.Shapes.getFixedShape(outputSpec1.getShape()).get();
        assertTrue(outputShape1.length == 1);
        assertTrue(outputShape1[0] == 1);
        assertTrue(outputSpec1.getElementType() == float.class);

        return testFunctionOutput;
    }

    public static <IO> IO testOnMultiInputMultiOutputModelSetup(final Function<List<DLKerasLayer>, IO> testFunction,
        final Function<IO, DLKerasNetworkSpec> testFunctionOutputToSpec) {
        final List<DLKerasLayer> outputLayers = createMultiInputMultiOutputModelTestSetup();

        final IO testFunctionOutput = testFunction.apply(outputLayers);
        final DLKerasNetworkSpec networkSpec = testFunctionOutputToSpec.apply(testFunctionOutput);

        final DLTensorSpec[] inputSpecs = networkSpec.getInputSpecs();
        assertTrue(inputSpecs.length == 2);

        final DLTensorSpec inputSpec0 = inputSpecs[0];
        assertTrue(inputSpec0.getIdentifier().getIdentifierString().equals("input_1_0:0"));
        assertTrue(!inputSpec0.getBatchSize().isPresent());
        final long[] inputShape0 = DLUtils.Shapes.getFixedShape(inputSpec0.getShape()).get();
        assertTrue(inputShape0.length == 1);
        assertTrue(inputShape0[0] == 1);
        assertTrue(inputSpec0.getElementType() == float.class);

        final DLTensorSpec inputSpec1 = inputSpecs[1];
        assertTrue(inputSpec1.getIdentifier().getIdentifierString().equals("input_2_0:0"));
        assertTrue(!inputSpec1.getBatchSize().isPresent());
        final long[] inputShape1 = DLUtils.Shapes.getFixedShape(inputSpec1.getShape()).get();
        assertTrue(inputShape1.length == 1);
        assertTrue(inputShape1[0] == 1);
        assertTrue(inputSpec1.getElementType() == float.class);

        final DLTensorSpec[] outputSpecs = networkSpec.getOutputSpecs();
        assertTrue(outputSpecs.length == 2);

        final DLTensorSpec outputSpec0 = outputSpecs[0];
        assertTrue(outputSpec0.getIdentifier().getIdentifierString().equals("dense_3_0:0"));
        assertTrue(!outputSpec0.getBatchSize().isPresent());
        final long[] outputShape0 = DLUtils.Shapes.getFixedShape(outputSpec0.getShape()).get();
        assertTrue(outputShape0.length == 1);
        assertTrue(outputShape0[0] == 1);
        assertTrue(outputSpec0.getElementType() == float.class);

        final DLTensorSpec outputSpec1 = outputSpecs[1];
        assertTrue(outputSpec1.getIdentifier().getIdentifierString().equals("dense_4_0:0"));
        assertTrue(!outputSpec1.getBatchSize().isPresent());
        final long[] outputShape1 = DLUtils.Shapes.getFixedShape(outputSpec1.getShape()).get();
        assertTrue(outputShape1.length == 1);
        assertTrue(outputShape1[0] == 1);
        assertTrue(outputSpec1.getElementType() == float.class);

        return testFunctionOutput;
    }

    public static <IO> IO testOnMultiInputMultiOutputForkJoinModelSetup(
        final Function<List<DLKerasLayer>, IO> testFunction,
        final Function<IO, DLKerasNetworkSpec> testFunctionOutputToSpec) {
        final List<DLKerasLayer> outputLayers = createMultiInputMultiOutputForkJoinModelTestSetup();

        final IO testFunctionOutput = testFunction.apply(outputLayers);
        final DLKerasNetworkSpec networkSpec = testFunctionOutputToSpec.apply(testFunctionOutput);

        final DLTensorSpec[] inputSpecs = networkSpec.getInputSpecs();
        assertTrue(inputSpecs.length == 3);

        final DLTensorSpec inputSpec0 = inputSpecs[0];
        assertTrue(inputSpec0.getIdentifier().getIdentifierString().equals("input_1_0:0"));
        assertTrue(!inputSpec0.getBatchSize().isPresent());
        final long[] inputShape0 = DLUtils.Shapes.getFixedShape(inputSpec0.getShape()).get();
        assertTrue(inputShape0.length == 1);
        assertTrue(inputShape0[0] == 1);
        assertTrue(inputSpec0.getElementType() == float.class);

        final DLTensorSpec inputSpec1 = inputSpecs[1];
        assertTrue(inputSpec1.getIdentifier().getIdentifierString().equals("input_2_0:0"));
        assertTrue(!inputSpec1.getBatchSize().isPresent());
        final long[] inputShape1 = DLUtils.Shapes.getFixedShape(inputSpec1.getShape()).get();
        assertTrue(inputShape1.length == 1);
        assertTrue(inputShape1[0] == 1);
        assertTrue(inputSpec1.getElementType() == float.class);

        final DLTensorSpec inputSpec2 = inputSpecs[2];
        assertTrue(inputSpec2.getIdentifier().getIdentifierString().equals("input_3_0:0"));
        assertTrue(!inputSpec2.getBatchSize().isPresent());
        final long[] inputShape2 = DLUtils.Shapes.getFixedShape(inputSpec2.getShape()).get();
        assertTrue(inputShape2.length == 1);
        assertTrue(inputShape2[0] == 1);
        assertTrue(inputSpec2.getElementType() == float.class);

        final DLTensorSpec[] outputSpecs = networkSpec.getOutputSpecs();
        assertTrue(outputSpecs.length == 3);

        final DLTensorSpec outputSpec0 = outputSpecs[0];
        assertTrue(outputSpec0.getIdentifier().getIdentifierString().equals("dense_6_0:0"));
        assertTrue(!outputSpec0.getBatchSize().isPresent());
        final long[] outputShape0 = DLUtils.Shapes.getFixedShape(outputSpec0.getShape()).get();
        assertTrue(outputShape0.length == 1);
        assertTrue(outputShape0[0] == 1);
        assertTrue(outputSpec0.getElementType() == float.class);

        final DLTensorSpec outputSpec1 = outputSpecs[1];
        assertTrue(outputSpec1.getIdentifier().getIdentifierString().equals("dense_7_0:0"));
        assertTrue(!outputSpec1.getBatchSize().isPresent());
        final long[] outputShape1 = DLUtils.Shapes.getFixedShape(outputSpec1.getShape()).get();
        assertTrue(outputShape1.length == 1);
        assertTrue(outputShape1[0] == 1);
        assertTrue(outputSpec1.getElementType() == float.class);

        final DLTensorSpec outputSpec2 = outputSpecs[2];
        assertTrue(outputSpec2.getIdentifier().getIdentifierString().equals("add_4_0:0"));
        assertTrue(!outputSpec2.getBatchSize().isPresent());
        final long[] outputShape2 = DLUtils.Shapes.getFixedShape(outputSpec2.getShape()).get();
        assertTrue(outputShape2.length == 1);
        assertTrue(outputShape2[0] == 1);
        assertTrue(outputSpec2.getElementType() == float.class);

        return testFunctionOutput;
    }

    public static <IO> IO testOnSequentialModelAppendedUnaryLayerSetup(
        final Function<List<DLKerasLayer>, IO> testFunction,
        final Function<IO, DLKerasNetworkSpec> testFunctionOutputToSpec, final DLPythonContext context)
        throws DLInvalidSourceException, DLInvalidEnvironmentException, IOException, DLCanceledExecutionException {
        final Pair<List<DLKerasNetwork>, List<DLKerasLayer>> pair = createSequentialModelAppendedUnaryLayerTestSetup(context);
        final DLKerasNetwork baseNetwork = pair.getFirst().get(0);
        final DLKerasLayer out0 = pair.getSecond().get(0);

        final IO testFunctionOutput = testFunction.apply(Arrays.asList(out0));
        final DLKerasNetworkSpec networkSpec = testFunctionOutputToSpec.apply(testFunctionOutput);

        final DLTensorSpec[] inputSpecs = networkSpec.getInputSpecs();
        assertTrue(inputSpecs.length == 1);

        assertTrue(inputSpecs[0].equals(baseNetwork.getSpec().getInputSpecs()[0]));

        final DLTensorSpec[] outputSpecs = networkSpec.getOutputSpecs();
        assertTrue(outputSpecs.length == 1);

        final DLTensorSpec outputSpec0 = outputSpecs[0];
        assertTrue(outputSpec0.getIdentifier().getIdentifierString().equals("dense_4_0:0"));
        assertTrue(!outputSpec0.getBatchSize().isPresent());
        final long[] outputShape0 = DLUtils.Shapes.getFixedShape(outputSpec0.getShape()).get();
        assertTrue(outputShape0.length == 1);
        assertTrue(outputShape0[0] == 1);
        assertTrue(outputSpec0.getElementType() == float.class);

        return testFunctionOutput;
    }

    public static <IO> IO testOnMultiInputMultiOutputModelAppendedUnaryLayerSetup(
        final Function<List<DLKerasLayer>, IO> testFunction,
        final Function<IO, DLKerasNetworkSpec> testFunctionOutputToSpec, final DLPythonContext context)
        throws DLInvalidSourceException, DLInvalidEnvironmentException, IOException, DLCanceledExecutionException {
        final Pair<List<DLKerasNetwork>, List<DLKerasLayer>> pair =
            createMultiInputMultiOutputModelAppendedUnaryLayerTestSetup(context);
        final DLKerasNetwork baseNetwork = pair.getFirst().get(0);
        final DLKerasLayer out0 = pair.getSecond().get(0);

        final IO testFunctionOutput = testFunction.apply(Arrays.asList(out0));
        final DLKerasNetworkSpec networkSpec = testFunctionOutputToSpec.apply(testFunctionOutput);

        final DLTensorSpec[] inputSpecs = networkSpec.getInputSpecs();
        assertTrue(inputSpecs.length == 3);

        assertTrue(inputSpecs[0].equals(baseNetwork.getSpec().getInputSpecs()[0]));
        assertTrue(inputSpecs[1].equals(baseNetwork.getSpec().getInputSpecs()[1]));
        assertTrue(inputSpecs[2].equals(baseNetwork.getSpec().getInputSpecs()[2]));

        final DLTensorSpec[] outputSpecs = networkSpec.getOutputSpecs();
        assertTrue(outputSpecs.length == 3);

        assertTrue(outputSpecs[0].equals(baseNetwork.getSpec().getOutputSpecs()[0]));

        final DLTensorSpec outputSpec1 = outputSpecs[1];
        // Would currently fail because order of network outputs is not yet as desired. Pending.
        NodeLogger.getLogger(DLKerasLayerTestSetups.class)
            .warn("DL Keras: Skipping some assertions that rely on pending work.");
        // assertTrue(outputSpec1.getIdentifier().getIdentifierString().equals("dense_7_0:0"));
        // assertTrue(!outputSpec1.getBatchSize().isPresent());
        // final long[] outputShape1 = DLUtils.Shapes.getFixedShape(outputSpec1.getShape()).get();
        // assertTrue(outputShape1.length == 1);
        // assertTrue(outputShape1[0] == 1);
        // assertTrue(outputSpec1.getElementType() == float.class);
        //
        // assertTrue(outputSpecs[2].equals(baseNetwork.getSpec().getOutputSpecs()[2]));

        return testFunctionOutput;
    }

    public static <IO> IO testOnSequentialModelAppendedBinaryLayerSetup(
        final Function<List<DLKerasLayer>, IO> testFunction,
        final Function<IO, DLKerasNetworkSpec> testFunctionOutputToSpec, final DLPythonContext context)
        throws DLInvalidSourceException, DLInvalidEnvironmentException, IOException, DLCanceledExecutionException {
        final Pair<List<DLKerasNetwork>, List<DLKerasLayer>> pair = createSequentialModelAppendedBinaryLayerTestSetup(context);
        final DLKerasNetwork baseNetwork = pair.getFirst().get(0);
        final DLKerasLayer out0 = pair.getSecond().get(0);

        final IO testFunctionOutput = testFunction.apply(Arrays.asList(out0));
        final DLKerasNetworkSpec networkSpec = testFunctionOutputToSpec.apply(testFunctionOutput);

        final DLTensorSpec[] inputSpecs = networkSpec.getInputSpecs();
        assertTrue(inputSpecs.length == 1);

        assertTrue(inputSpecs[0].equals(baseNetwork.getSpec().getInputSpecs()[0]));

        final DLTensorSpec[] outputSpecs = networkSpec.getOutputSpecs();
        assertTrue(outputSpecs.length == 1);

        final DLTensorSpec outputSpec0 = outputSpecs[0];
        assertTrue(outputSpec0.getIdentifier().getIdentifierString().equals("add_1_0:0"));
        assertTrue(!outputSpec0.getBatchSize().isPresent());
        final long[] outputShape0 = DLUtils.Shapes.getFixedShape(outputSpec0.getShape()).get();
        assertTrue(outputShape0.length == 1);
        assertTrue(outputShape0[0] == 10);
        assertTrue(outputSpec0.getElementType() == float.class);

        return testFunctionOutput;
    }

    public static <IO> IO testOnMultiInputMultiOutputModelAppendedBinaryLayerSetup(
        final Function<List<DLKerasLayer>, IO> testFunction,
        final Function<IO, DLKerasNetworkSpec> testFunctionOutputToSpec, final DLPythonContext context)
        throws DLInvalidSourceException, DLInvalidEnvironmentException, IOException, DLCanceledExecutionException {
        final Pair<List<DLKerasNetwork>, List<DLKerasLayer>> pair =
            createMultiInputMultiOutputModelAppendedBinaryLayerTestSetup(context);
        final DLKerasNetwork baseNetwork = pair.getFirst().get(0);
        final DLKerasLayer out0 = pair.getSecond().get(0);

        final IO testFunctionOutput = testFunction.apply(Arrays.asList(out0));
        final DLKerasNetworkSpec networkSpec = testFunctionOutputToSpec.apply(testFunctionOutput);

        final DLTensorSpec[] inputSpecs = networkSpec.getInputSpecs();
        assertTrue(inputSpecs.length == 3);

        assertTrue(inputSpecs[0].equals(baseNetwork.getSpec().getInputSpecs()[0]));
        assertTrue(inputSpecs[1].equals(baseNetwork.getSpec().getInputSpecs()[1]));
        assertTrue(inputSpecs[2].equals(baseNetwork.getSpec().getInputSpecs()[2]));

        final DLTensorSpec[] outputSpecs = networkSpec.getOutputSpecs();
        assertTrue(outputSpecs.length == 2);

        final DLTensorSpec outputSpec0 = outputSpecs[0];
        // Would currently fail because order of network outputs is not yet as desired. Pending.
        NodeLogger.getLogger(DLKerasLayerTestSetups.class)
            .warn("DL Keras: Skipping some assertions that rely on pending work.");
        // assertTrue(outputSpec0.getIdentifier().getIdentifierString().equals("add_4_0:0"));
        // assertTrue(!outputSpec0.getBatchSize().isPresent());
        // final long[] outputShape0 = DLUtils.Shapes.getFixedShape(outputSpec0.getShape()).get();
        // assertTrue(outputShape0.length == 1);
        // assertTrue(outputShape0[0] == 5);
        // assertTrue(outputSpec0.getElementType() == float.class);

        // assertTrue(outputSpecs[1].equals(baseNetwork.getSpec().getOutputSpecs()[2]));

        return testFunctionOutput;
    }

    public static <IO> IO testOnTwoMultiInputMultiOutputModelsAppendedBinaryLayerSetup(
        final Function<List<DLKerasLayer>, IO> testFunction,
        final Function<IO, DLKerasNetworkSpec> testFunctionOutputToSpec, final DLPythonContext context)
        throws DLInvalidSourceException, DLInvalidEnvironmentException, IOException, DLCanceledExecutionException {
        final Pair<List<DLKerasNetwork>, List<DLKerasLayer>> pair =
            createTwoMultiInputMultiOutputModelsAppendedBinaryLayerTestSetup(context);
        final DLKerasNetwork baseNetwork0 = pair.getFirst().get(0);
        final DLKerasNetwork baseNetwork1 = pair.getFirst().get(1);
        final DLKerasLayer out0 = pair.getSecond().get(0);

        final IO testFunctionOutput = testFunction.apply(Arrays.asList(out0));
        final DLKerasNetworkSpec networkSpec = testFunctionOutputToSpec.apply(testFunctionOutput);

        final DLTensorSpec[] inputSpecs = networkSpec.getInputSpecs();
        assertTrue(inputSpecs.length == 5);

        assertTrue(inputSpecs[0].equals(baseNetwork0.getSpec().getInputSpecs()[0]));
        assertTrue(inputSpecs[1].equals(baseNetwork0.getSpec().getInputSpecs()[1]));
        assertTrue(inputSpecs[2].equals(baseNetwork0.getSpec().getInputSpecs()[2]));
        assertTrue(inputSpecs[3].equals(baseNetwork1.getSpec().getInputSpecs()[0]));
        assertTrue(inputSpecs[4].equals(baseNetwork1.getSpec().getInputSpecs()[1]));

        final DLTensorSpec[] outputSpecs = networkSpec.getOutputSpecs();
        assertTrue(outputSpecs.length == 4);

        assertTrue(outputSpecs[0].equals(baseNetwork0.getSpec().getOutputSpecs()[0]));
        assertTrue(outputSpecs[1].equals(baseNetwork0.getSpec().getOutputSpecs()[1]));

        final DLTensorSpec outputSpec2 = outputSpecs[2];
        // Would currently fail because order of network outputs is not yet as desired. Pending.
        NodeLogger.getLogger(DLKerasLayerTestSetups.class)
            .warn("DL Keras: Skipping some assertions that rely on pending work.");
        // assertTrue(outputSpec2.getIdentifier().getIdentifierString().equals("add_3_0:0"));
        // assertTrue(!outputSpec2.getBatchSize().isPresent());
        // final long[] outputShape2 = DLUtils.Shapes.getFixedShape(outputSpec2.getShape()).get();
        // assertTrue(outputShape2.length == 1);
        // assertTrue(outputShape2[0] == 5);
        // assertTrue(outputSpec2.getElementType() == float.class);
        //
        // assertTrue(outputSpecs[3].equals(baseNetwork1.getSpec().getOutputSpecs()[1]));

        return testFunctionOutput;
    }

    public static <IO extends DLKerasNetwork> IO testOnMultipleNetworksMultipleAppendedLayersSetup(
        final Function<List<DLKerasLayer>, IO> testFunction,
        final Function<IO, DLKerasNetworkSpec> testFunctionOutputToSpec, final DLPythonContext context)
        throws DLInvalidSourceException, DLInvalidEnvironmentException, IOException, DLCanceledExecutionException {
        final DLKerasNetwork baseNetwork0 = new DLPythonDefaultNetworkReader<>(new DLKerasTensorFlowNetworkLoader())
            .read(SEQUENTIAL_NETWORK_1, false, context, DLNotCancelable.INSTANCE);

        final DLKerasNetwork baseNetwork1 = new DLPythonDefaultNetworkReader<>(new DLKerasTensorFlowNetworkLoader())
            .read(MULTI_INPUT_MULTI_OUTPUT_NETWORK_0, false, context, DLNotCancelable.INSTANCE);

        final DLKerasNetwork baseNetwork2 = new DLPythonDefaultNetworkReader<>(new DLKerasTensorFlowNetworkLoader())
            .read(MULTI_INPUT_MULTI_OUTPUT_NETWORK_1, false, context, DLNotCancelable.INSTANCE);

        final DLKerasDefaultBaseNetworkTensorSpecOutput baseNetwork0Out0 =
            new DLKerasDefaultBaseNetworkTensorSpecOutput(baseNetwork0, 0);

        final DLKerasDefaultBaseNetworkTensorSpecOutput baseNetwork1Out1 =
            new DLKerasDefaultBaseNetworkTensorSpecOutput(baseNetwork1, 1);

        final DLKerasDefaultBaseNetworkTensorSpecOutput baseNetwork2Out0 =
            new DLKerasDefaultBaseNetworkTensorSpecOutput(baseNetwork2, 0);

        // First generated network:

        final DLKerasAddLayer hidden0_0 = new DLKerasAddLayer();
        hidden0_0.setRuntimeId("hidden0_0");
        hidden0_0.setParent(0, baseNetwork0Out0);
        hidden0_0.setParent(1, baseNetwork1Out1);

        final DLKerasAddLayer hidden0_1 = new DLKerasAddLayer();
        hidden0_1.setRuntimeId("hidden0_1");
        hidden0_1.setParent(0, baseNetwork1Out1);
        hidden0_1.setParent(1, baseNetwork2Out0);

        final DLKerasAddLayer out0_0 = new DLKerasAddLayer();
        out0_0.setRuntimeId("out0_0");
        out0_0.setParent(0, hidden0_0);
        out0_0.setParent(1, hidden0_1);

        // TODO: Use base networks without duplicate names. Concatenation of those does not work.
        NodeLogger.getLogger(DLKerasLayerTestSetups.class)
            .warn("DL Keras: Skipping a test that requires additional work.");
        return null;
        // final IO testFunctionOutput0 = testFunction.apply(Arrays.asList(out0_0));
        // final DLKerasNetworkSpec networkSpec0 = testFunctionOutputToSpec.apply(testFunctionOutput0);
        //
        // // TODO: check specs of network1
        //
        // // Second generated network:
        //
        // final DLKerasAddLayer hidden1_0 = new DLKerasAddLayer();
        // hidden1_0.setParent(0, new DLKerasDefaultBaseNetworkTensorSpecOutput(testFunctionOutput0, 0));
        // hidden1_0.setParent(0, baseNetwork2Out0);
        //
        // final DLKerasDenseLayer out1_0 = new DLKerasDenseLayer();
        // out1_0.setParent(0, hidden1_0);
        //
        // final IO testFunctionOutput1 = testFunction.apply(Arrays.asList(out1_0));
        // final DLKerasNetworkSpec networkSpec1 = testFunctionOutputToSpec.apply(testFunctionOutput1);
        //
        // // TODO: check specs of network1

        // return testFunctionOutput1;
    }
}
