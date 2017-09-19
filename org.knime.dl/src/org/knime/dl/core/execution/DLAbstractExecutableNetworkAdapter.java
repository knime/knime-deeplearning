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
package org.knime.dl.core.execution;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.knime.dl.core.DLLayerData;
import org.knime.dl.core.DLLayerDataFactory;
import org.knime.dl.core.DLLayerDataSpec;
import org.knime.dl.core.data.DLReadableBuffer;
import org.knime.dl.core.data.DLWritableBuffer;

/**
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
public abstract class DLAbstractExecutableNetworkAdapter implements DLExecutableNetworkAdapter {

	private final DLExecutableNetwork<?, ?, ?, ?> m_network;

	private final DLLayerDataFactory m_layerDataFactory;

	private final Collection<DLLayerDataSpec> m_requestedOutputs;

	private HashMap<DLLayerDataSpec, DLLayerDataBatch<? extends DLWritableBuffer>> m_input;

	private HashMap<DLLayerDataSpec, DLLayerDataBatch<? extends DLReadableBuffer>> m_output;

	protected DLAbstractExecutableNetworkAdapter(final DLExecutableNetwork<?, ?, ?, ?> network,
			final DLLayerDataFactory layerDataFactory, final Set<DLLayerDataSpec> requestedOutputs) {
		m_network = network;
		m_layerDataFactory = layerDataFactory;
		m_requestedOutputs = new ArrayList<>(requestedOutputs);
	}

	protected abstract Map<DLLayerDataSpec, ?> extractNetworkInput(
			Map<DLLayerDataSpec, DLLayerDataBatch<? extends DLWritableBuffer>> adapterInput);

	protected abstract Map<DLLayerDataSpec, ?> extractNetworkOutput(
			Map<DLLayerDataSpec, DLLayerDataBatch<? extends DLReadableBuffer>> adapterOutput);

	@Override
	public DLExecutableNetwork<?, ?, ?, ?> getNetwork() {
		return m_network;
	}

	@Override
	public void execute(final DLNetworkInputPreparer<DLLayerDataBatch<? extends DLWritableBuffer>> inputPreparer,
			final DLNetworkOutputConsumer<DLLayerDataBatch<? extends DLReadableBuffer>> outputConsumer,
			final long batchSize) throws Exception {
		if (m_input == null) {
			final DLLayerDataSpec[] inputSpecs = m_network.getSpec().getInputSpecs();
			m_input = new HashMap<>(inputSpecs.length);
			for (final DLLayerDataSpec spec : inputSpecs) {
				m_input.put(spec, m_layerDataFactory.createWritableLayerDataBatch(spec, batchSize));
			}
			m_output = new HashMap<>(m_requestedOutputs.size());
			for (final DLLayerDataSpec spec : m_requestedOutputs) {
				m_output.put(spec, m_layerDataFactory.createReadableLayerDataBatch(spec, batchSize));
			}
		}
		inputPreparer.prepare(m_input);
		executeInternal(batchSize);
		for (final DLLayerDataBatch<?> input : m_input.values()) {
			for (final DLLayerData<?> layerData : input.getBatch()) {
				layerData.getBuffer().reset();
			}
		}
		outputConsumer.accept(m_output);
		for (final DLLayerDataBatch<?> output : m_output.values()) {
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
	private <I, O> void executeInternal(final long batchSize) throws Exception {
		final DLExecutableNetwork<I, O, ?, ?> network = (DLExecutableNetwork<I, O, ?, ?>) m_network;
		final Map<DLLayerDataSpec, I> networkInput = (Map<DLLayerDataSpec, I>) extractNetworkInput(m_input);
		final Map<DLLayerDataSpec, O> networkOutput = (Map<DLLayerDataSpec, O>) extractNetworkOutput(m_output);
		network.execute(networkInput, networkOutput, batchSize);
	}
}
