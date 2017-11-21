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
 *   Jul 3, 2017 (marcel): created
 */
package org.knime.dl.core.training;

import java.nio.BufferOverflowException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;

import org.knime.core.data.DataValue;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.NodeLogger;
import org.knime.dl.core.DLRowIterator;
import org.knime.dl.core.DLTensor;
import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.core.data.DLWritableBuffer;
import org.knime.dl.core.data.convert.DLDataValueToTensorConverter;
import org.knime.dl.core.data.convert.DLDataValueToTensorConverterFactory;
import org.knime.dl.core.execution.DLInvalidNetworkInputException;
import org.knime.dl.core.execution.DLNetworkInputPreparer;
import org.knime.dl.util.DLUtils;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public class DLKnimeNetworkLearner implements AutoCloseable {

	private static final NodeLogger LOGGER = NodeLogger.getLogger(DLKnimeNetworkLearner.class);

	private final DLTrainableNetworkAdapter m_network;

	private final Map<DLTensorSpec, DLDataValueToTensorConverter<DataValue, ?>> m_inputConverters;

	@SuppressWarnings("unchecked")
	public DLKnimeNetworkLearner(final DLTrainableNetworkAdapter network,
			final Map<DLTensorSpec, DLDataValueToTensorConverterFactory<?, ?>> inputConverters) {
		m_network = network;
		m_inputConverters = new HashMap<>(inputConverters.size());
		for (final Entry<DLTensorSpec, DLDataValueToTensorConverterFactory<?, ?>> converter : inputConverters
				.entrySet()) {
			m_inputConverters.put(converter.getKey(),
					(DLDataValueToTensorConverter<DataValue, ?>) converter.getValue().createConverter());
		}
	}

	public void train(final DLRowIterator inputIterator, final DLTrainingMonitor monitor) throws Exception {
		final long expectedBatchSize = m_network.getNetwork().getTrainingConfig().getBatchSize();
		final AtomicLong requestedBatchesCurrent = new AtomicLong();
		// FIXME: only valid if we don't crop the last batch
		final long requestedBatchesTotal = m_network.getNetwork().getTrainingConfig().getEpochs()
				* (long) Math.ceil(inputIterator.size() / (double) expectedBatchSize);
		m_network.train(new DLNetworkInputPreparer<DLTensor<? extends DLWritableBuffer>>() {

			@Override
			public long size() {
				return inputIterator.size();
			}

			@Override
			public void prepare(final Map<DLTensorSpec, DLTensor<? extends DLWritableBuffer>> inputTensors,
					final long batchIndex) throws CanceledExecutionException {
				// fill tensors (= batch) row by row of the input table
				final long batchOffset = batchIndex * expectedBatchSize;
				long rowIndex;
				for (rowIndex = 0; rowIndex < expectedBatchSize; rowIndex++) {
					final Map<DLTensorSpec, List<DataValue>> row = inputIterator.get(batchOffset + rowIndex);
					for (final Entry<DLTensorSpec, DLTensor<? extends DLWritableBuffer>> entry : inputTensors
							.entrySet()) {
						final DLTensorSpec tensorSpec = entry.getKey();
						final DLTensor<? extends DLWritableBuffer> tensor = entry.getValue();
						final DLDataValueToTensorConverter<DataValue, ?> converter = m_inputConverters.get(tensorSpec);
						try {
							converter.convert(row.get(tensorSpec), (DLTensor) tensor);
						} catch (final BufferOverflowException ex) {
							final long sampleSize = DLUtils.Shapes.getFixedSize(tensorSpec.getShape())
									.orElseThrow(() -> new DLInvalidNetworkInputException(
											"Tensor specification does not provide a fully defined shape. This is not supported, yet."));
							throw new DLInvalidNetworkInputException(
									"Node input size exceeds the expected size of network input/target '"
											+ tensor.getSpec().getName() + "'. Neuron count is " + sampleSize
											+ ", batch size is " + expectedBatchSize + ". Thus, expected input size is "
											+ sampleSize * expectedBatchSize
											+ ". Please check the column selection for this input "
											+ "and validate the node's input data.",
									ex);
						}
					}
					if (!inputIterator.hasNext()) {
						break;
					}
				}
				// check if tensors are entirely filled
				for (final Entry<DLTensorSpec, DLTensor<? extends DLWritableBuffer>> entry : inputTensors.entrySet()) {
					final DLTensorSpec tensorSpec = entry.getKey();
					final DLTensor<? extends DLWritableBuffer> tensor = entry.getValue();
					if (tensor.getBuffer().size() != tensor.getBuffer().getCapacity()) {
						final long sampleSize = DLUtils.Shapes.getFixedSize(tensorSpec.getShape())
								.orElseThrow(() -> new DLInvalidNetworkInputException("Tensor '" + tensorSpec.getName()
										+ "' does not provide a fully defined shape. This is not supported, yet."));
						if (rowIndex + 1 < expectedBatchSize) {
							// pad buffer if its only partially filled
							tensor.getBuffer().setSize(sampleSize * expectedBatchSize);
							LOGGER.warn("Zero-pad an input batch of network input/target '" + tensorSpec.getName()
									+ "' as its size is " + (rowIndex + 1) + " while the expected batch size is "
									+ expectedBatchSize
									+ ". This might lead to undesired training behavior depending on the network's semantics. "
									+ "You can avoid that by using an input table "
									+ "which number of rows is a multiple of the expected batch size.");
						} else if (tensor.getBuffer().size() % (sampleSize * expectedBatchSize) != 0) {
							throw new DLInvalidNetworkInputException(
									"Node input size does not match the expected size of network input/target '"
											+ tensor.getSpec().getName() + "'. Neuron count is " + sampleSize
											+ ", batch size is " + expectedBatchSize + ". Thus, expected input size is "
											+ sampleSize * expectedBatchSize + ". However, node input size is "
											+ tensor.getBuffer().size()
											+ ". Please check the column selection for this input "
											+ "and validate the node's input data.");
						}
					}
				}
				monitor.getExecutionContext().checkCanceled();
				monitor.getExecutionContext().setProgress(
						requestedBatchesCurrent.incrementAndGet() / (double) requestedBatchesTotal,
						"Processing batch number " + batchIndex + "...");
			}
		}, monitor);
	}

	@Override
	public void close() throws Exception {
		m_network.close();
	}
}
