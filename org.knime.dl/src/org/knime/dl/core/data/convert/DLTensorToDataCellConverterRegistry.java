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
 *   May 16, 2017 (marcel): created
 */
package org.knime.dl.core.data.convert;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

import org.eclipse.core.runtime.IConfigurationElement;
import org.knime.core.data.DataCell;
import org.knime.core.node.NodeLogger;
import org.knime.dl.core.DLAbstractExtensionPointRegistry;
import org.knime.dl.core.DLTensor;
import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.core.data.DLReadableBuffer;

/**
 * Registry for deep learning output converter factories that allow conversion of {@link DLTensor tensor} types into
 * {@link DataCell data cells}.
 *
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public final class DLTensorToDataCellConverterRegistry extends DLAbstractExtensionPointRegistry {
    
    private static final NodeLogger LOGGER = NodeLogger.getLogger(DLTensorToDataCellConverterRegistry.class);

	private static final String EXT_POINT_ID = "org.knime.dl.DLTensorToDataCellConverterFactory";

	private static final String EXT_POINT_ATTR_CLASS = "DLTensorToDataCellConverterFactory";

    private static final String EXT_POINT_ATTR_DEPRECATED = "deprecated";

	private static DLTensorToDataCellConverterRegistry instance;

	/**
	 * Returns the singleton instance.
	 *
	 * @return the singleton instance
	 */
	public static synchronized DLTensorToDataCellConverterRegistry getInstance() {
		if (instance == null) {
			instance = new DLTensorToDataCellConverterRegistry();
		}
		return instance;
	}

    /** Map of the non deprecated converters */
	private final HashMap<String, DLTensorToDataCellConverterFactory<?, ?>> m_converters = new HashMap<>();

    /** Map of all converters (also deprecated converters */
    private final HashMap<String, DLTensorToDataCellConverterFactory<?, ?>> m_allConverters = new HashMap<>();

	/**
	 * Creates a new registry instance.
	 */
	private DLTensorToDataCellConverterRegistry() {
		super(EXT_POINT_ID, EXT_POINT_ATTR_CLASS);
		register();
	}

	// access methods:

	/**
     * Returns all deep learning {@link DLTensorToDataCellConverterFactory converter factories} that create converters
     * which convert a specific source type considering a source spec. Doesn't return deprecated converters.
     *
     * @param sourceType the source type
     * @param sourceSpec the source spec
     * @return all deep learning converter factories that allow conversion of the source type
     */
	public final List<DLTensorToDataCellConverterFactory<?, ? extends DataCell>> getFactoriesForSourceType(
			final Class<? extends DLReadableBuffer> sourceType, final DLTensorSpec sourceSpec) {
		final ArrayList<DLTensorToDataCellConverterFactory<?, ? extends DataCell>> convs = new ArrayList<>();
		for (final DLTensorToDataCellConverterFactory<?, ?> candidate : m_converters.values()) {
		    try {
		        if (candidate.getBufferType().isAssignableFrom(sourceType)) {
		            final OptionalLong destCount = candidate.getDestCount(sourceSpec);
		            convs.add(candidate);
		            // TODO: Figure out whether this is the best we can do
		            // Currently a missing destCount is a direct indicator that the converter
		            // can have multiple outputs
		            if (!destCount.isPresent() || destCount.getAsLong() > 1) {
		                // if we have multiple outputs, we can also output a list
		                convs.add(new DLTensorToListCellConverterFactory<>(candidate));
		            }
		        }
		    } catch (Throwable t) {
		        LOGGER.warn("An unexpected error occurred in DLTensorToDataCellConverter '" 
		                + candidate.getIdentifier() + "'.", t);
		    }
		}
		convs.sort(Comparator.comparing(DLTensorToDataCellConverterFactory::getIdentifier));
		return convs;
	}

	/**
     * Returns the preferred deep learning {@link DLTensorToDataCellConverterFactory converter factories} that create
     * converters which convert a specific source type considering a source spec. Doesn't return deprecated converters.
     *
     * @param sourceType the source type
     * @param sourceSpec the source spec
     * @return all deep learning converter factories that allow conversion of the source type
     */
	public List<DLTensorToDataCellConverterFactory<?, ? extends DataCell>> getPreferredFactoriesForSourceType(
			final Class<? extends DLReadableBuffer> sourceType, final DLTensorSpec sourceSpec) {
		final List<DLTensorToDataCellConverterFactory<?, ? extends DataCell>> convs = getFactoriesForSourceType(
				sourceType, sourceSpec);
		// remove redundant converters
		for (int i = convs.size() - 1; i >= 0; i--) {
			final DLTensorToDataCellConverterFactory<?, ? extends DataCell> conv = convs.get(i);
			if (conv.getBufferType() != sourceType) {
				for (int j = 0; j < convs.size(); j++) {
					if (i != j && convs.get(j).getDestType().equals(conv.getDestType())) {
						convs.remove(i);
						break;
					}
				}
			}
		}
		return convs;
	}

	/**
     * Returns the deep learning {@link DLTensorToDataCellConverterFactory converter factory} that matches the given
     * identifier if present. Also returns deprecated converters.
     *
     * @param identifier the unique identifier
     * @return the converter factory that matches the identifier
     */
	public final Optional<DLTensorToDataCellConverterFactory<?, ? extends DataCell>> getConverterFactory(
			final String identifier) {
		if (identifier == null || identifier.isEmpty()) {
			return Optional.empty();
		}
        if (isCollectionConverter(identifier)) {
            final Optional<DLTensorToDataCellConverterFactory<?, ?>> conv =
                getConverterFactory(extractElementConverter(identifier));
			if (conv.isPresent()) {
				return Optional.of(new DLTensorToListCellConverterFactory<>(conv.get()));
			} else {
				return Optional.empty();
			}
		}
        return Optional.ofNullable(m_allConverters.get(identifier));
	}

    /**
     * Checks if the converter with the given identifier is deprecated.
     *
     * @param identifier the identifier of the converter factory
     * @return true if the converter is deprecated
     * @throws IllegalArgumentException if the identifier doesn't correspond to a registered converter
     */
    public final boolean isDeprecated(final String identifier) {
        if (identifier == null || identifier.isEmpty()) {
            throw new IllegalArgumentException("The converter identifier can't be empty.");
        }
        // If the identifier points to a collection converter we need to handle it recursively
        if (isCollectionConverter(identifier)) {
            return isDeprecated(extractElementConverter(identifier));
        }
        // If the identifier is not known at all we throw an exception
        if (!m_allConverters.containsKey(identifier)) {
            throw new IllegalArgumentException("The converter with the identifier " + identifier
                + "is not registered. Are you missing a KNIME Extension?");
        }
        return !m_converters.containsKey(identifier);
    }

    // :access methods

    // static helpers:

    private static boolean isCollectionConverter(final String identifier) {
        return identifier.startsWith(DLTensorToListCellConverterFactory.class.getName());
    }

    private static String extractElementConverter(final String identifier) {
        return identifier.substring(DLTensorToListCellConverterFactory.class.getName().length() + 1,
            identifier.length() - 1);
    }

    // :static helpers

	// registration:

	/**
	 * Registers the given converter factory.
	 *
	 * @param converter the converter factory to register
	 * @throws IllegalArgumentException if a converter factory with the same identifier is already registered or if the
	 *             given converter factory's identifier or name is null or empty
	 */
	public final void registerConverter(final DLTensorToDataCellConverterFactory<?, ?> converter)
			throws IllegalArgumentException {
        registerConverterInternal(converter, false);
	}

	@Override
	protected void registerInternal(final IConfigurationElement elem, final Map<String, String> attrs)
			throws Throwable {
        final boolean deprecated = Boolean.parseBoolean(elem.getAttribute(EXT_POINT_ATTR_DEPRECATED));
		registerConverterInternal(
            (DLTensorToDataCellConverterFactory<?, ?>)elem.createExecutableExtension(EXT_POINT_ATTR_CLASS), deprecated);
	}

    private synchronized void registerConverterInternal(final DLTensorToDataCellConverterFactory<?, ?> converter,
        final boolean deprecated) {
		final String id = converter.getIdentifier();
		if (id == null || id.isEmpty()) {
			throw new IllegalArgumentException("The converter factory's id must be neither null nor empty.");
		}
        if (m_allConverters.containsKey(id)) {
			throw new IllegalArgumentException("A converter factory with id '" + id + "' is already registered.");
		}
        m_allConverters.put(id, converter);
        if (!deprecated) {
            m_converters.put(id, converter);
        }
	}
	// :registration
}
