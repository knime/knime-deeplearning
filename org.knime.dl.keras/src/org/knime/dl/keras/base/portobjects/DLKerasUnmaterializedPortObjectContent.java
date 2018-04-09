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
import java.util.Collections;
import java.util.List;

import org.knime.core.data.filestore.FileStore;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.dl.core.DLInvalidSourceException;
import org.knime.dl.keras.core.DLKerasNetwork;
import org.knime.dl.keras.core.layers.DLInvalidInputSpecException;
import org.knime.dl.keras.core.layers.DLKerasLayer;
import org.knime.dl.keras.core.layers.DLKerasLayerGraphSerializer;
import org.knime.dl.keras.core.layers.DLKerasNetworkMaterializer;
import org.knime.dl.keras.core.layers.DLKerasNetworkSpecInferrer;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
final class DLKerasUnmaterializedPortObjectContent implements DLKerasPortObjectContent {

    private final List<DLKerasLayer> m_outputLayers;

    private DLKerasNetwork m_network;

    private DLKerasNetworkPortObjectSpecBase m_spec;

    public DLKerasUnmaterializedPortObjectContent(final List<DLKerasLayer> outputLayers) {
        m_outputLayers = outputLayers != null ? new ArrayList<>(outputLayers) : Collections.emptyList();
    }

    @Override
    public DLKerasNetwork getNetwork(final FileStore fileStore) throws DLInvalidSourceException, IOException {
        if (m_network == null) {
            try {
                m_network =
                    new DLKerasNetworkMaterializer().materialize(m_outputLayers, fileStore.getFile().toURI().toURL());
            } catch (final Exception e) {
                throw new IOException(
                    "An error occurred while creating the Keras network from its layer specifications. See log for details.",
                    e);
            }
        }
        return m_network;
    }

    @Override
    public DLKerasNetworkPortObjectSpecBase getSpec() {
        if (m_spec == null) {
            try {
                m_spec = new DLKerasNetworkPortObjectSpec(
                    new DLKerasNetworkSpecInferrer().inferNetworkSpec(m_outputLayers), DLKerasNetwork.class);
            } catch (final DLInvalidInputSpecException e) {
                throw new RuntimeException("An error occurred while creating the Keras network specification from"
                    + " its layer specifications. See log for details.", e);
            }
        }
        return m_spec;
    }

    public DLKerasMaterializedPortObjectContent materialize(final FileStore fileStore)
        throws DLInvalidSourceException, IOException {
        final DLKerasNetwork materialized = getNetwork(fileStore);
        return new DLKerasMaterializedPortObjectContent(materialized, false);
    }

    static final class Serializer {

        public void savePortObjectContent(final DLKerasUnmaterializedPortObjectContent portObjectContent,
            final ObjectOutputStream objOut, final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
            new DLKerasLayerGraphSerializer().writeGraphTo(portObjectContent.m_outputLayers, objOut);
        }

        public DLKerasUnmaterializedPortObjectContent loadPortObjectContent(final ObjectInputStream objIn,
            final PortObjectSpec spec, final ExecutionMonitor exec) throws IOException, CanceledExecutionException {
            try {
                final List<DLKerasLayer> outputLayers = new DLKerasLayerGraphSerializer().readGraphFrom(objIn);
                final DLKerasUnmaterializedPortObjectContent portObjectContent =
                    new DLKerasUnmaterializedPortObjectContent(outputLayers);
                portObjectContent.m_spec = (DLKerasNetworkPortObjectSpecBase)spec;
                return portObjectContent;
            } catch (final ClassNotFoundException e) {
                throw new IOException("Failed to load Keras deep learning network port object."
                    + " Are you missing a KNIME Deep Learning extension.", e);
            } catch (final Exception e) {
                throw new IOException("Failed to load Keras deep learning network port object. See log for details.",
                    e);
            }
        }
    }
}
