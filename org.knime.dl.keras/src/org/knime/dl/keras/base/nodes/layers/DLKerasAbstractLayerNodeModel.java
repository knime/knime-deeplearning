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
import java.util.List;
import java.util.UUID;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortType;
import org.knime.dl.core.DLInvalidSourceException;
import org.knime.dl.core.DLNetworkSpec;
import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.keras.base.portobjects.DLKerasNetworkPortObject;
import org.knime.dl.keras.base.portobjects.DLKerasNetworkPortObjectBase;
import org.knime.dl.keras.base.portobjects.DLKerasNetworkPortObjectSpec;
import org.knime.dl.keras.base.portobjects.DLKerasNetworkPortObjectSpecBase;
import org.knime.dl.keras.base.portobjects.DLKerasUnmaterializedNetworkPortObject;
import org.knime.dl.keras.base.portobjects.DLKerasUnmaterializedNetworkPortObjectSpec;
import org.knime.dl.keras.core.layers.DLKerasDefaultBaseNetworkTensorSpecOutput;
import org.knime.dl.keras.core.layers.DLKerasInnerLayer;
import org.knime.dl.keras.core.layers.DLKerasLayer;
import org.knime.dl.keras.core.struct.Structs;
import org.knime.dl.keras.core.struct.access.MemberReadAccess;
import org.knime.dl.keras.core.struct.access.MemberWriteAccess;
import org.knime.dl.keras.core.struct.access.StructAccess;
import org.knime.dl.keras.core.struct.instance.MemberReadWriteInstance;
import org.knime.dl.keras.core.struct.instance.StructInstance;
import org.knime.dl.keras.core.struct.instance.StructInstances;
import org.knime.dl.keras.core.struct.nodesettings.NodeSettingsStructs;
import org.knime.dl.keras.core.struct.param.ParameterStructs;
import org.knime.dl.util.DLUtils;

