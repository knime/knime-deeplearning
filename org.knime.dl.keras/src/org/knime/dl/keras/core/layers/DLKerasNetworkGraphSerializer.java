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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.knime.core.data.filestore.FileStore;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.dl.core.DLNetworkFileStoreLocation;
import org.knime.dl.core.DLNetworkLocation;
import org.knime.dl.core.DLNetworkReferenceLocation;
import org.knime.dl.keras.core.DLKerasNetworkSpec;
import org.knime.dl.keras.core.layers.DLKerasNetworkGraphIterator.DLKerasLayerVisitor;
import org.knime.dl.keras.core.struct.Structs;
import org.knime.dl.keras.core.struct.instance.MemberReadInstance;
import org.knime.dl.keras.core.struct.instance.MemberReadWriteInstance;
import org.knime.dl.keras.core.struct.instance.MemberWriteInstance;
import org.knime.dl.keras.core.struct.instance.StructInstance;
import org.knime.dl.keras.core.struct.nodesettings.NodeSettingsStructs;
import org.knime.dl.keras.core.struct.param.ParameterStructs;
import org.knime.dl.keras.core.struct.param.ValidityException;

import gnu.trove.TIntArrayList;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public final class DLKerasNetworkGraphSerializer {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(DLKerasNetworkGraphSerializer.class);

    private static final String CFG_KEY_GRAPH = "layer_graph";

    private static final String CFG_KEY_LAYER = "layer";

    private static final String CFG_KEY_LAYER_CLASS = "class";

    private static final String CFG_KEY_LAYER_PARAMS = "parameters";

    private static final String CFG_KEY_LAYER_PARENTS = "parents";

    private static final String CFG_KEY_LAYER_RUNTIME_ID = "runtime_id";

    private static final String CFG_KEY_PARENT_INDEX = "parent_index";

    private static final String CFG_KEY_INDEX_IN_PARENT = "index_in_parent";

    private static final String CFG_KEY_BASE_NETWORK_OUTPUT_INDEX = "output_index";

    private static final String CFG_KEY_BASE_NETWORK_SOURCE = "source";

    private static final String CFG_KEY_OUTPUT_LAYERS = "output_layers";

    private static final MemoryAlertAwareGuavaCache CACHE_SERIALIZED =
        MemoryAlertAwareGuavaCache.getInstanceSerialized();

    private static final MemoryAlertAwareGuavaCache CACHE_OBJECTS = MemoryAlertAwareGuavaCache.getInstanceObjects();

    private DLKerasNetworkGraphSerializer() {
    }

    public static final List<FileStore> getNetworkFileStores(final List<DLKerasLayer> outputLayers) {
        final ArrayList<FileStore> networkFileStores = new ArrayList<>(3);
        new DLKerasNetworkGraphTopologicalOrderIterator(outputLayers).visitAll(new DLKerasLayerVisitor() {

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

    public static void writeGraphTo(final List<DLKerasLayer> outputLayers, final ObjectOutputStream objOut)
        throws IOException {

        // Ids of the layers already saved. If a id appears again it isn't saved again with all its parents
        final HashSet<String> savedIds = new HashSet<>();
        objOut.writeInt(outputLayers.size());
        for (final DLKerasLayer outputLayer : outputLayers) {
            objOut.writeObject(saveLayerAndParentsCached(outputLayer, savedIds));
        }
    }

    public static List<DLKerasLayer> readGraphFrom(final ObjectInputStream objIn,
        final Consumer<DLKerasBaseNetworkTensorSpecOutput> baseNetworkSourceAmender)
        throws IOException, ClassNotFoundException {

        final int numOutputs = objIn.readInt();
        final List<DLKerasLayer> outputLayers = new ArrayList<>(numOutputs);

        for (int i = 0; i < numOutputs; i++) {
            final DLKerasLayerBytes layerBytesObj = (DLKerasLayerBytes)objIn.readObject();
            outputLayers.add(loadLayerAndParentsCached(layerBytesObj));
        }
        return outputLayers;
    }

    /**
     * Creates a (or loads a cached) DLKerasLayerByte object which contains the layer id and a byte representation of
     * the layer and all its parents.
     */
    private static DLKerasLayerBytes saveLayerAndParentsCached(final DLKerasLayer layer, final HashSet<String> savedIds)
        throws IOException {
        final String id = layer.getRuntimeId();

        // Don't save anything if the layer has been saved before
        if (savedIds.contains(id)) {
            return new DLKerasLayerBytes(id, null);
        }

        // Get the serialized layer from the cache or serialize it
        final DLKerasLayerBytes layerBytes;
        final Optional<Object> optLayerBytes = CACHE_SERIALIZED.get(id);
        if (optLayerBytes.isPresent()) {
            layerBytes = (DLKerasLayerBytes)optLayerBytes.get();
        } else {
            layerBytes = saveLayerAndParents(layer, savedIds);
            CACHE_SERIALIZED.put(id, layerBytes);
        }
        savedIds.add(id);
        return layerBytes;
    }

    private static DLKerasLayer loadLayerAndParentsCached(final DLKerasLayerBytes layerBytesObj)
        throws IOException, ClassNotFoundException {
        final String id = layerBytesObj.getId();
        final DLKerasLayer layerObj;
        final Optional<Object> optLayerObj = CACHE_OBJECTS.get(id);
        if (optLayerObj.isPresent()) {
            layerObj = (DLKerasLayer)optLayerObj.get();
        } else {
            layerObj = loadLayerAndParents(layerBytesObj);
            CACHE_OBJECTS.put(id, layerObj);
        }
        return layerObj;
    }

    private static DLKerasLayerBytes saveLayerAndParents(final DLKerasLayer layer, final HashSet<String> savedIds)
        throws IOException {
        // TODO is the runtime id needed here? Do we need a DLKerasLayerBytes object or is a byte[] enough?
        final String id = layer.getRuntimeId();

        final ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        final ObjectOutputStream objOut = new ObjectOutputStream(byteOut);

        // Write the parents to the objOut
        if (layer instanceof DLKerasInnerLayer) {
            final DLKerasInnerLayer innerLayer = (DLKerasInnerLayer)layer;
            // Write the number of parents to read to the objOut
            final int numParents = innerLayer.getNumParents();
            objOut.writeInt(numParents);
            // Loop over parents and save in the objOut
            for (int i = 0; i < numParents; i++) {
                final DLKerasTensorSpecsOutput parent = innerLayer.getParent(i);
                // TODO handle BaseNetworkTensorSpecOutput
                if (parent instanceof DLKerasLayer) {
                    objOut.writeInt(innerLayer.getTensorIndexInParent(i));
                    objOut.writeObject(saveLayerAndParentsCached((DLKerasLayer)parent, savedIds));
                }
            }
        } else {
            // The layer has no parents
            objOut.writeInt(0);
        }

        // Save the layer itself
        try {
            saveLayer(layer, objOut);
        } catch (InvalidSettingsException e) {
            throw new IOException("Could not save layer", e);
        }

        // Get the final byte representation
        objOut.flush();
        final byte[] layerBytes = byteOut.toByteArray();

        return new DLKerasLayerBytes(id, layerBytes);
    }

    private static DLKerasLayer loadLayerAndParents(final DLKerasLayerBytes layerBytesObj)
        throws IOException, ClassNotFoundException {
        final String id = layerBytesObj.getId();

        final ByteArrayInputStream byteIn = new ByteArrayInputStream(layerBytesObj.getLayerData());
        final ObjectInputStream objIn = new ObjectInputStream(byteIn);

        // Read the parents of this layer
        final int numParents = objIn.readInt();
        final DLKerasTensorSpecsOutput[] parents = new DLKerasTensorSpecsOutput[numParents];
        final int[] idxInParents = new int[numParents];
        for (int i = 0; i < numParents; i++) {
            // TODO handle BaseNetworkSpecOutput
            idxInParents[i] = objIn.readInt();
            parents[i] = loadLayerAndParentsCached((DLKerasLayerBytes)objIn.readObject());
        }

        // Load the layer itself
        return loadLayer(objIn, id, parents, idxInParents);
    }

    /**
     * Saves one layer to a object stream.
     */
    private static void saveLayer(final DLKerasLayer layer, final ObjectOutputStream objOut)
        throws InvalidSettingsException, IOException {
        // TODO Layers are saved in NodeSettings right now. Is this the best solution?
        // TODO Add a version identifier to determine how layers are saved for this version

        // Create NodeSettings to save the layer
        final NodeSettings layerSettings = new NodeSettings(CFG_KEY_LAYER); // TODO key
        // Save the layer class
        layerSettings.addString(CFG_KEY_LAYER_CLASS, layer.getClass().getCanonicalName());
        // Save the layer settings
        // TODO: Avoid redundant creation of layer struct (not instance), should be cached somewhere.
        final StructInstance<MemberReadWriteInstance<?>, ?> layerInstance = ParameterStructs.createInstance(layer);
        final StructInstance<MemberWriteInstance<?>, ?> settingsInstance = NodeSettingsStructs
            .createNodeSettingsInstance(layerSettings.addNodeSettings(CFG_KEY_LAYER_PARAMS), layerInstance.struct());
        Structs.shallowCopyUnsafe(layerInstance, settingsInstance);
        // Write the NodeSettings to the objOut
        objOut.writeObject(layerSettings);
    }

    private static DLKerasLayer loadLayer(final ObjectInputStream objIn, final String id,
        final DLKerasTensorSpecsOutput[] parents, final int[] idxInParents) throws IOException, ClassNotFoundException {
        try {
            // Get the layer NodeSettings
            final NodeSettings layerSettings = (NodeSettings)objIn.readObject();
            // Get the layer class
            final Class<?> layerClass = Class.forName(layerSettings.getString(CFG_KEY_LAYER_CLASS));
            // Create the layer
            final DLKerasLayer layer = (DLKerasLayer)layerClass.newInstance();
            // Load settings into layer
            final StructInstance<MemberReadWriteInstance<?>, ?> layerInstance = ParameterStructs.createInstance(layer);
            final StructInstance<MemberReadInstance<?>, ?> settingsInstance =
                NodeSettingsStructs.createNodeSettingsInstance(
                    (NodeSettingsRO)layerSettings.getNodeSettings(CFG_KEY_LAYER_PARAMS), layerInstance.struct());
            Structs.shallowCopyUnsafe(settingsInstance, layerInstance);
            // Load parent settings
            // TODO handle BaseNetworkSpecOutput
            if (parents.length != 0 && layer instanceof DLKerasInnerLayer) {
                final DLKerasInnerLayer innerLayer = (DLKerasInnerLayer)layer;
                // TODO check if parents list fits?
                for (int i = 0; i < parents.length; i++) {
                    innerLayer.setParent(i, parents[i]);
                    innerLayer.setTensorIndexInParent(i, idxInParents[i]);
                }
            }
            // Set runtime id
            layer.setRuntimeId(id);
            return layer;
        } catch (final InvalidSettingsException e) {
            throw new IOException("Could not load saved layer.", e); // TODO error message
        } catch (final InstantiationException | IllegalAccessException e) {
            throw new IOException("Could not load saved layer.", e); // TODO error message
        }
    }

    // TODO move somewhere else?
    private static class DLKerasLayerBytes implements Serializable {

        private static final long serialVersionUID = 1L;

        private final String m_id;

        private final byte[] m_layerData;

        public DLKerasLayerBytes(final String id, final byte[] layerData) {
            m_id = id;
            m_layerData = layerData;
        }

        /**
         * @return the id
         */
        public String getId() {
            return m_id;
        }

        /**
         * @return the layer represented in bytes
         */
        public byte[] getLayerData() {
            return m_layerData;
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // LEGACY CODE TODO REMOVE writeGraphToOld
    // ---------------------------------------------------------------------------------------------------------------

    /**
     * Writes the Keras network graph specified by the given output layers and their inputs (i.e. predecessor nodes) to
     * a stream.
     *
     * @param outputLayers the output layers of the network to serialize
     * @param objOut the stream to which to write the network graph, it is the client's responsibility to close it
     * @throws IOException if failed to write the network graph to stream
     */
    public static Map<Integer, DLKerasBaseNetworkTensorSpecOutput>
        writeGraphToOld(final List<DLKerasLayer> outputLayers, final ObjectOutputStream objOut) throws IOException {
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
            new DLKerasNetworkGraphTopologicalOrderIterator(outputLayers).visitAll(new DLKerasLayerVisitor() {

                @Override
                public void visitOutput(final DLKerasInnerLayer outputLayer) throws Exception {
                    visitHidden(outputLayer);
                    outputLayerIndices.add(layerIndices.get(outputLayer));
                }

                @Override
                public void visitHidden(final DLKerasInnerLayer innerLayer) throws Exception {
                    final NodeSettingsWO layerSettings = saveLayer(innerLayer);
                    final NodeSettingsWO parentSettings = layerSettings.addNodeSettings(CFG_KEY_LAYER_PARENTS);
                    for (int i = 0; i < innerLayer.getNumParents(); i++) {
                        final DLKerasTensorSpecsOutput parent = innerLayer.getParent(i);
                        NodeSettingsWO parentSetting = parentSettings.addNodeSettings(Integer.toString(i));
                        parentSetting.addInt(CFG_KEY_PARENT_INDEX, layerIndices.get(parent));
                        parentSetting.addInt(CFG_KEY_INDEX_IN_PARENT, innerLayer.getTensorIndexInParent(i));
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

                private NodeSettingsWO saveLayer(final DLKerasLayer layer)
                    throws ValidityException, InvalidSettingsException {
                    final NodeSettingsWO layerSettings = createLayerSettings(layer);
                    // TODO: Avoid redundant creation of layer struct (not instance), should be cached somewhere.
                    final StructInstance<MemberReadWriteInstance<?>, ?> layerInstance =
                        ParameterStructs.createInstance(layer);
                    final StructInstance<MemberWriteInstance<?>, ?> settingsInstance =
                        NodeSettingsStructs.createNodeSettingsInstance(
                            layerSettings.addNodeSettings(CFG_KEY_LAYER_PARAMS), layerInstance.struct());
                    Structs.shallowCopyUnsafe(layerInstance, settingsInstance);
                    layerSettings.addString(CFG_KEY_LAYER_RUNTIME_ID, layer.getRuntimeId());
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
    public static List<DLKerasLayer> readGraphFromOld(final ObjectInputStream objIn,
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
                    layer = (DLKerasTensorSpecsOutput)layerClass.newInstance();
                    final StructInstance<MemberReadWriteInstance<?>, ?> layerInstance =
                        ParameterStructs.createInstance((DLKerasLayer)layer);
                    final StructInstance<MemberReadInstance<?>, ?> settingsInstance = NodeSettingsStructs
                        .createNodeSettingsInstance((NodeSettingsRO)layerSettings.getNodeSettings(CFG_KEY_LAYER_PARAMS),
                            layerInstance.struct());
                    Structs.shallowCopyUnsafe(settingsInstance, layerInstance);
                    if (layer instanceof DLKerasInnerLayer) {
                        final DLKerasInnerLayer innerLayer = ((DLKerasInnerLayer)layer);
                        final NodeSettings parentSettings = layerSettings.getNodeSettings(CFG_KEY_LAYER_PARENTS);
                        loadParentSettings(loadedLayers, innerLayer, parentSettings);
                    }
                    ((DLKerasLayer)layer).setRuntimeId(layerSettings.getString(CFG_KEY_LAYER_RUNTIME_ID));
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

    private static void loadParentSettings(final DLKerasTensorSpecsOutput[] loadedLayers,
        final DLKerasInnerLayer innerLayer, final NodeSettings parentSettings) throws InvalidSettingsException {
        try {
            newLoadParentSettings(loadedLayers, innerLayer, parentSettings);
        } catch (InvalidSettingsException e) {
            oldLoadParentSettings(loadedLayers, innerLayer, parentSettings);
        }
    }

    private static void oldLoadParentSettings(final DLKerasTensorSpecsOutput[] loadedLayers,
        final DLKerasInnerLayer innerLayer, final NodeSettings parentSettings) throws InvalidSettingsException {
        for (int j = 0; j < parentSettings.getChildCount(); j++) {
            innerLayer.setParent(j, loadedLayers[parentSettings.getInt(Integer.toString(j))]);
            innerLayer.setTensorIndexInParent(j, 0);
        }
    }

    private static void newLoadParentSettings(final DLKerasTensorSpecsOutput[] loadedLayers,
        final DLKerasInnerLayer innerLayer, final NodeSettings parentSettings) throws InvalidSettingsException {
        for (int j = 0; j < parentSettings.getChildCount(); j++) {
            NodeSettings parentSetting = parentSettings.getNodeSettings(Integer.toString(j));
            int parentIndex = parentSetting.getInt(CFG_KEY_PARENT_INDEX);
            int indexInParent = parentSetting.getInt(CFG_KEY_INDEX_IN_PARENT);
            innerLayer.setParent(j, loadedLayers[parentIndex]);
            innerLayer.setTensorIndexInParent(j, indexInParent);
        }
    }
}
