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
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.knime.core.data.filestore.FileStore;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
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

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public final class DLKerasNetworkGraphSerializer {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(DLKerasNetworkGraphSerializer.class);

    private static final String CURRENT_VERSION_ID = "20";

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

    /**
     * Writes the Keras network graph specified by the given output layers and their inputs (i.e. predecessor nodes) to
     * a stream.
     *
     * @param outputLayers the output layers of the network to serialize
     * @param objOut the stream to which to write the network graph, it is the client's responsibility to close it
     * @throws IOException if failed to write the network graph to stream
     */
    public static void writeGraphTo(final List<DLKerasLayer> outputLayers, final ObjectOutputStream objOut)
        throws IOException {

        // Write a version of the serialization
        objOut.writeObject(CURRENT_VERSION_ID);

        // Ids of the layers already saved. If a id appears again it isn't saved again with all its parents
        final HashSet<String> savedIds = new HashSet<>();
        objOut.writeInt(outputLayers.size());
        for (final DLKerasLayer outputLayer : outputLayers) {
            objOut.writeObject(saveSpecOutputCached(outputLayer, savedIds));
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

        final String version;
        try {
            version = (String)objIn.readObject();
        } catch (Exception e) {
            // This is an old version without an saved version number
            return readGraphFrom10(objIn, baseNetworkSourceAmender);
        }

        if (version.equals("20")) {
            return readGraphFrom20(objIn, baseNetworkSourceAmender);
        } else {
            throw new IOException(""); // TODO error message
        }
    }

    private static List<DLKerasLayer> readGraphFrom20(final ObjectInputStream objIn,
        final Consumer<DLKerasBaseNetworkTensorSpecOutput> baseNetworkSourceAmender)
        throws IOException, ClassNotFoundException {

        final int numOutputs = objIn.readInt();
        final List<DLKerasLayer> outputLayers = new ArrayList<>(numOutputs);

        for (int i = 0; i < numOutputs; i++) {
            final DLKerasSerializedTensorSpecOutput serializedOutput =
                (DLKerasSerializedTensorSpecOutput)objIn.readObject();
            outputLayers.add((DLKerasLayer)loadSpecOutputCached(serializedOutput, baseNetworkSourceAmender));
        }
        return outputLayers;
    }

    /**
     * Creates a (or loads a cached) DLKerasLayerByte object which contains the layer id and a byte representation of
     * the layer and all its parents.
     */
    private static DLKerasSerializedTensorSpecOutput saveSpecOutputCached(final DLKerasTensorSpecsOutput specOutput,
        final HashSet<String> savedIds) throws IOException {
        // Remember if the layer should be cached later
        boolean cacheSerialized = true;

        // If this is a layer we may not have to serialize it
        if (specOutput instanceof DLKerasLayer) {
            final DLKerasLayer layer = (DLKerasLayer)specOutput;
            final String id = layer.getRuntimeId();

            // Don't save anything if the layer has been saved before
            if (savedIds.contains(id)) {
                return new DLKerasSerializedIdHolder(id);
            }
            // TODO Could it happen that a not complete layer gets saved and nodes are missing in the port object and cache

            // Check if the layer has been cached and the cached version
            // doesn't save layers already saved
            final Optional<Object> optSerizalizedLayer = CACHE_SERIALIZED.get(id);
            if (optSerizalizedLayer.isPresent()) {
                // Layer has been cached
                final DLKerasSerializedInnerLayer cachedLayer = (DLKerasSerializedInnerLayer)optSerizalizedLayer.get();
                if (Collections.disjoint(cachedLayer.getIncludedLayers(), savedIds)) {
                    // The cached layer only contains nodes not saved already
                    savedIds.addAll(cachedLayer.getIncludedLayers());
                    return cachedLayer;
                }
                cacheSerialized = false;
            }
        }

        // Serialize the layer
        final ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        final ObjectOutputStream objOut = new ObjectOutputStream(byteOut);

        if (specOutput instanceof DLKerasLayer) {
            final DLKerasLayer layer = (DLKerasLayer)specOutput;
            // Save the layer
            final HashSet<String> includedIds = saveLayerAndParents(layer, savedIds, objOut);
            objOut.flush();
            final DLKerasSerializedInnerLayer serializedLayer =
                new DLKerasSerializedInnerLayer(layer.getRuntimeId(), byteOut.toByteArray(), includedIds);
            // Add included ids
            savedIds.addAll(serializedLayer.getIncludedLayers());
            // Cache if there isn't another cached version
            if (cacheSerialized) {
                CACHE_SERIALIZED.put(layer.getRuntimeId(), serializedLayer);
            }
            return serializedLayer;
        } else if (specOutput instanceof DLKerasBaseNetworkTensorSpecOutput) {
            saveLayer(specOutput, objOut);
            objOut.flush();
            return new DLKerasSerializedBaseNetwork(byteOut.toByteArray());
        } else {
            throw new UnsupportedOperationException("Layer class '" + specOutput.getClass().getCanonicalName()
                + "' is not marked as either " + DLKerasLayer.class.getCanonicalName() + " or "
                + DLKerasBaseNetworkTensorSpecOutput.class.getCanonicalName() + ". This is an implementation error."); // TODO error message
        }
    }

    private static DLKerasTensorSpecsOutput loadSpecOutputCached(
        final DLKerasSerializedTensorSpecOutput serializedOutput,
        final Consumer<DLKerasBaseNetworkTensorSpecOutput> baseNetworkSourceAmender)
        throws IOException, ClassNotFoundException {
        if (serializedOutput instanceof DLKerasSerializedIdHolder) {
            final String id = ((DLKerasSerializedIdHolder)serializedOutput).getId();
            // Check the cache for the layer
            final Optional<Object> optLayerObj = CACHE_OBJECTS.get(id);
            if (optLayerObj.isPresent()) {
                // Return the layer from the cache
                return (DLKerasLayer)optLayerObj.get();
            }
        }

        // Deserialize the layer
        if (serializedOutput instanceof DLKerasSerializedDataHolder) {
            final ByteArrayInputStream byteIn =
                new ByteArrayInputStream(((DLKerasSerializedDataHolder)serializedOutput).getLayerData());
            final ObjectInputStream objIn = new ObjectInputStream(byteIn);

            if (serializedOutput instanceof DLKerasSerializedInnerLayer) {
                final String id = ((DLKerasSerializedInnerLayer)serializedOutput).getId();
                final DLKerasLayer layerObj = loadLayerAndParents(objIn, id, baseNetworkSourceAmender);
                CACHE_OBJECTS.put(id, layerObj);
                return layerObj;
            } else if (serializedOutput instanceof DLKerasSerializedBaseNetwork) {
                return loadLayer(objIn, null, null, null, baseNetworkSourceAmender);
            } else {
                throw new UnsupportedOperationException(
                    "Serialized layer class '" + serializedOutput.getClass().getCanonicalName()
                        + "' is not marked as either " + DLKerasSerializedIdHolder.class.getCanonicalName() + " or "
                        + DLKerasSerializedBaseNetwork.class.getCanonicalName() + ". This is an implementation error.");
            }
        } else {
            throw new IllegalStateException("Layer neither cached nor saved. This is an implementation error."); // TODO can this happen?
        }
    }

    private static HashSet<String> saveLayerAndParents(final DLKerasLayer layer, final HashSet<String> savedIds,
        final ObjectOutputStream objOut) throws IOException {

        // Keep track of the layer ids included
        final HashSet<String> includedIds = new HashSet<>();

        // Write the parents to the objOut
        if (layer instanceof DLKerasInnerLayer) {
            final DLKerasInnerLayer innerLayer = (DLKerasInnerLayer)layer;
            // Write the number of parents to read to the objOut
            final int numParents = innerLayer.getNumParents();
            objOut.writeInt(numParents);
            // Loop over parents and save in the objOut
            for (int i = 0; i < numParents; i++) {
                final DLKerasTensorSpecsOutput parent = innerLayer.getParent(i);
                objOut.writeInt(innerLayer.getTensorIndexInParent(i));
                final DLKerasSerializedTensorSpecOutput serializedParent = saveSpecOutputCached(parent, savedIds);
                objOut.writeObject(serializedParent);
                // Remember which layers are included in the parent
                if (serializedParent instanceof DLKerasSerializedInnerLayer) {
                    includedIds.addAll(((DLKerasSerializedInnerLayer)serializedParent).getIncludedLayers());
                }
            }
        } else {
            // The layer has no parents
            objOut.writeInt(0);
        }

        // Save the layer itself
        saveLayer(layer, objOut);
        includedIds.add(layer.getRuntimeId());

        return includedIds;
    }

    private static DLKerasLayer loadLayerAndParents(final ObjectInputStream objIn, final String id,
        final Consumer<DLKerasBaseNetworkTensorSpecOutput> baseNetworkSourceAmender)
        throws IOException, ClassNotFoundException {

        // Read the parents of this layer
        final int numParents = objIn.readInt();
        final DLKerasTensorSpecsOutput[] parents = new DLKerasTensorSpecsOutput[numParents];
        final int[] idxInParents = new int[numParents];
        for (int i = 0; i < numParents; i++) {
            idxInParents[i] = objIn.readInt();
            parents[i] =
                loadSpecOutputCached((DLKerasSerializedTensorSpecOutput)objIn.readObject(), baseNetworkSourceAmender);
        }

        // Load the layer itself
        return (DLKerasLayer)loadLayer(objIn, id, parents, idxInParents, baseNetworkSourceAmender);
    }

    /**
     * Saves one layer to a object stream.
     */
    private static void saveLayer(final DLKerasTensorSpecsOutput layer, final ObjectOutputStream objOut)
        throws IOException {
        // TODO Layers are saved in NodeSettings right now. Is this the best solution?
        // TODO Add a version identifier to determine how layers are saved for this version

        // Create NodeSettings to save the layer
        final NodeSettings layerSettings = new NodeSettings(CFG_KEY_LAYER);
        // Save the layer class
        layerSettings.addString(CFG_KEY_LAYER_CLASS, layer.getClass().getCanonicalName());

        // Check if this is a layer or base network
        if (layer instanceof DLKerasLayer) {
            try {
                // Save the layer settings
                // TODO: Avoid redundant creation of layer struct (not instance), should be cached somewhere.
                final StructInstance<MemberReadWriteInstance<?>, ?> layerInstance =
                    ParameterStructs.createInstance(layer);
                final StructInstance<MemberWriteInstance<?>, ?> settingsInstance =
                    NodeSettingsStructs.createNodeSettingsInstance(layerSettings.addNodeSettings(CFG_KEY_LAYER_PARAMS),
                        layerInstance.struct());
                Structs.shallowCopyUnsafe(layerInstance, settingsInstance);
            } catch (InvalidSettingsException e) {
                throw new IOException("Could not save layer.", e); // TODO error message
            }
            // Write the NodeSettings to the objOut
            objOut.writeObject(layerSettings);

        } else if (layer instanceof DLKerasBaseNetworkTensorSpecOutput) {
            // Layer is a base network
            final DLKerasBaseNetworkTensorSpecOutput baseNetwork = (DLKerasBaseNetworkTensorSpecOutput)layer;
            // Save the output index
            layerSettings.addInt(CFG_KEY_BASE_NETWORK_OUTPUT_INDEX, baseNetwork.getBaseNetworkOutputIndex());
            // Save the network source
            final DLNetworkLocation baseNetworkSource = baseNetwork.getBaseNetworkSource();
            if (baseNetworkSource instanceof DLNetworkReferenceLocation) {
                layerSettings.addString(CFG_KEY_BASE_NETWORK_SOURCE, baseNetworkSource.getURI().toString());
            } else {
                // TODO how to handle this?
            }
            // Write the NodeSettings to the objOut
            objOut.writeObject(layerSettings);
            // Save the output specs to the objOut TODO check if this is the best way
            objOut.writeObject(baseNetwork.getBaseNetworkSpec());
        }
    }

    private static DLKerasTensorSpecsOutput loadLayer(final ObjectInputStream objIn, final String id,
        final DLKerasTensorSpecsOutput[] parents, final int[] idxInParents,
        final Consumer<DLKerasBaseNetworkTensorSpecOutput> baseNetworkSourceAmender)
        throws IOException, ClassNotFoundException {
        try {
            // Get the layer NodeSettings
            final NodeSettings layerSettings = (NodeSettings)objIn.readObject();
            // Get the layer class
            final Class<?> layerClass = Class.forName(layerSettings.getString(CFG_KEY_LAYER_CLASS));
            // Create the layer
            if (DLKerasLayer.class.isAssignableFrom(layerClass)) {
                final DLKerasLayer layer = (DLKerasLayer)layerClass.newInstance();
                // Load settings into layer
                final StructInstance<MemberReadWriteInstance<?>, ?> layerInstance =
                    ParameterStructs.createInstance(layer);
                final StructInstance<MemberReadInstance<?>, ?> settingsInstance =
                    NodeSettingsStructs.createNodeSettingsInstance(
                        (NodeSettingsRO)layerSettings.getNodeSettings(CFG_KEY_LAYER_PARAMS), layerInstance.struct());
                Structs.shallowCopyUnsafe(settingsInstance, layerInstance);
                // Load parent settings
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
            } else if (DLKerasBaseNetworkTensorSpecOutput.class.isAssignableFrom(layerClass)) {
                final int outputIdx = layerSettings.getInt(CFG_KEY_BASE_NETWORK_OUTPUT_INDEX);
                final DLKerasNetworkSpec spec = (DLKerasNetworkSpec)objIn.readObject();
                final DLKerasBaseNetworkTensorSpecOutput baseNetwork =
                    new DLKerasDefaultBaseNetworkTensorSpecOutput(spec, outputIdx);
                if (layerSettings.containsKey(CFG_KEY_BASE_NETWORK_SOURCE)) {
                    final URI sourceURI = new URI(layerSettings.getString(CFG_KEY_BASE_NETWORK_SOURCE));
                    baseNetwork.setBaseNetworkSource(new DLNetworkReferenceLocation(sourceURI));
                } else if (baseNetworkSourceAmender != null) {
                    baseNetworkSourceAmender.accept(baseNetwork);
                }
                return baseNetwork;
            } else {
                throw new UnsupportedOperationException("Layer class '" + layerClass.getCanonicalName()
                    + "' is not marked as either " + DLKerasLayer.class.getCanonicalName() + " or "
                    + DLKerasBaseNetworkTensorSpecOutput.class.getCanonicalName()
                    + ". This is an implementation error.");
            }
        } catch (final InvalidSettingsException e) {
            throw new IOException("Could not load saved layer.", e); // TODO error message
        } catch (final InstantiationException | IllegalAccessException e) {
            throw new IOException("Could not load saved layer.", e); // TODO error message
        } catch (URISyntaxException e) {
            throw new IOException("Could not load saved layer.", e); // TODO error message
        }
    }

    // -------------------------------------------------------------------------------------------------
    // INTERFACES AND CLASSES FOR CACHING SERIALIZED STUFF TODO rename them
    // -------------------------------------------------------------------------------------------------

    private static interface DLKerasSerializedTensorSpecOutput extends Serializable {
        // Marker interface
    }

    private static interface DLKerasSerializedDataHolder extends DLKerasSerializedTensorSpecOutput {
        byte[] getLayerData();
    }

    private static class DLKerasSerializedIdHolder implements DLKerasSerializedTensorSpecOutput {

        private static final long serialVersionUID = 1L;

        private final String m_id;

        public DLKerasSerializedIdHolder(final String id) {
            m_id = id;
        }

        public String getId() {
            return m_id;
        }

    }

    // TODO move somewhere else?
    private static class DLKerasSerializedInnerLayer extends DLKerasSerializedIdHolder
        implements DLKerasSerializedDataHolder {

        private static final long serialVersionUID = 1L;

        private final byte[] m_layerData;

        private final transient HashSet<String> m_includedLayers;

        public DLKerasSerializedInnerLayer(final String id, final byte[] layerData,
            final HashSet<String> includedLayers) {
            super(id);
            m_layerData = layerData;
            m_includedLayers = includedLayers;
        }

        @Override
        public byte[] getLayerData() {
            return m_layerData;
        }

        public HashSet<String> getIncludedLayers() {
            return m_includedLayers;
        }
    }

    private static class DLKerasSerializedBaseNetwork implements DLKerasSerializedDataHolder {
        private final byte[] m_layerData;

        public DLKerasSerializedBaseNetwork(final byte[] layerData) {
            m_layerData = layerData;
        }

        @Override
        public byte[] getLayerData() {
            return m_layerData;
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // LEGACY CODE
    // ---------------------------------------------------------------------------------------------------------------

    private static List<DLKerasLayer> readGraphFrom10(final ObjectInputStream objIn,
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
