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

import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.dl.keras.core.struct.Member;
import org.knime.dl.keras.core.struct.instance.MemberReadInstance;
import org.knime.dl.keras.core.struct.instance.MemberWriteInstance;
import org.knime.dl.keras.core.struct.param.Required;
import org.knime.dl.keras.core.struct.param.ParameterMember;
import org.scijava.util.ClassUtils;

import net.miginfocom.swing.MigLayout;

/**
 * <p>
 * NB: Heavily inspired by work of Curtis Rueden.
 * </p>
 * 
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
class SwingTextWidgetFactory implements SwingWidgetFactory<String> {

    @Override
    public boolean supports(final Member<?> member) {
        return ClassUtils.isText(member.getRawType())
            && ((ParameterMember<?>)member).getOptionalStatus().equals(Required.Required);
    }

    @Override
    public SwingWidget<String> create(final Member<String> model) {
        return new Widget(model);
    }

    // -- Helper classes --

    private class Widget extends AbstractSwingWidget<String> implements SwingTextWidget, DocumentListener {

        private JPanel panel;

        private JTextComponent textComponent;

        public Widget(final Member<String> model) {
            super(model);
        }

        @Override
        public JPanel getComponent() {
            if (panel != null)
                return panel;

            panel = new JPanel();
            final MigLayout layout = new MigLayout("fillx,ins 3 0 3 0", "[fill,grow]");
            panel.setLayout(layout);

            // construct text widget of the appropriate style, if specified
            if (SwingWidgets.isStyle(this, AREA_STYLE)) {
                textComponent = new JTextArea("");
            } else {
                textComponent = new JTextField("");
            }
            getComponent().add(textComponent);
            textComponent.getDocument().addDocumentListener(this);

            return panel;
        }

        // -- DocumentListener methods --

        @Override
        public void changedUpdate(final DocumentEvent e) {
            //            m_text = textComponent.getText();
        }

        @Override
        public void insertUpdate(final DocumentEvent e) {
            //            m_text = textComponent.getText();
        }

        @Override
        public void removeUpdate(final DocumentEvent e) {
            //            m_text =   textComponent.getText();
        }

        // -- Model change event listener --
        @Override
        public void loadFrom(MemberReadInstance<String> instance, final PortObjectSpec[] spec)
            throws InvalidSettingsException {
            textComponent.setText(instance.get());
        }

        @Override
        public void saveTo(MemberWriteInstance<String> instance) throws InvalidSettingsException {
            instance.set(textComponent.getText());
        }

        @Override
        public void setEnabled(boolean enabled) {
            textComponent.setEnabled(enabled);
        }
    }
}
