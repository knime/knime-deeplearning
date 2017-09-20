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

import java.io.Serializable;

/**
 * Implementations of this interface have to override {@link #hashCode()} and {@link #equals(Object)} in a value-based
 * way. Expected behavior is to essentially just delegate to the respective methods of the object returned by
 * {@link #getIdentifier()}, see {@link DLAbstractNetworkType}.
 * <P>
 * Network, network spec and network type must be part of the same bundle (or rather: loaded by the same class loader)
 * for serialization reasons. The respective serializers must be part of that bundle as well.
 *
 * @param <N> the {@link DLNetwork network} that is associated with this network type
 * @param <S> the {@link DLNetworkSpec network spec} that is associated with this network type
 * @param <R> the source type of the network that is associated with this network type
 *
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
public interface DLNetworkType<N extends DLNetwork<S, R>, S extends DLNetworkSpec<R>, R> extends Serializable {

	String getIdentifier();

	String getName();

	/**
	 * Note: serializer and network type must be part of the same bundle (or rather: loaded by the same class loader).
	 *
	 * @return the network serializer
	 */
	DLNetworkSerializer<N, S> getNetworkSerializer();

	/**
	 * Note: serializer and network type must be part of the same bundle (or rather: loaded by the same class loader).
	 *
	 * @return the network spec serializer
	 */
	DLNetworkSpecSerializer<S> getNetworkSpecSerializer();

	/**
	 * Returns the {@link DLNetworkLoader loader} for this network type.
	 * <P>
	 * Implementing classes and extending interfaces should narrow the return type of this method.
	 *
	 * @return the loader for this network type
	 */
	DLNetworkLoader<R> getLoader();

	/**
	 * Creates a {@link DLNetwork network} from a source and a spec.
	 *
	 * @param spec the spec
	 * @param source the source
	 * @return the network
	 * @throws DLInvalidSourceException if the source is unavailable or invalid
	 * @throws IllegalArgumentException if the spec is invalid
	 */
	N wrap(S spec, R source) throws DLInvalidSourceException, IllegalArgumentException;

	/**
	 * Value-based.
	 * <P>
	 * Inherited documentation: {@inheritDoc}
	 */
	@Override
	int hashCode();

	/**
	 * Value-based.
	 * <P>
	 * Inherited documentation: {@inheritDoc}
	 */
	@Override
	boolean equals(Object obj);
}
