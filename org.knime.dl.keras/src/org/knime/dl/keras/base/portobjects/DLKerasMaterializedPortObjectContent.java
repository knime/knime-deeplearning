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
import java.util.Objects;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.dl.core.DLInvalidSourceException;
import org.knime.dl.core.DLNetworkFileStoreLocation;
import org.knime.dl.core.DLNetworkLocation;
import org.knime.dl.core.DLNetworkReferenceLocation;
import org.knime.dl.keras.core.DLKerasNetwork;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
final class DLKerasMaterializedPortObjectContent implements DLKerasPortObjectContent {

    private final DLKerasNetworkPortObjectSpec m_spec;

    /**
     * Is <code>null</code> after deserialization or after a call to {@link #setNetworkSource(DLNetworkLocation)} set a
     * new network source until {@link #getNetwork()} is called for the first time.
     */
    private DLKerasNetwork m_network;

    /**
     * Is <code>null</code> after deserialization if it pointed to a file store in which case it must be populated by
     * clients via {@link #setNetworkSource(DLNetworkLocation)}.
     */
    private DLNetworkLocation m_networkSource;

    DLKerasMaterializedPortObjectContent(final DLKerasNetwork network) {
        m_spec = new DLKerasNetworkPortObjectSpec(network.getSpec(), network.getClass());
        m_network = network;
        m_networkSource = checkNotNull(network.getSource());
    }

    /**
     * Deserialization constructor.
     */
    private DLKerasMaterializedPortObjectContent(final DLKerasNetworkPortObjectSpec spec) {
        m_spec = spec;
    }

    @Override
    public DLKerasNetworkPortObjectSpec getSpec() {
        return m_spec;
    }

    DLKerasNetwork getNetwork() throws DLInvalidSourceException {
        if (m_network == null) {
            m_network = m_spec.getNetworkSpec().create(m_networkSource);
        }
        return m_network;
    }

    DLNetworkLocation getNetworkSource() {
        return m_networkSource;
    }

    void setNetworkSource(final DLNetworkLocation networkSource) {
        if (!networkSource.equals(m_networkSource)) {
            m_networkSource = networkSource;
            m_network = null;
        }
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(m_networkSource).toHashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != getClass()) {
            return false;
        }
        return Objects.equals(((DLKerasMaterializedPortObjectContent)obj).m_networkSource, m_networkSource);
    }

    static final class Serializer {

        public void savePortObjectContent(final DLKerasMaterializedPortObjectContent portObjectContent,
            final ObjectOutputStream objOut, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
            final DLNetworkLocation networkSource = portObjectContent.m_networkSource;
            if (networkSource instanceof DLNetworkReferenceLocation) {
                objOut.writeBoolean(true);
                objOut.writeObject(networkSource.getURI().toURL());
            } else if (networkSource instanceof DLNetworkFileStoreLocation) {
                objOut.writeBoolean(false);
            } else {
                throw new UnsupportedOperationException("Keras network source (" + networkSource
                    + ") is neither of type " + DLNetworkReferenceLocation.class.getCanonicalName()
                    + " nor of type " + DLNetworkFileStoreLocation.class.getCanonicalName()
                    + ". This is an implementation error.");
            }
        }

        public DLKerasMaterializedPortObjectContent loadPortObjectContent(final ObjectInputStream objIn,
            final PortObjectSpec spec, final ExecutionMonitor exec) throws IOException, CanceledExecutionException {
            final DLKerasMaterializedPortObjectContent portObjectContent =
                new DLKerasMaterializedPortObjectContent((DLKerasNetworkPortObjectSpec)spec);
            if (objIn.readBoolean()) {
                try {
                    portObjectContent.m_networkSource =
                        new DLNetworkReferenceLocation(((URL)objIn.readObject()).toURI());
                } catch (final ClassNotFoundException e) {
                    throw new IOException("Failed to load Keras deep learning network port object."
                        + " Are you missing a KNIME Deep Learning extension?", e);
                } catch (final Exception e) {
                    throw new IOException(
                        "Failed to load Keras deep learning network port object. See log for details.", e);
                }
            }
            return portObjectContent;
        }
    }
}
