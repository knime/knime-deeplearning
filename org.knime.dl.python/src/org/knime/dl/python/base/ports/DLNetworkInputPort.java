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
 * History
 *   Nov 17, 2020 (marcel): created
 */
package org.knime.dl.python.base.ports;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.dl.core.DLCancelable;
import org.knime.dl.core.DLCanceledExecutionException;
import org.knime.dl.core.DLInvalidEnvironmentException;
import org.knime.dl.core.DLInvalidSourceException;
import org.knime.dl.core.DLMissingExtensionException;
import org.knime.dl.core.DLNotCancelable;
import org.knime.dl.python.core.DLPythonContext;
import org.knime.dl.python.core.DLPythonDefaultContext;
import org.knime.dl.python.core.DLPythonNetwork;
import org.knime.dl.python.core.DLPythonNetworkHandle;
import org.knime.dl.python.core.DLPythonNetworkLoader;
import org.knime.dl.python.core.DLPythonNetworkLoaderRegistry;
import org.knime.dl.python.core.DLPythonNetworkPortObject;
import org.knime.python2.PythonModuleSpec;
import org.knime.python2.config.WorkspacePreparer;
import org.knime.python2.kernel.PythonKernel;
import org.knime.python2.ports.InputPort;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 */
public class DLNetworkInputPort implements InputPort {

    private final String m_variableName;

    public DLNetworkInputPort(final String variableName) {
        m_variableName = variableName;
    }

    @Override
    public String getVariableName() {
        return m_variableName;
    }

    @Override
    public double getExecuteProgressWeight() {
        return 0.5;
    }

    @Override
    public Collection<PythonModuleSpec> getRequiredModules() {
        return Collections.emptyList();
    }

    @Override
    public void configure(final PortObjectSpec inSpec) throws InvalidSettingsException {
        // Nothing to configure.
    }

    @Override
    public WorkspacePreparer prepareInDialog(final PortObjectSpec inSpec) throws NotConfigurableException {
        // Nothing to prepare here, see below.
        return null;
    }

    @Override
    public WorkspacePreparer prepareInDialog(final PortObject inObject) throws NotConfigurableException {
        if (inObject != null) {
            final DLPythonNetwork inNetwork;
            try {
                inNetwork = ((DLPythonNetworkPortObject<?>)inObject).getNetwork();
            } catch (final DLInvalidSourceException | IOException ex) {
                throw new NotConfigurableException(ex.getMessage(), ex);
            }
            return kernel -> {
                try {
                    setupNetwork(inNetwork, new DLPythonDefaultContext(kernel), DLNotCancelable.INSTANCE);
                } catch (final Exception ex) {
                    throw new IllegalStateException(
                        "Deep Learning network could not be loaded. Try again by pressing the \"Reset workspace\" button.",
                        ex);
                }
            };
        } else {
            throw new NotConfigurableException("Input deep learning network port object is missing.");
        }
    }

    @Override
    public void execute(final PortObject inObject, final PythonKernel kernel, final ExecutionMonitor monitor)
        throws Exception {
        throw new IllegalStateException("not yet implemented"); // TODO: implement
    }

    private <N extends DLPythonNetwork> void setupNetwork(final N inputNetwork, final DLPythonContext context,
        final DLCancelable cancelable) throws DLMissingExtensionException, DLInvalidSourceException,
        DLInvalidEnvironmentException, IOException, DLCanceledExecutionException {
        final DLPythonNetworkLoader<N> loader =
            DLPythonNetworkLoaderRegistry.getInstance().getNetworkLoader((Class<N>)inputNetwork.getClass()).orElseThrow(
                () -> new DLMissingExtensionException("Python back end '" + inputNetwork.getClass().getCanonicalName()
                    + "' could not be found. Are you missing a KNIME Deep Learning extension?"));
        final DLPythonNetworkHandle networkHandle = loader.load(inputNetwork, context, true, cancelable);
        final String networkHandleId = networkHandle.getIdentifier();
        try {
            context.executeInKernel("import DLPythonNetwork\n" + //
                "global " + m_variableName + "\n" + //
                m_variableName + " = DLPythonNetwork.get_network('" + networkHandleId + "').model", cancelable);
        } catch (final IOException e) {
            throw new IOException(
                "An error occurred while communicating with Python (while setting up the Python network).", e);
        }
    }
}
