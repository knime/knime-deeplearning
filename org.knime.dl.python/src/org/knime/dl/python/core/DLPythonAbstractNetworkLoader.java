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
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.FileUtil;
import org.knime.dl.core.DLCancelable;
import org.knime.dl.core.DLCanceledExecutionException;
import org.knime.dl.core.DLInstallationTestTimeout;
import org.knime.dl.core.DLInstallationTestTimeoutException;
import org.knime.dl.core.DLInvalidDestinationException;
import org.knime.dl.core.DLInvalidEnvironmentException;
import org.knime.dl.core.DLMissingDependencyException;
import org.knime.dl.python.prefs.DLPythonPreferences;
import org.knime.python2.util.PythonUtils;

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
    public final synchronized void checkAvailability(final DLPythonContext context, final boolean forceRefresh,
        final int timeout, final DLCancelable cancelable)
        throws DLMissingDependencyException, DLInstallationTestTimeoutException {
        getInstallationTester().testInstallation(context, forceRefresh, timeout, this, cancelable);
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

        protected Exception m_exception = null;

        /**
         * Create a new installation tester.
         */
        public DLPythonInstallationTester() {
            DLPythonPreferences.addPreferencesChangeListener(e -> {
                m_tested = false;
            });
        }

        protected synchronized void testInstallation(final DLPythonContext context, final boolean forceRefresh,
            final int timeout, final DLPythonAbstractNetworkLoader<?> loader, final DLCancelable cancelable)
            throws DLMissingDependencyException, DLInstallationTestTimeoutException {
            if (forceRefresh || !m_tested) {
                m_tested = false;
                m_exception = runTest(context, timeout, loader, cancelable);
                m_tested = true;
            }
            if (m_exception != null) {
                if (m_exception instanceof DLMissingDependencyException) {
                    throw (DLMissingDependencyException)m_exception;
                } else if (m_exception instanceof DLInstallationTestTimeoutException) {
                    throw (DLInstallationTestTimeoutException)m_exception;
                } else {
                    throw new IllegalStateException("Implementation error.", m_exception);
                }
            }
        }

        private static Exception runTest(final DLPythonContext context, final int timeout,
            final DLPythonAbstractNetworkLoader<?> loader, final DLCancelable cancelable) {
            final Future<DLMissingDependencyException> test =
                KNIMEConstants.GLOBAL_THREAD_POOL.enqueue(new InstallationTestCallable(context, loader, cancelable));
            final String networkTypeName = loader.getNetworkType().getCanonicalName();
            try {
                return test.get(timeout, TimeUnit.MILLISECONDS);
            } catch (final CancellationException ex) {
                return new DLInstallationTestTimeoutException(
                    "Installation test for Python deep learning back end '" + networkTypeName + "' was canceled.", ex);
            } catch (final InterruptedException ex) {
                Thread.currentThread().interrupt();
                test.cancel(true);
                return new DLInstallationTestTimeoutException(
                    "Installation test for Python deep learning back end '" + networkTypeName + "' was interrupted.",
                    ex);
            } catch (final TimeoutException ex) {
                test.cancel(true);
                return new DLInstallationTestTimeoutException(
                    "Installation test for Python deep learning back end '" + networkTypeName + "' timed out. "
                        + "Please make sure your Python environment is properly set up and "
                        + "consider increasing the timeout (currently " + timeout + " ms) using the VM option " + "'-D"
                        + DLInstallationTestTimeout.INSTALLATION_TEST_VM_OPT + "=<value-in-ms>'.",
                    ex);
            } catch (final ExecutionException ex) {
                throw new IllegalStateException(PythonUtils.Misc.unwrapExecutionException(ex).orElse(ex));
            }
        }

        private static final class InstallationTestCallable implements Callable<DLMissingDependencyException> {

            private final DLPythonContext m_context;

            private final DLPythonAbstractNetworkLoader<?> m_loader;

            private final DLCancelable m_cancelable;

            private InstallationTestCallable(final DLPythonContext context,
                final DLPythonAbstractNetworkLoader<?> loader, final DLCancelable cancelable) {
                m_context = context;
                m_loader = loader;
                m_cancelable = cancelable;
            }

            @SuppressWarnings("resource") // Python context is closed by client.
            @Override
            public DLMissingDependencyException call() throws Exception {
                try {
                    final DLPythonAbstractCommands commands = m_loader.createCommands(m_context); // NOSONAR See above.
                    commands.testInstallation(m_cancelable);
                    return null;
                } catch (final DLInvalidEnvironmentException ex) {
                    String message = Strings.isNullOrEmpty(ex.getMessage()) //
                        ? ("Unknown error of type '" + ex.getClass().getName() + "'.") //
                        : ex.getMessage();
                    message += "\nIn case Python packages are missing: you can create a new Python Conda environment "
                        + "that contains all packages required by the KNIME deep learning integrations in the \"KNIME "
                        + "Deep Learning\" Preferences";
                    return new DLMissingDependencyException(message, ex);
                } catch (final DLCanceledExecutionException ex) {
                    NodeLogger.getLogger(DLPythonAbstractNetworkLoader.class).debug(ex);
                    throw new CancellationException(ex.getMessage());
                }
            }
        }
    }
}
