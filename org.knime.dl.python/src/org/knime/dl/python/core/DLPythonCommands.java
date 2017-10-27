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
package org.knime.dl.python.core;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.knime.dl.core.DLInvalidEnvironmentException;
import org.knime.dl.core.DLNetworkSpec;
import org.knime.dl.core.DLTensor;
import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.core.data.DLReadableBuffer;
import org.knime.dl.core.data.DLWritableBuffer;

/**
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
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

	void testInstallation() throws DLInvalidEnvironmentException;

	DLPythonNetworkHandle loadNetwork(String path) throws DLInvalidEnvironmentException, IOException;

	DLNetworkSpec extractNetworkSpec(DLPythonNetworkHandle network) throws DLInvalidEnvironmentException, IOException;

	void saveNetwork(DLPythonNetworkHandle network, String path) throws DLInvalidEnvironmentException, IOException;

	void setNetworkInputs(DLPythonNetworkHandle network,
			Map<? extends DLTensorSpec, ? extends DLTensor<? extends DLWritableBuffer>> inputs, long batchSize)
			throws DLInvalidEnvironmentException, IOException;

	void executeNetwork(DLPythonNetworkHandle network, Set<? extends DLTensorSpec> requestedOutputs, long batchSize)
			throws DLInvalidEnvironmentException, IOException;

	void getNetworkOutputs(DLPythonNetworkHandle network,
			Map<? extends DLTensorSpec, ? extends DLTensor<? extends DLReadableBuffer>> outputs)
			throws DLInvalidEnvironmentException, IOException;

	void setNetworkTrainingInputs(DLPythonNetworkHandle network,
			Map<? extends DLTensorSpec, ? extends DLTensor<? extends DLWritableBuffer>> trainingData,
			Map<? extends DLTensorSpec, ? extends DLTensor<? extends DLWritableBuffer>> targetData, long batchSize)
			throws DLInvalidEnvironmentException, IOException;

	void trainNetwork(DLPythonNetworkHandle network, long batchSize) throws DLInvalidEnvironmentException, IOException;

	void getTrainingResults(DLPythonNetworkHandle network);
}
