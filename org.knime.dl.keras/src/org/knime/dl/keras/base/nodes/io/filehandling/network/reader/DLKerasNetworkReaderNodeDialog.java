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
 *   Feb 17, 2020 (benjamin): created
 */
package org.knime.dl.keras.base.nodes.io.filehandling.network.reader;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.dl.core.DLCanceledExecutionException;
import org.knime.dl.core.DLInstallationTestTimeoutException;
import org.knime.dl.core.DLMissingDependencyException;
import org.knime.dl.core.DLNotCancelable;
import org.knime.dl.keras.core.DLKerasNetworkLoader;
import org.knime.dl.python.core.DLPythonNetworkLoader;
import org.knime.dl.python.core.DLPythonNetworkLoaderRegistry;
import org.knime.filehandling.core.node.portobject.reader.PortObjectReaderNodeDialog;

/**
 * Dialog for the Keras reader node.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 */
final class DLKerasNetworkReaderNodeDialog extends PortObjectReaderNodeDialog<DLKerasNetworkReaderNodeConfig> {

    private JComboBox<KerasBackend> m_kerasBackend;

    DLKerasNetworkReaderNodeDialog(final PortsConfiguration portsConfig) {
        super(portsConfig, new DLKerasNetworkReaderNodeConfig(), "keras_network_reader", JFileChooser.FILES_ONLY);
        final JPanel kerasBackendPanel = createKerasBackendSelectionPanel();
        addAdditionalPanel(kerasBackendPanel);
    }

    private JPanel createKerasBackendSelectionPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = createAndInitGBC();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        panel.add(new JLabel("Keras back end:"), gbc);

        gbc.gridx++;
        m_kerasBackend = new JComboBox<>();
        addKerasBackendOptions(m_kerasBackend);
        m_kerasBackend.addActionListener(e -> {
            final KerasBackend selectedBackend = (KerasBackend)m_kerasBackend.getSelectedItem();
            getConfig().getKerasBackend().setStringValue(selectedBackend.m_networkType);
        });
        m_kerasBackend.setSelectedIndex(0);
        panel.add(m_kerasBackend, gbc);
        return panel;
    }

    private static void addKerasBackendOptions(final JComboBox<KerasBackend> combobox) {
        // TODO remember the unavailable backends to show more helpful error messages
        final List<KerasBackend> availableBackends = DLPythonNetworkLoaderRegistry.getInstance() //
            .getAllNetworkLoaders().stream() //
            .filter(nl -> nl instanceof DLKerasNetworkLoader) //
            .filter(DLKerasNetworkReaderNodeDialog::checkAvailable) //
            .map(nl -> (DLKerasNetworkLoader<?>)nl) //
            .map(nl -> new KerasBackend(nl.getName(), nl.getNetworkType().getCanonicalName())) //
            .sorted(Comparator.comparing(KerasBackend::toString)) //
            .collect(Collectors.toList());
        for (final KerasBackend b : availableBackends) {
            combobox.addItem(b);
        }
    }

    /** Checks if the given loader is available */
    private static boolean checkAvailable(final DLPythonNetworkLoader<?> networkLoader) {
        try {
            DLPythonNetworkLoaderRegistry.getInstance();
            networkLoader.checkAvailability(false, DLPythonNetworkLoaderRegistry.getInstallationTestTimeout(),
                DLNotCancelable.INSTANCE);
            return true;
        } catch (final DLMissingDependencyException | DLInstallationTestTimeoutException
                | DLCanceledExecutionException e) {
            return false;
        }
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws NotConfigurableException {
        super.loadSettingsFrom(settings, specs);
        final String kerasBackend = getConfig().getKerasBackend().getStringValue();
        m_kerasBackend.setSelectedItem(new KerasBackend(null, kerasBackend));
    }

    private static class KerasBackend {

        private final String m_name;

        private final String m_networkType;

        public KerasBackend(final String name, final String networkType) {
            m_name = name;
            m_networkType = networkType;
        }

        @Override
        public String toString() {
            return m_name;
        }

        @Override
        public boolean equals(final Object obj) {
            if (!(obj instanceof KerasBackend)) {
                return false;
            }
            final KerasBackend o = (KerasBackend)obj;
            return m_networkType.equals(o.m_networkType);
        }

        @Override
        public int hashCode() {
            return Objects.hash(m_networkType);
        }
    }
}
