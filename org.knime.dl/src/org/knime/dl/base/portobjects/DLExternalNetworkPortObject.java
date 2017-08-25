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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.util.Collections;
import java.util.UUID;
import java.util.zip.ZipEntry;

import org.apache.commons.io.FilenameUtils;
import org.knime.core.data.filestore.FileStore;
import org.knime.core.data.filestore.FileStorePortObject;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortObjectZipInputStream;
import org.knime.core.node.port.PortObjectZipOutputStream;
import org.knime.core.util.FileUtil;
import org.knime.dl.core.DLExternalNetwork;
import org.knime.dl.core.DLNetwork;
import org.knime.dl.core.DLNetworkSpec;
import org.knime.dl.core.io.DLNetworkReader;
import org.knime.dl.core.io.DLNetworkReaderRegistry;

/**
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
public class DLExternalNetworkPortObject extends FileStorePortObject implements DLNetworkPortObject {

	/**
	 * @param source <i>not</i> the relative path of the file store itself
	 */
	public static FileStore createFileStoreForCopy(final URL source, final ExecutionContext exec) throws IOException {
		final String ext = FilenameUtils.getExtension(source.getFile());
		return exec.createFileStore(UUID.randomUUID().toString() + FilenameUtils.EXTENSION_SEPARATOR + ext);
	}

	private DLExternalNetwork<?, URL> m_network;
	private DLNetworkPortObjectSpec m_spec;

	// internally used stuff
	private DLNetworkReader<?, ?, URL> m_reader;

	public <N extends DLExternalNetwork<S, URL>, S extends DLNetworkSpec> DLExternalNetworkPortObject(final N network,
			final DLNetworkReader<N, S, URL> reader, final FileStore store) throws IOException {
		super(Collections.singletonList(store));
		m_reader = reader;
		m_spec = new DLDefaultNetworkPortObjectSpec(network.getSpec());
		m_network = network;

		// Actually the framework should do that for us, but it doesn't
		flushToFileStore();
	}

	/**
	 * Empty framework constructor. Must not be called by client code.
	 */
	public DLExternalNetworkPortObject() {
		// fields get populated by serializer
	}

	// TODO not called by framework
	@Override
	protected void flushToFileStore() throws IOException {
		try (FileOutputStream fos = new FileOutputStream(getFileStore(0).getFile())) {
			FileUtil.copy(m_network.getSource().openStream(), fos);
		}
	}

	@Override
	public DLNetwork<?> getNetwork() {
		if (m_network == null) {
			try {
				// TODO type-safety
				m_network = ((DLNetworkReader) m_reader).create(getFileStore(0).getFile().toURI().toURL(),
						m_spec.getNetworkSpec());
			} catch (IllegalArgumentException | IOException e) {
				// TODO exception handling etc...
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return m_network;
	}

	@Override
	public DLNetworkPortObjectSpec getSpec() {
		return m_spec;
	}

	public static final class Serializer extends PortObjectSerializer<DLExternalNetworkPortObject> {

		@Override
		public void savePortObject(final DLExternalNetworkPortObject portObject, final PortObjectZipOutputStream out,
				final ExecutionMonitor exec) throws IOException, CanceledExecutionException {
			// TODO
			out.putNextEntry(new ZipEntry("dl-port-object"));
			final ObjectOutputStream oos = new ObjectOutputStream(out);
			oos.writeUTF(portObject.m_reader.getIdentifier());
			oos.flush();
		}

		@Override
		public DLExternalNetworkPortObject loadPortObject(final PortObjectZipInputStream in, final PortObjectSpec spec,
				final ExecutionMonitor exec) throws IOException, CanceledExecutionException {
			final DLExternalNetworkPortObject portObject = new DLExternalNetworkPortObject();
			in.getNextEntry();
			final ObjectInputStream ois = new ObjectInputStream(in);
			final String id = ois.readUTF();
			// TODO: cast safety, error msgs
			portObject.m_reader =
					(DLNetworkReader<?, ?, URL>) DLNetworkReaderRegistry.getInstance().getNetworkReader(id)
							.orElseThrow(() -> new IllegalStateException("No reader found for id '" + id + "'."));
			portObject.m_spec = (DLNetworkPortObjectSpec) spec;
			return portObject;
		}
	}
}
