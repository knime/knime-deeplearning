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
package org.knime.dl.base.nodes.executor2;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JComboBox;
import javax.swing.JLabel;

import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.dl.base.nodes.AbstractGridBagDialogComponentGroup;
import org.knime.dl.base.nodes.DialogComponentObjectSelection;
import org.knime.dl.base.portobjects.DLNetworkPortObjectSpec;
import org.knime.dl.base.settings.ConfigUtil;
import org.knime.dl.core.DLNetwork;
import org.knime.dl.core.DLNetworkSpec;
import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.core.execution.DLExecutionContext;
import org.knime.dl.core.execution.DLExecutionContextRegistry;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
class DLExecutorGeneralPanel extends AbstractGridBagDialogComponentGroup {

    private final DLExecutorGeneralConfig m_cfg;

    private final DialogComponentObjectSelection<DLExecutionContext<?, ?>> m_dcBackend;

    DLExecutorGeneralPanel(final DLExecutorGeneralConfig cfg) {
        m_cfg = cfg;

        // construct panel:

        // execution context ("back end") selection
        m_dcBackend =
            new DialogComponentObjectSelection<>(m_cfg.getContextEntry(), DLExecutionContext::getName, "Back end");

        addDoubleColumnRow(getFirstComponent(m_dcBackend, JLabel.class),
            getFirstComponent(m_dcBackend, JComboBox.class));

        addNumberSpinnerRowComponent(
            ConfigUtil.toSettingsModelIntegerBounded(m_cfg.getBatchSizeEntry(), 1, Integer.MAX_VALUE),
            "Input batch size", 100);
        addCheckboxRow(ConfigUtil.toSettingsModelBoolean(m_cfg.getKeepInputColumnsEntry()),
            "Keep input columns in output table", true);
    }

    @Override
    public void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        final DLNetworkPortObjectSpec portObjectSpec =
            (DLNetworkPortObjectSpec)specs[DLAbstractExecutorNodeModel.IN_NETWORK_PORT_IDX];
        final Class<? extends DLNetwork> networkType = portObjectSpec.getNetworkType();
        final DLNetworkSpec networkSpec = portObjectSpec.getNetworkSpec();
        refreshAvailableBackends(networkType);

        // Check if the network has pre-defined input batch sizes. Note that different batch sizes for the same network
        // are not supported (for networks with multiple inputs).
        long batchSize = -1;
        for (final DLTensorSpec inputSpec : networkSpec.getInputSpecs()) {
            if (inputSpec.getBatchSize().isPresent()) {
                final long bs = inputSpec.getBatchSize().getAsLong();
                if (batchSize == -1) {
                    batchSize = bs;
                } else {
                    if (batchSize != bs) {
                        throw new NotConfigurableException(
                            "The input network has multiple inputs with different pre-defined batch sizes. "
                                + "This is not supported. Please make sure to use a network with uniform "
                                + "input batch sizes or no pre-defined batch size at all.");
                    }
                }
            }
        }
        if (batchSize != -1) {
            m_cfg.getBatchSizeEntry().setValue((int)batchSize);
            m_cfg.getBatchSizeEntry().setEnabled(false);
        } else {
            m_cfg.getBatchSizeEntry().setEnabled(true);
        }
    }

    void refreshAvailableBackends(final Class<? extends DLNetwork> networkType) throws NotConfigurableException {
        final List<DLExecutionContext<?, ?>> availableExecutionContexts =
            DLExecutionContextRegistry.getInstance().getExecutionContextsForNetworkType((networkType)) //
                .stream() //
                .sorted(Comparator.comparing(DLExecutionContext::getName)) //
                .collect(Collectors.toList());
        final DLExecutionContext<?, ?> value = m_cfg.getContextEntry().getValue();
        final DLExecutionContext<?, ?> selectedContext;
        // needs to be called to ensure that there is a list of items to select from
        m_dcBackend.replaceListItems(availableExecutionContexts, null);
        if (availableExecutionContexts.isEmpty()) {
            throw new NotConfigurableException("There is no available back end that supports the input network.");
        } else if (value != null && containsContext(availableExecutionContexts, value)) {
            selectedContext = value;
        } else {
            selectedContext = availableExecutionContexts.get(0);
            m_cfg.getContextEntry().setValue(selectedContext);
        }
        m_dcBackend.replaceListItems(availableExecutionContexts, selectedContext);
    }

    private static boolean containsContext(final List<DLExecutionContext<?, ?>> contexts,
        final DLExecutionContext<?, ?> context) {
        for (final DLExecutionContext<?, ?> check : contexts) {
            if (check.getNetworkType().isAssignableFrom(context.getNetworkType())) {
                return true;
            }
        }
        return false;
    }
}
