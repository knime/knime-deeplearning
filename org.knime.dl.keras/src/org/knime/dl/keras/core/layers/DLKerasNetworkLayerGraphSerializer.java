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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.knime.core.data.filestore.FileStore;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsWO;
import org.knime.dl.core.DLNetworkFileStoreLocation;
import org.knime.dl.core.DLNetworkLocation;
import org.knime.dl.core.DLNetworkReferenceLocation;
import org.knime.dl.keras.core.DLKerasNetworkSpec;
import org.knime.dl.keras.core.layers.DLKerasNetworkLayerGraphIterator.DLKerasLayerVisitor;

import gnu.trove.TIntArrayList;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public final class DLKerasNetworkLayerGraphSerializer {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(DLKerasNetworkLayerGraphSerializer.class);

    private static final String CFG_KEY_GRAPH = "layer_graph";

    private static final String CFG_KEY_LAYER_CLASS = "class";

    private static final String CFG_KEY_LAYER_PARAMS = "parameters";

    private static final String CFG_KEY_LAYER_PARENTS = "parents";

    private static final String CFG_KEY_BASE_NETWORK_OUTPUT_INDEX = "output_index";

    private static final String CFG_KEY_BASE_NETWORK_SOURCE = "source";

    private static final String CFG_KEY_OUTPUT_LAYERS = "output_layers";

    private DLKerasNetworkLayerGraphSerializer() {
    }

    public static final List<FileStore> getNetworkFileStores(final List<DLKerasLayer> outputLayers) {
        final ArrayList<FileStore> networkFileStores = new ArrayList<>(3);
        new DLKerasNetworkLayerGraphTopologicalOrderIterator(outputLayers).visitAll(new DLKerasLayerVisitor() {

            @Override
            public void visitOutput(final DLKerasInnerLayer outputLayer) throws Exception {
                // no op - we are only interested in base networks
            }

            @Override
            public void visitInputOutput(final DLKerasInputLayer inputOutputLayer) throws Exception {
                // no op - we are only interested in base networks
            }

            @Override
            public void visitInput(final DLKerasInputLayer inputLayer) throws Exception {
                // no op - we are only interested in base networks
            }

            @Override
            public void visitHidden(final DLKerasInnerLayer hiddenLayer) throws Exception {
                // no op - we are only interested in base networks
            }

            @Override
            public void visitBaseNetworkOutput(final DLKerasBaseNetworkTensorSpecOutput baseNetworkOutput) {
                final DLNetworkLocation baseNetworkLocation = baseNetworkOutput.getBaseNetworkSource();
                if (baseNetworkLocation instanceof DLNetworkFileStoreLocation) {
                    final DLNetworkFileStoreLocation baseNetworkFileStoreLocation =
                        (DLNetworkFileStoreLocation)baseNetworkLocation;
                    networkFileStores.add(baseNetworkFileStoreLocation.getFileStore());
                }
            }
        });
        return networkFileStores;
    }

    /**
     * Writes the Keras network graph specified by the given output layers and their inputs (i.e. predecessor nodes) to
     * a stream.
     *
     * @param outputLayers the output layers of the network to serialize
     * @param objOut the stream to which to write the network graph, it is the client's responsibility to close it
     * @throws IOException if failed to write the network graph to stream
     */
    public static Map<Integer, DLKerasBaseNetworkTensorSpecOutput> writeGraphTo(final List<DLKerasLayer> outputLayers,
        final ObjectOutputStream objOut) throws IOException {
        final NodeSettings graphSettings = new NodeSettings(CFG_KEY_GRAPH);
        final AtomicInteger layerIndexCounter = new AtomicInteger();
        final Map<DLKerasTensorSpecsOutput, Integer> layerIndices = new HashMap<>();
        try {
            final TIntArrayList outputLayerIndices = new TIntArrayList(outputLayers.size());
            // Collects all base network specs. We have to serialize them outside the node settings.
            final LinkedHashMap<Integer, DLKerasNetworkSpec> baseNetworkSpecs = new LinkedHashMap<>(2);
            // Collects all the base networks whose network location cannot be simply (de)serialized.
            final LinkedHashMap<Integer, DLKerasBaseNetworkTensorSpecOutput> nonReferenceBaseNetworkLayers =
                new LinkedHashMap<>(2);
            new DLKerasNetworkLayerGraphTopologicalOrderIterator(outputLayers).visitAll(new DLKerasLayerVisitor() {

                @Override
                public void visitOutput(final DLKerasInnerLayer outputLayer) throws Exception {
                    visitHidden(outputLayer);
                    outputLayerIndices.add(layerIndices.get(outputLayer));
                }

                @Override
                public void visitHidden(final DLKerasInnerLayer innerLayer) throws Exception {
                    final NodeSettingsWO layerSettings = saveLayer(innerLayer);
                    final NodeSettingsWO parentIndices = layerSettings.addNodeSettings(CFG_KEY_LAYER_PARENTS);
                    for (int i = 0; i < innerLayer.getNumParents(); i++) {
                        final DLKerasTensorSpecsOutput parent = innerLayer.getParent(i);
                        parentIndices.addInt(Integer.toString(i), layerIndices.get(parent));
                    }
                }

                @Override
                public void visitInput(final DLKerasInputLayer inputLayer) throws Exception {
                    saveLayer(inputLayer);
                }

                @Override
                public void visitInputOutput(final DLKerasInputLayer inputOutputLayer) throws Exception {
                    saveLayer(inputOutputLayer);
                    outputLayerIndices.add(layerIndices.get(inputOutputLayer));
                }

                @Override
                public void visitBaseNetworkOutput(final DLKerasBaseNetworkTensorSpecOutput baseNetworkOutput) {
                    final NodeSettingsWO layerSettings = createLayerSettings(baseNetworkOutput);
                    final int layerIndex = layerIndices.get(baseNetworkOutput);
                    layerSettings.addInt(CFG_KEY_BASE_NETWORK_OUTPUT_INDEX,
                        baseNetworkOutput.getBaseNetworkOutputIndex());
                    final DLNetworkLocation baseNetworkSource = baseNetworkOutput.getBaseNetworkSource();
                    if (baseNetworkSource instanceof DLNetworkReferenceLocation) {
                        layerSettings.addString(CFG_KEY_BASE_NETWORK_SOURCE, baseNetworkSource.getURI().toString());
                    } else {
                        nonReferenceBaseNetworkLayers.put(layerIndex, baseNetworkOutput);
                    }
                    baseNetworkSpecs.put(layerIndex, baseNetworkOutput.getBaseNetworkSpec());
                }

                private NodeSettingsWO saveLayer(final DLKerasLayer layer) {
                    final NodeSettingsWO layerSettings = createLayerSettings(layer);
                    try {
                        // TODO: Avoid redundant creation of layer struct (not instance), should be cached somewhere.
                        new DLKerasLayerStructInstance(layer)
                            .saveSettingsTo(layerSettings.addNodeSettings(CFG_KEY_LAYER_PARAMS));
                    } catch (final InvalidSettingsException e) {
                        LOGGER.error(e);
                        throw new RuntimeException(e);
                    }
                    return layerSettings;
                }

                private NodeSettingsWO createLayerSettings(final DLKerasTensorSpecsOutput layer) {
                    assert !layerIndices.containsKey(layer);
                    final int layerIndex = layerIndexCounter.getAndIncrement();
                    layerIndices.put(layer, layerIndex);
                    final NodeSettingsWO layerSettings = graphSettings.addNodeSettings(Integer.toString(layerIndex));
                    layerSettings.addString(CFG_KEY_LAYER_CLASS, layer.getClass().getCanonicalName());
                    return layerSettings;
                }
            });
            graphSettings.addIntArray(CFG_KEY_OUTPUT_LAYERS, outputLayerIndices.toNativeArray());
            // Write to stream.
            objOut.writeInt(baseNetworkSpecs.size());
            for (final Entry<Integer, DLKerasNetworkSpec> entry : baseNetworkSpecs.entrySet()) {
                objOut.writeInt(entry.getKey());
                objOut.writeObject(entry.getValue());
            }
            objOut.writeObject(graphSettings);
            return nonReferenceBaseNetworkLayers;
        } catch (final Exception e) {
            throw new IOException("An exception occurred while saving the Keras layer graph. See log for details.", e);
        }
    }

    /**
     * Reads a Keras network graph from stream and returns its output layers. The entire graph can be accessed via the
     * layers' input (i.e. predecessor node) relationships.
     *
     * @param objIn the stream from which to read the network graph, it is the client's responsibility to close it
     * @param baseNetworkSourceAmender may be <code>null</code>
     * @return the read network graph
     * @throws IOException if failed to read the network graph from stream
     * @throws ClassNotFoundException if a network graph related class (e.g. a layer) could not be found
     */
    public static List<DLKerasLayer> readGraphFrom(final ObjectInputStream objIn,
        final Consumer<DLKerasBaseNetworkTensorSpecOutput> baseNetworkSourceAmender)
        throws IOException, ClassNotFoundException {
        try {
            // Read from stream.
            final int numBaseNetworks = objIn.readInt();
            final LinkedHashMap<Integer, DLKerasNetworkSpec> baseNetworkSpecs;
            if (numBaseNetworks > 0) {
                baseNetworkSpecs = new LinkedHashMap<>(numBaseNetworks);
                for (int i = 0; i < numBaseNetworks; i++) {
                    final int layerIndex = objIn.readInt();
                    final DLKerasNetworkSpec spec = (DLKerasNetworkSpec)objIn.readObject();
                    baseNetworkSpecs.put(layerIndex, spec);
                }
            } else {
                baseNetworkSpecs = null;
            }
            final NodeSettings graphSettings = (NodeSettings)objIn.readObject();

            // -1 because of saved output indices
            final int numLayers = graphSettings.getChildCount() - 1;
            final DLKerasTensorSpecsOutput[] loadedLayers = new DLKerasTensorSpecsOutput[numLayers];
            for (int i = 0; i < numLayers; i++) {
                final NodeSettings layerSettings = graphSettings.getNodeSettings(Integer.toString(i));
                final Class<?> layerClass = Class.forName(layerSettings.getString(CFG_KEY_LAYER_CLASS));
                final DLKerasTensorSpecsOutput layer;
                if (DLKerasLayer.class.isAssignableFrom(layerClass)) {
                    // Ordinary layers must expose a public nullary constructor.
                    layer = (DLKerasLayer)layerClass.newInstance();
                    // TODO: Avoid redundant creation of layer struct (not instance), should be cached somewhere.
                    new DLKerasLayerStructInstance((DLKerasLayer)layer)
                        .loadSettingsFrom(layerSettings.getNodeSettings(CFG_KEY_LAYER_PARAMS));
                    if (layer instanceof DLKerasInnerLayer) {
                        final DLKerasInnerLayer innerLayer = ((DLKerasInnerLayer)layer);
                        final NodeSettings parentIndices = layerSettings.getNodeSettings(CFG_KEY_LAYER_PARENTS);
                        for (int j = 0; j < parentIndices.getChildCount(); j++) {
                            innerLayer.setParent(j, loadedLayers[parentIndices.getInt(Integer.toString(j))]);
                        }
                    }
                } else if (DLKerasBaseNetworkTensorSpecOutput.class.isAssignableFrom(layerClass)) {
                    final DLKerasNetworkSpec spec = baseNetworkSpecs.get(i);
                    final int outputIndex = layerSettings.getInt(CFG_KEY_BASE_NETWORK_OUTPUT_INDEX);
                    layer = new DLKerasDefaultBaseNetworkTensorSpecOutput(spec, outputIndex);
                    final DLNetworkLocation baseNetworkSource;
                    if (layerSettings.containsKey(CFG_KEY_BASE_NETWORK_SOURCE)) {
                        final URI sourceURI = new URI(layerSettings.getString(CFG_KEY_BASE_NETWORK_SOURCE));
                        baseNetworkSource = new DLNetworkReferenceLocation(sourceURI);
                        ((DLKerasDefaultBaseNetworkTensorSpecOutput)layer).setBaseNetworkSource(baseNetworkSource);
                    } else if (baseNetworkSourceAmender != null) {
                        baseNetworkSourceAmender.accept((DLKerasBaseNetworkTensorSpecOutput)layer);
                    }
                } else {
                    throw new UnsupportedOperationException("Layer class '" + layerClass.getCanonicalName()
                        + "' is not marked as either " + DLKerasLayer.class.getCanonicalName() + " or "
                        + DLKerasBaseNetworkTensorSpecOutput.class.getCanonicalName()
                        + ". This is an implementation error.");
                }
                loadedLayers[i] = layer;
            }
            final int[] outputLayerIndices = graphSettings.getIntArray(CFG_KEY_OUTPUT_LAYERS);
            final ArrayList<DLKerasLayer> outputs = new ArrayList<>(outputLayerIndices.length);
            for (int i = 0; i < outputLayerIndices.length; i++) {
                outputs.add((DLKerasLayer)loadedLayers[outputLayerIndices[i]]);
            }
            return outputs;
        } catch (final ClassNotFoundException e) {
            LOGGER.error(e);
            throw new ClassNotFoundException(
                "A class could not be found while loading the Keras layer graph. See log for details.", e);
        } catch (final Exception e) {
            LOGGER.error(e);
            throw new IOException("An exception occurred while loading the Keras layer graph. See log for details.", e);
        }
    }
}
