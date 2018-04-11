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
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.knime.dl.core.DLInvalidDestinationException;
import org.knime.dl.core.DLInvalidEnvironmentException;
import org.knime.dl.core.DLInvalidSourceException;
import org.knime.dl.keras.core.DLKerasAbstractCommands;
import org.knime.dl.keras.core.DLKerasNetwork;
import org.knime.dl.keras.core.DLKerasNetworkLoader;
import org.knime.dl.keras.core.layers.DLKerasNetworkLayerGraphIterator.DLKerasLayerVisitor;
import org.knime.dl.keras.core.layers.DLKerasNetworkLayerGraphIterator.DLNetworkLayerGraphTraversalException;
import org.knime.dl.keras.tensorflow.core.DLKerasTensorFlowNetwork;
import org.knime.dl.python.core.DLPythonDefaultContext;
import org.knime.dl.python.core.DLPythonNetworkHandle;
import org.knime.dl.python.core.DLPythonNetworkLoader;
import org.knime.dl.python.core.DLPythonNetworkLoaderRegistry;
import org.knime.dl.python.util.DLPythonSourceCodeBuilder;
import org.knime.dl.python.util.DLPythonUtils;

import com.google.common.collect.Lists;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public final class DLKerasNetworkMaterializer {

    private int m_layerVariableSuffix;

    /**
     * Materializes the Keras network graph specified by the given output layers and their parents (i.e. predecessor
     * nodes).
     *
     * @param outputLayers the output layers of the network to materialize
     * @param saveUrl the location where the materialized network is saved
     * @return the materialized network
     * @throws DLNetworkLayerGraphTraversalException if traversing the network graph failed
     * @throws DLInvalidEnvironmentException if materialization in the back end failed
     * @throws IOException if failed to materialize and save the network due to I/O related errors
     */
    public DLKerasNetwork materialize(final List<DLKerasLayer> outputLayers, final URL saveUrl)
        throws DLInvalidEnvironmentException, IOException {
        m_layerVariableSuffix = 0;

        // Parse layer graph and generate code:

        final DLKerasNetworkLayerGraphIterator iterator = new DLKerasNetworkLayerGraphIterator(outputLayers);

        final List<String> inputVariables = new ArrayList<>();
        final String[] outputVariables = new String[outputLayers.size()];
        final Map<DLKerasLayer, String> layerVariables = new HashMap<>();

        for (int i = 0; i < outputLayers.size(); i++) {
            final DLKerasLayer output = outputLayers.get(i);
            final String outputVariable = getNextLayerVariable();
            outputVariables[i] = outputVariable;
            layerVariables.put(output, outputVariable);
        }

        final DLKerasNetworkLayerNameGenerator layerNameGen = new DLKerasNetworkLayerNameGenerator();
        final ArrayList<String> generatedCodeLines = new ArrayList<>();

        iterator.visitAll(new DLKerasLayerVisitor() {

            @Override
            public void visitOutput(final DLKerasInnerLayer outputLayer) throws Exception {
                visitHidden(outputLayer);
            }

            @Override
            public void visitHidden(final DLKerasInnerLayer innerLayer) throws Exception {
                final DLKerasLayer[] parents = innerLayer.getParents();
                final String[] parentVariables = new String[parents.length];
                for (int i = 0; i < parents.length; i++) {
                    final DLKerasLayer parent = parents[i];
                    String parentVariable = layerVariables.get(parent);
                    if (parentVariable == null) {
                        parentVariable = getNextLayerVariable();
                        layerVariables.put(parent, parentVariable);
                    } // else it's a fork in the graph
                    parentVariables[i] = parentVariable;
                }
                final String layerVariable = layerVariables.get(innerLayer);

                generatedCodeLines.add(layerVariable + " = " //
                    + innerLayer.getBackendRepresentation(layerNameGen.getNextLayerName(innerLayer)) //
                    + '(' + (parentVariables.length == 1 //
                        ? parentVariables[0] //
                        : "[" + String.join(",", parentVariables) + "]") //
                    + ')');
            }

            @Override
            public void visitInput(final DLKerasInputLayer inputLayer) throws Exception {
                final String layerVariable = layerVariables.get(inputLayer);
                inputVariables.add(layerVariable);
                generatedCodeLines.add(layerVariable + " = "
                    + inputLayer.getBackendRepresentation(layerNameGen.getNextLayerName(inputLayer)));
            }
        });

        // We traversed the graph along the layers' dependencies (i.e. from child to parent) from outputs to inputs.
        // Thus, reversing the order of the lines of code yields a legal expression.
        final String generatedCode = Lists.reverse(generatedCodeLines).stream().collect(Collectors.joining("\n"));

        // Execute generated code and save network:

        // TODO: Hard-coded for the moment.
        final Class<DLKerasTensorFlowNetwork> backend = DLKerasTensorFlowNetwork.class;

        final DLPythonNetworkLoader<? extends DLKerasNetwork> loader = DLPythonNetworkLoaderRegistry.getInstance()
            .getNetworkLoader(backend).orElseThrow(() -> new IllegalStateException("Back end for Keras network type '"
                + backend.getName() + "' is missing. " + "Are you missing a KNIME Deep Learning extension?"));
        try (final DLKerasAbstractCommands commands =
            ((DLKerasNetworkLoader<?>)loader).createCommands(new DLPythonDefaultContext())) {
            final DLPythonSourceCodeBuilder b = DLPythonUtils.createSourceCodeBuilder() //
                .a("import keras").n(generatedCode) //
                .n("generated_network = keras.models.Model(") //
                .a("inputs=[" + String.join(",", inputVariables) + "]").a(", ") //
                .a("outputs=[" + String.join(",", outputVariables) + "]") //
                .a(")") //
                .n("import DLPythonNetworkType") //
                .n("network_type = DLPythonNetworkType.get_model_network_type(generated_network)") //
                .n("import DLPythonNetwork") //
                .n("DLPythonNetwork.add_network('generated_network', network_type.wrap_model(generated_network))");

            commands.getContext().executeInKernel(b.toString());

            final DLPythonNetworkHandle handle = new DLPythonNetworkHandle("generated_network");
            try {
                loader.save(handle, saveUrl, commands.getContext());
            } catch (final DLInvalidDestinationException e) {
                throw new IOException(e);
            }
            DLKerasNetwork network;
            try {
                network = loader.fetch(handle, saveUrl, commands.getContext());
            } catch (final DLInvalidSourceException e) {
                throw new IOException(e);
            }
            return network;
        }
    }

    private String getNextLayerVariable() {
        return "generated_layer_" + m_layerVariableSuffix++;
    }
}
