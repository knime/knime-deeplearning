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
package org.knime.dl.python.core;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.List;

import org.knime.core.data.filestore.FileStore;
import org.knime.dl.core.DLCancelable;
import org.knime.dl.core.DLCanceledExecutionException;
import org.knime.dl.core.DLInstallationTestable;
import org.knime.dl.core.DLInvalidDestinationException;
import org.knime.dl.core.DLInvalidEnvironmentException;
import org.knime.dl.core.DLInvalidSourceException;
import org.knime.dl.core.DLNetworkLocation;
import org.knime.python.typeextension.PythonModuleExtensions;

/**
 * @param <N> the Python network type
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public interface DLPythonNetworkLoader<N extends DLPythonNetwork> extends DLInstallationTestable<DLPythonContext> {

	/**
	 * Returns the network type that's associated with this Python loader.
	 *
	 * @return the network type that's associated with this Python loader
	 */
	Class<N> getNetworkType();

	/**
	 * Returns the name of the module that contains this network loader's target network type on Python side. The module
	 * must be discoverable via the PYTHONPATH (this can be ensured by registration at extension point
	 * {@link PythonModuleExtensions}).
	 * <P>
	 * The network type class on Python side has to extend the abstract base class <code>DLPythonNetworkType</code> from
	 * module <code>DLPythonNetworkType</code> and implement its abstract properties and methods. A singleton instance
	 * of the class has to be created and registered via <code>DLPythonNetworkType.add_network_type(instance)</code>.
	 * <P>
	 * The network type's module must not import any third party modules (i.e. no modules that are not part of the
	 * Python standard library or not provided by KNIME - especially no modules that belong to the network type's back
	 * end).<br>
	 * Thus, the actual implementation of the network type's functionality must be kept within a separate module
	 * (recommended naming scheme is <code>DL*NetworkType</code> for the network type's module and
	 * <code>DL*Network</code> for the module that contains the actual implementation). The network type class may only
	 * import the implementation module lazily (i.e. in method scope).
	 *
	 * @return the module name
	 */
	String getPythonModuleName();

	/**
	 * Returns all model file extensions that this loader is able to load from.
	 *
	 * @return all supported model file extensions without a leading dot, not null, may be empty
	 */
	List<String> getLoadModelURLExtensions();

	/**
	 * Returns the file extension that this loader uses for saving models.
	 *
	 * @return the file extension without a leading dot, not null, may be empty
	 */
	String getSaveModelURLExtension();

    /**
     * Checks if the given source is valid and resolves it to URL.
     *
     * @param source the source
     * @throws DLInvalidSourceException if the source is unavailable or invalid
     * @return the valid and resolved URL
     */
    URL validateSource(URI source) throws DLInvalidSourceException;

    /**
     * Checks if the given destination is valid and resolves it to URL.
     *
     * @param destination the destination
     * @throws DLInvalidDestinationException if the destination is invalid
     * @return the valid and resolved URL
     */
    URL validateDestination(URI destination) throws DLInvalidDestinationException;

    /**
     * Loads a network from a source into a context. This method should be preferred over
     * {@link #load(URI, DLPythonContext, boolean, DLCancelable)} because the implementation can use information of the
     * network object.
     *
     * @param network the network
     * @param context the context
     * @param loadTrainingConfig true if the training configuration enclosed in <code>network</code> - if any - shall be
     *            loaded. This is an optional feature for supporting back ends. For non-supporting back ends, calling
     *            this method with different values for <code>loadTrainingConfig</code> should result in same return
     *            values and side effects.
     * @param cancelable to check if the operation has been canceled
     * @return the network handle
     * @throws DLInvalidSourceException if the source is unavailable or invalid
     * @throws DLInvalidEnvironmentException if the context is invalid
     * @throws IOException if failed to load the network
     * @throws DLCanceledExecutionException if the operation has been canceled
     */
    default DLPythonNetworkHandle load(final N network, final DLPythonContext context, final boolean loadTrainingConfig, final DLCancelable cancelable)
        throws DLInvalidSourceException, DLInvalidEnvironmentException, IOException, DLCanceledExecutionException {
        return load(network.getSource().getURI(), context, loadTrainingConfig, cancelable);
    }

	/**
	 * Loads a network from a source into a context.
	 *
	 * @param source the source
	 * @param context the context
	 * @param loadTrainingConfig true if the training configuration enclosed in <code>source</code> - if any - shall be
	 *            loaded. This is an optional feature for supporting back ends. For non-supporting back ends, calling
	 *            this method with different values for <code>loadTrainingConfig</code> should result in same return
	 *            values and side effects.
	 * @param cancelable to check if the operation has been canceled
	 * @return the network handle
	 * @throws DLInvalidSourceException if the source is unavailable or invalid
	 * @throws DLInvalidEnvironmentException if the context is invalid
	 * @throws IOException if failed to load the network
	 * @throws DLCanceledExecutionException if the operation has been canceled
	 */
	DLPythonNetworkHandle load(URI source, DLPythonContext context, boolean loadTrainingConfig, DLCancelable cancelable)
			throws DLInvalidSourceException, DLInvalidEnvironmentException, IOException, DLCanceledExecutionException;

	/**
	 * Fetches the network representation of a handle from a context.
	 *
	 * @param handle the handle
	 * @param source the source
	 * @param context the context
	 * @param cancelable to check if the operation has been canceled
	 * @return the network
	 * @throws IllegalArgumentException if the handle is invalid
	 * @throws DLInvalidSourceException if the source is unavailable or invalid
	 * @throws DLInvalidEnvironmentException if the context is invalid
	 * @throws IOException if failed to fetch the network
	 * @throws DLCanceledExecutionException if the operation has been canceled
	 */
    N fetch(DLPythonNetworkHandle handle, DLNetworkLocation source, DLPythonContext context, DLCancelable cancelable)
			throws IllegalArgumentException, DLInvalidSourceException, DLInvalidEnvironmentException, IOException, DLCanceledExecutionException;

	/**
	 * Saves a network from a context to a destination.
	 *
	 * @param handle the handle
	 * @param destination the destination
	 * @param context the context
	 * @param cancelable to check if the operation has been canceled
	 * @throws IllegalArgumentException if the handle is invalid
	 * @throws DLInvalidDestinationException if the destination is invalid
	 * @throws DLInvalidEnvironmentException if the context is invalid
	 * @throws IOException if failed to save the network
	 * @throws DLCanceledExecutionException if the operation has been canceled
	 */
	void save(DLPythonNetworkHandle handle, URI destination, DLPythonContext context, DLCancelable cancelable)
			throws IllegalArgumentException, DLInvalidDestinationException, DLInvalidEnvironmentException, IOException, DLCanceledExecutionException;

    /**
     * Returns a new Python compatible port object that stores the given network in the given file store. Note that the
     * network's source and the file store may reference the same file.
     *
     * @param network the Python network to store in the port object
     * @param fileStore the file store in which to store the network
     * @return the port object that contains the given network
     * @throws IOException if failed to store the network in the port object
     */
    DLPythonNetworkPortObject<? extends DLPythonNetwork> createPortObject(N network, FileStore fileStore)
        throws IOException;
}
