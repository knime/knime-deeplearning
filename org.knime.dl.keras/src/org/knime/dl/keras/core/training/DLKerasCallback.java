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
package org.knime.dl.keras.core.training;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.knime.dl.base.nodes.AbstractGridBagDialogComponentGroup;
import org.knime.dl.base.nodes.IDialogComponentGroup;
import org.knime.dl.base.settings.AbstractConfig;
import org.knime.dl.base.settings.Config;
import org.knime.dl.base.settings.ConfigUtil;
import org.knime.dl.python.util.DLPythonUtils;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public interface DLKerasCallback extends Config {

	default String getIdentifier() {
		return getClass().getCanonicalName();
	}

	String getName();

	String getKerasIdentifier();

	String getBackendRepresentation();

	IDialogComponentGroup getParameterDialogGroup();

	public abstract static class DLKerasAbstractCallback extends AbstractConfig implements DLKerasCallback {

		protected final String m_name;

		protected final String m_kerasIdentifier;

		protected IDialogComponentGroup m_dialogComponentGroup;

		protected DLKerasAbstractCallback(final String configKey, final String name, final String kerasIdentifier) {
			super(configKey);
			m_name = name;
			m_kerasIdentifier = kerasIdentifier;
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
		public String getBackendRepresentation() {
			final LinkedHashMap<String, String> namedParams = new LinkedHashMap<>();
			populateNamedParameters(namedParams);
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

	public static class DLKerasTerminateOnNaN extends DLKerasAbstractCallback {

		static final String CFG_KEY = "terminate_on_nan";

		public DLKerasTerminateOnNaN() {
			super(CFG_KEY, "Terminate on NaN loss", "keras.callbacks.TerminateOnNaN");
		}

		@Override
		protected void populateNamedParameters(final Map<String, String> namedParams) {
			// no op
		}

		@Override
		protected IDialogComponentGroup getParameterDialogGroupInternal() {
			return new AbstractGridBagDialogComponentGroup() {
			};
		}
	}

	public static class DLKerasEarlyStopping extends DLKerasAbstractCallback {

		static final String CFG_KEY = "early_stopping";

		static final String CFG_KEY_MONITOR = "monitor";

		static final String CFG_KEY_MIN_DELTA = "min_delta";

		static final String CFG_KEY_PATIENCE = "patience";

		static final String CFG_KEY_MODE = "mode";

		public DLKerasEarlyStopping() {
			super(CFG_KEY, "Terminate on training stagnation (early stopping)", "keras.callbacks.EarlyStopping");
			setEntryValue(CFG_KEY_MONITOR, String.class, "loss");
			setEntryValue(CFG_KEY_MIN_DELTA, Double.class, 0.0);
			setEntryValue(CFG_KEY_PATIENCE, Integer.class, 0);
			setEntryValue(CFG_KEY_MODE, String.class, "auto");
		}

		@Override
		protected void populateNamedParameters(final Map<String, String> namedParams) {
			namedParams.put("monitor", DLPythonUtils.toPython(getEntryValue(CFG_KEY_MONITOR, String.class)));
			namedParams.put("min_delta", DLPythonUtils.toPython(getEntryValue(CFG_KEY_MIN_DELTA, Double.class)));
			namedParams.put("patience", DLPythonUtils.toPython(getEntryValue(CFG_KEY_PATIENCE, Integer.class)));
			namedParams.put("mode", DLPythonUtils.toPython(getEntryValue(CFG_KEY_MODE, String.class)));
		}

		@Override
		protected IDialogComponentGroup getParameterDialogGroupInternal() {
			return new DLKerasEarlyStoppingDialog(this);
		}

		private static class DLKerasEarlyStoppingDialog extends AbstractGridBagDialogComponentGroup {

			private DLKerasEarlyStoppingDialog(final DLKerasEarlyStopping model) {
				addNumberSpinnerRowComponent(ConfigUtil.toSettingsModelDoubleBounded(
						model.get(CFG_KEY_MIN_DELTA, Double.class), 0, Double.MAX_VALUE), "Min. delta", 0.01);
				addNumberSpinnerRowComponent(ConfigUtil.toSettingsModelIntegerBounded(
						model.get(CFG_KEY_PATIENCE, Integer.class), 0, Integer.MAX_VALUE), "Patience", 1);
			}
		}
	}

	public static class DLKerasReduceLROnPlateau extends DLKerasAbstractCallback {

		static final String CFG_KEY = "reduce_lr_on_plateau";

		static final String CFG_KEY_MONITOR = "monitor";

		static final String CFG_KEY_FACTOR = "factor";

		static final String CFG_KEY_PATIENCE = "patience";

		static final String CFG_KEY_MODE = "mode";

		static final String CFG_KEY_EPSILON = "epsilon";

		static final String CFG_KEY_COOLDOWN = "cooldown";

		static final String CFG_KEY_MIN_LR = "min_lr";

		public DLKerasReduceLROnPlateau() {
			super(CFG_KEY, "Reduce learning rate on plateau", "keras.callbacks.ReduceLROnPlateau");
			setEntryValue(CFG_KEY_MONITOR, String.class, "loss");
			setEntryValue(CFG_KEY_FACTOR, Double.class, 0.1);
			setEntryValue(CFG_KEY_PATIENCE, Integer.class, 10);
			setEntryValue(CFG_KEY_MODE, String.class, "auto");
			setEntryValue(CFG_KEY_EPSILON, Double.class, 1e-4);
			setEntryValue(CFG_KEY_COOLDOWN, Integer.class, 0);
			setEntryValue(CFG_KEY_MIN_LR, Double.class, 0.0);
		}

		@Override
		protected void populateNamedParameters(final Map<String, String> namedParams) {
			namedParams.put("monitor", DLPythonUtils.toPython(getEntryValue(CFG_KEY_MONITOR, String.class)));
			namedParams.put("factor", DLPythonUtils.toPython(getEntryValue(CFG_KEY_FACTOR, Double.class)));
			namedParams.put("patience", DLPythonUtils.toPython(getEntryValue(CFG_KEY_PATIENCE, Integer.class)));
			namedParams.put("mode", DLPythonUtils.toPython(getEntryValue(CFG_KEY_MODE, String.class)));
			namedParams.put("epsilon", DLPythonUtils.toPython(getEntryValue(CFG_KEY_EPSILON, Double.class)));
			namedParams.put("cooldown", DLPythonUtils.toPython(getEntryValue(CFG_KEY_COOLDOWN, Integer.class)));
			namedParams.put("min_lr", DLPythonUtils.toPython(getEntryValue(CFG_KEY_MIN_LR, Double.class)));
		}

		@Override
		protected IDialogComponentGroup getParameterDialogGroupInternal() {
			return new DLKerasReduceLROnPlateauDialog(this);
		}

		private static class DLKerasReduceLROnPlateauDialog extends AbstractGridBagDialogComponentGroup {

			private DLKerasReduceLROnPlateauDialog(final DLKerasReduceLROnPlateau model) {
				addNumberSpinnerRowComponent(ConfigUtil.toSettingsModelDoubleBounded(
						model.get(CFG_KEY_FACTOR, Double.class), 0, Math.nextDown(1.0)), "Factor", 0.01);
				addNumberSpinnerRowComponent(ConfigUtil.toSettingsModelIntegerBounded(
						model.get(CFG_KEY_PATIENCE, Integer.class), 0, Integer.MAX_VALUE), "Patience", 1);
				addNumberSpinnerRowComponent(ConfigUtil.toSettingsModelDoubleBounded(
						model.get(CFG_KEY_EPSILON, Double.class), 0, Double.MAX_VALUE), "Epsilon", 1e-4);
				addNumberSpinnerRowComponent(ConfigUtil.toSettingsModelIntegerBounded(
						model.get(CFG_KEY_COOLDOWN, Integer.class), 0, Integer.MAX_VALUE), "Cooldown", 1);
				addNumberSpinnerRowComponent(ConfigUtil.toSettingsModelDoubleBounded(
						model.get(CFG_KEY_MIN_LR, Double.class), 0, Double.MAX_VALUE), "Min. learning rate", 0.1);
			}
		}
	}
}