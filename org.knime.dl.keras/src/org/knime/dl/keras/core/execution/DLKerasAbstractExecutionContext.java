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
 * History
 *   May 19, 2017 (marcel): created
 */
package org.knime.dl.keras.core.execution;

import java.util.Set;

import org.knime.dl.core.DLLayerDataFactory;
import org.knime.dl.core.DLLayerDataRegistry;
import org.knime.dl.core.DLLayerDataSpec;
import org.knime.dl.core.execution.DLExecutionContext;
import org.knime.dl.keras.core.DLKerasNetwork;
import org.knime.dl.keras.core.DLKerasNetworkType;

/**
 * Executes a {@link DLKerasAbstractExecutableNetwork}.
 *
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
public abstract class DLKerasAbstractExecutionContext<NT extends DLKerasNetworkType<N, ?>, N extends DLKerasNetwork<?>>
		implements DLExecutionContext<N> {

	private final NT m_networkType;

	private final String m_name;

	private final DLLayerDataFactory m_layerDataFactory;

	protected DLKerasAbstractExecutionContext(final NT networkType, final String name) {
		m_networkType = networkType;
		m_name = name;
		m_layerDataFactory = DLLayerDataRegistry.getInstance().getLayerDataFactory(m_networkType)
				.orElseThrow(() -> new IllegalStateException("Deep learning network type '" + m_networkType
						+ "' is not supported. No layer data factory found."));
	}

	@Override
	public abstract DLKerasExecutableNetworkAdapter executable(N network, Set<DLLayerDataSpec> requestedOutputs);

	@Override
	public NT getNetworkType() {
		return m_networkType;
	}

	@Override
	public String getName() {
		return m_name;
	}

	@Override
	public DLLayerDataFactory getLayerDataFactory() {
		return m_layerDataFactory;
	}
}
