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

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.knime.dl.util.DLThrowingLambdas.DLThrowingBiConsumer;

/**
 * Allows iteration over a topological ordering of a Keras layer graph that is specified by a list of output layers
 * (i.e. leaf nodes). Please note that the first invocation of {@link #next()}, {@link #visitNext(DLKerasLayerVisitor)}
 * or the invocation of {@link #visitAll(DLKerasLayerVisitor)} trigger a full traversal of the layer graph to obtain its
 * topological ordering before the actual visiting is carried out. <br>
 * The order of layers of the same depth is governed by the behavior of
 * {@link DLKerasNetworkLayerGraphDepthFirstIterator}.
 *
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public final class DLKerasNetworkLayerGraphTopologicalOrderIterator implements DLKerasNetworkLayerGraphIterator {

    public static List<DLKerasTensorSpecsOutput>
        sortTopologically(final Collection<Entry<DLKerasTensorSpecsOutput, Integer>> maxDepthsFromOutputs) {
        return maxDepthsFromOutputs.stream()
            .sorted(Comparator.comparingInt(Entry<DLKerasTensorSpecsOutput, Integer>::getValue).reversed())
            .map(Entry<DLKerasTensorSpecsOutput, Integer>::getKey).collect(Collectors.toList());
    }

    private final DLKerasNetworkLayerGraphDepthFirstIterator m_depthFirstSearchIterator;

    private final Map<DLKerasTensorSpecsOutput, //
            DLThrowingBiConsumer<DLKerasLayerVisitor, DLKerasTensorSpecsOutput, Exception>> m_layerVisitors =
                new HashMap<>();

    /**
     * Populated in {@link #initializeSortedGraph()}.
     */
    private Map<DLKerasTensorSpecsOutput, Integer> m_maxDepthsFromOutputs;

    /**
     * Populated in {@link #initializeSortedGraph()}.
     */
    private Iterator<DLKerasTensorSpecsOutput> m_layersSortedByDepth;

    /**
     * Creates an iterator over the network graph specified by the given output layers and all their preceding layers.
     *
     * @param outputLayers the output layers that define the graph
     * @throws NullPointerException if the list of output layers or one of the output layers is <code>null</code>
     */
    public DLKerasNetworkLayerGraphTopologicalOrderIterator(final List<DLKerasLayer> outputLayers) {
        m_depthFirstSearchIterator = new DLKerasNetworkLayerGraphDepthFirstIterator(outputLayers);
    }

    @Override
    public boolean hasNext() {
        return m_layersSortedByDepth != null && m_layersSortedByDepth.hasNext() //
            || m_depthFirstSearchIterator.hasNext();
    }

    @Override
    public DLKerasTensorSpecsOutput visitNext(final DLKerasLayerVisitor visitor) {
        if (m_layersSortedByDepth == null) {
            initializeSortedGraph();
        }
        return visitNextInternal(visitor);
    }

    @Override
    public Map<DLKerasTensorSpecsOutput, Integer> visitAll(final DLKerasLayerVisitor visitor) {
        if (m_layersSortedByDepth == null) {
            initializeSortedGraph();
        }
        while (m_layersSortedByDepth.hasNext()) {
            visitNextInternal(visitor);
        }
        return m_maxDepthsFromOutputs;
    }

    private DLKerasTensorSpecsOutput visitNextInternal(final DLKerasLayerVisitor visitor) {
        final DLKerasTensorSpecsOutput layer = m_layersSortedByDepth.next();
        if (visitor != null) {
            try {
                m_layerVisitors.get(layer).accept(visitor, layer);
            } catch (final Exception e) {
                throw new DLNetworkLayerGraphTraversalException(e.getMessage(), e);
            }
        }
        if (!m_layersSortedByDepth.hasNext() && visitor != null) {
            try {
                visitor.noteLayerDepths(m_maxDepthsFromOutputs);
            } catch (final RuntimeException e) {
                throw new DLNetworkLayerGraphTraversalException(e.getMessage(), e);
            }
        }
        return layer;
    }

    private void initializeSortedGraph() {

        m_depthFirstSearchIterator.visitAll(new DLKerasLayerVisitor() {

            @Override
            public void visitOutput(final DLKerasInnerLayer outputLayer) throws Exception {
                m_layerVisitors.put(outputLayer, (v, l) -> v.visitOutput((DLKerasInnerLayer)l));
            }

            @Override
            public void visitHidden(final DLKerasInnerLayer hiddenLayer) throws Exception {
                m_layerVisitors.put(hiddenLayer, (v, l) -> v.visitHidden((DLKerasInnerLayer)l));
            }

            @Override
            public void visitInput(final DLKerasInputLayer inputLayer) throws Exception {
                m_layerVisitors.put(inputLayer, (v, l) -> v.visitInput((DLKerasInputLayer)l));
            }

            @Override
            public void visitInputOutput(final DLKerasInputLayer inputOutputLayer) throws Exception {
                m_layerVisitors.put(inputOutputLayer, (v, l) -> v.visitInputOutput((DLKerasInputLayer)l));
            }

            @Override
            public void visitBaseNetworkOutput(final DLKerasBaseNetworkTensorSpecOutput baseNetworkOutput) {
                m_layerVisitors.put(baseNetworkOutput,
                    (v, l) -> v.visitBaseNetworkOutput((DLKerasBaseNetworkTensorSpecOutput)l));
            }

            @Override
            public void noteLayerDepths(final Map<DLKerasTensorSpecsOutput, Integer> maxDepthsFromOutputs) {
                m_maxDepthsFromOutputs = Collections.unmodifiableMap(maxDepthsFromOutputs);
            }
        });

        m_layersSortedByDepth = sortTopologically(m_maxDepthsFromOutputs.entrySet()).iterator();
    }
}
