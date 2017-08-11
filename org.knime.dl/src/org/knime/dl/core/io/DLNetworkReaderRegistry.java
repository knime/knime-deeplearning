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
package org.knime.dl.core.io;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.core.runtime.IConfigurationElement;
import org.knime.dl.core.DLAbstractExtensionPointRegistry;
import org.knime.dl.core.DLNetworkType;

/**
 * Registry for deep learning {@link DLNetworkReader network handlers}.
 *
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
public class DLNetworkReaderRegistry extends DLAbstractExtensionPointRegistry {

	private static final String EXT_POINT_ID = "org.knime.dl.DLNetworkReader";

	private static final String EXT_POINT_ATTR_CLASS = "DLNetworkReader";

	private static DLNetworkReaderRegistry instance;

	/**
	 * Returns the singleton instance.
	 *
	 * @return the singleton instance
	 */
	public static DLNetworkReaderRegistry getInstance() {
		if (instance == null) {
			synchronized (DLNetworkReaderRegistry.class) {
				if (instance == null) {
					instance = new DLNetworkReaderRegistry();
				}
			}
		}
		return instance;
	}

	// TODO: mapping from network type does not make sense anymore as we check
	// for class compatibility in the access
	// method.
	private final HashMap<DLNetworkType<?, ?>, Set<DLNetworkReader<?, ?, ?>>> m_readers = new HashMap<>();

	private DLNetworkReaderRegistry() {
		super(EXT_POINT_ID, EXT_POINT_ATTR_CLASS);
		register();
	}

	// access methods:

	public Collection<DLNetworkReader<?, ?, ?>> getNetworkReadersForType(final DLNetworkType<?, ?> networkType) {
		return Collections.unmodifiableCollection(m_readers.get(networkType));
	}

	public Optional<DLNetworkReader<?, ?, ?>> getNetworkReader(final String identifier) {
		return m_readers.values().stream().flatMap(Set::stream).filter(r -> r.getIdentifier().equals(identifier))
				.findFirst();
	}
	// :access methods

	// registration:

	/**
	 * Registers the given network handler.
	 *
	 * @param formatHandler
	 *            the network handler to register
	 * @throws IllegalArgumentException
	 *             if the given network handlers's network type is null
	 */
	public void registerFormatHandler(final DLNetworkReader<?, ?, ?> formatHandler) throws IllegalArgumentException {
		registerFormatHandlerInternal(formatHandler);
	}

	@Override
	protected void registerInternal(final IConfigurationElement elem, final Map<String, String> attrs)
			throws Throwable {
		registerFormatHandlerInternal((DLNetworkReader<?, ?, ?>) elem.createExecutableExtension(EXT_POINT_ATTR_CLASS));
	}

	private synchronized void registerFormatHandlerInternal(final DLNetworkReader<?, ?, ?> formatHandler) {
		final DLNetworkType<?, ?> networkType = formatHandler.getNetworkType();
		if (networkType == null) {
			throw new IllegalArgumentException("The network handler's associated network type must not be null.");
		}
		Set<DLNetworkReader<?, ?, ?>> handlers = m_readers.get(networkType);
		if (handlers == null) {
			handlers = new HashSet<>();
			m_readers.put(networkType, handlers);
		}
		handlers.add(formatHandler);
	}
	// :registration
}
