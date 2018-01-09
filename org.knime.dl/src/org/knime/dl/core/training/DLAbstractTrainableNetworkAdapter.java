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
package org.knime.dl.core.training;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.knime.core.node.CanceledExecutionException;
import org.knime.dl.core.DLTensor;
import org.knime.dl.core.DLTensorFactory;
import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.core.data.DLWritableBuffer;
import org.knime.dl.core.execution.DLNetworkInputPreparer;
import org.knime.dl.core.execution.DLNetworkInputProvider;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public abstract class DLAbstractTrainableNetworkAdapter<N extends DLTrainableNetwork<?, ?>>
		implements DLTrainableNetworkAdapter {

	private final N m_network;

	private final DLTensorFactory m_layerDataFactory;

	private HashMap<DLTensorSpec, DLTensor<? extends DLWritableBuffer>> m_input;

	protected DLAbstractTrainableNetworkAdapter(final N network, final DLTensorFactory layerDataFactory) {
		m_network = network;
		m_layerDataFactory = layerDataFactory;
	}

	protected abstract Map<DLTensorSpec, ?> extractNetworkTensors(
			Map<DLTensorSpec, DLTensor<? extends DLWritableBuffer>> adapterInput);

	@Override
	public N getNetwork() {
		return m_network;
	}

	@Override
	public void train(final DLNetworkInputPreparer<DLTensor<? extends DLWritableBuffer>> inputPreparer,
			final DLTrainingMonitor monitor) throws Exception {
		if (m_input == null) {
			// pre-allocate network tensors, they will be filled by the given preparer and reset after each batch
			final long batchSize = m_network.getTrainingConfig().getBatchSize();
			final DLTensorSpec[] tensorSpecs = ArrayUtils.addAll(m_network.getSpec().getInputSpecs(),
					m_network.getSpec().getOutputSpecs());
			m_input = new HashMap<>(tensorSpecs.length);
			for (final DLTensorSpec spec : tensorSpecs) {
				m_input.put(spec, m_layerDataFactory.createWritableTensor(spec, batchSize));
			}
		}

		trainInternal(new DLNetworkInputProvider<DLTensor<? extends DLWritableBuffer>>() {

			@Override
			public long size() {
				return inputPreparer.size();
			}

			@Override
			public Map<DLTensorSpec, DLTensor<? extends DLWritableBuffer>> get(final long batchIndex)
					throws CanceledExecutionException {
				inputPreparer.prepare(m_input, batchIndex);
				return m_input;
			}
		}, monitor);
	}

	@Override
	public void close() throws Exception {
		m_network.close();
		if (m_input != null) {
			m_input.values().forEach(DLTensor::close);
		}
	}

	// TODO: type safety
	private <I, O> void trainInternal(final DLNetworkInputProvider<DLTensor<? extends DLWritableBuffer>> inputSupplier,
			final DLTrainingMonitor monitor) throws Exception {
		// HACK: just for poc
		((DLTrainableNetwork) m_network).train(inputSupplier, monitor);
	}
}