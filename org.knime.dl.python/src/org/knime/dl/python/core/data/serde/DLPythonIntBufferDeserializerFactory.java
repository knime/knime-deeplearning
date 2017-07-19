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
 *   Jun 28, 2017 (marcel): created
 */
package org.knime.dl.python.core.data.serde;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import org.knime.core.data.DataCell;
import org.knime.core.data.filestore.FileStoreFactory;
import org.knime.dl.core.DLLayerData;
import org.knime.dl.python.core.data.DLPythonIntBuffer;
import org.knime.python.typeextension.Deserializer;
import org.knime.python.typeextension.DeserializerFactory;

/**
 *
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
public class DLPythonIntBufferDeserializerFactory extends DeserializerFactory implements DLPythonDeserializerFactory {

    /**
     * The unique identifier of this deserializer factory.
     */
    public static final String IDENTIFIER = "org.knime.dl.python.core.data.serde.DLPythonIntBufferDeserializerFactory";

    /**
     * Empty framework constructor.
     */
    public DLPythonIntBufferDeserializerFactory() {
        super(DLPythonIntBuffer.TYPE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Deserializer createDeserializer() {
        return new DLPythonDeserializer<DLPythonIntBuffer>() {

            @Override
            public DataCell deserialize(final byte[] bytes, final FileStoreFactory fileStoreFactory)
                throws IOException {
                final ByteBuffer buffer = ByteBuffer.wrap(bytes);
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                // TODO: we serialize to a flat buffer for now
                // final int numDimensions = buffer.getInt();
                // final long[] shape = new long[numDimensions];
                // for (int i = 0; i < numDimensions; i++) {
                //    shape[i] = buffer.getLong();
                // }
                final IntBuffer intBuffer = buffer.asIntBuffer();
                final DLPythonIntBuffer value = new DLPythonIntBuffer(buffer.capacity() / Integer.BYTES);
                intBuffer.get(value.getStorageForWriting(0, intBuffer.limit()));
                return value;
            }

            @Override
            public void deserialize(final byte[] bytes, final DLLayerData<DLPythonIntBuffer> data) {
                final ByteBuffer buffer = ByteBuffer.wrap(bytes);
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                // TODO: we serialize to a flat buffer for now
                // final int numDimensions = buffer.getInt();
                // final long[] shape = new long[numDimensions];
                // for (int i = 0; i < numDimensions; i++) {
                //    shape[i] = buffer.getLong();
                // }
                final IntBuffer intBuffer = buffer.asIntBuffer();
                intBuffer.get(data.getBuffer().getStorageForWriting(0, intBuffer.limit()));
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<? extends DLPythonIntBuffer> getBufferType() {
        return DLPythonIntBuffer.class;
    }
}
