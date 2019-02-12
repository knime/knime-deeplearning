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
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.knime.core.data.StringValue;
import org.knime.core.data.filestore.FileStore;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.dl.base.portobjects.DLNetworkPortObject;
import org.knime.dl.core.DLCancelable;
import org.knime.dl.core.DLCanceledExecutionException;
import org.knime.dl.core.DLExecutionMonitorCancelable;
import org.knime.dl.core.DLInvalidDestinationException;
import org.knime.dl.core.DLInvalidEnvironmentException;
import org.knime.dl.core.DLInvalidSourceException;
import org.knime.dl.core.DLMissingExtensionException;
import org.knime.dl.core.DLNetworkFileStoreLocation;
import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.keras.base.portobjects.DLKerasNetworkPortObjectBase;
import org.knime.dl.keras.core.DLKerasNetwork;
import org.knime.dl.keras.core.DLKerasNetworkSpec;
import org.knime.dl.keras.util.DLKerasUtils;
import org.knime.dl.python.core.DLPythonContext;
import org.knime.dl.python.core.DLPythonDefaultContext;
import org.knime.dl.python.core.DLPythonNetwork;
import org.knime.dl.python.core.DLPythonNetworkHandle;
import org.knime.dl.python.core.DLPythonNetworkLoader;
import org.knime.dl.python.core.DLPythonNetworkLoaderRegistry;
import org.knime.dl.python.core.DLPythonNetworkPortObject;
import org.knime.dl.python.util.DLPythonSourceCodeBuilder;
import org.knime.dl.python.util.DLPythonUtils;

/**
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 */
public class DLKerasFreezeLayersNodeModel extends NodeModel {

    private static final String OUTPUT_NETWORK_VAR = "output_network";

    private static final String NETWORK_TYPE_IDENTIFIER = "network_type_identifier";

    static final int IN_NETWORK_PORT_IDX = 0;

    static StringFilterConfiguration createLayerFilterConfig() {
        return new StringFilterConfiguration("frozen_layers");
    }

    static String[] getLayerNames(final DLKerasNetworkSpec spec) {
        final DLTensorSpec[] hiddenSpecs = spec.getHiddenOutputSpecs();
        final DLTensorSpec[] outputSpecs = spec.getOutputSpecs();
        final List<String> layers = new ArrayList<>(hiddenSpecs.length + outputSpecs.length);
        final Consumer<DLTensorSpec[]> addToList =
            l -> Arrays.stream(l).map(t -> DLKerasUtils.Layers.getLayerName(t.getIdentifier())).forEach(layers::add);
        addToList.accept(hiddenSpecs);
        addToList.accept(outputSpecs);
        return layers.toArray(new String[layers.size()]);
    }

    private final StringFilterConfiguration m_frozenLayers = createLayerFilterConfig();

    DLKerasFreezeLayersNodeModel() {
        super(new PortType[]{DLKerasNetworkPortObjectBase.TYPE}, new PortType[]{DLKerasNetworkPortObjectBase.TYPE});
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        // TODO Should we save the frozen layers in the specs?
        // TODO check that configured layers are available
        // This node doesn't change the specs
        return inSpecs;
    }

    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        final DLKerasNetworkPortObjectBase portObject = (DLKerasNetworkPortObjectBase)inObjects[IN_NETWORK_PORT_IDX];
        final DLKerasNetwork inputNetwork = portObject.getNetwork();
        final DLCancelable cancelable = new DLExecutionMonitorCancelable(exec);

        // TODO handle stdout and stderr

