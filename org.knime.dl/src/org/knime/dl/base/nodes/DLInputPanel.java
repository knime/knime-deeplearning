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
 *   Jul 10, 2017 (marcel): created
 */
package org.knime.dl.base.nodes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;

import javax.swing.JComboBox;
import javax.swing.JLabel;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterPanel;
import org.knime.dl.base.nodes.DLConverterRefresher.DLNoConverterAvailableException;
import org.knime.dl.base.settings.DLAbstractInputConfig;
import org.knime.dl.base.settings.DLDataTypeColumnFilter;
import org.knime.dl.base.settings.DLInputConfig;
import org.knime.dl.core.DLContext;
import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.core.data.convert.DLDataValueToTensorConverterFactory;
import org.knime.dl.util.DLUtils;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <I> the type of input config
 */
public class DLInputPanel<I extends DLInputConfig<?>> extends AbstractGridBagDialogComponentGroup {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(DLInputPanel.class);

    /**
     * The config for this panel.
     */
    protected final I m_cfg;

    private final DLTensorSpec m_inputTensorSpec;

    private final DialogComponentObjectSelection<DLDataValueToTensorConverterFactory<?, ?>> m_dcConverter;

    private final DataColumnSpecFilterPanel m_dcInputColumns;

    private final DLTensorRole m_tensorRole;

    private final List<BiConsumer<?, ?>> m_listeners = new ArrayList<>();
    
    private DataTableSpec m_tableSpec;

    /**
     * 
     * @param cfg stores the configuration of this input
     * @param tensorSpec the spec of the tensor which will be fed with the input data
     * @param header the string displayed above the column selection e.g. "Input columns:"
     * @param tensorRole the role of the tensor (i.e. either INPUT or TARGET)
     */
    public DLInputPanel(final I cfg, final DLTensorSpec tensorSpec, final String header,
        final DLTensorRole tensorRole) {
        m_cfg = cfg;
        m_inputTensorSpec = tensorSpec;
        m_tensorRole = tensorRole;

        // construct panel:

        // meta information
        addLabelRow("Number of neurons: " + DLUtils.Shapes.getSizeAsString(m_inputTensorSpec.getShape()));

        addLabelRow("Shape: " + m_inputTensorSpec.getShape().toString());

        // converter selection
        m_dcConverter =
            new DialogComponentObjectSelection<>(m_cfg.getConverterEntry(), c -> "From " + c.getName(), "Conversion");
        addDoubleColumnRow(getFirstComponent(m_dcConverter, JLabel.class),
            getFirstComponent(m_dcConverter, JComboBox.class));

        // column selection
        addLabelRow(header);

        m_dcInputColumns = new DataColumnSpecFilterPanel();
        addComponent(m_dcInputColumns);

        m_cfg.getGeneralConfig().getContextEntry().addValueChangeListener((entry, oldValue) -> {
            try {
                refreshAvailableConverters();
            } catch (final NotConfigurableException ex) {
                throw new IllegalStateException(ex.getMessage(), ex);
            }
        });

        m_cfg.getConverterEntry()
            .addValueChangeListener(addListener((entry, oldValue) -> refreshAllowedInputColumns()));
    }

    /**
     * Adds <b>listener</b> to the managed listeners and returns it.
     * 
     * @param listener to add
     * @return <b>listener</b>
     */
    protected <T, U> BiConsumer<T, U> addListener(BiConsumer<T, U> listener) {
        m_listeners.add(listener);
        return listener;
    }

    /**
     * Checks if the user configuration makes sense and saves it in the config.
     * 
     * @param settings the settings to save to
     * @throws InvalidSettingsException if the user configuration is invalid
     */
    public void saveToSettings(final NodeSettingsWO settings) throws InvalidSettingsException {
        final DataColumnSpecFilterConfiguration columns = m_cfg.getInputColumnsEntry().getValue();
        m_dcInputColumns.saveConfiguration(columns);
        DLConfigurationUtility.inputMatchesTensorSpec(m_inputTensorSpec, columns.applyTo(m_tableSpec).getIncludes(),
            m_tableSpec, m_cfg.getConverterEntry().getValue(), m_tensorRole);
        m_cfg.saveToSettings(settings);
    }

