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
package org.knime.dl.python.core;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.knime.dl.core.DLCancelable;
import org.knime.dl.core.DLCanceledExecutionException;
import org.knime.dl.core.DLInvalidEnvironmentException;
import org.knime.dl.core.DLNetworkInputProvider;
import org.knime.dl.core.DLNetworkSpec;
import org.knime.dl.core.DLTensor;
import org.knime.dl.core.DLTensorId;
import org.knime.dl.core.data.DLReadableBuffer;
import org.knime.dl.core.data.DLWritableBuffer;
import org.knime.dl.core.training.DLTrainingMonitor;
import org.knime.dl.core.training.DLTrainingStatus;
import org.knime.dl.python.core.training.DLPythonTrainingStatus;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public interface DLPythonCommands extends AutoCloseable {

	/**
	 * @return the Python context
	 * @throws DLInvalidEnvironmentException if failed to properly setup the Python context. This includes failures
	 *             during the deep learning specific setup of the Python process, installation tests, registration of
	 *             all Python deep learning back ends and setup of the Python back end that is utilized by this commands
	 *             instance. The thrown exception contains a detailed error message that is suitable to be displayed to
	 *             the user.
	 */
	DLPythonContext getContext() throws DLInvalidEnvironmentException;

	void testInstallation(DLCancelable cancelable) throws DLInvalidEnvironmentException, DLCanceledExecutionException;

	DLPythonNetworkHandle loadNetwork(String path, boolean loadTrainingConfig, DLCancelable cancelable)
			throws DLInvalidEnvironmentException, IOException, DLCanceledExecutionException;

	DLNetworkSpec extractNetworkSpec(DLPythonNetworkHandle network, DLCancelable cancelable) throws DLInvalidEnvironmentException, IOException, DLCanceledExecutionException;

	void saveNetwork(DLPythonNetworkHandle network, String path, DLCancelable cancelable) throws DLInvalidEnvironmentException, IOException, DLCanceledExecutionException;

	void setNetworkInputs(DLPythonNetworkHandle network,
			Map<? extends DLTensorId, ? extends DLTensor<? extends DLWritableBuffer>> inputs, DLCancelable cancelable)
			throws DLInvalidEnvironmentException, IOException, DLCanceledExecutionException;

	void executeNetwork(DLPythonNetworkHandle network, Set<? extends DLTensorId> requestedOutputs, final long batchSize, DLCancelable cancelable)
			throws DLInvalidEnvironmentException, IOException, DLCanceledExecutionException;

	/**
	 * Retrieves the shapes of the output tensors from python. </br>
	 * <b>NOTE:</b> The first dimension of the returned shapes is the batch dimension, which is treated separately in
	 * the rest of the DL framework. Unknown dimensions are encoded as -1.
	 *
	 * @param network the network handle
	 * @param outputs the outputs for which we need the shapes
	 * @return the shapes of the output tensors, the first dimension is the batch dimension
	 * @throws DLInvalidEnvironmentException
	 * @throws IOException
	 */
	<T extends DLTensorId> Map<T, long[]> getNetworkOutputShapes(DLPythonNetworkHandle network, Set<T> outputs, DLCancelable cancelable)
			throws DLInvalidEnvironmentException, IOException, DLCanceledExecutionException;

	void getNetworkOutputs(DLPythonNetworkHandle network,
			Map<? extends DLTensorId, ? extends DLTensor<? extends DLReadableBuffer>> outputs, DLCancelable cancelable)
			throws DLInvalidEnvironmentException, IOException, DLCanceledExecutionException;

	/**
	 * @param the network to train
	 * @param trainingInputProvider the training data provider
	 * @param validationInputProvider the validation data provider, may be null in which case no validation will be
	 *            performed during training
	 * @param monitor the monitor that tracks the progress of the training run. Can be used to report progress, check
	 *            for cancellation or update the {@link DLTrainingStatus training status}.
	 */
	void trainNetwork(DLPythonNetworkHandle network, DLNetworkInputProvider trainingInputProvider,
			DLNetworkInputProvider validationInputProvider, DLTrainingMonitor<? extends DLPythonTrainingStatus> monitor)
			throws DLInvalidEnvironmentException, IOException, DLCanceledExecutionException;
}
