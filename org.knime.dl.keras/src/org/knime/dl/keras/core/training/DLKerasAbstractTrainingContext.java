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

import static com.google.common.base.Preconditions.checkNotNull;
import static org.knime.dl.util.DLUtils.Preconditions.checkNotNullOrEmpty;

import java.util.Arrays;
import java.util.Collection;

import org.knime.dl.core.DLTensorFactory;
import org.knime.dl.core.DLTensorRegistry;
import org.knime.dl.keras.core.DLKerasNetwork;
import org.knime.dl.keras.core.training.DLKerasCallback.DLKerasEarlyStopping;
import org.knime.dl.keras.core.training.DLKerasCallback.DLKerasReduceLROnPlateau;
import org.knime.dl.keras.core.training.DLKerasCallback.DLKerasTerminateOnNaN;
import org.knime.dl.keras.core.training.DLKerasLossFunction.DLKerasBinaryCrossEntropy;
import org.knime.dl.keras.core.training.DLKerasLossFunction.DLKerasCategoricalCrossEntropy;
import org.knime.dl.keras.core.training.DLKerasLossFunction.DLKerasCategoricalHinge;
import org.knime.dl.keras.core.training.DLKerasLossFunction.DLKerasCosineProximity;
import org.knime.dl.keras.core.training.DLKerasLossFunction.DLKerasHinge;
import org.knime.dl.keras.core.training.DLKerasLossFunction.DLKerasKullbackLeiblerDivergence;
import org.knime.dl.keras.core.training.DLKerasLossFunction.DLKerasLogCosh;
import org.knime.dl.keras.core.training.DLKerasLossFunction.DLKerasMeanAbsoluteError;
import org.knime.dl.keras.core.training.DLKerasLossFunction.DLKerasMeanAbsolutePercentageError;
import org.knime.dl.keras.core.training.DLKerasLossFunction.DLKerasMeanSquaredError;
import org.knime.dl.keras.core.training.DLKerasLossFunction.DLKerasMeanSquaredLogarithmicError;
import org.knime.dl.keras.core.training.DLKerasLossFunction.DLKerasPoisson;
import org.knime.dl.keras.core.training.DLKerasLossFunction.DLKerasSparseCategoricalCrossEntropy;
import org.knime.dl.keras.core.training.DLKerasLossFunction.DLKerasSquaredHinge;
import org.knime.dl.keras.core.training.DLKerasMetric.DLKerasAccuracy;
import org.knime.dl.keras.core.training.DLKerasOptimizer.DLKerasAdadelta;
import org.knime.dl.keras.core.training.DLKerasOptimizer.DLKerasAdagrad;
import org.knime.dl.keras.core.training.DLKerasOptimizer.DLKerasAdam;
import org.knime.dl.keras.core.training.DLKerasOptimizer.DLKerasAdamax;
import org.knime.dl.keras.core.training.DLKerasOptimizer.DLKerasNadam;
import org.knime.dl.keras.core.training.DLKerasOptimizer.DLKerasRMSProp;
import org.knime.dl.keras.core.training.DLKerasOptimizer.DLKerasStochasticGradientDescent;

/**
 * @param <N> the {@link DLKerasNetwork Keras network} type for which to create {@link DLKerasNetworkTrainingSession
 *            training sessions} * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public abstract class DLKerasAbstractTrainingContext<N extends DLKerasNetwork> implements DLKerasTrainingContext<N> {

	private final Class<N> m_networkType;

	private final String m_name;

	private final DLTensorFactory m_layerDataFactory;

	/**
	 * @param networkType the associated Keras network type
	 * @param name the friendly name of this training context, not null, not empty, suitable to be displayed to the user
	 */
	protected DLKerasAbstractTrainingContext(final Class<N> networkType, final String name) {
		m_networkType = checkNotNull(networkType);
		m_name = checkNotNullOrEmpty(name);
		m_layerDataFactory = DLTensorRegistry.getInstance().getTensorFactory(m_networkType)
				.orElseThrow(() -> new IllegalStateException("Deep learning network type '" + m_networkType
						+ "' is not supported. No layer data factory found."));
	}

	@Override
	public Class<N> getNetworkType() {
		return m_networkType;
	}

	@Override
	public String getName() {
		return m_name;
	}

	@Override
	public DLTensorFactory getTensorFactory() {
		return m_layerDataFactory;
	}

	@Override
	public Collection<DLKerasOptimizer> createOptimizers() {
		return Arrays.asList( //
				new DLKerasStochasticGradientDescent(), //
				new DLKerasRMSProp(), //
				new DLKerasAdagrad(), //
				new DLKerasAdadelta(), //
				new DLKerasAdam(), //
				new DLKerasAdamax(), //
				new DLKerasNadam());
	}

	@Override
	public Collection<DLKerasLossFunction> createLossFunctions() {
		return Arrays.asList( //
				new DLKerasMeanSquaredError(), //
				new DLKerasMeanAbsoluteError(), //
				new DLKerasMeanAbsolutePercentageError(), //
				new DLKerasMeanSquaredLogarithmicError(), //
				new DLKerasSquaredHinge(), //
				new DLKerasHinge(), //
				new DLKerasCategoricalHinge(), //
				new DLKerasLogCosh(), //
				new DLKerasCategoricalCrossEntropy(), //
				new DLKerasSparseCategoricalCrossEntropy(), //
				new DLKerasBinaryCrossEntropy(), //
				new DLKerasKullbackLeiblerDivergence(), //
				new DLKerasPoisson(), //
				new DLKerasCosineProximity());
	}

	@Override
	public Collection<DLKerasMetric> createMetrics() {
		return Arrays.asList( //
				new DLKerasAccuracy());
		// TODO: pending
		// new DLKerasMeanSquaredError(), //
		// new DLKerasMeanAbsoluteError(), //
		// new DLKerasMeanAbsolutePercentageError(), //
		// new DLKerasMeanSquaredLogarithmicError(), //
		// new DLKerasSquaredHinge(), //
		// new DLKerasHinge(), //
		// new DLKerasLogCosh(), //
		// new DLKerasCategoricalCrossEntropy(), //
		// new DLKerasSparseCategoricalCrossEntropy(), //
		// new DLKerasBinaryCrossEntropy(), //
		// new DLKerasKullbackLeiblerDivergence(), //
		// new DLKerasPoisson(), //
		// new DLKerasCosineProximity());
	}

	@Override
	public Collection<DLKerasCallback> createCallbacks() {
		return Arrays.asList( //
				new DLKerasTerminateOnNaN(), //
				new DLKerasEarlyStopping(), //
				new DLKerasReduceLROnPlateau());
	}
}
