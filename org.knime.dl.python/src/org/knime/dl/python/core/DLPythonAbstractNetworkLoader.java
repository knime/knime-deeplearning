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
package org.knime.dl.python.core;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.knime.core.util.FileUtil;
import org.knime.dl.core.DLCancelable;
import org.knime.dl.core.DLCanceledExecutionException;
import org.knime.dl.core.DLInstallationTestTimeout;
import org.knime.dl.core.DLInstallationTestTimeoutException;
import org.knime.dl.core.DLInvalidDestinationException;
import org.knime.dl.core.DLInvalidEnvironmentException;
import org.knime.dl.core.DLMissingDependencyException;
import org.knime.dl.python.prefs.DLPythonPreferences;

import com.google.common.base.Strings;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public abstract class DLPythonAbstractNetworkLoader<N extends DLPythonNetwork> implements DLPythonNetworkLoader<N> {

    protected abstract DLPythonAbstractCommands createCommands(DLPythonContext context)
        throws DLInvalidEnvironmentException;

    protected abstract DLPythonInstallationTester getInstallationTester();

    @Override
    public final synchronized void checkAvailability(final boolean forceRefresh, final int timeout,
        final DLCancelable cancelable) throws DLMissingDependencyException, DLInstallationTestTimeoutException {
        getInstallationTester().testInstallation(forceRefresh, timeout, this, cancelable);
    }

    @Override
    public void save(final DLPythonNetworkHandle handle, final URI destination, final DLPythonContext context,
        final DLCancelable cancelable) throws IllegalArgumentException, DLInvalidDestinationException,
        DLInvalidEnvironmentException, IOException, DLCanceledExecutionException {
        final File destinationFile = FileUtil.getFileFromURL(validateDestination(destination));
        final DLPythonAbstractCommands commands = createCommands(checkNotNull(context));
        commands.saveNetwork(checkNotNull(handle), destinationFile.getAbsolutePath(), cancelable);
    }

    protected static class DLPythonInstallationTester {

        protected boolean m_tested = false;

        protected boolean m_success;

        protected String m_message;

        protected DLInstallationTestTimeoutException m_timeoutException;

        private final Supplier<DLPythonContext> m_contextCreator;

        /**
         * Create a installation tester which uses the default Python context for the installation tests.
         *
         * @deprecated use {@link #DLPythonInstallationTester(Supplier)} to control which context is used
         */
        @Deprecated
        public DLPythonInstallationTester() {
            this(() -> new DLPythonDefaultContext());
        }

        /**
         * Create a installation tester which uses the given supplier to create a Python context in which the
         * installation tests are executed.
         *
         * @param contextCreator the context creator
         */
        public DLPythonInstallationTester(final Supplier<DLPythonContext> contextCreator) {
            m_contextCreator = contextCreator;
            DLPythonPreferences.addPreferencesChangeListener(e -> {
                m_tested = false;
            });
        }

        protected synchronized void testInstallation(final boolean forceRefresh, final int timeout,
            final DLPythonAbstractNetworkLoader<?> loader, final DLCancelable cancelable)
            throws DLMissingDependencyException, DLInstallationTestTimeoutException {
            if (forceRefresh || !m_tested) {
                final AtomicBoolean success = new AtomicBoolean();
                final AtomicReference<String> message = new AtomicReference<>();
                final AtomicReference<DLInstallationTestTimeoutException> timeoutException = new AtomicReference<>();
                try (final DLPythonContext context = m_contextCreator.get()) {
                    final Thread t = new Thread(() -> {
                        try {
                            loader.createCommands(context).testInstallation(cancelable);
                            success.set(true);
                        } catch (final Throwable th) {
                            message.set(Strings.isNullOrEmpty(th.getMessage())
                                ? "Unknown error of type '" + th.getClass().getName() + "'." //
                                : th.getMessage());
                            if (th instanceof Error) {
                                throw (Error)th;
                            }
                        }
                    }, "DL-Installation-Test-" + loader.getNetworkType().getCanonicalName());
                    t.start();
                    try {
                        t.join(timeout);
                    } catch (final InterruptedException e) {
                        if (!success.get()) {
                            t.interrupt();
                            message.getAndUpdate(msg -> {
                                if (msg == null) {
                                    msg = "Installation test for Python back end '"
                                        + loader.getNetworkType().getCanonicalName() + "' was interrupted.";
                                    timeoutException.set(new DLInstallationTestTimeoutException(msg, e));
                                }
                                return msg;
                            });
                        }
                        Thread.currentThread().interrupt();
                    }
                    if (!success.get() && timeoutException.get() == null) {
                        t.interrupt();
                        message.getAndUpdate(msg -> {
                            if (msg == null) {
                                msg = "Installation test for Python back end '"
                                    + loader.getNetworkType().getCanonicalName() + "' timed out. "
                                    + "Please make sure your Python environment is properly set up and "
                                    + "consider increasing the timeout (currently " + timeout
                                    + " ms) using the VM option " + "'-D"
                                    + DLInstallationTestTimeout.INSTALLATION_TEST_VM_OPT + "=<value-in-ms>'.";
                                timeoutException.set(new DLInstallationTestTimeoutException(msg));
                            } else {
                                msg += "\nIf packages are missing you can install the correct version of the "
                                    + "required packages on the 'Python Deep Learning' preference page.";
                            }
                            return msg;
                        });
                    }
                }
                m_tested = true;
                m_success = success.get();
                m_message = message.get();
                m_timeoutException = timeoutException.get();
            }
            if (!m_success) {
                if (m_timeoutException != null) {
                    throw m_timeoutException;
                } else {
                    throw new DLMissingDependencyException(m_message);
                }
            } else {
                m_message = null;
            }
        }
    }
}
