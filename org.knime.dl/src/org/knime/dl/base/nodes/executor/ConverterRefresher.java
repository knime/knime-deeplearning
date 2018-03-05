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
package org.knime.dl.base.nodes.executor;

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
import org.knime.dl.core.data.DLWritableBuffer;
import org.knime.dl.core.data.convert.DLCollectionDataValueToTensorConverterFactory;
import org.knime.dl.core.data.convert.DLDataValueToTensorConverterFactory;
import org.knime.dl.core.data.convert.DLDataValueToTensorConverterRegistry;

/**
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
class ConverterRefresher {
	
	private Set<DLDataValueToTensorConverterFactory<?, ?>> m_builtInElement = new HashSet<>(1);
	private Set<DLDataValueToTensorConverterFactory<?, ?>> m_builtInCollection = new HashSet<>(1);
	private Set<DLDataValueToTensorConverterFactory<?, ?>> m_extensionElement = new HashSet<>(1);
	private Set<DLDataValueToTensorConverterFactory<?, ?>> m_extensionCollection = new HashSet<>(1); 
	private HashSet<DataType> m_inputTypes = new HashSet<>();
	private final DataTableSpec m_lastTableSpec;
	private final Class<? extends DLWritableBuffer> m_bufferType;
	private final DLDataValueToTensorConverterRegistry m_converterRegistry = DLDataValueToTensorConverterRegistry.getInstance();
	private final Comparator<DLDataValueToTensorConverterFactory<?, ?>> m_comparator;
	private String[] m_names;
	private String[] m_ids;

	public ConverterRefresher(DataTableSpec lastTableSpec, Class<? extends DLWritableBuffer> bufferType, Comparator<DLDataValueToTensorConverterFactory<?, ?>> comparator) {
		m_lastTableSpec = lastTableSpec;
		m_bufferType = bufferType;
		m_comparator = comparator;
		initialize();
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
		List<DLDataValueToTensorConverterFactory<?, ?>> sortedConverters = sortConverters();
		extractNamesAndIds(sortedConverters);
	}
	
	private void extractNamesAndIds(List<DLDataValueToTensorConverterFactory<?, ?>> sortedConverters) {
		m_names = new String[sortedConverters.size()];
		m_ids = new String[sortedConverters.size()];
		for (int i = 0; i < sortedConverters.size(); i++) {
			DLDataValueToTensorConverterFactory<?, ?> converter = sortedConverters.get(i);
			m_names[i] = "From " + converter.getName();
			m_ids[i] = converter.getIdentifier();
		}
	}
	
	
	private List<DLDataValueToTensorConverterFactory<?, ?>> sortConverters() {
		return Stream.concat(
				Stream.concat(m_builtInElement.stream().sorted(m_comparator), m_builtInCollection.stream().sorted(m_comparator)),
				Stream.concat(m_extensionElement.stream().sorted(m_comparator), m_extensionCollection.stream().sorted(m_comparator)))
				.collect(Collectors.toList());
	}
	
	private void getAvailableConverters() {
		for (DataColumnSpec colSpec : m_lastTableSpec) {
			handleColSpec(colSpec);
		}
	}
	
	
	private void handleColSpec(DataColumnSpec colSpec) {
		if (checkIfKnownAndAdd(colSpec.getType())) {
			List<DLDataValueToTensorConverterFactory<? extends DataValue, ?>> converters = m_converterRegistry.getConverterFactories(
					colSpec.getType(), m_bufferType);
			if (!converters.isEmpty()) {
				for (DLDataValueToTensorConverterFactory<?, ?> converter : converters) {
					handleConverter(converter);
				}
			}
		}
	}
	
	private void handleConverter(DLDataValueToTensorConverterFactory<?, ?> converter) {
		if (isBuiltInConverter(converter)) {
			handleBuiltInConverter(converter);
		} else {
			handleExtensionConverter(converter);
		}
	}
	
	private boolean isBuiltInConverter(DLDataValueToTensorConverterFactory<?, ?> converter) {
		return converter.getClass().getCanonicalName().contains("org.knime.dl.core.data.convert");
	}
	
	private void handleBuiltInConverter(DLDataValueToTensorConverterFactory<?, ?> converter) {
		if (converter instanceof DLCollectionDataValueToTensorConverterFactory) {
			m_builtInCollection.add(converter);
		} else {
			m_builtInElement.add(converter);
		}
	}
	
	private void handleExtensionConverter(DLDataValueToTensorConverterFactory<?, ?> converter) {
		if (converter instanceof DLCollectionDataValueToTensorConverterFactory) {
			m_extensionCollection.add(converter);
		} else {
			m_extensionElement.add(converter);
		}
	}
	
	private boolean checkIfKnownAndAdd(DataType type) {
		return m_inputTypes.add(type);
	}
}
