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
 *   May 23, 2017 (marcel): created
 */
package org.knime.dl.python.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.knime.dl.python.core.data.DLPythonTypeMap;

import com.google.common.primitives.UnsignedBytes;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public final class DLPythonNumPyTypeMap implements DLPythonTypeMap {

	public static final DLPythonNumPyTypeMap INSTANCE = new DLPythonNumPyTypeMap();

	private final HashMap<String, List<Class<?>>> m_byExternalType;

	private final HashMap<Class<?>, List<String>> m_byInternalType;

	/**
	 * Creates a new instance of this type map.
	 */
	private DLPythonNumPyTypeMap() {
		m_byExternalType = new HashMap<>();
		m_byInternalType = new HashMap<>();
		// associate NumPy types with Java types
		registerMapping("bool", boolean.class);
		registerMapping("int8", byte.class);
		registerMapping("uint8", UnsignedBytes.class);
		registerMapping("int16", short.class);
		registerMapping("int32", int.class);
		registerMapping("int64", long.class);
		registerMapping("float16", float.class);
		registerMapping("float32", float.class);
		registerMapping("float64", double.class);
		registerMapping("str", String.class);
	}

	@Override
	public void registerMapping(final String externalType, final Class<?> internalType)
			throws IllegalArgumentException {
		if (externalType == null || externalType.isEmpty()) {
			throw new IllegalArgumentException("External type representation must neither be null nor empty.");
		}
		if (internalType == null) {
			throw new IllegalArgumentException("Internal type  must not be null.");
		}

		List<Class<?>> mappedInternals = m_byExternalType.get(externalType);
		if (mappedInternals == null) {
			mappedInternals = new ArrayList<>();
			m_byExternalType.put(externalType, mappedInternals);
		}
		// list traversals should be fine, we won't do this very often and lists won't be large
		if (!mappedInternals.contains(internalType)) {
			mappedInternals.add(internalType);
		}
		List<String> mappedExternals = m_byInternalType.get(internalType);
		if (mappedExternals == null) {
			mappedExternals = new ArrayList<>();
			m_byInternalType.put(internalType, mappedExternals);
		}
		if (!mappedExternals.contains(externalType)) {
			mappedExternals.add(externalType);
		}
	}

	@Override
	public Class<?> getPreferredInternalType(final String externalType) throws IllegalArgumentException {
		// TODO: do we need some sort of matching to prioritize types?
		return getInternalTypes(externalType).get(0);
	}

	@Override
	public List<Class<?>> getInternalTypes(final String externalType) throws IllegalArgumentException {
		final List<Class<?>> mappedInternals = m_byExternalType.get(externalType);
		if (mappedInternals == null) {
			throw new IllegalArgumentException("There is no matching internal type for'" + externalType + "'.");
		}
		return mappedInternals;
	}

	@Override
	public String getPreferredExternalType(final Class<?> internalType) throws IllegalArgumentException {
		// TODO: do we need some sort of matching to prioritize types?
		return getExternalTypes(internalType).get(0);
	}

	@Override
	public List<String> getExternalTypes(final Class<?> internalType) throws IllegalArgumentException {
		final List<String> mappedExternals = m_byInternalType.get(internalType);
		if (mappedExternals == null) {
			throw new IllegalArgumentException(
					"There is no matching external type for '" + internalType.getName() + "'.");
		}
		return mappedExternals;
	}
}
