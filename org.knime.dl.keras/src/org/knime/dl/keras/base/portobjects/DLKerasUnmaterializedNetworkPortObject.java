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
package org.knime.dl.keras.base.portobjects;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;

import org.knime.core.data.filestore.FileStore;
import org.knime.core.data.filestore.FileStorePortObject;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortObjectZipInputStream;
import org.knime.core.node.port.PortObjectZipOutputStream;
import org.knime.dl.core.DLInvalidSourceException;
import org.knime.dl.core.DLNetworkFileStoreLocation;
import org.knime.dl.keras.core.DLKerasNetwork;
import org.knime.dl.keras.core.layers.DLInvalidTensorSpecException;
import org.knime.dl.keras.core.layers.DLKerasLayer;
import org.knime.dl.keras.core.layers.DLKerasNetworkLayerGraphSerializer;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public final class DLKerasUnmaterializedNetworkPortObject extends FileStorePortObject
    implements DLKerasNetworkPortObjectBase {

    private static final String ZIP_ENTRY_NAME = "DLKerasUnmaterializedNetworkPortObject";

    private static List<FileStore> prependOutputFileStore(final FileStore outputFileStore,
        final List<FileStore> baseNetworkFileStores) {
        // Own file store must be first in the list. Instance code relies on that.
        baseNetworkFileStores.add(0, outputFileStore);
        return baseNetworkFileStores;
    }

    private DLKerasPortObjectContent m_content;

    public DLKerasUnmaterializedNetworkPortObject(final List<DLKerasLayer> outputLayers, final FileStore fileStore) {
        super(prependOutputFileStore(fileStore, DLKerasNetworkLayerGraphSerializer.getNetworkFileStores(outputLayers)));
        try {
            m_content = new DLKerasUnmaterializedPortObjectContent(outputLayers);
        } catch (final DLInvalidTensorSpecException e) {
            // This should not occur because the layer input specs were already validated in the preceding layer node.
            throw new IllegalStateException(e);
        }
    }

    /**
     * Deserialization constructor.
     */
    private DLKerasUnmaterializedNetworkPortObject(final DLKerasPortObjectContent content) {
        m_content = content;
    }

    @Override
    public DLKerasNetwork getNetwork() throws DLInvalidSourceException, IOException {
        if (m_content instanceof DLKerasUnmaterializedPortObjectContent) {
            final DLNetworkFileStoreLocation saveLocation = new DLNetworkFileStoreLocation(getFileStore(0));
            m_content = ((DLKerasUnmaterializedPortObjectContent)m_content).materialize(saveLocation);
        }
        final DLKerasMaterializedPortObjectContent materialized = (DLKerasMaterializedPortObjectContent)m_content;
        if (materialized.getNetworkSource() == null) {
            materialized.setNetworkSource(new DLNetworkFileStoreLocation(getFileStore(0)));
        }
        return materialized.getNetwork();
    }

    @Override
    public DLKerasNetworkPortObjectSpecBase getSpec() {
        return m_content.getSpec();
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + m_content.hashCode();
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != getClass()) {
            return false;
        }
        final DLKerasUnmaterializedNetworkPortObject other = (DLKerasUnmaterializedNetworkPortObject)obj;
        return super.equals(obj) //
            && other.m_content.equals(m_content);
    }

    @Override
    protected void postConstruct() throws IOException {
        if (m_content instanceof DLKerasUnmaterializedPortObjectContent) {
            final DLKerasUnmaterializedPortObjectContent unmaterialized =
                (DLKerasUnmaterializedPortObjectContent)m_content;
            final List<DLNetworkFileStoreLocation> baseNetworkSources = new ArrayList<>(getFileStoreCount() - 1);
            for (int i = 1; i < getFileStoreCount(); i++) {
                baseNetworkSources.add(new DLNetworkFileStoreLocation(getFileStore(i)));
            }
            unmaterialized.getSpec().amendBaseNetworkSources(baseNetworkSources);
        }
    }

    /**
     * Serializer of {@link DLKerasUnmaterializedNetworkPortObject}.
     */
    public static final class Serializer extends PortObjectSerializer<DLKerasUnmaterializedNetworkPortObject> {

        @Override
        public void savePortObject(final DLKerasUnmaterializedNetworkPortObject portObject,
            final PortObjectZipOutputStream out, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
            out.putNextEntry(new ZipEntry(ZIP_ENTRY_NAME));
            final ObjectOutputStream objOut = new ObjectOutputStream(out);
            final boolean isMaterialized = portObject.m_content instanceof DLKerasMaterializedPortObjectContent;
            objOut.writeBoolean(isMaterialized);
            if (isMaterialized) {
                new DLKerasMaterializedPortObjectContent.Serializer()
                    .savePortObjectContent((DLKerasMaterializedPortObjectContent)portObject.m_content, objOut, exec);
            }
            objOut.flush();
        }

        @Override
        public DLKerasUnmaterializedNetworkPortObject loadPortObject(final PortObjectZipInputStream in,
            final PortObjectSpec spec, final ExecutionMonitor exec) throws IOException, CanceledExecutionException {
            final ZipEntry entry = in.getNextEntry();
            if (!ZIP_ENTRY_NAME.equals(entry.getName())) {
                throw new IOException("Failed to load Keras deep learning network port object. Invalid zip entry name '"
                    + entry.getName() + "', expected '" + ZIP_ENTRY_NAME + "'.");
            }
            final ObjectInputStream objIn = new ObjectInputStream(in);
            DLKerasPortObjectContent portObjectContent;
            if (objIn.readBoolean()) { // materialized?
                portObjectContent =
                    new DLKerasMaterializedPortObjectContent.Serializer().loadPortObjectContent(objIn, spec, exec);
            } else {
                portObjectContent = new DLKerasUnmaterializedPortObjectContent.Serializer().loadPortObjectContent(spec);
            }
            return new DLKerasUnmaterializedNetworkPortObject(portObjectContent);
        }
    }
}
