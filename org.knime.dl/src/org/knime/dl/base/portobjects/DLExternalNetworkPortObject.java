/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
import org.knime.core.util.FileUtil;
import org.knime.dl.core.DLExternalNetworkSpec;
import org.knime.dl.core.DLExternalNetworkType;
import org.knime.dl.core.DLInvalidSourceException;
import org.knime.dl.core.DLNetwork;

/**
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
public class DLExternalNetworkPortObject extends FileStorePortObject implements DLNetworkPortObject {

	private static final NodeLogger LOGGER = NodeLogger.getLogger(DLNetworkPortObject.class);

	/**
	 * @param source <i>not</i> the relative path of the file store itself
	 */
	public static FileStore createFileStoreForCopy(final URL source, final ExecutionContext exec) throws IOException {
		final String ext = FilenameUtils.getExtension(source.getFile());
		return createFileStoreForSaving(ext, exec);
	}

	public static FileStore createFileStoreForSaving(final String ext, final ExecutionContext exec) throws IOException {
		return exec.createFileStore(UUID.randomUUID().toString() + FilenameUtils.EXTENSION_SEPARATOR + ext);
	}

	private DLNetwork<?, URL> m_network;

	private DLNetworkPortObjectSpec m_spec;

	public <N extends DLNetwork<S, URL>, S extends DLExternalNetworkSpec<URL>> DLExternalNetworkPortObject(
			final N network, final FileStore store) throws IOException {
		super(Collections.singletonList(store));
		m_network = network;
		m_spec = new DLDefaultNetworkPortObjectSpec(network.getSpec());
		// actually, the framework should do that for us, but it doesn't
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
				final DLExternalNetworkSpec<URL> spec = (DLExternalNetworkSpec<URL>) m_spec.getNetworkSpec();
				// TODO: this can be fixed when KNIME properly supports generic POs
				final DLExternalNetworkType type = spec.getNetworkType();
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

	// TODO: not called by framework, should be
	@Override
	protected void flushToFileStore() throws IOException {
		final File fileStoreFile = getFileStore(0).getFile();
		if (!fileStoreFile.toURI().toURL().equals(m_network.getSource())) {
			try (FileOutputStream fos = new FileOutputStream(fileStoreFile)) {
				FileUtil.copy(m_network.getSource().openStream(), fos);
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
