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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.knime.dl.core.DLCanceledExecutionException;
import org.knime.dl.core.DLInvalidDestinationException;
import org.knime.dl.core.DLInvalidEnvironmentException;
import org.knime.dl.core.DLInvalidSourceException;
import org.knime.dl.core.DLNetworkLocation;
import org.knime.dl.core.DLNotCancelable;
import org.knime.dl.core.DLTensorId;
import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.keras.core.DLKerasAbstractCommands;
import org.knime.dl.keras.core.DLKerasNetwork;
import org.knime.dl.keras.core.DLKerasNetworkLoader;
import org.knime.dl.keras.core.DLKerasNetworkSpec;
import org.knime.dl.keras.core.layers.DLKerasNetworkGraphIterator.DLKerasLayerVisitor;
import org.knime.dl.keras.core.layers.DLKerasNetworkGraphIterator.DLNetworkGraphTraversalException;
import org.knime.dl.keras.tensorflow.core.DLKerasTensorFlowNetwork;
import org.knime.dl.python.core.DLPythonDefaultContext;
import org.knime.dl.python.core.DLPythonNetworkHandle;
import org.knime.dl.python.core.DLPythonNetworkLoader;
import org.knime.dl.python.core.DLPythonNetworkLoaderRegistry;
import org.knime.dl.python.util.DLPythonSourceCodeBuilder;
import org.knime.dl.python.util.DLPythonUtils;