    @Override
    public final void loadSettingsFrom(NodeSettingsRO settings, PortObjectSpec[] specs)
        throws NotConfigurableException {
        throw new UnsupportedOperationException(
            "This class should only be loaded with DLInputPanel#loadSettingsFrom(NodeSettingsRO, DataTableSpec).");
    }

    /**
     * Loads the configuration stored in <b>settings</b>.
     * 
     * @param settings the settings to load
     * @param tableSpec
     * @throws NotConfigurableException if the configuration can't be loaded
     */
    public void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec tableSpec)
        throws NotConfigurableException {
        m_tableSpec = tableSpec;
        try {
            m_cfg.loadFromSettingsInDialog(settings, tableSpec);
        } catch (final InvalidSettingsException e) {
            // ignore
            LOGGER.debug(e.getMessage() != null ? e.getMessage() : "Trying to restore from invalid settings.", e);
        }
        m_dcInputColumns.loadConfiguration(m_cfg.getInputColumnsEntry().getValue(), tableSpec);
        refreshAvailableConverters();
        refreshAllowedInputColumns();
    }

    private void refreshAvailableConverters() throws NotConfigurableException {
        assert m_tableSpec != null;
        final DLContext<?> context = m_cfg.getGeneralConfig().getContextEntry().getValue();
        DLConverterRefresher converterRefresher;
        try {
            final Comparator<DLDataValueToTensorConverterFactory<?, ?>> nameComparator =
                Comparator.comparing(DLDataValueToTensorConverterFactory::getName);
            converterRefresher = new DLConverterRefresher(m_tableSpec,
                context.getTensorFactory().getWritableBufferType(m_inputTensorSpec), m_inputTensorSpec, false,
                nameComparator);
        } catch (final DLNoConverterAvailableException e) {
            throw new NotConfigurableException(e.getLongMessage());
        }
        final List<DLDataValueToTensorConverterFactory<?, ?>> converterFactories = converterRefresher.getConverters();
        m_dcConverter.replaceListItems(converterFactories, null);
    }

    /**
     * @return the spec this panel represents
     * 
     */
    public DLTensorSpec getTensorSpec() {
        return m_inputTensorSpec;
    }

    /**
     * Unregisters all listeners, this panel registered.
     */
    @SuppressWarnings({"unchecked", "rawtypes"}) // we brute force anyway
    public void unregisterListeners() {
        // brute force just try all config entries where we registered a listener
        for (BiConsumer listener : m_listeners) {
            m_cfg.getGeneralConfig().getContextEntry().removeValueChangeListener(listener);
            m_cfg.getConverterEntry().removeValueChangeListener(listener);
        }
        m_listeners.clear();
        m_dcConverter.unregisterListeners();
    }

    private void refreshAllowedInputColumns() {
        assert m_tableSpec != null;
        final Class<? extends DataValue> allowedColType = m_cfg.getConverterEntry().getValue().getSourceType();
        if (m_tableSpec.containsCompatibleType(allowedColType)) {
            // We need to save and reload the current configuration to take user actions into account that were taken
            // since the dialog was opened. Else those would be overridden by the initial configuration.
            m_dcInputColumns.saveConfiguration(m_cfg.getInputColumnsEntry().getValue());
            m_dcInputColumns.loadConfiguration(m_cfg.getInputColumnsEntry().getValue(), m_tableSpec);
            final DataColumnSpecFilterConfiguration filterConfig = new DataColumnSpecFilterConfiguration(
                DLAbstractInputConfig.CFG_KEY_INPUT_COL, new DLDataTypeColumnFilter(allowedColType));
            m_cfg.getInputColumnsEntry().setValue(filterConfig, true);
            m_dcInputColumns.updateWithNewConfiguration(filterConfig);
        }
        // FIXME (knime-core): (AN: Is this still valid?)
        // Strange behavior within DataColumnSpecFilterPanel (see
        // #toFilteredStringArray where m_filter is always
        // null because it doesn't get set in #updateWithNewConfiguration (only
        // in the super class).
        // Also see NameFilterPanel#loadConfiguration where
        // #getRemovedFromIncludeList and #getRemovedFromExcludeList
        // get added to the panel, which makes sense in general but not really
        // when updating the filter config).
    }
}