        try (final DLPythonContext pythonContext = new DLPythonDefaultContext()) {
            // Load the input network
            final DLPythonNetworkHandle inputNetworkHandle =
                DLPythonNetworkLoaderRegistry.getInstance().getNetworkLoader(inputNetwork.getClass())
                    .orElseThrow(() -> new DLMissingExtensionException(
                        "Python back end '" + inputNetwork.getClass().getCanonicalName()
                            + "' could not be found. Are you missing a KNIME Deep Learning extension?"))
                    .load(inputNetwork.getSource().getURI(), pythonContext, false, cancelable);

            // Freeze the layers
            final String freezeSourceCode = createFreezeSourceCode(inputNetworkHandle, inputNetwork.getSpec());
            pythonContext.executeInKernel(freezeSourceCode, cancelable);

            // Save the output network
            final DLPythonNetworkPortObject<?> outputPortObject = saveOutputNetwork(exec, cancelable, pythonContext);
            return new PortObject[]{outputPortObject};
        }
    }

    private String createFreezeSourceCode(final DLPythonNetworkHandle inputNetworkHandle,
        final DLKerasNetworkSpec networkSpec) {
        // Get the configured layers
        final String[] layers = getLayerNames(networkSpec);
        final String[] frozen = m_frozenLayers.applyTo(layers).getIncludes();

        // Get the model
        final DLPythonSourceCodeBuilder b = DLPythonUtils.createSourceCodeBuilder() //
            .a("import DLPythonNetwork") //
            .n("import keras.backend as K") //
            .n("from tensorflow import saved_model") //
            .n(OUTPUT_NETWORK_VAR).a(" = DLPythonNetwork.get_network(").as(inputNetworkHandle.getIdentifier())
            /**/ .a(").model");

        // Freeze the configured layers
        b //
            .n("for l in ").a(OUTPUT_NETWORK_VAR).a(".layers:") //
            .n().t().a("l.trainable = not l.name in ").as(frozen);

        return b.toString();
    }

    private static <N extends DLPythonNetwork> DLPythonNetworkPortObject<?> saveOutputNetwork(
        final ExecutionContext exec, final DLCancelable cancelable, final DLPythonContext pythonContext)
        throws DLCanceledExecutionException, DLInvalidEnvironmentException, IOException, CanceledExecutionException,
        DLMissingExtensionException, DLInvalidDestinationException, DLInvalidSourceException {
        final String loaderIdentifierSourceCode = DLPythonUtils.createSourceCodeBuilder() //
            .a("import DLPythonNetwork") //
            .n("import DLPythonNetworkType") //
            .n("import pandas as pd") //
            .n("network_type = DLPythonNetworkType.get_model_network_type(").a(OUTPUT_NETWORK_VAR).a(")") //
            .n("DLPythonNetwork.add_network(network_type.wrap_model(") //
            /**/ .a(OUTPUT_NETWORK_VAR).a("), ").as(OUTPUT_NETWORK_VAR).a(")") //
            .n("global network_type_identifier") //
            .n(NETWORK_TYPE_IDENTIFIER).a(" = pd.DataFrame(data=[network_type.identifier])") //
            .toString();
        pythonContext.executeInKernel(loaderIdentifierSourceCode, cancelable);
        final String networkLoaderIdentifier = ((StringValue)pythonContext.getKernel()
            .getDataTable(NETWORK_TYPE_IDENTIFIER, exec, exec).iterator().next().getCell(0)).getStringValue();
        @SuppressWarnings("unchecked")
        final DLPythonNetworkLoader<N> loader = (DLPythonNetworkLoader<N>)DLPythonNetworkLoaderRegistry.getInstance()
            .getNetworkLoader(networkLoaderIdentifier)
            .orElseThrow(() -> new DLMissingExtensionException("Python back end '" + networkLoaderIdentifier
                + "' could not be found. Are you missing a KNIME Deep Learning extension?"));
        final FileStore fileStore =
            DLNetworkPortObject.createFileStoreForSaving(loader.getSaveModelURLExtension(), exec);
        final URI fileStoreURI = fileStore.getFile().toURI();
        final DLPythonNetworkHandle handle = new DLPythonNetworkHandle(OUTPUT_NETWORK_VAR);
        loader.save(handle, fileStoreURI, pythonContext, cancelable);
        if (!fileStore.getFile().exists()) {
            throw new IllegalStateException(
                "Failed to save output deep learning network '" + OUTPUT_NETWORK_VAR + "'.");
        }
        return loader.createPortObject(
            loader.fetch(handle, new DLNetworkFileStoreLocation(fileStore), pythonContext, cancelable), fileStore);
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_frozenLayers.saveConfiguration(settings);
    }

    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        // TODO nothing to do?
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
