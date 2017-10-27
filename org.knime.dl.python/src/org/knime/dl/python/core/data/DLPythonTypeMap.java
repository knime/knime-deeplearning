/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *   May 23, 2017 (marcel): created
 */
package org.knime.dl.python.core.data;

import java.util.List;

/**
 *
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
public interface DLPythonTypeMap {

	/**
	 * Registers a mutual mapping of an external and an internal type.
	 *
	 * @param pythonType the string representation of the external type
	 * @param internalType the internal type
	 * @throws IllegalArgumentException when any of the types is null or the external type representation is empty
	 */
	void registerMapping(final String pythonType, final Class<?> internalType) throws IllegalArgumentException;

	/**
	 * Returns the preferred internal type that matches the given external type.
	 *
	 * @param pythonType the external type
	 * @return the preferred internal type that matches the given external type
	 * @throws IllegalArgumentException when no matching internal type was found
	 */
	Class<?> getPreferredInternalType(String pythonType) throws IllegalArgumentException;

	/**
	 * Returns all internal types that match the given external type.
	 *
	 * @param pythonType the external type
	 * @return all internal types that match the given external type
	 * @throws IllegalArgumentException when no matching internal type was found
	 */
	List<Class<?>> getInternalTypes(String pythonType) throws IllegalArgumentException;

	/**
	 * Returns the preferred external type representation that matches the given internal type.
	 *
	 * @param internalType the internal type
	 * @return the preferred external type that matches the given internal type
	 * @throws IllegalArgumentException when no matching external type was found
	 */
	String getPreferredExternalType(Class<?> internalType) throws IllegalArgumentException;

	/**
	 * Returns all external type representations that match the given internal type.
	 *
	 * @param internalType the internal type
	 * @return all external types that match the given internal type
	 * @throws IllegalArgumentException when no matching external type was found
	 */
	List<String> getExternalTypes(Class<?> internalType) throws IllegalArgumentException;
}
