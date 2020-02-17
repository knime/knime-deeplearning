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
 * History
 *   Feb 17, 2020 (benjamin): created
 */
package org.knime.dl.keras.base.nodes.io.filehandling.network.reader;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.knime.core.data.filestore.FileStore;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.node.port.PortObject;
import org.knime.dl.base.portobjects.DLNetworkPortObject;
import org.knime.dl.core.DLExecutionMonitorCancelable;
import org.knime.dl.core.DLNetworkReferenceLocation;
import org.knime.dl.keras.base.portobjects.DLKerasNetworkPortObject;
import org.knime.dl.keras.core.DLKerasNetwork;
import org.knime.dl.keras.core.DLKerasNetworkLoader;
import org.knime.dl.python.core.DLPythonDefaultNetworkReader;
import org.knime.dl.python.core.DLPythonNetworkLoader;
import org.knime.dl.python.core.DLPythonNetworkLoaderRegistry;
import org.knime.filehandling.core.node.portobject.reader.PortObjectFromFileReaderNodeModel;

/**
 * Model of the Keras network reader.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 */
final class DLKerasNetworkReaderNodeModel extends PortObjectFromFileReaderNodeModel<DLKerasNetworkReaderNodeConfig> {

    DLKerasNetworkReaderNodeModel(final NodeCreationConfiguration creationConfig) {
        super(creationConfig, new DLKerasNetworkReaderNodeConfig());
    }

    @Override
    protected PortObject[] read(final InputStream inputStream, final ExecutionContext exec) throws Exception {
        final DLExecutionMonitorCancelable cancelable = new DLExecutionMonitorCancelable(exec);

        // Copy the file to a file store
        final String ext = getConfiguredFileExtension();
        final FileStore fileStore = DLNetworkPortObject.createFileStoreForSaving(ext, exec);
        final Path fileStorePath = fileStore.getFile().toPath();
        final URI fileStoreUri = fileStorePath.toUri();
        Files.copy(inputStream, fileStorePath);

        // Get the network loader
        final DLKerasNetworkLoader<?> loader = getNetworkLoader();
        // Check that the loader is available
        loader.checkAvailability(false, DLPythonNetworkLoaderRegistry.getInstallationTestTimeout(), cancelable);
        // Validate the source
        loader.validateSource(fileStoreUri);

        // Read the network
        final DLKerasNetwork network = new DLPythonDefaultNetworkReader<>(loader)
            .read(new DLNetworkReferenceLocation(fileStoreUri), true, cancelable);

        // Create the port object
        final DLKerasNetworkPortObject portObject = new DLKerasNetworkPortObject(network, fileStore);
        return new PortObject[]{portObject};
    }

    /** Get the file extension of the configured file */
    private String getConfiguredFileExtension() {
        final String path = getConfig().getFileChooserModel().getPathOrURL();
        return path.substring(path.lastIndexOf('.') + 1);
    }

    /** Get the configure network loader */
    private DLKerasNetworkLoader<?> getNetworkLoader() throws InvalidSettingsException {
        final String networkType = getConfig().getKerasBackend().getStringValue();
        final Optional<DLPythonNetworkLoader<?>> networkLoader =
            DLPythonNetworkLoaderRegistry.getInstance().getNetworkLoader(networkType);
        if (networkLoader.isPresent()) {
            return (DLKerasNetworkLoader<?>)networkLoader.get();
        }
        throw new InvalidSettingsException("The selected Keras back end for the network type \"" + networkType
            + "\" is not available. Are you missing a KNIME Deep Learning extension?");
    }
}
