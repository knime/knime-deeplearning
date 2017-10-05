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
 */
package org.knime.dl.core;

import java.io.IOException;

/**
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
public interface DLExternalNetworkLoader<N extends DLExternalNetwork<?, R>, H, R, C> extends DLNetworkLoader<R> {

	/**
	 * Checks if the given context is valid.
	 *
	 * @param context the context
	 * @throws DLInvalidContextException if the context validation failed.
	 *             {@link DLInvalidContextException#getMessage()} contains the detailed test report that is suitable to
	 *             be displayed to the user.
	 */
	void validateContext(C context) throws DLInvalidContextException;

	/**
	 * Loads a network from a source into a context.
	 *
	 * @param source the source
	 * @param context the context
	 * @return the network handle
	 * @throws DLInvalidSourceException if the source is unavailable or invalid
	 * @throws DLInvalidContextException if the context is invalid
	 * @throws IOException if failed to load the network
	 */
	H load(R source, C context) throws DLInvalidSourceException, DLInvalidContextException, IOException;

	/**
	 * Fetches the network representation of a handle from a context.
	 *
	 * @param handle the handle
	 * @param source the source
	 * @param context the context
	 * @return the network
	 * @throws IllegalArgumentException if the handle is invalid
	 * @throws DLInvalidSourceException if the source is unavailable or invalid
	 * @throws DLInvalidContextException if the context is invalid
	 * @throws IOException if failed to fetch the network
	 */
	N fetch(H handle, R source, C context)
			throws IllegalArgumentException, DLInvalidSourceException, DLInvalidContextException, IOException;

	/**
	 * Saves a network from a context to a destination.
	 *
	 * @param handle the handle
	 * @param destination the destination
	 * @param context the context
	 * @throws IllegalArgumentException if the handle is invalid
	 * @throws DLInvalidDestinationException if the destination is invalid
	 * @throws DLInvalidContextException if the context is invalid
	 * @throws IOException if failed to save the network
	 */
	void save(H handle, R destination, C context)
			throws IllegalArgumentException, DLInvalidDestinationException, DLInvalidContextException, IOException;
}
