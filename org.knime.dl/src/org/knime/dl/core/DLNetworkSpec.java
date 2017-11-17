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
 *   Apr 17, 2017 (dietzc): created
 */
package org.knime.dl.core;

import java.io.Serializable;
import java.util.Optional;

import org.knime.dl.core.training.DLTrainingConfig;

/**
 * The spec of a {@link DLNetwork}.
 * <P>
 * Implementations of this interface must ensure that all of their contents are {@link Serializable serializable}.
 * <P>
 * Implementations of this interface must override {@link #equals(Object)} and {@link #hashCode()} in a value-based way.
 * <P>
 * Deep learning spec objects are intended to be used throughout the application and must not reference heavy data
 * objects or external resources. Spec objects are stateless.
 *
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 */
public interface DLNetworkSpec extends Serializable {

	// TODO: these could be collections if this would be preferred sometime. However, keep in mind that certain back
	// ends (e.g. Keras when training multi-output networks) require ordered collections of specs and should narrow the
	// return type.

	DLTensorSpec[] getInputSpecs();

	DLTensorSpec[] getHiddenOutputSpecs();

	DLTensorSpec[] getOutputSpecs();

	/**
	 * Returns the {@link DLTrainingConfig training configuration} of the network that is described by this spec if it
	 * was configured.
	 *
	 * @return the {@link DLTrainingConfig training configuration} of the network that is described by this spec if it
	 *         was configured
	 */
	default Optional<? extends DLTrainingConfig> getTrainingConfig() {
		return Optional.empty();
	}

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
