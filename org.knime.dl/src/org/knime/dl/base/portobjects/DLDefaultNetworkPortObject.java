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
 *   May 22, 2017 (marcel): created
 */
package org.knime.dl.base.portobjects;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.zip.ZipEntry;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.AbstractPortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortObjectZipInputStream;
import org.knime.core.node.port.PortObjectZipOutputStream;
import org.knime.dl.core.DLExternalNetwork;
import org.knime.dl.core.DLExternalNetworkSpec;
import org.knime.dl.core.DLInvalidSourceException;
import org.knime.dl.core.DLNetwork;
import org.knime.dl.core.DLNetworkSerializer;
import org.knime.dl.core.DLNetworkSpec;
import org.knime.dl.core.DLNetworkType;
import org.knime.dl.core.DLNetworkTypeRegistry;

/**
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
public class DLDefaultNetworkPortObject extends AbstractPortObject implements DLNetworkPortObject {

	private static final NodeLogger LOGGER = NodeLogger.getLogger(DLNetworkPortObject.class);

	private static final String ZIP_ENTRY_NAME = "DLExternalNetworkPortObject";

	private DLNetworkPortObjectSpec m_spec;

	private DLNetwork<?, ?> m_network;

	/**
	 * Creates a new instance of this port object. s
	 *
	 * @param networkReference the source of the network
	 * @param spec the corresponding port object spec
	 */
	public DLDefaultNetworkPortObject(final DLNetwork<?, ?> network) {
		m_spec = new DLDefaultNetworkPortObjectSpec(network.getSpec());
		m_network = network;
	}

	/**
	 * Empty framework constructor. Must not be called by client code.
	 */
	public DLDefaultNetworkPortObject() {
		// fields get populated by serializer
	}

	@Override
	public DLNetworkPortObjectSpec getSpec() {
		return m_spec;
	}

	@Override
	public DLNetwork<?, ?> getNetwork() throws DLInvalidSourceException, IOException {
		if (m_network instanceof DLExternalNetwork) {
			// check if network source is still available
			validateNetworkSource((DLExternalNetwork<?, ?>) m_network);
		}
		return m_network;
	}

	@Override
	protected void save(final PortObjectZipOutputStream out, final ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		out.putNextEntry(new ZipEntry(ZIP_ENTRY_NAME));
		final ObjectOutputStream objOut = new ObjectOutputStream(out);
		final DLNetworkType<?, ?, ?> type = m_network.getSpec().getNetworkType();
		objOut.writeUTF(type.getIdentifier());
		@SuppressWarnings({ "unchecked" }) // serializer is fetched from network's type - they must match
		final DLNetworkSerializer<DLNetwork<?, ?>, ?> ser =
				(DLNetworkSerializer<DLNetwork<?, ?>, ?>) type.getNetworkSerializer();
		ser.serialize(objOut, m_network);
	}

	@Override
	protected void load(final PortObjectZipInputStream in, final PortObjectSpec spec, final ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		in.getNextEntry();
		m_spec = (DLNetworkPortObjectSpec) spec;
		final ObjectInputStream objIn = new ObjectInputStream(in);
		final String id = objIn.readUTF();
		@SuppressWarnings("unchecked") // if this cast fails, there is an implementation error in the registry
		final DLNetworkType<?, DLNetworkSpec<?>, ?> type = (DLNetworkType<?, DLNetworkSpec<?>, ?>) DLNetworkTypeRegistry
				.getInstance().getNetworkType(id)
				.orElseThrow(() -> new IllegalStateException("Failed to load deep learning network. Network type '" + id
						+ "' could not be found. Are you missing a KNIME Deep Learning extension?."));
		m_network = type.getNetworkSerializer().deserialize(objIn, m_spec.getNetworkSpec());
	}

	private <A> void validateNetworkSource(final DLExternalNetwork<? extends DLExternalNetworkSpec<A>, A> net)
			throws DLInvalidSourceException {
		// we want to fail fast in case the network is missing
		try {
			net.getSpec().getNetworkType().getLoader().validateSource(net.getSource());
		} catch (final DLInvalidSourceException e) {
			LOGGER.debug("Source validation failed. Invalid source: '" + net.getSource().toString() + "'.", e);
			throw e;
		}
	}

	// KNIME
	public static final class Serializer extends AbstractPortObjectSerializer<DLDefaultNetworkPortObject> {
	}
}
