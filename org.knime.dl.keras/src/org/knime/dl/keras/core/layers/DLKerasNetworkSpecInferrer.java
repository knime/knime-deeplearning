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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.knime.dl.core.DLDefaultTensorId;
import org.knime.dl.core.DLDefaultTensorSpec;
import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.keras.core.DLKerasNetworkSpec;
import org.knime.dl.keras.core.layers.DLKerasNetworkGraphIterator.DLKerasLayerVisitor;
import org.knime.dl.keras.core.layers.DLKerasNetworkGraphIterator.DLNetworkGraphTraversalException;
import org.knime.dl.keras.core.layers.impl.DLKerasCollectLayer;
import org.knime.dl.keras.tensorflow.core.DLKerasTensorFlowNetworkSpec;

import com.google.common.collect.Sets;

import gnu.trove.TIntHashSet;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public final class DLKerasNetworkSpecInferrer {

    private final List<DLKerasLayer> m_outputLayers;

    private final Map<DLKerasTensorSpecsOutput, List<DLTensorSpec>> m_layerToTensorMap = new HashMap<>();

    private DLKerasNetworkSpec m_inferredSpec = null;

    /**
     * Creates a new instance of this class that allows to infer the specification of the Keras network graph specified
     * by the given output layers and their inputs (i.e. predecessor nodes).
     *
     * @param outputLayers the output layers of the network whose spec to infer
     */
    public DLKerasNetworkSpecInferrer(final List<DLKerasLayer> outputLayers) {
        m_outputLayers = outputLayers;
    }

    Map<DLKerasTensorSpecsOutput, List<DLTensorSpec>> getLayerToTensorMap() {
        if (m_inferredSpec == null) {
            inferNetworkSpec();
        }
        return m_layerToTensorMap;
    }

    /**
     * Infers the specification of the Keras network graph.
     *
     * @return the inferred network spec
     * @throws DLNetworkGraphTraversalException if traversing the network graph failed
     */
    public DLKerasNetworkSpec inferNetworkSpec() {
        if (m_inferredSpec != null) {
            return m_inferredSpec;
        }

        final List<Function<DLKerasNetworkLayerNameGenerator, List<DLTensorSpec>>> inputSpecsToInfer =
            new ArrayList<>(5);
        final List<Function<DLKerasNetworkLayerNameGenerator, List<DLTensorSpec>>> hiddenSpecsToInfer =
            new ArrayList<>(20);
        final List<Function<DLKerasNetworkLayerNameGenerator, List<DLTensorSpec>>> outputSpecsToInfer =
            new ArrayList<>(5);

        final LinkedHashMap<DLKerasNetworkSpec, DLKerasBaseNetworkSpecHelperStruct> baseNetworkSpecs =
            new LinkedHashMap<>();

        new DLKerasNetworkGraphTopologicalOrderIterator(m_outputLayers).visitAll(new DLKerasLayerVisitor() {

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
                final Function<DLKerasNetworkLayerNameGenerator, List<DLTensorSpec>> inferInputHiddenTensorSpecs =
                    new Function<DLKerasNetworkLayerNameGenerator, List<DLTensorSpec>>() {

                        private List<DLTensorSpec> m_inputHiddenTensorSpec;

                        @Override
                        public List<DLTensorSpec> apply(final DLKerasNetworkLayerNameGenerator gen) {
                            if (m_inputHiddenTensorSpec == null) {
                                m_inputHiddenTensorSpec = inferTensorSpecs(gen, inputLayer);
                            }
                            return m_inputHiddenTensorSpec;
                        }
                    };
                inputSpecsToInfer.add(inferInputHiddenTensorSpecs);
                hiddenSpecsToInfer.add(inferInputHiddenTensorSpecs);
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
                m_layerToTensorMap.computeIfAbsent(baseNetworkOutput,
                    bno -> Arrays.asList(baseNetworkSpec.getOutputSpecs()));
                // Re-use base network if it's already connected to another layer.
                // TODO: This behavior may not be intended.
                final DLKerasBaseNetworkSpecHelperStruct baseNetworkHelper =
                    baseNetworkSpecs.computeIfAbsent(baseNetworkSpec, bns -> {
                        final DLKerasBaseNetworkSpecHelperStruct bnh =
                            new DLKerasBaseNetworkSpecHelperStruct(baseNetworkSpec);
                        inputSpecsToInfer.add(bnh::inferInputTensorSpecs);
                        hiddenSpecsToInfer.add(bnh::inferHiddenTensorSpecs);
                        outputSpecsToInfer.add(bnh::inferOutputTensorSpecs);
                        return bnh;
                    });
                final int baseNetworkOutputIndex = baseNetworkOutput.getBaseNetworkOutputIndex();
                baseNetworkHelper.m_connectedOutputs.add(baseNetworkOutputIndex);
                // TODO: Support appending to hidden layers.
            }
        });

        // Base network layer names are reserved.
        final DLKerasNetworkLayerNameGenerator layerNameGen =
            DLKerasNetworkLayerNameGenerator.createFromBaseNetworks(baseNetworkSpecs.keySet());

        final DLTensorSpec[] inputSpecs = collectTensorSpecs(layerNameGen, inputSpecsToInfer);
        final DLTensorSpec[] hiddenSpecs = collectTensorSpecs(layerNameGen, hiddenSpecsToInfer);
        final DLTensorSpec[] outputSpecs = collectTensorSpecs(layerNameGen, outputSpecsToInfer);

        // the hidden specs may contain duplicates if collect layers are used
        LinkedHashSet<DLTensorSpec> distinctHiddenSpecs = new LinkedHashSet<>(Arrays.asList(hiddenSpecs));
        LinkedHashSet<DLTensorSpec> distinctOutputSpecs = new LinkedHashSet<>(Arrays.asList(outputSpecs));

        // if collect layers are used, output and hidden layers may contain the same specs in which case
        // those specs are actually outputs
        Set<DLTensorSpec> nonOutputHiddenSpecs = Sets.difference(distinctHiddenSpecs, distinctOutputSpecs);

        // TODO: Only TensorFlow is supported at the moment.
        m_inferredSpec = new DLKerasTensorFlowNetworkSpec(inputSpecs,
            nonOutputHiddenSpecs.toArray(new DLTensorSpec[nonOutputHiddenSpecs.size()]), outputSpecs);
        return m_inferredSpec;
    }

    private List<DLTensorSpec> inferTensorSpecs(final DLKerasNetworkLayerNameGenerator layerNameGen,
        final DLKerasLayer layer) {
        if (layer instanceof DLKerasCollectLayer) {
            return handleCollectLayer((DLKerasCollectLayer)layer);
        }
        List<DLTensorSpec> tensorSpecs;
        try {
            tensorSpecs = layer.getOutputSpecs();
        } catch (final DLInvalidTensorSpecException e) {
            throw new DLNetworkGraphTraversalException(e.getMessage(), e);
        }
        final List<DLTensorSpec> amendedTensorSpecs = new ArrayList<>(tensorSpecs.size());
        final String layerName = layerNameGen.getNextLayerName(layer);
        for (int i = 0; i < tensorSpecs.size(); i++) {
            final DLTensorSpec tensorSpec = tensorSpecs.get(i);
            // We cannot represent Keras layer nodes via KNIME nodes (unless we introduce "layer define" and
            // "layer apply" nodes). Thus, the node index is always zero.
            final String tensorName = layerNameGen.getOutputTensorName(layerName, 0, i);
            amendedTensorSpecs.add(amendSpec(tensorSpec, tensorName));
        }
        m_layerToTensorMap.put(layer, amendedTensorSpecs);
        return amendedTensorSpecs;
    }

    private List<DLTensorSpec> handleCollectLayer(final DLKerasCollectLayer layer) {
        List<DLTensorSpec> collectedParentTensors = new ArrayList<>();
        for (int i = 0; i < layer.getNumParents(); i++) {
            DLKerasTensorSpecsOutput parent = layer.getParent(i);
            List<DLTensorSpec> parentTensors = m_layerToTensorMap.get(parent);
            if (parentTensors == null) {
                throw new IllegalStateException("Parents must be visited prior to children.");
            }
            collectedParentTensors.addAll(parentTensors);
        }
        m_layerToTensorMap.put(layer, collectedParentTensors);
        return collectedParentTensors;
    }

    private DLTensorSpec amendSpec(final DLTensorSpec tensorSpec, final String tensorName) {
        final DLTensorSpec amendedTensorSpec;
        if (tensorSpec.getBatchSize().isPresent()) {
            amendedTensorSpec = new DLDefaultTensorSpec(new DLDefaultTensorId(tensorName), tensorName,
                tensorSpec.getBatchSize().getAsLong(), tensorSpec.getShape(), tensorSpec.getElementType(),
                tensorSpec.getDimensionOrder());
        } else {
            amendedTensorSpec = new DLDefaultTensorSpec(new DLDefaultTensorId(tensorName), tensorName,
                tensorSpec.getShape(), tensorSpec.getElementType(), tensorSpec.getDimensionOrder());
        }
        return amendedTensorSpec;
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

        private List<DLTensorSpec> inferHiddenTensorSpecs(final DLKerasNetworkLayerNameGenerator generator) {
            final DLTensorSpec[] allHiddenTensorSpecs = m_networkSpec.getHiddenOutputSpecs();
            final List<DLTensorSpec> hiddenTensorSpecs =
                new ArrayList<>(allHiddenTensorSpecs.length + m_connectedOutputs.size());
            hiddenTensorSpecs.addAll(Arrays.asList(allHiddenTensorSpecs));
            final DLTensorSpec[] allOutTensorSpecs = m_networkSpec.getOutputSpecs();
            for (int i = 0; i < allOutTensorSpecs.length; i++) {
                // Connected output specs become hidden specs.
                if (m_connectedOutputs.contains(i)) {
                    hiddenTensorSpecs.add(allOutTensorSpecs[i]);
                }
            }
            return hiddenTensorSpecs;
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
