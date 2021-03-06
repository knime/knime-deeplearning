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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.knime.core.data.filestore.FileStore;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeView;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.dl.base.portobjects.DLNetworkPortObject;
import org.knime.dl.keras.base.nodes.layers.DLKerasAbstractBinaryLayerNodeFactory.DLKerasBinaryLayerNodeModel;
import org.knime.dl.keras.base.portobjects.DLKerasNetworkPortObjectBase;
import org.knime.dl.keras.base.portobjects.DLKerasNetworkPortObjectSpecBase;
import org.knime.dl.keras.base.portobjects.DLKerasUnmaterializedNetworkPortObject;
import org.knime.dl.keras.base.portobjects.DLKerasUnmaterializedNetworkPortObjectSpec;
import org.knime.dl.keras.core.DLKerasNetworkLoader;
import org.knime.dl.keras.core.layers.DLKerasBinaryLayer;
import org.knime.dl.keras.core.struct.param.ValidityException;

/**
 * Abstract implementation of a {@link NodeFactory} which can serve as the basis to create nodes based on
 * {@link DLKerasBinaryLayer}s.
 * 
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 * @param <T>
 */
public abstract class DLKerasAbstractBinaryLayerNodeFactory<T extends DLKerasBinaryLayer>
    extends NodeFactory<DLKerasBinaryLayerNodeModel<T>> {

    private Class<T> m_layerType;

    /**
     * @param layerType to wrap as a node.
     */
    protected DLKerasAbstractBinaryLayerNodeFactory(final Class<T> layerType) {
        m_layerType = layerType;
    }

    @Override
    public DLKerasBinaryLayerNodeModel<T> createNodeModel() {
        return new DLKerasBinaryLayerNodeModel<>(m_layerType);
    }

    @Override
    protected int getNrNodeViews() {
        return 0;
    }

    @Override
    protected boolean hasDialog() {
        return true;
    }

    @Override
    protected NodeDialogPane createNodeDialogPane() {
        try {
            return new DLKerasLayerNodeDialogPane<>(m_layerType);
        } catch (ValidityException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public NodeView<DLKerasBinaryLayerNodeModel<T>> createNodeView(int viewIndex,
        DLKerasBinaryLayerNodeModel<T> nodeModel) {
        return null;
    }

    static class DLKerasBinaryLayerNodeModel<T extends DLKerasBinaryLayer> extends DLKerasAbstractLayerNodeModel<T> {

        protected DLKerasBinaryLayerNodeModel(Class<T> layerType) {
            super(new PortType[]{DLKerasNetworkPortObjectBase.TYPE, DLKerasNetworkPortObjectBase.TYPE},
                new PortType[]{DLKerasNetworkPortObjectBase.TYPE}, layerType);
        }

        @Override
        protected PortObjectSpec[] configure(PortObjectSpec[] inSpecs) throws InvalidSettingsException {
            setLayerParent(m_layer, 0, (DLKerasNetworkPortObjectSpecBase)inSpecs[0]);
            setLayerParent(m_layer, 1, (DLKerasNetworkPortObjectSpecBase)inSpecs[1]);
            m_layer.validateInputSpecs();
            m_layer.validateParameters();
            return new PortObjectSpec[]{new DLKerasUnmaterializedNetworkPortObjectSpec(Arrays.asList(m_layer))};
        }

        @Override
        protected PortObject[] execute(PortObject[] inObjects, ExecutionContext exec) throws Exception {
            amendBaseNetworkSource(m_layer, 0, (DLKerasNetworkPortObjectBase)inObjects[0]);
            amendBaseNetworkSource(m_layer, 1, (DLKerasNetworkPortObjectBase)inObjects[1]);
            final FileStore fileStore =
                DLNetworkPortObject.createFileStoreForSaving(DLKerasNetworkLoader.SAVE_MODEL_URL_EXTENSION, exec);
            return new PortObject[]{new DLKerasUnmaterializedNetworkPortObject(Arrays.asList(m_layer), fileStore)};
        }

        @Override
        protected void validateSettings(NodeSettingsRO settings) throws InvalidSettingsException {
            // NB: Nothing to validate
        }

        @Override
        protected void loadInternals(File nodeInternDir, ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
            // NB: Nothing to load
        }

        @Override
        protected void saveInternals(File nodeInternDir, ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
            // NB: Nothing to save
        }

    }
}
