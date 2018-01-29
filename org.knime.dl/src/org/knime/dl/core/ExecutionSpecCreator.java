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

public class ExecutionSpecCreator {
	private final DLTensorFactory m_tensorFactory;
	private final long m_batchSize;
	private final DataRow m_row;
	private final FilterIndicesProvider m_filterIndicesProvider;

	public static Set<DLTensorSpec> createExecutionSpecs(final DataRow firstRow, final DLTensorFactory tensorFactory,
			final long batchSize, final Map<DLTensorId, int[]> columnsForTensorId,
			final Map<DLTensorSpec, DLDataValueToTensorConverterFactory<?, ?>> inputConverterFactories)
			throws DLMissingExtensionException {
		final LinkedHashSet<DLTensorSpec> executionInputSpecs = new LinkedHashSet<>(inputConverterFactories.size());
		final ExecutionSpecCreator specCreator = new ExecutionSpecCreator(tensorFactory, batchSize, firstRow,
				columnsForTensorId::get);
		for (final Entry<DLTensorSpec, DLDataValueToTensorConverterFactory<?, ?>> entry : inputConverterFactories
				.entrySet()) {
			executionInputSpecs.add(specCreator.createExecutionTensorSpec(entry.getKey(), entry.getValue()));
		}
		return executionInputSpecs;
	}

	private ExecutionSpecCreator(final DLTensorFactory tensorFactory, final long batchSize, final DataRow row,
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
