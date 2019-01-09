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
import java.util.Set;

import org.knime.dl.core.DLContext;
import org.knime.dl.core.DLFixedTensorShape;
import org.knime.dl.core.DLNetwork;
import org.knime.dl.core.DLNetworkFixedSizeInputPreparer;
import org.knime.dl.core.DLTensorSpec;

/**
 * Represents the training back end for a certain network type. Creates {@link DLNetworkTrainingSession training
 * sessions} to train networks.
 *
 * @param <N> the {@link DLNetwork network} type for which to create {@link DLNetworkTrainingSession training sessions}
 * @param <CFG> the {@link DLTrainingConfig} that specifies how a network is trained within a training session
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public interface DLTrainingContext<N extends DLNetwork, CFG extends DLTrainingConfig> extends DLContext<N> {

	/**
	 * @return the identifier of this training context, not null, not empty, must be unique across all training contexts
	 */
	@Override
    default String getIdentifier() {
		return getClass().getCanonicalName();
	}

	/**
	 * @return the available optimizers in this training context
	 */
	Collection<? extends DLOptimizer> createOptimizers();

	/**
	 * @return the available loss functions in this training context
	 */
	Collection<? extends DLLossFunction> createLossFunctions();

	/**
	 * Creates a {@link DLNetworkTrainingSession training session} for a given {@link DLNetwork network}.
	 *
	 * @param network the network to train
	 * @param trainingConfig the training configuration that specifies how the network will be trained
	 * @param executionInputSpecs a set of fully defined tensor specs. The set of tensor specs must exactly match the
	 *            network's input tensor specs with respect to the identifiers of the contained specs. A tensor spec is
	 *            fully defined if it features a non-empty batch size and a {@link DLFixedTensorShape fixed tensor
	 *            shape}.
	 * @param trainingInputPreparer the training data preparer
	 * @param validationInputPreparer the validation data preparer, may be null in which case no validation will be
	 *            performed during training
	 * @return the created training session
	 * @throws IllegalArgumentException if failed to create the training session due to invalid arguments
	 */
    DLNetworkTrainingSession<?> createTrainingSession(N network, CFG trainingConfig,
        Set<DLTensorSpec> executionInputSpecs, DLNetworkFixedSizeInputPreparer trainingInputPreparer,
        DLNetworkFixedSizeInputPreparer validationInputPreparer);
}
