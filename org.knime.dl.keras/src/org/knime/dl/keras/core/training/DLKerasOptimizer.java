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
 */
package org.knime.dl.keras.core.training;

import static org.knime.dl.util.DLUtils.Preconditions.checkNotNullOrEmpty;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.knime.dl.base.nodes.AbstractGridBagDialogComponentGroup;
import org.knime.dl.base.nodes.IDialogComponentGroup;
import org.knime.dl.base.settings.AbstractConfig;
import org.knime.dl.base.settings.Config;
import org.knime.dl.base.settings.ConfigEntry;
import org.knime.dl.base.settings.ConfigUtil;
import org.knime.dl.base.settings.DefaultConfigEntry;
import org.knime.dl.core.training.DLOptimizer;
import org.knime.dl.python.util.DLPythonUtils;

/**
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
public interface DLKerasOptimizer extends DLOptimizer, Config {

	static String DEFAULT_CFG_KEY = "optimizer";

	String getKerasIdentifier();

	ConfigEntry<Double> getClipNormEntry();

	ConfigEntry<Double> getClipValueEntry();

	IDialogComponentGroup getParameterDialogGroup();

	@Override
	String getBackendRepresentation();

	public abstract static class DLKerasAbstractOptimizer extends AbstractConfig implements DLKerasOptimizer {

		static final String CFG_KEY_CLIP_NORM = "clip_norm";

		static final String CFG_KEY_CLIP_VALUE = "clip_value";

		protected final String m_name;

		protected final String m_kerasIdentifier;

		protected IDialogComponentGroup m_dialogComponentGroup;

		protected DLKerasAbstractOptimizer(final String configKey, final String name, final String kerasIdentifier) {
			super(configKey);
			m_name = checkNotNullOrEmpty(name);
			m_kerasIdentifier = checkNotNullOrEmpty(kerasIdentifier);

			put(new DefaultConfigEntry<>(CFG_KEY_CLIP_NORM, Double.class, 1.0, false));
			put(new DefaultConfigEntry<>(CFG_KEY_CLIP_VALUE, Double.class, 1.0, false));
		}

		protected abstract void populateNamedParameters(final Map<String, String> namedParams);

		protected abstract IDialogComponentGroup getParameterDialogGroupInternal();

		@Override
		public String getName() {
			return m_name;
		}

		@Override
		public String getKerasIdentifier() {
			return m_kerasIdentifier;
		}

		@Override
		public ConfigEntry<Double> getClipNormEntry() {
			return get(CFG_KEY_CLIP_NORM, Double.class);
		}

		@Override
		public ConfigEntry<Double> getClipValueEntry() {
			return get(CFG_KEY_CLIP_VALUE, Double.class);
		}

		@Override
		public String getBackendRepresentation() {
			final LinkedHashMap<String, String> namedParams = new LinkedHashMap<>();
			populateNamedParameters(namedParams);
			final ConfigEntry<Double> clipNorm = getClipNormEntry();
			if (clipNorm.getEnabled()) {
				namedParams.put("clipnorm", DLPythonUtils.toPython(clipNorm.getValue()));
			}
			final ConfigEntry<Double> clipValue = getClipValueEntry();
			if (clipValue.getEnabled()) {
				namedParams.put("clipvalue", DLPythonUtils.toPython(clipValue.getValue()));
			}
			return m_kerasIdentifier + "(" + namedParams.entrySet().stream()
					.map(np -> np.getKey() + "=" + np.getValue()).collect(Collectors.joining(", ")) + ")";
		}

		@Override
		public IDialogComponentGroup getParameterDialogGroup() {
			if (m_dialogComponentGroup == null) {
				m_dialogComponentGroup = getParameterDialogGroupInternal();
			}
			return m_dialogComponentGroup;
		}
	}

	// TODO: we should add a "since" attribute to these optimizers to enable checking if they're available for the local
	// Keras installation

	public static class DLKerasStochasticGradientDescent extends DLKerasAbstractOptimizer {

		static final String CFG_KEY_LR = "lr";

		static final String CFG_KEY_MOMENTUM = "momentum";

		static final String CFG_KEY_DECAY = "decay";

		static final String CFG_KEY_NESTEROV = "nesterov";

		public DLKerasStochasticGradientDescent() {
			super(DEFAULT_CFG_KEY, "Stochastic gradient descent", "keras.optimizers.SGD");
			setEntryValue(CFG_KEY_LR, Double.class, 0.01);
			setEntryValue(CFG_KEY_MOMENTUM, Double.class, 0d);
			setEntryValue(CFG_KEY_DECAY, Double.class, 0d);
			setEntryValue(CFG_KEY_NESTEROV, Boolean.class, false);
		}

		@Override
		protected void populateNamedParameters(final Map<String, String> namedParams) {
			namedParams.put("lr", DLPythonUtils.toPython(getEntryValue(CFG_KEY_LR, Double.class)));
			namedParams.put("momentum", DLPythonUtils.toPython(getEntryValue(CFG_KEY_MOMENTUM, Double.class)));
			namedParams.put("decay", DLPythonUtils.toPython(getEntryValue(CFG_KEY_DECAY, Double.class)));
			namedParams.put("nesterov", DLPythonUtils.toPython(getEntryValue(CFG_KEY_NESTEROV, Boolean.class)));
		}

		@Override
		protected IDialogComponentGroup getParameterDialogGroupInternal() {
			return new DLKerasStochasticGradientDescentDialog(this);
		}

		private static class DLKerasStochasticGradientDescentDialog extends AbstractGridBagDialogComponentGroup {

			private DLKerasStochasticGradientDescentDialog(final DLKerasStochasticGradientDescent model) {
				addNumberSpinnerRowComponent(ConfigUtil.toSettingsModelDoubleBounded(
						model.get(CFG_KEY_LR, Double.class), 0, Double.MAX_VALUE), "Learning rate", 0.01);
				addNumberSpinnerRowComponent(ConfigUtil.toSettingsModelDoubleBounded(
						model.get(CFG_KEY_MOMENTUM, Double.class), 0, Double.MAX_VALUE), "Momentum", 0.01);
				addNumberSpinnerRowComponent(ConfigUtil.toSettingsModelDoubleBounded(
						model.get(CFG_KEY_DECAY, Double.class), 0, Double.MAX_VALUE), "Learning rate decay", 0.001);
				addCheckboxRow(ConfigUtil.toSettingsModelBoolean(model.get(CFG_KEY_NESTEROV, Boolean.class)),
						"Use Nesterov momentum?", false);
			}
		}
	}

	public static class DLKerasRMSProp extends DLKerasAbstractOptimizer {

		static final String CFG_KEY_LR = "lr";

		static final String CFG_KEY_RHO = "rho";

		static final String CFG_KEY_EPSILON = "epsilon";

		static final String CFG_KEY_DECAY = "decay";

		public DLKerasRMSProp() {
			super(DEFAULT_CFG_KEY, "RMSProp", "keras.optimizers.RMSProp");
			setEntryValue(CFG_KEY_LR, Double.class, 0.001);
			setEntryValue(CFG_KEY_RHO, Double.class, 0.9);
			setEntryValue(CFG_KEY_EPSILON, Double.class, 1e-8);
			setEntryValue(CFG_KEY_DECAY, Double.class, 0d);
		}

		@Override
		public void populateNamedParameters(final Map<String, String> namedParams) {
			namedParams.put("lr", DLPythonUtils.toPython(getEntryValue(CFG_KEY_LR, Double.class)));
			namedParams.put("rho", DLPythonUtils.toPython(getEntryValue(CFG_KEY_RHO, Double.class)));
			namedParams.put("epsilon", DLPythonUtils.toPython(getEntryValue(CFG_KEY_EPSILON, Double.class)));
			namedParams.put("decay", DLPythonUtils.toPython(getEntryValue(CFG_KEY_DECAY, Double.class)));
		}

		@Override
		protected IDialogComponentGroup getParameterDialogGroupInternal() {
			return new DLKerasRMSPropDialog(this);
		}

		private static class DLKerasRMSPropDialog extends AbstractGridBagDialogComponentGroup {

			private DLKerasRMSPropDialog(final DLKerasRMSProp model) {
				addNumberSpinnerRowComponent(ConfigUtil.toSettingsModelDoubleBounded(
						model.get(CFG_KEY_LR, Double.class), 0, Double.MAX_VALUE), "Learning rate", 0.001);
				addNumberSpinnerRowComponent(ConfigUtil.toSettingsModelDoubleBounded(
						model.get(CFG_KEY_RHO, Double.class), 0, Double.MAX_VALUE), "Rho", 0.1);
				addNumberSpinnerRowComponent(ConfigUtil.toSettingsModelDoubleBounded(
						model.get(CFG_KEY_EPSILON, Double.class), 0, Double.MAX_VALUE), "Epsilon", 1e-8);
				addNumberSpinnerRowComponent(ConfigUtil.toSettingsModelDoubleBounded(
						model.get(CFG_KEY_DECAY, Double.class), 0, Double.MAX_VALUE), "Learning rate decay", 0.0001);
			}
		}
	}

	public static class DLKerasAdagrad extends DLKerasAbstractOptimizer {

		static final String CFG_KEY_LR = "lr";

		static final String CFG_KEY_EPSILON = "epsilon";

		static final String CFG_KEY_DECAY = "decay";

		public DLKerasAdagrad() {
			super(DEFAULT_CFG_KEY, "Adagrad", "keras.optimizers.Adagrad");
			setEntryValue(CFG_KEY_LR, Double.class, 0.01);
			setEntryValue(CFG_KEY_EPSILON, Double.class, 1e-8);
			setEntryValue(CFG_KEY_DECAY, Double.class, 0d);
		}

		@Override
		protected void populateNamedParameters(final Map<String, String> namedParams) {
			namedParams.put("lr", DLPythonUtils.toPython(getEntryValue(CFG_KEY_LR, Double.class)));
			namedParams.put("epsilon", DLPythonUtils.toPython(getEntryValue(CFG_KEY_EPSILON, Double.class)));
			namedParams.put("decay", DLPythonUtils.toPython(getEntryValue(CFG_KEY_DECAY, Double.class)));
		}

		@Override
		protected IDialogComponentGroup getParameterDialogGroupInternal() {
			return new DLKerasAdagradDialog(this);
		}

		private static class DLKerasAdagradDialog extends AbstractGridBagDialogComponentGroup {

			private DLKerasAdagradDialog(final DLKerasAdagrad model) {
				addNumberSpinnerRowComponent(ConfigUtil.toSettingsModelDoubleBounded(
						model.get(CFG_KEY_LR, Double.class), 0, Double.MAX_VALUE), "Learning rate", 0.01);
				addNumberSpinnerRowComponent(ConfigUtil.toSettingsModelDoubleBounded(
						model.get(CFG_KEY_EPSILON, Double.class), 0, Double.MAX_VALUE), "Epsilon", 1e-8);
				addNumberSpinnerRowComponent(ConfigUtil.toSettingsModelDoubleBounded(
						model.get(CFG_KEY_DECAY, Double.class), 0, Double.MAX_VALUE), "Learning rate decay", 0.001);
			}
		}
	}

	public static class DLKerasAdadelta extends DLKerasAbstractOptimizer {

		static final String CFG_KEY_LR = "lr";

		static final String CFG_KEY_RHO = "rho";

		static final String CFG_KEY_EPSILON = "epsilon";

		static final String CFG_KEY_DECAY = "decay";

		public DLKerasAdadelta() {
			super(DEFAULT_CFG_KEY, "Adadelta", "keras.optimizers.Adadelta");
			setEntryValue(CFG_KEY_LR, Double.class, 1.0);
			setEntryValue(CFG_KEY_RHO, Double.class, 0.95);
			setEntryValue(CFG_KEY_EPSILON, Double.class, 1e-8);
			setEntryValue(CFG_KEY_DECAY, Double.class, 0d);
		}

		@Override
		protected void populateNamedParameters(final Map<String, String> namedParams) {
			namedParams.put("lr", DLPythonUtils.toPython(getEntryValue(CFG_KEY_LR, Double.class)));
			namedParams.put("rho", DLPythonUtils.toPython(getEntryValue(CFG_KEY_RHO, Double.class)));
			namedParams.put("epsilon", DLPythonUtils.toPython(getEntryValue(CFG_KEY_EPSILON, Double.class)));
			namedParams.put("decay", DLPythonUtils.toPython(getEntryValue(CFG_KEY_DECAY, Double.class)));
		}

		@Override
		protected IDialogComponentGroup getParameterDialogGroupInternal() {
			return new DLKerasAdadeltaDialog(this);
		}

		private static class DLKerasAdadeltaDialog extends AbstractGridBagDialogComponentGroup {

			private DLKerasAdadeltaDialog(final DLKerasAdadelta model) {
				addNumberSpinnerRowComponent(ConfigUtil.toSettingsModelDoubleBounded(
						model.get(CFG_KEY_LR, Double.class), 0, Double.MAX_VALUE), "Learning rate", 0.1);
				addNumberSpinnerRowComponent(ConfigUtil.toSettingsModelDoubleBounded(
						model.get(CFG_KEY_RHO, Double.class), 0, Double.MAX_VALUE), "Rho", 0.01);
				addNumberSpinnerRowComponent(ConfigUtil.toSettingsModelDoubleBounded(
						model.get(CFG_KEY_EPSILON, Double.class), 0, Double.MAX_VALUE), "Epsilon", 1e-8);
				addNumberSpinnerRowComponent(ConfigUtil.toSettingsModelDoubleBounded(
						model.get(CFG_KEY_DECAY, Double.class), 0, Double.MAX_VALUE), "Learning rate decay", 0.1);
			}
		}
	}

	public static class DLKerasAdam extends DLKerasAbstractOptimizer {

		static final String CFG_KEY_LR = "lr";

		static final String CFG_KEY_BETA_1 = "beta_1";

		static final String CFG_KEY_BETA_2 = "beta_2";

		static final String CFG_KEY_EPSILON = "epsilon";

		static final String CFG_KEY_DECAY = "decay";

		public DLKerasAdam() {
			super(DEFAULT_CFG_KEY, "Adam", "keras.optimizers.Adam");
			setEntryValue(CFG_KEY_LR, Double.class, 0.001);
			setEntryValue(CFG_KEY_BETA_1, Double.class, 0.9);
			setEntryValue(CFG_KEY_BETA_2, Double.class, 0.999);
			setEntryValue(CFG_KEY_EPSILON, Double.class, 1e-8);
			setEntryValue(CFG_KEY_DECAY, Double.class, 0d);
		}

		@Override
		protected void populateNamedParameters(final Map<String, String> namedParams) {
			namedParams.put("lr", DLPythonUtils.toPython(getEntryValue(CFG_KEY_LR, Double.class)));
			namedParams.put("beta_1", DLPythonUtils.toPython(getEntryValue(CFG_KEY_BETA_1, Double.class)));
			namedParams.put("beta_2", DLPythonUtils.toPython(getEntryValue(CFG_KEY_BETA_2, Double.class)));
			namedParams.put("epsilon", DLPythonUtils.toPython(getEntryValue(CFG_KEY_EPSILON, Double.class)));
			namedParams.put("decay", DLPythonUtils.toPython(getEntryValue(CFG_KEY_DECAY, Double.class)));
		}

		@Override
		protected IDialogComponentGroup getParameterDialogGroupInternal() {
			return new DLKerasAdamDialog(this);
		}

		private static class DLKerasAdamDialog extends AbstractGridBagDialogComponentGroup {

			private DLKerasAdamDialog(final DLKerasAdam model) {
				addNumberSpinnerRowComponent(ConfigUtil.toSettingsModelDoubleBounded(
						model.get(CFG_KEY_LR, Double.class), 0, Double.MAX_VALUE), "Learning rate", 0.001);
				addNumberSpinnerRowComponent(ConfigUtil.toSettingsModelDoubleBounded(
						model.get(CFG_KEY_BETA_1, Double.class), Math.nextUp(0d), Math.nextDown(1d)), "Beta 1", 0.01);
				addNumberSpinnerRowComponent(ConfigUtil.toSettingsModelDoubleBounded(
						model.get(CFG_KEY_BETA_2, Double.class), Math.nextUp(0d), Math.nextDown(1d)), "Beta 2", 0.001);
				addNumberSpinnerRowComponent(ConfigUtil.toSettingsModelDoubleBounded(
						model.get(CFG_KEY_EPSILON, Double.class), 0, Double.MAX_VALUE), "Epsilon", 1e-8);
				addNumberSpinnerRowComponent(ConfigUtil.toSettingsModelDoubleBounded(
						model.get(CFG_KEY_DECAY, Double.class), 0, Double.MAX_VALUE), "Learning rate decay", 0.0001);
			}
		}
	}

	public static class DLKerasAdamax extends DLKerasAbstractOptimizer {

		static final String CFG_KEY_LR = "lr";

		static final String CFG_KEY_BETA_1 = "beta_1";

		static final String CFG_KEY_BETA_2 = "beta_2";

		static final String CFG_KEY_EPSILON = "epsilon";

		static final String CFG_KEY_DECAY = "decay";

		public DLKerasAdamax() {
			super(DEFAULT_CFG_KEY, "Adamax", "keras.optimizers.Adamax");
			setEntryValue(CFG_KEY_LR, Double.class, 0.002);
			setEntryValue(CFG_KEY_BETA_1, Double.class, 0.9);
			setEntryValue(CFG_KEY_BETA_2, Double.class, 0.999);
			setEntryValue(CFG_KEY_EPSILON, Double.class, 1e-8);
			setEntryValue(CFG_KEY_DECAY, Double.class, 0d);
		}

		@Override
		protected void populateNamedParameters(final Map<String, String> namedParams) {
			namedParams.put("lr", DLPythonUtils.toPython(getEntryValue(CFG_KEY_LR, Double.class)));
			namedParams.put("beta_1", DLPythonUtils.toPython(getEntryValue(CFG_KEY_BETA_1, Double.class)));
			namedParams.put("beta_2", DLPythonUtils.toPython(getEntryValue(CFG_KEY_BETA_2, Double.class)));
			namedParams.put("epsilon", DLPythonUtils.toPython(getEntryValue(CFG_KEY_EPSILON, Double.class)));
			namedParams.put("decay", DLPythonUtils.toPython(getEntryValue(CFG_KEY_DECAY, Double.class)));
		}

		@Override
		protected IDialogComponentGroup getParameterDialogGroupInternal() {
			return new DLKerasAdamaxDialog(this);
		}

		private static class DLKerasAdamaxDialog extends AbstractGridBagDialogComponentGroup {

			private DLKerasAdamaxDialog(final DLKerasAdamax model) {
				addNumberSpinnerRowComponent(ConfigUtil.toSettingsModelDoubleBounded(
						model.get(CFG_KEY_LR, Double.class), 0, Double.MAX_VALUE), "Learning rate", 0.001);
				addNumberSpinnerRowComponent(ConfigUtil.toSettingsModelDoubleBounded(
						model.get(CFG_KEY_BETA_1, Double.class), Math.nextUp(0d), Math.nextDown(1d)), "Beta 1", 0.01);
				addNumberSpinnerRowComponent(ConfigUtil.toSettingsModelDoubleBounded(
						model.get(CFG_KEY_BETA_2, Double.class), Math.nextUp(0d), Math.nextDown(1d)), "Beta 2", 0.001);
				addNumberSpinnerRowComponent(ConfigUtil.toSettingsModelDoubleBounded(
						model.get(CFG_KEY_EPSILON, Double.class), 0, Double.MAX_VALUE), "Epsilon", 1e-8);
				addNumberSpinnerRowComponent(ConfigUtil.toSettingsModelDoubleBounded(
						model.get(CFG_KEY_DECAY, Double.class), 0, Double.MAX_VALUE), "Learning rate decay", 0.0001);
			}
		}
	}

	public static class DLKerasNadam extends DLKerasAbstractOptimizer {

		static final String CFG_KEY_LR = "lr";

		static final String CFG_KEY_BETA_1 = "beta_1";

		static final String CFG_KEY_BETA_2 = "beta_2";

		static final String CFG_KEY_EPSILON = "epsilon";

		static final String CFG_KEY_SCHEDULE_DECAY = "schedule_decay";

		public DLKerasNadam() {
			super(DEFAULT_CFG_KEY, "Nadam", "keras.optimizers.Nadam");
			setEntryValue(CFG_KEY_LR, Double.class, 0.002);
			setEntryValue(CFG_KEY_BETA_1, Double.class, 0.9);
			setEntryValue(CFG_KEY_BETA_2, Double.class, 0.999);
			setEntryValue(CFG_KEY_EPSILON, Double.class, 1e-8);
			setEntryValue(CFG_KEY_SCHEDULE_DECAY, Double.class, 0.004);
		}

		@Override
		protected void populateNamedParameters(final Map<String, String> namedParams) {
			namedParams.put("lr", DLPythonUtils.toPython(getEntryValue(CFG_KEY_LR, Double.class)));
			namedParams.put("beta_1", DLPythonUtils.toPython(getEntryValue(CFG_KEY_BETA_1, Double.class)));
			namedParams.put("beta_2", DLPythonUtils.toPython(getEntryValue(CFG_KEY_BETA_2, Double.class)));
			namedParams.put("epsilon", DLPythonUtils.toPython(getEntryValue(CFG_KEY_EPSILON, Double.class)));
			namedParams.put("schedule_decay",
					DLPythonUtils.toPython(getEntryValue(CFG_KEY_SCHEDULE_DECAY, Double.class)));
		}

		@Override
		protected IDialogComponentGroup getParameterDialogGroupInternal() {
			return new DLKerasNadamDialog(this);
		}

		private static class DLKerasNadamDialog extends AbstractGridBagDialogComponentGroup {

			private DLKerasNadamDialog(final DLKerasNadam model) {
				addNumberSpinnerRowComponent(ConfigUtil.toSettingsModelDoubleBounded(
						model.get(CFG_KEY_LR, Double.class), 0, Double.MAX_VALUE), "Learning rate", 0.001);
				addNumberSpinnerRowComponent(ConfigUtil.toSettingsModelDoubleBounded(
						model.get(CFG_KEY_BETA_1, Double.class), Math.nextUp(0d), Math.nextDown(1d)), "Beta 1", 0.01);
				addNumberSpinnerRowComponent(ConfigUtil.toSettingsModelDoubleBounded(
						model.get(CFG_KEY_BETA_2, Double.class), Math.nextUp(0d), Math.nextDown(1d)), "Beta 2", 0.001);
				addNumberSpinnerRowComponent(ConfigUtil.toSettingsModelDoubleBounded(
						model.get(CFG_KEY_EPSILON, Double.class), 0, Double.MAX_VALUE), "Epsilon", 1e-8);
				addNumberSpinnerRowComponent(
						ConfigUtil.toSettingsModelDouble(model.get(CFG_KEY_SCHEDULE_DECAY, Double.class)),
						"Schedule decay", 0.001);
			}
		}
	}
}
