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
 * History
 *   Jun 13, 2017 (marcel): created
 */
package org.knime.dl.keras.base.nodes.learner;

import java.util.Collection;
import java.util.stream.Collectors;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.dl.base.settings.AbstractConfig;
import org.knime.dl.base.settings.AbstractConfigEntry;
import org.knime.dl.base.settings.ConfigEntry;
import org.knime.dl.base.settings.DefaultConfigEntry;
import org.knime.dl.core.DLNetwork;
import org.knime.dl.core.training.DLTrainingContextRegistry;
import org.knime.dl.keras.core.training.DLKerasCallback.DLKerasEarlyStopping;
import org.knime.dl.keras.core.training.DLKerasCallback.DLKerasReduceLROnPlateau;
import org.knime.dl.keras.core.training.DLKerasCallback.DLKerasTerminateOnNaN;
import org.knime.dl.keras.core.training.DLKerasOptimizer;
import org.knime.dl.keras.core.training.DLKerasTrainingContext;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
final class DLKerasLearnerGeneralConfig extends AbstractConfig {

	static final String CFG_KEY_ROOT = "general_settings";

	static final String CFG_KEY_TRAINING_CONTEXT = "backend";

	static final String CFG_KEY_EPOCHS = "epochs";

	static final String CFG_KEY_BATCH_SIZE = "batch_size";

	static final String CFG_KEY_OPTIMIZER = "optimizer";

	static final String CFG_KEY_CLIP_NORM = "clip_norm";

	static final String CFG_KEY_CLIP_VALUE = "clip_value";

	static final String CFG_KEY_TERMINATE_ON_NAN = "terminate_on_nan";

	static final String CFG_KEY_EARLY_STOPPING = "early_stopping";

	static final String CFG_KEY_REDUCE_LR_ON_PLATEAU = "reduce_lr_on_plateau";

	static Collection<DLKerasTrainingContext<?>> getAvailableTrainingContexts(
			final Class<? extends DLNetwork> networkType) {
		return DLTrainingContextRegistry.getInstance().getTrainingContextsForNetworkType((networkType)) //
				.stream() //
				.filter(tc -> tc instanceof DLKerasTrainingContext) //
				.map(tc -> (DLKerasTrainingContext<?>) tc) //
				.collect(Collectors.toList());
	}

