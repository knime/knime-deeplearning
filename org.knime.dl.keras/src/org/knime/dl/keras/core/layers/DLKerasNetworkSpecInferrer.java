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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Function;

import org.knime.dl.core.DLDefaultTensorId;
import org.knime.dl.core.DLDefaultTensorSpec;
import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.keras.core.DLKerasGenericNetworkSpec;
import org.knime.dl.keras.core.DLKerasNetworkSpec;
import org.knime.dl.keras.core.layers.DLKerasNetworkLayerGraphIterator.DLKerasLayerVisitor;
import org.knime.dl.keras.core.layers.DLKerasNetworkLayerGraphIterator.DLNetworkLayerGraphTraversalException;

import gnu.trove.TIntHashSet;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public final class DLKerasNetworkSpecInferrer {

    private final List<DLKerasLayer> m_outputLayers;

    /**
     * Creates a new instance of this class that allows to infer the specification of the Keras network graph specified
     * by the given output layers and their inputs (i.e. predecessor nodes).
     *
     * @param outputLayers the output layers of the network whose spec to infer
     */
    public DLKerasNetworkSpecInferrer(final List<DLKerasLayer> outputLayers) {
        m_outputLayers = outputLayers;
    }

    /**
     * Infers the specification of the Keras network graph.
     *
     * @return the inferred network spec
     * @throws DLNetworkLayerGraphTraversalException if traversing the network graph failed
     */
    public DLKerasNetworkSpec inferNetworkSpec() {
        final List<Function<DLKerasNetworkLayerNameGenerator, List<DLTensorSpec>>> inputSpecsToInfer =
            new ArrayList<>(5);
        final List<Function<DLKerasNetworkLayerNameGenerator, List<DLTensorSpec>>> hiddenSpecsToInfer =
            new ArrayList<>(20);
        final List<Function<DLKerasNetworkLayerNameGenerator, List<DLTensorSpec>>> outputSpecsToInfer =
            new ArrayList<>(5);

        final LinkedHashMap<DLKerasNetworkSpec, DLKerasBaseNetworkSpecHelperStruct> baseNetworkSpecs =
            new LinkedHashMap<>();

        new DLKerasNetworkLayerGraphTopologicalOrderIterator(m_outputLayers).visitAll(new DLKerasLayerVisitor() {

            @Override
            public void visitOutput(final DLKerasInnerLayer outputLayer) throws Exception {
                outputSpecsToInfer.add(gen -> inferTensorSpecs(gen, outputLayer));
            }

            @Override
            public void visitHidden(final DLKerasInnerLayer hiddenLayer) throws Exception {
                hiddenSpecsToInfer.add(gen -> inferTensorSpecs(gen, hiddenLayer));
            }

            @Override
            public void visitInput(final DLKerasInputLayer inputLayer) throws Exception {
                inputSpecsToInfer.add(gen -> inferTensorSpecs(gen, inputLayer));
            }

            @Override
            public void visitInputOutput(final DLKerasInputLayer inputOutputLayer) throws Exception {
                final Function<DLKerasNetworkLayerNameGenerator, List<DLTensorSpec>> inferInputOutputTensorSpecs =
                    new Function<DLKerasNetworkLayerNameGenerator, List<DLTensorSpec>>() {

                        private List<DLTensorSpec> m_inputOutputTensorSpec;

                        @Override
                        public List<DLTensorSpec> apply(final DLKerasNetworkLayerNameGenerator gen) {
                            if (m_inputOutputTensorSpec == null) {
                                m_inputOutputTensorSpec = inferTensorSpecs(gen, inputOutputLayer);
                            }
                            return m_inputOutputTensorSpec;
                        }
                    };
                inputSpecsToInfer.add(inferInputOutputTensorSpecs);
                outputSpecsToInfer.add(inferInputOutputTensorSpecs);
            }

            @Override
            public void visitBaseNetworkOutput(final DLKerasBaseNetworkTensorSpecOutput baseNetworkOutput) {
                final DLKerasNetworkSpec baseNetworkSpec = baseNetworkOutput.getBaseNetworkSpec();
                DLKerasBaseNetworkSpecHelperStruct baseNetworkHelper = baseNetworkSpecs.get(baseNetworkSpec);
                // Re-use base network if it's already connected to another layer.
                // TODO: This behavior may not be intended.
                if (baseNetworkHelper == null) {
                    baseNetworkHelper = new DLKerasBaseNetworkSpecHelperStruct(baseNetworkSpec);
                    baseNetworkSpecs.put(baseNetworkSpec, baseNetworkHelper);
                    inputSpecsToInfer.add(baseNetworkHelper::inferInputTensorSpecs);
                    outputSpecsToInfer.add(baseNetworkHelper::inferOutputTensorSpecs);
                }
                final int baseNetworkOutputIndex = baseNetworkOutput.getBaseNetworkOutputIndex();
                baseNetworkHelper.m_connectedOutputs.add(baseNetworkOutputIndex);
            }
        });

        // Base network layer names are reserved.
        final DLKerasNetworkLayerNameGenerator layerNameGen =
            DLKerasNetworkLayerNameGenerator.createFromBaseNetworks(baseNetworkSpecs.keySet());

        final DLTensorSpec[] inputSpecs = collectTensorSpecs(layerNameGen, inputSpecsToInfer);
        final DLTensorSpec[] hiddenSpecs = new DLTensorSpec[0];
        // TODO: uncomment collectTensorSpecs(layerNameGen, hiddenSpecsToInfer);
        final DLTensorSpec[] outputSpecs = collectTensorSpecs(layerNameGen, outputSpecsToInfer);

        return new DLKerasGenericNetworkSpec(inputSpecs, hiddenSpecs, outputSpecs);
    }

    private List<DLTensorSpec> inferTensorSpecs(final DLKerasNetworkLayerNameGenerator layerNameGen,
        final DLKerasLayer layer) {
        List<DLTensorSpec> tensorSpecs;
        try {
            tensorSpecs = layer.getOutputSpecs();
        } catch (final DLInvalidTensorSpecException e) {
            throw new DLNetworkLayerGraphTraversalException(e.getMessage(), e);
        }
        final List<DLTensorSpec> amendedTensorSpecs = new ArrayList<>(tensorSpecs.size());
        for (int i = 0; i < tensorSpecs.size(); i++) {
            final DLTensorSpec tensorSpec = tensorSpecs.get(i);
            final DLTensorSpec amendedTensorSpec;
            final String layerName = layerNameGen.getNextLayerName(layer);
            // We cannot represent Keras layer nodes via KNIME nodes (unless we introduce "layer define" and
            // "layer apply" nodes). Thus, the node index is always zero.
            final String tensorName = layerNameGen.getOutputTensorName(layerName, 0, i);
            if (tensorSpec.getBatchSize().isPresent()) {
                amendedTensorSpec = new DLDefaultTensorSpec(new DLDefaultTensorId(tensorName), tensorName,
                    tensorSpec.getBatchSize().getAsLong(), tensorSpec.getShape(), tensorSpec.getElementType(),
                    tensorSpec.getDimensionOrder());
            } else {
                amendedTensorSpec = new DLDefaultTensorSpec(new DLDefaultTensorId(tensorName), tensorName,
                    tensorSpec.getShape(), tensorSpec.getElementType(), tensorSpec.getDimensionOrder());
            }
            amendedTensorSpecs.add(amendedTensorSpec);
        }
        return amendedTensorSpecs;
    }

    private DLTensorSpec[] collectTensorSpecs(final DLKerasNetworkLayerNameGenerator layerNameGen,
        final List<Function<DLKerasNetworkLayerNameGenerator, List<DLTensorSpec>>> specsToInfer) {
        return specsToInfer.stream().flatMap(f -> f.apply(layerNameGen).stream()).toArray(l -> new DLTensorSpec[l]);
    }

    private static class DLKerasBaseNetworkSpecHelperStruct {

        private final DLKerasNetworkSpec m_networkSpec;

        private final TIntHashSet m_connectedOutputs = new TIntHashSet(5);

        private DLKerasBaseNetworkSpecHelperStruct(final DLKerasNetworkSpec networkSpec) {
            m_networkSpec = networkSpec;
        }

        private List<DLTensorSpec> inferInputTensorSpecs(final DLKerasNetworkLayerNameGenerator generator) {
            return Arrays.asList(m_networkSpec.getInputSpecs());
        }

        private List<DLTensorSpec> inferOutputTensorSpecs(final DLKerasNetworkLayerNameGenerator generator) {
            final DLTensorSpec[] allOutTensorSpecs = m_networkSpec.getOutputSpecs();
            final List<DLTensorSpec> outTensorSpecs =
                new ArrayList<>(allOutTensorSpecs.length - m_connectedOutputs.size());
            for (int i = 0; i < allOutTensorSpecs.length; i++) {
                if (!m_connectedOutputs.contains(i)) {
                    outTensorSpecs.add(allOutTensorSpecs[i]);
                }
            }
            return outTensorSpecs;
        }
    }
}
