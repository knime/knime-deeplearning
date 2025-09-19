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
package org.knime.dl.keras.core;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.UUID;

import org.apache.commons.io.FilenameUtils;
import org.knime.base.filehandling.remote.files.RemoteFile;
import org.knime.base.filehandling.remote.files.RemoteFileHandlerRegistry;
import org.knime.core.data.filestore.FileStore;
import org.knime.core.util.FileUtil;
import org.knime.core.util.Version;
import org.knime.dl.core.DLCancelable;
import org.knime.dl.core.DLException;
import org.knime.dl.core.DLInvalidDestinationException;
import org.knime.dl.core.DLInvalidEnvironmentException;
import org.knime.dl.core.DLInvalidSourceException;
import org.knime.dl.keras.base.portobjects.DLKerasNetworkPortObject;
import org.knime.dl.python.core.DLPythonAbstractNetworkLoader;
import org.knime.dl.python.core.DLPythonContext;
import org.knime.dl.python.core.DLPythonNetworkHandle;

import com.google.common.base.Strings;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public abstract class DLKerasAbstractNetworkLoader<N extends DLKerasNetwork> extends DLPythonAbstractNetworkLoader<N>
		implements DLKerasNetworkLoader<N> {

    private static final Version COMPATIBILITY_VERSION_LIMIT = new Version("2.15");

    public static URL validateKerasNetworkSource(final URI source) throws DLInvalidSourceException {
        try {
            final URL sourceURL = source.toURL();
            final RemoteFile<?> file = RemoteFileHandlerRegistry.getRemoteFileHandler(sourceURL.getProtocol())
                .createRemoteFile(sourceURL.toURI(), null, null);
            if (!file.exists()) {
                throw new DLInvalidSourceException(
                    "Cannot find Keras network file at location '" + sourceURL.toString() + "'.");
            }
            return sourceURL;
        } catch (final Exception e) {
            throw new DLInvalidSourceException(
                "An error occurred while resolving the Keras network file location.\nCause: " + e.getMessage());
        }
    }

    public static URL validateKerasNetworkDestination(final URI destination) throws DLInvalidDestinationException {
        try {
            final URL destinationURL = destination.toURL();
            // we found a remote file handler. however, we still don't know if we can write here.
            RemoteFileHandlerRegistry.getRemoteFileHandler(destinationURL.getProtocol()).createRemoteFile(destination,
                null, null);
            return destinationURL;
        } catch (final Exception e) {
            throw new DLInvalidDestinationException(
                "An error occurred while resolving the Keras network file location.\nCause: " + e.getMessage());
        }
    }

    @Override
    public URL validateSource(final URI source) throws DLInvalidSourceException {
        return validateKerasNetworkSource(source);
    }

    @Override
    public URL validateDestination(final URI destination) throws DLInvalidDestinationException {
        return validateKerasNetworkDestination(destination);
    }

    @Override
    public DLPythonNetworkHandle load(final N network, final DLPythonContext kernel, final boolean loadTrainingConfig,
        final DLCancelable cancelable) throws DLInvalidSourceException, DLInvalidEnvironmentException, IOException {
        final Version kerasVersion = network.getSpec().getKerasVersion();
        final boolean compatibilityMode =
            kerasVersion == null || kerasVersion.compareTo(COMPATIBILITY_VERSION_LIMIT) <= 0;
        return loadInternal(network.getSource().getURI(), kernel, loadTrainingConfig, compatibilityMode, cancelable);
    }

    @Override
    public DLPythonNetworkHandle load(final URI source, final DLPythonContext kernel, final boolean loadTrainingConfig,
        final DLCancelable cancelable)
        throws DLInvalidSourceException, DLInvalidEnvironmentException, IOException {
        return loadInternal(source, kernel, loadTrainingConfig, false, cancelable);
    }

    private DLPythonNetworkHandle loadInternal(final URI source, final DLPythonContext kernel, final boolean loadTrainingConfig,
        final boolean compatibilityMode, final DLCancelable cancelable)
        throws DLInvalidSourceException, DLInvalidEnvironmentException, IOException {
        final URL sourceURL = validateSource(source);
        File file = null;
        try {
            file = FileUtil.getFileFromURL(sourceURL);
        } catch (final IllegalArgumentException ex) {
            // Unknown protocol or resolving failed. Handled below.
        }
        try {
			if (file == null) {
                file = resolveToTmpFile(RemoteFileHandlerRegistry.getRemoteFileHandler(sourceURL.getProtocol())
                    .createRemoteFile(source, null, null));
			}

			final String filePath = file.getAbsolutePath();
			final String fileExtension = FilenameUtils.getExtension(filePath);

			final DLKerasAbstractCommands commands = createCommands(checkNotNull(kernel));
			final DLPythonNetworkHandle networkHandle;
			if (fileExtension.equals("h5")) {
				networkHandle = commands.loadNetwork(filePath, loadTrainingConfig, compatibilityMode, cancelable);
			} else if (fileExtension.equals("json")) {
				networkHandle = commands.loadNetworkFromJson(filePath, compatibilityMode, cancelable);
			} else if (fileExtension.equals("yaml")) {
				networkHandle = commands.loadNetworkFromYaml(filePath, compatibilityMode, cancelable);
			} else {
				throw new DLInvalidSourceException(
						"Keras network reader only supports network files of type h5, json and yaml.");
			}
			return networkHandle;
		} catch (final Exception e) {
			String message = "An error occurred while communicating with Python (while reading in the Keras network).";
			if (e instanceof DLException && !Strings.isNullOrEmpty(e.getMessage())) {
				message += "\nCause: " + e.getMessage();
			}
			throw new IOException(message, e);
		}
	}

    @Override
    public DLKerasNetworkPortObject createPortObject(final N network, final FileStore fileStore) throws IOException {
        return new DLKerasNetworkPortObject(network, fileStore);
    }

	private File resolveToTmpFile(final RemoteFile<?> remote) throws Exception {
		// Later we may want to have a repo to store already downloaded networks...
		final String extension = FilenameUtils.getExtension(remote.getFullName());
		final File tmp = FileUtil.createTempFile(UUID.randomUUID().toString(), "." + extension, true);
		try (FileOutputStream stream = new FileOutputStream(tmp)) {
			FileUtil.copy(remote.openInputStream(), stream);
		}
		return tmp;
	}
}
