/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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

import org.apache.commons.io.IOUtils;
import org.knime.dl.core.DLInvalidContextException;
import org.knime.python2.PythonPreferencePage;
import org.knime.python2.kernel.PythonKernel;
import org.knime.python2.kernel.PythonKernelOptions;
import org.knime.python2.kernel.PythonKernelOptions.PythonVersionOption;
import org.knime.python2.kernel.PythonModuleExtensions;

import com.google.common.base.Strings;

/**
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
public final class DLPythonDefaultContext implements DLPythonContext {

	public static PythonKernel createKernel() throws DLInvalidContextException {
		try {
			final PythonKernelOptions options = new PythonKernelOptions();
			options.setPythonVersionOption(PythonVersionOption.PYTHON3);
			return new PythonKernel(options);
		} catch (final IOException e) {
			final String msg = !Strings.isNullOrEmpty(e.getMessage())
					? "An error occurred while trying to launch Python: " + e.getMessage()
					: "An unknown error occurred while trying to launch Python. See log for details.";
			throw new DLInvalidContextException(msg, e);
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
	public PythonKernel getKernel() throws DLInvalidContextException {
		if (m_kernel == null) {
			m_kernel = createKernel();
		}
		return m_kernel;
	}

	@Override
	public String[] execute(File script, final String... args) throws IOException {
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
	public void close() {
		if (isKernelOpen()) {
			m_kernel.close();
		}
	}
}
