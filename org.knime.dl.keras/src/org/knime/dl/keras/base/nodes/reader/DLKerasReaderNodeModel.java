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
 * History
 *   May 9, 2017 (marcel): created
 */
package org.knime.dl.keras.base.nodes.reader;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.InvalidPathException;
import java.util.Arrays;
import java.util.List;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.defaultnodesettings.SettingsModelStringArray;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.util.FileUtil;
import org.knime.dl.base.portobjects.DLDefaultNetworkPortObject;
import org.knime.dl.base.portobjects.DLDefaultNetworkPortObjectSpec;
import org.knime.dl.base.portobjects.DLExternalNetworkPortObject;
import org.knime.dl.base.portobjects.DLNetworkPortObject;
import org.knime.dl.core.DLException;
import org.knime.dl.core.DLInvalidSourceException;
import org.knime.dl.core.DLNetworkType;
import org.knime.dl.core.DLNetworkTypeRegistry;
import org.knime.dl.core.DLUnavailableDependencyException;
import org.knime.dl.keras.core.DLKerasNetwork;
import org.knime.dl.keras.core.DLKerasNetworkType;
import org.knime.dl.python.core.DLPythonDefaultNetworkReader;

import com.google.common.base.Strings;

/**
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
final class DLKerasReaderNodeModel extends NodeModel {

	private static final String CFG_KEY_FILE_PATH = "file_path";

	private static final String CFG_KEY_BACKEND = "backend";

	private static final String CFG_KEY_COPY_NETWORK = "copy_network";

	private static final NodeLogger LOGGER = NodeLogger.getLogger(DLKerasReaderNodeModel.class);

	static SettingsModelString createFilePathStringModel(final String defaultPath) {
		return new SettingsModelString(CFG_KEY_FILE_PATH, defaultPath);
	}

	static SettingsModelStringArray createKerasBackendModel() {
		return new SettingsModelStringArray(CFG_KEY_BACKEND, new String[] { "<none>", null });
	}

	static SettingsModelBoolean createCopyNetworkSettingsModel() {
		return new SettingsModelBoolean(CFG_KEY_COPY_NETWORK, false);
	}

	// TODO: this should be fetched from the Keras loader (#getLoadModelFileExtensions)
	static List<String> getValidInputFileExtensions() {
		return Arrays.asList("h5", "json", "yaml");
	}

	private final SettingsModelString m_smFilePath = createFilePathStringModel("");

	private final SettingsModelStringArray m_smBackend = createKerasBackendModel();

	private final SettingsModelBoolean m_smCopyNetwork = createCopyNetworkSettingsModel();

	private String m_lastFilePath;

	private String m_lastBackendId;

	private DLKerasNetwork<?> m_network;

	protected DLKerasReaderNodeModel() {
		super(null, new PortType[] { DLNetworkPortObject.TYPE });
	}

	@Override
	protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
		final String filePath = m_smFilePath.getStringValue();
		final String backendId = m_smBackend.getStringArrayValue()[1];
		if (filePath == null || filePath.isEmpty()) {
			throw new InvalidSettingsException("No file selected. Please configure the node.");
		}
		if (backendId == null || backendId.isEmpty()) {
			throw new InvalidSettingsException("No back end selected. Please configure the node.");
		}
		final URL url;
		try {
			url = FileUtil.toURL(filePath);
		} catch (InvalidPathException | MalformedURLException e) {
			throw new InvalidSettingsException("Invalid or unsupported file path: '" + filePath + "'.", e);
		}
		final DLKerasNetworkType<?, ?> backend = getBackend(backendId);
		try {
			backend.checkAvailability(false);
		} catch (final DLUnavailableDependencyException e) {
			throw new InvalidSettingsException(
					"Selected Keras back end '" + backend.getName() + "' is not available anymore. "
							+ "Please check your local installation.\nDetails: " + e.getMessage());
		}
		try {
			backend.getLoader().validateSource(url);
		} catch (final DLInvalidSourceException e) {
			throw new InvalidSettingsException(e.getMessage(), e);
		}
		if (!filePath.equals(m_lastFilePath) || !backendId.equals(m_lastBackendId)) {
			try {
				m_network = new DLPythonDefaultNetworkReader<>(backend.getLoader()).read(url);
			} catch (final Exception e) {
				String message;
				if (e instanceof DLException) {
					message = e.getMessage();
				} else {
					if (!Strings.isNullOrEmpty(e.getMessage())) {
						LOGGER.error(e.getMessage());
					}
					message = "Failed to read deep learning network specification. See log for details.";
				}
				throw new InvalidSettingsException(message, e);
			}
			m_lastFilePath = filePath;
			m_lastBackendId = backendId;
		}
		return new PortObjectSpec[] { new DLDefaultNetworkPortObjectSpec(m_network.getSpec()) };
	}

	@Override
	protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
		if (m_lastFilePath == null || m_lastBackendId == null) {
			throw new RuntimeException("Node is not yet configured, please configure first.");
		}
		final DLKerasNetworkType<?, ?> backend = getBackend(m_lastBackendId);
		try {
			backend.getLoader().validateSource(m_network.getSource());
		} catch (final DLInvalidSourceException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		final PortObject out;
		if (m_smCopyNetwork.getBooleanValue()) {
			out = new DLExternalNetworkPortObject(m_network,
					DLExternalNetworkPortObject.createFileStoreForCopy(FileUtil.toURL(m_lastFilePath), exec));
		} else {
			out = new DLDefaultNetworkPortObject(m_network);
		}
		return new PortObject[] { out };
	}

	@Override
	protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		// no op
	}

	@Override
	protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		// no op
	}

	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
		m_smFilePath.saveSettingsTo(settings);
		m_smBackend.saveSettingsTo(settings);
		m_smCopyNetwork.saveSettingsTo(settings);
	}

	@Override
	protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
		m_smFilePath.validateSettings(settings);
		m_smBackend.validateSettings(settings);
		m_smCopyNetwork.validateSettings(settings);
	}

	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
		m_smFilePath.loadSettingsFrom(settings);
		m_smBackend.loadSettingsFrom(settings);
		m_smCopyNetwork.loadSettingsFrom(settings);
	}

	@Override
	protected void reset() {
		// no op
	}

	private DLKerasNetworkType<?, ?> getBackend(final String backendId) throws InvalidSettingsException {
		final DLNetworkType<?, ?, ?> backend = DLNetworkTypeRegistry.getInstance().getNetworkType(backendId)
				.orElseThrow(() -> new InvalidSettingsException("Selected Keras back end '" + backendId
						+ "' cannot be found. Are you missing a KNIME extension?"));
		if (!(backend instanceof DLKerasNetworkType)) {
			throw new InvalidSettingsException("Selected back end '" + backendId
					+ "' is not a valid Keras back end. This is an implementation error.");
		}
		return (DLKerasNetworkType<?, ?>) backend;
	}
}
