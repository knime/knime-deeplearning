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
package org.knime.dl.keras.core.kernel;

import java.io.IOException;

import org.knime.core.node.NodeLogger;
import org.knime.dl.core.DLLayerDataSpec;
import org.knime.dl.keras.core.DLKerasNetwork.DLKerasNetworkSpec;
import org.knime.dl.keras.core.DLKerasTypeMap;
import org.knime.dl.keras.core.data.DLKerasLayerDataSpecTableCreatorFactory;
import org.knime.dl.python.core.kernel.DLPythonKernel;

import com.google.common.base.Strings;

public class DLKerasPythonKernel extends DLPythonKernel {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(DLKerasPythonKernel.class);

    private final DLKerasPythonKernelConfig m_config;

    public DLKerasPythonKernel(final DLKerasPythonKernelConfig config) throws IOException {
        super(config);
        m_config = config;
        if (!testInstallation()) {
            throw new IOException("Keras installation test failed. Please ensure that Keras is properly installed.");
        }
    }

    public DLKerasNetworkHandle loadNetworkFromH5(final String networkFilePath) throws IOException {
        execute(m_config.getLoadFromH5Code(networkFilePath));
        return new DLKerasNetworkHandle(DLKerasPythonKernelConfig.MODEL_NAME);
    }

    public DLKerasNetworkHandle loadNetworkSpecFromJson(final String networkFilePath) throws IOException {
        execute(m_config.getLoadSpecFromJsonCode(networkFilePath));
        return new DLKerasNetworkHandle(DLKerasPythonKernelConfig.MODEL_NAME);
    }

    public DLKerasNetworkHandle loadNetworkSpecFromYaml(final String networkFilePath) throws IOException {
        execute(m_config.getLoadSpecFromYamlCode(networkFilePath));
        return new DLKerasNetworkHandle(DLKerasPythonKernelConfig.MODEL_NAME);
    }

    public DLKerasNetworkSpec extractNetworkSpec(final DLKerasNetworkHandle network, final DLKerasTypeMap typeMap)
        throws IOException {
        assert network.getIdentifier().equals(DLKerasPythonKernelConfig.MODEL_NAME); // TODO
        execute(m_config.getExtractSpecsCode());
        final DLLayerDataSpec[] inputSpecs = (DLLayerDataSpec[])getData(DLKerasPythonKernelConfig.INPUT_SPECS_NAME,
            new DLKerasLayerDataSpecTableCreatorFactory(typeMap)).getTable();
        final DLLayerDataSpec[] intermediateOutputSpecs =
            (DLLayerDataSpec[])getData(DLKerasPythonKernelConfig.INTERMEDIATE_OUTPUT_SPECS_NAME,
                new DLKerasLayerDataSpecTableCreatorFactory(typeMap)).getTable();
        final DLLayerDataSpec[] outputSpecs = (DLLayerDataSpec[])getData(DLKerasPythonKernelConfig.OUTPUT_SPECS_NAME,
            new DLKerasLayerDataSpecTableCreatorFactory(typeMap)).getTable();

        // TODO: Keras does not expose "intermediate/hidden outputs" for the moment
        // as we're not yet able to extract those via the executor node.
        // Support for this will be added in a future enhancement patch.
        return new DLKerasNetworkSpec(inputSpecs, new DLLayerDataSpec[0] /* TODO intermediateOutputSpecs */,
            outputSpecs);
    }

    public void executeNetwork(final String singleOutputColumnName) throws IOException {
        String[] output = null;
        try {
            output = execute(m_config.getExecuteCode(singleOutputColumnName));
        } finally {
            if (output != null && !Strings.isNullOrEmpty(output[1])) {
                LOGGER.debug(output[1]);
            }
        }
    }

    private boolean testInstallation() {
        try {
            execute(m_config.getTestKerasInstallationCode());
        } catch (final Exception e) {
            return false;
        }
        return true;
    }

    public static class DLKerasNetworkHandle {

        private final String m_identifier;

        private DLKerasNetworkHandle(final String identifier) {
            m_identifier = identifier;
        }

        private String getIdentifier() {
            return m_identifier;
        }
    }
}
