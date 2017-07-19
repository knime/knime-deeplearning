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
 * History
 *   Jul 10, 2017 (marcel): created
 */
package org.knime.dl.base.nodes.executor;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Optional;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.filter.NameFilterConfiguration.FilterResult;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterPanel;
import org.knime.dl.core.DLLayerDataSpec;
import org.knime.dl.core.backend.DLBackend;
import org.knime.dl.core.backend.DLBackendRegistry;
import org.knime.dl.core.data.convert.input.DLDataValueToLayerDataConverterFactory;
import org.knime.dl.core.data.convert.input.DLDataValueToLayerDataConverterRegistry;
import org.knime.dl.util.DLUtils;

/**
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
@SuppressWarnings("serial")
final class DLInputLayerDataPanel extends JPanel {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(DLExecutorNodeModel.class);

    private final DLLayerDataSpec m_inputDataSpec;

    private final DLInputLayerDataModelConfig m_cfg;

    private final DataColumnSpecFilterPanel m_dcInputColumns;

    private final GridBagConstraints m_constr;

    private DataTableSpec m_lastTableSpec;

    DLInputLayerDataPanel(final DLLayerDataSpec inputDataSpec, final DLInputLayerDataModelConfig cfg,
        final DataTableSpec tableSpec) {
        super(new GridBagLayout());
        m_inputDataSpec = inputDataSpec;
        m_cfg = cfg;
        m_lastTableSpec = tableSpec;
        m_dcInputColumns = new DataColumnSpecFilterPanel(DataValue.class);
        m_constr = new GridBagConstraints();
        m_constr.gridx = 0;
        m_constr.gridy = 0;
        m_constr.weightx = 1;
        m_constr.anchor = GridBagConstraints.WEST;
        m_constr.fill = GridBagConstraints.VERTICAL;
        constructPanel();
    }

    DLInputLayerDataModelConfig getConfig() {
        return m_cfg;
    }

    DataColumnSpecFilterPanel getInputColumns() {
        return m_dcInputColumns;
    }

    void loadFromSettings(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws InvalidSettingsException, NotConfigurableException {
        m_lastTableSpec = (DataTableSpec)specs[DLExecutorNodeModel.IN_DATA_PORT_IDX];
        m_cfg.loadFromSettings(settings);
        m_dcInputColumns.loadConfiguration(m_cfg.getInputColumnsModel(), m_lastTableSpec);
        m_dcInputColumns.updateWithNewConfiguration(m_cfg.getInputColumnsModel());
    }

    void saveToSettings(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_dcInputColumns.saveConfiguration(m_cfg.getInputColumnsModel());
        m_cfg.saveToSettings(settings);
        final FilterResult filter = m_cfg.getInputColumnsModel().applyTo(m_lastTableSpec);
        if (filter.getIncludes().length > DLUtils.Shapes.getSize(m_inputDataSpec.getShape())) {
            throw new InvalidSettingsException("More columns selected (" + filter.getIncludes().length
                + ") than input neurons available (" + DLUtils.Shapes.getSize(m_inputDataSpec.getShape())
                + ") for input '" + m_inputDataSpec.getName() + "'.");
        }
        DataType type = null;
        for (final String include : filter.getIncludes()) {
            if (type == null) {
                type = m_lastTableSpec.getColumnSpec(include).getType();
            } else {
                if (!m_lastTableSpec.getColumnSpec(include).getType().equals(type)) {
                    throw new InvalidSettingsException(
                        "Only columns of the same type can be converted into a single layer (input '"
                            + m_inputDataSpec.getName() + "').");
                }
            }
        }
    }

    private void constructPanel() {
        setBorder(BorderFactory.createTitledBorder("Input: " + m_inputDataSpec.getName()));
        // meta information:
        final JPanel numNeurons = new JPanel();
        final GridBagConstraints numNeuronsConstr = new GridBagConstraints();
        numNeuronsConstr.insets = new Insets(5, 0, 5, 0);
        numNeurons.add(new JLabel("Number of neurons: " + DLUtils.Shapes.getSize(m_inputDataSpec.getShape())),
            numNeuronsConstr);
        add(numNeurons, m_constr);
        m_constr.gridy++;
        final JPanel shape = new JPanel();
        final GridBagConstraints shapeConstr = new GridBagConstraints();
        shapeConstr.insets = new Insets(5, 0, 5, 0);
        shape.add(new JLabel("Shape: " + m_inputDataSpec.shapeToString()), shapeConstr);
        add(shape, m_constr);
        m_constr.gridy++;
        // converter selection
        final String[][] availableConverters = getAvailableConverters();
        if (availableConverters[0].length == 0) {
            LOGGER.error("No converters available for input '" + m_inputDataSpec.getName() + "'.");
            availableConverters[0] = new String[]{"<none>"};
            availableConverters[1] = new String[]{null};
        }
        m_cfg.getConverterModel().setStringValue(availableConverters[1][0]);
        final DialogComponentIdFromPrettyStringSelection dcConverter = new DialogComponentIdFromPrettyStringSelection(
            m_cfg.getConverterModel(), "Converter", availableConverters[0], availableConverters[1]);
        m_cfg.addAvailableConvertersChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(final ChangeEvent e) {
                // update converter selection list. Try to preserve the
                // selected converter (e.g. when a different back
                // end has a corresponding converter with equal name).
                // TODO: also check for equal specs and search for a
                // converter that preserves the specs if the check fails
                final SettingsModelString smConverter = m_cfg.getConverterModel();
                final String oldConverter = smConverter.getStringValue();
                try {
                    final String[][] newConverters = getAvailableConverters();
                    final String newConverter =
                        Arrays.asList(newConverters[0]).contains(oldConverter) ? oldConverter : newConverters[0][0];
                    // TODO: check if new converters are valid instead of just trying and catching
                    smConverter.setStringValue(newConverter);
                    dcConverter.replaceListItems(newConverters[0], newConverters[1], newConverter);
                } catch (final Exception ex) {
                    LOGGER.error("No converters available for input '" + m_inputDataSpec.getName() + "'.");
                }
            }
        });
        add(dcConverter.getComponentPanel(), m_constr);
        m_constr.gridy++;
        // column selection
        final DataColumnSpecFilterPanel dcInputColumns = m_dcInputColumns;

        m_cfg.addAllowedInputTypesChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(final ChangeEvent e) {
                // update column selection list
                // final SettingsModelColumnFilter2 smInputColumns =
                // m_cfg.getInputColumnsModel();
                // final Collection<Class<? extends DataValue>>
                // allowedInputTypes = m_cfg.getAllowedInputTypes();
                // TODO: update filter
            }
        });
        final JPanel inputColumnsLabel = new JPanel();
        inputColumnsLabel.add(new JLabel("Input columns:"));
        add(inputColumnsLabel, m_constr);
        m_constr.gridy++;
        final JPanel inputColumnsFilter = new JPanel();
        inputColumnsFilter.add(dcInputColumns);
        add(inputColumnsFilter, m_constr);
        m_constr.gridy++;
    }

    private String[][] getAvailableConverters() {
        final Optional<DLBackend> backend = DLBackendRegistry.getBackend(m_cfg.getBackendModel().getStringValue());
        if (!backend.isPresent()) {
            throw new RuntimeException(
                "Backend '" + m_cfg.getBackendModel().getStringValue() + "' could not be found.");
        }
        final HashSet<DataType> inputTypes = new HashSet<>();
        final LinkedHashSet<DLDataValueToLayerDataConverterFactory<?, ?>> converterFactories = new LinkedHashSet<>();
        final DLDataValueToLayerDataConverterRegistry converters =
            DLDataValueToLayerDataConverterRegistry.getInstance();
        for (final DataColumnSpec inputColSpec : m_lastTableSpec) {
            if (inputTypes.add(inputColSpec.getType())) {
                converterFactories.addAll(converters.getConverterFactories(inputColSpec.getType(),
                    backend.get().getWritableBufferType(m_inputDataSpec)));
            }
        }
        final String[] names = new String[converterFactories.size()];
        final String[] ids = new String[converterFactories.size()];
        int i = 0;
        for (final DLDataValueToLayerDataConverterFactory<?, ?> converter : converterFactories) {
            names[i] = converter.getName();
            ids[i] = converter.getIdentifier();
            i++;
        }
        return new String[][]{names, ids};
    }
}