import gnu.trove.TIntHashSet;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public final class DLKerasNetworkMaterializer {

    private final List<DLKerasLayer> m_outputLayers;

    private final DLNetworkLocation m_saveLocation;

    /**
     * Creates a new instance of this class that allows to materialize the Keras network graph specified by the given
     * output layers and their inputs (i.e. predecessor nodes) and save it to the given <code>URL</code>.
     *
     * @param outputLayers the output layers of the network to materialize
     * @param saveLocation the location where the materialized network is saved
     */
    public DLKerasNetworkMaterializer(final List<DLKerasLayer> outputLayers, final DLNetworkLocation saveLocation) {
        m_outputLayers = outputLayers;
        m_saveLocation = saveLocation;
    }

    /**
     * Materializes the Keras network graph.
     *
     * @return the materialized network
     * @throws DLNetworkGraphTraversalException if traversing the network graph failed
     * @throws DLInvalidEnvironmentException if materialization in the back end failed
     * @throws DLInvalidSourceException if materialization involved an existing base network whose source is unavailable
     *             or invalid
     * @throws IOException if failed to materialize and save the network due to I/O related errors
     */
    public DLKerasNetwork materialize() throws DLInvalidEnvironmentException, DLInvalidSourceException, IOException {
        final DLKerasNetworkSpecInferrer specInferrer = new DLKerasNetworkSpecInferrer(m_outputLayers);
        specInferrer.inferNetworkSpec();
        // Parse layer graph.
        final DLKerasNetworkMaterializerParser parser = new DLKerasNetworkMaterializerParser(
            specInferrer.getLayerToTensorMap());
        new DLKerasNetworkGraphDepthFirstIterator(m_outputLayers).visitAll(parser);

        // TODO: Hard-coded for the moment.
        final Class<DLKerasTensorFlowNetwork> backend = DLKerasTensorFlowNetwork.class;

        final DLPythonNetworkLoader<? extends DLKerasNetwork> loader = DLPythonNetworkLoaderRegistry.getInstance()
            .getNetworkLoader(backend).orElseThrow(() -> new IllegalStateException("Back end for Keras network type '"
                + backend.getName() + "' is missing. " + "Are you missing a KNIME Deep Learning extension?"));

        try (final DLKerasAbstractCommands commands =
            ((DLKerasNetworkLoader<?>)loader).createCommands(new DLPythonDefaultContext())) {
            // Load base networks (if any). Make base networks available on Python side for later. Collect base network
            // specs.We need the network specs (a) to reserve the layer names that are already present in the base
            // networks and (b) to specify the inputs and outputs of the new network that come from the base networks.
            final LinkedHashMap<DLKerasNetworkSpec, DLKerasBaseNetworkHelperStruct> baseNetworks =
                parser.m_baseNetworks;
            final List<DLKerasNetworkSpec> baseNetworkSpecs = retrieveBaseNetworkSpecs(loader, commands, baseNetworks);

            // Base network layer names are reserved.
            final DLKerasNetworkLayerNameGenerator layerNameGen =
                DLKerasNetworkLayerNameGenerator.createFromBaseNetworks(baseNetworkSpecs);

            // Topological ordering.
            final List<DLKerasTensorSpecsOutput> layersSortedByDepth =
                DLKerasNetworkGraphTopologicalOrderIterator.sortTopologically(parser.m_maxDepthsFromOutputs.entrySet());

            // Generate code lines according to the topological ordering above. This ensures that each inner layer's
            // inputs are generated before itself and that we get an "intuitive" layer naming order ("layerX_1" "before"
            // "layerX_2" etc.).
            final StringJoiner generatedCodeJoiner = new StringJoiner("\n");
            for (final DLKerasTensorSpecsOutput layer : layersSortedByDepth) {
                generatedCodeJoiner.add(parser.m_codeLinesToGenerate.get(layer).apply(layerNameGen));
            }
            final String generatedCode = generatedCodeJoiner.toString();

            // Execute generated code and build network whose lists of inputs and outputs (possibly) contain inputs and
            // outputs of base networks which have to be "expanded" (base network -> inputs/outputs) first.
            // Make generated network available on Python side.
            final DLPythonSourceCodeBuilder b = DLPythonUtils.createSourceCodeBuilder() //
                .n("import DLPythonNetwork") //
                .n(baseNetworks.values(),
                    n -> n.m_variable + " = DLPythonNetwork.get_network(\"" + n.m_variable + "\").model") //
                .n("import keras").n(generatedCode) //
                .n("generated_network = keras.models.Model(") //
                // All inputs and (non-connected) outputs of the base networks are kept.
                // TODO: This behavior may not be intended.
                .a("inputs=[" + String.join(",", expandNetworkInputs(parser.m_inputVariables)) + "]").a(", ") //
                .a("outputs=[" + String.join(",", expandNetworkOutputs(parser.m_outputVariables)) + "]") //
                .a(")") //
                .n("import DLPythonNetworkType") //
                .n("network_type = DLPythonNetworkType.get_model_network_type(generated_network)") //
                .n("DLPythonNetwork.add_network(network_type.wrap_model(generated_network), \"generated_network\")");
            try {
                commands.getContext(DLNotCancelable.INSTANCE).executeInKernel(b.toString(), DLNotCancelable.INSTANCE);
            } catch (final DLCanceledExecutionException e) {
                // Won't happen
            }

            final DLPythonNetworkHandle handle = new DLPythonNetworkHandle("generated_network");
            try {
                loader.save(handle, m_saveLocation.getURI(), commands.getContext(DLNotCancelable.INSTANCE),
                    DLNotCancelable.INSTANCE);
            } catch (final DLInvalidDestinationException | DLCanceledExecutionException e) {
                throw new IOException(e);
            }
            try {
                return loader.fetch(handle, m_saveLocation, commands.getContext(DLNotCancelable.INSTANCE),
                    DLNotCancelable.INSTANCE);
            } catch (final DLInvalidSourceException | DLCanceledExecutionException e) {
                throw new IOException(e);
            }
        }
    }

    private static List<DLKerasNetworkSpec> retrieveBaseNetworkSpecs(
        final DLPythonNetworkLoader<? extends DLKerasNetwork> loader, final DLKerasAbstractCommands commands,
        final LinkedHashMap<DLKerasNetworkSpec, DLKerasBaseNetworkHelperStruct> baseNetworks)
        throws DLInvalidSourceException, DLInvalidEnvironmentException, IOException {
        final List<DLKerasNetworkSpec> baseNetworkSpecs = new ArrayList<>(baseNetworks.size());
        for (final DLKerasBaseNetworkHelperStruct baseNetworkHelper : baseNetworks.values()) {
            baseNetworkSpecs.add(baseNetworkHelper.m_networkSpec);
            try {
                final DLPythonNetworkHandle baseNetworkHandle =
                    loader.load(baseNetworkHelper.m_networkSource.getURI(),
                        commands.getContext(DLNotCancelable.INSTANCE), true, DLNotCancelable.INSTANCE);
                final DLPythonSourceCodeBuilder b = DLPythonUtils.createSourceCodeBuilder() //
                    .n("import DLPythonNetwork") //
                    .n("DLPythonNetwork.add_network(") //
                    /**/ .a("DLPythonNetwork.get_network(").as(baseNetworkHandle.getIdentifier()).a("), ") //
                    /**/ .as(baseNetworkHelper.m_variable).a(")");
                commands.getContext(DLNotCancelable.INSTANCE).executeInKernel(b.toString(),
                    DLNotCancelable.INSTANCE);
            } catch (final DLCanceledExecutionException e) {
                // Won't happen
            }
        }
        return baseNetworkSpecs;
    }

    private List<String> expandNetworkInputs(final List<Object> networkInputs) {
        final List<String> expandedNetworkInputs = new ArrayList<>();
        for (final Object networkInput : networkInputs) {
            if (networkInput instanceof String) {
                expandedNetworkInputs.add((String)networkInput);
            } else {
                final DLKerasBaseNetworkHelperStruct baseNetworkHelper = (DLKerasBaseNetworkHelperStruct)networkInput;
                for (int i = 0; i < baseNetworkHelper.m_networkSpec.getInputSpecs().length; i++) {
                    expandedNetworkInputs.add(baseNetworkHelper.m_variable + ".inputs[" + i + "]");
                }
            }
        }
        return expandedNetworkInputs;
    }

    private List<String> expandNetworkOutputs(final List<Object> networkOutputs) {
        final List<String> expandedNetworkOutputs = new ArrayList<>();
        for (final Object networkOutput : networkOutputs) {
            if (networkOutput instanceof String) {
                expandedNetworkOutputs.add((String)networkOutput);
            } else {
                final DLKerasBaseNetworkHelperStruct baseNetworkHelper = (DLKerasBaseNetworkHelperStruct)networkOutput;
                for (int i = 0; i < baseNetworkHelper.m_networkSpec.getOutputSpecs().length; i++) {
                    if (!baseNetworkHelper.m_connectedOutputs.contains(i)) {
                        expandedNetworkOutputs.add(baseNetworkHelper.m_variable + ".outputs[" + i + "]");
                    }
                }
            }
        }
        return expandedNetworkOutputs;
    }

    private static class DLKerasNetworkMaterializerParser implements DLKerasLayerVisitor {
        
        private final Map<DLKerasTensorSpecsOutput, List<DLTensorSpec>> m_layerToTensorSpecMap;

        private final List<Object> m_inputVariables = new ArrayList<>();

        private final List<Object> m_outputVariables = new ArrayList<>();

        private final Map<DLTensorId, String> m_tensorVariables = new HashMap<>();

        private int m_layerVariableSuffix = 0;

        private final LinkedHashMap<DLKerasNetworkSpec, DLKerasBaseNetworkHelperStruct> m_baseNetworks =
            new LinkedHashMap<>();

        private int m_baseNetworkVariableSuffix = 0;

        private final Map<DLKerasTensorSpecsOutput, Function<DLKerasNetworkLayerNameGenerator, String>> m_codeLinesToGenerate =
            new HashMap<>();

        private Map<DLKerasTensorSpecsOutput, Integer> m_maxDepthsFromOutputs;
        
        /**
         * @param layerToTensorSpecMap 
         * 
         */
        public DLKerasNetworkMaterializerParser(
            Map<DLKerasTensorSpecsOutput,List<DLTensorSpec>> layerToTensorSpecMap) {
            m_layerToTensorSpecMap = layerToTensorSpecMap;
        }

        @Override
        public void visitOutput(final DLKerasInnerLayer outputLayer) throws Exception {
            final List<String> tensorVariables = addTensorVariables(outputLayer);
            m_outputVariables.addAll(tensorVariables);

            visitHidden(outputLayer);
        }

        @Override
        public void visitHidden(final DLKerasInnerLayer innerLayer) throws Exception {
            final List<String> parentVariables = extractParentVariables(innerLayer);
            final List<String> tensorVariables = getTensorVariables(innerLayer);
            m_codeLinesToGenerate.put(innerLayer, gen -> String.join(", ", tensorVariables) + " = "
                + innerLayer.getBackendRepresentation(gen.getNextLayerName(innerLayer)) //
                + '(' + innerLayer.populateCall(parentVariables)
                + ')');
        }
        
        private List<String> getTensorVariables(DLKerasTensorSpecsOutput tensorSpecsOutput) {
            return getTensorSpecs(tensorSpecsOutput).stream()
                    .map(s -> m_tensorVariables.get(s.getIdentifier())).collect(Collectors.toList());
        }

        private List<String> extractParentVariables(final DLKerasInnerLayer innerLayer) {
            List<String> parentVariables = new ArrayList<>();
            for (int i = 0; i < innerLayer.getNumParents(); i++) {
                DLKerasTensorSpecsOutput parent = innerLayer.getParent(i);
                DLTensorSpec inputSpec = innerLayer.getInputTensorSpec(i);
                String variable = m_tensorVariables.get(inputSpec.getIdentifier());
                if (variable == null) {
                    addTensorVariables(parent);
                    variable = m_tensorVariables.get(inputSpec.getIdentifier());
                }
                if (variable  == null) {
                    throw new IllegalStateException("Can't find variable for tensor " 
                            + inputSpec + " after adding all tensors of respective parent.");
                }
                parentVariables.add(variable);
            }
            return parentVariables;
        }
        
        private List<DLTensorSpec> getTensorSpecs(DLKerasTensorSpecsOutput tensorSpecOutput) {
            return m_layerToTensorSpecMap.get(tensorSpecOutput);
        }
        
        private List<String> addTensorVariables(DLKerasTensorSpecsOutput tensorOutput) {
            List<DLTensorSpec> tensorSpecs = getTensorSpecs(tensorOutput);
            List<String> addedVariables = new ArrayList<>(tensorSpecs.size());
            for (DLTensorSpec tensorSpec : tensorSpecs) {
                String variable = getNextLayerVariable();
                addedVariables.add(variable);
                m_tensorVariables.put(tensorSpec.getIdentifier(), variable);
            }
            return addedVariables;
        }

        @Override
        public void visitInput(final DLKerasInputLayer inputLayer) throws Exception {
            final List<String> tensorVariable = getTensorVariables(inputLayer);
            m_inputVariables.addAll(tensorVariable);
            m_codeLinesToGenerate.put(inputLayer,
                gen -> String.join(", ",tensorVariable) + " = " 
                        + inputLayer.getBackendRepresentation(gen.getNextLayerName(inputLayer)));
        }

        @Override
        public void visitInputOutput(final DLKerasInputLayer inputOutputLayer) throws Exception {
            List<String> variables = addTensorVariables(inputOutputLayer);
            m_inputVariables.addAll(variables);
            m_outputVariables.add(variables);
            m_codeLinesToGenerate.put(inputOutputLayer, gen -> String.join(", ", variables) + " = "
                + inputOutputLayer.getBackendRepresentation(gen.getNextLayerName(inputOutputLayer)));
        }

        @Override
        public void visitBaseNetworkOutput(final DLKerasBaseNetworkTensorSpecOutput baseNetworkOutput) {
            final DLKerasNetworkSpec baseNetworkSpec = baseNetworkOutput.getBaseNetworkSpec();
            // Re-use base network if it's already connected to another layer.
            // TODO: This behavior may not be intended.
            DLKerasBaseNetworkHelperStruct baseNetworkHelper = m_baseNetworks.get(baseNetworkSpec);
            if (baseNetworkHelper == null) {
                baseNetworkHelper = new DLKerasBaseNetworkHelperStruct(getNextBaseNetworkVariable(), baseNetworkSpec,
                    baseNetworkOutput.getBaseNetworkSource());
                m_baseNetworks.put(baseNetworkSpec, baseNetworkHelper);
                m_inputVariables.add(baseNetworkHelper);
                m_outputVariables.add(baseNetworkHelper);
            }
            final int baseNetworkOutputIndex = baseNetworkOutput.getBaseNetworkOutputIndex();
            baseNetworkHelper.m_connectedOutputs.add(baseNetworkOutputIndex);
            final List<String> tensorVariable = getTensorVariables(baseNetworkOutput);
            final String baseNetworkVariable = baseNetworkHelper.m_variable;
            m_codeLinesToGenerate.put(baseNetworkOutput,
                gen -> tensorVariable + " = " + baseNetworkVariable + ".outputs[" + baseNetworkOutputIndex + "]");
        }

        @Override
        public void noteLayerDepths(final Map<DLKerasTensorSpecsOutput, Integer> maxDepthsFromOutputs) {
            m_maxDepthsFromOutputs = maxDepthsFromOutputs;
        }

        private String getNextBaseNetworkVariable() {
            return "base_network_" + m_baseNetworkVariableSuffix++;
        }

        private String getNextLayerVariable() {
            return "generated_layer_" + m_layerVariableSuffix++;
        }
    }

    private static class DLKerasBaseNetworkHelperStruct {

        private final String m_variable;

        private final DLKerasNetworkSpec m_networkSpec;

        private final DLNetworkLocation m_networkSource;

        private final TIntHashSet m_connectedOutputs = new TIntHashSet(5);

        private DLKerasBaseNetworkHelperStruct(final String variable, final DLKerasNetworkSpec networkSpec,
            final DLNetworkLocation networkSource) {
            m_variable = variable;
            m_networkSpec = networkSpec;
            m_networkSource = networkSource;
        }
    }
}
