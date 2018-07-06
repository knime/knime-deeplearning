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

package org.knime.dl.keras.core.layers.dialog.seed;

import java.util.Random;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.dl.keras.core.layers.dialog.tuple.DLKerasTuple;
import org.knime.dl.keras.core.struct.Member;
import org.knime.dl.keras.core.struct.dialog.AbstractSwingWidget;
import org.knime.dl.keras.core.struct.dialog.SwingWidget;
import org.knime.dl.keras.core.struct.dialog.SwingWidgetFactory;
import org.knime.dl.keras.core.struct.instance.MemberReadInstance;
import org.knime.dl.keras.core.struct.instance.MemberWriteInstance;
import org.knime.dl.keras.core.struct.param.Required;
import org.knime.dl.keras.core.struct.param.FieldParameterMember;
import org.knime.dl.keras.core.struct.param.ParameterMember;
import org.scijava.util.ClassUtils;

import net.miginfocom.swing.MigLayout;

/**
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public class SwingDLKerasSeedWidgetFactory implements SwingWidgetFactory<DLKerasSeed> {

    @Override
    public boolean supports(final Member<?> member) {
        return ClassUtils.canCast(member.getRawType(), DLKerasSeed.class)
            && ((ParameterMember<?>)member).getOptionalStatus().equals(Required.Required);
    }

    @Override
    public SwingWidget<DLKerasSeed> create(final Member<DLKerasSeed> model) {
        return new Widget(model);
    }

    // -- Helper classes --

    private class Widget extends AbstractSwingWidget<DLKerasSeed> {

        private JPanel panel;

        private JTextField m_textField;

        private JButton m_seedButton;

        private JCheckBox m_optionalBox;

        public Widget(final Member<DLKerasSeed> model) {
            super(model);
        }

        @Override
        public JPanel getComponent() {
            if (panel != null)
                return panel;

            boolean isOptional = false;
            boolean defaultSelected = false;
            if (member() instanceof FieldParameterMember) {
                FieldParameterMember<?> fpm = (FieldParameterMember<?>)member();
                DLKerasSeed seed = ((DLKerasSeed)fpm.getDefault());
                isOptional = seed.isOptional();
                defaultSelected = seed.isEnabled();
            } else {
                throw new IllegalStateException(
                    "The member must be of type FieldParameterMember for DLKerasTupleWidgets.");
            }

            panel = new JPanel();
            final MigLayout layout = new MigLayout("fillx,ins 0 0 0 0", "[][fill,grow]10[]", "");
            panel.setLayout(layout);

            if (isOptional) {
                m_optionalBox = new JCheckBox();
                m_optionalBox.addActionListener(a -> setEnabled(m_optionalBox.isSelected()));
                m_optionalBox.setSelected(defaultSelected);
                panel.add(m_optionalBox);
            }

            m_textField = new JTextField(30);
            panel.add(m_textField, "growx");

            Random rnd = new Random();
            m_seedButton = new JButton("New seed");
            m_seedButton.addActionListener(a -> m_textField.setText(rnd.nextLong() + ""));
            panel.add(m_seedButton);

            setEnabled(defaultSelected);
            return panel;
        }

        @Override
        public void loadFrom(MemberReadInstance<DLKerasSeed> instance, PortObjectSpec[] spec)
            throws InvalidSettingsException {
            instance.load();
            DLKerasSeed seed = instance.get();
            m_textField.setText(seed.getSeed() + "");
            if (m_optionalBox != null) {
                m_optionalBox.setSelected(seed.isEnabled());
                setEnabled(seed.isEnabled());
            }
        }

        @Override
        public void saveTo(MemberWriteInstance<DLKerasSeed> instance) throws InvalidSettingsException {
            try {
                instance.set(new DLKerasSeed(Long.parseLong(m_textField.getText()), m_optionalBox.isSelected(), false));
                instance.save();
            } catch (NumberFormatException e) {
                throw new InvalidSettingsException("Could not save seed value. Must be non floating point number");
            }
        }

        @Override
        public void setEnabled(boolean enabled) {
            m_textField.setEnabled(enabled);
            m_seedButton.setEnabled(enabled);
        }
    }
}
