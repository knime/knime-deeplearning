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
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.io.IOUtils;
import org.knime.core.util.ThreadUtils;
import org.knime.dl.core.DLCancelable;
import org.knime.dl.core.DLCanceledExecutionException;
import org.knime.dl.core.DLInvalidEnvironmentException;
import org.knime.python.typeextension.PythonModuleExtensions;
import org.knime.python2.PythonPreferencePage;
import org.knime.python2.extensions.serializationlibrary.interfaces.TableChunker;
import org.knime.python2.extensions.serializationlibrary.interfaces.TableCreator;
import org.knime.python2.extensions.serializationlibrary.interfaces.TableCreatorFactory;
import org.knime.python2.kernel.PythonKernel;
import org.knime.python2.kernel.PythonKernelException;
import org.knime.python2.kernel.PythonKernelOptions;
import org.knime.python2.kernel.PythonKernelOptions.PythonVersionOption;

import com.google.common.base.Strings;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public final class DLPythonDefaultContext implements DLPythonContext {

    private static final AtomicInteger THREAD_UNIQUE_ID = new AtomicInteger();

	public static PythonKernel createKernel() throws DLInvalidEnvironmentException {
		try {
			final PythonKernelOptions options = new PythonKernelOptions();
			options.setPythonVersionOption(PythonVersionOption.PYTHON3);
			return new PythonKernel(options);
		} catch (final IOException e) {
			final String msg = !Strings.isNullOrEmpty(e.getMessage())
					? "An error occurred while trying to launch Python: " + e.getMessage()
					: "An unknown error occurred while trying to launch Python. See log for details.";
			throw new DLInvalidEnvironmentException(msg, e);
		}
	}

	private PythonKernel m_kernel;

	public DLPythonDefaultContext() {
		// kernel will be created on demand
	}

	public DLPythonDefaultContext(final PythonKernel kernel) {
		m_kernel = checkNotNull(kernel);
	}

	@Override
	public boolean isKernelOpen() {
		return m_kernel != null;
	}

	@Override
	public PythonKernel getKernel() throws DLInvalidEnvironmentException {
		if (m_kernel == null) {
			m_kernel = createKernel();
		}
		return m_kernel;
	}

	@Override
	public String[] execute(final DLCancelable cancelable, final File script, final String... args) throws IOException {
		final String[] pbargs = new String[args.length + 2];
		pbargs[0] = PythonPreferencePage.getPython3Path();
		pbargs[1] = script.getAbsolutePath();
		for (int i = 0; i < args.length; i++) {
			pbargs[i + 2] = args[i];
		}
		final ProcessBuilder pb = new ProcessBuilder(pbargs);
		// Add all python modules to PYTHONPATH variable
		String existingPath = pb.environment().get("PYTHONPATH");
		existingPath = existingPath == null ? "" : existingPath;
		final String externalPythonPath = PythonModuleExtensions.getPythonPath();
		if ((externalPythonPath != null) && !externalPythonPath.isEmpty()) {
			if (existingPath.isEmpty()) {
				existingPath = externalPythonPath;
			} else {
				existingPath = existingPath + File.pathSeparator + externalPythonPath;
			}
		}
		existingPath = existingPath + File.pathSeparator;
		pb.environment().put("PYTHONPATH", existingPath);
		// TODO check if canceled!
		final Process p = pb.start();
		try {
			final StringWriter stdout = new StringWriter();
			IOUtils.copy(p.getInputStream(), stdout, StandardCharsets.UTF_8);

			final StringWriter stderr = new StringWriter();
			IOUtils.copy(p.getErrorStream(), stderr, StandardCharsets.UTF_8);

			return new String[] { stdout.toString(), stderr.toString() };
		} finally {
			p.destroyForcibly();
			IOUtils.closeQuietly(p.getOutputStream());
			IOUtils.closeQuietly(p.getInputStream());
			IOUtils.closeQuietly(p.getErrorStream());
		}
	}

    @Override
    public String[] executeInKernel(final String code, final DLCancelable cancelable)
        throws DLCanceledExecutionException, DLInvalidEnvironmentException, IOException {
        // Copied from PythonKernel
        final AtomicBoolean done = new AtomicBoolean(false);
        final AtomicReference<Exception> exception = new AtomicReference<Exception>(null);
        final Thread nodeExecutionThread = Thread.currentThread();
        final AtomicReference<String[]> output = new AtomicReference<String[]>();
        // Thread running the execute
        ThreadUtils.threadWithContext(new Runnable() {
            @Override
            public void run() {
                String[] out;
                try {
                    out = getKernel().execute(code);
                    output.set(out);
                    // If the error log has content throw it as exception
                    if (!out[1].isEmpty()) {
                        throw new DLInvalidEnvironmentException("An error occurred in Python: " + out[1]);
                    }
                } catch (final Exception e) {
                    exception.set(e);
                }
                done.set(true);
                // Wake up waiting thread
                nodeExecutionThread.interrupt();
            }
        }, "KNIME-DL-Python-Exec-" + THREAD_UNIQUE_ID.incrementAndGet()).start();
        // Wait until execution is done
        while (done.get() != true) {
            try {
                // Wake up once a second to check if execution has been canceled
                Thread.sleep(1000);
            } catch (final InterruptedException e) {
                // Happens if python thread is done
            }
            cancelable.checkCanceled();
        }
        // If their was an exception in the execution thread throw it here
        if (exception.get() != null) {
            narrowPythonException(exception.get());
        }
        return output.get();
    }

    @Override
    public void putDataInKernel(final String name, final TableChunker tableChunker, final int rowsPerChunk, final DLCancelable cancelable)
        throws IOException, DLCanceledExecutionException, DLInvalidEnvironmentException {
        // TODO check if canceled once KNIME python supports canceling the data transfer
        getKernel().putData(name, tableChunker, rowsPerChunk);
    }

    @Override
    public TableCreator<?> getDataFromKernel(final String name, final TableCreatorFactory tcf, final DLCancelable cancelable)
        throws IOException, DLCanceledExecutionException, DLInvalidEnvironmentException {
        // TODO check if canceled once KNIME python supports canceling the data transfer
        return getKernel().getData(name, tcf);
    }

	@Override
	public void close() {
		if (isKernelOpen()) {
			m_kernel.close();
		}
	}

    private static void narrowPythonException(final Exception e)
        throws DLCanceledExecutionException, DLInvalidEnvironmentException, IOException {
        if (e instanceof DLCanceledExecutionException) {
            throw (DLCanceledExecutionException)e;
        } else if (e instanceof DLInvalidEnvironmentException) {
            throw (DLInvalidEnvironmentException)e;
        } else if (e instanceof IOException) {
            throw (IOException)e;
        } else if (e instanceof PythonKernelException) {
            throw new DLInvalidEnvironmentException("An error occurred in Python:", e);
        } else {
            throw new IOException(e);
        }
    }
}
