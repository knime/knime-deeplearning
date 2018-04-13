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
package org.knime.dl.keras.theano.core;

import java.io.File;
import java.io.IOException;

import org.knime.dl.core.DLInvalidEnvironmentException;
import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.keras.core.DLKerasAbstractCommands;
import org.knime.dl.python.core.DLPythonTensorSpecTableCreatorFactory;
import org.knime.dl.python.core.DLPythonContext;
import org.knime.dl.python.core.DLPythonNetworkHandle;
import org.knime.dl.python.core.DLPythonNumPyTypeMap;
import org.knime.dl.python.util.DLPythonUtils;
import org.knime.dl.util.DLUtils;
import org.knime.python2.kernel.PythonKernel;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public final class DLKerasTheanoCommands extends DLKerasAbstractCommands {

	public DLKerasTheanoCommands() throws DLInvalidEnvironmentException {
	}

	public DLKerasTheanoCommands(final DLPythonContext context) throws DLInvalidEnvironmentException {
		super(context);
	}

	@Override
	public DLKerasTheanoNetworkSpec extractNetworkSpec(final DLPythonNetworkHandle handle)
			throws DLInvalidEnvironmentException, IOException {
		getContext().executeInKernel(getExtractNetworkSpecsCode(handle));
		final PythonKernel kernel = getContext().getKernel();
		final DLTensorSpec[] inputSpecs = (DLTensorSpec[]) kernel
				.getData(INPUT_SPECS_NAME, new DLPythonTensorSpecTableCreatorFactory(DLPythonNumPyTypeMap.INSTANCE))
				.getTable();
		// final DLTensorSpec[] hiddenOutputSpecs =
		// (DLTensorSpec[]) m_kernel.getData(DLPythonCommandsConfig.INTERMEDIATE_OUTPUT_SPECS_NAME,
		// new DLKerasTensorSpecTableCreatorFactory(DLPythonNumPyTypeMap.INSTANCE)).getTable();
		final DLTensorSpec[] outputSpecs = (DLTensorSpec[]) kernel
				.getData(OUTPUT_SPECS_NAME, new DLPythonTensorSpecTableCreatorFactory(DLPythonNumPyTypeMap.INSTANCE))
				.getTable();
		// TODO: Keras does not expose "hidden outputs" (see above) for the moment as we're not yet able to
		// extract those via the executor node. Support for this will be added in a future enhancement patch.
		return new DLKerasTheanoNetworkSpec(inputSpecs, new DLTensorSpec[0] /* TODO hiddenOutputSpecs */, outputSpecs);
	}

	@Override
	protected String getSetupEnvironmentCode() {
		return "import os\n" + //
				"os.environ['KERAS_BACKEND'] = 'theano'\n";
	}

	@Override
	protected File getInstallationTestFile() throws IOException {
		return DLUtils.Files.getFileFromSameBundle(this, "py/DLKerasTheanoNetworkTester.py");
	}

	@Override
	protected String getSetupBackendCode() {
		return "";
	}

	@Override
	protected String getLoadNetworkCode(final String path, final boolean loadTrainingConfig) {
		return "import DLPythonNetwork\n" + //
				"from DLKerasTheanoNetwork import DLKerasTheanoNetworkReader\n" + //
				"network = DLKerasTheanoNetworkReader().read(r'" + path + "', compile="
				+ DLPythonUtils.toPython(loadTrainingConfig) + ")\n" + //
				"DLPythonNetwork.add_network('" + DEFAULT_MODEL_NAME + "', network)";
	}

	@Override
	protected String getLoadNetworkFromJsonCode(final String path) {
		return "import DLPythonNetwork\n" + //
				"from DLKerasTheanoNetwork import DLKerasTheanoNetworkReader\n" + //
				"network = DLKerasTheanoNetworkReader().readFromJson(r'" + path + "')\n" + //
				"DLPythonNetwork.add_network('" + DEFAULT_MODEL_NAME + "', network)";
	}

	@Override
	protected String getLoadNetworkFromYamlCode(final String path) {
		return "import DLPythonNetwork\n" + //
				"from DLKerasTheanoNetwork import DLKerasTheanoNetworkReader\n" + //
				"network = DLKerasTheanoNetworkReader().readFromYaml(r'" + path + "')\n" + //
				"DLPythonNetwork.add_network('" + DEFAULT_MODEL_NAME + "', network)";
	}
}
