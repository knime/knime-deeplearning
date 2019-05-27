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
package org.knime.dl.base.nodes.executor2;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.data.DataCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.dl.base.nodes.DialogComponentObjectSelection;
import org.knime.dl.base.settings.ConfigUtil;
import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.core.data.DLReadableBuffer;
import org.knime.dl.core.data.convert.DLTensorToDataCellConverterFactory;
import org.knime.dl.core.data.convert.DLTensorToDataCellConverterRegistry;
import org.knime.dl.core.data.convert.DLTensorToListCellConverterFactory;
import org.knime.dl.core.execution.DLExecutionContext;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
final class DLExecutorOutputPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final DLExecutorOutputConfig m_cfg;

    private final DLTensorSpec m_outputTensorSpec;

    private final DialogComponentObjectSelection<DLTensorToDataCellConverterFactory<?, ?>> m_dcConverter;

    private final CopyOnWriteArrayList<ChangeListener> m_removeListeners;

    DLExecutorOutputPanel(final DLExecutorOutputConfig cfg, final DLTensorSpec outputDataSpec, final String suffix)
        throws NotConfigurableException {
        super(new GridBagLayout());
        m_cfg = cfg;
        m_outputTensorSpec = outputDataSpec;
        m_removeListeners = new CopyOnWriteArrayList<>();

        // construct panel:

        setBorder(BorderFactory.createTitledBorder("Output: " + m_outputTensorSpec.getName() + suffix));
        final GridBagConstraints constr = new GridBagConstraints();
        constr.gridx = 0;
        constr.gridy = 0;
        constr.weightx = 1;
        constr.anchor = GridBagConstraints.WEST;
        constr.fill = GridBagConstraints.VERTICAL;
        // meta information
        final JPanel shape = new JPanel();
        final GridBagConstraints shapeConstr = new GridBagConstraints();
        shapeConstr.insets = new Insets(5, 0, 5, 0);
        shape.add(new JLabel("Shape: " + m_outputTensorSpec.getShape().toString()), shapeConstr);
        add(shape, constr);
        // 'remove' button, see bottom for click event handling
        final JButton outputRemoveBtn = new JButton("remove");
        final GridBagConstraints outputRemoveBtnConstr = (GridBagConstraints)constr.clone();
        outputRemoveBtnConstr.weightx = 1;
        outputRemoveBtnConstr.anchor = GridBagConstraints.EAST;
        outputRemoveBtnConstr.fill = GridBagConstraints.NONE;
        outputRemoveBtnConstr.insets = new Insets(0, 0, 0, 5);
        add(outputRemoveBtn, outputRemoveBtnConstr);
        constr.gridy++;
        // converter selection
        final DLTensorToDataCellConverterRegistry converterRegistry = DLTensorToDataCellConverterRegistry.getInstance();
        m_dcConverter = new DialogComponentObjectSelection<>(m_cfg.getConverterEntry(),
            c -> "To " + c.getName() + (converterRegistry.isDeprecated(c.getIdentifier()) ? " (deprecated)" : ""),
            "Conversion");
        add(m_dcConverter.getComponentPanel(), constr);
        constr.gridy++;
        // prefix text input
        final DialogComponentString dcPrefix = new DialogComponentString(
            ConfigUtil.toSettingsModelString(m_cfg.getPrefixEntry()), "Output columns prefix");
        add(dcPrefix.getComponentPanel(), constr);
        constr.gridy++;
        // 'remove' button click event: remove output
        outputRemoveBtn.addActionListener(e -> onRemove());

        m_cfg.getGeneralConfig().getContextEntry().addValueChangeListener((e, oldValue) -> {
            try {
                refreshAvailableConverters();
            } catch (final NotConfigurableException ex) {
                throw new IllegalStateException(ex.getMessage(), ex);
            }
        });

        refreshAvailableConverters();
    }

    public void unregisterListeners() {
        m_dcConverter.unregisterListeners();
    }

    DLExecutorOutputConfig getConfig() {
        return m_cfg;
    }

    void addRemoveListener(final ChangeListener l) {
        if (!m_removeListeners.contains(l)) {
            m_removeListeners.add(l);
        }
    }

    void removeRemoveListener(final ChangeListener l) {
        m_removeListeners.remove(l);
    }

    void loadFromSettings(final NodeSettingsRO settings, final PortObjectSpec[] specs) throws NotConfigurableException {
        try {
            m_cfg.loadFromSettings(settings);
        } catch (final InvalidSettingsException e) {
            // ignore
        }
        refreshAvailableConverters();
    }

    void saveToSettings(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_cfg.saveToSettings(settings);
    }

    private void refreshAvailableConverters() throws NotConfigurableException {
        final DLExecutionContext<?> executionContext = m_cfg.getGeneralConfig().getContextEntry().getValue();
        final DLTensorToDataCellConverterRegistry convRegistry = DLTensorToDataCellConverterRegistry.getInstance();
        final Class<? extends DLReadableBuffer> bufferType =
            executionContext.getTensorFactory().getReadableBufferType(m_outputTensorSpec);
        final List<DLTensorToDataCellConverterFactory<?, ? extends DataCell>> converterFactories =
            convRegistry.getPreferredFactoriesForSourceType(bufferType, m_outputTensorSpec);
        final Set<DLTensorToDataCellConverterFactory<?, ?>> builtInElement = new HashSet<>(1);
        final Set<DLTensorToDataCellConverterFactory<?, ?>> builtInCollection = new HashSet<>(1);
        final Set<DLTensorToDataCellConverterFactory<?, ?>> extensionElement = new HashSet<>(1);
        final Set<DLTensorToDataCellConverterFactory<?, ?>> extensionCollection = new HashSet<>(1);
        for (final DLTensorToDataCellConverterFactory<?, ? extends DataCell> converter : converterFactories) {
            if (converter.getClass().getCanonicalName().contains("org.knime.dl.core.data.convert")) {
                if (converter instanceof DLTensorToListCellConverterFactory) {
                    builtInCollection.add(converter);
                } else {
                    builtInElement.add(converter);
                }
            } else {
                if (converter instanceof DLTensorToListCellConverterFactory) {
                    extensionCollection.add(converter);
                } else {
                    extensionElement.add(converter);
                }
            }
        }
        final Comparator<DLTensorToDataCellConverterFactory<?, ?>> nameComparator =
            Comparator.comparing(DLTensorToDataCellConverterFactory::getName);
        final Predicate<? super DLTensorToDataCellConverterFactory<?, ?>> canComputeOutTableSpec =
            cf -> cf.getDestCount(m_outputTensorSpec).isPresent();
        final List<DLTensorToDataCellConverterFactory<?, ?>> converterFactoriesSorted = Stream
            .concat(
                Stream.concat(builtInElement.stream().sorted(nameComparator),
                    extensionElement.stream().sorted(nameComparator)),
                Stream.concat(builtInCollection.stream().sorted(nameComparator),
                    extensionCollection.stream().sorted(nameComparator)))
            // remove all converters for which we can't calculate the table outputSpec
            .filter(canComputeOutTableSpec).collect(Collectors.toList());
        if (converterFactoriesSorted.isEmpty()) {
            throw new NotConfigurableException(
                "No converter available for the output data type (" + m_outputTensorSpec.getElementType().getTypeName()
                    + ") of network output '" + m_outputTensorSpec.getName()
                    + "'. Please make sure you are not missing a KNIME Deep Learning extension "
                    + "and/or try to use a network that outputs different data types.");
        }
        final DLTensorToDataCellConverterFactory<?, ?> previousConverter = m_cfg.getConverterEntry().getValue();
        if (previousConverter != null && !converterFactoriesSorted.contains(previousConverter)
            && previousConverter.getBufferType().isAssignableFrom(bufferType)
            && canComputeOutTableSpec.test(previousConverter)) {
            // The previous converter is deprecated now but still applicable
            // We should add it to the list to keep it selected
            converterFactoriesSorted.add(previousConverter);
        }
        m_dcConverter.replaceListItems(converterFactoriesSorted, null);
    }

    private void onRemove() {
        for (final ChangeListener l : m_removeListeners) {
            l.stateChanged(new ChangeEvent(this));
        }
    }
}
