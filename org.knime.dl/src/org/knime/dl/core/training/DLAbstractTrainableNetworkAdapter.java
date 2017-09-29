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
 */
package org.knime.dl.core.training;

import java.util.HashMap;
import java.util.Map;

import org.knime.dl.core.DLLayerData;
import org.knime.dl.core.DLLayerDataFactory;
import org.knime.dl.core.DLLayerDataSpec;
import org.knime.dl.core.data.DLWritableBuffer;
import org.knime.dl.core.execution.DLLayerDataBatch;
import org.knime.dl.core.execution.DLNetworkInputPreparer;

/**
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
public abstract class DLAbstractTrainableNetworkAdapter<N extends DLTrainableNetwork<?, ?, ?, ?>>
		implements DLTrainableNetworkAdapter {

	private final N m_network;

	private final DLLayerDataFactory m_layerDataFactory;

	private HashMap<DLLayerDataSpec, DLLayerDataBatch<? extends DLWritableBuffer>> m_trainingData;

	private HashMap<DLLayerDataSpec, DLLayerDataBatch<? extends DLWritableBuffer>> m_targetData;

	protected DLAbstractTrainableNetworkAdapter(final N network, final DLLayerDataFactory layerDataFactory) {
		m_network = network;
		m_layerDataFactory = layerDataFactory;
	}

	protected abstract Map<DLLayerDataSpec, ?> extractTrainingData(
			Map<DLLayerDataSpec, DLLayerDataBatch<? extends DLWritableBuffer>> adapterInput);

	protected abstract Map<DLLayerDataSpec, ?> extractTargetData(
			Map<DLLayerDataSpec, DLLayerDataBatch<? extends DLWritableBuffer>> adapterOutput);

	@Override
	public N getNetwork() {
		return m_network;
	}

	@Override
	public void train(final DLNetworkInputPreparer<DLLayerDataBatch<? extends DLWritableBuffer>> trainingDataPreparer,
			final DLNetworkInputPreparer<DLLayerDataBatch<? extends DLWritableBuffer>> testDataPreparer,
			final long batchSize) throws Exception {
		if (m_trainingData == null) {
			final DLLayerDataSpec[] inputSpecs = m_network.getSpec().getInputSpecs();
			m_trainingData = new HashMap<>(inputSpecs.length);
			for (final DLLayerDataSpec spec : inputSpecs) {
				m_trainingData.put(spec, m_layerDataFactory.createWritableLayerDataBatch(spec, batchSize));
			}
			final DLLayerDataSpec[] outputSpecs = m_network.getSpec().getOutputSpecs();
			m_targetData = new HashMap<>(outputSpecs.length);
			for (final DLLayerDataSpec spec : outputSpecs) {
				m_targetData.put(spec, m_layerDataFactory.createWritableLayerDataBatch(spec, batchSize));
			}
		}
		trainingDataPreparer.prepare(m_trainingData);
		testDataPreparer.prepare(m_targetData);
		trainInternal(batchSize);
		for (final DLLayerDataBatch<?> input : m_trainingData.values()) {
			for (final DLLayerData<?> layerData : input.getBatch()) {
				layerData.getBuffer().reset();
			}
		}
		for (final DLLayerDataBatch<?> output : m_targetData.values()) {
			for (final DLLayerData<?> layerData : output.getBatch()) {
				layerData.getBuffer().reset();
			}
		}
	}

	@Override
	public void close() throws Exception {
		m_network.close();
	}

	// TODO: type safety
	private <I, O> void trainInternal(final long batchSize) throws Exception {
		final DLTrainableNetwork<I, O, ?, ?> network = (DLTrainableNetwork<I, O, ?, ?>) m_network;
		final Map<DLLayerDataSpec, I> trainingData = (Map<DLLayerDataSpec, I>) extractTrainingData(m_trainingData);
		final Map<DLLayerDataSpec, O> targetData = (Map<DLLayerDataSpec, O>) extractTargetData(m_targetData);
		network.train(trainingData, targetData, batchSize);
	}
}
