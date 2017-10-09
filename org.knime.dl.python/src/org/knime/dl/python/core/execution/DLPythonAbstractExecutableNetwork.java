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
 * History
 *   May 3, 2017 (marcel): created
 */
package org.knime.dl.python.core.execution;

import java.net.URL;
import java.util.Map;

import org.knime.dl.core.DLInvalidContextException;
import org.knime.dl.core.DLTensor;
import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.core.data.DLReadableBuffer;
import org.knime.dl.core.data.DLWritableBuffer;
import org.knime.dl.core.execution.DLAbstractExecutableNetwork;
import org.knime.dl.python.core.DLPythonAbstractCommands;
import org.knime.dl.python.core.DLPythonNetwork;
import org.knime.dl.python.core.DLPythonNetworkHandle;
import org.knime.dl.python.core.DLPythonNetworkSpec;

/**
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
public abstract class DLPythonAbstractExecutableNetwork<N extends DLPythonNetwork<S>, S extends DLPythonNetworkSpec, //
		C extends DLPythonAbstractCommands<?>>
	extends DLAbstractExecutableNetwork<DLTensor<? extends DLWritableBuffer>, //
			DLTensor<? extends DLReadableBuffer>, N, S, URL> {

	private C m_commands;

	private DLPythonNetworkHandle m_handle;

	protected DLPythonAbstractExecutableNetwork(final N network) {
		super(network);
	}

	protected abstract C createCommands() throws DLInvalidContextException;

	// TODO: we may need an own type class (cf. org.knime.core.data.DataType) as "DLTensor.class" isn't really
	// informative here.

	@Override
	public Class<?> getInputType() {
		return DLTensor.class;
	}

	@Override
	public Class<?> getOutputType() {
		return DLTensor.class;
	}

	@Override
	public void execute(final Map<DLTensorSpec, DLTensor<? extends DLWritableBuffer>> input,
			final Map<DLTensorSpec, DLTensor<? extends DLReadableBuffer>> output, final long batchSize)
			throws Exception {
		if (m_commands == null) {
			m_commands = createCommands();
			m_handle = m_network.getSpec().getNetworkType().getLoader().load(m_network.getSource(),
					m_commands.getContext());
		}
		m_commands.setNetworkInputs(m_handle, input, batchSize);
		m_commands.executeNetwork(m_handle, output.keySet(), batchSize);
		m_commands.getNetworkOutputs(m_handle, output);
	}

	@Override
	public void close() throws Exception {
		if (m_commands != null) {
			m_commands.close();
		}
	}
}
