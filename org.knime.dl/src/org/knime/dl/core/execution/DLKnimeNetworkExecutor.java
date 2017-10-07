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
package org.knime.dl.core.execution;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataValue;
import org.knime.core.node.ExecutionContext;
import org.knime.dl.core.DLTensor;
import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.core.data.DLReadableBuffer;
import org.knime.dl.core.data.DLWritableBuffer;
import org.knime.dl.core.data.convert.DLDataValueToTensorConverter;
import org.knime.dl.core.data.convert.DLDataValueToTensorConverterFactory;
import org.knime.dl.core.data.convert.DLTensorToDataCellConverter;
import org.knime.dl.core.data.convert.DLTensorToDataCellConverterFactory;

/**
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
public class DLKnimeNetworkExecutor implements AutoCloseable {

	private final DLExecutableNetworkAdapter m_network;

	private final Map<DLTensorSpec, DLDataValueToTensorConverter<DataValue, ?>> m_inputConverters;

	private final Map<DLTensorSpec, DLTensorToDataCellConverter<?, DataCell>> m_outputConverters;

	@SuppressWarnings("unchecked")
	public DLKnimeNetworkExecutor(final DLExecutableNetworkAdapter network,
			final Map<DLTensorSpec, DLDataValueToTensorConverterFactory<?, ?>> inputConverters,
			final Map<DLTensorSpec, DLTensorToDataCellConverterFactory<?, ?>> outputConverters) {
		m_network = network;
		m_inputConverters = new HashMap<>(inputConverters.size());
		for (final Entry<DLTensorSpec, DLDataValueToTensorConverterFactory<?, ?>> inputConverter : inputConverters
				.entrySet()) {
			m_inputConverters.put(inputConverter.getKey(),
					(DLDataValueToTensorConverter<DataValue, ?>) inputConverter.getValue().createConverter());
		}
		m_outputConverters = new HashMap<>(outputConverters.size());
		for (final Entry<DLTensorSpec, DLTensorToDataCellConverterFactory<?, ?>> outputConverter : outputConverters
				.entrySet()) {
			m_outputConverters.put(outputConverter.getKey(),
					(DLTensorToDataCellConverter<?, DataCell>) outputConverter.getValue().createConverter());
		}
	}

	@SuppressWarnings("unchecked")
	public void execute(final Map<DLTensorSpec, ? extends Iterable<DataValue>[]> inputs,
			final Consumer<Map<DLTensorSpec, DataCell[][]>> outputConsumer, final ExecutionContext exec,
			final int batchSize) throws Exception {
		m_network.execute(in -> {
			// Filling network inputs
			for (final Entry<DLTensorSpec, ? extends Iterable<DataValue>[]> input : inputs.entrySet()) {
				final Iterable<DataValue>[] batch = input.getValue();
				// buffer for single layer
				final DLTensorBatch<?> converted = in.get(input.getKey());
				final DLDataValueToTensorConverter<DataValue, ?> inConverter = m_inputConverters.get(input.getKey());

				for (int i = 0; i < batch.length; i++) {
					final DLTensor buffer = converted.getBatch()[i];
					try {
						inConverter.convert(batch[i], buffer);
					} catch (final BufferOverflowException ex) {
						throw new DLInvalidNetworkInputException(
								"Node input size did not match neuron count of network input '"
										+ buffer.getSpec().getName() + "'. Node input exceeded the neuron count of "
										+ ((DLWritableBuffer) buffer.getBuffer()).getCapacity() + ".",
								ex);
					}
					// TODO: the cast to writable buffer should not be
					// necessary
					// here!
					if (buffer.getBuffer().size() != ((DLWritableBuffer) buffer.getBuffer()).getCapacity()) {
						throw new DLInvalidNetworkInputException(
								"Node input size did not match neuron count of network input '"
										+ buffer.getSpec().getName() + "'. Neuron count is "
										+ ((DLWritableBuffer) buffer.getBuffer()).getCapacity()
										+ ", node input size was " + buffer.getBuffer().size() + ".");
					}
				}
			}
		}, out -> {
			// TODO: we don't want to allocate a new map each time
			// output for each layerdataspec as a batch of rows (cells)
			final HashMap<DLTensorSpec, DataCell[][]> convertedOutput = new HashMap<>(out.size());
			for (final Entry<DLTensorSpec, DLTensorBatch<? extends DLReadableBuffer>> o : out.entrySet()) {
				// TODO move out of loop
				// array of rows. for each DL tensor we create a row.
				final DLTensorToDataCellConverter<?, DataCell> converter = m_outputConverters.get(o.getKey());
				convertedOutput.put(o.getKey(), new DataCell[batchSize][]);
				final ArrayList<DataCell>[] toCollect = new ArrayList[batchSize];
				for (int i = 0; i < batchSize; i++) {
					// thanks java
					try {
						final int j = i;
						toCollect[i] = new ArrayList<>();
						converter.convert(exec, (DLTensor) o.getValue().getBatch()[i], new Consumer<DataCell>() {

							@Override
							public void accept(final DataCell t) {
								toCollect[j].add(t);
							}
						});
					} catch (final BufferUnderflowException ex) {
						throw new DLInvalidNetworkOutputException("Unexpected network output. Size of network output '"
								+ o.getKey().getName() + "' did not match its specification.");
					} catch (final Exception e) {
						// TODO
						throw new RuntimeException(e);
					}
					// TODO
					convertedOutput.get(o.getKey())[i] = toCollect[i].toArray(new DataCell[toCollect[i].size()]);
				}
			}
			outputConsumer.accept(convertedOutput);
		}, batchSize);
	}


	@Override
	public void close() throws Exception {
		m_network.close();
	}
}
