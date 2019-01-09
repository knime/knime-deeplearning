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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.knime.dl.core.DLCanceledExecutionException;
import org.knime.dl.core.DLFixedTensorShape;
import org.knime.dl.core.DLInvalidNetworkInputException;
import org.knime.dl.core.DLNetwork;
import org.knime.dl.core.DLNetworkFixedSizeInputPreparer;
import org.knime.dl.core.DLNetworkInputProvider;
import org.knime.dl.core.DLTensor;
import org.knime.dl.core.DLTensorFactory;
import org.knime.dl.core.DLTensorId;
import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.core.data.DLWritableBuffer;
import org.knime.dl.util.DLUtils;

import com.google.common.collect.Sets;

/**
 * Abstract base class for implementations of {@link DLNetworkTrainingSession}.
 *
 * @param <S> the type of the {@link DLTrainingStatus status} that contains information about the training progress
 *            while the session is running
 * @param <N> the type of the {@link DLNetwork network} to train
 * @param <CFG> the type of the {@link DLTrainingConfig training config} that specifies how the network will be trained
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public abstract class DLAbstractNetworkTrainingSession<S extends DLTrainingStatus, N extends DLNetwork, CFG extends DLTrainingConfig>
		implements DLNetworkTrainingSession<S> {

	// Constructor argument validation:

	private static boolean areInputSpecsCongruent(final DLNetwork network,
			final Set<DLTensorSpec> executionInputSpecs) {
		final Set<DLTensorSpec> inputSpecs = Sets.newHashSet(ArrayUtils.addAll(network.getSpec().getInputSpecs(),
				network.getSpec().getOutputSpecs()));
		if (inputSpecs.size() != executionInputSpecs.size()) {
			return false;
		}
		final Set<DLTensorId> inputSpecIds = inputSpecs.stream().map(DLTensorSpec::getIdentifier)
				.collect(Collectors.toSet());
		final Set<DLTensorId> executionInputSpecIds = executionInputSpecs.stream().map(DLTensorSpec::getIdentifier)
				.collect(Collectors.toSet());
		return Sets.symmetricDifference(inputSpecIds, executionInputSpecIds).isEmpty();
	}

	private static boolean areExecInputSpecsFullyDefined(final Set<DLTensorSpec> executionInputSpecs) {
		return executionInputSpecs.stream()
				.allMatch(s -> s.getBatchSize().isPresent() && DLUtils.Shapes.isFixed(s.getShape()));
	}

	private static boolean doExecInputsSpecsMatchConfig(final DLTrainingConfig trainingConfig,
			final Set<DLTensorSpec> executionInputSpecs) {
		return executionInputSpecs.stream()
				.allMatch(s -> s.getBatchSize().getAsLong() == trainingConfig.getBatchSize());
	}

	/**
	 * The network to train.
	 */
	protected final N m_network;

	/**
	 * The training configuration that specifies how the network will be trained.
	 */
	protected final CFG m_trainingConfig;

	/**
	 * The network's fully defined input tensor specs.
	 */
	protected final Set<DLTensorSpec> m_executionInputSpecs;

	/**
	 * Provides the training data batches.
	 */
	protected final DLNetworkInputProvider m_trainingInputProvider;

	/**
	 * Specifies whether validation will be performed during training.
	 */
	protected final boolean m_doValidation;

	/**
	 * Provides the validation date batches. Non-null if {@link #m_doValidation} is true.
	 */
	protected final DLNetworkInputProvider m_validationInputProvider;

	/**
	 * The tensor factory that is used to create the network's input and target tensors.
	 */
	protected final DLTensorFactory m_tensorFactory;

	/**
	 * Initialized during the first call of {@link #run(DLTrainingMonitor)}.
	 */
	protected Map<DLTensorId, DLTensor<? extends DLWritableBuffer>> m_trainingInput;

	/**
	 * Initialized during the first call of {@link #run(DLTrainingMonitor)} if {@link #m_doValidation} is true.
	 */
	protected Map<DLTensorId, DLTensor<? extends DLWritableBuffer>> m_validationInput;

	/**
	 * @param network the network to train
	 * @param trainingConfig the training configuration that specifies how the network will be trained
	 * @param executionInputSpecs a set of fully defined tensor specs. The set of tensor specs must exactly match the
	 *            network's input tensor specs with respect to the identifiers of the contained specs. A tensor spec is
	 *            fully defined if it features a non-empty batch size and a {@link DLFixedTensorShape fixed tensor
	 *            shape}.
	 * @param trainingInputPreparer the training data preparer
	 * @param validationInputPreparer the validation data preparer, may be null in which case no validation will be
	 *            performed during training
	 * @param tensorFactory the tensor factory that is used to create the network's input and target tensors
	 */
    protected DLAbstractNetworkTrainingSession(final N network, final CFG trainingConfig,
        final Set<DLTensorSpec> executionInputSpecs, final DLNetworkFixedSizeInputPreparer trainingInputPreparer,
        final DLNetworkFixedSizeInputPreparer validationInputPreparer, final DLTensorFactory tensorFactory) {
        checkArgument(areInputSpecsCongruent(checkNotNull(network), checkNotNull(executionInputSpecs)),
				"Network input specs and execution input specs differ.");
		checkArgument(areExecInputSpecsFullyDefined(executionInputSpecs),
				"Execution input specs are not fully defined.");
		checkArgument(doExecInputsSpecsMatchConfig(trainingConfig, executionInputSpecs),
				"Batch size of execution input specs to not match training config.");
		m_network = network;
		m_trainingConfig = checkNotNull(trainingConfig);
		m_executionInputSpecs = executionInputSpecs;
		checkNotNull(trainingInputPreparer);
		m_trainingInputProvider = new DLNetworkInputProvider() {

			@Override
			public long getNumBatches() {
				return trainingInputPreparer.getNumBatches();
			}

			@Override
			public Map<DLTensorId, DLTensor<? extends DLWritableBuffer>> get(final long batchIndex)
					throws DLCanceledExecutionException, DLInvalidNetworkInputException {
				trainingInputPreparer.prepare(m_trainingInput, batchIndex);
				return m_trainingInput;
			}

			@Override
			public void close() throws Exception {
				trainingInputPreparer.close();
			}
		};
		m_doValidation = validationInputPreparer != null;
		m_validationInputProvider = m_doValidation ? new DLNetworkInputProvider() {

			@Override
			public long getNumBatches() {
				return validationInputPreparer.getNumBatches();
			}

			@Override
			public Map<DLTensorId, DLTensor<? extends DLWritableBuffer>> get(final long batchIndex)
					throws DLCanceledExecutionException, DLInvalidNetworkInputException {
				validationInputPreparer.prepare(m_validationInput, batchIndex);
				return m_validationInput;
			}

			@Override
			public void close() throws Exception {
				validationInputPreparer.close();
			}
		} : null;
		m_tensorFactory = tensorFactory;
	}

	/**
	 * Contains the actual training logic.
	 * <P>
	 * This method is called once by {@link DLNetworkTrainingSession#run(DLTrainingMonitor)} after the network
	 * input/targets were set up.
	 *
	 * @param monitor the monitor that tracks the progress of the training run. Can be used to report progress, check
	 *            for cancellation or update the {@link DLTrainingStatus training status}. Note, that the training
	 *            status' <code>trainingStarted</code> and <code>trainingEnded</code> events are called by the calling
	 *            <code>run</code> method and must not be called by this method.
	 * @throws DLCanceledExecutionException if execution was canceled by the user
	 * @throws Exception if any other exception occurs during training
	 */
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
		// lazily preallocate training input/target tensors
		if (m_trainingInput == null) {
			m_trainingInput = new HashMap<>(m_executionInputSpecs.size());
			for (final DLTensorSpec spec : m_executionInputSpecs) {
				m_trainingInput.put(spec.getIdentifier(), m_tensorFactory.createWritableTensor(spec));
			}
		}
		// lazily preallocate validation input/target tensors
		if (m_doValidation && m_validationInput == null) {
			m_validationInput = new HashMap<>(m_executionInputSpecs.size());
			for (final DLTensorSpec spec : m_executionInputSpecs) {
				// we need to replace the training data batch size by the validation data batch size. Specs are fully
				// defined, no need to check if optionals are present.
				final DLTensorSpec validationSpec = m_tensorFactory.createExecutionTensorSpec(spec,
						m_trainingConfig.getValidationBatchSize(), DLUtils.Shapes.getFixedShape(spec.getShape()).get());
				m_validationInput.put(validationSpec.getIdentifier(),
						m_tensorFactory.createWritableTensor(validationSpec));
			}
		}
		trainInternal(monitor);
		monitor.getTrainingStatus().trainingEnded().raise(null);
	}

	@Override
	public void close() throws Exception {
		if (m_trainingInput != null) {
			m_trainingInput.values().forEach(DLTensor::close);
		}
		if (m_validationInput != null) {
			m_validationInput.values().forEach(DLTensor::close);
		}
	}
}
