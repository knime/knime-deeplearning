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
 *   Jun 12, 2020 (benjamin): created
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
import org.eclipse.swt.widgets.Group;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.python2.config.AbstractPythonConfigPanel;

/**
 * @author Benamin Wilhelm, KNIME GmbH, Konstanz, Germany
 */
final class DLPythonLibrarySelectionPanel extends AbstractPythonConfigPanel<DLPythonLibrarySelectionConfig, Composite> {

    DLPythonLibrarySelectionPanel(final DLPythonLibrarySelectionConfig config, final Composite parent) {
        super(config, parent);
        createLibrarySelectionPanel(config, getPanel());
    }

    @Override
    protected Composite createPanel(final Composite parent) {
        final Composite panel = new Composite(parent, SWT.NONE);
        final GridLayout gridLayout = new GridLayout();
        gridLayout.marginWidth = 0;
        panel.setLayout(gridLayout);
        return panel;
    }

    static void createLibrarySelectionPanel(final DLPythonLibrarySelectionConfig config, final Composite panel) {
        final LibrarySelectionRadioGroup librarySelection =
            new LibrarySelectionRadioGroup(config.getLibrarySelection(), panel);
        final GridData gridData = new GridData();
        gridData.grabExcessHorizontalSpace = true;
        gridData.horizontalAlignment = SWT.FILL;
        librarySelection.setLayoutData(gridData);
    }

    private static final class LibrarySelectionRadioGroup extends Composite {

        private final Button m_kerasRadioButton;

        private final Button m_tf2RadioButton;

        public LibrarySelectionRadioGroup(final SettingsModelString libraryConfig, final Composite parent) {
            super(parent, SWT.NONE);
            final Group radioButtonGroup = new Group(this, SWT.NONE);
            radioButtonGroup.setText("Library used in DL Python nodes");
            m_kerasRadioButton = new Button(radioButtonGroup, SWT.RADIO);
            m_kerasRadioButton.setText(DLPythonLibrarySelection.KERAS.getName());
            m_tf2RadioButton = new Button(radioButtonGroup, SWT.RADIO);
            m_tf2RadioButton.setText(DLPythonLibrarySelection.TF2.getName());
            final RowLayout rowLayout = new RowLayout(SWT.HORIZONTAL);
            rowLayout.pack = false;
            rowLayout.justify = true;
            rowLayout.marginLeft = 0;
            radioButtonGroup.setLayout(rowLayout);
            radioButtonGroup.pack();
            setSelectedLibrary(libraryConfig.getStringValue());
            libraryConfig.addChangeListener(e -> setSelectedLibrary(libraryConfig.getStringValue()));
            final SelectionListener radioButtonSelectionListener = createRadioButtonSelectionListener(libraryConfig);
            m_kerasRadioButton.addSelectionListener(radioButtonSelectionListener);
            m_tf2RadioButton.addSelectionListener(radioButtonSelectionListener);

        }

        private void setSelectedLibrary(final String libraryId) {
            final DLPythonLibrarySelection library = DLPythonLibrarySelection.fromId(libraryId);
            final Button pythonRadioButtonToSelect;
            final Button pythonRadioButtonToUnselect;
            if (DLPythonLibrarySelection.KERAS.equals(library)) {
                pythonRadioButtonToSelect = m_kerasRadioButton;
                pythonRadioButtonToUnselect = m_tf2RadioButton;
            } else if (DLPythonLibrarySelection.TF2.equals(library)) {
                pythonRadioButtonToSelect = m_tf2RadioButton;
                pythonRadioButtonToUnselect = m_kerasRadioButton;
            } else {
                throw new IllegalStateException(
                    "Selected library for DL Python nodes is neither Keras nor TensorFlow 2. "
                        + "This is an implementation error.");
            }
            if (!pythonRadioButtonToSelect.getSelection()) {
                pythonRadioButtonToSelect.setSelection(true);
            }
            if (pythonRadioButtonToUnselect.getSelection()) {
                pythonRadioButtonToUnselect.setSelection(false);
            }
        }

        private static SelectionListener createRadioButtonSelectionListener(final SettingsModelString libraryConfig) {
            return new SelectionListener() {

                @Override
                public void widgetSelected(final SelectionEvent e) {
                    final Button button = (Button)e.widget;
                    if (button.getSelection()) {
                        final DLPythonLibrarySelection selectedLibrary =
                            DLPythonLibrarySelection.fromName(button.getText());
                        libraryConfig.setStringValue(selectedLibrary.getId());
                    }
                }

                @Override
                public void widgetDefaultSelected(final SelectionEvent e) {
                    widgetSelected(e);
                }
            };
        }
    }
}
