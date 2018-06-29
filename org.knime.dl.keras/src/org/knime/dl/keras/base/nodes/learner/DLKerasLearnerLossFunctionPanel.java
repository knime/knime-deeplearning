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
package org.knime.dl.keras.base.nodes.learner;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;

import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.jdesktop.swingx.JXCollapsiblePane;
import org.knime.base.node.jsnippet.guarded.GuardedDocument;
import org.knime.base.node.jsnippet.guarded.GuardedSection;
import org.knime.base.node.jsnippet.ui.JSnippetTextArea;
import org.knime.base.node.util.KnimeSyntaxTextArea;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.dl.base.nodes.DialogComponentObjectSelection;
import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.keras.core.training.DLKerasLossFunction;
import org.knime.dl.keras.core.training.DLKerasTrainingContext;

/**
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class DLKerasLearnerLossFunctionPanel {

    private final JPanel m_panel;

    private final DialogComponentObjectSelection<DLKerasLossFunction> m_dcLossFunction;

    private final RTextScrollPane m_customCodeArea;

    private final DLKerasLearnerTargetConfig m_cfg;

    private final DLTensorSpec m_targetTensorSpec;

    private final JXCollapsiblePane m_lossPanel;

    DLKerasLearnerLossFunctionPanel(DLKerasLearnerTargetConfig cfg, final DLTensorSpec targetTensorSpec) {
        m_targetTensorSpec = targetTensorSpec;
        m_cfg = cfg;
        m_dcLossFunction = new DialogComponentObjectSelection<DLKerasLossFunction>(cfg.getLossFunctionEntry(),
            DLKerasLossFunction::getName, "");
        CustomLossFunctionTextArea customCodeArea = new CustomLossFunctionTextArea();
        customCodeArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_PYTHON);
        m_customCodeArea = new RTextScrollPane(customCodeArea);
        m_customCodeArea.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        m_customCodeArea.setPreferredSize(new Dimension(1, 350));

        JRadioButton useStandardLossFunction = new JRadioButton("Standard loss function");
        JRadioButton useCustomLossFunction = new JRadioButton("Custom loss function");
        ButtonGroup btnGroup = new ButtonGroup();
        btnGroup.add(useStandardLossFunction);
        btnGroup.add(useCustomLossFunction);
        m_lossPanel = new JXCollapsiblePane();
        m_lossPanel.setAnimated(false);
        m_lossPanel.add(m_dcLossFunction.getComponentPanel());
        m_lossPanel.add(m_customCodeArea);
        m_cfg.getUseCustomLossEntry().addLoadListener(e -> {
            updateLossPanel(e.getValue());
            if (e.getValue()) {
                useCustomLossFunction.doClick();
            } else {
                useStandardLossFunction.doClick();
            }
        });
        m_cfg.getUseCustomLossEntry().addValueChangeListener((entry, oldValue) -> updateLossPanel(entry.getValue()));
        useStandardLossFunction.addChangeListener(e -> {
            m_cfg.getUseCustomLossEntry().setValue(!useStandardLossFunction.isSelected());
        });
        useStandardLossFunction.doClick();
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(useStandardLossFunction);
        buttonPanel.add(useCustomLossFunction);

        m_panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.CENTER;
        m_panel.add(buttonPanel, gbc);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 2;
        gbc.fill = GridBagConstraints.BOTH;
        m_panel.add(m_lossPanel, gbc);
    }

    private void updateLossPanel(boolean useCustomLoss) {
        m_lossPanel.setCollapsed(true);
        m_lossPanel.removeAll();
        m_lossPanel.add(useCustomLoss ? m_customCodeArea : m_dcLossFunction.getComponentPanel());
        m_lossPanel.setCollapsed(false);
    }

    JPanel getPanel() {
        return m_panel;
    }

    void loadSettings() throws NotConfigurableException {
        refreshAvailableLossFunctions();
        CustomLossFunctionDocument doc = new CustomLossFunctionDocument(m_cfg.getCustomLossFunctionEntry().getValue().getCustomCodeDialog());
        doc.addDocumentListener(new DocumentListener() {
            private int m_lastLineCount = m_customCodeArea.getTextArea().getLineCount();

            @Override
            public void insertUpdate(DocumentEvent e) {
                updateIfLineCountChanged();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateIfLineCountChanged();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateIfLineCountChanged();
            }
            
            private void updateIfLineCountChanged() {
                int newLineCount = m_customCodeArea.getTextArea().getLineCount();
                if (newLineCount != m_lastLineCount) {
                    m_lastLineCount = newLineCount;
                    m_panel.revalidate();
                }
            }
            
        });
        m_customCodeArea.getTextArea().setDocument(
            doc);
    }

    void saveSettings() throws InvalidSettingsException {
        try {
            m_cfg.getCustomLossFunctionEntry().getValue().setCustomCode(m_customCodeArea.getTextArea().getText());
        } catch (IllegalArgumentException e) {
            throw new InvalidSettingsException(e.getMessage(), e);
        }
    }

    private void refreshAvailableLossFunctions() throws NotConfigurableException {
        final DLKerasTrainingContext<?> trainingContext = m_cfg.getGeneralConfig().getContextEntry().getValue();
        final List<DLKerasLossFunction> availableLossFunctions = trainingContext.createLossFunctions() //
            .stream() //
            .sorted(Comparator.comparing(DLKerasLossFunction::getName)) //
            .collect(Collectors.toList());
        if (availableLossFunctions.isEmpty()) {
            throw new NotConfigurableException("No loss functions available for output '" + m_targetTensorSpec.getName()
                + "' (with training context '" + trainingContext.getName() + "').");
        }
        final DLKerasLossFunction selectedLossFunction = m_cfg.getLossFunctionEntry().getValue() != null
            ? m_cfg.getLossFunctionEntry().getValue() : availableLossFunctions.get(0);
        for (int i = availableLossFunctions.size() - 1; i >= 0; i--) {
            if (availableLossFunctions.get(i).getClass() == selectedLossFunction.getClass()) {
                availableLossFunctions.remove(i);
                availableLossFunctions.add(i, selectedLossFunction);
            }
        }
        m_dcLossFunction.replaceListItems(availableLossFunctions, selectedLossFunction);
    }

    private static class CustomLossFunctionDocument extends GuardedDocument {

        private static final String LOSS_FUNCTION_REGEX = "def custom_loss\\(y_true, y_pred\\):\\n";

        private static final String LOSS_FUNCTION_SIGNATURE = "def custom_loss(y_true, y_pred):\n";

        /**
         * 
         */
        private static final long serialVersionUID = 1L;

        CustomLossFunctionDocument(String customCode) throws NotConfigurableException {
            super(SyntaxConstants.SYNTAX_STYLE_PYTHON);
            String[] splitAtFunctionLine = customCode.split(LOSS_FUNCTION_REGEX);
            if (splitAtFunctionLine.length != 2) {
                throw new NotConfigurableException("The provided custom loss function code is invalid: " + customCode);
            }
            try {
                insertString(getLength(), splitAtFunctionLine[0], null);
                GuardedSection functionLine = addGuardedSection("customLossFunctionDefinition", getLength());
                functionLine.setText(LOSS_FUNCTION_SIGNATURE);
                insertString(getLength(), splitAtFunctionLine[1], null);
            } catch (BadLocationException e) {
                throw new NotConfigurableException(
                    "The provided custom loss function causes layout problems: " + customCode);
            }
        }
    }

    /**
     * The logic in this class is shamelessly copied from {@link JSnippetTextArea}.
     * If we should need this logic somewhere else, we can simply extract it into a public class but for now we keep
     * it private to avoid any problems if we should decide to go public in the future.
     * 
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    private static class CustomLossFunctionTextArea extends KnimeSyntaxTextArea {

        /**
         * 
         */
        private static final long serialVersionUID = 1L;

        /**
         * {@inheritDoc}
         */
        @Override
        public Color getForegroundForToken(final Token t) {
            if (isInGuardedSection(t.getOffset())) {
                return Color.gray;
            } else {
                return super.getForegroundForToken(t);
            }
        }

        /**
         * Returns true when offset is within a guarded section.
         *
         * @param offset the offset to test
         * @return true when offset is within a guarded section.
         */
        private boolean isInGuardedSection(final int offset) {
            GuardedDocument doc = (GuardedDocument)getDocument();

            for (String name : doc.getGuardedSections()) {
                GuardedSection gs = doc.getGuardedSection(name);
                if (gs.contains(offset)) {
                    return true;
                }
            }
            return false;
        }
    }
}
