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
package org.knime.dl.keras.core.layers.impl.merge;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.knime.core.node.InvalidSettingsException;
import org.knime.dl.keras.core.layers.DLInvalidTensorSpecException;
import org.knime.dl.keras.core.layers.DLKerasAbstractBinaryInnerLayer;
import org.knime.dl.keras.core.layers.DLKerasMergeLayer;
import org.knime.dl.keras.core.layers.DLLayerUtils;
import org.knime.dl.keras.core.layers.DLParameterValidationUtils;
import org.knime.dl.keras.core.layers.dialog.tuple.DLKerasTuple;
import org.knime.dl.keras.core.layers.dialog.tuple.DLKerasTuple.Constraint;
import org.knime.dl.keras.core.struct.param.Parameter;
import org.knime.dl.python.util.DLPythonUtils;

/**
 * Layer that computes a dot product between samples in two tensors.
 * 
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public class DLKerasDotLayer extends DLKerasAbstractBinaryInnerLayer implements DLKerasMergeLayer {

    @Parameter(label = "Axes")
    private DLKerasTuple m_axes = new DLKerasTuple("-1", 1, 2, EnumSet.of(Constraint.NEGATIVE, Constraint.ZERO));

    @Parameter(label = "Normalize")
    private boolean m_normalize = false;

    /**
     */
    public DLKerasDotLayer() {
        super("keras.layers.Dot");
    }

    @Override
    public void validateParameters() throws InvalidSettingsException {
    }

    @Override
    protected void validateInputSpec(Class<?> firstInputElementType, Class<?> secondInputElementType,
        Long[] firstInputShape, Long[] secondInputShape) throws DLInvalidTensorSpecException {
        int[] actualAxes = getAxes(firstInputShape.length, secondInputShape.length);
        checkInputSpec(
            DLParameterValidationUtils.dimensionsMatch(firstInputShape[actualAxes[0]], secondInputShape[actualAxes[1]]),
            "The axes along which to calculate the dot product must match.");
    }

    private int[] getAxes(int rank1, int rank2) {
        int[] actualAxes = new int[2];
        Long[] axes = m_axes.getTuple();
        actualAxes[0] = axes[0].intValue();
        actualAxes[1] = axes.length == 2 ? axes[1].intValue() : actualAxes[0];
        actualAxes[0] = DLLayerUtils.getAxisIndex(actualAxes[0], rank1);
        actualAxes[1] = DLLayerUtils.getAxisIndex(actualAxes[1], rank2);
        return actualAxes;
    }

    @Override
    protected Long[] inferOutputShape(Long[] firstInputShape, Long[] secondInputShape) {
        int[] axes = getAxes(firstInputShape.length, secondInputShape.length);
        List<Long> outShape =
            Stream.concat(filterDimension(axes[0], firstInputShape), filterDimension(axes[1], secondInputShape))
                .collect(Collectors.toList());
        if (outShape.isEmpty()) {
            outShape.add(1L);
        }
        return outShape.toArray(new Long[outShape.size()]);
    }

    private static Stream<Long> filterDimension(int filterIndex, Long[] shape) {
        return IntStream.range(0, shape.length).sequential().filter(i -> i != filterIndex).mapToObj(i -> shape[i]);
    }

    @Override
    protected void populateParameters(List<String> positionalParams, Map<String, String> namedParams) {
        positionalParams.add(DLPythonUtils.toPython(Arrays.stream(m_axes.getTuple())
            .mapToInt(d -> DLLayerUtils.exampleShapeIndexToBatchShapeIndex(d.intValue())).toArray()));
        namedParams.put("normalize", DLPythonUtils.toPython(m_normalize));
    }

}
