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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
package org.knime.dl.core.data.convert;

import java.util.List;

import org.knime.core.data.DataValue;
import org.knime.dl.core.data.DLWritableBuffer;

/**
 * Handles shape inference on an abstract level. Note that we currently only allow single tensor data values to be
 * selected as input i.e. it is not possible to select multiple list columns.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public abstract class DLAbstractTensorDataValueToTensorConverterFactory<I extends DataValue, O extends DLWritableBuffer>
		implements DLDataValueToTensorConverterFactory<I, O> {

	/**
	 * @return the shape of the element
	 */
	protected abstract long[] getDataShapeInternal(I element);

	/**
	 * @throws IllegalArgumentException if <code>input</code> is not a singleton
	 */
	@Override
	public final long[] getDataShape(final List<? extends DataValue> input) throws IllegalArgumentException {
		if (input.size() > 1) {
			throw new IllegalArgumentException(
					"For non-scalar data values, only single column selection is supported.");
		}
		final DataValue element = input.get(0);
		if (getSourceType().isInstance(element)) {
			throw new IllegalArgumentException("The provided values are not compatible with the converter.");
		}
		@SuppressWarnings("unchecked") // see instanceof check above
		final long[] shape = getDataShapeInternal((I) element);
		return shape;
	}
}
