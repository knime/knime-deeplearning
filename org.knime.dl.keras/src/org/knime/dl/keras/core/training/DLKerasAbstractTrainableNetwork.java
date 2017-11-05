/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 */
package org.knime.dl.keras.core.training;

import java.io.IOException;
import java.net.URL;

import org.knime.core.data.filestore.FileStore;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.NodeLogger;
import org.knime.dl.base.portobjects.DLNetworkPortObject;
import org.knime.dl.core.DLInvalidEnvironmentException;
import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.keras.base.portobjects.DLKerasNetworkPortObject;
import org.knime.dl.keras.core.DLKerasAbstractCommands;
import org.knime.dl.keras.core.DLKerasNetwork;
import org.knime.dl.keras.core.execution.DLKerasAbstractExecutableNetwork;
import org.knime.dl.python.core.DLPythonNetworkHandle;
import org.knime.dl.python.core.DLPythonNetworkLoader;
import org.knime.dl.python.core.DLPythonNetworkLoaderRegistry;
import org.knime.dl.python.core.training.DLPythonAbstractTrainableNetwork;

/**
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
public abstract class DLKerasAbstractTrainableNetwork<N extends DLKerasNetwork, C extends DLKerasAbstractCommands>
	extends DLPythonAbstractTrainableNetwork<N, DLKerasTrainingConfig, C> implements DLKerasTrainableNetwork {

	protected DLKerasAbstractTrainableNetwork(final N network, final DLKerasTrainingConfig trainingConfig) {
		super(network, trainingConfig);
		boolean hasFixedBatchSizes = false;
		boolean hasVariableBatchSizes = false;
		for (final DLTensorSpec inputSpec : network.getSpec().getInputSpecs()) {
			if (inputSpec.getBatchSize().isPresent()) {
				hasFixedBatchSizes = true;
			} else {
				hasVariableBatchSizes = true;
			}
		}
		if (hasFixedBatchSizes && hasVariableBatchSizes) {
			NodeLogger.getLogger(DLKerasAbstractExecutableNetwork.class)
					.warn("Input network has both inputs with pre-defined batch size and variable batch size. "
							+ "This may not be supported by Keras and could lead to a runtime error.");
		}
	}

	@Override
	protected void setNetworkTrainingConfig(final DLPythonNetworkHandle handle, final C commands,
			final DLKerasTrainingConfig config) throws DLInvalidEnvironmentException, IOException {
		commands.setNetworkTrainingConfig(handle, config);
	}

	@Override
	public DLKerasNetworkPortObject getTrainedNetwork(final ExecutionContext exec) throws Exception {
		if (m_commands == null) {
			throw new IllegalStateException("Network was not trained, yet.");
		}
		final DLPythonNetworkLoader<? extends DLKerasNetwork> loader = DLPythonNetworkLoaderRegistry.getInstance()
				.getNetworkLoader(m_network.getClass()).get();
		final FileStore fileStore = DLNetworkPortObject.createFileStoreForSaving(loader.getSaveModelURLExtension(),
				exec);
		final URL fileStoreURL = fileStore.getFile().toURI().toURL();
		loader.save(m_handle, fileStoreURL, m_commands.getContext());
		if (!fileStore.getFile().exists()) {
			throw new IllegalStateException("Failed to save trained Keras deep learning network.");
		}
		return new DLKerasNetworkPortObject(loader.fetch(m_handle, fileStoreURL, m_commands.getContext()), fileStore);
	}
}
