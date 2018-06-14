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

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;
import org.knime.dl.keras.core.layers.DLKerasRNNLayer;
import org.knime.dl.keras.core.struct.param.ValidityException;

/**
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <T> The type of {@link DLKerasRNNLayer}
 */
public abstract class DLKerasAbstractRNNLayerNodeFactory<T extends DLKerasRNNLayer>
    extends NodeFactory<DLKerasRNNLayerNodeModel<T>> {

    private final Class<T> m_layerType;

    private final int m_numHiddenStates;

    /**
     * @param layerType class object of the layer type
     * @param numHiddenStates the number of hidden states <b>layerType</b> has
     */
    public DLKerasAbstractRNNLayerNodeFactory(Class<T> layerType, int numHiddenStates) {
        m_layerType = layerType;
        m_numHiddenStates = numHiddenStates;
    }

    @Override
    public DLKerasRNNLayerNodeModel<T> createNodeModel() {
        return new DLKerasRNNLayerNodeModel<>(m_layerType, m_numHiddenStates);
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
    public NodeView<DLKerasRNNLayerNodeModel<T>> createNodeView(int viewIndex,
        DLKerasRNNLayerNodeModel<T> nodeModel) {
        return null;
    }
    
}
