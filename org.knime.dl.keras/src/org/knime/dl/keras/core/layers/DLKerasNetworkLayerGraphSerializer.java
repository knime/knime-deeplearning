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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsWO;
import org.knime.dl.keras.core.layers.DLKerasNetworkLayerGraphIterator.DLKerasLayerVisitor;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public final class DLKerasNetworkLayerGraphSerializer {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(DLKerasNetworkLayerGraphSerializer.class);

    private static final String CFG_KEY_GRAPH = "layer_graph";

    private static final String CFG_KEY_OUTPUT_LAYERS = "outputs";

    private static final String CFG_KEY_LAYER_CLASS = "class";

    private static final String CFG_KEY_LAYER_PARAMS = "parameters";

    private static final String CFG_KEY_LAYER_PARENTS = "parents";

    /**
     * Writes the Keras network graph specified by the given output layers and their inputs (i.e. predecessor nodes) to
     * a stream.
     *
     * @param outputLayers the output layers of the network to serialize
     * @param objOut the stream to which to write the network graph
     * @throws IOException if failed to write the network graph to stream
     */
    public void writeGraphTo(final List<DLKerasLayer> outputLayers, final ObjectOutputStream objOut)
        throws IOException {
        final NodeSettings graphSettings = new NodeSettings(CFG_KEY_GRAPH);
        final AtomicInteger layerIndexCounter = new AtomicInteger();
        final Map<DLKerasLayer, Integer> layerIndices = new HashMap<>();
        try {
            final int[] outputLayerIndices = new int[outputLayers.size()];
            for (int i = 0; i < outputLayers.size(); i++) {
                outputLayerIndices[i] = layerIndexCounter.getAndIncrement();
                layerIndices.put(outputLayers.get(i), outputLayerIndices[i]);
            }
            graphSettings.addIntArray(CFG_KEY_OUTPUT_LAYERS, outputLayerIndices);

            new DLKerasNetworkLayerGraphIterator(outputLayers).visitAll(new DLKerasLayerVisitor() {

                @Override
                public void visitOutput(final DLKerasInnerLayer outputLayer) throws Exception {
                    visitHidden(outputLayer);
                }

                @Override
                public void visitHidden(final DLKerasInnerLayer innerLayer) throws Exception {
                    final NodeSettingsWO layerSettings = saveLayer(innerLayer);
                    final NodeSettingsWO parentIndices = layerSettings.addNodeSettings(CFG_KEY_LAYER_PARENTS);
                    for (int i = 0; i < innerLayer.getNumParents(); i++) {
                        final DLKerasTensorSpecsOutput parent = innerLayer.getParent(i);
                        if (parent instanceof DLKerasLayer) {
                            final Integer parentLayerIndex = layerIndices.computeIfAbsent((DLKerasLayer)parent,
                                l -> layerIndexCounter.getAndIncrement());
                            parentIndices.addInt(Integer.toString(i), parentLayerIndex);
                        }
                    }
                }

                @Override
                public void visitInput(final DLKerasInputLayer inputLayer) throws Exception {
                    saveLayer(inputLayer);
                }

                @Override
                public void visitInputOutput(final DLKerasInputLayer inputOutputLayer) throws Exception {
                    saveLayer(inputOutputLayer);
                }

                @Override
                public void visitBaseNetworkOutput(final DLKerasBaseNetworkTensorSpecOutput baseNetworkOutput) {
                    // no op
                }

                private NodeSettingsWO saveLayer(final DLKerasLayer layer) {
                    // Each layer is either an output layer or someone's parent. We created indices for both cases above.
                    final Integer layerIndex = layerIndices.get(layer);
                    final NodeSettingsWO layerSettings = graphSettings.addNodeSettings(layerIndex.toString());
                    try {
                        layerSettings.addString(CFG_KEY_LAYER_CLASS, layer.getClass().getCanonicalName());
                        // TODO: Avoid redundant creation of layer struct (not instance), should be cached somewhere.
                        new DLKerasLayerStructInstance(layer)
                            .saveSettingsTo(layerSettings.addNodeSettings(CFG_KEY_LAYER_PARAMS));
                    } catch (final InvalidSettingsException e) {
                        LOGGER.error(e);
                        throw new RuntimeException(e);
                    }
                    return layerSettings;
                }
            });
        } catch (final Exception e) {
            throw new IOException("An exception occurred while saving the Keras layer graph. See log for details.", e);
        }
        graphSettings.writeToFile(objOut);
    }

    /**
     * Reads a Keras network graph from stream and returns its output layers. The entire graph can be accessed via the
     * layers' input (i.e. predecessor node) relationships.
     *
     * @param objIn the stream from which to read the network graph
     * @return the read network graph
     * @throws IOException if failed to read the network graph from stream
     * @throws ClassNotFoundException if a network graph related class (e.g. a layer) could not be found
     */
    public List<DLKerasLayer> readGraphFrom(final ObjectInputStream objIn) throws IOException, ClassNotFoundException {
        try {
            final NodeSettings graphSettings = NodeSettings.readFromFile(objIn);
            final DLKerasLayer[] loadedLayers = new DLKerasLayer[graphSettings.getChildCount()];
            for (int i = graphSettings.getChildCount() - 1 - 1; i >= 0; i--) { // -1 because of saved output indices
                final NodeSettings layerSettings = graphSettings.getNodeSettings(Integer.toString(i));
                // Layers must expose a public nullary constructor.
                final DLKerasLayer layer =
                    (DLKerasLayer)Class.forName(layerSettings.getString(CFG_KEY_LAYER_CLASS)).newInstance();
                // TODO: Avoid redundant creation of layer struct (not instance), should be cached somewhere.
                new DLKerasLayerStructInstance(layer)
                    .loadSettingsFrom(layerSettings.getNodeSettings(CFG_KEY_LAYER_PARAMS));
                if (layer instanceof DLKerasInnerLayer) {
                    final NodeSettings parentIndices = layerSettings.getNodeSettings(CFG_KEY_LAYER_PARENTS);
                    final DLKerasInnerLayer innerLayer = ((DLKerasInnerLayer)layer);
                    for (int j = 0; j < parentIndices.getChildCount(); j++) {
                        innerLayer.setParent(j, loadedLayers[parentIndices.getInt(Integer.toString(j))]);
                    }
                }
                loadedLayers[i] = layer;
            }
            final int[] outputLayerIndices = graphSettings.getIntArray(CFG_KEY_OUTPUT_LAYERS);
            final ArrayList<DLKerasLayer> outputs = new ArrayList<>(outputLayerIndices.length);
            for (int i = 0; i < outputLayerIndices.length; i++) {
                outputs.add(loadedLayers[outputLayerIndices[i]]);
            }
            return outputs;
        } catch (final InvalidSettingsException | InstantiationException | IllegalAccessException e) {
            LOGGER.error(e);
            throw new IOException("An exception occurred while loading the Keras layer graph. See log for details.", e);
        }
    }
}
