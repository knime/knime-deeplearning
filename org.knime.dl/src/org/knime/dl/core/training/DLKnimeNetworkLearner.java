/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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

import org.knime.core.data.DataCell;
import org.knime.core.data.DataValue;
import org.knime.core.node.ExecutionContext;
import org.knime.dl.core.DLLayerData;
import org.knime.dl.core.DLLayerDataSpec;
import org.knime.dl.core.data.DLWritableBuffer;
import org.knime.dl.core.data.convert.DLDataValueToLayerDataConverter;
import org.knime.dl.core.data.convert.DLDataValueToLayerDataConverterFactory;
import org.knime.dl.core.data.convert.DLLayerDataToDataCellConverter;
import org.knime.dl.core.data.convert.DLLayerDataToDataCellConverterFactory;
import org.knime.dl.core.execution.DLInvalidNetworkInputException;
import org.knime.dl.core.execution.DLLayerDataBatch;

/**
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
public class DLKnimeNetworkLearner implements AutoCloseable {

	private final DLTrainableNetworkAdapter m_network;

	private final Map<DLLayerDataSpec, DLDataValueToLayerDataConverter<DataValue, ?>> m_inputConverters;

	private final Map<DLLayerDataSpec, DLLayerDataToDataCellConverter<?, DataCell>> m_outputConverters;

	@SuppressWarnings("unchecked")
	public DLKnimeNetworkLearner(final DLTrainableNetworkAdapter network,
			final Map<DLLayerDataSpec, DLDataValueToLayerDataConverterFactory<?, ?>> inputConverters,
			final Map<DLLayerDataSpec, DLLayerDataToDataCellConverterFactory<?, ?>> outputConverters) {
		m_network = network;
		m_inputConverters = new HashMap<>(inputConverters.size());
		for (final Entry<DLLayerDataSpec, DLDataValueToLayerDataConverterFactory<?, ?>> inputConverter : inputConverters
				.entrySet()) {
			m_inputConverters.put(inputConverter.getKey(),
					(DLDataValueToLayerDataConverter<DataValue, ?>) inputConverter.getValue().createConverter());
		}
		m_outputConverters = new HashMap<>(outputConverters.size());
		for (final Entry<DLLayerDataSpec, DLLayerDataToDataCellConverterFactory<?, ?>> outputConverter : outputConverters
				.entrySet()) {
			m_outputConverters.put(outputConverter.getKey(),
					(DLLayerDataToDataCellConverter<?, DataCell>) outputConverter.getValue().createConverter());
		}
	}

	@SuppressWarnings("unchecked")
	public void train(final Map<DLLayerDataSpec, ? extends Iterable<DataValue>[]> trainingData,
			final Map<DLLayerDataSpec, ? extends Iterable<DataValue>[]> targetData, final ExecutionContext exec,
			final int batchSize) throws Exception {
		m_network.train(training -> {
			// Fill training data
			// TODO: these input population routines are shared by executor and learner, abstract!
			for (final Entry<DLLayerDataSpec, ? extends Iterable<DataValue>[]> trainingBatch : trainingData
					.entrySet()) {
				final Iterable<DataValue>[] batch = trainingBatch.getValue();
				// buffer for single layer
				final DLLayerDataBatch<?> converted = training.get(trainingBatch.getKey());
				final DLDataValueToLayerDataConverter<DataValue, ?> inConverter =
						m_inputConverters.get(trainingBatch.getKey());

				for (int i = 0; i < batch.length; i++) {
					final DLLayerData buffer = converted.getBatch()[i];
					try {
						inConverter.convert(batch[i], buffer);
					} catch (final BufferOverflowException ex) {
						throw new DLInvalidNetworkInputException(
								"Node input size did not match neuron count of network input '"
										+ buffer.getSpec().getName() + "'. Node input exceeded the neuron count of "
										+ ((DLWritableBuffer) buffer.getBuffer()).getCapacity() + ".",
								ex);
					}
					// TODO: the cast to writable buffer should not be necessary here!
					if (buffer.getBuffer().size() != ((DLWritableBuffer) buffer.getBuffer()).getCapacity()) {
						throw new DLInvalidNetworkInputException(
								"Node input size did not match neuron count of network input '"
										+ buffer.getSpec().getName() + "'. Neuron count is "
										+ ((DLWritableBuffer) buffer.getBuffer()).getCapacity()
										+ ", node input size was " + buffer.getBuffer().size() + ".");
					}
				}
			}
		}, target -> {
			// Fill target data
			// TODO: these input population routines are shared by executor and learner, abstract!
			for (final Entry<DLLayerDataSpec, ? extends Iterable<DataValue>[]> targetBatch : targetData.entrySet()) {
				final Iterable<DataValue>[] batch = targetBatch.getValue();
				// buffer for single layer
				final DLLayerDataBatch<?> converted = target.get(targetBatch.getKey());
				final DLDataValueToLayerDataConverter<DataValue, ?> inConverter =
						m_inputConverters.get(targetBatch.getKey());

				for (int i = 0; i < batch.length; i++) {
					final DLLayerData buffer = converted.getBatch()[i];
					try {
						inConverter.convert(batch[i], buffer);
					} catch (final BufferOverflowException ex) {
						throw new DLInvalidNetworkInputException(
								"Node input size did not match neuron count of network input '"
										+ buffer.getSpec().getName() + "'. Node input exceeded the neuron count of "
										+ ((DLWritableBuffer) buffer.getBuffer()).getCapacity() + ".",
								ex);
					}
					// TODO: the cast to writable buffer should not be necessary here!
					if (buffer.getBuffer().size() != ((DLWritableBuffer) buffer.getBuffer()).getCapacity()) {
						throw new DLInvalidNetworkInputException(
								"Node input size did not match neuron count of network input '"
										+ buffer.getSpec().getName() + "'. Neuron count is "
										+ ((DLWritableBuffer) buffer.getBuffer()).getCapacity()
										+ ", node input size was " + buffer.getBuffer().size() + ".");
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
