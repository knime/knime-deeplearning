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
package org.knime.dl.keras.core.layers.impl.core;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import org.knime.core.node.InvalidSettingsException;
import org.knime.dl.core.DLDefaultTensorId;
import org.knime.dl.core.DLDefaultTensorSpec;
import org.knime.dl.core.DLDimensionOrder;
import org.knime.dl.core.DLTensorShape;
import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.keras.core.layers.DLKerasAbstractLayer;
import org.knime.dl.keras.core.layers.DLKerasDataType;
import org.knime.dl.keras.core.layers.DLKerasInputLayer;
import org.knime.dl.keras.core.layers.DLKerasTensorSpecsOutput;
import org.knime.dl.keras.core.layers.dialog.tuple.DLKerasTuple;
import org.knime.dl.keras.core.layers.dialog.tuple.DLKerasTuple.Constraint;
import org.knime.dl.keras.core.struct.param.Parameter;
import org.knime.dl.python.core.DLPythonNumPyTypeMap;
import org.knime.dl.python.util.DLPythonUtils;
import org.knime.dl.util.DLUtils;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public final class DLKerasDefaultInputLayer extends DLKerasAbstractLayer implements DLKerasInputLayer {

    @Parameter(label = "Shape")
    private DLKerasTuple m_shape = new DLKerasTuple("1", 1, Integer.MAX_VALUE, EnumSet.of(Constraint.PARTIAL));

    @Parameter(label = "Batch Size", min = "0", required = false)
    private Integer m_batchSize = null;

    // TODO: Fetch available types from DLPythonNumPyTypeMap via supplier.
    @Parameter(label = "Data Type")
    private DLKerasDataType m_dataType = DLKerasDataType.FLOAT_32;

    /**
     * Constructor
     */
    public DLKerasDefaultInputLayer() {
        super("keras.layers.Input");
    }

    @Override
    public List<DLTensorSpec> getOutputSpecs() {
        return Arrays.asList(createTensorSpec());
    }

    private DLDefaultTensorSpec createTensorSpec() {
        final DLTensorShape shape = DLUtils.Shapes.shapeFromLongArray(getShape());
        final Class<?> elementType = DLPythonNumPyTypeMap.INSTANCE.getPreferredInternalType(m_dataType.value());
        final DLDimensionOrder dimensionOrder = DLDimensionOrder.TDHWC;
        String name = "dummy";
        DLDefaultTensorId id = new DLDefaultTensorId(name);
        if (m_batchSize == null) {
            return new DLDefaultTensorSpec(id, name, shape, elementType, dimensionOrder);
        }
        return new DLDefaultTensorSpec(id, name, m_batchSize, shape, elementType, dimensionOrder);
    }

    @Override
    public void validateParameters() throws InvalidSettingsException {
        // NB: Nothing to do
    }

    @Override
    public boolean equalsIgnoreName(final DLKerasTensorSpecsOutput other) {
        if (other == this) {
            return true;
        }
        if (other == null || other.getClass() != getClass()) {
            return false;
        }
        final DLKerasDefaultInputLayer otherInputLayer = (DLKerasDefaultInputLayer)other;
        return otherInputLayer.getBackendRepresentation(null).equals(getBackendRepresentation(null));
    }

    @Override
    protected void populateParameters(final List<String> positionalParams, final Map<String, String> namedParams) {
        final String[] shape = Arrays.stream(getShape())
            .map(l -> l != null ? DLPythonUtils.toPython(l) : DLPythonUtils.NONE).toArray(l -> new String[l]);
        namedParams.put("batch_shape",
            "(" + DLPythonUtils.toPython(m_batchSize == null ? 32 : m_batchSize) + "," + String.join(",", shape) + ")");
        namedParams.put("dtype", DLPythonUtils.toPython(m_dataType.value()));
    }

    private Long[] getShape() {
        return m_shape.getTuple();
    }
}
