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
package org.knime.dl.core.training;

import java.util.Collection;

import org.knime.dl.core.DLNetwork;
import org.knime.dl.core.DLTensorFactory;
import org.knime.dl.core.execution.DLExecutableNetwork;

/**
 * Creates {@link DLExecutableNetwork executable deep learning networks}.
 *
 * @param <N> the {@link DLNetwork network} type from which to create trainable networks
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public interface DLTrainingContext<N extends DLNetwork, CFG extends DLTrainingConfig> {

	/**
	 * Returns the network type that's associated with this training context.
	 *
	 * @return the network type that's associated with this training context
	 */
	Class<N> getNetworkType();

	/**
	 * Returns the identifier of this training context which is neither null nor empty and must be unique across all
	 * training contexts.
	 *
	 * @return the identifier of this training context
	 */
	default String getIdentifier() {
		return getClass().getCanonicalName();
	}

	/**
	 * Returns the friendly name of this training context which is neither null nor empty and is suitable for
	 * presentation to the user.
	 *
	 * @return the friendly name of this training context
	 */
	String getName();

	// TODO: remove, register at combination of network type and "execution mode"/input type (local/BufferedDataTable
	// etc.)
	DLTensorFactory getTensorFactory();

	/**
	 * Returns the available {@link DLOptimizer optimizers} in this training context.
	 *
	 * @return the available optimizers in this training context
	 */
	Collection<? extends DLOptimizer> createOptimizers();

	/**
	 * Returns the available {@link DLLossFunction loss functions} in this training context.
	 *
	 * @return the available loss functions in this training context
	 */
	Collection<? extends DLLossFunction> createLossFunctions();

	/**
	 * Creates a {@link DLTrainableNetworkAdapter trainable network} given a {@link DLNetwork network}.
	 *
	 * @param network the network
	 * @throws RuntimeException if failed to create the trainable network
	 */
	DLTrainableNetworkAdapter trainable(N network, CFG trainingConfig) throws RuntimeException;
}
