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
package org.knime.dl.core.execution;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IConfigurationElement;
import org.knime.dl.core.DLAbstractExtensionPointRegistry;
import org.knime.dl.core.DLNetwork;

/**
 * Registry for deep learning {@link DLExecutionContext execution contexts}.
 *
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public final class DLExecutionContextRegistry extends DLAbstractExtensionPointRegistry {

	private static final String EXT_POINT_ID = "org.knime.dl.DLExecutionContext";

	private static final String EXT_POINT_ATTR_CLASS = "DLExecutionContext";

	private static DLExecutionContextRegistry instance;

	/**
	 * Returns the singleton instance.
	 *
	 * @return the singleton instance
	 */
	public static synchronized DLExecutionContextRegistry getInstance() {
		if (instance == null) {
			instance = new DLExecutionContextRegistry();
		}
		return instance;
	}

	private final Set<DLExecutionContext<?>> m_ctxs = new HashSet<>();

	private DLExecutionContextRegistry() {
		super(EXT_POINT_ID, EXT_POINT_ATTR_CLASS);
		register();
	}

	// access methods:

	/**
	 * Returns all execution contexts that are compatible to the given network type.
	 *
	 * @param networkType the network type
	 * @return the execution contexts
	 */
	public Collection<DLExecutionContext<?>> getExecutionContextsForNetworkType(
			final Class<? extends DLNetwork> networkType) {
		return m_ctxs.stream() //
				.filter(ctx -> ctx.getNetworkType().isAssignableFrom(networkType)) //
				.collect(Collectors.toList());
	}

	/**
	 * Returns the execution context with the given identifier if present.
	 *
	 * @param identifier the identifier
	 * @return the execution context if present
	 */
	public Optional<DLExecutionContext<?>> getExecutionContext(final String identifier) {
		return m_ctxs.stream() //
				.filter(ctx -> ctx.getIdentifier().equals(identifier)) //
				.findFirst();
	}
	// :access methods

	// registration:

	/**
	 * Registers the given execution context.
	 *
	 * @param executionContext the execution context to register
	 * @throws IllegalArgumentException if the given execution context's network type is null
	 */
	public void registerExecutionContext(final DLExecutionContext<?> executionContext) throws IllegalArgumentException {
		registerExecutionContextInternal(executionContext);
	}

	@Override
	protected void registerInternal(final IConfigurationElement elem, final Map<String, String> attrs)
			throws Throwable {
		registerExecutionContextInternal((DLExecutionContext<?>) elem.createExecutableExtension(EXT_POINT_ATTR_CLASS));
	}

	private synchronized void registerExecutionContextInternal(final DLExecutionContext<?> ctx) {
		final Class<? extends DLNetwork> networkType = ctx.getNetworkType();
		if (networkType == null) {
			throw new IllegalArgumentException("The execution context's associated network type must not be null.");
		}
		m_ctxs.add(ctx);
	}
	// :registration
}
