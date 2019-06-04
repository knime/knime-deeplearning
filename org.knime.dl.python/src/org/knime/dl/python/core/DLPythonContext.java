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

import java.io.File;
import java.io.IOException;

import org.knime.dl.core.DLCancelable;
import org.knime.dl.core.DLCanceledExecutionException;
import org.knime.dl.core.DLInvalidEnvironmentException;
import org.knime.dl.core.DLUncheckedException;
import org.knime.python2.extensions.serializationlibrary.interfaces.TableChunker;
import org.knime.python2.extensions.serializationlibrary.interfaces.TableCreator;
import org.knime.python2.extensions.serializationlibrary.interfaces.TableCreatorFactory;
import org.knime.python2.kernel.PythonKernel;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public interface DLPythonContext extends AutoCloseable {

	boolean isKernelOpen();

	PythonKernel getKernel() throws DLInvalidEnvironmentException;

	// NB: we cannot offer an execute method that allows direct execution of a source code string as there are known
	// issues when trying to execute Python with "-c" option enabled from a Windows batch file.

	/**
	 * Executes the given script with the given arguments.
	 *
     * @param cancelable to check if execution has been canceled
     * @param script the Python script
     * @param args the arguments of the Python script
	 * @return stdout and stderr in an array list
	 * @throws IOException if an error occurred while starting the Python process
	 * @throws DLCanceledExecutionException if the execution has been canceled
	 */
	String[] execute(DLCancelable cancelable, File script, String... args) throws IOException, DLCanceledExecutionException;

	/**
	 * Executes the given source code.
	 *
	 * @param code the Python source code
     * @param cancelable to check if execution has been canceled
	 * @return stdout and stderr in an array list
	 * @throws DLCanceledExecutionException if the execution has been canceled
	 * @throws DLInvalidEnvironmentException if execution failed, i.e. if the Python kernel returns an error output
     * @throws IOException if an error occurred while communicating with the Python kernel
	 */
	String[] executeInKernel(final String code, DLCancelable cancelable)
	        throws DLCanceledExecutionException, DLInvalidEnvironmentException, IOException;

    String[] executeAsyncInKernel(final String code, DLCancelable cancelable)
        throws DLCanceledExecutionException, DLInvalidEnvironmentException, IOException;

    void putDataInKernel(String name, final TableChunker tableChunker, final int rowsPerChunk, DLCancelable cancelable)
        throws IOException, DLCanceledExecutionException, DLInvalidEnvironmentException;

    TableCreator<?> getDataFromKernel(String name, TableCreatorFactory tcf, DLCancelable cancelable)
        throws IOException, DLCanceledExecutionException, DLInvalidEnvironmentException;

    /**
     * Set the environment variable in the running Python process.
     *
     * @param name name of the environment variable
     * @param value value of the environment variable
     * @param cancelable to check if execution has been canceled
	 * @throws DLCanceledExecutionException if the execution has been canceled
	 * @throws DLInvalidEnvironmentException if execution failed, i.e. if the Python kernel returns an error output
     * @throws IOException if an error occurred while communicating with the Python kernel
     * @since 3.8
     */
    void setEnvironmentVariable(String name, String value, DLCancelable cancelable) throws DLCanceledExecutionException, DLInvalidEnvironmentException, IOException;

    /**
     * @throws DLUncheckedException if an exception occurred while cleaning up the underlying Python kernel
     */
	@Override
	void close();
}
