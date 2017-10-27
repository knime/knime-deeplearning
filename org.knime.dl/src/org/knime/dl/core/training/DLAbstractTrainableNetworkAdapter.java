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
package org.knime.dl.core.training;

import java.util.HashMap;
import java.util.Map;

import org.knime.dl.core.DLTensor;
import org.knime.dl.core.DLTensorFactory;
import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.core.data.DLWritableBuffer;
import org.knime.dl.core.execution.DLNetworkInputPreparer;

/**
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
public abstract class DLAbstractTrainableNetworkAdapter<N extends DLTrainableNetwork<?, ?>>
		implements DLTrainableNetworkAdapter {

	private final N m_network;

	private final DLTensorFactory m_layerDataFactory;

	private HashMap<DLTensorSpec, DLTensor<? extends DLWritableBuffer>> m_trainingData;

	private HashMap<DLTensorSpec, DLTensor<? extends DLWritableBuffer>> m_targetData;

	protected DLAbstractTrainableNetworkAdapter(final N network, final DLTensorFactory layerDataFactory) {
		m_network = network;
		m_layerDataFactory = layerDataFactory;
	}

	protected abstract Map<DLTensorSpec, ?> extractTrainingData(
			Map<DLTensorSpec, DLTensor<? extends DLWritableBuffer>> adapterInput);

	protected abstract Map<DLTensorSpec, ?> extractTargetData(
			Map<DLTensorSpec, DLTensor<? extends DLWritableBuffer>> adapterOutput);

	@Override
	public N getNetwork() {
		return m_network;
	}

	@Override
	public void train(final DLNetworkInputPreparer<DLTensor<? extends DLWritableBuffer>> trainingDataPreparer,
			final DLNetworkInputPreparer<DLTensor<? extends DLWritableBuffer>> testDataPreparer, final long batchSize)
			throws Exception {
		if (m_trainingData == null) {
			final DLTensorSpec[] inputSpecs = m_network.getSpec().getInputSpecs();
			m_trainingData = new HashMap<>(inputSpecs.length);
			for (final DLTensorSpec spec : inputSpecs) {
				m_trainingData.put(spec, m_layerDataFactory.createWritableTensor(spec, batchSize));
			}
			final DLTensorSpec[] outputSpecs = m_network.getSpec().getOutputSpecs();
			m_targetData = new HashMap<>(outputSpecs.length);
			for (final DLTensorSpec spec : outputSpecs) {
				m_targetData.put(spec, m_layerDataFactory.createWritableTensor(spec, batchSize));
			}
		}
		trainingDataPreparer.prepare(m_trainingData);
		testDataPreparer.prepare(m_targetData);
		trainInternal(batchSize);
		for (final DLTensor<?> training : m_trainingData.values()) {
			training.getBuffer().reset();
		}
		for (final DLTensor<?> target : m_targetData.values()) {
			target.getBuffer().reset();
		}
	}

	@Override
	public void close() throws Exception {
		m_network.close();
	}

	// TODO: type safety
	private <I, O> void trainInternal(final long batchSize) throws Exception {
		final DLTrainableNetwork<I, O> network = (DLTrainableNetwork<I, O>) m_network;
		final Map<DLTensorSpec, I> trainingData = (Map<DLTensorSpec, I>) extractTrainingData(m_trainingData);
		final Map<DLTensorSpec, O> targetData = (Map<DLTensorSpec, O>) extractTargetData(m_targetData);
		network.train(trainingData, targetData, batchSize);
	}
}
