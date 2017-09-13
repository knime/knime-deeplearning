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
package org.knime.dl.core.data.convert;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.core.runtime.IConfigurationElement;
import org.knime.core.data.DataCell;
import org.knime.dl.core.DLAbstractExtensionPointRegistry;
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
public final class DLLayerDataToDataCellConverterRegistry extends DLAbstractExtensionPointRegistry {

	private static final String EXT_POINT_ID = "org.knime.dl.DLLayerDataToDataCellConverterFactory";

	private static final String EXT_POINT_ATTR_CLASS = "DLLayerDataToDataCellConverterFactory";

	private static DLLayerDataToDataCellConverterRegistry instance;

	/**
	 * Returns the singleton instance.
	 *
	 * @return the singleton instance
	 */
	public static synchronized DLLayerDataToDataCellConverterRegistry getInstance() {
		if (instance == null) {
			instance = new DLLayerDataToDataCellConverterRegistry();
		}
		return instance;
	}

	private final HashMap<String, DLLayerDataToDataCellConverterFactory<?, ?>> m_converters = new HashMap<>();

	/**
	 * Creates a new registry instance.
	 */
	private DLLayerDataToDataCellConverterRegistry() {
		super(EXT_POINT_ID, EXT_POINT_ATTR_CLASS);
		register();
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
		for (final DLLayerDataToDataCellConverterFactory<?, ?> candidate : m_converters.values()) {
			if (candidate.getBufferType().isAssignableFrom(sourceType)) {
				convs.add(candidate);
				if (candidate.getDestCount(sourceSpec) > 1) {
					convs.add(new DLLayerDataToListCellConverterFactory<>(candidate));
					convs.add(new DLLayerDataToSetCellConverterFactory<>(candidate));
				}
			}
		}
		convs.sort(Comparator.comparing(DLLayerDataToDataCellConverterFactory::getIdentifier));
		return convs;
	}

	/**
	 * Returns the preferred deep learning {@link DLLayerDataToDataCellConverterFactory converter factories} that create
	 * converters which convert a specific source type considering a source spec.
	 *
	 * @param sourceType the source type
	 * @param sourceSpec the source spec
	 * @return all deep learning converter factories that allow conversion of the source type
	 */
	public List<DLLayerDataToDataCellConverterFactory<?, ? extends DataCell>> getPreferredFactoriesForSourceType(
			final Class<? extends DLReadableBuffer> sourceType, final DLLayerDataSpec sourceSpec) {
		final List<DLLayerDataToDataCellConverterFactory<?, ? extends DataCell>> convs =
				getFactoriesForSourceType(sourceType, sourceSpec);
		// remove redundant converters
		for (int i = convs.size() - 1; i >= 0; i--) {
			final DLLayerDataToDataCellConverterFactory<?, ? extends DataCell> conv = convs.get(i);
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
		return Optional.ofNullable(m_converters.get(identifier));
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

	@Override
	protected void registerInternal(final IConfigurationElement elem, final Map<String, String> attrs)
			throws Throwable {
		registerConverterInternal(
				(DLLayerDataToDataCellConverterFactory<?, ?>) elem.createExecutableExtension(EXT_POINT_ATTR_CLASS));
	}

	private synchronized void registerConverterInternal(final DLLayerDataToDataCellConverterFactory<?, ?> converter) {
		final String id = converter.getIdentifier();
		if (id == null || id.isEmpty()) {
			throw new IllegalArgumentException("The converter factory's id must be neither null nor empty.");
		}
		if (m_converters.containsKey(id)) {
			throw new IllegalArgumentException("A converter factory with id '" + id + "' is already registered.");
		}
		m_converters.put(id, converter);
	}
	// :registration
}
