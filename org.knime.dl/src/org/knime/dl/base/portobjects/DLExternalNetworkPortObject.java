/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
package org.knime.dl.base.portobjects;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.UUID;

import org.apache.commons.io.FilenameUtils;
import org.knime.core.data.filestore.FileStore;
import org.knime.core.data.filestore.FileStorePortObject;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortObjectZipInputStream;
import org.knime.core.node.port.PortObjectZipOutputStream;
import org.knime.core.util.DuplicateKeyException;
import org.knime.core.util.FileUtil;
import org.knime.dl.core.DLInvalidSourceException;
import org.knime.dl.core.DLNetwork;
import org.knime.dl.core.DLNetworkSpec;
import org.knime.dl.core.DLNetworkType;

import com.google.common.base.Strings;

/**
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
public class DLExternalNetworkPortObject extends FileStorePortObject implements DLNetworkPortObject {

	private static final NodeLogger LOGGER = NodeLogger.getLogger(DLNetworkPortObject.class);

	/**
	 * Creates a new file store handle. The name of the file store is randomly generated except for the file extension
	 * which equals the extension of the given source URL. This is useful if a certain file extension is expected when
	 * reading in the stored network at a later point.
	 *
	 * @param source the URL of the source file, <i>not</i> the URL of the file store that will be created
	 * @param exec the execution context that is used to create the file store
	 * @return the created file store
	 * @throws IOException if creating the file store failed for some reason
	 */
	public static FileStore createFileStoreForCopy(final URL source, final ExecutionContext exec) throws IOException {
		final String ext = FilenameUtils.getExtension(source.getFile());
		return createFileStoreForSaving(ext, exec);
	}

	/**
	 * Creates a new file store handle. The name of the file store is randomly generated except for the file extension
	 * which can be specified via the respective parameter.
	 *
	 * @param ext the file extension of the file store, may be null or empty in which case the created file store has no
	 *            file extension
	 * @param exec the execution context that is used to create the file store
	 * @return the created file store
	 * @throws IOException if failed to create the file store
	 */
	public static FileStore createFileStoreForSaving(final String ext, final ExecutionContext exec) throws IOException {
		final String path = UUID.randomUUID().toString()
				+ (!Strings.isNullOrEmpty(ext) ? FilenameUtils.EXTENSION_SEPARATOR + ext : "");
		try {
			return exec.createFileStore(path);
		} catch (final DuplicateKeyException e) {
			throw new IOException(e.getMessage(), e);
		}
	}

	private DLNetwork<?, URL> m_network;

	private DLNetworkPortObjectSpec m_spec;

	/**
	 * Creates a new instance of this port object. The given network will be stored in the given file store.
	 *
	 * @param network the network to store
	 * @param store the file store in which to store the network
	 * @throws IOException if failed to store the network
	 */
	public <N extends DLNetwork<S, URL>, S extends DLNetworkSpec<URL>> DLExternalNetworkPortObject(final N network,
			final FileStore store) throws IOException {
		super(Collections.singletonList(store));
		m_network = network;
		m_spec = new DLDefaultNetworkPortObjectSpec(network.getSpec());
		// Copy network to file store.
		flushToFileStore();
	}

	/**
	 * Empty framework constructor. Must not be called by client code.
	 */
	public DLExternalNetworkPortObject() {
		// fields get populated by serializer
	}

	@Override
	public DLNetwork<?, ?> getNetwork() throws DLInvalidSourceException, IOException {
		if (m_network == null) {
			try {
				@SuppressWarnings("unchecked") // type constraint is fulfilled, see constructor
				final DLNetworkSpec<URL> spec = (DLNetworkSpec<URL>) m_spec.getNetworkSpec();
				// TODO: this can be fixed when KNIME properly supports generic POs
				final DLNetworkType type = spec.getNetworkType();
				final URL source = getFileStore(0).getFile().toURI().toURL();
				m_network = type.wrap(spec, source);
			} catch (final DLInvalidSourceException e) {
				LOGGER.debug(e.getMessage(), e);
				throw e;
			} catch (final IOException e) {
				LOGGER.debug("Failed to load deep learning network from file store.", e);
				throw e;
			}
		}
		return m_network;
	}

	@Override
	public DLNetworkPortObjectSpec getSpec() {
		return m_spec;
	}

	@Override
	protected void flushToFileStore() throws IOException {
		final URL networkSource = m_network.getSource();
		final File fileStoreFile = getFileStore(0).getFile();
		if (!fileStoreFile.toURI().toURL().equals(networkSource)) {
			try (InputStream in = networkSource.openStream();
					FileOutputStream out = new FileOutputStream(fileStoreFile)) {
				FileUtil.copy(in, out);
			}
		}
	}

	public static final class Serializer extends PortObjectSerializer<DLExternalNetworkPortObject> {

		@Override
		public void savePortObject(final DLExternalNetworkPortObject portObject, final PortObjectZipOutputStream out,
				final ExecutionMonitor exec) throws IOException, CanceledExecutionException {
			// no op: PO spec is the only thing that is need to be saved
		}

		@Override
		public DLExternalNetworkPortObject loadPortObject(final PortObjectZipInputStream in, final PortObjectSpec spec,
				final ExecutionMonitor exec) throws IOException, CanceledExecutionException {
			final DLExternalNetworkPortObject portObject = new DLExternalNetworkPortObject();
			portObject.m_spec = (DLNetworkPortObjectSpec) spec;
			return portObject;
		}
	}
}
