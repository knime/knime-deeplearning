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
package org.knime.dl.keras.base.nodes.layers.manipulation.freeze;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.keras.base.nodes.layers.manipulation.DLKerasAbstractManipulationNodeModel;
import org.knime.dl.keras.core.DLKerasNetworkSpec;
import org.knime.dl.keras.util.DLKerasUtils;
import org.knime.dl.python.core.DLPythonNetworkHandle;
import org.knime.dl.python.util.DLPythonUtils;

/**
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 */
public class DLKerasFreezeLayersNodeModel extends DLKerasAbstractManipulationNodeModel {

    static StringFilterConfiguration createLayerFilterConfig() {
        return new StringFilterConfiguration("frozen_layers");
    }

    static String[] getLayerNames(final DLKerasNetworkSpec spec) {
        final Set<String> layers = new HashSet<>();
        final Consumer<DLTensorSpec[]> addToList =
            l -> Arrays.stream(l).map(t -> DLKerasUtils.Layers.getLayerName(t.getIdentifier())).forEach(layers::add);
        addToList.accept(spec.getHiddenOutputSpecs());
        addToList.accept(spec.getOutputSpecs());
        return layers.toArray(new String[layers.size()]);
    }

    private final StringFilterConfiguration m_frozenLayers = createLayerFilterConfig();

    DLKerasFreezeLayersNodeModel() {
        super();
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        // TODO Check the configuration:
        // - If enforce inclusion is activated: All configured included layers must be available
        // - If enforce exclusion is activated: All configured excluded layers must be available
        // This is currently not implemented in the NameFilterConfiguraion used. (The column filter doesn't do the check either)

        // TODO Save the frozen layers in the network spec
        // This node doesn't change the specs
        return inSpecs;
    }

    @Override
    protected String createManipulationSourceCode(final DLPythonNetworkHandle inputNetworkHandle,
        final DLKerasNetworkSpec networkSpec) {
        // Get the configured layers
        final String[] layers = getLayerNames(networkSpec);
        final String[] frozen = m_frozenLayers.applyTo(layers).getIncludes();

        // Freeze the configured layers
        return DLPythonUtils.createSourceCodeBuilder() //
            .n("for l in ").a(OUTPUT_NETWORK_VAR).a(".layers:") //
            .n().t().a("l.trainable = not l.name in ").as(frozen) //
            .toString();
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_frozenLayers.saveConfiguration(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        // nothing to validate
    }

    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_frozenLayers.loadConfigurationInModel(settings);
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
