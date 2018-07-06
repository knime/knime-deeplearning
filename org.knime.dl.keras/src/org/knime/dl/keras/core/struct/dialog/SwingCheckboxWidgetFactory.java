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

package org.knime.dl.keras.core.struct.dialog;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.dl.keras.core.struct.Member;
import org.knime.dl.keras.core.struct.instance.MemberReadInstance;
import org.knime.dl.keras.core.struct.instance.MemberWriteInstance;
import org.scijava.util.ClassUtils;

/**
 * <p>
 * NB: Heavily inspired by work of Curtis Rueden.
 * </p>
 * 
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
class SwingCheckboxWidgetFactory implements SwingWidgetFactory<Boolean> {

    @Override
    public boolean supports(final Member<?> member) {
        if (member.getRawType() != null) {
            return ClassUtils.isBoolean(member.getRawType());
        }
        return false;
    }

    @Override
    public SwingWidget<Boolean> create(final Member<Boolean> model) {
        return new Widget(model);
    }

    // -- Helper classes --

    private class Widget extends AbstractSwingWidget<Boolean> {

        private JPanel m_panel;

        private JCheckBox m_checkBox;

        public Widget(final Member<Boolean> model) {
            super(model);
        }

        @Override
        public JPanel getComponent() {
            if (m_panel != null)
                return m_panel;

            m_panel = new JPanel(new GridBagLayout());

            final GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.weighty = 0;
            gbc.weightx = 1;
            gbc.fill = GridBagConstraints.HORIZONTAL;

            m_checkBox = new JCheckBox();
            m_panel.add(m_checkBox, gbc);
            return m_panel;
        }

        // -- Model change event listener --

        @Override
        public void loadFrom(MemberReadInstance<Boolean> instance, PortObjectSpec[] spec)
            throws InvalidSettingsException {
            instance.load();
            m_checkBox.setSelected(instance.get());
        }

        @Override
        public void saveTo(MemberWriteInstance<Boolean> instance) throws InvalidSettingsException {
            instance.set(m_checkBox.isSelected());
            instance.save();
        }

        @Override
        public void setEnabled(boolean enabled) {
            m_checkBox.setEnabled(enabled);
        }
    }
}
