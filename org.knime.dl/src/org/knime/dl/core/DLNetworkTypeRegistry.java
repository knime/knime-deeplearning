/// *
// * ------------------------------------------------------------------------
// *
// * Copyright by KNIME AG, Zurich, Switzerland
// * Website: http://www.knime.com; Email: contact@knime.com
// *
// * This program is free software; you can redistribute it and/or modify
// * it under the terms of the GNU General Public License, Version 3, as
// * published by the Free Software Foundation.
// *
// * This program is distributed in the hope that it will be useful, but
// * WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with this program; if not, see <http://www.gnu.org/licenses>.
// *
// * Additional permission under GNU GPL version 3 section 7:
// *
// * KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
// * Hence, KNIME and ECLIPSE are both independent programs and are not
// * derived from each other. Should, however, the interpretation of the
// * GNU GPL Version 3 ("License") under any applicable laws result in
// * KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
// * you the additional permission to use and propagate KNIME together with
// * ECLIPSE with only the license terms in place for ECLIPSE applying to
// * ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
// * license terms of ECLIPSE themselves allow for the respective use and
// * propagation of ECLIPSE together with KNIME.
// *
// * Additional permission relating to nodes for KNIME that extend the Node
// * Extension (and in particular that are based on subclasses of NodeModel,
// * NodeDialog, and NodeView) and that only interoperate with KNIME through
// * standard APIs ("Nodes"):
// * Nodes are deemed to be separate and independent programs and to not be
// * covered works. Notwithstanding anything to the contrary in the
// * License, the License does not apply to Nodes, you are not required to
// * license Nodes under the License, and you are granted a license to
// * prepare and propagate Nodes, in each case even if such Nodes are
// * propagated with or for interoperation with KNIME. The owner of a Node
// * may freely choose the license terms applicable to such Node, including
// * when such Node is propagated with or for interoperation with KNIME.
// * ---------------------------------------------------------------------
// *
// */
// package org.knime.dl.core;
//
// import java.util.ArrayList;
// import java.util.Collection;
// import java.util.Collections;
// import java.util.HashMap;
// import java.util.Map;
// import java.util.Optional;
//
// import org.eclipse.core.runtime.IConfigurationElement;
//
/// **
// * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
// * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
// */
// public class DLNetworkTypeRegistry extends DLAbstractExtensionPointRegistry {
//
// private static final String EXT_POINT_ID = "org.knime.dl.DLNetworkType";
//
// private static final String EXT_POINT_ATTR_CLASS = "DLNetworkType";
//
// private static DLNetworkTypeRegistry instance;
//
// /**
// * Returns the singleton instance.
// *
// * @return the singleton instance
// */
// public static synchronized DLNetworkTypeRegistry getInstance() {
// if (instance == null) {
// instance = new DLNetworkTypeRegistry();
// }
// return instance;
// }
//
// private final HashMap<String, DLNetworkType> m_types = new HashMap<>();
//
// private DLNetworkTypeRegistry() {
// super(EXT_POINT_ID, EXT_POINT_ATTR_CLASS);
// register();
// for (final DLNetworkType type : m_types.values()) {
// try {
// type.checkAvailability(true);
// } catch (final DLUnavailableDependencyException e) {
// // ignore - we just want to trigger installation tests here
// }
// }
// }
//
// // access methods:
//
// /**
// * Returns the network type with the given identifier if present.
// *
// * @param identifier the identifier
// * @return the network type if present
// */
// public Optional<DLNetworkType> getNetworkType(final String identifier) {
// return Optional.ofNullable(m_types.get(identifier));
// }
//
// /**
// * Returns all registered network types.
// *
// * @return all registered network types
// */
// public Collection<DLNetworkType> getAllNetworkTypes() {
// return Collections.unmodifiableCollection(m_types.values());
// }
//
// /**
// * Returns all registered, available network types. {@link DLExternalNetwork External networks} are available if
// * their {@link DLExternalNetworkType#checkAvailability() external dependencies} are available.
// *
// * @return all registered, available network types
// *
// * @see DLExternalNetworkType#checkAvailability()
// * @see DLInstallationTester
// */
// public Collection<DLNetworkType> getAllAvailableNetworkTypes() {
// final ArrayList<DLNetworkType> types = new ArrayList<>(m_types.values());
// for (int i = types.size() - 1; i >= 0; i--) {
// final DLNetworkType type = types.get(i);
// try {
// type.checkAvailability(false);
// } catch (final DLUnavailableDependencyException e) {
// types.remove(i);
// }
// }
// return types;
// }
// // :access methods
//
// // registration:
//
// /**
// * Registers a network type.
// *
// * @param type the network type
// *
// * @throws IllegalArgumentException if the network type is already registered
// */
// public void registerNetworkType(final DLNetworkType type) throws IllegalArgumentException {
// registerNetworkTypeInternal(type);
// }
//
// @Override
// protected void registerInternal(final IConfigurationElement elem, final Map<String, String> attrs)
// throws Throwable {
// registerNetworkTypeInternal((DLNetworkType) elem.createExecutableExtension(EXT_POINT_ATTR_CLASS));
// }
//
// private synchronized void registerNetworkTypeInternal(final DLNetworkType type) {
// if (!m_types.containsKey(type.getIdentifier())) {
// m_types.put(type.getIdentifier(), type);
// } else {
// throw new IllegalArgumentException(
// "The network type with identifier '" + type.getIdentifier() + "' is already registered!");
// }
// }
// // :registration
// }
