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
package org.knime.dl.keras.base.nodes.layers.manipulation.outputs;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.function.Consumer;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.keras.base.nodes.layers.manipulation.DLKerasAbstractManipulationNodeModel;
import org.knime.dl.keras.base.portobjects.DLKerasNetworkPortObjectSpecBase;
import org.knime.dl.keras.core.DLKerasNetworkSpec;
import org.knime.dl.keras.util.DLKerasUtils;
import org.knime.dl.python.util.DLPythonSourceCodeBuilder;
import org.knime.dl.python.util.DLPythonUtils;

/**
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 */
public class DLKerasSelectOutputLayersNodeModel extends DLKerasAbstractManipulationNodeModel {

    static SettingsModelStringArray createOutputTensorsSM() {
        return new SettingsModelStringArray("output_tensors", new String[0]);
    }

    private final SettingsModelStringArray m_outputTensors = createOutputTensorsSM();

    DLKerasSelectOutputLayersNodeModel() {
        super();
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        final DLKerasNetworkPortObjectSpecBase spec = (DLKerasNetworkPortObjectSpecBase)inSpecs[0];
        final DLKerasNetworkSpec networkSpec = spec.getNetworkSpec();

        final String[] selectedOutputs = m_outputTensors.getStringArrayValue();
        if (selectedOutputs.length == 0) {
            throw new InvalidSettingsException("No output selected. Select at least one output tensor.");
        }

        final HashSet<String> tensors = new HashSet<>();
        final Consumer<DLTensorSpec[]> addToSet = tensorSpecs -> Arrays.stream(tensorSpecs)
            .map(s -> s.getIdentifier().getIdentifierString()).forEach(tensors::add);
        addToSet.accept(networkSpec.getHiddenOutputSpecs());
        addToSet.accept(networkSpec.getOutputSpecs());

        // Check that all configured outputs are available
        for (final String id : selectedOutputs) {
            if (!tensors.contains(id)) {
                throw new InvalidSettingsException("The tensor '" + id
                    + "' is configured as an output but not available anymore. Please reconfigure the node.");
            }
        }

        // We don't know the output spec yet because we don't know which hidden tensors will
        // be available in the resulting model. The network spec doesn't hold structure information.
        return new PortObjectSpec[]{null};
    }

    @Override
    protected String createManipulationSourceCode(final DLKerasNetworkSpec networkSpec) {

        // Create a array for the new outputs
        final DLPythonSourceCodeBuilder b = DLPythonUtils.createSourceCodeBuilder() //
            .n("inputs = ").a(OUTPUT_NETWORK_VAR).a(".inputs") //
            .n("outputs = []"); //

        for (final String tensor : m_outputTensors.getStringArrayValue()) {
            final String layerName = DLKerasUtils.Layers.getLayerName(tensor);
            final int nodeIndex = DLKerasUtils.Tensors.getNodeIndex(tensor);
            final int tensorIndex = DLKerasUtils.Tensors.getTensorIndex(tensor);

            b // Append the output tensor
                .n("layer = ").a(OUTPUT_NETWORK_VAR).a(".get_layer(").as(layerName).a(")") //
                .n("tensors = layer.get_output_at(").a(nodeIndex).a(")") //
                .n("if isinstance(tensors, list):") //
                /**/ .n().t().a("outputs.append(tensors[").a(tensorIndex).a("])") //
                .n("else:") //
                /**/ .n().t().a("outputs.append(tensors)");
        }

        b // Create the new model
            .n("from keras.models import Model")//
            .n(OUTPUT_NETWORK_VAR).a(" = Model(inputs, outputs)");

        b // Remove irrelevant inputs
            .n("orig_inputs = inputs") //
            .n("relevant_inputs = []") //
            .n("for v in ").a(OUTPUT_NETWORK_VAR).a("._nodes_by_depth.values():") //
            .n().t().a("if orig_inputs == []:") //
            .n().t().t().a("break") //
            .n().t().a("for n in v:") //
            .n().t().t().a("for in_tensor in n.input_tensors:") //
            .n().t().t().t().a("if in_tensor in orig_inputs:") //
            .n().t().t().t().t().a("relevant_inputs.append(in_tensor)") //
            .n().t().t().t().t().a("orig_inputs.remove(in_tensor)"); //

        b // Create new model with only relevant inputs
            .n(OUTPUT_NETWORK_VAR).a(" = Model(relevant_inputs, outputs)");

        return b.toString();
    }

    @Override
    protected void saveSettingsToDerived(final NodeSettingsWO settings) {
        m_outputTensors.saveSettingsTo(settings);
    }

    @Override
    protected void validateSettingsDerived(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_outputTensors.validateSettings(settings);
    }

    @Override
    protected void loadValidatedSettingsFromDerived(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_outputTensors.loadSettingsFrom(settings);
    }

    @Override
    protected void reset() {
        // nothing to do
    }

    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // nothing to do
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // nothing to do
    }
}
