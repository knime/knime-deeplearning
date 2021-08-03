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
 */

package org.knime.dl.python.base.node;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.knime.base.node.util.exttool.ExtToolOutputNodeModel;
import org.knime.core.data.filestore.FileStore;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortType;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.FlowVariable.Type;
import org.knime.dl.core.DLCancelable;
import org.knime.dl.core.DLCanceledExecutionException;
import org.knime.dl.core.DLInvalidEnvironmentException;
import org.knime.dl.core.DLInvalidSourceException;
import org.knime.dl.core.DLNetworkFileStoreLocation;
import org.knime.dl.python.core.DLPythonContext;
import org.knime.dl.python.core.DLPythonDefaultContext;
import org.knime.dl.python.core.DLPythonNetwork;
import org.knime.dl.python.core.DLPythonNetworkHandle;
import org.knime.dl.python.core.DLPythonNetworkLoader;
import org.knime.dl.python.core.DLPythonNetworkPortObject;
import org.knime.dl.python.prefs.DLPythonPreferences;
import org.knime.python2.PythonCommand;
import org.knime.python2.PythonModuleSpec;
import org.knime.python2.PythonVersion;
import org.knime.python2.config.PythonCommandConfig;
import org.knime.python2.config.PythonFlowVariableOptions;
import org.knime.python2.config.PythonSourceCodeConfig;
import org.knime.python2.extensions.serializationlibrary.SerializationOptions;
import org.knime.python2.kernel.PythonCancelable;
import org.knime.python2.kernel.PythonCanceledExecutionException;
import org.knime.python2.kernel.PythonIOException;
import org.knime.python2.kernel.PythonKernel;
import org.knime.python2.kernel.PythonKernelOptions;
import org.knime.python2.kernel.PythonKernelQueue;

