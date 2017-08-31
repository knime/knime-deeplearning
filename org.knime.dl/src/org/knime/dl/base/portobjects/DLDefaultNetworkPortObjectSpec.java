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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.zip.ZipEntry;

import org.knime.core.node.port.PortObjectSpecZipInputStream;
import org.knime.core.node.port.PortObjectSpecZipOutputStream;
import org.knime.dl.core.DLNetworkSpec;
import org.knime.dl.core.DLNetworkSpecSerializer;
import org.knime.dl.core.DLNetworkType;
import org.knime.dl.core.DLNetworkTypeRegistry;

/**
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
public class DLDefaultNetworkPortObjectSpec implements DLNetworkPortObjectSpec {

	private static final String ZIP_ENTRY_NAME = "DLDefaultNetworkPortObjectSpec";

	private DLNetworkSpec m_spec;

	public DLDefaultNetworkPortObjectSpec(final DLNetworkSpec spec) {
		m_spec = spec;
	}

	/**
	 * Empty framework constructor. Must not be called by client code.
	 */
	public DLDefaultNetworkPortObjectSpec() {
		// fields get populated by serializer
	}

	@Override
	public DLNetworkSpec getNetworkSpec() {
		return m_spec;
	}

	public static final class Serializer extends PortObjectSpecSerializer<DLDefaultNetworkPortObjectSpec> {
		@Override
		public void savePortObjectSpec(final DLDefaultNetworkPortObjectSpec portObjectSpec,
				final PortObjectSpecZipOutputStream out) throws IOException {
			out.putNextEntry(new ZipEntry(ZIP_ENTRY_NAME));
			final ObjectOutputStream objOut = new ObjectOutputStream(out);
			final DLNetworkType<?, ?> type = portObjectSpec.m_spec.getNetworkType();
			objOut.writeUTF(type.getIdentifier());
			@SuppressWarnings("unchecked")
			final DLNetworkSpecSerializer<DLNetworkSpec> serializer =
					((DLNetworkSpecSerializer<DLNetworkSpec>) type.getNetworkSpecSerializer());
			serializer.serialize(objOut, portObjectSpec.m_spec);
		}

		@Override
		public DLDefaultNetworkPortObjectSpec loadPortObjectSpec(final PortObjectSpecZipInputStream in)
				throws IOException {
			final ZipEntry entry = in.getNextEntry();
			if (!ZIP_ENTRY_NAME.equals(entry.getName())) {
				throw new IOException("Expected zip entry '" + ZIP_ENTRY_NAME + "', got " + entry.getName());
			}
			final ObjectInputStream objIn = new ObjectInputStream(in);
			final String id = objIn.readUTF();
			final DLNetworkType<?, ? extends DLNetworkSpec> type =
					DLNetworkTypeRegistry.getInstance().getNetworkType(id)
							.orElseThrow(() -> new IOException("Failed to load deep learning network. Network type '"
									+ id + "' could not be found. Are you missing a KNIME Deep Learning extension?"));
			final DLNetworkSpec deserialized = type.getNetworkSpecSerializer().deserialize(objIn);
			return new DLDefaultNetworkPortObjectSpec(deserialized);
		}
	}
}
