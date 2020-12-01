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
 *   Nov 17, 2020 (marcel): created
 */
package org.knime.dl.python.base.node;

import org.knime.core.node.NodeDialogPane;
import org.knime.dl.python.prefs.DLPythonPreferences;
import org.knime.python2.PythonVersion;
import org.knime.python2.config.PythonCommandConfig;
import org.knime.python2.config.PythonExecutableSelectionPanel;
import org.knime.python2.config.PythonFixedVersionExecutableSelectionPanel;
import org.knime.python2.config.PythonSourceCodeConfig;
import org.knime.python2.config.PythonSourceCodeOptionsPanel;
import org.knime.python2.config.PythonSourceCodePanel;
import org.knime.python2.generic.VariableNames;
import org.knime.python2.generic.templates.SourceCodeTemplate;
import org.knime.python2.generic.templates.SourceCodeTemplateRepository;
import org.knime.python2.nodes.PythonNodeDialogContent;
import org.knime.python2.ports.InputPort;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 */
public final class DLPythonNodeDialogContent {

    /**
     * Creates a {@link PythonNodeDialogContent} instance specifically configured for use in the deep learning scripting
     * node dialogs.
     *
     * @param dialog The parent dialog.
     * @param inPorts The input ports of the node.
     * @param config The configuration object of the node.
     * @param variableNames The input and output variables in the workspace on Python side.
     * @param templateRepositoryId The unique name of the {@link SourceCodeTemplateRepository repository} containing the
     *            {@link SourceCodeTemplate script templates} of the node.
     * @return The created dialog content.
     */
    public static PythonNodeDialogContent createDialogContent(final NodeDialogPane dialog, final InputPort[] inPorts,
        final PythonSourceCodeConfig config, final VariableNames variableNames, final String templateRepositoryId) {
        final PythonSourceCodeOptionsPanel optionsPanel =
            new PythonSourceCodeOptionsPanel(DLPythonPreferences.getSerializerPreference());
        final PythonExecutableSelectionPanel executablePanel =
            new PythonFixedVersionExecutableSelectionPanel(dialog, new PythonCommandConfig(PythonVersion.PYTHON3,
                DLPythonPreferences::getCondaInstallationPath, DLPythonPreferences::getPythonCommandPreference));
        final PythonSourceCodePanel scriptPanel =
            new PythonSourceCodePanel(dialog, variableNames, optionsPanel, executablePanel);
        return new PythonNodeDialogContent(dialog, inPorts, config, scriptPanel, optionsPanel, executablePanel,
            templateRepositoryId);
    }

    private DLPythonNodeDialogContent() {}
}
