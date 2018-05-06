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
package org.knime.dl.keras.core.training;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.knime.dl.core.DLTensorId;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public class DLKerasDefaultTrainingConfig implements DLKerasTrainingConfig {

	private final int m_epochs;
	private final long m_batchSize;
	private final long m_validationBatchSize;
	private final DLKerasOptimizer m_optimizer;
	private final Map<DLTensorId, DLKerasLossFunction> m_losses;
	private final Collection<DLKerasCallback> m_callbacks;

	/**
	 * @param epochs the number of times to iterate over the training data before training is finished. Note that the
	 *            actual number of executed epochs can be smaller in case of early stopping.
	 * @param batchSize the number of training samples to use for a single training step
	 * @param validationBatchSize may be null in which case the validation batch size defaults the to batch size. This
	 *            value only matters if performing model evaluation during training.
	 * @param optimizer the optimizer that is used for model updating
	 * @param losses a mapping of network outputs to loss functions. There must be a mapping for each of the outputs of
	 *            the network that will be trained.
	 * @param callbacks may be null or empty in which case it defaults to an empty list
	 */
	public DLKerasDefaultTrainingConfig(final int epochs, final int batchSize, final Integer validationBatchSize,
			final DLKerasOptimizer optimizer, final Map<DLTensorId, DLKerasLossFunction> losses,
			final Collection<DLKerasCallback> callbacks) {
		m_epochs = epochs;
		m_batchSize = batchSize;
		m_validationBatchSize = validationBatchSize != null ? validationBatchSize : batchSize;
		m_optimizer = optimizer;
		m_losses = Collections.unmodifiableMap(new HashMap<>(losses));
		m_callbacks = callbacks != null ? Collections.unmodifiableCollection(new ArrayList<>(callbacks))
				: Collections.emptyList();
	}

	@Override
	public int getEpochs() {
		return m_epochs;
	}

	@Override
	public long getBatchSize() {
		return m_batchSize;
	}

	@Override
	public long getValidationBatchSize() {
		return m_validationBatchSize;
	}

	@Override
	public DLKerasOptimizer getOptimizer() {
		return m_optimizer;
	}

	@Override
    public Map<DLTensorId, DLKerasLossFunction> getLosses() {
		return m_losses;
	}

	@Override
	public Collection<DLKerasCallback> getCallbacks() {
		return m_callbacks;
	}
}
