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
package org.knime.dl.keras.cntk.core;

import java.io.File;
import java.io.IOException;

import org.knime.core.util.Version;
import org.knime.dl.core.DLCancelable;
import org.knime.dl.core.DLCanceledExecutionException;
import org.knime.dl.core.DLInvalidEnvironmentException;
import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.keras.core.DLKerasAbstractCommands;
import org.knime.dl.python.core.DLPythonContext;
import org.knime.dl.python.core.DLPythonNetworkHandle;
import org.knime.dl.python.core.DLPythonNumPyTypeMap;
import org.knime.dl.python.core.DLPythonTensorSpecTableCreatorFactory;
import org.knime.dl.python.core.SingleValueTableCreator;
import org.knime.dl.util.DLUtils;
import org.knime.python2.extensions.serializationlibrary.interfaces.Cell;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public final class DLKerasCNTKCommands extends DLKerasAbstractCommands {

	public DLKerasCNTKCommands() throws DLInvalidEnvironmentException {
	}

	public DLKerasCNTKCommands(final DLPythonContext context) throws DLInvalidEnvironmentException {
		super(context);
	}

    @Override
    public DLKerasCNTKNetworkSpec extractNetworkSpec(final DLPythonNetworkHandle handle, final DLCancelable cancelable)
        throws DLInvalidEnvironmentException, IOException, DLCanceledExecutionException {
        getContext(cancelable).executeInKernel(getExtractNetworkSpecsCode(handle), cancelable);
        final DLTensorSpec[] inputSpecs = (DLTensorSpec[])getContext(cancelable).getDataFromKernel(INPUT_SPECS_NAME,
            new DLPythonTensorSpecTableCreatorFactory(DLPythonNumPyTypeMap.INSTANCE), cancelable).getTable();
        final DLTensorSpec[] hiddenOutputSpecs =
            (DLTensorSpec[])getContext(cancelable).getDataFromKernel(HIDDEN_OUTPUT_SPECS_NAME,
                new DLPythonTensorSpecTableCreatorFactory(DLPythonNumPyTypeMap.INSTANCE), cancelable).getTable();
        final DLTensorSpec[] outputSpecs = (DLTensorSpec[])getContext(cancelable).getDataFromKernel(OUTPUT_SPECS_NAME,
            new DLPythonTensorSpecTableCreatorFactory(DLPythonNumPyTypeMap.INSTANCE), cancelable).getTable();

        // Get the python version
        getContext(cancelable).executeInKernel(getExtractPythonVersionCode(), cancelable);
        final String pythonVersion = (String)getContext(cancelable).getDataFromKernel(PYTHON_VERSION_NAME,
            (s, ts) -> new SingleValueTableCreator<>(s, Cell::getStringValue), cancelable).getTable();

        // TODO Get the keras version from python
        final Version kerasVersion = null;

        return new DLKerasCNTKNetworkSpec(new Version(pythonVersion), kerasVersion, inputSpecs, hiddenOutputSpecs,
            outputSpecs);
    }

	@Override
	protected String getSetupEnvironmentCode() {
		return "import os\n" + //
				"os.environ['KERAS_BACKEND'] = 'cntk'\n";
	}

	@Override
	protected File getInstallationTestFile() throws IOException {
		return DLUtils.Files.getFileFromSameBundle(this, "py/DLKerasCNTKNetworkTester.py");
	}

	@Override
	protected String getSetupBackendCode() {
		return "";
	}

    @Override
    protected DLKerasCNTKNetworkReaderCommands getNetworkReaderCommands() {
        return new DLKerasCNTKNetworkReaderCommands();
    }

    private static class DLKerasCNTKNetworkReaderCommands extends DLKerasAbstractNetworkReaderCommands {

        private DLKerasCNTKNetworkReaderCommands() {
            super("from DLKerasCNTKNetwork import DLKerasCNTKNetworkReader", "DLKerasCNTKNetworkReader()");
        }
    }
}
