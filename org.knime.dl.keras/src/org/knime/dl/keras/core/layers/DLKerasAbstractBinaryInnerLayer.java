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

import java.util.Collections;
import java.util.List;

import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.keras.core.struct.param.Parameter;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public abstract class DLKerasAbstractBinaryInnerLayer extends DLKerasAbstractInnerLayer implements DLKerasBinaryLayer {
    
    private static final int NUM_PARENTS = 2;

    // Don't ask 
    @Parameter(label = "First input tensor", min = "0")
    private DLTensorSpec m_spec1 = null;

    // Don't ask 
    @Parameter(label = "Second input tensor", min = "1")
    private DLTensorSpec m_spec2 = null;

    public DLKerasAbstractBinaryInnerLayer(final String kerasIdentifier) {
        super(kerasIdentifier, 2);
    }

    public DLKerasAbstractBinaryInnerLayer(final String kerasIdentifier, final DLKerasLayer firstParent,
        final DLKerasLayer secondParent) {
        super(kerasIdentifier, new DLKerasLayer[]{firstParent, secondParent});
    }
    
    @Override
    public String populateCall(List<String> inputTensors) {
        if (inputTensors.size() != NUM_PARENTS) {
            throw new IllegalArgumentException("A binary layer expects two input tensors");
        }
        return "[" + String.join(",", inputTensors);
    }

    @Override
    public DLTensorSpec getInputTensorSpec(int index) {
        if (index == 0) {
            return m_spec1;
        } else if (index == 1) {
            return m_spec2;
        }
        throw new IndexOutOfBoundsException();
    }

    // Convenience methods:

    protected abstract void validateInputSpec(Class<?> firstInputElementType, Class<?> secondInputElementType,
        Long[] firstInputShape, Long[] secondInputShape) throws DLInvalidTensorSpecException;

    protected abstract Long[] inferOutputShape(Long[] firstInputShape, Long[] secondInputShape);

    /**
     * The default behavior is to return <code>firstInputElementType</code>.
     */
    protected Class<?> inferOutputElementType(final Class<?> firstInputElementType,
        final Class<?> secondInputElementType) {
        return firstInputElementType;
    }

    @Override
    protected final void validateInputSpecs(final List<Class<?>> inputElementTypes, final List<Long[]> inputShapes)
        throws DLInvalidTensorSpecException {
        validateInputSpec(inputElementTypes.get(0), inputElementTypes.get(1), inputShapes.get(0), inputShapes.get(1));
    }

    @Override
    protected final List<Class<?>> inferOutputElementTypes(final List<Class<?>> inputElementTypes)
        throws DLInvalidTensorSpecException {
        return Collections.singletonList(inferOutputElementType(inputElementTypes.get(0), inputElementTypes.get(1)));
    }

    @Override
    protected final List<Long[]> inferOutputShapes(final List<Long[]> inputShape) {
        return Collections.singletonList(inferOutputShape(inputShape.get(0), inputShape.get(1)));
    }
}