/**
 * Shamelessly copied and pasted from knime-python.
 *
 * @param <CFG> the python source code config type
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public abstract class DLPythonNodeModel<CFG extends PythonSourceCodeConfig> extends ExtToolOutputNodeModel {

	private CFG m_config = createConfig();

    private final PythonCommandConfig m_executableConfig = new PythonCommandConfig(PythonVersion.PYTHON3,
        DLPythonPreferences::getCondaInstallationPath, DLPythonPreferences::getPythonCommandPreference);

	private final LinkedList<String> m_stdout;

	private final LinkedList<String> m_stderr;

	public DLPythonNodeModel(final PortType[] inPortTypes, final PortType[] outPortTypes) {
		super(inPortTypes, outPortTypes);
		m_stdout = new LinkedList<>();
		m_stderr = new LinkedList<>();
	}

	protected abstract CFG createConfig();

    /**
     * Update the standard out and standard error view.
     *
     * @param output stdout at index 0 and stderr at index 1.
     */
    protected final void updateStdoutStderr(final String[] output) {
        // Stdout
        if (!output[0].isEmpty()) {
            final String[] stdout = output[0].split("\n");
            m_stdout.addAll(Arrays.asList(stdout));
            setExternalOutput(m_stdout);
        }

        // Stderr
        if (!output[1].isEmpty()) {
            final String[] stderr = output[1].split("\n");
            m_stderr.addAll(Arrays.asList(stderr));
            setExternalErrorOutput(m_stderr);
        }
    }

	protected final CFG getConfig() {
		return m_config;
	}

    protected PythonCommand getPythonCommand() {
        return m_executableConfig.getCommand();
    }

    protected PythonKernelOptions getKernelOptions() {
        final PythonCommand pythonCommand = getPythonCommand();

        final CFG config = getConfig();
        final String serializerId = new PythonFlowVariableOptions(getAvailableFlowVariables()).getSerializerId()
            .orElse(DLPythonPreferences.getSerializerPreference());
        final SerializationOptions serializationOptions =
            new SerializationOptions(config.getChunkSize(), config.isConvertingMissingToPython(),
                config.isConvertingMissingFromPython(), config.getSentinelOption(), config.getSentinelValue())
                    .forSerializerId(serializerId);

        return new PythonKernelOptions(PythonVersion.PYTHON3, null, pythonCommand, serializationOptions);
    }

    protected DLPythonContext getNextContextFromQueue(final PythonCancelable cancelable)
        throws PythonCanceledExecutionException, PythonIOException {
        return getNextContextFromQueue(Collections.emptySet(), Collections.emptySet(), cancelable);
    }

    protected DLPythonContext getNextContextFromQueue(final Set<PythonModuleSpec> requiredAdditionalModules,
        final PythonCancelable cancelable) throws PythonCanceledExecutionException, PythonIOException {
        return getNextContextFromQueue(requiredAdditionalModules, Collections.emptySet(), cancelable);
    }

    protected DLPythonContext getNextContextFromQueue(final Set<PythonModuleSpec> requiredAdditionalModules,
        final Set<PythonModuleSpec> optionalAdditionalModules, final PythonCancelable cancelable)
        throws PythonCanceledExecutionException, PythonIOException {
        final PythonKernelOptions options = getKernelOptions();
        final PythonCommand command = options.getUsePython3() //
            ? options.getPython3Command() //
            : options.getPython2Command();
        final PythonKernel kernel = PythonKernelQueue.getNextKernel(command, requiredAdditionalModules,
            optionalAdditionalModules, options, cancelable);
        return new DLPythonDefaultContext(kernel);
    }

	/**
	 * Push new variables to the stack. Only pushes new variables to the stack if they are new or changed in type or
	 * value.
	 *
	 * @param newVariables The flow variables to push
	 */
	protected void addNewVariables(final Collection<FlowVariable> newVariables) {
		final Map<String, FlowVariable> flowVariables = getAvailableFlowVariables();
		for (final FlowVariable variable : newVariables) {
			// Only push if variable is new or has changed type or value
			boolean push = true;
			if (flowVariables.containsKey(variable.getName())) {
				// Old variable with the name exists
				final FlowVariable oldVariable = flowVariables.get(variable.getName());
				if (oldVariable.getType().equals(variable.getType())) {
					// Old variable has the same type
					if (variable.getType().equals(Type.INTEGER)) {
						if (oldVariable.getIntValue() == variable.getIntValue()) {
							// Old variable has the same value
							push = false;
						}
					} else if (variable.getType().equals(Type.DOUBLE)) {
						if (new Double(oldVariable.getDoubleValue()).equals(new Double(variable.getDoubleValue()))) {
							// Old variable has the same value
							push = false;
						}
					} else if (variable.getType().equals(Type.STRING)) {
						if (oldVariable.getStringValue().equals(variable.getStringValue())) {
							// Old variable has the same value
							push = false;
						}
					}
				}
			}
			if (push) {
				if (variable.getType().equals(Type.INTEGER)) {
					pushFlowVariableInt(variable.getName(), variable.getIntValue());
				} else if (variable.getType().equals(Type.DOUBLE)) {
					pushFlowVariableDouble(variable.getName(), variable.getDoubleValue());
				} else if (variable.getType().equals(Type.STRING)) {
					pushFlowVariableString(variable.getName(), variable.getStringValue());
				}
			}
		}
	}

    protected <N extends DLPythonNetwork> DLPythonNetworkPortObject<? extends DLPythonNetwork> createOutputPortObject(
        final DLPythonNetworkLoader<N> loader, final DLPythonNetworkHandle handle, final FileStore fileStore,
        final DLPythonContext context, final DLCancelable cancelable) throws DLInvalidSourceException, DLInvalidEnvironmentException, IOException, DLCanceledExecutionException {
        final N network = loader.fetch(handle, new DLNetworkFileStoreLocation(fileStore), context, cancelable);
        return loader.createPortObject(network, fileStore);
    }

	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
		m_config.saveTo(settings);
		m_executableConfig.saveSettingsTo(settings);
	}

	@Override
	protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
		final CFG config = createConfig();
		config.loadFrom(settings);
		m_executableConfig.loadSettingsFrom(settings);
	}

	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
		final CFG config = createConfig();
		config.loadFrom(settings);
		m_config = config;
		m_executableConfig.loadSettingsFrom(settings);
	}

    @Override
    protected void reset() {
        super.reset();
        m_stdout.clear();
        m_stderr.clear();
    }
}
