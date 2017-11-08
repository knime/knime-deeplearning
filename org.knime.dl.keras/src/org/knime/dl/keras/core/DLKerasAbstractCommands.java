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

import org.knime.dl.core.DLInvalidEnvironmentException;
import org.knime.dl.keras.core.training.DLKerasTrainingConfig;
import org.knime.dl.python.core.DLPythonAbstractCommands;
import org.knime.dl.python.core.DLPythonContext;
import org.knime.dl.python.core.DLPythonNetworkHandle;
import org.knime.dl.python.util.DLPythonSourceCodeBuilder;
import org.knime.dl.python.util.DLPythonUtils;

/**
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
public abstract class DLKerasAbstractCommands extends DLPythonAbstractCommands {

	protected DLKerasAbstractCommands() {
	}

	protected DLKerasAbstractCommands(final DLPythonContext context) {
		super(context);
	}

	protected abstract String getLoadNetworkFromJsonCode(String path);

	protected abstract String getLoadNetworkFromYamlCode(String path);

	public DLPythonNetworkHandle loadNetworkFromJson(final String path)
			throws DLInvalidEnvironmentException, IOException {
		getContext().executeInKernel(getLoadNetworkFromJsonCode(path));
		// TODO: we should get the model name (= handle identifier) from Python
		return new DLPythonNetworkHandle(DEFAULT_MODEL_NAME);
	}

	public DLPythonNetworkHandle loadNetworkFromYaml(final String path)
			throws DLInvalidEnvironmentException, IOException {
		getContext().executeInKernel(getLoadNetworkFromYamlCode(path));
		// TODO: we should get the model name (= handle identifier) from Python
		return new DLPythonNetworkHandle(DEFAULT_MODEL_NAME);
	}

	public void setNetworkTrainingConfig(final DLPythonNetworkHandle handle, final DLKerasTrainingConfig config)
			throws DLInvalidEnvironmentException, IOException {
		final DLPythonSourceCodeBuilder b = DLPythonUtils.createSourceCodeBuilder() //
				.a("from DLKerasNetwork import DLKerasTrainingConfig") //
				.n("config = DLKerasTrainingConfig()") //
				.n("config.batch_size = ").a(config.getBatchSize()) //
				.n("config.epochs = ").a(config.getEpochs()) //
				// TODO: how to import dependencies (here: of optimizer and losses) in a generic way?
				.n("import keras") //
				.n("config.optimizer = ").a(config.getOptimizer().getBackendRepresentation()) //
				.n(config.getLosses().entrySet(),
						e -> "config.loss[" + DLPythonUtils.toPython(e.getKey().getName()) + "] = "
								+ e.getValue().getBackendRepresentation()) //
				.n(config.getCallbacks(), c -> "config.callbacks.append(" + c.getBackendRepresentation() + ")");
		if (config.getValidationSplit().isPresent()) {
			b.n("config.validation_split = ").a(config.getValidationSplit().getAsDouble());
		}
		b.n("config.shuffle = ").a(config.getShuffle()) //
				.n("import DLPythonNetwork") //
				.n("network = DLPythonNetwork.get_network(").as(handle.getIdentifier()).a(")")
				.n("network.spec.training_config = config");
		getContext().executeInKernel(b.toString());
	}
}
