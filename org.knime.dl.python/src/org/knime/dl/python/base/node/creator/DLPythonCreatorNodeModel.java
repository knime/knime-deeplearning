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
package org.knime.dl.python.base.node.creator;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.stream.Collectors;

import org.knime.core.data.StringValue;
import org.knime.core.data.filestore.FileStore;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.dl.base.portobjects.DLNetworkPortObject;
import org.knime.dl.core.DLInvalidEnvironmentException;
import org.knime.dl.core.DLInvalidSourceException;
import org.knime.dl.core.DLMissingExtensionException;
import org.knime.dl.python.base.node.DLPythonNodeModel;
import org.knime.dl.python.core.DLPythonContext;
import org.knime.dl.python.core.DLPythonDefaultContext;
import org.knime.dl.python.core.DLPythonNetwork;
import org.knime.dl.python.core.DLPythonNetworkHandle;
import org.knime.dl.python.core.DLPythonNetworkLoader;
import org.knime.dl.python.core.DLPythonNetworkLoaderRegistry;
import org.knime.dl.python.core.DLPythonNetworkPortObject;
import org.knime.dl.python.util.DLPythonSourceCodeBuilder;
import org.knime.dl.python.util.DLPythonUtils;
import org.knime.python2.kernel.PythonKernel;

/**
 * Shamelessly copied and pasted from python source.
 *
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
final class DLPythonCreatorNodeModel extends DLPythonNodeModel<DLPythonCreatorNodeConfig> {

	protected DLPythonCreatorNodeModel() {
		super(null, new PortType[] { DLPythonNetworkPortObject.TYPE });
	}

	static void checkExecutePostConditions(final DLPythonContext context)
			throws DLInvalidEnvironmentException, IOException {
		final String outputNetworkName = DLPythonCreatorNodeConfig.getVariableNames().getGeneralOutputObjects()[0];
		// check if output network variable was assigned at all
		DLPythonSourceCodeBuilder b = DLPythonUtils.createSourceCodeBuilder() //
				.a("try:") //
				.n().t().a(outputNetworkName) //
				.n("except NameError:") //
				.n().t().a("raise NameError(") //
				.as("Variable '" + outputNetworkName
						+ "' is not defined. Please make sure to define it in your script.")
				.a(")");
		context.executeInKernel(b.toString());
		// check if output network variable was assigned with a valid value
		b = DLPythonUtils.createSourceCodeBuilder() //
				.a("import DLPythonNetworkType") //
				.n("try:") //
				.n().t().a("DLPythonNetworkType.get_model_network_type(").a(outputNetworkName).a(")") //
				.n("except TypeError as ex:") //
				.n().t().a("raise TypeError(str(ex) + ") //
				.as("\\nPlease check your assignment of '" + outputNetworkName
						+ "' within the script and make sure you are not missing any KNIME extensions.") //
				.a(")");
		context.executeInKernel(b.toString());
	}

	@Override
	protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec) throws Exception {
		final DLPythonContext context = new DLPythonDefaultContext(new PythonKernel(getKernelOptions()));
		try {
			context.getKernel().putFlowVariables(DLPythonCreatorNodeConfig.getVariableNames().getFlowVariables(),
					getAvailableFlowVariables().values());
			final String loadBackendCode = DLPythonNetworkLoaderRegistry.getInstance().getAllNetworkLoaders().stream()
					.map(nl -> "import " + nl.getPythonModuleName() + "\n") //
					.collect(Collectors.joining());
			// TODO: we should move this logic out of the node in a later iteration
			context.getKernel().execute(loadBackendCode, exec);
			final String outputNetworkName = DLPythonCreatorNodeConfig.getVariableNames().getGeneralOutputObjects()[0];
			String[] output = context.getKernel().execute(getConfig().getSourceCode(), exec);
			setExternalOutput(new LinkedList<>(Arrays.asList(output[0].split("\n"))));
			setExternalErrorOutput(new LinkedList<>(Arrays.asList(output[1].split("\n"))));
			checkExecutePostConditions(context);
			output = context.getKernel().execute("import DLPythonNetwork\n" + //
					"import DLPythonNetworkType\n" + //
					"import pandas as pd\n" + //
					"network_type = DLPythonNetworkType.get_model_network_type(" + outputNetworkName + ")\n" + //
					"DLPythonNetwork.add_network('" + outputNetworkName + "', network_type.wrap_model("
					+ outputNetworkName + "))\n" + //
					"global network_type_identifier\n" + //
					"network_type_identifier = pd.DataFrame(data=[network_type.identifier])\n", exec);
			setExternalOutput(new LinkedList<>(Arrays.asList(output[0].split("\n"))));
			setExternalErrorOutput(new LinkedList<>(Arrays.asList(output[1].split("\n"))));
			exec.createSubProgress(0.5).setProgress(1);
			final Collection<FlowVariable> variables = context.getKernel()
					.getFlowVariables(DLPythonCreatorNodeConfig.getVariableNames().getFlowVariables());
			final String networkLoaderIdentifier = ((StringValue) context.getKernel()
					.getDataTable("network_type_identifier", exec, exec).iterator().next().getCell(0)).getStringValue();
			final DLPythonNetworkLoader<?> loader = DLPythonNetworkLoaderRegistry.getInstance()
					.getNetworkLoader(networkLoaderIdentifier)
					.orElseThrow(() -> new DLMissingExtensionException("Python back end '" + networkLoaderIdentifier
							+ "' could not be found. Are you missing a KNIME Deep Learning extension?"));
			final FileStore fileStore = DLNetworkPortObject.createFileStoreForSaving(loader.getSaveModelURLExtension(),
					exec);
			final URL fileStoreURL = fileStore.getFile().toURI().toURL();
			final DLPythonNetworkHandle handle = new DLPythonNetworkHandle(outputNetworkName);
			loader.save(handle, fileStoreURL, context);
			if (!fileStore.getFile().exists()) {
				throw new IllegalStateException(
						"Failed to save output deep learning network '" + outputNetworkName + "'.");
			}
			addNewVariables(variables);
			return new DLNetworkPortObject[] {
					createOutputPortObject(loader, handle, fileStoreURL, context, fileStore) };
		} finally {
			context.close();
		}
	}

	@Override
	protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
		return new PortObjectSpec[] { null };
	}

	@Override
	protected DLPythonCreatorNodeConfig createConfig() {
		return new DLPythonCreatorNodeConfig();
	}

	private <N extends DLPythonNetwork> DLPythonNetworkPortObject<? extends DLPythonNetwork> createOutputPortObject(
			final DLPythonNetworkLoader<N> loader, final DLPythonNetworkHandle handle, final URL source,
			final DLPythonContext context, final FileStore fileStore)
			throws DLInvalidSourceException, DLInvalidEnvironmentException, IOException {
		final N network = loader.fetch(handle, source, context);
		return loader.createPortObject(network, fileStore);
	}
}
