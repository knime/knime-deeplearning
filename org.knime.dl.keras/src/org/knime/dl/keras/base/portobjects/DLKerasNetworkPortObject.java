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

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.knime.core.data.filestore.FileStore;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortObjectZipInputStream;
import org.knime.core.node.port.PortObjectZipOutputStream;
import org.knime.dl.base.portobjects.DLAbstractNetworkPortObject;
import org.knime.dl.base.portobjects.DLNetworkPortObject;
import org.knime.dl.core.DLInvalidEnvironmentException;
import org.knime.dl.core.DLInvalidSourceException;
import org.knime.dl.core.DLNetworkFileStoreLocation;
import org.knime.dl.core.DLNetworkLocation;
import org.knime.dl.core.DLNetworkReferenceLocation;
import org.knime.dl.keras.core.DLKerasNetwork;
import org.knime.dl.keras.core.DLKerasNetworkSpec;
import org.knime.dl.python.core.DLPythonDefaultNetworkReader;
import org.knime.dl.python.core.DLPythonNetwork;
import org.knime.dl.python.core.DLPythonNetworkLoader;
import org.knime.dl.python.core.DLPythonNetworkLoaderRegistry;

/**
 * Keras implementation of a deep learning {@link DLNetworkPortObject network port object}.
 *
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public final class DLKerasNetworkPortObject extends
    DLAbstractNetworkPortObject<DLKerasNetwork, DLKerasNetworkPortObjectSpec> implements DLKerasNetworkPortObjectBase {

    private static final String ZIP_ENTRY_NAME = "DLKerasNetworkPortObject";

    private static List<FileStore> getNetworkFileStore(final DLKerasNetwork network) {
        final DLNetworkLocation networkSource = network.getSource();
        if (networkSource instanceof DLNetworkReferenceLocation) {
            return Collections.emptyList();
        } else if (networkSource instanceof DLNetworkFileStoreLocation) {
            return Collections.singletonList(((DLNetworkFileStoreLocation)networkSource).getFileStore());
        } else {
            throw new UnsupportedOperationException("Keras network source (" + networkSource + ") is neither of type "
                + DLNetworkReferenceLocation.class.getCanonicalName() + " nor of type "
                + DLNetworkFileStoreLocation.class.getCanonicalName() + ". This is an implementation error.");
        }
    }

    private final DLKerasMaterializedPortObjectContent m_content;

    /**
     * Creates a new Keras deep learning network port object. The given network is stored in (i.e. copied to if not
     * already there) the given file store.
     *
     * @param network the Keras deep learning network to store
     * @param fileStore the file store in which to store the network
     * @throws IOException if failed to store the network
     */
    public DLKerasNetworkPortObject(final DLKerasNetwork network, final FileStore fileStore) throws IOException {
        this(network, Collections.singletonList(fileStore));
    }

    /**
     * Creates a new Keras deep learning network port object. The given network is stored according to its
     * {@link DLPythonNetwork#getSource() source} type. Currently, {@link DLNetworkReferenceLocation} and
     * {@link DLNetworkFileStoreLocation} are the supported network source types:
     * <ul>
     * <li>{@link DLNetworkReferenceLocation}: The port object simply stores the reference to the network location.
     * Changes to the network location are not handled (i.e. the reference may simply become invalid).</li>
     * <li>{@link DLNetworkFileStoreLocation}: The port object shares the network's underlying file store. Changes to
     * the network file store are reflected by this port object.</li>
     * </ul>
     *
     * @param network the Keras deep learning network
     * @throws IllegalArgumentException if the network's source is not of a supported type supported
     * @throws IOException if failed to store the network
     */
    public DLKerasNetworkPortObject(final DLKerasNetwork network) throws IOException {
        this(network, getNetworkFileStore(network));
    }

    /**
     * Copies the given network to the first file store of the given file store list if that file store exists and the
     * network is not already stored there. If the network was copied, a new network instance is created that points to
     * the file store location.
     */
    private DLKerasNetworkPortObject(final DLKerasNetwork network, final List<FileStore> fileStores)
        throws IOException {
        super(fileStores);
        m_network = network;
        if (getFileStoreCount() > 0) {
            final URL fileStoreUrl = getFileStore(0).getFile().toURI().toURL();
            final URL networkUrl = m_network.getSource().getURI().toURL();
            if (!fileStoreUrl.equals(networkUrl)) {
                // Copy network to file store.
                flushToFileStoreInternal(m_network, getFileStore(0));
                try {
                    m_network = m_network.getSpec().create(new DLNetworkFileStoreLocation(getFileStore(0)));
                } catch (final DLInvalidSourceException e) {
                    throw new IOException(e.getMessage(), e);
                }
            }
        }
        m_content = new DLKerasMaterializedPortObjectContent(checkNotNull(m_network));
        m_spec = m_content.getSpec();
    }

    /**
     * Deserialization constructor.
     */
    private DLKerasNetworkPortObject(final DLKerasMaterializedPortObjectContent content) {
        m_content = content;
        m_spec = content.getSpec();
    }

    @Override
    protected void postConstruct() throws IOException {
        // Set network source if pointed to a file store.
        final DLNetworkLocation networkSource = amendNetworkSource();

        // Ensure backward compatibility in case we deserialized an outdated network spec that contains tensor specs
        // without a tensor id or a dimension order. See DLTensorSpec#getIdentifier().
        final DLKerasNetworkSpec spec = m_spec.getNetworkSpec();
        if (specIsOutdated(spec)) {
            // Network is outdated (3.5), bring to 3.6.
            final DLKerasNetwork network = upgradeNetwork(spec, networkSource);
            m_content.setNetwork(network);
            m_spec = new DLKerasNetworkPortObjectSpec(network.getSpec(), m_spec.getNetworkType());
        }
    }

    private boolean specIsOutdated(final DLKerasNetworkSpec spec) {
        return Stream.of(spec.getInputSpecs(), spec.getHiddenOutputSpecs(), spec.getOutputSpecs()).flatMap(Stream::of)
            .anyMatch(s -> s.getIdentifier() == null || s.getDimensionOrder() == null);
    }

    private DLKerasNetwork upgradeNetwork(final DLKerasNetworkSpec oldNetworkSpec,
        final DLNetworkLocation networkSource) throws IOException {
        // Reread network and rebuild spec.
        final DLPythonNetworkLoader<? extends DLKerasNetwork> loader =
            DLPythonNetworkLoaderRegistry.getInstance().getNetworkLoader(m_spec.getNetworkType()).orElseThrow(
                () -> new IllegalStateException("Keras back end '" + m_spec.getNetworkType().getCanonicalName()
                    + "' cannot be found. Are you missing a KNIME Deep Learning extension?"));
        try {
            return new DLPythonDefaultNetworkReader<>(loader).read(networkSource, true);
        } catch (DLInvalidSourceException | DLInvalidEnvironmentException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    protected DLKerasNetwork getNetworkInternal(final DLKerasNetworkPortObjectSpec spec)
        throws DLInvalidSourceException, IOException {
        amendNetworkSource();
        return m_content.getNetwork();
    }

    @Override
    protected void flushToFileStoreInternal(final DLKerasNetwork network, final FileStore fileStore)
        throws IOException {
        DLNetworkPortObject.copyFileToFileStore(network.getSource().getURI(), fileStore);
    }

    @Override
    protected void hashCodeInternal(final HashCodeBuilder b) {
        b.append(m_content);
    }

    @Override
    protected boolean equalsInternal(final DLNetworkPortObject other) {
        return Objects.equals(((DLKerasNetworkPortObject)other).m_content, m_content);
    }

    private DLNetworkLocation amendNetworkSource() {
        DLNetworkLocation networkSource = m_content.getNetworkSource();
        if (networkSource == null) {
            // Must be a file store location, otherwise the port object content deserializer would already have set the
            // network source.
            networkSource = new DLNetworkFileStoreLocation(getFileStore(0));
            m_content.setNetworkSource(networkSource);
        }
        return networkSource;
    }

    /**
     * Serializer of {@link DLKerasNetworkPortObject}.
     */
    public static final class Serializer extends PortObjectSerializer<DLKerasNetworkPortObject> {

        @Override
        public void savePortObject(final DLKerasNetworkPortObject portObject, final PortObjectZipOutputStream out,
            final ExecutionMonitor exec) throws IOException, CanceledExecutionException {
            out.putNextEntry(new ZipEntry(ZIP_ENTRY_NAME));
            final ObjectOutputStream objOut = new ObjectOutputStream(out);
            new DLKerasMaterializedPortObjectContent.Serializer().savePortObjectContent(portObject.m_content, objOut,
                exec);
            objOut.flush();
        }

        @Override
        public DLKerasNetworkPortObject loadPortObject(final PortObjectZipInputStream in, final PortObjectSpec spec,
            final ExecutionMonitor exec) throws IOException, CanceledExecutionException {
            final ZipEntry entry = in.getNextEntry();
            if (!ZIP_ENTRY_NAME.equals(entry.getName())) {
                throw new IOException("Failed to load Keras deep learning network port object. Invalid zip entry name '"
                    + entry.getName() + "', expected '" + ZIP_ENTRY_NAME + "'.");
            }
            final ObjectInputStream objIn = new ObjectInputStream(in);
            final DLKerasMaterializedPortObjectContent portObjectContent =
                new DLKerasMaterializedPortObjectContent.Serializer().loadPortObjectContent(objIn, spec, exec);
            return new DLKerasNetworkPortObject(portObjectContent);
        }
    }
}
