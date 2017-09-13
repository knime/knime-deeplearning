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
package org.knime.dl.keras.theano.core;

import java.io.IOException;

import org.knime.dl.core.DLInvalidContextException;
import org.knime.dl.core.DLLayerDataSpec;
import org.knime.dl.keras.core.DLKerasAbstractCommands;
import org.knime.dl.keras.core.DLKerasLayerDataSpecTableCreatorFactory;
import org.knime.dl.python.core.DLPythonAbstractCommandsConfig;
import org.knime.dl.python.core.DLPythonContext;
import org.knime.dl.python.core.DLPythonNetworkHandle;
import org.knime.dl.python.core.data.DLPythonTypeMap;
import org.knime.python2.kernel.PythonKernel;

/**
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
public final class DLKerasTheanoCommands extends DLKerasAbstractCommands<DLKerasTheanoCommandsConfig> {

	public DLKerasTheanoCommands() throws DLInvalidContextException {
		super(new DLKerasTheanoCommandsConfig());
	}

	public DLKerasTheanoCommands(final DLPythonContext context) throws DLInvalidContextException {
		super(new DLKerasTheanoCommandsConfig(), context);
	}

	@Override
	public DLKerasTheanoNetworkSpec extractNetworkSpec(final DLPythonNetworkHandle handle,
			final DLPythonTypeMap typeMap) throws DLInvalidContextException, IOException {
		final PythonKernel kernel = m_context.getKernel();
		kernel.execute(m_config.getExtractNetworkSpecsCode(handle));
		final DLLayerDataSpec[] inputSpecs =
				(DLLayerDataSpec[]) kernel.getData(DLPythonAbstractCommandsConfig.INPUT_SPECS_NAME,
						new DLKerasLayerDataSpecTableCreatorFactory(typeMap)).getTable();
		// final DLLayerDataSpec[] intermediateOutputSpecs =
		// (DLLayerDataSpec[]) m_kernel.getData(DLPythonCommandsConfig.INTERMEDIATE_OUTPUT_SPECS_NAME,
		// new DLKerasLayerDataSpecTableCreatorFactory(typeMap)).getTable();
		final DLLayerDataSpec[] outputSpecs =
				(DLLayerDataSpec[]) kernel.getData(DLPythonAbstractCommandsConfig.OUTPUT_SPECS_NAME,
						new DLKerasLayerDataSpecTableCreatorFactory(typeMap)).getTable();

		// TODO: Keras does not expose "intermediate/hidden outputs" (see above) for the moment as we're not yet able to
		// extract those via the executor node. Support for this will be added in a future enhancement patch.
		return new DLKerasTheanoNetworkSpec(inputSpecs, new DLLayerDataSpec[0] /* TODO intermediateOutputSpecs */,
				outputSpecs);
	}
}
