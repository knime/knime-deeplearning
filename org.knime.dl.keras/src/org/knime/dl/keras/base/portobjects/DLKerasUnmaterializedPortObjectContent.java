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
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.dl.core.DLNetworkLocation;
import org.knime.dl.keras.core.DLKerasNetwork;
import org.knime.dl.keras.core.layers.DLInvalidTensorSpecException;
import org.knime.dl.keras.core.layers.DLKerasLayer;
import org.knime.dl.keras.core.layers.DLKerasNetworkMaterializer;
import org.knime.dl.python.core.DLPythonContext;
import org.knime.dl.util.DLUtils;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
final class DLKerasUnmaterializedPortObjectContent implements DLKerasPortObjectContent {

    private final DLKerasUnmaterializedNetworkPortObjectSpec m_spec;

    DLKerasUnmaterializedPortObjectContent(final List<DLKerasLayer> outputLayers) throws DLInvalidTensorSpecException {
        this(new DLKerasUnmaterializedNetworkPortObjectSpec(outputLayers));
    }

    /**
     * Deserialization constructor.
     */
    private DLKerasUnmaterializedPortObjectContent(final DLKerasUnmaterializedNetworkPortObjectSpec spec) {
        m_spec = spec;
    }

    @Override
    public DLKerasUnmaterializedNetworkPortObjectSpec getSpec() {
        return m_spec;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(m_spec).toHashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != getClass()) {
            return false;
        }
        return Objects.equals(((DLKerasUnmaterializedPortObjectContent)obj).m_spec, m_spec);
    }

    public DLKerasMaterializedPortObjectContent materialize(final DLPythonContext context,
        final DLNetworkLocation saveLocation) throws IOException {
        try {
            final DLKerasNetwork materialized =
                new DLKerasNetworkMaterializer(m_spec.getOutputLayers(), saveLocation).materialize(context);
            return new DLKerasMaterializedPortObjectContent(materialized);
        } catch (final Exception e) { // NOSONAR
            final String message =
                "An error occurred while creating the Keras network from its layer specifications. Details:\n" +
                    DLUtils.Misc.findDisplayableErrorMessage(e).orElse(e.getMessage());
            throw new IOException(message, e);
        }
    }

    static final class Serializer {

        // No serialization needed.

        public DLKerasUnmaterializedPortObjectContent loadPortObjectContent(final PortObjectSpec spec) {
            return new DLKerasUnmaterializedPortObjectContent((DLKerasUnmaterializedNetworkPortObjectSpec)spec);
        }
    }
}
