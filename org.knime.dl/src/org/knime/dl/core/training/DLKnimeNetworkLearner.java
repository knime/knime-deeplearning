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
 * History
 *   Jul 3, 2017 (marcel): created
 */
package org.knime.dl.core.training;

import java.nio.BufferOverflowException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.knime.core.data.DataValue;
import org.knime.core.node.ExecutionContext;
import org.knime.dl.core.DLTensor;
import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.core.data.DLWritableBuffer;
import org.knime.dl.core.data.convert.DLDataValueToTensorConverter;
import org.knime.dl.core.data.convert.DLDataValueToTensorConverterFactory;
import org.knime.dl.core.execution.DLInvalidNetworkInputException;
import org.knime.dl.util.DLUtils;

/**
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
public class DLKnimeNetworkLearner implements AutoCloseable {

	private final DLTrainableNetworkAdapter m_network;

	private final Map<DLTensorSpec, DLDataValueToTensorConverter<DataValue, ?>> m_inputConverters;

	private final Map<DLTensorSpec, DLDataValueToTensorConverter<DataValue, ?>> m_outputConverters;

	@SuppressWarnings("unchecked")
	public DLKnimeNetworkLearner(final DLTrainableNetworkAdapter network,
			final Map<DLTensorSpec, DLDataValueToTensorConverterFactory<?, ?>> inputConverters,
			final Map<DLTensorSpec, DLDataValueToTensorConverterFactory<?, ?>> outputConverters) {
		m_network = network;
		m_inputConverters = new HashMap<>(inputConverters.size());
		for (final Entry<DLTensorSpec, DLDataValueToTensorConverterFactory<?, ?>> inputConverter : inputConverters
				.entrySet()) {
			m_inputConverters.put(inputConverter.getKey(),
					(DLDataValueToTensorConverter<DataValue, ?>) inputConverter.getValue().createConverter());
		}
		m_outputConverters = new HashMap<>(outputConverters.size());
		for (final Entry<DLTensorSpec, DLDataValueToTensorConverterFactory<?, ?>> outputConverter : outputConverters
				.entrySet()) {
			m_outputConverters.put(outputConverter.getKey(),
					(DLDataValueToTensorConverter<DataValue, ?>) outputConverter.getValue().createConverter());
		}
	}

	@SuppressWarnings("unchecked")
	public void train(final Map<DLTensorSpec, ? extends Iterable<DataValue>[]> trainingData,
			final Map<DLTensorSpec, ? extends Iterable<DataValue>[]> targetData, final ExecutionContext exec,
			final int batchSize) throws Exception {
		final int expectedBatchSize = m_network.getNetwork().getTrainingConfig().getBatchSize();
		m_network.train(training -> {
			for (final Entry<DLTensorSpec, ? extends Iterable<DataValue>[]> input : trainingData.entrySet()) {
				final Iterable<DataValue>[] batch = input.getValue();
				// buffer for single layer
				final DLTensor<?> converted = training.get(input.getKey());
				final DLDataValueToTensorConverter<DataValue, ?> inConverter = m_inputConverters.get(input.getKey());

				for (int i = 0; i < batch.length; i++) {
					try {
						inConverter.convert(batch[i], (DLTensor) converted);
					} catch (final BufferOverflowException ex) {
						final long shape = DLUtils.Shapes.getFixedSize(input.getKey().getShape())
								.orElseThrow(() -> new DLInvalidNetworkInputException(
										"Tensor spec does not provide a fully defined shape."));
						throw new DLInvalidNetworkInputException(
								"Input size did not match the expected input size of network input '"
										+ converted.getSpec().getName() + "'. Neuron count is " + shape
										+ ", batch size is " + expectedBatchSize + ". Thus, expected input size is "
										+ shape * expectedBatchSize + ". However, node input size was "
										+ converted.getBuffer().size()
										+ ". Please check the column selection for this input "
										+ "and validate the node's input data.",
								ex);
					}
				}
				// TODO: the cast to writable buffer should not be necessary here!
				if (converted.getBuffer().size() != ((DLWritableBuffer) converted.getBuffer()).getCapacity()) {
					final long shape = DLUtils.Shapes.getFixedSize(input.getKey().getShape())
							.orElseThrow(() -> new DLInvalidNetworkInputException(
									"Tensor spec does not provide a fully defined shape."));
					if (expectedBatchSize > batchSize) {
						// pad buffer if its only partially filled
						((DLWritableBuffer) converted.getBuffer()).setSize(shape * expectedBatchSize);
					} else if (converted.getBuffer().size() != shape * expectedBatchSize) {
						throw new DLInvalidNetworkInputException(
								"Input size did not match the expected input size of network input '"
										+ converted.getSpec().getName() + "'. Neuron count is " + shape
										+ ", batch size is " + expectedBatchSize + ". Thus, expected input size is "
										+ shape * expectedBatchSize + ". However, node input size was "
										+ converted.getBuffer().size()
										+ ". Please check the column selection for this input "
										+ "and validate the node's input data.");
					}
				}
			}
		}, target -> {
			for (final Entry<DLTensorSpec, ? extends Iterable<DataValue>[]> input : targetData.entrySet()) {
				final Iterable<DataValue>[] batch = input.getValue();
				// buffer for single layer
				final DLTensor<?> converted = target.get(input.getKey());
				final DLDataValueToTensorConverter<DataValue, ?> inConverter = m_outputConverters.get(input.getKey());

				for (int i = 0; i < batch.length; i++) {
					try {
						inConverter.convert(batch[i], (DLTensor) converted);
					} catch (final BufferOverflowException ex) {
						final long shape = DLUtils.Shapes.getFixedSize(input.getKey().getShape())
								.orElseThrow(() -> new DLInvalidNetworkInputException(
										"Tensor spec does not provide a fully defined shape."));
						throw new DLInvalidNetworkInputException(
								"Input size did not match the expected input size of network input '"
										+ converted.getSpec().getName() + "'. Neuron count is " + shape
										+ ", batch size is " + expectedBatchSize + ". Thus, expected input size is "
										+ shape * expectedBatchSize + ". However, node input size was "
										+ converted.getBuffer().size()
										+ ". Please check the column selection for this input "
										+ "and validate the node's input data.",
								ex);
					}
				}
				// TODO: the cast to writable buffer should not be necessary here!
				if (converted.getBuffer().size() != ((DLWritableBuffer) converted.getBuffer()).getCapacity()) {
					final long shape = DLUtils.Shapes.getFixedSize(input.getKey().getShape())
							.orElseThrow(() -> new DLInvalidNetworkInputException(
									"Tensor spec does not provide a fully defined shape."));
					if (expectedBatchSize > batchSize) {
						// pad buffer if its only partially filled
						((DLWritableBuffer) converted.getBuffer()).setSize(shape * expectedBatchSize);
					} else if (converted.getBuffer().size() != shape * expectedBatchSize) {
						throw new DLInvalidNetworkInputException(
								"Input size did not match the expected input size of network input '"
										+ converted.getSpec().getName() + "'. Neuron count is " + shape
										+ ", batch size is " + expectedBatchSize + ". Thus, expected input size is "
										+ shape * expectedBatchSize + ". However, node input size was "
										+ converted.getBuffer().size()
										+ ". Please check the column selection for this input "
										+ "and validate the node's input data.");
					}
				}
			}
		}, batchSize);
	}

	@Override
	public void close() throws Exception {
		m_network.close();
	}
}
