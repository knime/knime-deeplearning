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
 *   Jun 26, 2017 (marcel): created
 */
package org.knime.dl.python.core.data.serde;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import org.knime.dl.core.data.DLBuffer;
import org.knime.dl.python.core.data.DLPythonIntBuffer;
import org.knime.python.typeextension.Serializer;
import org.knime.python.typeextension.SerializerFactory;

/**
 *
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
public class DLPythonIntBufferSerializerFactory extends SerializerFactory<DLPythonIntBuffer>
		implements DLSerializerFactory {

	/**
	 * The unique identifier of this serializer factory.
	 */
	public static final String IDENTIFIER = "org.knime.dl.python.core.data.serde.DLPythonIntBufferSerializerFactory";

	/**
	 * Empty framework constructor.
	 */
	public DLPythonIntBufferSerializerFactory() {
		super(DLPythonIntBuffer.class);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Serializer<? extends DLPythonIntBuffer> createSerializer() {

		return new Serializer<DLPythonIntBuffer>() {

			@Override
			public byte[] serialize(final DLPythonIntBuffer value) throws IOException {
				// TODO: we serialize to flat buffers for now
				// final int numDimensions = value.getNumDimensions();
				// final long[] shape = value.getShape();
				final long size = value.size();
				final long numBytes = /*
										 * Integer.BYTES + numDimensions *
										 * Long.BYTES +
										 */ size * Integer.BYTES;
				if (numBytes > Integer.MAX_VALUE) {
					throw new IOException("Transmitting data to Python failed. Buffer size exceeds the limit of "
							+ Integer.MAX_VALUE + "bytes.");
				}
				final IntBuffer intBuffer = IntBuffer.wrap(value.getStorageForReading(0, size));
				final ByteBuffer buffer = ByteBuffer.allocate((int) numBytes);
				buffer.order(ByteOrder.LITTLE_ENDIAN);
				buffer.asIntBuffer().put(intBuffer);
				// TODO: we serialize to flat buffers for now
				// buffer.putInt(numDimensions);
				// for (final long dim : shape) {
				// buffer.putLong(dim);
				// }
				return buffer.array();
			}
		};
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Class<? extends DLBuffer> getBufferType() {
		return DLPythonIntBuffer.class;
	}
}
