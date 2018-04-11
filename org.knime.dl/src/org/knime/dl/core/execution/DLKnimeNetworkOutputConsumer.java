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
package org.knime.dl.core.execution;

import java.lang.reflect.Array;
import java.nio.BufferUnderflowException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.append.AppendedColumnRow;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.filestore.FileStoreCell;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.streamable.RowOutput;
import org.knime.dl.core.DLInvalidNetworkOutputException;
import org.knime.dl.core.DLTensor;
import org.knime.dl.core.DLTensorId;
import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.core.data.DLReadableBuffer;
import org.knime.dl.core.data.convert.DLTensorToDataCellConverter;
import org.knime.dl.core.data.convert.DLTensorToDataCellConverterFactory;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public final class DLKnimeNetworkOutputConsumer implements DLNetworkOutputConsumer {

	private final RowOutput m_output;

	private final Supplier<DataRow> m_baseRows;

	private final boolean m_append;

	private final ExecutionContext m_exec;

    /**
     * The iteration order of this map determines the order in which cells are appended to each output row.
     */
    private final LinkedHashMap<DLTensorId, DLKnimeOutputConsumerHelperStruct> m_helpers;

    /**
     * <code>null</code> before the first call of {@link #accept(Map)}.
     */
    private DataCell[] m_temp;

    /**
     * @param append if true, the output cells created by this instance will be appended to their respective base rows.
     *            Otherwise new rows will be created which retain the row keys of their respective base rows.
     * @param converters the iteration order of this linked map determines the order in which cells are appended to each
     *            output row. Each key that is present in this map is expected to be present in the argument of
     *            {@link #accept(Map)}.
     * @param exec needed for the creation of {@link FileStoreCell file store cells}.
     */
    public DLKnimeNetworkOutputConsumer(final RowOutput output, final Supplier<DataRow> baseRows, final boolean append,
        final LinkedHashMap<DLTensorId, DLTensorToDataCellConverterFactory<?, ?>> converters,
        final ExecutionContext exec) {
		m_output = output;
		m_baseRows = baseRows;
		m_append = append;
		m_exec = exec;
		m_helpers = new LinkedHashMap<>(converters.size());
		for (final Entry<DLTensorId, DLTensorToDataCellConverterFactory<?, ?>> entry : converters.entrySet()) {
			final DLKnimeOutputConsumerHelperStruct helper = new DLKnimeOutputConsumerHelperStruct();
			helper.m_factory = entry.getValue();
			helper.m_converter = entry.getValue().createConverter();
			m_helpers.put(entry.getKey(), helper);
		}
	}

	@Override
	public void accept(final Map<DLTensorId, DLTensor<? extends DLReadableBuffer>> tensors) {
		if (m_temp == null) {
			// initialize output structs the first time we know how the network output looks like
			initialize(tensors);
		}
		for (final Entry<DLTensorId, DLKnimeOutputConsumerHelperStruct> entry : m_helpers.entrySet()) {
			final DLTensorId identifier = entry.getKey();
			final DLKnimeOutputConsumerHelperStruct helper = entry.getValue();
			final DLTensor<? extends DLReadableBuffer> tensor = tensors.get(identifier);
			try {
				// converter source type and tensor element type must match
				final DLTensorToDataCellConverter converter = helper.m_converter;
				converter.convert(tensor, helper.m_temp, m_exec);
			} catch (final BufferUnderflowException ex) {
				throw new DLInvalidNetworkOutputException("Unexpected network output. Size of network output '"
						+ tensor.getSpec().getName() + "' did not match its specification.");
			} catch (final Exception e) {
				throw new RuntimeException(e);
			}
		}
		// batch might be incomplete
		final DLTensor<? extends DLReadableBuffer> tensor = tensors.values().iterator().next();
		final long batchSize = tensor.getBuffer().size() / tensor.getExampleSize();
		for (int r = 0; r < batchSize; r++) {
			int c = 0;
			for (final DLTensorId identifier : tensors.keySet()) {
				final DLKnimeOutputConsumerHelperStruct helper = m_helpers.get(identifier);
				final DataCell[] temp = helper.m_temp;
				// casting is fine here as we are already performing exact multiplication in the initialize method
				final int o = (int) (r * helper.m_numOutputElements);
				for (int i = 0; i < helper.m_numOutputElements; i++) {
					m_temp[c + i] = temp[o + i];
				}
				c += helper.m_numOutputElements;
			}
			DataRow baseRow;
			try {
				baseRow = m_baseRows.get();
			} catch (final NoSuchElementException e) {
				// this should only occur in case of incomplete last batches and pre-defined batch size
				break;
			}
			try {
				if (m_append) {
					m_output.push(new AppendedColumnRow(baseRow, m_temp.clone()));
				} else {
					m_output.push(new DefaultRow(baseRow.getKey(), m_temp));
				}
			} catch (final InterruptedException ex) {
				Thread.currentThread().interrupt();
				return;
			}
		}
	}

	@Override
	public void close() throws Exception {
		m_output.close();
	}

	private void initialize(final Map<DLTensorId, DLTensor<? extends DLReadableBuffer>> tensors) {
		// must be present
		final long batchSize = tensors.values().iterator().next().getSpec().getBatchSize().getAsLong();
		if (batchSize > Integer.MAX_VALUE) {
			throw new IllegalArgumentException(
					"Batch size (" + batchSize + ") is larger than 2^31-1. This is currently not supported.");
		}
		long totalNumOutputElements = 0;
        for (final Entry<DLTensorId, DLKnimeOutputConsumerHelperStruct> entry : m_helpers.entrySet()) {
            final DLKnimeOutputConsumerHelperStruct helper = entry.getValue();
            final DLTensorSpec tensorSpec = tensors.get(entry.getKey()).getSpec();
			// must be present by now
			helper.m_numOutputElements = helper.m_factory.getDestCount(tensorSpec).getAsLong();
            if (helper.m_numOutputElements > Integer.MAX_VALUE) {
                throw new IllegalArgumentException(
                    "Number of output elements (" + helper.m_numOutputElements + ") of output '" + tensorSpec.getName()
                        + "' is larger than 2^31-1. This is currently not supported.");
            }
			try {
				helper.m_temp = (DataCell[]) Array.newInstance(helper.m_factory.getDestType().getCellClass(),
						Math.multiplyExact((int) batchSize, (int) helper.m_numOutputElements));
			} catch (final ArithmeticException e) {
                throw new IllegalArgumentException("Number of output elements of output '" + tensorSpec.getName()
                    + "' times batch size is larger than 2^31-1. This is currently not supported.", e);
			}
			totalNumOutputElements += helper.m_numOutputElements;
		}
		if (totalNumOutputElements > Integer.MAX_VALUE) {
			throw new IllegalArgumentException("Number of output elements (" + totalNumOutputElements
					+ ") is larger than 2^31-1. This is currently not supported.");
		}
		m_temp = new DataCell[(int) totalNumOutputElements];
	}

	private static final class DLKnimeOutputConsumerHelperStruct {

		private DLTensorToDataCellConverterFactory<?, ?> m_factory;

		private DLTensorToDataCellConverter<?, ?> m_converter;

		private long m_numOutputElements;

		private DataCell[] m_temp;
	}
}
