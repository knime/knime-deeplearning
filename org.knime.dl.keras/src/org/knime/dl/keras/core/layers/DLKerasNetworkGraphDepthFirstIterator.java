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

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Allows depth-first search on a Keras layer graph that is specified by a list of output layers (i.e. leaf nodes). The
 * search begins at the first output layer in the list and follows the first (then the second etc.) input (i.e. parent)
 * of a layer if one possesses multiple inputs.
 *
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public final class DLKerasNetworkGraphDepthFirstIterator implements DLKerasNetworkGraphIterator {

    private final Deque<DLKerasTensorSpecsOutput> m_pendingLayers = new ArrayDeque<>();

    private final LinkedHashMap<DLKerasTensorSpecsOutput, Integer> m_layerDepths = new LinkedHashMap<>();

    private final LinkedHashMap<DLKerasInnerLayer, DLKerasTensorSpecsOutput[]> m_layerParents = new LinkedHashMap<>();

    /**
     * Creates an iterator over the network graph specified by the given output layers and all their preceding layers.
     *
     * @param outputLayers the output layers that define the graph
     * @throws NullPointerException if the list of output layers or one of the output layers is <code>null</code>
     */
    public DLKerasNetworkGraphDepthFirstIterator(final List<DLKerasLayer> outputLayers) {
        for (int i = 0; i < outputLayers.size(); i++) {
            final DLKerasLayer output = outputLayers.get(i);
            if (output == null) {
                throw new NullPointerException("Keras output layer at output index " + i + " is null.");
            }
            m_pendingLayers.add(output);
        }
    }

    @Override
    public boolean hasNext() {
        return !m_pendingLayers.isEmpty();
    }

    /**
     * Visits the next layer in the network graph and passes it to the adequate method of the given visitor.
     *
     * @param visitor the visitor to which the next layer is passed, may be <code>null</code>
     * @return the visited layer
     * @throws DLNetworkGraphTraversalException if an exception occurred while traversing the graph
     * @throws NoSuchElementException if the graph has no more elements
     */
    @Override
    public DLKerasTensorSpecsOutput visitNext(final DLKerasLayerVisitor visitor) {
        final DLKerasTensorSpecsOutput layer = m_pendingLayers.pop();
        if (layer instanceof DLKerasInnerLayer) {
            final DLKerasInnerLayer innerLayer = (DLKerasInnerLayer)layer;
            final DLKerasTensorSpecsOutput[] parents = getParents(innerLayer);
            m_layerParents.put(innerLayer, parents);
            for (int i = parents.length - 1; i >= 0; i--) {
                final DLKerasTensorSpecsOutput parent = parents[i];
                // Parent may already be present if it's a fork.
                if (!m_layerDepths.containsKey(parent)) {
                    m_pendingLayers.push(parent);
                }
            }
            // Must be an output layer if not already contained.
            final int depth = m_layerDepths.computeIfAbsent(layer, k -> 0);
            updateDepths(parents, depth + 1);
            if (visitor != null) {
                try {
                    // Pseudo visitor pattern.
                    if (depth == 0) {
                        visitor.visitOutput((DLKerasInnerLayer)layer);
                    } else {
                        visitor.visitHidden((DLKerasInnerLayer)layer);
                    }
                } catch (final Exception e) {
                    throw new DLNetworkGraphTraversalException(e.getMessage(), e);
                }
            }
        } else if (layer instanceof DLKerasInputLayer) {
            // Must be an output layer if not already contained.
            final int depth = m_layerDepths.computeIfAbsent(layer, k -> 0);
            if (visitor != null) {
                try {
                    if (depth == 0) {
                        visitor.visitInputOutput((DLKerasInputLayer)layer);
                    } else {
                        visitor.visitInput((DLKerasInputLayer)layer);
                    }
                } catch (final Exception e) {
                    throw new DLNetworkGraphTraversalException(e.getMessage(), e);
                }
            }
        } else if (layer instanceof DLKerasBaseNetworkTensorSpecOutput) {
            try {
                visitor.visitBaseNetworkOutput((DLKerasBaseNetworkTensorSpecOutput)layer);
            } catch (final Exception e) {
                throw new DLNetworkGraphTraversalException(e.getMessage(), e);
            }
        } else {
            throw new IllegalStateException("Keras layer '" + layer.getClass().getTypeName() + "' (" + layer
                + ") is not marked as inner layer, input layer or base network output."
                + " This is an implementation error.");
        }
        if (!hasNext() && visitor != null) {
            try {
                visitor.noteLayerDepths(Collections.unmodifiableMap(m_layerDepths));
            } catch (final RuntimeException e) {
                throw new DLNetworkGraphTraversalException(e.getMessage(), e);
            }
        }
        return layer;
    }

    /**
     * Visits all layers in the network graph (DFS) and passes each of them to the adequate method of the given visitor.
     *
     * @param visitor the visitor to which the layers are passed
     * @throws DLNetworkGraphTraversalException if an exception occurred while traversing the graph
     */
    @Override
    public Map<DLKerasTensorSpecsOutput, Integer> visitAll(final DLKerasLayerVisitor visitor) {
        while (hasNext()) {
            visitNext(visitor);
        }
        return Collections.unmodifiableMap(m_layerDepths);
    }

    private DLKerasTensorSpecsOutput[] getParents(final DLKerasInnerLayer layer) {
        final DLKerasTensorSpecsOutput[] parents = new DLKerasTensorSpecsOutput[layer.getNumParents()];
        for (int i = 0; i < parents.length; i++) {
            parents[i] = layer.getParent(i);
            if (parents[i] == null) {
                throw new IllegalStateException("Parent at input index " + i + " of Keras layer '"
                    + layer.getClass().getTypeName() + "' (" + layer + ") is null.");
            }
        }
        return parents;
    }

    private void updateDepths(final DLKerasTensorSpecsOutput[] layers, final int depth) {
        for (final DLKerasTensorSpecsOutput layer : layers) {
            final Integer layerDepth = m_layerDepths.get(layer);
            if (layerDepth == null || layerDepth < depth) {
                m_layerDepths.put(layer, depth);
                if (layer instanceof DLKerasInnerLayer) {
                    final DLKerasTensorSpecsOutput[] parents = m_layerParents.get(layer);
                    if (parents != null) {
                        updateDepths(parents, depth + 1);
                    }
                }
            }
        }
    }
}
