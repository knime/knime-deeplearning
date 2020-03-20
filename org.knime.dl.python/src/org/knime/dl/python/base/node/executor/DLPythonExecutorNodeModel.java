/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 *
 * History
 *   Sep 25, 2014 (Patrick Winter): created
 */
package org.knime.dl.python.base.node.executor;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.dl.core.DLCancelable;
import org.knime.dl.core.DLCanceledExecutionException;
import org.knime.dl.core.DLExecutionMonitorCancelable;
import org.knime.dl.core.DLInvalidEnvironmentException;
import org.knime.dl.core.DLInvalidSourceException;
import org.knime.dl.core.DLMissingExtensionException;
import org.knime.dl.python.base.node.DLPythonNodeModel;
import org.knime.dl.python.core.DLPythonContext;
import org.knime.dl.python.core.DLPythonNetwork;
import org.knime.dl.python.core.DLPythonNetworkHandle;
import org.knime.dl.python.core.DLPythonNetworkLoader;
import org.knime.dl.python.core.DLPythonNetworkLoaderRegistry;
import org.knime.dl.python.core.DLPythonNetworkPortObject;
import org.knime.python2.kernel.PythonExecutionMonitorCancelable;

/**
 * Shamelessly copied and pasted from python predictor.
 *
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 */
final class DLPythonExecutorNodeModel extends DLPythonNodeModel<DLPythonExecutorNodeConfig> {

	static final int IN_NETWORK_PORT_IDX = 0;

	static final int IN_DATA_PORT_IDX = 1;

    static <N extends DLPythonNetwork> void setupNetwork(final N inputNetwork, final DLPythonContext context,
        final DLCancelable cancelable)
			throws DLMissingExtensionException, DLInvalidSourceException, DLInvalidEnvironmentException, IOException, DLCanceledExecutionException {
        final DLPythonNetworkLoader<N> loader =
            DLPythonNetworkLoaderRegistry.getInstance().getNetworkLoader((Class<N>)inputNetwork.getClass())
				.orElseThrow(() -> new DLMissingExtensionException(
						"Python back end '" + inputNetwork.getClass().getCanonicalName()
								+ "' could not be found. Are you missing a KNIME Deep Learning extension?"));
        final DLPythonNetworkHandle networkHandle = loader.load(inputNetwork, context, false, cancelable);
		final String networkHandleId = networkHandle.getIdentifier();
		final String inputNetworkName = DLPythonExecutorNodeConfig.getVariableNames().getGeneralInputObjects()[0];
		try {
			context.executeInKernel("import DLPythonNetwork\n" + //
					"global " + inputNetworkName + "\n" + //
					inputNetworkName + " = DLPythonNetwork.get_network('" + networkHandleId + "').model", cancelable);
		} catch (final IOException e) {
			throw new IOException(
					"An error occurred while communicating with Python (while setting up the Python network).", e);
		}
	}

	private DataTableSpec m_lastIncomingTableSpec;

	DLPythonExecutorNodeModel() {
		super(new PortType[] { DLPythonNetworkPortObject.TYPE, BufferedDataTable.TYPE },
				new PortType[] { BufferedDataTable.TYPE });
	}

	@Override
	protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec) throws Exception {
		// if the input table is empty, we simply output another empty table
		final BufferedDataTable inTable = (BufferedDataTable) inData[IN_DATA_PORT_IDX];
		if (inTable.size() == 0 || inTable.getSpec().getNumColumns() == 0) {
			final BufferedDataContainer emptyContainer = exec.createDataContainer(new DataTableSpec());
			emptyContainer.close();
			setWarningMessage("Input table is empty. Node created an empty output table.");
			return new PortObject[] { emptyContainer.getTable() };
		}
		BufferedDataTable outTable = null;
        final DLCancelable cancelable = new DLExecutionMonitorCancelable(exec);
        try (final DLPythonContext context = getNextContextFromQueue(new PythonExecutionMonitorCancelable(exec))) {
			context.getKernel().putFlowVariables(DLPythonExecutorNodeConfig.getVariableNames().getFlowVariables(),
					getAvailableFlowVariables().values());
			final DLPythonNetworkPortObject<?> portObject = (DLPythonNetworkPortObject<?>) inData[IN_NETWORK_PORT_IDX];
			final DLPythonNetwork network = portObject.getNetwork();
            setupNetwork(network, context, cancelable);
			exec.createSubProgress(0.1).setProgress(1);
			context.getKernel().putDataTable(DLPythonExecutorNodeConfig.getVariableNames().getInputTables()[0], inTable,
					exec.createSubProgress(0.2));
			final String[] output = context.executeInKernel(getConfig().getSourceCode(), cancelable);
			setExternalOutput(new LinkedList<>(Arrays.asList(output[0].split("\n"))));
			setExternalErrorOutput(new LinkedList<>(Arrays.asList(output[1].split("\n"))));
			exec.createSubProgress(0.4).setProgress(1);
			final Collection<FlowVariable> variables = context.getKernel()
					.getFlowVariables(DLPythonExecutorNodeConfig.getVariableNames().getFlowVariables());
			outTable = context.getKernel().getDataTable(
					DLPythonExecutorNodeConfig.getVariableNames().getOutputTables()[0], exec,
					exec.createSubProgress(0.3));
			addNewVariables(variables);
		}
		return new BufferedDataTable[] { outTable };
	}

	@Override
	protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
		final DataTableSpec inTableSpec = (DataTableSpec) inSpecs[IN_DATA_PORT_IDX];
		// warn user if previously present columns changed or vanished which could lead to unexpected problems in the
		// Python code
		boolean tableChanged = false;
		if (m_lastIncomingTableSpec != null) {
			if (inTableSpec == null) {
				tableChanged = true;
			} else {
				for (final DataColumnSpec oldCol : m_lastIncomingTableSpec) {
					final DataColumnSpec newCol = inTableSpec.getColumnSpec(oldCol.getName());
					if (!oldCol.equalStructure(newCol)) {
						tableChanged = true;
						break;
					}
				}
			}
		}
		if (tableChanged) {
			setWarningMessage("Input table changed.");
		}
		m_lastIncomingTableSpec = inTableSpec;
		return new PortObjectSpec[] { null };
	}

	@Override
	protected DLPythonExecutorNodeConfig createConfig() {
		return new DLPythonExecutorNodeConfig();
	}
}
