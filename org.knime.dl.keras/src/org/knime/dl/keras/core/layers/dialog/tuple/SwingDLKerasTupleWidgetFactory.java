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

package org.knime.dl.keras.core.layers.dialog.tuple;

import java.awt.Color;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.apache.commons.lang3.StringUtils;
import org.jdesktop.swingx.JXCollapsiblePane;
import org.knime.core.node.InvalidSettingsException;
import org.knime.dl.keras.base.nodes.learner.view.jfreechart.DocumentAdapter;
import org.knime.dl.keras.core.layers.DLParameterValidationUtils;
import org.knime.dl.keras.core.struct.Member;
import org.knime.dl.keras.core.struct.dialog.AbstractSwingWidget;
import org.knime.dl.keras.core.struct.dialog.SwingWidget;
import org.knime.dl.keras.core.struct.dialog.SwingWidgetFactory;
import org.knime.dl.keras.core.struct.instance.MemberReadInstance;
import org.knime.dl.keras.core.struct.instance.MemberWriteInstance;
import org.knime.dl.keras.core.struct.param.ParameterMember;
import org.scijava.util.ClassUtils;

import net.miginfocom.swing.MigLayout;

/**
 * @author David Kolb, KNIME GmbH, Konstanz, Germany
 */
public class SwingDLKerasTupleWidgetFactory implements SwingWidgetFactory<DLKerasTuple> {

    @Override
    public boolean supports(final Member<?> member) {
        return ClassUtils.canCast(member.getRawType(), DLKerasTuple.class) && ((ParameterMember<?>)member).isRequired();
    }

    @Override
    public SwingWidget<DLKerasTuple> create(final Member<DLKerasTuple> model) {
        return new Widget(model);
    }

    // -- Helper classes --

    private class Widget extends AbstractSwingWidget<DLKerasTuple> {

        private JPanel panel;

        private TupleTextField m_textField;

        private DLKerasTuple m_lastTuple = null;

        public Widget(final Member<DLKerasTuple> model) {
            super(model);
        }

        @Override
        public JPanel getComponent() {
            if (panel != null)
                return panel;

            panel = new JPanel(new MigLayout("fillx,ins 0 0 0 0", "[fill,grow]"));

            m_textField = new TupleTextField();
            panel.add(m_textField, "growx");

            return panel;
        }

        @Override
        public void loadFrom(MemberReadInstance<DLKerasTuple> instance) throws InvalidSettingsException {
            m_lastTuple = instance.get();
            m_textField.setReferenceTuple(m_lastTuple);
            m_textField.setTuple(instance.get().getTuple());
        }

        @Override
        public void saveTo(MemberWriteInstance<DLKerasTuple> instance) throws InvalidSettingsException {
            instance.set(new DLKerasTuple(m_textField.getTuple(), m_lastTuple.getMinLength(),
                m_lastTuple.getMaxLength(), m_lastTuple.isPartialAllowed()));
        }

        @Override
        public void setEnabled(final boolean enabled) {
            m_textField.setEnabled(enabled);
        }
    }

    /**
     * A JTextField that turns red if no double number is entered.
     */
    private class TupleTextField extends JPanel {

        private static final long serialVersionUID = 1L;

        private DLKerasTuple m_refernceTuple;

        private JTextField m_tuple = new JTextField();

        private JLabel m_errorMessage = new JLabel();

        JXCollapsiblePane m_errorPanel = new JXCollapsiblePane();

        public TupleTextField() {
            m_tuple.getDocument().addDocumentListener((DocumentAdapter)e -> updateStatus());
            this.setLayout(new MigLayout("fillx,ins 0 0 0 0", "[fill,grow]"));
            this.add(m_tuple, "wrap, growx");

            m_errorMessage.setForeground(Color.RED);
            m_errorPanel.setAnimated(false);
            m_errorPanel.add(m_errorMessage);
            m_errorPanel.setCollapsed(true);
            this.add(m_errorPanel, "growx");
        }

        private boolean checkText() {
            final String text = m_tuple.getText();
            final String stripepd = text.replaceAll("\\s+","");
            if (m_refernceTuple.isPartialAllowed()) {
                if (!stripepd.matches(DLParameterValidationUtils.PARTIAL_SHAPE_PATTERN) || text.isEmpty()) {
                    m_errorMessage.setText("Invalid tuple format: '" + m_tuple.getText() + "' Must be digits"
                        + (m_refernceTuple.isPartialAllowed() ? " or a question mark" : "") + " separated by a comma.");
                    return false;
                }
            } else {
                if ((!stripepd.matches(DLParameterValidationUtils.SHAPE_PATTERN))) {
                    m_errorMessage.setText("Invalid tuple format: '" + m_tuple.getText() + "' Must be digits separated by a comma.");
                    return false;
                }
            }
            Long[] testTuple = DLKerasTuple.stringToTuple(text);
            if (testTuple.length < m_refernceTuple.getMinLength()
                || testTuple.length > m_refernceTuple.getMaxLength()) {
                m_errorMessage.setText("Invalid tuple length: '" + testTuple.length + "'. Length must be in between "
                    + m_refernceTuple.getMinLength() + "-" + m_refernceTuple.getMaxLength() + ".");
                return false;
            }
            if (checkTupleZeroOrNegative(testTuple)) {
                m_errorMessage.setText("Tuple must not contain zero or negative values.");
                return false;
            }

            return true;
        }

        private boolean checkTupleZeroOrNegative(Long[] tuple) {
            for (Long l : tuple) {
                if (l != null && l <= 0) {
                    return true;
                }
            }
            return false;
        }

        private void updateStatus() {
            if (!checkText()) {
                m_tuple.setForeground(Color.RED);
                m_errorPanel.setCollapsed(false);
            } else {
                // TextField content is valid, hence reset color to default
                m_tuple.setForeground(null);
                // Hide the error message
                m_errorPanel.setCollapsed(true);
            }
        }

        public void setReferenceTuple(final DLKerasTuple ref) {
            m_refernceTuple = ref;
        }

        public void setTuple(final Long[] tuple) {
            m_tuple.setText(DLKerasTuple.tupleToString(tuple));
        }

        public Long[] getTuple() throws InvalidSettingsException {
            if (checkText()) {
                return DLKerasTuple.stringToTuple(m_tuple.getText());
            } else {
                throw new InvalidSettingsException(m_errorMessage.getText());
            }
        }
    }
}
