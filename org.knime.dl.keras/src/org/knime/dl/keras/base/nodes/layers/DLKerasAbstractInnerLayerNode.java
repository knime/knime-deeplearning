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
package org.knime.dl.keras.base.nodes.layers;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.knime.core.node.InvalidSettingsException;
import org.knime.dl.core.DLInvalidSourceException;
import org.knime.dl.core.DLNetworkLocation;
import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.keras.base.portobjects.DLKerasNetworkPortObject;
import org.knime.dl.keras.base.portobjects.DLKerasNetworkPortObjectBase;
import org.knime.dl.keras.base.portobjects.DLKerasNetworkPortObjectSpec;
import org.knime.dl.keras.base.portobjects.DLKerasNetworkPortObjectSpecBase;
import org.knime.dl.keras.base.portobjects.DLKerasUnmaterializedNetworkPortObject;
import org.knime.dl.keras.base.portobjects.DLKerasUnmaterializedNetworkPortObjectSpec;
import org.knime.dl.keras.core.DLKerasNetworkSpec;
import org.knime.dl.keras.core.layers.DLInvalidTensorSpecException;
import org.knime.dl.keras.core.layers.DLKerasBaseNetworkTensorSpecOutput;
import org.knime.dl.keras.core.layers.DLKerasInnerLayer;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public abstract class DLKerasAbstractInnerLayerNode extends DLKerasAbstractLayerNode {

    protected DLKerasAbstractInnerLayerNode(final DLKerasInnerLayer layer) {
        super(layer);
    }

    protected void setLayerParent(final int index, final DLKerasNetworkPortObjectSpecBase parentPortObjectSpec)
        throws InvalidSettingsException {
        final DLKerasInnerLayer innerLayer = (DLKerasInnerLayer)m_layer;
        if (parentPortObjectSpec instanceof DLKerasNetworkPortObjectSpec) {
            // Append to existing network.
            if (parentPortObjectSpec.getNetworkSpec().getOutputSpecs().length != 1) {
                throw new InvalidSettingsException(
                    "Appending layers to networks with multiple outputs is not yet supported.");
            }
            final DLKerasDefaultBaseNetworkSpecTensorSpecOutput baseNetworkOutput =
                new DLKerasDefaultBaseNetworkSpecTensorSpecOutput(parentPortObjectSpec.getNetworkSpec(), 0);
            innerLayer.setParent(index, baseNetworkOutput);
        } else if (parentPortObjectSpec instanceof DLKerasUnmaterializedNetworkPortObjectSpec) {
            // Append to layer.
            innerLayer.setParent(index,
                ((DLKerasUnmaterializedNetworkPortObjectSpec)parentPortObjectSpec).getOutputLayer());
        } else {
            throw new InvalidSettingsException(
                "Input port object spec (" + parentPortObjectSpec.getClass().getCanonicalName()
                    + ") is neither of type " + DLKerasNetworkPortObjectSpec.class.getCanonicalName() + " nor of type "
                    + DLKerasUnmaterializedNetworkPortObjectSpec.class.getCanonicalName()
                    + ". This is an implementation error.");
        }
    }

    protected void amendBaseNetworkSource(final int index, final DLKerasNetworkPortObjectBase parentPortObject)
        throws InvalidSettingsException, DLInvalidSourceException, IOException {
        if (parentPortObject instanceof DLKerasNetworkPortObject) {
            final DLKerasDefaultBaseNetworkSpecTensorSpecOutput baseNetworkOutput =
                (DLKerasDefaultBaseNetworkSpecTensorSpecOutput)((DLKerasInnerLayer)m_layer).getParent(index);
            baseNetworkOutput.amendBaseNetworkSource(parentPortObject.getNetwork().getSource());
        } else if (parentPortObject instanceof DLKerasUnmaterializedNetworkPortObject) {
            // no op - there is no base network
        } else {
            throw new InvalidSettingsException("Input port object (" + parentPortObject.getClass().getCanonicalName()
                + ") is neither of type " + DLKerasNetworkPortObject.class.getCanonicalName() + " nor of type "
                + DLKerasUnmaterializedNetworkPortObject.class.getCanonicalName()
                + ". This is an implementation error.");
        }
    }

    @Override
    protected void validateLayer() throws InvalidSettingsException {
        super.validateLayer();
        ((DLKerasInnerLayer)m_layer).validateInputSpecs();
    }

    protected static final class DLKerasDefaultBaseNetworkSpecTensorSpecOutput
        implements DLKerasBaseNetworkTensorSpecOutput {

        private final DLKerasNetworkSpec m_baseNetworkSpec;

        private final int m_baseNetworkOutputIndex;

        private final List<DLTensorSpec> m_outputTensorSpec;

        private DLNetworkLocation m_baseNetworkSource;

        public DLKerasDefaultBaseNetworkSpecTensorSpecOutput(final DLKerasNetworkSpec baseNetworkSpec,
            final int baseNetworkOutputIndex) {
            m_baseNetworkSpec = baseNetworkSpec;
            m_baseNetworkOutputIndex = baseNetworkOutputIndex;
            m_outputTensorSpec = Collections.singletonList(baseNetworkSpec.getOutputSpecs()[baseNetworkOutputIndex]);
        }

        /**
         * @throws IllegalStateException if the base network source is already set
         * @throws NullPointerException if the argument is <code>null</code>
         */
        protected void amendBaseNetworkSource(final DLNetworkLocation baseNetworkSource) {
            checkState(m_baseNetworkSource == null);
            m_baseNetworkSource = checkNotNull(baseNetworkSource);
        }

        @Override
        public DLKerasNetworkSpec getBaseNetworkSpec() {
            return m_baseNetworkSpec;
        }

        @Override
        public DLNetworkLocation getBaseNetworkSource() {
            checkState(m_baseNetworkSource != null, "Base network source is not set. This is an implementation error.");
            return m_baseNetworkSource;
        }

        @Override
        public int getBaseNetworkOutputIndex() {
            return m_baseNetworkOutputIndex;
        }

        @Override
        public List<DLTensorSpec> getOutputSpecs() throws DLInvalidTensorSpecException {
            return m_outputTensorSpec;
        }

        @Override
        public int hashCode() {
            int result = 1;
            result = 31 * result + m_baseNetworkSpec.hashCode();
            result = 31 * result + m_baseNetworkOutputIndex;
            result = 31 * result + ((m_baseNetworkSource == null) ? 0 : m_baseNetworkSource.hashCode());
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
            final DLKerasDefaultBaseNetworkSpecTensorSpecOutput other =
                (DLKerasDefaultBaseNetworkSpecTensorSpecOutput)obj;
            if (other.m_baseNetworkOutputIndex != m_baseNetworkOutputIndex) {
                return false;
            }
            if (other.m_baseNetworkSource != null) {
                if (m_baseNetworkSource == null) {
                    return false;
                }
            } else if (!other.m_baseNetworkSource.equals(m_baseNetworkSource)) {
                return false;
            }
            if (!other.m_baseNetworkSpec.equals(m_baseNetworkSpec)) {
                return false;
            }
            return true;
        }
    }
}
