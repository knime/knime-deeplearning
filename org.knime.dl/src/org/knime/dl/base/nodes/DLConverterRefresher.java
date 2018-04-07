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
package org.knime.dl.base.nodes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.DataValue.UtilityFactory;
import org.knime.core.data.ExtensibleUtilityFactory;
import org.knime.core.node.InvalidSettingsException;
import org.knime.dl.core.DLException;
import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.core.data.DLWritableBuffer;
import org.knime.dl.core.data.convert.DLCollectionDataValueToTensorConverterFactory;
import org.knime.dl.core.data.convert.DLDataValueToTensorConverterFactory;
import org.knime.dl.core.data.convert.DLDataValueToTensorConverterRegistry;
import org.knime.dl.util.DLUtils;

/**
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public final class DLConverterRefresher {

	private final Set<DLDataValueToTensorConverterFactory<?, ?>> m_builtInElement = new HashSet<>(1);
	private final Set<DLDataValueToTensorConverterFactory<?, ?>> m_builtInCollection = new HashSet<>(1);
	private final Set<DLDataValueToTensorConverterFactory<?, ?>> m_extensionElement = new HashSet<>(1);
	private final Set<DLDataValueToTensorConverterFactory<?, ?>> m_extensionCollection = new HashSet<>(1);
	private final HashSet<DataType> m_inputTypes = new HashSet<>();
	private final DataTableSpec m_tableSpec;
	private final Class<? extends DLWritableBuffer> m_bufferType;
	private final DLDataValueToTensorConverterRegistry m_converterRegistry = DLDataValueToTensorConverterRegistry
			.getInstance();
	private final Comparator<DLDataValueToTensorConverterFactory<?, ?>> m_comparator;
	private List<DLDataValueToTensorConverterFactory<?, ?>> m_converters;
	private String[] m_names;
	private String[] m_ids;

	/**
	 * @param tableSpec must contain at least one column
	 * @param isTrainingTargetSpec true if <code>tensorSpec</code> is a training target. Only used to properly phrase
	 *            error messages if no converters are available ("network input" vs. "network target").
	 * @throws DLNoConverterAvailableException if no converter is available. The exception offers both a short and a
	 *             long error description message that are both suitable to be shown to the user.
	 */
	public DLConverterRefresher(final DataTableSpec tableSpec, final Class<? extends DLWritableBuffer> bufferType,
			final DLTensorSpec tensorSpec, final boolean isTrainingTargetSpec,
			final Comparator<DLDataValueToTensorConverterFactory<?, ?>> comparator)
			throws DLNoConverterAvailableException {
		m_tableSpec = tableSpec;
		m_bufferType = bufferType;
		m_comparator = comparator;
		initialize();
		if (m_converters.isEmpty()) {
			final String inputOrTargetStr = isTrainingTargetSpec ? "target" : "input";
			final List<DLDataValueToTensorConverterFactory<? extends DataValue, ?>> convertersForBuffer = m_converterRegistry
					.getConverterFactoriesForBufferType(bufferType);
			if (convertersForBuffer.isEmpty()) {
				// no converters available at all, user can't do much
				final String message = "No converter available for the expected " + inputOrTargetStr + " data type ("
						+ tensorSpec.getElementType().getTypeName() + ") of network " + inputOrTargetStr + " '"
						+ tensorSpec.getName() + "'.";
				final String longMessage = message
						+ " Please make sure you are not missing a KNIME Deep Learning extension "
						+ "and/or try to use a network that expects different " + inputOrTargetStr + " data types.";
				throw new DLNoConverterAvailableException(message, longMessage);
			} else {
				// converters available but user did not supply compatible data
				final String suppliedTypesStr = DLUtils.Strings.joinAbbreviated(m_inputTypes, ", ", 2);
				final List<String> supportedTypes = convertersForBuffer.stream()
						.map(DLDataValueToTensorConverterFactory::getSourceType) //
						.map(type -> {
							final UtilityFactory utility = DataType.getUtilityFor(type);
							if (utility instanceof ExtensibleUtilityFactory) {
								return ((ExtensibleUtilityFactory) utility).getName();
							} else {
								return type.getTypeName();
							}
						}) //
						.collect(Collectors.toSet()).stream().sorted().collect(Collectors.toList());
				final String supportedTypesStr = DLUtils.Strings.joinAbbreviated(supportedTypes, ", ", 10);
				final String message = "None of the data types present in the input table can be converted into the data type ("
						+ tensorSpec.getElementType().getTypeName() + ") accepted by network " + inputOrTargetStr + " '"
						+ tensorSpec.getName() + "'.";
				final String longMessage = "None of the data types present in the input table (" + suppliedTypesStr
						+ ") can be converted into the data type (" + tensorSpec.getElementType().getTypeName()
						+ ") accepted by network " + inputOrTargetStr + " '" + tensorSpec.getName()
						+ "'. Please include columns of compatible types (e.g. " + supportedTypesStr
						+ ") or collections of those types in the input table. "
						+ "Also, please make sure you are not missing a KNIME Deep Learning extension, "
						+ "especially if you are working with data types from other extensions (e.g. images).";
				throw new DLNoConverterAvailableException(message, longMessage);
			}
		}
	}

	public List<DLDataValueToTensorConverterFactory<?, ?>> getConverters() {
		return new ArrayList<>(m_converters);
	}

	public String[] getConverterNames() {
		if (m_names == null) {
			throw new IllegalStateException("The ConverterRefresher has not been initialized properly.");
		}
		return m_names;
	}

	public String[] getConverterIdentifiers() {
		if (m_ids == null) {
			throw new IllegalStateException("The ConverterRefresher has not been initialized properly.");
		}
		return m_ids;
	}

	private void initialize() {
		getAvailableConverters();
		m_converters = sortConverters();
		extractNamesAndIds();
	}

	private void extractNamesAndIds() {
		m_names = new String[m_converters.size()];
		m_ids = new String[m_converters.size()];
		for (int i = 0; i < m_converters.size(); i++) {
			final DLDataValueToTensorConverterFactory<?, ?> converter = m_converters.get(i);
			m_names[i] = "From " + converter.getName();
			m_ids[i] = converter.getIdentifier();
		}
	}

	private List<DLDataValueToTensorConverterFactory<?, ?>> sortConverters() {
		return Stream.concat(
				Stream.concat(m_builtInElement.stream().sorted(m_comparator),
						m_builtInCollection.stream().sorted(m_comparator)),
				Stream.concat(m_extensionElement.stream().sorted(m_comparator),
						m_extensionCollection.stream().sorted(m_comparator)))
				.collect(Collectors.toList());
	}

	private void getAvailableConverters() {
		for (final DataColumnSpec colSpec : m_tableSpec) {
			handleColSpec(colSpec);
		}
	}

	private void handleColSpec(final DataColumnSpec colSpec) {
		if (checkIfKnownAndAdd(colSpec.getType())) {
			final List<DLDataValueToTensorConverterFactory<? extends DataValue, ?>> converters = m_converterRegistry
					.getConverterFactories(colSpec.getType(), m_bufferType);
			if (!converters.isEmpty()) {
				for (final DLDataValueToTensorConverterFactory<?, ?> converter : converters) {
					handleConverter(converter);
				}
			}
		}
	}

	private void handleConverter(final DLDataValueToTensorConverterFactory<?, ?> converter) {
		if (isBuiltInConverter(converter)) {
			handleBuiltInConverter(converter);
		} else {
			handleExtensionConverter(converter);
		}
	}

	private boolean isBuiltInConverter(final DLDataValueToTensorConverterFactory<?, ?> converter) {
		return converter.getClass().getCanonicalName().contains("org.knime.dl.core.data.convert");
	}

	private void handleBuiltInConverter(final DLDataValueToTensorConverterFactory<?, ?> converter) {
		if (converter instanceof DLCollectionDataValueToTensorConverterFactory) {
			m_builtInCollection.add(converter);
		} else {
			m_builtInElement.add(converter);
		}
	}

	private void handleExtensionConverter(final DLDataValueToTensorConverterFactory<?, ?> converter) {
		if (converter instanceof DLCollectionDataValueToTensorConverterFactory) {
			m_extensionCollection.add(converter);
		} else {
			m_extensionElement.add(converter);
		}
	}

	private boolean checkIfKnownAndAdd(final DataType type) {
		return m_inputTypes.add(type);
	}

	public static final class DLNoConverterAvailableException extends InvalidSettingsException implements DLException {

		private final String m_longMessage;

		public DLNoConverterAvailableException(final String message, final String longMessage) {
			super(message);
			m_longMessage = longMessage;
		}

		public String getLongMessage() {
			return m_longMessage;
		}
	}
}
