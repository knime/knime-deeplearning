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
 * History
 *   Dec 14, 2017 (adrian): created
 */
package org.knime.dl.core.data.convert;

import java.util.List;

import org.knime.core.data.DataValue;
import org.knime.core.node.util.CheckUtils;
import org.knime.dl.core.DLTensor;
import org.knime.dl.core.data.DLWritableBuffer;

/**
 * Handles shape inference on an abstract level. Note that we currently only allow single tensor data values to be
 * selected as input i.e. it is not possible to select multiple list columns.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public abstract class DLAbstractTensorDataValueToTensorConverter<FROM extends DataValue, VIA extends DLWritableBuffer>
		implements DLDataValueToTensorConverter<FROM, VIA> {

	protected static final String ERROR_MSG = "For lists and vectors only single column selection is allowed.";

	/**
	 * @param element an element of FROM
	 * @return the shape of the element
	 */
//	protected abstract long[] getShapeInternal(FROM element);

	protected abstract void convertInternal(FROM element, DLTensor<VIA> output);

//	@Override
//	public final long[] getShape(final List<? extends FROM> input) {
//		CheckUtils.checkArgument(input.size() == 1, ERROR_MSG);
//		return getShapeInternal(input.get(0));
//	}

	@Override
	public final void convert(final Iterable<? extends FROM> input, final DLTensor<VIA> output) {
		boolean isNotSingle = false;
		for (final FROM val : input) {
			if (isNotSingle) {
				throw new IllegalArgumentException(ERROR_MSG);
			}
			convertInternal(val, output);
			isNotSingle = true;
		}
	}
}