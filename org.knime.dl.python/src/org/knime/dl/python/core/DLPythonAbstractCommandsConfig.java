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
 */
package org.knime.dl.python.core;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;

import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.util.DLUtils;
import org.osgi.framework.FrameworkUtil;

/**
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
public abstract class DLPythonAbstractCommandsConfig {

	// String constants that are used on Python side:

	public static final String DEFAULT_MODEL_NAME = "model";

	public static final String INPUT_SPECS_NAME = "input_specs";

	public static final String HIDDEN_OUTPUT_SPECS_NAME = "intermediate_output_specs";

	public static final String OUTPUT_SPECS_NAME = "output_specs";

	public static final String INPUT_TABLE_NAME = "input_table";

	public static final String OUTPUT_TABLE_NAME = "output_table";

	public abstract File getInstallationTestScript() throws IOException;

	public abstract String getLoadNetworkCode(final String path);

	public String getSetupEnvironmentCode() {
		return "";
	}

	public String getSetupBackendCode() {
		return "";
	}

	public String getSaveNetworkCode(final DLPythonNetworkHandle handle, final String path) {
		return "import DLPythonNetwork\n" + //
				"network = DLPythonNetwork.get_network('" + handle.getIdentifier() + "')\n" + //
				"network.save(r'" + path + "')";
	}

	public String getExtractNetworkSpecsCode(final DLPythonNetworkHandle handle) throws IOException {
		return "import DLPythonNetworkSpecExtractor\n" + //
				"global " + INPUT_SPECS_NAME + "\n" + //
				"global " + HIDDEN_OUTPUT_SPECS_NAME + "\n" + //
				"global " + OUTPUT_SPECS_NAME + "\n" + //
				INPUT_SPECS_NAME + ", " + HIDDEN_OUTPUT_SPECS_NAME + ", " + OUTPUT_SPECS_NAME + " = " + //
				"DLPythonNetworkSpecExtractor.get_layer_data_specs_as_data_frame('" + handle.getIdentifier() + "')";
	}

	public String getExecuteNetworkCode(final DLPythonNetworkHandle handle, final Set<DLTensorSpec> requestedOutputs) {
		// TODO: add requestedOutputs functionality
		return "import DLPythonNetwork\n" + //
				"network = DLPythonNetwork.get_network('" + handle.getIdentifier() + "')\n" + //
				"in_data = {}\n" + //
				"for input_spec in network.spec.input_specs:\n" + //
				"	in_data[input_spec.name] = globals()[input_spec.name]\n" + //
				"out_data = network.execute(in_data)\n" + //
				"for name, data in out_data.items():\n" + //
				"	globals()[name] = data";
	}

	protected final File getSnippetFile(final String relativePath) throws IOException {
		final String bundleName = FrameworkUtil.getBundle(getClass()).getSymbolicName();
		return DLUtils.Files.getFileFromBundle(bundleName, relativePath);
	}

	protected final String readSnippetContent(final String snippetPath) throws IOException {
		return new String(Files.readAllBytes(Paths.get(snippetPath)), StandardCharsets.UTF_8);
	}
}
