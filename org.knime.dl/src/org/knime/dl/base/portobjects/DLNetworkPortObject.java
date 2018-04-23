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
 *   May 16, 2017 (marcel): created
 */
package org.knime.dl.base.portobjects;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.UUID;

import javax.swing.JComponent;

import org.apache.commons.io.FilenameUtils;
import org.knime.core.data.filestore.FileStore;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.PortTypeRegistry;
import org.knime.core.util.DuplicateKeyException;
import org.knime.core.util.FileUtil;
import org.knime.dl.core.DLInvalidSourceException;
import org.knime.dl.core.DLNetwork;

import com.google.common.base.Strings;

/**
 * Base interface for all deep learning port objects.
 *
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public interface DLNetworkPortObject extends PortObject {

	/**
	 * The base deep learning network port type.
	 */
	public static final PortType TYPE = PortTypeRegistry.getInstance().getPortType(DLNetworkPortObject.class);

	/**
     * Creates a new file store handle. The name of the file store is randomly generated except for the file extension
     * which equals the extension of the given source URI. This is useful if a certain file extension is expected when
     * reading in the stored network at a later point.
     *
     * @param source the URI of the source file, <i>not</i> the URI of the file store that will be created
     * @param exec the execution context that is used to create the file store
     * @return the created file store
     * @throws IOException if failed to create the file store
     */
    public static FileStore createFileStoreForCopy(final URI source, final ExecutionContext exec) throws IOException {
        final String ext = FilenameUtils.getExtension(source.getPath());
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

	/**
	 * Copies a single file (no directory) from a source URL to a destination file store.
	 *
	 * @param fileSource the source URL
	 * @param destination the file store
	 * @throws IOException if copying to file store failed
	 */
    public static void copyFileToFileStore(final URI fileSource, final FileStore destination) throws IOException {
		final File file = destination.getFile();
        final URL fileSourceURL = fileSource.toURL();
        if (!file.toURI().toURL().equals(fileSourceURL)) {
            try (InputStream in = fileSourceURL.openStream(); FileOutputStream out = new FileOutputStream(file)) {
				FileUtil.copy(in, out);
			}
		}
	}

	/**
	 * Returns the contained {@link DLNetwork}.
	 *
	 * @return the network
	 * @throws DLInvalidSourceException if network source has become unavailable or invalid
	 * @throws IOException if retrieving the network implied I/O which failed (optional)
	 */
	DLNetwork getNetwork() throws DLInvalidSourceException, IOException;

	@Override
	default String getSummary() {
		return "Deep Learning Network";
	}

	@Override
	DLNetworkPortObjectSpec getSpec();

	@Override
	default JComponent[] getViews() {
		return new JComponent[0];
	}
}
