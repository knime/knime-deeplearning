/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 *
 * History
 *   Sep 25, 2014 (Patrick Winter): created
 */
package org.knime.dl.python.base.node.learner;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.stream.Collectors;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.StringValue;
import org.knime.core.data.filestore.FileStore;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.dl.base.portobjects.DLExternalNetworkPortObject;
import org.knime.dl.base.portobjects.DLNetworkPortObject;
import org.knime.dl.core.DLInvalidContextException;
import org.knime.dl.core.DLInvalidSourceException;
import org.knime.dl.core.DLNetworkType;
import org.knime.dl.core.DLNetworkTypeRegistry;
import org.knime.dl.python.base.node.DLPythonNodeModel;
import org.knime.dl.python.core.DLPythonContext;
import org.knime.dl.python.core.DLPythonDefaultContext;
import org.knime.dl.python.core.DLPythonNetwork;
import org.knime.dl.python.core.DLPythonNetworkHandle;
import org.knime.dl.python.core.DLPythonNetworkLoader;
import org.knime.dl.python.core.DLPythonNetworkType;
import org.knime.python2.kernel.PythonKernel;

/**
 * Shamelessly copied and pasted from python predictor.
 *
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
final class DLPythonLearnerNodeModel extends DLPythonNodeModel<DLPythonLearnerNodeConfig> {

	static final int IN_NETWORK_PORT_IDX = 0;

	static final int IN_DATA_PORT_IDX = 1;

	static void setupNetwork(final DLPythonNetwork<?> network, final DLPythonContext context)
			throws DLInvalidSourceException, DLInvalidContextException, IOException {
		final DLPythonNetworkHandle networkHandle =
				network.getSpec().getNetworkType().getLoader().load(network.getSource(), context);
		final String networkHandleId = networkHandle.getIdentifier();
		final String inputNetworkName = DLPythonLearnerNodeConfig.getVariableNames().getGeneralInputObjects()[0];
		try {
			context.getKernel().execute("import DLPythonNetwork\n" + //
					"global " + inputNetworkName + "\n" + //
					inputNetworkName + " = DLPythonNetwork.get_network('" + networkHandleId + "').model");
		} catch (final IOException e) {
			throw new IOException(
					"An error occurred while communicating with Python (while setting up the Python network).", e);
		}
	}

	static void checkExecutePostConditions(final PythonKernel kernel) throws Exception {
		final String outputNetworkName = DLPythonLearnerNodeConfig.getVariableNames().getGeneralOutputObjects()[0];
		String[] output = kernel.execute("try:\n" + //
				"	" + outputNetworkName + '\n' + //
				"except NameError:\n" + //
				"	print(\"Variable '" + outputNetworkName
				+ "' is not defined. Please make sure to define it in your script.\")");
		if (!output[0].isEmpty()) {
			throw new Exception(output[0].trim());
		}
		output = kernel.execute("import DLPythonNetworkType\n" + //
				"try:\n" + //
				"	DLPythonNetworkType.get_model_network_type(" + outputNetworkName + ")\n" + //
				"except TypeError as ex:\n" + //
				"	print(str(ex) + " //
				+ "'\\nPlease check your assignment of \\'" + outputNetworkName
				+ "\\' within the script and make sure you are not missing any KNIME extensions.')");
		if (!output[0].isEmpty()) {
			throw new Exception(output[0].trim());
		}
	}

	private DataTableSpec m_lastIncomingTableSpec;

	DLPythonLearnerNodeModel() {
		super(new PortType[] { DLNetworkPortObject.TYPE, BufferedDataTable.TYPE },
				new PortType[] { DLNetworkPortObject.TYPE });
	}

	@Override
	protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec) throws Exception {
		final DLNetworkPortObject portObject = (DLNetworkPortObject) inData[IN_NETWORK_PORT_IDX];
		if (!(portObject.getNetwork() instanceof DLPythonNetwork)) {
			throw new InvalidSettingsException("Input deep learning network is not Python compatible.");
		}
		final DLPythonNetwork<?> inNetwork = (DLPythonNetwork<?>) portObject.getNetwork();

		// if the input table is empty, we simply copy and output the input network
		final BufferedDataTable inTable = (BufferedDataTable) inData[IN_DATA_PORT_IDX];
		if (inTable.size() == 0 || inTable.getSpec().getNumColumns() == 0) {
			inNetwork.getSpec().getNetworkType().getLoader().validateSource(inNetwork.getSource());
			setWarningMessage("Input table is empty. Output network equals input network.");
			final FileStore fileStore = DLExternalNetworkPortObject.createFileStoreForCopy(inNetwork.getSource(), exec);
			return new DLNetworkPortObject[] { new DLExternalNetworkPortObject(inNetwork, fileStore) };
		}

		final PythonKernel kernel = new PythonKernel(getKernelOptions());
		try {
			kernel.putFlowVariables(DLPythonLearnerNodeConfig.getVariableNames().getFlowVariables(),
					getAvailableFlowVariables().values());

			final DLPythonContext context = new DLPythonDefaultContext(kernel);
			setupNetwork(inNetwork, context);

			final String loadBackendCode = DLNetworkTypeRegistry.getInstance().getAllNetworkTypes().stream()
					.filter(nt -> nt instanceof DLPythonNetworkType)
					.map(nt -> "import " + ((DLPythonNetworkType<?, ?>) nt).getPythonModuleName() + "\n")
					.collect(Collectors.joining());
			// TODO: we should move this logic out of the node in a later iteration
			kernel.execute(loadBackendCode, exec);
			exec.createSubProgress(0.1).setProgress(1);
			kernel.putDataTable(DLPythonLearnerNodeConfig.getVariableNames().getInputTables()[0], inTable,
					exec.createSubProgress(0.2));
			final String outputNetworkName = DLPythonLearnerNodeConfig.getVariableNames().getGeneralOutputObjects()[0];
			String[] output = kernel.execute(getConfig().getSourceCode(), exec);
			setExternalOutput(new LinkedList<>(Arrays.asList(output[0].split("\n"))));
			setExternalErrorOutput(new LinkedList<>(Arrays.asList(output[1].split("\n"))));
			checkExecutePostConditions(kernel);
			output = kernel.execute("import DLPythonNetwork\n" + //
					"import DLPythonNetworkType\n" + //
					"import pandas as pd\n" + //
					"network_type = DLPythonNetworkType.get_model_network_type(" + outputNetworkName + ")\n" + //
					"DLPythonNetwork.add_network('" + outputNetworkName + "', network_type.wrap_model("
					+ outputNetworkName + "))\n" + //
					"global network_type_identifier\n" + //
					"network_type_identifier = pd.DataFrame(data=[network_type.identifier])\n", exec);
			setExternalOutput(new LinkedList<>(Arrays.asList(output[0].split("\n"))));
			setExternalErrorOutput(new LinkedList<>(Arrays.asList(output[1].split("\n"))));
			exec.createSubProgress(0.4).setProgress(1);
			final Collection<FlowVariable> variables =
					kernel.getFlowVariables(DLPythonLearnerNodeConfig.getVariableNames().getFlowVariables());
			final String networkTypeIdentifier = ((StringValue) kernel
					.getDataTable("network_type_identifier", exec, exec).iterator().next().getCell(0)).getStringValue();
			final DLNetworkType<?, ?, ?> networkType = DLNetworkTypeRegistry.getInstance()
					.getNetworkType(networkTypeIdentifier).orElseThrow(() -> new IllegalStateException(
							"Execution back end '" + networkTypeIdentifier + "' could not be found."));
			if (!(networkType instanceof DLPythonNetworkType)) {
				throw new IllegalStateException("Deep learning network type '" + networkTypeIdentifier
						+ "' associated with Python network '" + outputNetworkName
						+ "' does not seem to be Python compatible. This is an implementation error.");
			}
			final DLPythonNetworkLoader<?> loader = ((DLPythonNetworkType<?, ?>) networkType).getLoader();
			final FileStore fileStore =
					DLExternalNetworkPortObject.createFileStoreForSaving(loader.getSaveModelURLExtension(), exec);
			final URL fileStoreURL = fileStore.getFile().toURI().toURL();
			final DLPythonNetworkHandle handle = new DLPythonNetworkHandle(outputNetworkName);
			loader.save(handle, fileStoreURL, context);
			if (!fileStore.getFile().exists()) {
				throw new IllegalStateException(
						"Failed to save output deep learning network '" + outputNetworkName + "'.");
			}

			addNewVariables(variables);

			final DLPythonNetwork<?> outNetwork = loader.fetch(handle, fileStoreURL, context);
			return new DLNetworkPortObject[] { new DLExternalNetworkPortObject(outNetwork, fileStore) };
		} finally {
			kernel.close();
		}
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
	protected DLPythonLearnerNodeConfig createConfig() {
		return new DLPythonLearnerNodeConfig();
	}
}
