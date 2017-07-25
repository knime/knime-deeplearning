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
import java.util.Iterator;
import java.util.List;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.util.FileUtil;
import org.knime.dl.base.portobjects.DLDefaultNetworkPortObjectSpec;
import org.knime.dl.base.portobjects.DLNetworkPortObject;
import org.knime.dl.base.portobjects.DLNetworkPortObjectSpec;
import org.knime.dl.base.portobjects.DLNetworkReferencePortObject;
import org.knime.dl.core.DLNetwork;
import org.knime.dl.core.backend.DLBackend;
import org.knime.dl.core.backend.DLBackendRegistry;
import org.knime.dl.core.backend.DLProfile;
import org.knime.dl.core.io.DLNetworkReader;
import org.knime.dl.keras.core.DLKerasDefaultBackend;

/**
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
final class DLKerasReaderNodeModel extends NodeModel {

	static SettingsModelString createFilePathStringModel(final String defaultPath) {
		return new SettingsModelString("file_path", defaultPath);
	}

	static List<String> getValidInputFileExtensions() {
		return Arrays.asList("h5", "json", "yaml");
	}

	private final DLBackend m_backend;

	private final DLNetworkReader<?> m_reader;

	private final SettingsModelString m_filePath = createFilePathStringModel("");

	private DLNetworkPortObjectSpec m_spec;

	private String m_lastFilePath;

	protected DLKerasReaderNodeModel() {
		super(null, new PortType[] { DLNetworkPortObject.TYPE });
		// TODO: referring to a default implementation must be avoided, else our abstractions are pointless
		m_backend = DLBackendRegistry.getBackend(DLKerasDefaultBackend.IDENTIFIER)
				.orElseThrow(() -> new IllegalStateException(
						"Selected back end '" + DLKerasDefaultBackend.IDENTIFIER + "' could not be found."));
		m_reader = m_backend.createReader();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
		final String filePath = m_filePath.getStringValue();
		if (filePath == null || filePath.isEmpty()) {
			throw new InvalidSettingsException("Empty file path.");
		}
		if (!filePath.equals(m_lastFilePath)) {
			final URL url;
			try {
				url = FileUtil.toURL(filePath);
			} catch (InvalidPathException | MalformedURLException e) {
				throw new InvalidSettingsException("Invalid or unsupported file path. See log for details.", e);
			}
			DLNetwork network;
			try {
				network = m_reader.readNetwork(url);
			} catch (final Exception e) {
				throw new InvalidSettingsException(
						"Failed to read deep learning network specification. See log for details.", e);
			}

			final DLProfile profile = new DLProfile() {

				@Override
				public Iterator<DLBackend> iterator() {
					return Arrays.asList(m_backend).iterator();
				}

				@Override
				public int size() {
					return 1;
				}
			};
			m_spec = new DLDefaultNetworkPortObjectSpec(network, profile, url);
			m_lastFilePath = filePath;
		}
		return new PortObjectSpec[] { m_spec };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
		final URL url = FileUtil.toURL(m_lastFilePath);
		return new PortObject[] { new DLNetworkReferencePortObject(url, m_spec) };
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		// TODO check if input file still exists (see FileReaderNodeModel#loadInternals(File,ExecutionMonitor))
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		// no op
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
		m_filePath.saveSettingsTo(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
		m_filePath.validateSettings(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
		m_filePath.loadSettingsFrom(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void reset() {
		// no op
	}
}