/**
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
abstract class DLKerasAbstractLayerNodeModel<T extends DLKerasLayer> extends NodeModel {

    protected final T m_layer;

    private final StructAccess<MemberReadAccess<?, NodeSettingsRO>> m_settingsRO;

    private final StructAccess<MemberWriteAccess<?, NodeSettingsWO>> m_settingsWO;

    protected final StructInstance<MemberReadWriteInstance<?>, ?> m_instance;

    protected DLKerasAbstractLayerNodeModel(PortType[] in, PortType[] out, Class<T> layerType) {
        super(in, out);
        try {
            m_layer = layerType.newInstance();
            m_layer.setRuntimeId(UUID.randomUUID().toString());
            m_instance =
                StructInstances.createReadWriteInstance(m_layer, ParameterStructs.createStructAccess(layerType));
            m_settingsRO = NodeSettingsStructs.createStructROAccess(m_instance.struct());
            m_settingsWO = NodeSettingsStructs.createStructWOAccess(m_instance.struct());
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalArgumentException("Can't create layer " + layerType.getName()
                + ". Most likely empty constructor in layer implementation is missing.", e);
        }
    }

    @Override
    protected void saveSettingsTo(NodeSettingsWO settings) {
        // TODO avoid recreating of settingsStructInstance
        try {
            Structs.shallowCopyUnsafe(m_instance, StructInstances.createWriteInstance(settings, m_settingsWO));
        } catch (InvalidSettingsException e) {
            e.printStackTrace();
            // NB: Shouldn't ever happen
        }
    }

    void amendBaseNetworkSource(final DLKerasInnerLayer layer, final int index,
        final DLKerasNetworkPortObjectBase parentPortObject)
        throws InvalidSettingsException, DLInvalidSourceException, IOException {
        if (parentPortObject instanceof DLKerasNetworkPortObject) {
            final DLKerasDefaultBaseNetworkTensorSpecOutput baseNetworkOutput =
                (DLKerasDefaultBaseNetworkTensorSpecOutput)layer.getParent(index);
            baseNetworkOutput.setBaseNetworkSource(parentPortObject.getNetwork().getSource());
        } else if (parentPortObject instanceof DLKerasUnmaterializedNetworkPortObject) {
            // no op - there is no base network
        } else {
            throw new InvalidSettingsException("Input port object (" + parentPortObject.getClass().getCanonicalName()
                + ") is neither of type " + DLKerasNetworkPortObject.class.getCanonicalName() + " nor of type "
                + DLKerasUnmaterializedNetworkPortObject.class.getCanonicalName()
                + ". This is an implementation error.");
        }
    }

    void setLayerParent(final DLKerasInnerLayer layer, final int parentIndex,
        final DLKerasNetworkPortObjectSpecBase parentPortObjectSpec) throws InvalidSettingsException {
        final DLKerasInnerLayer innerLayer = layer;

        // If the specified inner layer does not have an input tensor spec at the specified index, we 
        // set the first tensor spec as default
        DLTensorSpec inputSpec = innerLayer.getInputTensorSpec(parentIndex);
        DLNetworkSpec networkSpec = parentPortObjectSpec.getNetworkSpec();
        if (inputSpec == null
            || !DLUtils.Networks.findTensorSpecIndexById(inputSpec, networkSpec.getOutputSpecs()).isPresent()) {
            inputSpec = networkSpec.getOutputSpecs()[0];
            innerLayer.setInputTensorSpec(parentIndex, inputSpec);
        }

        int indexInParent =
            DLUtils.Networks.findTensorSpecIndexById(inputSpec, networkSpec.getOutputSpecs()).orElseThrow(
                () -> new IllegalStateException("The set tensor spec can't be found in the parent's output specs."));
        innerLayer.setTensorIndexInParent(parentIndex, indexInParent);

        if (parentPortObjectSpec instanceof DLKerasNetworkPortObjectSpec) {
            appendToExistingNetwork(parentIndex, parentPortObjectSpec, innerLayer, indexInParent);
        } else if (parentPortObjectSpec instanceof DLKerasUnmaterializedNetworkPortObjectSpec) {
            appendToLayer(parentIndex, parentPortObjectSpec, innerLayer);
        } else {
            throw new InvalidSettingsException(
                "Input port object spec (" + parentPortObjectSpec.getClass().getCanonicalName()
                    + ") is neither of type " + DLKerasNetworkPortObjectSpec.class.getCanonicalName() + " nor of type "
                    + DLKerasUnmaterializedNetworkPortObjectSpec.class.getCanonicalName()
                    + ". This is an implementation error.");
        }
    }

    private static void appendToLayer(final int parentIndex,
        final DLKerasNetworkPortObjectSpecBase parentPortObjectSpec, final DLKerasInnerLayer innerLayer)
        throws InvalidSettingsException {
        final List<DLKerasLayer> outputLayers =
            ((DLKerasUnmaterializedNetworkPortObjectSpec)parentPortObjectSpec).getOutputLayers();
        if (outputLayers.size() > 1) {
            throw new InvalidSettingsException(
                "Appending a layer to a layer of a list of layers is not yet supported.");
        }
        innerLayer.setParent(parentIndex, outputLayers.get(0));
    }

    private static void appendToExistingNetwork(final int parentIndex,
        final DLKerasNetworkPortObjectSpecBase parentPortObjectSpec, final DLKerasInnerLayer innerLayer,
        final int indexInParent) {
        final DLKerasDefaultBaseNetworkTensorSpecOutput baseNetworkOutput =
            new DLKerasDefaultBaseNetworkTensorSpecOutput(parentPortObjectSpec.getNetworkSpec(), indexInParent);
        innerLayer.setParent(parentIndex, baseNetworkOutput);
    }

    @Override
    protected void loadValidatedSettingsFrom(NodeSettingsRO settings) throws InvalidSettingsException {
        Structs.shallowCopyUnsafe(StructInstances.createReadInstance(settings, m_settingsRO), m_instance);
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

    @Override
    protected final void reset() {
        m_layer.setRuntimeId(UUID.randomUUID().toString());
    }
}