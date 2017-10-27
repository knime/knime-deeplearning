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
package org.knime.dl.keras.core;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.apache.commons.io.FilenameUtils;
import org.knime.core.data.filestore.FileStore;
import org.knime.core.util.FileUtil;
import org.knime.dl.core.DLInvalidDestinationException;
import org.knime.dl.core.DLInvalidEnvironmentException;
import org.knime.dl.core.DLInvalidSourceException;
import org.knime.dl.keras.base.portobjects.DLKerasNetworkPortObject;
import org.knime.dl.python.core.DLPythonAbstractNetworkLoader;
import org.knime.dl.python.core.DLPythonContext;
import org.knime.dl.python.core.DLPythonNetwork;
import org.knime.dl.python.core.DLPythonNetworkHandle;
import org.knime.dl.python.core.DLPythonNetworkPortObject;

/**
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
public abstract class DLKerasAbstractNetworkLoader<N extends DLKerasNetwork> extends DLPythonAbstractNetworkLoader<N>
		implements DLKerasNetworkLoader<N> {

	@Override
	protected abstract DLKerasAbstractCommands createCommands(DLPythonContext context)
			throws DLInvalidEnvironmentException;

	@Override
	public void validateSource(final URL source) throws DLInvalidSourceException {
		final File sourceFile;
		try {
			sourceFile = FileUtil.getFileFromURL(source);
		} catch (final IllegalArgumentException e) {
			throw new DLInvalidSourceException(
					"An error occurred while resolving the Keras network file location.\nCause\n:" + e.getMessage());
		}
		if (sourceFile == null || !sourceFile.exists()) {
			throw new DLInvalidSourceException(
					"Cannot find Keras network file at location '" + source.toString() + "'.");
		}
	}

	@Override
	public void validateDestination(final URL destination) throws DLInvalidDestinationException {
		final File destinationFile;
		try {
			destinationFile = FileUtil.getFileFromURL(destination);
		} catch (final IllegalArgumentException e) {
			throw new DLInvalidDestinationException(
					"An error occurred while resolving the Keras network file location.\nCause\n:" + e.getMessage());
		}
		if (destinationFile == null || destinationFile.exists()) {
			throw new DLInvalidDestinationException(
					"Invalid file destination or file already exists: '" + destination.toString() + "'.");
		}
	}

	@Override
	public DLPythonNetworkHandle load(final URL source, final DLPythonContext kernel)
			throws DLInvalidSourceException, DLInvalidEnvironmentException, IOException {
		validateSource(source);
		final String filePath = FileUtil.getFileFromURL(source).getAbsolutePath();
		final String fileExtension = FilenameUtils.getExtension(filePath);
		final DLKerasAbstractCommands commands = createCommands(checkNotNull(kernel));
		try {
			final DLPythonNetworkHandle networkHandle;
			if (fileExtension.equals("h5")) {
				networkHandle = commands.loadNetwork(filePath);
			} else if (fileExtension.equals("json")) {
				networkHandle = commands.loadNetworkFromJson(filePath);
			} else if (fileExtension.equals("yaml")) {
				networkHandle = commands.loadNetworkFromYaml(filePath);
			} else {
				throw new DLInvalidSourceException(
						"Keras network reader only supports network files of type h5, json and yaml.");
			}
			return networkHandle;
		} catch (final IOException e) {
			throw new IOException(
					"An error occurred while communicating with Python (while reading in the Keras network).", e);
		}
	}

	@Override
	public DLPythonNetworkPortObject<? extends DLPythonNetwork> createPortObject(final N network,
			final FileStore fileStore) throws IOException {
		return new DLKerasNetworkPortObject(network, fileStore);
	}
}
