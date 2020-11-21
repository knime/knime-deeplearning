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
 *   May 17, 2017 (marcel): created
 */
package org.knime.dl.core.execution;

import java.util.Set;

import org.knime.dl.core.DLContext;
import org.knime.dl.core.DLFixedTensorShape;
import org.knime.dl.core.DLNetwork;
import org.knime.dl.core.DLNetworkInputPreparer;
import org.knime.dl.core.DLTensorId;
import org.knime.dl.core.DLTensorSpec;

/**
 * Represents the execution back end for a certain network type. Creates {@link DLNetworkExecutionSession execution
 * sessions} to execute networks.
 *
 * @param <N> the {@link DLNetwork network} type for which to create {@link DLNetworkExecutionSession execution
 *            sessions}
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public interface DLExecutionContext<C, N extends DLNetwork> extends DLContext<C, N> {

    C createDefaultContext();

	/**
	 * Creates a {@link DLNetworkExecutionSession execution session} for a given {@link DLNetwork network}.
	 *
	 * @param network the network to execute
	 * @param executionInputSpecs a set of fully defined tensor specs. The set of tensor specs must exactly match the
	 *            network's input tensor specs with respect to the identifiers of the contained specs. A tensor spec is
	 *            fully defined if it features a non-empty batch size and a {@link DLFixedTensorShape fixed tensor
	 *            shape}.
	 * @param requestedOutputs a set of tensor ids whose corresponding network outputs shall be fed to the output
	 *            consumer. This is useful if only parts of the network's outputs are relevant or if interested in the
	 *            outputs of hidden layers.
	 * @param inputPreparer the input data preparer
	 * @param outputConsumer the network output consumer
	 * @return the created execution session
	 * @throws IllegalArgumentException if failed to create the execution session due to invalid arguments
	 */
	DLNetworkExecutionSession createExecutionSession(C context, N network, Set<DLTensorSpec> executionInputSpecs,
			Set<DLTensorId> requestedOutputs, DLNetworkInputPreparer inputPreparer,
			DLNetworkOutputConsumer outputConsumer);

}
