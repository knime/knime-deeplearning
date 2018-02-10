/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
package org.knime.dl.core;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataValue;
import org.knime.dl.core.data.convert.DLDataValueToTensorConverterFactory;
import org.knime.dl.util.DLUtils;

/**
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public class DLExecutionSpecCreator {
	private final DLTensorFactory m_tensorFactory;
	private final long m_batchSize;
	private final DataRow m_row;
	private final FilterIndicesProvider m_filterIndicesProvider;

	public static Set<DLTensorSpec> createExecutionSpecs(final DataRow firstRow, final DLTensorFactory tensorFactory,
			final long batchSize, final Map<DLTensorId, int[]> columnsForTensorId,
			final Map<DLTensorSpec, DLDataValueToTensorConverterFactory<?, ?>> inputConverterFactories)
			throws DLMissingExtensionException {
		final LinkedHashSet<DLTensorSpec> executionInputSpecs = new LinkedHashSet<>(inputConverterFactories.size());
		final DLExecutionSpecCreator specCreator = new DLExecutionSpecCreator(tensorFactory, batchSize, firstRow,
				columnsForTensorId::get);
		for (final Entry<DLTensorSpec, DLDataValueToTensorConverterFactory<?, ?>> entry : inputConverterFactories
				.entrySet()) {
			executionInputSpecs.add(specCreator.createExecutionTensorSpec(entry.getKey(), entry.getValue()));
		}
		return executionInputSpecs;
	}

	private DLExecutionSpecCreator(final DLTensorFactory tensorFactory, final long batchSize, final DataRow row,
			final FilterIndicesProvider filterIndicesProvider) {
		m_tensorFactory = tensorFactory;
		m_batchSize = batchSize;
		m_row = row;
		m_filterIndicesProvider = filterIndicesProvider;
	}

	public DLTensorSpec createExecutionTensorSpec(final DLTensorSpec configureSpec,
			final DLDataValueToTensorConverterFactory<?, ?> converterFactory) throws DLMissingExtensionException {
		final long[] dataShape = converterFactory.getDataShape(getValuesForIndices(m_row,
				m_filterIndicesProvider.getFilterIndicesForTensor(configureSpec.getIdentifier())));
		final long[] executionShape = DLUtils.Shapes.calculateExecutionShape(configureSpec.getShape(), dataShape);
		return m_tensorFactory.createExecutionTensorSpec(configureSpec, m_batchSize, executionShape);
	}

	private List<? extends DataValue> getValuesForIndices(final DataRow row, final int[] indices) {
		return Arrays.stream(indices).mapToObj(row::getCell).collect(Collectors.toList());
	}

	private interface FilterIndicesProvider {
		public int[] getFilterIndicesForTensor(DLTensorId tensorId) throws DLMissingExtensionException;
	}
}
