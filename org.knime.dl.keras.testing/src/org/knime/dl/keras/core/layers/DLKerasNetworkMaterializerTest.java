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
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Test;
import org.knime.core.util.FileUtil;
import org.knime.core.util.Pair;
import org.knime.dl.core.DLInvalidEnvironmentException;
import org.knime.dl.core.DLInvalidSourceException;
import org.knime.dl.core.DLNetworkLocation;
import org.knime.dl.core.DLNetworkReferenceLocation;
import org.knime.dl.keras.core.DLKerasNetwork;
import org.knime.dl.keras.core.DLKerasNetworkLoader;
import org.knime.dl.keras.core.DLKerasNetworkSpec;
import org.knime.dl.keras.tensorflow.core.DLKerasTensorFlowNetworkLoader;
import org.knime.dl.python.core.DLPythonDefaultNetworkReader;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public final class DLKerasNetworkMaterializerTest extends DLKerasNetworkMaterializerSpecInferrerTestBase {

    private List<File> m_networkSaveFiles;

    private List<DLNetworkLocation> m_networkSaveLocations;

    @After
    public void cleanup() {
        if (m_networkSaveFiles != null) {
            for (final File f : m_networkSaveFiles) {
                f.delete();
            }
            m_networkSaveFiles.clear();
            m_networkSaveFiles = null;
            assert m_networkSaveLocations != null;
            m_networkSaveLocations = null;
        }
    }

    @Test
    public void testMaterializeSingleLayer() {
        testOnSingleLayer(this::materializeAndCheckCommonPostconditions, DLKerasNetwork::getSpec);
    }

    @Test
    public void testMaterializeSequentialModel() {
        testOnSequentialModel(this::materializeAndCheckCommonPostconditions, DLKerasNetwork::getSpec);
    }

    @Test
    public void testMaterializeMultiInputModel() {
        testOnMultiInputModel(this::materializeAndCheckCommonPostconditions, DLKerasNetwork::getSpec);
    }

    @Test
    public void testMaterializeMultiOutputModel() {
        testOnMultiOutputModel(this::materializeAndCheckCommonPostconditions, DLKerasNetwork::getSpec);
    }

    @Test
    public void testMaterializeMultiInputMultiOutputModel() {
        testOnMultiInputMultiOutputModel(this::materializeAndCheckCommonPostconditions, DLKerasNetwork::getSpec);
    }

    @Test
    public void testMaterializeMultiInputMultiOutputForkJoinModel() {
        testOnMultiInputMultiOutputForkJoinModel(this::materializeAndCheckCommonPostconditions,
            DLKerasNetwork::getSpec);
    }

    @Test
    public void testAppendUnaryLayerToSequentialModel()
        throws DLInvalidSourceException, DLInvalidEnvironmentException, IOException {
        testOnSequentialModelAppendedUnaryLayer(this::materializeAndCheckCommonPostconditions, DLKerasNetwork::getSpec);
    }

    @Test
    public void testAppendUnaryLayerToMultiInputMultiOutputModel()
        throws DLInvalidSourceException, DLInvalidEnvironmentException, IOException {
        testOnMultiInputMultiOutputModelAppendedUnaryLayer(this::materializeAndCheckCommonPostconditions,
            DLKerasNetwork::getSpec);
    }

    @Test
    public void testAppendBinaryLayerToSequentialModel()
        throws DLInvalidSourceException, DLInvalidEnvironmentException, IOException {
        testOnSequentialModelAppendedBinaryLayer(this::materializeAndCheckCommonPostconditions,
            DLKerasNetwork::getSpec);
    }

    @Test
    public void testAppendBinaryLayerToMultiInputMultiOutputModel()
        throws DLInvalidSourceException, DLInvalidEnvironmentException, IOException {
        testOnMultiInputMultiOutputModelAppendedBinaryLayer(this::materializeAndCheckCommonPostconditions,
            DLKerasNetwork::getSpec);
    }

    @Test
    public void testAppendBinaryLayerToTwoMultiInputMultiOutputModels()
        throws DLInvalidSourceException, DLInvalidEnvironmentException, IOException {
        testOnTwoMultiInputMultiOutputModelsAppendedBinaryLayer(this::materializeAndCheckCommonPostconditions,
            DLKerasNetwork::getSpec);
    }

    @Test
    public void testAppendMultipleLayersToMultipleNetworks()
        throws DLInvalidSourceException, DLInvalidEnvironmentException, IOException {
        testOnMultipleNetworksMultipleAppendedLayers(this::materializeAndCheckCommonPostconditions,
            DLKerasNetwork::getSpec);
    }

    private DLKerasNetwork materializeAndCheckCommonPostconditions(final List<DLKerasLayer> outputLayers) {
        try {
            final Pair<File, DLNetworkLocation> pair = createNetworkSaveFile();
            final DLKerasNetwork network = new DLKerasNetworkMaterializer(outputLayers, pair.getSecond()).materialize();
            final DLKerasNetworkSpec networkSpec = network.getSpec();

            assert pair.getFirst().length() > 0;
            assert network.getSource().equals(pair.getSecond());

            final DLKerasNetworkSpec rereadNetworkSpec =
                new DLPythonDefaultNetworkReader<>(new DLKerasTensorFlowNetworkLoader()).read(pair.getSecond(), false)
                    .getSpec();
            assert networkSpec.equals(rereadNetworkSpec);

            return network;
        } catch (final Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private Pair<File, DLNetworkLocation> createNetworkSaveFile() throws IOException {
        if (m_networkSaveFiles == null) {
            m_networkSaveFiles = new ArrayList<>();
            assert m_networkSaveLocations == null;
            m_networkSaveLocations = new ArrayList<>();
        }
        final File networkSaveFile =
            FileUtil.createTempFile("dl-keras-materializer-test", "." + DLKerasNetworkLoader.SAVE_MODEL_URL_EXTENSION);
        assert networkSaveFile.length() == 0;
        m_networkSaveFiles.add(networkSaveFile);
        final DLNetworkReferenceLocation networkSaveLocation = new DLNetworkReferenceLocation(networkSaveFile.toURI());
        m_networkSaveLocations.add(networkSaveLocation);
        return new Pair<>(networkSaveFile, networkSaveLocation);
    }
}
