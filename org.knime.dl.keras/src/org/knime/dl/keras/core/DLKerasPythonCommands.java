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
package org.knime.dl.keras.core;

import java.io.IOException;
import java.util.Set;

import org.knime.core.node.NodeLogger;
import org.knime.dl.core.DLLayerDataSpec;
import org.knime.dl.keras.core.data.DLKerasLayerDataSpecTableCreatorFactory;
import org.knime.dl.python.core.DLPythonNetworkHandle;
import org.knime.dl.python.core.kernel.DLPythonCommands;
import org.knime.python2.generic.ScriptingNodeUtils;
import org.knime.python2.kernel.PythonKernel;
import org.knime.python2.kernel.PythonKernelOptions;

/**
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
public class DLKerasPythonCommands extends DLPythonCommands {

	private static final NodeLogger LOGGER = NodeLogger.getLogger(DLKerasPythonCommands.class);

	private static boolean testedInstallation = false;

	public static PythonKernel createKernel() throws IOException {
		return new PythonKernel(new PythonKernelOptions());
	}

	private final DLKerasPythonKernelConfig m_config;

	public DLKerasPythonCommands() throws IOException {
		this(DLKerasPythonCommands.createKernel());
	}

	public DLKerasPythonCommands(final PythonKernel kernel) throws IOException {
		super(kernel);
		m_config = new DLKerasPythonKernelConfig();
		if (!testedInstallation) {
			if (!testInstallation()) {
				throw new IOException(
						"Keras installation test failed. Please ensure that Keras is properly installed.");
			}
			testedInstallation = true;
		}
	}

	public DLPythonNetworkHandle loadNetworkFromH5(final String networkFilePath) throws IOException {
		m_kernel.execute(m_config.getLoadFromH5Code(networkFilePath));
		return new DLPythonNetworkHandle(DLKerasPythonKernelConfig.MODEL_NAME);
	}

	public DLPythonNetworkHandle loadNetworkSpecFromJson(final String networkFilePath) throws IOException {
		m_kernel.execute(m_config.getLoadSpecFromJsonCode(networkFilePath));
		return new DLPythonNetworkHandle(DLKerasPythonKernelConfig.MODEL_NAME);
	}

	public DLPythonNetworkHandle loadNetworkSpecFromYaml(final String networkFilePath) throws IOException {
		m_kernel.execute(m_config.getLoadSpecFromYamlCode(networkFilePath));
		return new DLPythonNetworkHandle(DLKerasPythonKernelConfig.MODEL_NAME);
	}

	public void saveNetworkToH5(final DLPythonNetworkHandle networkHandle, final String networkFilePath)
			throws IOException {
		m_kernel.execute(m_config.getSaveToH5Code(networkFilePath));
	}

	public DLKerasNetworkSpec extractNetworkSpec(final DLPythonNetworkHandle network, final DLNumPyTypeMap typeMap)
			throws IOException {
		assert network.getIdentifier().equals(DLKerasPythonKernelConfig.MODEL_NAME); // TODO
		m_kernel.execute(m_config.getExtractSpecsCode());
		final DLLayerDataSpec[] inputSpecs = (DLLayerDataSpec[]) m_kernel
				.getData(DLKerasPythonKernelConfig.INPUT_SPECS_NAME,
						new DLKerasLayerDataSpecTableCreatorFactory(typeMap))
				.getTable();
		// final DLLayerDataSpec[] intermediateOutputSpecs =
		// (DLLayerDataSpec[])
		// m_kernel.getData(DLKerasPythonKernelConfig.INTERMEDIATE_OUTPUT_SPECS_NAME,
		// new DLKerasLayerDataSpecTableCreatorFactory(typeMap)).getTable();
		final DLLayerDataSpec[] outputSpecs = (DLLayerDataSpec[]) m_kernel
				.getData(DLKerasPythonKernelConfig.OUTPUT_SPECS_NAME,
						new DLKerasLayerDataSpecTableCreatorFactory(typeMap))
				.getTable();

		// TODO: Keras does not expose "intermediate/hidden outputs" for the
		// moment as we're not yet able to extract
		// those via the executor node. Support for this will be added in a
		// future enhancement patch.
		return new DLKerasDefaultNetworkSpec(inputSpecs,
				new DLLayerDataSpec[0] /* TODO intermediateOutputSpecs */, outputSpecs);
	}

	public void executeNetwork(final Set<DLLayerDataSpec> requestedOutputs) throws IOException {
		final String[] output = m_kernel.execute(m_config.getExecuteCode(requestedOutputs));
		if (!output[1].isEmpty()) {
			LOGGER.debug(ScriptingNodeUtils.shortenString(output[1], 1000));
		}
	}

	private boolean testInstallation() {
		try {
			m_kernel.execute(m_config.getTestKerasInstallationCode());
		} catch (final Exception e) {
			return false;
		}
		return true;
	}
}
