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
 */
package org.knime.dl.core.training;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.knime.dl.core.DLCanceledExecutionException;
import org.knime.dl.core.DLNetwork;
import org.knime.dl.core.DLNetworkInputPreparer;
import org.knime.dl.core.DLNetworkInputProvider;
import org.knime.dl.core.DLTensor;
import org.knime.dl.core.DLTensorFactory;
import org.knime.dl.core.DLTensorId;
import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.core.data.DLWritableBuffer;

import com.google.common.collect.Sets;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public abstract class DLAbstractNetworkTrainingSession<S extends DLTrainingStatus, N extends DLNetwork, CFG extends DLTrainingConfig>
		implements DLNetworkTrainingSession<S> {

	private static boolean areInputSpecsValid(final DLNetwork network, final Set<DLTensorSpec> executionInputSpecs) {
		final DLTensorSpec[] inputSpecs = ArrayUtils.addAll(network.getSpec().getInputSpecs(),
				network.getSpec().getOutputSpecs());
		if (inputSpecs.length != executionInputSpecs.size()) {
			return false;
		}
		final Set<DLTensorId> inputSpecIds = Arrays.stream(inputSpecs).map(DLTensorSpec::getIdentifier)
				.collect(Collectors.toSet());
		final Set<DLTensorId> executionInputSpecIds = executionInputSpecs.stream().map(DLTensorSpec::getIdentifier)
				.collect(Collectors.toSet());
		return Sets.symmetricDifference(inputSpecIds, executionInputSpecIds).isEmpty();
	}

	private static boolean isTensorFactoryValid(final DLNetwork network, final DLTensorFactory tensorFactory) {
		return network.getClass() == tensorFactory.getNetworkType();
	}

	protected final N m_network;

	protected final CFG m_trainingConfig;

	protected final Set<DLTensorSpec> m_executionInputSpecs;

	protected final long m_batchSize;

	protected final DLNetworkInputProvider m_inputProvider;

	protected final DLTensorFactory m_tensorFactory;

	/**
	 * Initialized during the first call of {@link #run(DLTrainingMonitor)}.
	 */
	protected Map<DLTensorId, DLTensor<? extends DLWritableBuffer>> m_input;

	protected DLAbstractNetworkTrainingSession(final N network, final CFG trainingConfig,
			final Set<DLTensorSpec> executionInputSpecs, final DLNetworkInputPreparer inputPreparer,
			final DLTensorFactory tensorFactory) {
		checkArgument(areInputSpecsValid(network, executionInputSpecs),
				"Network input specs and execution input specs differ.");
		checkArgument(isTensorFactoryValid(network, tensorFactory), "Tensor factory does not match network type.");
		m_network = network;
		m_trainingConfig = trainingConfig;
		m_executionInputSpecs = executionInputSpecs;
		m_batchSize = m_executionInputSpecs.stream().findAny()
				.orElseThrow(() -> new IllegalArgumentException("The network must have at least one input."))
				.getBatchSize().orElseThrow(() -> new IllegalArgumentException(
						"The spec of an input tensor must contain a batch size at execution-of-training time."));
		m_inputProvider = new DLNetworkInputProvider() {

			@Override
			public long getNumBatches() {
				return inputPreparer.getNumBatches();
			}

			@Override
			public Map<DLTensorId, DLTensor<? extends DLWritableBuffer>> get(final long batchIndex)
					throws DLCanceledExecutionException {
				inputPreparer.prepare(m_input, batchIndex);
				return m_input;
			}

			@Override
			public void close() throws Exception {
				inputPreparer.close();
			}
		};
		m_tensorFactory = tensorFactory;
	}

	protected abstract void trainInternal(DLTrainingMonitor<? extends S> monitor)
			throws DLCanceledExecutionException, Exception;

	@Override
	public N getNetwork() {
		return m_network;
	}

	@Override
	public DLTrainingConfig getTrainingConfig() {
		return m_trainingConfig;
	}

	@Override
	public void run(final DLTrainingMonitor<? extends S> monitor) throws DLCanceledExecutionException, Exception {
		monitor.getTrainingStatus().trainingStarted().raise(null);
		// lazily preallocate input/target tensors
		if (m_input == null) {
			m_input = new HashMap<>(m_executionInputSpecs.size());
			for (final DLTensorSpec spec : m_executionInputSpecs) {
				m_input.put(spec.getIdentifier(), m_tensorFactory.createWritableTensor(spec));
			}
		}
		trainInternal(monitor);
		monitor.getTrainingStatus().trainingEnded().raise(null);
	}

	@Override
	public void close() throws Exception {
		if (m_input != null) {
			m_input.values().forEach(DLTensor::close);
		}
	}
}
