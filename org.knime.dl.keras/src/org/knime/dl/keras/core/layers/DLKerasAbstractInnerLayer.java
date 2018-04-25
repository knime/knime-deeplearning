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
package org.knime.dl.keras.core.layers;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayList;
import java.util.List;

import org.knime.dl.core.DLDefaultTensorId;
import org.knime.dl.core.DLDefaultTensorSpec;
import org.knime.dl.core.DLDimensionOrder;
import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.util.DLUtils;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public abstract class DLKerasAbstractInnerLayer extends DLKerasAbstractLayer implements DLKerasInnerLayer {

    protected static void checkInputSpec(final boolean expression, final String message)
        throws DLInvalidTensorSpecException {
        if (!expression) {
            throw new DLInvalidTensorSpecException("Invalid input specs. " + message);
        }
    }

    private final DLKerasTensorSpecsOutput[] m_parents;

    public DLKerasAbstractInnerLayer(final String kerasIdentifier, final int numParents) {
        super(kerasIdentifier);
        m_parents = new DLKerasTensorSpecsOutput[numParents];
    }

    public DLKerasAbstractInnerLayer(final String kerasIdentifier, final DLKerasTensorSpecsOutput[] parents) {
        super(kerasIdentifier);
        m_parents = checkNotNull(parents);
    }

    // Convenience methods:

    protected abstract void validateInputSpecs(List<Class<?>> inputElementTypes, List<Long[]> inputShapes)
        throws DLInvalidTensorSpecException;

    protected abstract List<Class<?>> inferOutputElementTypes(List<Class<?>> inputElementTypes)
        throws DLInvalidTensorSpecException;

    protected abstract List<Long[]> inferOutputShapes(List<Long[]> inputShape);

    @Override
    public int getNumParents() {
        return m_parents.length;
    }

    @Override
    public DLKerasTensorSpecsOutput getParent(final int index) {
        return m_parents[index];
    }

    @Override
    public void setParent(final int index, final DLKerasTensorSpecsOutput parent) {
        checkState(m_parents[index] == null);
        checkNotNull(parent);
        checkArgument(parent != this);
        m_parents[index] = parent;
    }

    @Override
    public final List<DLTensorSpec> getOutputSpecs() throws DLInvalidTensorSpecException {
        final DLInputSpecsHelperStruct inputSpecs = collectInputSpecs();
        validateInputSpecs(inputSpecs.m_elementTypes, inputSpecs.m_shapes);
        final List<Class<?>> outputElementTypes = inferOutputElementTypes(inputSpecs.m_elementTypes);
        final List<Long[]> outputShapes = inferOutputShapes(inputSpecs.m_shapes);
        final List<DLTensorSpec> outputSpecs = new ArrayList<>(outputShapes.size());
        for (int i = 0; i < outputShapes.size(); i++) {
            outputSpecs.add(DLDefaultTensorSpec.create(new DLDefaultTensorId("dummy"), "dummy", inputSpecs.m_batchSize,
                outputShapes.get(i), outputElementTypes.get(i), inputSpecs.m_dimensionOrder));
        }
        return outputSpecs;
    }

    @Override
    public final void validateInputSpecs() throws DLInvalidTensorSpecException {
        final DLInputSpecsHelperStruct inputSpecs = collectInputSpecs();
        validateInputSpecs(inputSpecs.m_elementTypes, inputSpecs.m_shapes);
    }

    @Override
    public boolean equalsIgnoreName(final DLKerasTensorSpecsOutput other) {
        if (other == this) {
            return true;
        }
        if (other == null || other.getClass() != getClass()) {
            return false;
        }
        final DLKerasAbstractInnerLayer otherInnerLayer = (DLKerasAbstractInnerLayer)other;
        if (otherInnerLayer.m_parents.length != m_parents.length) {
            return false;
        }
        if (!otherInnerLayer.getBackendRepresentation(null).equals(getBackendRepresentation(null))) {
            return false;
        }
        for (int i = 0; i < m_parents.length; i++) {
            if (!otherInnerLayer.m_parents[i].equalsIgnoreName(m_parents[i])) {
                return false;
            }
        }
        return true;
    }

    private DLInputSpecsHelperStruct collectInputSpecs() throws DLInvalidTensorSpecException {
        Long inputBatchSize = null;
        final List<Long[]> inputShapes = new ArrayList<>(m_parents.length);
        final List<Class<?>> inputElementTypes = new ArrayList<>(m_parents.length);
        DLDimensionOrder inputDimensionOrder = null;
        for (final DLKerasTensorSpecsOutput parent : m_parents) {
            final List<DLTensorSpec> parentOutputSpecs = parent.getOutputSpecs();
            for (final DLTensorSpec parentOutputSpec : parentOutputSpecs) {
                if (parentOutputSpec.getBatchSize().isPresent()) {
                    final long parentBatchSize = parentOutputSpec.getBatchSize().getAsLong();
                    if (inputBatchSize == null) {
                        inputBatchSize = parentBatchSize;
                    } else {
                        checkInputSpec(inputBatchSize == parentBatchSize,
                            "Batch sizes differ: " + inputBatchSize + " vs. " + parentBatchSize + ".");
                    }
                }
                inputShapes.add(DLUtils.Shapes.shapeToLongArray(parentOutputSpec.getShape()));
                inputElementTypes.add(parentOutputSpec.getElementType());
                final DLDimensionOrder parentDimensionOrder = parentOutputSpec.getDimensionOrder();
                if (inputDimensionOrder == null) {
                    inputDimensionOrder = parentDimensionOrder;
                } else {
                    // TODO: implement equals/hashCode/toString in DLDefaultDimensionOrder
                    checkInputSpec(inputDimensionOrder.equals(parentDimensionOrder),
                        "Dimension orders differ: " + inputDimensionOrder + " vs. " + parentDimensionOrder + ".");
                }
            }
        }
        return new DLInputSpecsHelperStruct(inputBatchSize, inputShapes, inputElementTypes, inputDimensionOrder);
    }

    private static final class DLInputSpecsHelperStruct {

        private final Long m_batchSize;

        private final List<Long[]> m_shapes;

        private final List<Class<?>> m_elementTypes;

        private final DLDimensionOrder m_dimensionOrder;

        private DLInputSpecsHelperStruct(final Long inputBatchSize, final List<Long[]> inputShapes,
            final List<Class<?>> inputElementTypes, final DLDimensionOrder inputDimensionOrder) {
            m_batchSize = inputBatchSize;
            m_shapes = inputShapes;
            m_elementTypes = inputElementTypes;
            m_dimensionOrder = inputDimensionOrder;
        }
    }
}
