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

import java.util.List;

import org.knime.dl.core.DLTensorSpec;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public interface DLKerasInnerLayer extends DLKerasLayer {

    /**
     * @return number of parents (=input tensors)
     */
    int getNumParents();

    /**
     * @throws IndexOutOfBoundsException if <code>index</code> is negative or greater than or equal to
     *             {@link #getNumParents()}
     * @return may be <code>null</code> if the parent at the given index is not yet set
     */
    DLTensorSpec getInputTensorSpec(int index);

    /**
     * @throws IndexOutOfBoundsException if <code>index</code> is negative or greater than or equal to
     *             {@link #getNumParents()}
     * @return may be <code>null</code> if the parent at the given index is not yet set
     */
    DLKerasTensorSpecsOutput getParent(int index);

    /**
     * @throws NullPointerException if <code>parent</code> is <code>null</code>
     * @throws IllegalArgumentException if <code>parent</code> is this instance
     * @throws IndexOutOfBoundsException if <code>index</code> is negative or greater than or equal to
     *             {@link #getNumParents()}
     */
    void setParent(int index, DLKerasTensorSpecsOutput parent);

    void validateInputSpecs() throws DLInvalidTensorSpecException;
    
    /**
     * Populates the parameters of the __call__ method of this layer.
     * Important note: Do not surround the returned String with parenthesis unless the call function expects
     * a tuple as input.
     * 
     * @param inputTensors the python variable names of the input tensors
     * @return a String of python code providing the arguments for the __call__ method of this layer
     */
    String populateCall(List<String> inputTensors);
}
