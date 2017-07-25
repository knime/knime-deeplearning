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
 *   May 16, 2017 (marcel): created
 */
package org.knime.dl.core.data.convert.output;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.knime.core.data.DataCell;
import org.knime.core.node.NodeLogger;
import org.knime.dl.core.DLLayerData;
import org.knime.dl.core.DLLayerDataSpec;
import org.knime.dl.core.data.DLReadableBuffer;

/**
 * Registry for deep learning output converter factories that allow conversion of {@link DLLayerData layer data} types
 * into {@link DataCell data cells}.
 *
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
public final class DLLayerDataToDataCellConverterRegistry {

	private static final String CONVERTER_EXT_POINT_ID = "org.knime.dl.DLLayerDataToDataCellConverterFactory";

	private static final String CONVERTER_EXT_POINT_ATTR_FACTORY_CLASS = "DLLayerDataToDataCellConverterFactory";

	private static final NodeLogger LOGGER = NodeLogger.getLogger(DLLayerDataToDataCellConverterRegistry.class);

	private static DLLayerDataToDataCellConverterRegistry instance;

	public static DLLayerDataToDataCellConverterRegistry getInstance() {
		if (instance == null) {
			synchronized (DLLayerDataToDataCellConverterRegistry.class) {
				if (instance == null) {
					instance = new DLLayerDataToDataCellConverterRegistry();
				}
			}
		}
		return instance;
	}

	private final HashMap<String, DLLayerDataToDataCellConverterFactory<?, ?>> m_convById = new HashMap<>();

	/**
	 * Creates a new registry instance.
	 */
	private DLLayerDataToDataCellConverterRegistry() {
		// register converters
		registerExtensionPoint(CONVERTER_EXT_POINT_ID, CONVERTER_EXT_POINT_ATTR_FACTORY_CLASS,
				this::registerConverterInternal);
	}

	private <T> void registerExtensionPoint(final String extPointId, final String attrFactoryClass,
			final Consumer<T> registerMethod) {
		try {
			final IExtensionRegistry registry = Platform.getExtensionRegistry();
			final IExtensionPoint point = registry.getExtensionPoint(extPointId);
			if (point == null) {
				final String msg = "Invalid extension point: '" + extPointId + "'.";
				LOGGER.error(msg);
				throw new IllegalStateException(msg);
			}
			for (final IConfigurationElement elem : point.getConfigurationElements()) {
				final String factory = elem.getAttribute(attrFactoryClass);
				final String extension = elem.getDeclaringExtension().getUniqueIdentifier();
				if (factory == null || factory.isEmpty()) {
					LOGGER.error("The extension '" + extension + "' doesn't provide the required attribute '"
							+ attrFactoryClass + "'.");
					LOGGER.error("Extension '" + extension + "' was ignored.");
					continue;
				}
				try {
					registerMethod.accept((T) elem.createExecutableExtension(attrFactoryClass));
				} catch (final Throwable t) {
					LOGGER.error("An error or exception occurred while initializing the deep learning converter "
							+ "or adapter factory '" + factory + "'.", t);
					LOGGER.error("Extension '" + extension + "' was ignored.", t);
					continue;
				}
			}
		} catch (final Exception e) {
			LOGGER.error("An exception occurred while registering deep learning converter and adapter factories.", e);
		}
	}

	// access methods:

	/**
	 * Returns all deep learning {@link DLLayerDataToDataCellConverterFactory converter factories} that create
	 * converters which convert a specific source type considering a source spec.
	 *
	 * @param sourceType the source type
	 * @param sourceSpec the source spec
	 * @return all deep learning converter factories that allow conversion of the source type
	 */
	public final List<DLLayerDataToDataCellConverterFactory<?, ? extends DataCell>> getFactoriesForSourceType(
			final Class<? extends DLReadableBuffer> sourceType, final DLLayerDataSpec sourceSpec) {
		final ArrayList<DLLayerDataToDataCellConverterFactory<?, ? extends DataCell>> convs = new ArrayList<>();
		for (final DLLayerDataToDataCellConverterFactory<?, ?> candidate : m_convById.values()) {
			if (candidate.getBufferType().isAssignableFrom(sourceType)) {
				convs.add(candidate);
				if (candidate.getDestCount(sourceSpec) > 1) {
					convs.add(new DLLayerDataToListCellConverterFactory<>(candidate));
					convs.add(new DLLayerDataToSetCellConverterFactory<>(candidate));
				}
			}
		}
		return convs;
	}

	/**
	 * Returns the preferred deep learning {@link DLLayerDataToDataCellConverterFactory converter factory} that creates
	 * converters which convert a specific source type considering a source spec.
	 *
	 * @param sourceType the source type
	 * @return the preferred deep learning converter factory that allows conversion of the source type
	 */
	public final Optional<DLLayerDataToDataCellConverterFactory<?, ? extends DataCell>> getPreferredFactoryForSourceType(
			final Class<? extends DLReadableBuffer> sourceType, final DLLayerDataSpec sourceSpec) {
		return getFactoriesForSourceType(sourceType, sourceSpec).stream().findFirst();
	}

	/**
	 * Returns the deep learning {@link DLLayerDataToDataCellConverterFactory converter factory} that matches the given
	 * identifier if present.
	 *
	 * @param identifier the unique identifier
	 * @return the converter factory that matches the identifier
	 */
	public final Optional<DLLayerDataToDataCellConverterFactory<?, ? extends DataCell>> getConverterFactory(
			final String identifier) {
		if (identifier == null || identifier.isEmpty()) {
			return Optional.empty();
		}
		if (identifier.startsWith(DLLayerDataToListCellConverterFactory.class.getName())) {
			final String elementConverterId = identifier.substring(
					DLLayerDataToListCellConverterFactory.class.getName().length() + 1, identifier.length() - 1);
			final Optional<DLLayerDataToDataCellConverterFactory<?, ?>> conv = getConverterFactory(elementConverterId);
			if (conv.isPresent()) {
				return Optional.of(new DLLayerDataToListCellConverterFactory<>(conv.get()));
			} else {
				return Optional.empty();
			}
		}
		if (identifier.startsWith(DLLayerDataToSetCellConverterFactory.class.getName())) {
			final String elementConverterId = identifier.substring(
					DLLayerDataToSetCellConverterFactory.class.getName().length() + 1, identifier.length() - 1);
			final Optional<DLLayerDataToDataCellConverterFactory<?, ?>> conv = getConverterFactory(elementConverterId);
			if (conv.isPresent()) {
				return Optional.of(new DLLayerDataToSetCellConverterFactory<>(conv.get()));
			} else {
				return Optional.empty();
			}
		}
		return Optional.ofNullable(m_convById.get(identifier));
	}
	// :access methods

	// registration:

	/**
	 * Registers the given converter factory.
	 *
	 * @param converter the converter factory to register
	 * @throws IllegalArgumentException if a converter factory with the same identifier is already registered or if the
	 *             given converter factory's identifier or name is null or empty
	 */
	public final void registerConverter(final DLLayerDataToDataCellConverterFactory<?, ?> converter)
			throws IllegalArgumentException {
		registerConverterInternal(converter);
	}

	private synchronized void registerConverterInternal(final DLLayerDataToDataCellConverterFactory<?, ?> converter) {
		final String id = converter.getIdentifier();
		if (id == null || id.isEmpty()) {
			throw new IllegalArgumentException("The converter factory's id must be neither null nor empty.");
		}
		if (m_convById.containsKey(id)) {
			throw new IllegalArgumentException("A converter factory with id '" + id + "' is already registered.");
		}
		m_convById.put(id, converter);
	}
	// :registration

	/**
	 * @see org.knime.core.data.convert.ConversionKey
	 */
	private static class ConversionKey {

		private final int m_hashCode;

		private final Class<?> m_sourceType;

		private final Object m_destType;

		private ConversionKey(final DLLayerDataToDataCellConverterFactory<?, ?> factory) {
			this(factory.getBufferType(), factory.getDestType());
		}

		private ConversionKey(final Class<?> sourceType, final Object destType) {
			m_sourceType = sourceType;
			m_destType = destType;
			final int prime = 31;
			m_hashCode = prime * (prime + sourceType.hashCode()) + destType.hashCode();
		}

		@Override
		public int hashCode() {
			return m_hashCode;
		}

		@Override
		public boolean equals(final Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null || getClass() != obj.getClass()) {
				return false;
			}
			final ConversionKey other = (ConversionKey) obj;
			return Objects.equals(m_destType, other.m_destType) && Objects.equals(m_sourceType, other.m_sourceType);
		}
	}
}
