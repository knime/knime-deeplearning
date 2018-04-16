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
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.knime.core.node.port.PortObjectSpecZipInputStream;
import org.knime.core.node.port.PortObjectSpecZipOutputStream;
import org.knime.dl.base.portobjects.DLAbstractNetworkPortObjectSpec;
import org.knime.dl.keras.core.DLKerasNetwork;
import org.knime.dl.keras.core.DLKerasNetworkSpec;
import org.knime.dl.keras.core.layers.DLInvalidTensorSpecException;
import org.knime.dl.keras.core.layers.DLKerasLayer;
import org.knime.dl.keras.core.layers.DLKerasNetworkLayerGraphSerializer;
import org.knime.dl.keras.core.layers.DLKerasNetworkSpecInferrer;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public final class DLKerasUnmaterializedNetworkPortObjectSpec
    extends DLAbstractNetworkPortObjectSpec<DLKerasNetworkSpec> implements DLKerasNetworkPortObjectSpecBase {

    private static final String ZIP_ENTRY_NAME = "DLKerasUnmaterializedNetworkPortObjectSpec";

    private final List<DLKerasLayer> m_outputLayers;

    public DLKerasUnmaterializedNetworkPortObjectSpec(final DLKerasLayer outputLayer)
        throws DLInvalidTensorSpecException {
        super(new DLKerasNetworkSpecInferrer().inferNetworkSpec(Collections.singletonList(checkNotNull(outputLayer))),
            DLKerasNetwork.class);
        m_outputLayers = Collections.singletonList(outputLayer);
    }

    @Override
    @SuppressWarnings("unchecked") // ensured by the constructor of this class
    public Class<? extends DLKerasNetwork> getNetworkType() {
        return (Class<? extends DLKerasNetwork>)super.getNetworkType();
    }

    public DLKerasLayer getOutputLayer() {
        return m_outputLayers.get(0);
    }

    @Override
    protected void hashCodeInternal(final HashCodeBuilder b) {
        // no op - everything's handled in abstract base class. Output layers do not matter here.
    }

    @Override
    protected boolean equalsInternal(final org.knime.dl.base.portobjects.DLNetworkPortObjectSpec other) {
        // no op - everything's handled in abstract base class. Output layers do not matter here.
        return true;
    }

    /**
     * Serializer of {@link DLKerasUnmaterializedNetworkPortObjectSpec}.
     */
    public static final class Serializer extends PortObjectSpecSerializer<DLKerasUnmaterializedNetworkPortObjectSpec> {
        @Override
        public void savePortObjectSpec(final DLKerasUnmaterializedNetworkPortObjectSpec portObjectSpec,
            final PortObjectSpecZipOutputStream out) throws IOException {
            out.putNextEntry(new ZipEntry(ZIP_ENTRY_NAME));
            final ObjectOutputStream objOut = new ObjectOutputStream(out);
            try {
                new DLKerasNetworkLayerGraphSerializer().writeGraphTo(portObjectSpec.m_outputLayers, objOut);
            } catch (final Exception e) {
                throw new IOException(
                    "Failed to save Keras deep learning network port object spec. See log for details.", e);
            }
        }

        @Override
        public DLKerasUnmaterializedNetworkPortObjectSpec loadPortObjectSpec(final PortObjectSpecZipInputStream in)
            throws IOException {
            final ZipEntry entry = in.getNextEntry();
            if (!ZIP_ENTRY_NAME.equals(entry.getName())) {
                throw new IOException("Failed to load Keras deep learning network. Invalid zip entry name '"
                    + entry.getName() + "', expected '" + ZIP_ENTRY_NAME + "'.");
            }
            final ObjectInputStream objIn = new ObjectInputStream(in);
            try {
                final List<DLKerasLayer> outputLayers = new DLKerasNetworkLayerGraphSerializer().readGraphFrom(objIn);
                return new DLKerasUnmaterializedNetworkPortObjectSpec(outputLayers.get(0));
            } catch (final ClassNotFoundException e) {
                throw new IOException("Failed to load Keras deep learning network port object spec."
                    + " Are you missing a KNIME Deep Learning extension?", e);
            } catch (final Exception e) {
                throw new IOException(
                    "Failed to load Keras deep learning network port object spec. See log for details", e);
            }
        }
    }
}
