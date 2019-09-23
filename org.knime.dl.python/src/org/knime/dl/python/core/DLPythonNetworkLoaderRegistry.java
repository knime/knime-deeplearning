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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.eclipse.core.runtime.IConfigurationElement;
import org.knime.core.node.NodeLogger;
import org.knime.dl.core.DLAbstractExtensionPointRegistry;
import org.knime.dl.core.DLCanceledExecutionException;
import org.knime.dl.core.DLInstallationTestTimeout;
import org.knime.dl.core.DLInstallationTestTimeoutException;
import org.knime.dl.core.DLMissingDependencyException;
import org.knime.dl.core.DLNotCancelable;

/**
 * Registry for deep learning {@link DLPythonNetworkLoader network Python loaders}.
 *
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public final class DLPythonNetworkLoaderRegistry extends DLAbstractExtensionPointRegistry {

	private static final String EXT_POINT_ID = "org.knime.dl.python.DLPythonNetworkLoader";

	private static final String EXT_POINT_ATTR_CLASS = "DLPythonNetworkLoader";

	private static final NodeLogger LOGGER = NodeLogger.getLogger(DLPythonNetworkLoaderRegistry.class);

	private static DLPythonNetworkLoaderRegistry instance;

	/**
	 * Returns the singleton instance.
	 *
	 * @return the singleton instance
	 */
	public static synchronized DLPythonNetworkLoaderRegistry getInstance() {
		if (instance == null) {
			instance = new DLPythonNetworkLoaderRegistry();
            // First set instance, then register. Registering usually activates other bundles. Those may try to access
            // this registry (while the instance is still null) which would trigger another instance construction.
            instance.register();
            instance.testInstallation();
		}
		return instance;
	}

	private final Map<Class<?>, DLPythonNetworkLoader<?>> m_loaders = new HashMap<>();

    private DLPythonNetworkLoaderRegistry() {
        super(EXT_POINT_ID, EXT_POINT_ATTR_CLASS);
        // Do not trigger registration or perform installation tests here. See #getInstance() above.
    }

	/**
     * @return the installation test timeout as returned by
     *         {@link DLInstallationTestTimeout#getInstallationTestTimeout()}
     */
	public static int getInstallationTestTimeout() {
        return DLInstallationTestTimeout.getInstallationTestTimeout();
	}

	// access methods:

	/**
	 * Returns all registered Python network loaders.
	 *
	 * @return all registered Python network loaders
	 */
	public Collection<DLPythonNetworkLoader<?>> getAllNetworkLoaders() {
		return Collections.unmodifiableCollection(m_loaders.values());
	}

	/**
	 * Returns the Python network loader that is associated with the given network type if present.
	 *
	 * @param networkType the network type
	 * @return the loader if present
	 */
	public <N extends DLPythonNetwork> Optional<DLPythonNetworkLoader<N>> getNetworkLoader(final Class<N> networkType) {
		@SuppressWarnings("unchecked") // this is guaranteed by the interface
		final DLPythonNetworkLoader<N> loader = (DLPythonNetworkLoader<N>) m_loaders.get(networkType);
		return Optional.ofNullable(loader);
	}

	/**
	 * Returns the Python network loader that is associated with the network type of the given name if present.
	 *
	 * @param networkTypeName the name of the network type
	 * @return the loader if present
	 */
	public Optional<DLPythonNetworkLoader<?>> getNetworkLoader(final String networkTypeName) {
		for (final Entry<Class<?>, DLPythonNetworkLoader<?>> entry : m_loaders.entrySet()) {
			if (entry.getKey().getCanonicalName().equals(networkTypeName)) {
				return Optional.of(entry.getValue());
			}
		}
		return Optional.empty();
	}
	// :access methods

	// registration:

	/**
	 * Registers the given Python network loader.
	 *
	 * @param loader the Python loader to register
	 * @throws IllegalArgumentException if there is already a loader registered for the given loader's network type or
	 *             if the given loader's network type is null
	 */
	public void registerExecutionContext(final DLPythonNetworkLoader<?> loader) throws IllegalArgumentException {
		registerPythonLoaderInternal(loader);
	}

	@Override
	protected void registerInternal(final IConfigurationElement elem, final Map<String, String> attrs)
			throws Throwable {
		registerPythonLoaderInternal((DLPythonNetworkLoader<?>) elem.createExecutableExtension(EXT_POINT_ATTR_CLASS));
	}

	private synchronized void registerPythonLoaderInternal(final DLPythonNetworkLoader<?> loader) {
		final Class<?> networkType = loader.getNetworkType();
		if (m_loaders.containsKey(networkType)) {
			throw new IllegalArgumentException(
					"There is already a Python network loader registered for the given loader's network type.");
		}
		if (networkType == null) {
			throw new IllegalArgumentException("The Python network loader's associated network type must not be null.");
		}
		m_loaders.put(networkType, loader);
	}
	// :registration

    private void testInstallation() {
        // Test each installation independently.
        for (final DLPythonNetworkLoader<?> loader : m_loaders.values()) {
            new Thread(() -> {
                try {
                    loader.checkAvailability(true, getInstallationTestTimeout(), DLNotCancelable.INSTANCE);
                } catch (final DLInstallationTestTimeoutException e) {
                    Thread.currentThread().interrupt();
                    LOGGER.debug("Installation test for deep learning Python back end '"
                        + loader.getNetworkType().getCanonicalName() + "' timed out or was interrupted.");
                } catch (final DLMissingDependencyException e) {
                    LOGGER.debug("Installation test for deep learning Python back end '"
                        + loader.getNetworkType().getCanonicalName() + "' failed: " + e.getMessage());
                } catch (final DLCanceledExecutionException e) {
                    // Doesn't happen.
                }
            }, "DL-Installation-Test-Trigger-" + loader.getNetworkType().getName()).start();
        }
    }
}
