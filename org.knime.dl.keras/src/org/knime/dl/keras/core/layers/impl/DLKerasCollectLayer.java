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
package org.knime.dl.keras.core.layers.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.NotImplementedException;
import org.knime.core.node.InvalidSettingsException;
import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.keras.core.layers.DLInvalidTensorSpecException;
import org.knime.dl.keras.core.layers.DLKerasAbstractInnerLayer;

/**
 * This layer collects the outputs of multiple Keras layers.
 * Its primary use case is the creation of networks with multiple outputs which is otherwise not possible.
 * It is not an actual Keras layer, which is why it has to be handled with special care during materialization.
 * Despite its behavioral similarity to parameter free merge layers, this layer implementation does not extend the
 * respective abstract class to highlight its special position within the layer framework.
 * 
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class DLKerasCollectLayer extends DLKerasAbstractInnerLayer {
    
    private static final int NUM_PARENTS = 2;

    // Don't ask 
//    @Parameter(label = "First input tensor", min = "0")
    private DLTensorSpec m_spec1 = null;

    // Don't ask 
//    @Parameter(label = "Second input tensor", min = "1")
    private DLTensorSpec m_spec2 = null;

    /**
     */
    public DLKerasCollectLayer() {
        super("collect (dummy identifier)", NUM_PARENTS);
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

    @Override
    public void setInputTensorSpec(int index, DLTensorSpec inputTensorSpec) {
        if (index == 0) {
            m_spec1 = inputTensorSpec;
        } else if (index == 1) {
            m_spec2 = inputTensorSpec;
        } else {
            throw new IndexOutOfBoundsException();
        }
    }

    @Override
    public String populateCall(List<String> inputTensors) {
        throw new NotImplementedException("The populateCall method of a collect layer should never be called. This is an implementation error.");
    }

    @Override
    public void validateParameters() throws InvalidSettingsException {
        // No parameters to validate
    }

    @Override
    protected void validateInputSpecs(List<Class<?>> inputElementTypes, List<Long[]> inputShapes)
        throws DLInvalidTensorSpecException {
        // this layer just collects and has no requirements on the inputs
        
    }

    @Override
    protected List<Class<?>> inferOutputElementTypes(List<Class<?>> inputElementTypes)
        throws DLInvalidTensorSpecException {
        return new ArrayList<>(inputElementTypes);
    }

    @Override
    protected List<Long[]> inferOutputShapes(List<Long[]> inputShape) {
        return new ArrayList<>(inputShape);
    }

    @Override
    protected void populateParameters(List<String> positionalParams, Map<String, String> namedParams) {
        // no parameters to populate
    }

}
