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
 *   May 22, 2017 (marcel): created
 */
package org.knime.dl.core.backend;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.knime.core.node.NodeLogger;

/**
 * Registry for deep learning {@link DLBackend back ends}.
 *
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
public final class DLBackendRegistry {

    /**
     * The identifier of the {@link DLBackend} extension point.
     */
    public static final String EXT_POINT_ID = "org.knime.dl.DLBackend";

    /**
     * The attribute that specifies the extending back end class.
     */
    public static final String EXT_POINT_ATTR_BACKEND_CLASS = "DLBackend";

    private static final NodeLogger LOGGER = NodeLogger.getLogger(DLBackendRegistry.class);

    // lazy initialization
    private static DLBackendRegistry instance;

    /**
     * Returns the singleton instance.
     *
     * @return the global deep learning back end extension registry
     */
    public static DLBackendRegistry getInstance() {
        if (instance == null) {
            synchronized (DLBackendRegistry.class) {
                if (instance == null) {
                    instance = new DLBackendRegistry();
                }
            }
        }
        return instance;
    }

    /**
     * Returns all registered deep learning {@link DLBackend back ends}.
     *
     * @return the deep learning back ends
     */
    public static Collection<DLBackend> getAllBackends() {
        return Collections.unmodifiableCollection(getInstance().m_backends.values());
    }

    /**
     * Returns the {@link DLBackend back end} that corresponds to the given identifier if it does exist.
     *
     * @param identifier the identifier
     * @return the back end if it does exist
     */
    public static Optional<DLBackend> getBackend(final String identifier) {
        final DLBackend backend = getInstance().m_backends.get(identifier);
        if (backend == null) {
            return Optional.empty();
        }
        return Optional.of(backend);
    }

    /**
     * Returns the preferred {@link DLBackend back end} for the given {@link DLProfile profile}.
     *
     * @param profile the profile
     * @return the preferred back end
     * @throws NullPointerException if the argument is null
     * @throws IllegalArgumentException if the argument is empty
     */
    public static DLBackend getPreferredBackend(final DLProfile profile)
        throws NullPointerException, IllegalArgumentException {
        checkNotNull(profile);
        checkArgument(profile.size() > 0);
        if (profile.size() == 1) {
            return profile.iterator().next();
        } else {
            // TODO
            return profile.iterator().next();
        }
    }

    /**
     * Returns all {@link DLBackend back ends} that have the given friendly name.
     *
     * @param name the friendly name
     * @return the back ends, not null
     */
    public static Collection<DLBackend> getBackends(final String name) {
        final ArrayList<DLBackend> backends = new ArrayList<>(1);
        for (final DLBackend be : getInstance().m_backends.values()) {
            if (be.getName().equals(name)) {
                backends.add(be);
            }
        }
        return backends;
    }

    /**
     * Registers the given {@link DLBackend back end}.
     *
     * @param backend the back end to register
     * @throws IllegalArgumentException if a back end with the same identifier is already registered or if the given
     *             backend's identifier or name is null or empty
     */
    public static void register(final DLBackend backend) throws IllegalArgumentException {
        getInstance().registerBackend(backend);
    }

    private final HashMap<String, DLBackend> m_backends;

    private DLBackendRegistry() {
        m_backends = new HashMap<>();
        // register back ends
        try {
            final IExtensionRegistry registry = Platform.getExtensionRegistry();
            final IExtensionPoint point = registry.getExtensionPoint(EXT_POINT_ID);
            if (point == null) {
                final String msg = "Invalid extension point: '" + EXT_POINT_ID + "'.";
                LOGGER.error(msg);
                throw new IllegalStateException(msg);
            }
            for (final IConfigurationElement elem : point.getConfigurationElements()) {
                final String backend = elem.getAttribute(EXT_POINT_ATTR_BACKEND_CLASS);
                final String extension = elem.getDeclaringExtension().getUniqueIdentifier();
                if (backend == null || backend.isEmpty()) {
                    LOGGER.error("The extension '" + extension + "' doesn't provide the required attribute '"
                        + EXT_POINT_ATTR_BACKEND_CLASS + "'.");
                    LOGGER.error("Extension '" + extension + "' was ignored.");
                    continue;
                }
                try {
                    final DLBackend theBackend =
                        (DLBackend)elem.createExecutableExtension(EXT_POINT_ATTR_BACKEND_CLASS);
                    registerBackend(theBackend);
                } catch (final Throwable t) {
                    LOGGER.error("An error or exception occurred while initializing the deep learning back end '"
                        + backend + "'.", t);
                    LOGGER.error("Extension '" + extension + "' was ignored.", t);
                    continue;
                }
            }
        } catch (final Exception e) {
            LOGGER.error("An exception occurred while registering deep learning back ends.", e);
        }
    }

    private synchronized void registerBackend(final DLBackend backend) throws IllegalArgumentException {
        final String id = backend.getIdentifier();
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("The back end's id must be neither null nor empty.");
        }
        final String name = backend.getName();
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("The back end's name must be neither null nor empty.");
        }
        if (m_backends.containsKey(id)) {
            throw new IllegalArgumentException("A back end with id '" + id + "' is already registered.");
        }
        m_backends.put(id, backend);
    }
}
