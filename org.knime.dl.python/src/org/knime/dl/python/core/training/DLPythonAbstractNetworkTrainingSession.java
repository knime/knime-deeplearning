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
package org.knime.dl.python.core.training;

import java.io.IOException;
import java.util.Set;

import org.knime.dl.core.DLCanceledExecutionException;
import org.knime.dl.core.DLFixedTensorShape;
import org.knime.dl.core.DLInvalidEnvironmentException;
import org.knime.dl.core.DLMissingExtensionException;
import org.knime.dl.core.DLNetworkInputPreparer;
import org.knime.dl.core.DLTensorFactory;
import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.core.training.DLAbstractNetworkTrainingSession;
import org.knime.dl.core.training.DLTrainingConfig;
import org.knime.dl.core.training.DLTrainingMonitor;
import org.knime.dl.core.training.DLTrainingStatus;
import org.knime.dl.python.core.DLPythonCommands;
import org.knime.dl.python.core.DLPythonNetwork;
import org.knime.dl.python.core.DLPythonNetworkHandle;
import org.knime.dl.python.core.DLPythonNetworkLoaderRegistry;

/**
 * Abstract base class for implementations of {@link DLPythonNetworkTrainingSession}.
 *
 * @param <S> the type of the {@link DLTrainingStatus status} that contains information about the training progress
 *            while the session is running
 * @param <N> the type of the {@link DLPythonNetwork network} to train
 * @param <CFG> the type of the {@link DLTrainingConfig training config} that specifies how the network will be trained
 * @param <C> the type of the {@link DLPythonCommands} that are used to control the training process on Python side
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public abstract class DLPythonAbstractNetworkTrainingSession<S extends DLPythonTrainingStatus, N extends DLPythonNetwork, //
		CFG extends DLTrainingConfig, C extends DLPythonCommands>
	extends DLAbstractNetworkTrainingSession<S, N, CFG> implements DLPythonNetworkTrainingSession<S> {

	/**
	 * The Python commands that are used to control the training process on Python side. Is instantiated via
	 * {@link #createCommands()} at the beginning of the first call of {@link #trainInternal(DLTrainingMonitor)}.
	 */
	protected C m_commands;

	/**
	 * The Python handle of the network that is trained. Is instantiated at the beginning of the first call of
	 * {@link #trainInternal(DLTrainingMonitor)}.
	 */
	protected DLPythonNetworkHandle m_handle;

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
	protected DLPythonAbstractNetworkTrainingSession(final N network, final CFG trainingConfig,
			final Set<DLTensorSpec> executionInputSpecs, final DLNetworkInputPreparer trainingInputPreparer,
			final DLNetworkInputPreparer validationInputPreparer, final DLTensorFactory tensorFactory) {
		super(network, trainingConfig, executionInputSpecs, trainingInputPreparer, validationInputPreparer,
				tensorFactory);
	}

	/**
	 * Creates the back end specific Python commands that are used to instruct the training process on Python side.
	 * <P>
	 * This method is called during the first call of
	 * {@link DLPythonAbstractNetworkTrainingSession#trainInternal(DLTrainingMonitor)}.<br>
	 * The caller is responsible for {@link AutoCloseable#close() closing} the command.
	 *
	 * @return the created Python commands
	 * @throws DLInvalidEnvironmentException if failed to create valid Python commands
	 */
	protected abstract C createCommands() throws DLInvalidEnvironmentException;

	/**
	 * Sets the given training config for the given network handle.
	 * <P>
	 * This method is called during the first call of
	 * {@link DLPythonAbstractNetworkTrainingSession#trainInternal(DLTrainingMonitor)}.
	 *
	 * @param handle the handle of the network for which to set the training config
	 * @param config the training config to set
	 * @throws DLInvalidEnvironmentException if setting the training config led to an exception on Python side or if the
	 *             Python side was not properly set up
	 * @throws IOException if an error occurred while communication with Python
	 */
	protected abstract void setNetworkTrainingConfig(DLPythonNetworkHandle handle, CFG config)
			throws DLInvalidEnvironmentException, IOException;

	@Override
	public void close() throws Exception {
		super.close();
		if (m_commands != null) {
			m_commands.close();
		}
	}

	@Override
	protected void trainInternal(final DLTrainingMonitor<? extends S> monitor)
			throws DLCanceledExecutionException, Exception {
		if (m_commands == null) {
			m_commands = createCommands();
			m_handle = DLPythonNetworkLoaderRegistry.getInstance().getNetworkLoader(m_network.getClass()).orElseThrow(
					() -> new DLMissingExtensionException("Python back end '" + m_network.getClass().getCanonicalName()
							+ "' could not be found. Are you missing a KNIME Deep Learning extension?"))
                .load(m_network.getSource().getURI(), m_commands.getContext(), true);
			setNetworkTrainingConfig(m_handle, m_trainingConfig);
		}
		m_commands.trainNetwork(m_handle, m_trainingInputProvider, m_validationInputProvider, monitor);
	}
}
