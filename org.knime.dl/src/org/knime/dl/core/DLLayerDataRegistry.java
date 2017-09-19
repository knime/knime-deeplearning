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
package org.knime.dl.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.eclipse.core.runtime.IConfigurationElement;

/**
 * Registry for deep learning layer data factories that create {@link LayerData layer data} representations for certain
 * network types.
 *
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
public final class DLLayerDataRegistry extends DLAbstractExtensionPointRegistry {

	// TODO: needed? could be property of the type - this is a 1:1 mapping anyway...
	// or we register pairs of execution/training contexts and layer data factories - that's where we need them anyway
	// (could be part of the exec/training registries)

	private static final String EXT_POINT_ID = "org.knime.dl.DLLayerDataFactory";

	private static final String EXT_POINT_ATTR_CLASS = "DLLayerDataFactory";

	private static DLLayerDataRegistry instance;

	/**
	 * Returns the singleton instance.
	 *
	 * @return the singleton instance
	 */
	public static synchronized DLLayerDataRegistry getInstance() {
		if (instance == null) {
			instance = new DLLayerDataRegistry();
		}
		return instance;
	}

	private final HashMap<DLNetworkType<?, ?, ?>, DLLayerDataFactory> m_layerData = new HashMap<>();

	public DLLayerDataRegistry() {
		super(EXT_POINT_ID, EXT_POINT_ATTR_CLASS);
		register();
	}

	// access methods:

	/**
	 * Returns the layer data factory that is compatible to the given network type if present.
	 *
	 * @param network the network type
	 * @return the layer data factory
	 */
	public Optional<DLLayerDataFactory> getLayerDataFactory(final DLNetworkType<?, ?, ?> networkType) {
		final DLLayerDataFactory layerData = m_layerData.get(networkType);
		return Optional.ofNullable(layerData);
	}
	// :access methods

	// registration:

	/**
	 * Registers the given layer data factory.
	 *
	 * @param layerData the layer data factory to register
	 * @throws IllegalArgumentException if a layer data factory of the same network type is already registered or if the
	 *             given layer data factory's network type is null
	 */
	public void registerLayerData(final DLLayerDataFactory layerData) throws IllegalArgumentException {
		registerLayerDataInternal(layerData);
	}

	@Override
	protected void registerInternal(final IConfigurationElement elem, final Map<String, String> attrs)
			throws Throwable {
		registerLayerDataInternal((DLLayerDataFactory) elem.createExecutableExtension(EXT_POINT_ATTR_CLASS));
	}

	private synchronized void registerLayerDataInternal(final DLLayerDataFactory layerData) {
		final DLNetworkType<?, ?, ?> networkType = layerData.getNetworkType();
		if (networkType == null) {
			throw new IllegalArgumentException("The layer data factory's associated network type must not be null.");
		}
		if (m_layerData.containsKey(networkType)) {
			throw new IllegalArgumentException(
					"A layer data factory of network type '" + networkType + "' is already registered.");
		}
		m_layerData.put(networkType, layerData);
	}
	// :registration
}