	@SuppressWarnings("rawtypes") // Java limitation
	DLKerasLearnerGeneralConfig() {
		super(CFG_KEY_ROOT);
		put(new AbstractConfigEntry<DLKerasTrainingContext>(CFG_KEY_TRAINING_CONTEXT, DLKerasTrainingContext.class) {

			@Override
			protected void saveEntry(final NodeSettingsWO settings)
					throws InvalidSettingsException, UnsupportedOperationException {
				final String identifier = m_value != null ? m_value.getIdentifier() : "null";
				settings.addString(getEntryKey(), identifier);
			}

			@Override
			protected void loadEntry(final NodeSettingsRO settings)
					throws InvalidSettingsException, IllegalStateException, UnsupportedOperationException {
				final String trainingContextIdentifier = settings.getString(getEntryKey());
				if (!trainingContextIdentifier.equals("null")) {
					m_value = (DLKerasTrainingContext) DLTrainingContextRegistry.getInstance()
							.getTrainingContext(trainingContextIdentifier)
							.orElseThrow(() -> new InvalidSettingsException("Learner back end '"
									+ trainingContextIdentifier
									+ "' could not be found. Are you missing a KNIME Deep Learning extension?"));
				}
			}
		});
		put(new DefaultConfigEntry<>(CFG_KEY_EPOCHS, Integer.class, 1));
		put(new DefaultConfigEntry<>(CFG_KEY_BATCH_SIZE, Integer.class, 100));

		put(new AbstractConfigEntry<DLKerasOptimizer>(CFG_KEY_OPTIMIZER, DLKerasOptimizer.class) {

			@Override
			protected void saveEntry(final NodeSettingsWO settings)
					throws InvalidSettingsException, UnsupportedOperationException {
				final NodeSettingsWO optimizerSettings = settings.addNodeSettings(getEntryKey());
				if (m_value != null) {
					optimizerSettings.addString("identifier", m_value.getClass().getCanonicalName());
					m_value.saveToSettings(optimizerSettings);
				} else {
					optimizerSettings.addString("identifier", "null");
				}
			}

			@Override
			protected void loadEntry(final NodeSettingsRO settings)
					throws InvalidSettingsException, IllegalStateException, UnsupportedOperationException {
				final NodeSettingsRO optimizerSettings = settings.getNodeSettings(getEntryKey());
				final String optimizerIdentifier = optimizerSettings.getString("identifier");
				if (!optimizerIdentifier.equals("null")) {
					m_value = getTrainingContextEntry().getValue().createOptimizers().stream()
							.filter(o -> o.getClass().getCanonicalName().equals(optimizerIdentifier))//
							.findFirst() //
							.orElseThrow(() -> new InvalidSettingsException("Optimizer '" + optimizerIdentifier
									+ "' could not be found. Are you missing a KNIME Deep Learning extension?"));
					m_value.loadFromSettings(optimizerSettings);
				}
			}
		});

		put(new DefaultConfigEntry<>(CFG_KEY_CLIP_NORM, Double.class, 1.0, false));
		put(new DefaultConfigEntry<>(CFG_KEY_CLIP_VALUE, Double.class, 1.0, false));

		put(new DefaultConfigEntry<>(CFG_KEY_TERMINATE_ON_NAN, DLKerasTerminateOnNaN.class, new DLKerasTerminateOnNaN(),
				false));
		put(new DefaultConfigEntry<>(CFG_KEY_EARLY_STOPPING, DLKerasEarlyStopping.class, new DLKerasEarlyStopping(),
				false));
		put(new DefaultConfigEntry<>(CFG_KEY_REDUCE_LR_ON_PLATEAU, DLKerasReduceLROnPlateau.class,
				new DLKerasReduceLROnPlateau(), false));
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	ConfigEntry<DLKerasTrainingContext<?>> getTrainingContextEntry() {
		return (ConfigEntry) get(CFG_KEY_TRAINING_CONTEXT, DLKerasTrainingContext.class);
	}

	ConfigEntry<Integer> getEpochsEntry() {
		return get(CFG_KEY_EPOCHS, Integer.class);
	}

	ConfigEntry<Integer> getBatchSizeEntry() {
		return get(CFG_KEY_BATCH_SIZE, Integer.class);
	}

	ConfigEntry<DLKerasOptimizer> getOptimizerEntry() {
		return get(CFG_KEY_OPTIMIZER, DLKerasOptimizer.class);
	}

	ConfigEntry<Double> getClipNormEntry() {
		return get(CFG_KEY_CLIP_NORM, Double.class);
	}

	ConfigEntry<Double> getClipValueEntry() {
		return get(CFG_KEY_CLIP_VALUE, Double.class);
	}

	ConfigEntry<DLKerasTerminateOnNaN> getTerminateOnNaNEntry() {
		return get(CFG_KEY_TERMINATE_ON_NAN, DLKerasTerminateOnNaN.class);
	}

	ConfigEntry<DLKerasEarlyStopping> getEarlyStoppingEntry() {
		return get(CFG_KEY_EARLY_STOPPING, DLKerasEarlyStopping.class);
	}

	ConfigEntry<DLKerasReduceLROnPlateau> getReduceLROnPlateauEntry() {
		return get(CFG_KEY_REDUCE_LR_ON_PLATEAU, DLKerasReduceLROnPlateau.class);
	}

	void copyClipSettingsToOptimizer() {
		final DLKerasOptimizer optimizer = getOptimizerEntry().getValue();
		if (optimizer != null) {
			optimizer.setClipNorm(getClipNormEntry());
			optimizer.setClipValue(getClipValueEntry());
		}
	}
}
