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
 *   May 29, 2017 (marcel): created
 */
package org.knime.dl.core.data.convert.input;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.node.NodeLogger;
import org.knime.dl.core.DLLayerData;
import org.knime.dl.core.data.writables.DLWritableBuffer;

/**
 * Registry for deep learning input converter factories that allow conversion of {@link DataValue data
 * values} into {@link DLLayerData layer data} types.
 *
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
public final class DLDataValueToLayerDataConverterRegistry {

    private static final String CONVERTER_EXT_POINT_ID = "org.knime.dl.DLDataValueToLayerDataConverterFactory";

    private static final String CONVERTER_EXT_POINT_ATTR_FACTORY_CLASS = "DLDataValueToLayerDataConverterFactory";

    private static final NodeLogger LOGGER = NodeLogger.getLogger(DLDataValueToLayerDataConverterRegistry.class);

    private static DLDataValueToLayerDataConverterRegistry instance;

    public static DLDataValueToLayerDataConverterRegistry getInstance() {
        if (instance == null) {
            synchronized (DLDataValueToLayerDataConverterRegistry.class) {
                if (instance == null) {
                    instance = new DLDataValueToLayerDataConverterRegistry();
                }
            }
        }
        return instance;
    }

    private final HashMap<String, DLDataValueToLayerDataConverterFactory<?, ?>> m_convById = new HashMap<>();

    private final HashMap<ConversionKey, Set<DLDataValueToLayerDataConverterFactory<?, ?>>> m_convBySourceDest =
        new HashMap<>();

    private final HashMap<Class<?>, Set<DLDataValueToLayerDataConverterFactory<?, ?>>> m_convBySource = new HashMap<>();

    private final HashMap<Class<?>, Set<DLDataValueToLayerDataConverterFactory<?, ?>>> m_convByDest = new HashMap<>();

    /**
     * Creates a new registry instance.
     */
    private DLDataValueToLayerDataConverterRegistry() {
        // register converters
        registerExtensionPoint(CONVERTER_EXT_POINT_ID, CONVERTER_EXT_POINT_ATTR_FACTORY_CLASS,
            this::registerConverterInternal);
    }

    @SuppressWarnings("unchecked")
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
                    registerMethod.accept((T)elem.createExecutableExtension(attrFactoryClass));
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
     * Returns all deep learning {@link DLDataValueToLayerDataConverterFactory converter factories} that create converters
     * which convert a specific source type.
     *
     * @param sourceType the source type
     * @return all deep learning converter factories that allow conversion of the source type
     */
    public final Collection<DLDataValueToLayerDataConverterFactory<? extends DataValue, ?>>
        getConverterFactories(final DataType sourceType, final Class<? extends DLWritableBuffer> bufferType) {
        final LinkedHashSet<DLDataValueToLayerDataConverterFactory<?, ?>> convs = new LinkedHashSet<>();
        for (final DLDataValueToLayerDataConverterFactory<?, ?> canidate : m_convById.values()) {
            if (canidate.getBufferType().isAssignableFrom(bufferType)
                && sourceType.isCompatible(canidate.getSourceType())) {
                convs.add(canidate);
            }
        }
        return convs;
    }

    /**
     * Returns the preferred deep learning {@link DLDataValueToLayerDataConverterFactory converter factory} that creates
     * converters which convert a specific source type into a specific destination type.
     *
     * @param sourceType the source type
     * @param destType the destination type
     * @return the preferred deep learning converter factory that allows conversion of the source type into the
     *         destination type
     */
    public final Optional<DLDataValueToLayerDataConverterFactory<? extends DataValue, ?>>
        getPreferredConverterFactory(final DataType sourceType, final Class<? extends DLWritableBuffer> destSpec) {
        return getConverterFactories(sourceType, destSpec).stream().findFirst();
    }

    public Optional<DLDataValueToLayerDataConverterFactory<?, ?>> getConverterFactory(final String identifier) {
        return Optional.ofNullable(m_convById.get(identifier));
    }

    public Collection<DLDataValueToLayerDataConverterFactory<?, ?>> getConverterFactoryByName(final String name) {
        return m_convById.values().stream().filter(c -> c.getName().equals(name)).collect(Collectors.toList());
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
    public final void registerConverter(final DLDataValueToLayerDataConverterFactory<?, ?> converter)
        throws IllegalArgumentException {
        registerConverterInternal(converter);
    }

    private synchronized void registerConverterInternal(final DLDataValueToLayerDataConverterFactory<?, ?> converter) {
        final String id = converter.getIdentifier();
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("The converter factory's id must be neither null nor empty.");
        }
        if (m_convById.containsKey(id)) {
            throw new IllegalArgumentException("A converter factory with id '" + id + "' is already registered.");
        }
        m_convById.put(id, converter);

        final ConversionKey key = new ConversionKey(converter);
        Set<DLDataValueToLayerDataConverterFactory<?, ?>> bySourceDest = m_convBySourceDest.get(key);
        if (bySourceDest == null) {
            bySourceDest = new LinkedHashSet<>();
            m_convBySourceDest.put(key, bySourceDest);
        }
        bySourceDest.add(converter);

        final Class<?> destType = converter.getBufferType();
        Set<DLDataValueToLayerDataConverterFactory<?, ?>> bySource = m_convBySource.get(destType);
        if (bySource == null) {
            bySource = new LinkedHashSet<>();
            m_convByDest.put(destType, bySource);
        }
        bySource.add(converter);

        final Class<?> sourceType = converter.getSourceType();
        Set<DLDataValueToLayerDataConverterFactory<?, ?>> byDest = m_convByDest.get(sourceType);
        if (byDest == null) {
            byDest = new LinkedHashSet<>();
            m_convBySource.put(sourceType, byDest);
        }
        byDest.add(converter);

    }

    /**
     * @see org.knime.core.data.convert.ConversionKey
     */
    static class ConversionKey {

        private final int m_hashCode;

        private final Class<?> m_sourceType;

        private final Object m_destType;

        private ConversionKey(final DLDataValueToLayerDataConverterFactory<?, ?> factory) {
            this(factory.getBufferType(), factory.getSourceType());
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
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final ConversionKey other = (ConversionKey)obj;
            return Objects.equals(this.m_destType, other.m_destType)
                && Objects.equals(this.m_sourceType, other.m_sourceType);
        }
    }
}
