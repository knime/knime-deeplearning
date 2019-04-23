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
package org.knime.dl.python.prefs;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.python2.config.AbstractPythonConfigPanel;

/**
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 */
final class DLPythonConfigSelectionPanel extends AbstractPythonConfigPanel<DLPythonConfigSelectionConfig, Composite> {

    DLPythonConfigSelectionPanel(final DLPythonConfigSelectionConfig config, final Composite parent) {
        super(config, parent);
        final Composite panel = getPanel();

        // Create the radio buttons
        final ConfigSelectionRadioGroup configSelection = new ConfigSelectionRadioGroup(config.getConfigSelection(),
            config.getPythonInstallationInfo(), config.getPythonInstallationError(), panel);
        final GridData gridData = new GridData();
        gridData.grabExcessHorizontalSpace = true;
        gridData.horizontalAlignment = SWT.FILL;
        configSelection.setLayoutData(gridData);
    }

    @Override
    protected Composite createPanel(final Composite parent) {
        final Composite panel = new Composite(parent, SWT.NONE);
        panel.setLayout(new GridLayout());
        return panel;
    }

    private static final class ConfigSelectionRadioGroup extends Composite {

        private final Button m_pythonConfigRadioButton;

        private final Button m_dlConfigRadioButton;

        public ConfigSelectionRadioGroup(final SettingsModelString configSelectionConfig,
            final SettingsModelString defaultInstallationInfo, final SettingsModelString defaultInstallationError,
            final Composite parent) {
            super(parent, SWT.NONE);

            // Layout
            final RowLayout rowLayout = new RowLayout(SWT.VERTICAL);
            rowLayout.pack = true;
            rowLayout.justify = true;
            rowLayout.spacing = 10;
            setLayout(rowLayout);

            // Python radio button
            m_pythonConfigRadioButton = new Button(this, SWT.RADIO);
            m_pythonConfigRadioButton.setText("Use Python configuration");

            // Installation info
            @SuppressWarnings("unused") // Reference to object is not needed here; everything is done in its constructor.
            Object unused = new InstallationStatusDisplayPanel(defaultInstallationInfo, defaultInstallationError, this);

            // DL radio button
            m_dlConfigRadioButton = new Button(this, SWT.RADIO);
            m_dlConfigRadioButton.setText("Use special Deep Learning configuration");
            pack();

            // Hooks
            setConfigSelection(configSelectionConfig.getStringValue());
            configSelectionConfig.addChangeListener(e -> setConfigSelection(configSelectionConfig.getStringValue()));
            addSelectionListener(m_pythonConfigRadioButton, configSelectionConfig, DLPythonConfigSelection.PYTHON);
            addSelectionListener(m_dlConfigRadioButton, configSelectionConfig, DLPythonConfigSelection.DL);
        }

        private void setConfigSelection(final String configSelectionId) {
            final DLPythonConfigSelection configSelection = DLPythonConfigSelection.fromId(configSelectionId);
            if (DLPythonConfigSelection.PYTHON.equals(configSelection)) {
                selectPython();
            } else if (DLPythonConfigSelection.DL.equals(configSelection)) {
                selectDl();
            } else {
                throw new IllegalStateException("Selected config selection is neither Python nor deep learning. "
                    + "This is an implementation error.");
            }
        }

        private void selectPython() {
            // Only change if the selection if wrong
            if (!m_pythonConfigRadioButton.getSelection() || m_dlConfigRadioButton.getSelection()) {
                m_pythonConfigRadioButton.setSelection(true);
                m_dlConfigRadioButton.setSelection(false);
            }
        }

        private void selectDl() {
            // Only change if the selection if wrong
            if (m_pythonConfigRadioButton.getSelection() || !m_dlConfigRadioButton.getSelection()) {
                m_pythonConfigRadioButton.setSelection(false);
                m_dlConfigRadioButton.setSelection(true);
            }
        }

        private static void addSelectionListener(final Button button, final SettingsModelString configSelectionConfig,
            final DLPythonConfigSelection value) {
            button.addSelectionListener(new SelectionListener() {

                @Override
                public void widgetSelected(final SelectionEvent e) {
                    if (button.getSelection()) {
                        configSelectionConfig.setStringValue(value.getId());
                    }
                }

                @Override
                public void widgetDefaultSelected(final SelectionEvent e) {
                    widgetSelected(e);
                }
            });
        }
    }
}
