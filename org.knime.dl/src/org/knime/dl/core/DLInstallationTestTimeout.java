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
package org.knime.dl.core;

import org.knime.core.node.NodeLogger;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public final class DLInstallationTestTimeout {

    private DLInstallationTestTimeout() {
        // Utility class.
    }

    /**
     * @see #getInstallationTestTimeout()
     */
    public static final String INSTALLATION_TEST_VM_OPT = "knime.dl.installationtesttimeout";

    /**
     * @see #getInstallationTestTimeout()
     */
    public static final int INSTALLATION_TEST_DEFAULT_TIMEOUT = 25000; // in ms

    private static final NodeLogger LOGGER = NodeLogger.getLogger(DLInstallationTestTimeout.class);

    private static int timeout = -1;

    /**
     * @return the installation test timeout that can be specified by the user via VM option
     *         {@link DLInstallationTestTimeout#INSTALLATION_TEST_VM_OPT}, defaults to
     *         {@link #INSTALLATION_TEST_DEFAULT_TIMEOUT}
     **/
    public static int getInstallationTestTimeout() {
        if (timeout == -1) {
            // Parse test timeout duration from VM option.
            try {
                timeout = Integer.parseInt(
                    System.getProperty(INSTALLATION_TEST_VM_OPT, Integer.toString(INSTALLATION_TEST_DEFAULT_TIMEOUT)));
            } catch (final NumberFormatException ex) {
                // Ignore, see below.
            }
            if (timeout < 0) {
                timeout = INSTALLATION_TEST_DEFAULT_TIMEOUT;
                LOGGER.warn("The VM option -D" + INSTALLATION_TEST_VM_OPT
                    + " was not set to a non-negative integer value, and thus defaults to " + timeout + " ms.");
            }
        }
        return timeout;
    }
}
