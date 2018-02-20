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
package org.knime.dl.keras.core.training;

import java.util.Objects;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.knime.dl.core.DLTensorId;
import org.knime.dl.keras.core.training.DLKerasCallback.DLKerasEarlyStopping;
import org.knime.dl.keras.core.training.DLKerasCallback.DLKerasReduceLROnPlateau;

/**
 * Abstract base class for quantities that are monitored during training, e.g. by {@link DLKerasEarlyStopping} and
 * {@link DLKerasReduceLROnPlateau} callbacks. <br>
 * This class contains all functionality that is needed to fully specify a monitored quantity. Derived classes are for
 * convenience only and expose more specific constructors.
 *
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public abstract class DLKerasAbstractMonitoredQuantity implements DLKerasMonitoredQuantity {

	private final DLKerasMetric m_quantity;

	private final DLTensorId m_output;

	private final boolean m_isValidation;

	/**
	 * @param quantity the monitored quantity. May be null, in which case the monitored quantity is interpreted as the
	 *            loss.
	 * @param output the output for which the quantity is monitored. May be null, in which case the quantity is
	 *            monitored for the entire network.
	 * @param isValidation true if the monitored quantity is a validation quantity (as opposed to a training quantity)
	 */
	protected DLKerasAbstractMonitoredQuantity(final DLKerasMetric quantity, final DLTensorId output,
			final boolean isValidation) {
		m_quantity = quantity;
		m_output = output;
		m_isValidation = isValidation;
	}

	@Override
	public final String getKerasIdentifier() {
		if (m_quantity == null && m_output == null) {
			return isValidationQuantity() ? "val_loss" : "loss";
		}
		// TODO: NYI - we need the layer name for this & have to investigate how Keras maps arbitrary metrics to an
		// identifier string
		throw new RuntimeException("not yet implemented");
	}

	@Override
	public final DLKerasMetric getQuantity() {
		return m_quantity;
	}

	@Override
	public final DLTensorId getOutput() {
		return m_output;
	}

	@Override
	public final boolean isValidationQuantity() {
		return m_isValidation;
	}

	@Override
	public final int hashCode() {
		final HashCodeBuilder b = new HashCodeBuilder(17, 37);
		b.append(m_quantity);
		b.append(m_output);
		b.append(m_isValidation);
		return b.toHashCode();
	}

	@Override
	public final boolean equals(final Object obj) {
		if (obj == this) {
			return true;
		}
		/* no strict type check, see documentation of DLKerasMonitoredQuantity#equals(Object) */
		if (obj == null || !(obj instanceof DLKerasMonitoredQuantity)) {
			return false;
		}
		final DLKerasMonitoredQuantity other = (DLKerasMonitoredQuantity) obj;
		return Objects.equals(other.getQuantity(), m_quantity) //
				&& Objects.equals(other.getOutput(), m_output) //
				&& other.isValidationQuantity() == m_isValidation //
				// Keras identifiers should be equal if other values are equal but let's stay defensive here.
				&& other.getKerasIdentifier() == getKerasIdentifier();
	}

	@Override
	public final String toString() {
		return (m_isValidation ? "Validation " : "Training ")
				+ (m_quantity != null ? m_quantity.getName().toLowerCase() : "loss") + " ("
				+ (m_output != null ? m_output.getIdentifierString() : "total") + ")";
	}
}
