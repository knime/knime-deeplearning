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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.knime.core.util.FileUtil;
import org.knime.dl.core.DLInvalidEnvironmentException;
import org.knime.dl.core.DLInvalidSourceException;
import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.keras.core.DLKerasNetwork;
import org.knime.dl.keras.core.DLKerasNetworkLoader;
import org.knime.dl.keras.core.DLKerasNetworkSpec;
import org.knime.dl.keras.core.layers.impl.DLKerasAddLayer;
import org.knime.dl.keras.core.layers.impl.DLKerasDefaultInputLayer;
import org.knime.dl.keras.core.layers.impl.DLKerasDenseLayer;
import org.knime.dl.keras.tensorflow.core.DLKerasTensorFlowNetworkLoader;
import org.knime.dl.python.core.DLPythonDefaultNetworkReader;
import org.knime.dl.util.DLUtils;
import org.knime.python2.PythonPreferencePage;
import org.osgi.service.prefs.BackingStoreException;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public class DLKerasNetworkMaterializerTest {

    private final URL m_sequentialBaseNetwork;

    private final URL m_multiInputMultiOutputBaseNetwork0;

    private final URL m_multiInputMultiOutputBaseNetwork1;

    public DLKerasNetworkMaterializerTest() throws MalformedURLException, IOException {
        m_sequentialBaseNetwork =
            DLUtils.Files.getFileFromSameBundle(this, "data/simple_test_model.h5").toURI().toURL();
        m_multiInputMultiOutputBaseNetwork0 =
            DLUtils.Files.getFileFromSameBundle(this, "data/3in_3out.h5").toURI().toURL();
        m_multiInputMultiOutputBaseNetwork1 =
            DLUtils.Files.getFileFromSameBundle(this, "data/multi_in_out.h5").toURI().toURL();
    }

    @Before
    public void setupPython() throws BackingStoreException, MalformedURLException, IOException {
        final IEclipsePreferences prefs = DefaultScope.INSTANCE.getNode("org.knime.python2");
        // TODO: remove
        prefs.put(PythonPreferencePage.PYTHON_3_PATH_CFG, "/home/marcel/python-configs/knime_keras_py36.sh");
        prefs.flush();
    }

    @Test
    public void testMaterializeSingleLayer()
        throws IOException, DLInvalidEnvironmentException, DLInvalidSourceException {
        final DLKerasDefaultInputLayer inout0 = new DLKerasDefaultInputLayer();

        final File networkSaveFile =
            FileUtil.createTempFile("keras-materialize-single", "." + DLKerasNetworkLoader.SAVE_MODEL_URL_EXTENSION);
        final URL networkSaveUrl = networkSaveFile.toURI().toURL();

        assert networkSaveFile.length() == 0;

        final DLKerasNetwork network =
            new DLKerasNetworkMaterializer(Arrays.asList(inout0), networkSaveUrl).materialize();
        final DLKerasNetworkSpec networkSpec = network.getSpec();

        assert networkSaveFile.length() > 0;
        assert network.getSource().equals(networkSaveUrl);

        final DLKerasNetworkSpec rereadNetworkSpec =
            new DLPythonDefaultNetworkReader<>(new DLKerasTensorFlowNetworkLoader()).read(networkSaveUrl, false)
                .getSpec();
        assert networkSpec.equals(rereadNetworkSpec);

        networkSaveFile.delete();

        final DLTensorSpec[] inputSpecs = networkSpec.getInputSpecs();
        assert inputSpecs.length == 1;

        final DLTensorSpec inputSpec0 = inputSpecs[0];
        assert inputSpec0.getName().equals("input_1:0");
        assert inputSpec0.getBatchSize().getAsLong() == 32;
        final long[] inputShape0 = DLUtils.Shapes.getFixedShape(inputSpec0.getShape()).get();
        assert inputShape0.length == 1;
        assert inputShape0[0] == 1;
        assert inputSpec0.getElementType() == float.class;

        final DLTensorSpec[] outputSpecs = network.getSpec().getOutputSpecs();
        assert outputSpecs.length == 1;

        final DLTensorSpec outputSpec0 = outputSpecs[0];
        assert outputSpec0.getName().equals("input_1:0");
        assert outputSpec0.getBatchSize().getAsLong() == 32;
        final long[] outputShape0 = DLUtils.Shapes.getFixedShape(outputSpec0.getShape()).get();
        assert outputShape0.length == 1;
        assert outputShape0[0] == 1;
        assert outputSpec0.getElementType() == float.class;
    }

    @Test
    public void testMaterializeSequentialModel()
        throws IOException, DLInvalidEnvironmentException, DLInvalidSourceException {
        final DLKerasDefaultInputLayer in0 = new DLKerasDefaultInputLayer();

        final DLKerasDenseLayer hidden0 = new DLKerasDenseLayer();
        hidden0.setParent(0, in0);

        final DLKerasDenseLayer hidden1 = new DLKerasDenseLayer();
        hidden1.setParent(0, hidden0);

        final DLKerasDenseLayer hidden2 = new DLKerasDenseLayer();
        hidden2.setParent(0, hidden1);

        final DLKerasDenseLayer out0 = new DLKerasDenseLayer();
        out0.setParent(0, hidden2);

        final File networkSaveFile =
            FileUtil.createTempFile("keras-materialize-seq", "." + DLKerasNetworkLoader.SAVE_MODEL_URL_EXTENSION);
        final URL networkSaveUrl = networkSaveFile.toURI().toURL();

        assert networkSaveFile.length() == 0;

        final DLKerasNetwork network =
            new DLKerasNetworkMaterializer(Arrays.asList(out0), networkSaveUrl).materialize();
        final DLKerasNetworkSpec networkSpec = network.getSpec();

        assert networkSaveFile.length() > 0;
        assert network.getSource().equals(networkSaveUrl);

        final DLKerasNetworkSpec rereadNetworkSpec =
            new DLPythonDefaultNetworkReader<>(new DLKerasTensorFlowNetworkLoader()).read(networkSaveUrl, false)
                .getSpec();
        assert networkSpec.equals(rereadNetworkSpec);

        networkSaveFile.delete();

        final DLTensorSpec[] inputSpecs = networkSpec.getInputSpecs();
        assert inputSpecs.length == 1;

        final DLTensorSpec inputSpec0 = inputSpecs[0];
        assert inputSpec0.getName().equals("input_1:0");
        assert inputSpec0.getBatchSize().getAsLong() == 32;
        final long[] inputShape0 = DLUtils.Shapes.getFixedShape(inputSpec0.getShape()).get();
        assert inputShape0.length == 1;
        assert inputShape0[0] == 1;
        assert inputSpec0.getElementType() == float.class;

        final DLTensorSpec[] outputSpecs = network.getSpec().getOutputSpecs();
        assert outputSpecs.length == 1;

        final DLTensorSpec outputSpec0 = outputSpecs[0];
        assert outputSpec0.getName().equals("dense_4/BiasAdd:0");
        assert outputSpec0.getBatchSize().getAsLong() == 32;
        final long[] outputShape0 = DLUtils.Shapes.getFixedShape(outputSpec0.getShape()).get();
        assert outputShape0.length == 1;
        assert outputShape0[0] == 1;
        assert outputSpec0.getElementType() == float.class;
    }

    @Test
    public void testMaterializeMultiInputModel()
        throws IOException, DLInvalidEnvironmentException, DLInvalidSourceException {
        final DLKerasDefaultInputLayer in0 = new DLKerasDefaultInputLayer();

        final DLKerasDefaultInputLayer in1 = new DLKerasDefaultInputLayer();

        final DLKerasAddLayer hidden0 = new DLKerasAddLayer();
        hidden0.setParent(0, in0);
        hidden0.setParent(1, in1);

        final DLKerasDenseLayer hidden1 = new DLKerasDenseLayer();
        hidden1.setParent(0, hidden0);

        final DLKerasDenseLayer hidden2 = new DLKerasDenseLayer();
        hidden2.setParent(0, hidden1);

        final DLKerasDenseLayer out0 = new DLKerasDenseLayer();
        out0.setParent(0, hidden2);

        final File networkSaveFile =
            FileUtil.createTempFile("keras-materialize-mult-in", "." + DLKerasNetworkLoader.SAVE_MODEL_URL_EXTENSION);
        final URL networkSaveUrl = networkSaveFile.toURI().toURL();

        assert networkSaveFile.length() == 0;

        final DLKerasNetwork network =
            new DLKerasNetworkMaterializer(Arrays.asList(out0), networkSaveUrl).materialize();
        final DLKerasNetworkSpec networkSpec = network.getSpec();

        assert networkSaveFile.length() > 0;
        assert network.getSource().equals(networkSaveUrl);

        final DLKerasNetworkSpec rereadNetworkSpec =
            new DLPythonDefaultNetworkReader<>(new DLKerasTensorFlowNetworkLoader()).read(networkSaveUrl, false)
                .getSpec();
        assert networkSpec.equals(rereadNetworkSpec);

        networkSaveFile.delete();

        final DLTensorSpec[] inputSpecs = networkSpec.getInputSpecs();
        assert inputSpecs.length == 2;

        final DLTensorSpec inputSpec0 = inputSpecs[0];
        assert inputSpec0.getName().equals("input_1:0");
        assert inputSpec0.getBatchSize().getAsLong() == 32;
        final long[] inputShape0 = DLUtils.Shapes.getFixedShape(inputSpec0.getShape()).get();
        assert inputShape0.length == 1;
        assert inputShape0[0] == 1;
        assert inputSpec0.getElementType() == float.class;

        final DLTensorSpec inputSpec1 = inputSpecs[1];
        assert inputSpec1.getName().equals("input_2:0");
        assert inputSpec1.getBatchSize().getAsLong() == 32;
        final long[] inputShape1 = DLUtils.Shapes.getFixedShape(inputSpec1.getShape()).get();
        assert inputShape1.length == 1;
        assert inputShape1[0] == 1;
        assert inputSpec1.getElementType() == float.class;

        final DLTensorSpec[] outputSpecs = network.getSpec().getOutputSpecs();
        assert outputSpecs.length == 1;

        final DLTensorSpec outputSpec0 = outputSpecs[0];
        assert outputSpec0.getName().equals("dense_3/BiasAdd:0");
        assert outputSpec0.getBatchSize().getAsLong() == 32;
        final long[] outputShape0 = DLUtils.Shapes.getFixedShape(outputSpec0.getShape()).get();
        assert outputShape0.length == 1;
        assert outputShape0[0] == 1;
        assert outputSpec0.getElementType() == float.class;
    }

    @Test
    public void testMaterializeMultiOutputModel()
        throws IOException, DLInvalidEnvironmentException, DLInvalidSourceException {
        final DLKerasDefaultInputLayer in0 = new DLKerasDefaultInputLayer();

        final DLKerasDenseLayer hidden0 = new DLKerasDenseLayer();
        hidden0.setParent(0, in0);

        final DLKerasDenseLayer hidden1 = new DLKerasDenseLayer();
        hidden1.setParent(0, hidden0);

        final DLKerasDenseLayer hidden2 = new DLKerasDenseLayer();
        hidden2.setParent(0, hidden1);

        final DLKerasDenseLayer out0 = new DLKerasDenseLayer();
        out0.setParent(0, hidden2);

        final DLKerasDenseLayer out1 = new DLKerasDenseLayer();
        out1.setParent(0, hidden2);

        final File networkSaveFile =
            FileUtil.createTempFile("keras-materialize-mult-out", "." + DLKerasNetworkLoader.SAVE_MODEL_URL_EXTENSION);
        final URL networkSaveUrl = networkSaveFile.toURI().toURL();

        assert networkSaveFile.length() == 0;

        final DLKerasNetwork network =
            new DLKerasNetworkMaterializer(Arrays.asList(out0, out1), networkSaveUrl).materialize();
        final DLKerasNetworkSpec networkSpec = network.getSpec();

        assert networkSaveFile.length() > 0;
        assert network.getSource().equals(networkSaveUrl);

        final DLKerasNetworkSpec rereadNetworkSpec =
            new DLPythonDefaultNetworkReader<>(new DLKerasTensorFlowNetworkLoader()).read(networkSaveUrl, false)
                .getSpec();
        assert networkSpec.equals(rereadNetworkSpec);

        networkSaveFile.delete();

        final DLTensorSpec[] inputSpecs = networkSpec.getInputSpecs();
        assert inputSpecs.length == 1;

        final DLTensorSpec inputSpec0 = inputSpecs[0];
        assert inputSpec0.getName().equals("input_1:0");
        assert inputSpec0.getBatchSize().getAsLong() == 32;
        final long[] inputShape0 = DLUtils.Shapes.getFixedShape(inputSpec0.getShape()).get();
        assert inputShape0.length == 1;
        assert inputShape0[0] == 1;
        assert inputSpec0.getElementType() == float.class;

        final DLTensorSpec[] outputSpecs = network.getSpec().getOutputSpecs();
        assert outputSpecs.length == 2;

        final DLTensorSpec outputSpec0 = outputSpecs[0];
        assert outputSpec0.getName().equals("dense_4/BiasAdd:0");
        assert outputSpec0.getBatchSize().getAsLong() == 32;
        final long[] outputShape0 = DLUtils.Shapes.getFixedShape(outputSpec0.getShape()).get();
        assert outputShape0.length == 1;
        assert outputShape0[0] == 1;
        assert outputSpec0.getElementType() == float.class;

        final DLTensorSpec outputSpec1 = outputSpecs[1];
        assert outputSpec1.getName().equals("dense_5/BiasAdd:0");
        assert outputSpec1.getBatchSize().getAsLong() == 32;
        final long[] outputShape1 = DLUtils.Shapes.getFixedShape(outputSpec1.getShape()).get();
        assert outputShape1.length == 1;
        assert outputShape1[0] == 1;
        assert outputSpec1.getElementType() == float.class;
    }

    @Test
    public void testMaterializeMultiInputMultiOutputModel()
        throws IOException, DLInvalidEnvironmentException, DLInvalidSourceException {
        final DLKerasDefaultInputLayer in0 = new DLKerasDefaultInputLayer();

        final DLKerasDefaultInputLayer in1 = new DLKerasDefaultInputLayer();

        final DLKerasAddLayer hidden0 = new DLKerasAddLayer();
        hidden0.setParent(0, in0);
        hidden0.setParent(1, in1);

        final DLKerasDenseLayer hidden1 = new DLKerasDenseLayer();
        hidden1.setParent(0, hidden0);

        final DLKerasDenseLayer hidden2 = new DLKerasDenseLayer();
        hidden2.setParent(0, hidden1);

        final DLKerasDenseLayer out0 = new DLKerasDenseLayer();
        out0.setParent(0, hidden2);

        final DLKerasDenseLayer out1 = new DLKerasDenseLayer();
        out1.setParent(0, hidden2);

        final File networkSaveFile = FileUtil.createTempFile("keras-materialize-mult-in-out",
            "." + DLKerasNetworkLoader.SAVE_MODEL_URL_EXTENSION);
        final URL networkSaveUrl = networkSaveFile.toURI().toURL();

        assert networkSaveFile.length() == 0;

        final DLKerasNetwork network =
            new DLKerasNetworkMaterializer(Arrays.asList(out0, out1), networkSaveUrl).materialize();
        final DLKerasNetworkSpec networkSpec = network.getSpec();

        assert networkSaveFile.length() > 0;
        assert network.getSource().equals(networkSaveUrl);

        final DLKerasNetworkSpec rereadNetworkSpec =
            new DLPythonDefaultNetworkReader<>(new DLKerasTensorFlowNetworkLoader()).read(networkSaveUrl, false)
                .getSpec();
        assert networkSpec.equals(rereadNetworkSpec);

        networkSaveFile.delete();

        final DLTensorSpec[] inputSpecs = networkSpec.getInputSpecs();
        assert inputSpecs.length == 2;

        final DLTensorSpec inputSpec0 = inputSpecs[0];
        assert inputSpec0.getName().equals("input_1:0");
        assert inputSpec0.getBatchSize().getAsLong() == 32;
        final long[] inputShape0 = DLUtils.Shapes.getFixedShape(inputSpec0.getShape()).get();
        assert inputShape0.length == 1;
        assert inputShape0[0] == 1;
        assert inputSpec0.getElementType() == float.class;

        final DLTensorSpec inputSpec1 = inputSpecs[1];
        assert inputSpec1.getName().equals("input_2:0");
        assert inputSpec1.getBatchSize().getAsLong() == 32;
        final long[] inputShape1 = DLUtils.Shapes.getFixedShape(inputSpec1.getShape()).get();
        assert inputShape1.length == 1;
        assert inputShape1[0] == 1;
        assert inputSpec1.getElementType() == float.class;

        final DLTensorSpec[] outputSpecs = network.getSpec().getOutputSpecs();
        assert outputSpecs.length == 2;

        final DLTensorSpec outputSpec0 = outputSpecs[0];
        assert outputSpec0.getName().equals("dense_3/BiasAdd:0");
        assert outputSpec0.getBatchSize().getAsLong() == 32;
        final long[] outputShape0 = DLUtils.Shapes.getFixedShape(outputSpec0.getShape()).get();
        assert outputShape0.length == 1;
        assert outputShape0[0] == 1;
        assert outputSpec0.getElementType() == float.class;

        final DLTensorSpec outputSpec1 = outputSpecs[1];
        assert outputSpec1.getName().equals("dense_4/BiasAdd:0");
        assert outputSpec1.getBatchSize().getAsLong() == 32;
        final long[] outputShape1 = DLUtils.Shapes.getFixedShape(outputSpec1.getShape()).get();
        assert outputShape1.length == 1;
        assert outputShape1[0] == 1;
        assert outputSpec1.getElementType() == float.class;
    }

    @Test
    public void testMaterializeMultiInputMultiOutputForkJoinModel()
        throws IOException, DLInvalidEnvironmentException, DLInvalidSourceException {
        final DLKerasDefaultInputLayer in0 = new DLKerasDefaultInputLayer();

        final DLKerasDefaultInputLayer in1 = new DLKerasDefaultInputLayer();

        final DLKerasDefaultInputLayer in2 = new DLKerasDefaultInputLayer();

        final DLKerasDenseLayer hidden0 = new DLKerasDenseLayer();
        hidden0.setParent(0, in0);

        final DLKerasAddLayer hidden1 = new DLKerasAddLayer();
        hidden1.setParent(0, hidden0);
        hidden1.setParent(1, in1);

        final DLKerasAddLayer hidden2 = new DLKerasAddLayer();
        hidden2.setParent(0, hidden1);
        hidden2.setParent(1, in2);

        final DLKerasAddLayer hidden3 = new DLKerasAddLayer();
        hidden3.setParent(0, hidden0);
        hidden3.setParent(1, hidden2);

        final DLKerasDenseLayer hidden4 = new DLKerasDenseLayer();
        hidden4.setParent(0, hidden2);

        final DLKerasDenseLayer hidden5 = new DLKerasDenseLayer();
        hidden5.setParent(0, hidden3);

        final DLKerasDenseLayer hidden6 = new DLKerasDenseLayer();
        hidden6.setParent(0, hidden3);

        final DLKerasDenseLayer hidden7 = new DLKerasDenseLayer();
        hidden7.setParent(0, hidden4);

        final DLKerasDenseLayer out0 = new DLKerasDenseLayer();
        out0.setParent(0, hidden5);

        final DLKerasDenseLayer out1 = new DLKerasDenseLayer();
        out1.setParent(0, hidden5);

        final DLKerasAddLayer out2 = new DLKerasAddLayer();
        out2.setParent(0, hidden6);
        out2.setParent(1, hidden7);

        final File networkSaveFile =
            FileUtil.createTempFile("keras-materialize-fork-join", "." + DLKerasNetworkLoader.SAVE_MODEL_URL_EXTENSION);
        final URL networkSaveUrl = networkSaveFile.toURI().toURL();

        assert networkSaveFile.length() == 0;

        final DLKerasNetwork network =
            new DLKerasNetworkMaterializer(Arrays.asList(out0, out1, out2), networkSaveUrl).materialize();
        final DLKerasNetworkSpec networkSpec = network.getSpec();

        assert networkSaveFile.length() > 0;
        assert network.getSource().equals(networkSaveUrl);

        final DLKerasNetworkSpec rereadNetworkSpec =
            new DLPythonDefaultNetworkReader<>(new DLKerasTensorFlowNetworkLoader()).read(networkSaveUrl, false)
                .getSpec();
        assert networkSpec.equals(rereadNetworkSpec);

        networkSaveFile.delete();

        final DLTensorSpec[] inputSpecs = networkSpec.getInputSpecs();
        assert inputSpecs.length == 3;

        final DLTensorSpec inputSpec0 = inputSpecs[0];
        assert inputSpec0.getName().equals("input_1:0");
        assert inputSpec0.getBatchSize().getAsLong() == 32;
        final long[] inputShape0 = DLUtils.Shapes.getFixedShape(inputSpec0.getShape()).get();
        assert inputShape0.length == 1;
        assert inputShape0[0] == 1;
        assert inputSpec0.getElementType() == float.class;

        final DLTensorSpec inputSpec1 = inputSpecs[1];
        assert inputSpec1.getName().equals("input_2:0");
        assert inputSpec1.getBatchSize().getAsLong() == 32;
        final long[] inputShape1 = DLUtils.Shapes.getFixedShape(inputSpec1.getShape()).get();
        assert inputShape1.length == 1;
        assert inputShape1[0] == 1;
        assert inputSpec1.getElementType() == float.class;

        final DLTensorSpec inputSpec2 = inputSpecs[2];
        assert inputSpec2.getName().equals("input_3:0");
        assert inputSpec2.getBatchSize().getAsLong() == 32;
        final long[] inputShape2 = DLUtils.Shapes.getFixedShape(inputSpec2.getShape()).get();
        assert inputShape2.length == 1;
        assert inputShape2[0] == 1;
        assert inputSpec2.getElementType() == float.class;

        final DLTensorSpec[] outputSpecs = networkSpec.getOutputSpecs();
        assert outputSpecs.length == 3;

        final DLTensorSpec outputSpec0 = outputSpecs[0];
        assert outputSpec0.getName().equals("dense_6/BiasAdd:0");
        assert outputSpec0.getBatchSize().getAsLong() == 32;
        final long[] outputShape0 = DLUtils.Shapes.getFixedShape(outputSpec0.getShape()).get();
        assert outputShape0.length == 1;
        assert outputShape0[0] == 1;
        assert outputSpec0.getElementType() == float.class;

        final DLTensorSpec outputSpec1 = outputSpecs[1];
        assert outputSpec1.getName().equals("dense_7/BiasAdd:0");
        assert outputSpec1.getBatchSize().getAsLong() == 32;
        final long[] outputShape1 = DLUtils.Shapes.getFixedShape(outputSpec1.getShape()).get();
        assert outputShape1.length == 1;
        assert outputShape1[0] == 1;
        assert outputSpec1.getElementType() == float.class;

        final DLTensorSpec outputSpec2 = outputSpecs[2];
        assert outputSpec2.getName().equals("add_4/add:0");
        assert outputSpec2.getBatchSize().getAsLong() == 32;
        final long[] outputShape2 = DLUtils.Shapes.getFixedShape(outputSpec2.getShape()).get();
        assert outputShape2.length == 1;
        assert outputShape2[0] == 1;
        assert outputSpec2.getElementType() == float.class;
    }

    @Test
    public void testAppendUnaryLayerToSequentialModel()
        throws IOException, DLInvalidEnvironmentException, DLInvalidSourceException {
        final DLKerasNetwork baseNetwork = new DLPythonDefaultNetworkReader<>(new DLKerasTensorFlowNetworkLoader())
            .read(m_sequentialBaseNetwork, false);

        final DLKerasDefaultBaseNetworkTensorSpecOutput baseNetworkOut0 =
            new DLKerasDefaultBaseNetworkTensorSpecOutput(baseNetwork, 0);

        final DLKerasDenseLayer out0 = new DLKerasDenseLayer();
        out0.setParent(0, baseNetworkOut0);

        final File networkSaveFile =
            FileUtil.createTempFile("keras-append-un-seq", "." + DLKerasNetworkLoader.SAVE_MODEL_URL_EXTENSION);
        final URL networkSaveUrl = networkSaveFile.toURI().toURL();

        assert networkSaveFile.length() == 0;

        final DLKerasNetwork network =
            new DLKerasNetworkMaterializer(Arrays.asList(out0), networkSaveUrl).materialize();
        final DLKerasNetworkSpec networkSpec = network.getSpec();

        assert networkSaveFile.length() > 0;
        assert network.getSource().equals(networkSaveUrl);

        final DLKerasNetworkSpec rereadNetworkSpec =
            new DLPythonDefaultNetworkReader<>(new DLKerasTensorFlowNetworkLoader()).read(networkSaveUrl, false)
                .getSpec();
        assert networkSpec.equals(rereadNetworkSpec);

        networkSaveFile.delete();

        final DLTensorSpec[] inputSpecs = networkSpec.getInputSpecs();
        assert inputSpecs.length == 1;

        assert inputSpecs[0].equals(baseNetwork.getSpec().getInputSpecs()[0]);

        final DLTensorSpec[] outputSpecs = network.getSpec().getOutputSpecs();
        assert outputSpecs.length == 1;

        final DLTensorSpec outputSpec0 = outputSpecs[0];
        assert outputSpec0.getName().equals("dense_4/BiasAdd:0");
        assert !outputSpec0.getBatchSize().isPresent();
        final long[] outputShape0 = DLUtils.Shapes.getFixedShape(outputSpec0.getShape()).get();
        assert outputShape0.length == 1;
        assert outputShape0[0] == 1;
        assert outputSpec0.getElementType() == float.class;
    }

    @Test
    public void testAppendUnaryLayerToMultiInputMultiOutputModel()
        throws DLInvalidSourceException, DLInvalidEnvironmentException, IOException {
        final DLKerasNetwork baseNetwork = new DLPythonDefaultNetworkReader<>(new DLKerasTensorFlowNetworkLoader())
            .read(m_multiInputMultiOutputBaseNetwork0, false);

        final DLKerasDefaultBaseNetworkTensorSpecOutput baseNetworkOut0 =
            new DLKerasDefaultBaseNetworkTensorSpecOutput(baseNetwork, 1);

        final DLKerasDenseLayer out0 = new DLKerasDenseLayer();
        out0.setParent(0, baseNetworkOut0);

        final File networkSaveFile =
            FileUtil.createTempFile("keras-append-un-mult", "." + DLKerasNetworkLoader.SAVE_MODEL_URL_EXTENSION);
        final URL networkSaveUrl = networkSaveFile.toURI().toURL();

        assert networkSaveFile.length() == 0;

        final DLKerasNetwork network =
            new DLKerasNetworkMaterializer(Arrays.asList(out0), networkSaveUrl).materialize();
        final DLKerasNetworkSpec networkSpec = network.getSpec();

        assert networkSaveFile.length() > 0;
        assert network.getSource().equals(networkSaveUrl);

        final DLKerasNetworkSpec rereadNetworkSpec =
            new DLPythonDefaultNetworkReader<>(new DLKerasTensorFlowNetworkLoader()).read(networkSaveUrl, false)
                .getSpec();
        assert networkSpec.equals(rereadNetworkSpec);

        networkSaveFile.delete();

        final DLTensorSpec[] inputSpecs = networkSpec.getInputSpecs();
        assert inputSpecs.length == 3;

        assert inputSpecs[0].equals(baseNetwork.getSpec().getInputSpecs()[0]);
        assert inputSpecs[1].equals(baseNetwork.getSpec().getInputSpecs()[1]);
        assert inputSpecs[2].equals(baseNetwork.getSpec().getInputSpecs()[2]);

        final DLTensorSpec[] outputSpecs = network.getSpec().getOutputSpecs();
        assert outputSpecs.length == 3;

        assert outputSpecs[0].equals(baseNetwork.getSpec().getOutputSpecs()[0]);

        final DLTensorSpec outputSpec1 = outputSpecs[1];
        assert outputSpec1.getName().equals("dense_7/BiasAdd:0");
        assert !outputSpec1.getBatchSize().isPresent();
        final long[] outputShape1 = DLUtils.Shapes.getFixedShape(outputSpec1.getShape()).get();
        assert outputShape1.length == 1;
        assert outputShape1[0] == 1;
        assert outputSpec1.getElementType() == float.class;

        assert outputSpecs[2].equals(baseNetwork.getSpec().getOutputSpecs()[2]);
    }

    @Test
    public void testAppendBinaryLayerToSequentialModel()
        throws DLInvalidSourceException, DLInvalidEnvironmentException, IOException {
        final DLKerasNetwork baseNetwork = new DLPythonDefaultNetworkReader<>(new DLKerasTensorFlowNetworkLoader())
            .read(m_sequentialBaseNetwork, false);

        final DLKerasDefaultBaseNetworkTensorSpecOutput baseNetworkOut0 =
            new DLKerasDefaultBaseNetworkTensorSpecOutput(baseNetwork, 0);

        final DLKerasAddLayer out0 = new DLKerasAddLayer();
        out0.setParent(0, baseNetworkOut0);
        out0.setParent(1, baseNetworkOut0);

        final File networkSaveFile =
            FileUtil.createTempFile("keras-append-bi-seq", "." + DLKerasNetworkLoader.SAVE_MODEL_URL_EXTENSION);
        final URL networkSaveUrl = networkSaveFile.toURI().toURL();

        assert networkSaveFile.length() == 0;

        final DLKerasNetwork network =
            new DLKerasNetworkMaterializer(Arrays.asList(out0), networkSaveUrl).materialize();
        final DLKerasNetworkSpec networkSpec = network.getSpec();

        assert networkSaveFile.length() > 0;
        assert network.getSource().equals(networkSaveUrl);

        final DLKerasNetworkSpec rereadNetworkSpec =
            new DLPythonDefaultNetworkReader<>(new DLKerasTensorFlowNetworkLoader()).read(networkSaveUrl, false)
                .getSpec();
        assert networkSpec.equals(rereadNetworkSpec);

        networkSaveFile.delete();

        final DLTensorSpec[] inputSpecs = networkSpec.getInputSpecs();
        assert inputSpecs.length == 1;

        assert inputSpecs[0].equals(baseNetwork.getSpec().getInputSpecs()[0]);

        final DLTensorSpec[] outputSpecs = network.getSpec().getOutputSpecs();
        assert outputSpecs.length == 1;

        final DLTensorSpec outputSpec0 = outputSpecs[0];
        assert outputSpec0.getName().equals("add_1/add:0");
        assert !outputSpec0.getBatchSize().isPresent();
        final long[] outputShape0 = DLUtils.Shapes.getFixedShape(outputSpec0.getShape()).get();
        assert outputShape0.length == 1;
        assert outputShape0[0] == 10;
        assert outputSpec0.getElementType() == float.class;
    }

    @Test
    public void testAppendBinaryLayerToMultiInputMultiOutputModel()
        throws DLInvalidSourceException, DLInvalidEnvironmentException, IOException {
        final DLKerasNetwork baseNetwork = new DLPythonDefaultNetworkReader<>(new DLKerasTensorFlowNetworkLoader())
            .read(m_multiInputMultiOutputBaseNetwork0, false);

        final DLKerasDefaultBaseNetworkTensorSpecOutput baseNetworkOut0 =
            new DLKerasDefaultBaseNetworkTensorSpecOutput(baseNetwork, 0);

        final DLKerasDefaultBaseNetworkTensorSpecOutput baseNetworkOut1 =
            new DLKerasDefaultBaseNetworkTensorSpecOutput(baseNetwork, 1);

        final DLKerasAddLayer out0 = new DLKerasAddLayer();
        out0.setParent(0, baseNetworkOut0);
        out0.setParent(1, baseNetworkOut1);

        final File networkSaveFile =
            FileUtil.createTempFile("keras-append-bi-mult", "." + DLKerasNetworkLoader.SAVE_MODEL_URL_EXTENSION);
        final URL networkSaveUrl = networkSaveFile.toURI().toURL();

        assert networkSaveFile.length() == 0;

        final DLKerasNetwork network =
            new DLKerasNetworkMaterializer(Arrays.asList(out0), networkSaveUrl).materialize();
        final DLKerasNetworkSpec networkSpec = network.getSpec();

        assert networkSaveFile.length() > 0;
        assert network.getSource().equals(networkSaveUrl);

        final DLKerasNetworkSpec rereadNetworkSpec =
            new DLPythonDefaultNetworkReader<>(new DLKerasTensorFlowNetworkLoader()).read(networkSaveUrl, false)
                .getSpec();
        assert networkSpec.equals(rereadNetworkSpec);

        networkSaveFile.delete();

        final DLTensorSpec[] inputSpecs = networkSpec.getInputSpecs();
        assert inputSpecs.length == 3;

        assert inputSpecs[0].equals(baseNetwork.getSpec().getInputSpecs()[0]);
        assert inputSpecs[1].equals(baseNetwork.getSpec().getInputSpecs()[1]);
        assert inputSpecs[2].equals(baseNetwork.getSpec().getInputSpecs()[2]);

        final DLTensorSpec[] outputSpecs = network.getSpec().getOutputSpecs();
        assert outputSpecs.length == 2;

        final DLTensorSpec outputSpec0 = outputSpecs[0];
        assert outputSpec0.getName().equals("add_4/add:0");
        assert !outputSpec0.getBatchSize().isPresent();
        final long[] outputShape0 = DLUtils.Shapes.getFixedShape(outputSpec0.getShape()).get();
        assert outputShape0.length == 1;
        assert outputShape0[0] == 5;
        assert outputSpec0.getElementType() == float.class;

        assert outputSpecs[1].equals(baseNetwork.getSpec().getOutputSpecs()[2]);
    }

    @Test
    public void testAppendBinaryLayerToTwoMultiInputMultiOutputModels()
        throws DLInvalidSourceException, DLInvalidEnvironmentException, IOException {
        final DLKerasNetwork baseNetwork0 = new DLPythonDefaultNetworkReader<>(new DLKerasTensorFlowNetworkLoader())
            .read(m_multiInputMultiOutputBaseNetwork0, false);

        final DLKerasNetwork baseNetwork1 = new DLPythonDefaultNetworkReader<>(new DLKerasTensorFlowNetworkLoader())
            .read(m_multiInputMultiOutputBaseNetwork1, false);

        final DLKerasDefaultBaseNetworkTensorSpecOutput baseNetwork0Out0 =
            new DLKerasDefaultBaseNetworkTensorSpecOutput(baseNetwork0, 2);

        final DLKerasDefaultBaseNetworkTensorSpecOutput baseNetwork1Out0 =
            new DLKerasDefaultBaseNetworkTensorSpecOutput(baseNetwork1, 0);

        final DLKerasAddLayer out0 = new DLKerasAddLayer();
        out0.setParent(0, baseNetwork0Out0);
        out0.setParent(1, baseNetwork1Out0);

        final File networkSaveFile =
            FileUtil.createTempFile("keras-append-bi-2-mults", "." + DLKerasNetworkLoader.SAVE_MODEL_URL_EXTENSION);
        final URL networkSaveUrl = networkSaveFile.toURI().toURL();

        assert networkSaveFile.length() == 0;

        final DLKerasNetwork network =
            new DLKerasNetworkMaterializer(Arrays.asList(out0), networkSaveUrl).materialize();
        final DLKerasNetworkSpec networkSpec = network.getSpec();

        assert networkSaveFile.length() > 0;
        assert network.getSource().equals(networkSaveUrl);

        final DLKerasNetworkSpec rereadNetworkSpec =
            new DLPythonDefaultNetworkReader<>(new DLKerasTensorFlowNetworkLoader()).read(networkSaveUrl, false)
                .getSpec();
        assert networkSpec.equals(rereadNetworkSpec);

        networkSaveFile.delete();

        final DLTensorSpec[] inputSpecs = networkSpec.getInputSpecs();
        assert inputSpecs.length == 5;

        assert inputSpecs[0].equals(baseNetwork0.getSpec().getInputSpecs()[0]);
        assert inputSpecs[1].equals(baseNetwork0.getSpec().getInputSpecs()[1]);
        assert inputSpecs[2].equals(baseNetwork0.getSpec().getInputSpecs()[2]);
        assert inputSpecs[0].equals(baseNetwork1.getSpec().getInputSpecs()[0]);
        assert inputSpecs[1].equals(baseNetwork1.getSpec().getInputSpecs()[1]);

        final DLTensorSpec[] outputSpecs = network.getSpec().getOutputSpecs();
        assert outputSpecs.length == 4;

        assert outputSpecs[0].equals(baseNetwork0.getSpec().getOutputSpecs()[0]);
        assert outputSpecs[1].equals(baseNetwork0.getSpec().getOutputSpecs()[1]);

        final DLTensorSpec outputSpec2 = outputSpecs[2];
        assert outputSpec2.getName().equals("add_3/add:0");
        assert !outputSpec2.getBatchSize().isPresent();
        final long[] outputShape2 = DLUtils.Shapes.getFixedShape(outputSpec2.getShape()).get();
        assert outputShape2.length == 1;
        assert outputShape2[0] == 5;
        assert outputSpec2.getElementType() == float.class;

        assert outputSpecs[3].equals(baseNetwork1.getSpec().getOutputSpecs()[1]);
    }

    @Test
    public void testAppendMultipleLayersToMultipleNetworks()
        throws DLInvalidSourceException, DLInvalidEnvironmentException, IOException {
        final DLKerasNetwork baseNetwork0 = new DLPythonDefaultNetworkReader<>(new DLKerasTensorFlowNetworkLoader())
            .read(m_sequentialBaseNetwork, false);

        final DLKerasNetwork baseNetwork1 = new DLPythonDefaultNetworkReader<>(new DLKerasTensorFlowNetworkLoader())
            .read(m_multiInputMultiOutputBaseNetwork0, false);

        final DLKerasNetwork baseNetwork2 = new DLPythonDefaultNetworkReader<>(new DLKerasTensorFlowNetworkLoader())
            .read(m_multiInputMultiOutputBaseNetwork1, false);

        final DLKerasDefaultBaseNetworkTensorSpecOutput baseNetwork0Out0 =
            new DLKerasDefaultBaseNetworkTensorSpecOutput(baseNetwork0, 0);

        final DLKerasDefaultBaseNetworkTensorSpecOutput baseNetwork1Out0 =
            new DLKerasDefaultBaseNetworkTensorSpecOutput(baseNetwork1, 0);

        final DLKerasDefaultBaseNetworkTensorSpecOutput baseNetwork1Out1 =
            new DLKerasDefaultBaseNetworkTensorSpecOutput(baseNetwork1, 1);

        final DLKerasDefaultBaseNetworkTensorSpecOutput baseNetwork2Out0 =
            new DLKerasDefaultBaseNetworkTensorSpecOutput(baseNetwork2, 0);

        // First generated network:

        final DLKerasAddLayer hidden0_0 = new DLKerasAddLayer();
        hidden0_0.setParent(0, baseNetwork0Out0);
        hidden0_0.setParent(1, baseNetwork1Out0);

        final DLKerasAddLayer hidden0_1 = new DLKerasAddLayer();
        hidden0_1.setParent(0, baseNetwork1Out1);
        hidden0_1.setParent(1, baseNetwork2Out0);

        final DLKerasAddLayer out0_0 = new DLKerasAddLayer();
        out0_0.setParent(0, hidden0_0);
        out0_0.setParent(1, hidden0_1);

        final File networkSaveFile0 = FileUtil.createTempFile("keras-append-mult-to-mult-0-",
            "." + DLKerasNetworkLoader.SAVE_MODEL_URL_EXTENSION);
        final URL networkSaveUrl0 = networkSaveFile0.toURI().toURL();

        assert networkSaveFile0.length() == 0;

        final DLKerasNetwork network0 =
            new DLKerasNetworkMaterializer(Arrays.asList(out0_0), networkSaveUrl0).materialize();
        final DLKerasNetworkSpec networkSpec0 = network0.getSpec();

        assert networkSaveFile0.length() > 0;
        assert network0.getSource().equals(networkSaveUrl0);

        final DLKerasNetworkSpec rereadNetworkSpec0 =
            new DLPythonDefaultNetworkReader<>(new DLKerasTensorFlowNetworkLoader()).read(networkSaveUrl0, false)
                .getSpec();
        assert networkSpec0.equals(rereadNetworkSpec0);

        // Second generated network:

        final DLKerasAddLayer hidden1_0 = new DLKerasAddLayer();
        hidden1_0.setParent(0, baseNetwork0Out0);
        hidden1_0.setParent(0, baseNetwork2Out0);

        final DLKerasDenseLayer out1_0 = new DLKerasDenseLayer();
        out1_0.setParent(0, hidden1_0);

        final File networkSaveFile1 = FileUtil.createTempFile("keras-append-mult-to-mult-1-",
            "." + DLKerasNetworkLoader.SAVE_MODEL_URL_EXTENSION);
        final URL networkSaveUrl1 = networkSaveFile1.toURI().toURL();

        assert networkSaveFile1.length() == 0;

        final DLKerasNetwork network1 =
            new DLKerasNetworkMaterializer(Arrays.asList(out1_0), networkSaveUrl1).materialize();
        final DLKerasNetworkSpec networkSpec1 = network1.getSpec();

        assert networkSaveFile1.length() > 0;
        assert network1.getSource().equals(networkSaveUrl1);

        final DLKerasNetworkSpec rereadNetworkSpec1 =
            new DLPythonDefaultNetworkReader<>(new DLKerasTensorFlowNetworkLoader()).read(networkSaveUrl1, false)
                .getSpec();
        assert networkSpec1.equals(rereadNetworkSpec1);

        networkSaveFile0.delete();
        networkSaveFile1.delete();

        // TODO: check specs of network0 and network1
        Assert.fail();
    }
}
