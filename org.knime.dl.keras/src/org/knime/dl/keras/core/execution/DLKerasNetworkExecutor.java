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
 *   May 19, 2017 (marcel): created
 */
package org.knime.dl.keras.core.execution;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.knime.core.util.FileUtil;
import org.knime.dl.core.DLLayerDataSpec;
import org.knime.dl.core.execution.DLLayerDataInput;
import org.knime.dl.core.execution.DLLayerDataOutput;
import org.knime.dl.core.execution.DLNetworkExecutor;
import org.knime.dl.keras.core.DLKerasBackend;
import org.knime.dl.keras.core.DLKerasExecutableNetwork;
import org.knime.dl.keras.core.kernel.DLKerasPythonKernel;
import org.knime.dl.keras.core.kernel.DLKerasPythonKernel.DLKerasNetworkHandle;
import org.knime.dl.keras.core.kernel.DLKerasPythonKernelConfig;

/**
 * Executes a {@link DLKerasExecutableNetwork}.
 *
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
public class DLKerasNetworkExecutor implements DLNetworkExecutor<DLKerasExecutableNetwork> {

    private final DLKerasBackend m_backend;

    private final DLKerasPythonKernel m_kernel;

    private boolean m_first;

    /**
     * Creates a new network executor for the given Keras back end.
     *
     * @param backend the Keras back end
     */
    public DLKerasNetworkExecutor(final DLKerasBackend backend) throws IOException {
        m_backend = backend;
        m_kernel = setupKernel();
        m_first = true;
    }

    private void init(final DLKerasExecutableNetwork network) throws IOException {
        final String filePath = FileUtil.getFileFromURL(network.getSource()).getAbsolutePath();
        final String fileExtension = FilenameUtils.getExtension(filePath);
        final DLKerasNetworkHandle networkHandle;
        if (fileExtension.equals("h5")) {
            networkHandle = m_kernel.loadNetworkFromH5(filePath);
        } else if (fileExtension.equals("json")) {
            networkHandle = m_kernel.loadNetworkSpecFromJson(filePath);
        } else if (fileExtension.equals("yaml")) {
            networkHandle = m_kernel.loadNetworkSpecFromYaml(filePath);
        } else {
            throw new IllegalArgumentException("Keras network reader only supports files of type h5, json and yaml.");
        }
        // TODO: we just want to bring the model specs back in the global
        // dict on the Python side -
        // no transfer to Java required
        m_kernel.extractNetworkSpec(networkHandle, m_backend.getTypeMap());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute(final DLKerasExecutableNetwork network, final Map<DLLayerDataSpec, DLLayerDataInput<?>> inputs,
        final Map<DLLayerDataSpec, DLLayerDataOutput<?>> outputs) throws RuntimeException {

        try {
            if (m_first) {
                init(network);
                m_first = false;
            }
            m_kernel.putNetworkInputs(inputs);
            m_kernel.executeNetwork(outputs.keySet().stream().findFirst().get().getName());
            m_kernel.fillNetworkOutputs(outputs);
        } catch (final Exception ex) {
            throw new RuntimeException("An exception occurred during execution of the Keras network.", ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws Exception {
        m_kernel.close();
    }

    private DLKerasPythonKernel setupKernel() throws IOException {
        final DLKerasPythonKernel kernel;
        try {
            kernel = new DLKerasPythonKernel(new DLKerasPythonKernelConfig());
        } catch (final Exception e) {
            throw new IOException("An exception occurred while setting up the Python kernel.", e);
        }
        return kernel;
    }
}
