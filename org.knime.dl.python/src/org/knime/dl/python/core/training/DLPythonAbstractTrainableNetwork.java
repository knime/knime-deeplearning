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
package org.knime.dl.python.core.training;

import java.io.IOException;

import org.knime.dl.core.DLInvalidEnvironmentException;
import org.knime.dl.core.DLMissingExtensionException;
import org.knime.dl.core.DLTensor;
import org.knime.dl.core.data.DLWritableBuffer;
import org.knime.dl.core.execution.DLNetworkInputProvider;
import org.knime.dl.core.training.DLAbstractTrainableNetwork;
import org.knime.dl.core.training.DLTrainingConfig;
import org.knime.dl.python.core.DLPythonCommands;
import org.knime.dl.python.core.DLPythonNetwork;
import org.knime.dl.python.core.DLPythonNetworkHandle;
import org.knime.dl.python.core.DLPythonNetworkLoaderRegistry;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public abstract class DLPythonAbstractTrainableNetwork<N extends DLPythonNetwork, //
		CFG extends DLTrainingConfig, C extends DLPythonCommands>
	extends DLAbstractTrainableNetwork<DLTensor<? extends DLWritableBuffer>, //
			DLTensor<? extends DLWritableBuffer>, CFG, N>
		implements DLPythonTrainableNetwork {

	protected C m_commands;

	protected DLPythonNetworkHandle m_handle;

	protected DLPythonAbstractTrainableNetwork(final N network, final CFG trainingConfig) {
		super(network, trainingConfig);
	}

	/**
	 * The caller is responsible for {@link AutoCloseable#close() closing} the command.
	 */
	protected abstract C createCommands() throws DLInvalidEnvironmentException;

	protected abstract void setNetworkTrainingConfig(DLPythonNetworkHandle handle, C commands, CFG config)
			throws DLInvalidEnvironmentException, IOException;

	@Override
	public Class<?> getTrainingDataType() {
		return DLTensor.class;
	}

	@Override
	public Class<?> getTargetDataType() {
		return DLTensor.class;
	}

	@Override
	public void train(final DLNetworkInputProvider<DLTensor<? extends DLWritableBuffer>> inputSupplier)
			throws Exception {
		if (m_commands == null) {
			m_commands = createCommands();
			m_handle = DLPythonNetworkLoaderRegistry.getInstance().getNetworkLoader(m_network.getClass()).orElseThrow(
					() -> new DLMissingExtensionException("Python back end '" + m_network.getClass().getCanonicalName()
							+ "' could not be found. Are you missing a KNIME Deep Learning extension?"))
					.load(m_network.getSource(), m_commands.getContext());
			setNetworkTrainingConfig(m_handle, m_commands, m_trainingConfig);
		}
		m_commands.trainNetwork(m_handle, inputSupplier);
		m_commands.getTrainingResults(m_handle);
	}

	@Override
	public void close() throws Exception {
		if (m_commands != null) {
			m_commands.close();
		}
	}
}
