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

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.NoSuchElementException;

import org.knime.dl.core.DLUncheckedException;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public interface DLKerasNetworkLayerGraphIterator extends Iterator<DLKerasTensorSpecsOutput> {

    /**
     * Behaves like {@link #visitNext(DLKerasLayerVisitor) visitNext(null)}.
     *
     * @return the next layer
     * @throws DLNetworkLayerGraphTraversalException if an exception occurred while traversing the graph
     * @throws NoSuchElementException if the graph has no more elements
     */
    @Override
    default DLKerasTensorSpecsOutput next() {
        return visitNext(null);
    }

    /**
     * Visits the next layer in the network graph and passes it to the adequate method of the given visitor.
     *
     * @param visitor the visitor to which the next layer is passed, may be <code>null</code> in which case this simply
     *            returns the next layer
     * @return the next layer
     * @throws DLNetworkLayerGraphTraversalException if an exception occurred while traversing the graph
     * @throws NoSuchElementException if the graph has no more elements
     */
    DLKerasTensorSpecsOutput visitNext(DLKerasLayerVisitor visitor);

    /**
     * Visits all layers in the network graph and passes each of them to the adequate method of the given visitor.
     *
     * @param visitor the visitor to which the layers are passed
     * @throws DLNetworkLayerGraphTraversalException if an exception occurred while traversing the graph
     */
    void visitAll(DLKerasLayerVisitor visitor);

    /**
     * The visitor used to traverse the Keras network graph.
     */
    public static interface DLKerasLayerVisitor {

        /**
         * Visitor method for output layers that are not input layers.
         */
        void visitOutput(final DLKerasInnerLayer outputLayer) throws Exception;

        /**
         * Visitor method for hidden layers.
         */
        void visitHidden(DLKerasInnerLayer hiddenLayer) throws Exception;

        /**
         * Visitor method for input layers that are not output layers. Also see
         * {@link #visitInputOutput(DLKerasInputLayer)}.
         */
        void visitInput(DLKerasInputLayer inputLayer) throws Exception;

        /**
         * Visitor method for input layers that are also output layers. Also see {@link #visitInput(DLKerasInputLayer)}.
         */
        void visitInputOutput(DLKerasInputLayer inputOutputLayer) throws Exception;

        /**
         * Visitor method for base network tensor outputs.
         */
        void visitBaseNetworkOutput(DLKerasBaseNetworkTensorSpecOutput baseNetworkOutput);

        /**
         * Called after the last layer was visited.
         *
         * @param maxDepthsFromOutputs a map that contains, for each layer, the maximum distance to any output layer
         */
        default void noteLayerDepths(final LinkedHashMap<DLKerasTensorSpecsOutput, Integer> maxDepthsFromOutputs) {
            // no op - most implementations won't need this information
        }
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
