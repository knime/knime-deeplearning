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
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;

import org.knime.dl.core.DLUncheckedException;

/**
 * Performs a depth-first search on a Keras layer graph specified by a list of output (i.e. leaf) nodes.
 *
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public final class DLKerasNetworkLayerGraphIterator implements Iterator<DLKerasLayer> {

    private final Deque<DLKerasLayer> m_pendingLayers = new ArrayDeque<>();

    private final LinkedHashSet<DLKerasLayer> m_encounteredLayers = new LinkedHashSet<>();

    /**
     * Creates an iterator over the network graph specified by the given output layers and their parents (i.e.
     * predecessor nodes) to a stream.
     *
     * @param outputLayers the output layers that define the graph
     * @throws NullPointerException if the list of output layers or one of the output layers is <code>null</code>
     */
    public DLKerasNetworkLayerGraphIterator(final List<DLKerasLayer> outputLayers) {
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
     * Calls {@link #visitNext(DLKerasLayerVisitor) visitNext(null)}.
     *
     * @throws DLNetworkLayerGraphTraversalException if an exception occurred while traversing the graph
     * @throws NoSuchElementException if the graph has no more elements
     */
    @Override
    public DLKerasLayer next() {
        return visitNext(null);
    }

    /**
     * Visits the next layer in the network graph and passes it to the adequate method of the given visitor.
     *
     * @param visitor the visitor to which the next layer is passed, may be <code>null</code>
     * @return the visited layer
     * @throws DLNetworkLayerGraphTraversalException if an exception occurred while traversing the graph
     * @throws NoSuchElementException if the graph has no more elements
     */
    public DLKerasLayer visitNext(final DLKerasLayerVisitor visitor) {
        final DLKerasLayer layer = m_pendingLayers.pop();
        if (layer instanceof DLKerasInnerLayer) {
            boolean isOutputNode = false;
            if (!m_encounteredLayers.contains(layer)) {
                m_encounteredLayers.add(layer);
                isOutputNode = true;
            }
            final DLKerasLayer[] parents = getParents((DLKerasInnerLayer)layer);
            for (int i = parents.length - 1; i >= 0; i--) {
                final DLKerasLayer parent = parents[i];
                if (!m_encounteredLayers.contains(parent)) {
                    m_encounteredLayers.add(parent);
                    m_pendingLayers.push(parent);
                }
            }
            if (visitor != null) {
                try {
                    // pseudo visitor pattern
                    if (isOutputNode) {
                        visitor.visitOutput((DLKerasInnerLayer)layer);
                    } else {
                        visitor.visitHidden((DLKerasInnerLayer)layer);
                    }
                } catch (final Exception e) {
                    throw new DLNetworkLayerGraphTraversalException(e.getMessage(), e);
                }
            }
        } else if (layer instanceof DLKerasInputLayer) {
            if (visitor != null) {
                try {
                    visitor.visitInput((DLKerasInputLayer)layer);
                } catch (final Exception e) {
                    throw new DLNetworkLayerGraphTraversalException(e.getMessage(), e);
                }
            }
        } else {
            throw new IllegalStateException(
                "Keras layer '" + layer.getClass().getTypeName() + "' (" + layer.getBackendRepresentation(null)
                    + ") is neither marked as inner layer nor as input layer." + " This is an implementation error.");
        }
        return layer;
    }

    /**
     * Visits all layers in the network graph (DFS) and passes each of them to the adequate method of the given visitor.
     *
     * @param visitor the visitor to which the layers are passed
     * @throws DLNetworkLayerGraphTraversalException if an exception occurred while traversing the graph
     */
    public void visitAll(final DLKerasLayerVisitor visitor) {
        while (hasNext()) {
            visitNext(visitor);
        }
    }

    private DLKerasLayer[] getParents(final DLKerasInnerLayer layer) {
        final DLKerasLayer[] parents = layer.getParents();
        for (int i = 0; i < parents.length; i++) {
            if (parents[i] == null) {
                throw new IllegalStateException("Parent at input index " + i + " of Keras layer '"
                    + layer.getClass().getTypeName() + "' (" + layer.getBackendRepresentation(null) + ") is null.");
            }
        }
        return parents;
    }

    /**
     * The visitor used to traverse the Keras network graph.
     */
    public static interface DLKerasLayerVisitor {

        /**
         * Visitor method for output layers.
         */
        void visitOutput(final DLKerasInnerLayer outputLayer) throws Exception;

        /**
         * Visitor method for hidden layers.
         */
        void visitHidden(DLKerasInnerLayer hiddenLayer) throws Exception;

        /**
         * Visitor method for input layers.
         */
        void visitInput(DLKerasInputLayer inputLayer) throws Exception;
    }

    /**
     * The exception that is thrown if an exception occurs during traversal of the network graph.
     */
    public static class DLNetworkLayerGraphTraversalException extends DLUncheckedException {

        private static final long serialVersionUID = 1L;

        public DLNetworkLayerGraphTraversalException(final String message, final Throwable cause) {
            super(message != null ? message : "An exception occurred while traversing the Keras network layer graph.",
                cause);
        }
    }
}
