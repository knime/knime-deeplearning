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
 * History
 *   May 31, 2017 (marcel): created
 */
package org.knime.dl.keras.base.nodes.learner;

import org.knime.core.data.DataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;
import org.knime.dl.base.settings.AbstractStandardConfigEntry;
import org.knime.dl.base.settings.ConfigEntry;
import org.knime.dl.base.settings.DLAbstractInputConfig;
import org.knime.dl.base.settings.DLDataTypeColumnFilter;
import org.knime.dl.core.DLTensorId;
import org.knime.dl.core.data.convert.DLDataValueToTensorConverterFactory;
import org.knime.dl.core.data.convert.DLDataValueToTensorConverterRegistry;
import org.knime.dl.keras.core.training.DLKerasLossFunction;
import org.knime.dl.keras.core.training.DLKerasLossFunction.DLKerasCustomLoss;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class DLKerasLearnerTargetConfig extends DLAbstractInputConfig<DLKerasLearnerGeneralConfig> {

    private static final String CFG_KEY_LOSS_FUNC = "loss_function";

    private static final String CFG_KEY_CUSTOM_LOSS_FUNC = "custom_loss_function";

    private static final String CFG_KEY_USE_CUSTOM_LOSS = "useCustomLoss";

    @SuppressWarnings("rawtypes") // Java limitation
    DLKerasLearnerTargetConfig(final DLTensorId targetTensorId, final String targetTensorName,
        final DLKerasLearnerGeneralConfig generalCfg) {
        super(targetTensorId, targetTensorName, generalCfg);
        put(new AbstractStandardConfigEntry<DLDataValueToTensorConverterFactory>(CFG_KEY_CONVERTER,
            DLDataValueToTensorConverterFactory.class) {

            @Override
            protected void saveEntry(final NodeSettingsWO settings) throws InvalidSettingsException {
                final String converterIdentifier = m_value != null //
                    ? m_value.getIdentifier() : CFG_VALUE_NULL_CONVERTER;
                settings.addString(getEntryKey(), converterIdentifier);
            }

            @Override
            protected void loadEntry(final NodeSettingsRO settings) throws InvalidSettingsException {
                final String converterIdentifier = settings.getString(getEntryKey());
                if (CFG_VALUE_NULL_CONVERTER.equals(converterIdentifier)) {
                    throw new InvalidSettingsException(
                        "No target data converter available for network target '" + getTensorNameOrId() + "'.");
                }
                m_value = DLDataValueToTensorConverterRegistry.getInstance().getConverterFactory(converterIdentifier)
                    .orElseThrow(() -> new InvalidSettingsException(
                        "Target data converter '" + converterIdentifier + "' of network target '" + getTensorNameOrId()
                            + "' could not be found. Are you missing a KNIME extension?"));
            }
        });
        put(new AbstractStandardConfigEntry<DataColumnSpecFilterConfiguration>(CFG_KEY_INPUT_COL,
            DataColumnSpecFilterConfiguration.class,
            new DataColumnSpecFilterConfiguration(CFG_KEY_INPUT_COL, new DLDataTypeColumnFilter(DataValue.class))) {

            @Override
            protected void saveEntry(final NodeSettingsWO settings) throws InvalidSettingsException {
                final NodeSettingsWO subSettings = settings.addNodeSettings(CFG_KEY_INPUT_COL);
                m_value.saveConfiguration(subSettings);
            }

            @Override
            protected void loadEntry(final NodeSettingsRO settings) throws InvalidSettingsException {
                // no op. Separate routines for loading in model and dialog required. See below.
            }
        });
        put(new AbstractStandardConfigEntry<DLKerasLossFunction>(CFG_KEY_LOSS_FUNC, DLKerasLossFunction.class) {

            @Override
            protected void saveEntry(final NodeSettingsWO settings) throws InvalidSettingsException {
                final String identifier = m_value != null ? m_value.getClass().getCanonicalName() : "null";
                settings.addString(getEntryKey(), identifier);
            }

            @Override
            protected void loadEntry(final NodeSettingsRO settings) throws InvalidSettingsException {
                final String identifier = settings.getString(getEntryKey());
                if (!identifier.equals("null")) {
                    m_value = getGeneralConfig().getContextEntry().getValue().createLossFunctions().stream()
                        .filter(o -> o.getClass().getCanonicalName().equals(identifier)) //
                        .findFirst() //
                        .orElseThrow(() -> new InvalidSettingsException(
                            "Loss function '" + identifier + "' of target '" + getTensorNameOrId()
                                + " could not be found. Are you missing a KNIME Deep Learning extension?"));
                }
            }
        });

        put(new AbstractStandardConfigEntry<Boolean>(CFG_KEY_USE_CUSTOM_LOSS, Boolean.class, false) {
            @Override
            protected boolean handleFailureToLoadConfigEntry(final NodeSettingsRO settings, final Exception cause) {
                // backward compatibility to versions prior to 3.6 where there was no custom loss
                m_value = false;
                return true;
            }
        });

        put(new AbstractStandardConfigEntry<DLKerasCustomLoss>(CFG_KEY_CUSTOM_LOSS_FUNC, DLKerasCustomLoss.class,
            new DLKerasCustomLoss(targetTensorId)) {

            private static final String CFG_KEY_CUSTOM_CODE = "custom_code";

            @Override
            protected void saveEntry(final NodeSettingsWO settings) throws InvalidSettingsException {
                settings.addString(CFG_KEY_CUSTOM_CODE, getValue().getCustomCodeDialog());
            }

            @Override
            protected void loadEntry(final NodeSettingsRO settings) throws InvalidSettingsException {
                final String customCode = settings.getString(CFG_KEY_CUSTOM_CODE);
                m_value.setCustomCode(customCode);
            }

            @Override
            protected boolean handleFailureToLoadConfigEntry(final NodeSettingsRO settings, final Exception cause) {
                m_value = new DLKerasCustomLoss(targetTensorId);
                return true;
            }
        });
    }

    ConfigEntry<DLKerasLossFunction> getLossFunctionEntry() {
        return get(CFG_KEY_LOSS_FUNC, DLKerasLossFunction.class);
    }

    ConfigEntry<DLKerasCustomLoss> getCustomLossFunctionEntry() {
        return get(CFG_KEY_CUSTOM_LOSS_FUNC, DLKerasCustomLoss.class);
    }

    ConfigEntry<Boolean> getUseCustomLossEntry() {
        return get(CFG_KEY_USE_CUSTOM_LOSS, Boolean.class);
    }
}
