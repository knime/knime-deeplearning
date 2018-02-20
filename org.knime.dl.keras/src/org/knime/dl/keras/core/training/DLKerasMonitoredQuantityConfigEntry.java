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

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.dl.base.settings.AbstractConfigEntry;
import org.knime.dl.core.DLDefaultTensorId;
import org.knime.dl.core.DLTensorId;
import org.knime.dl.keras.core.training.DLKerasMetrics.DLKerasAccuracy;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public class DLKerasMonitoredQuantityConfigEntry extends AbstractConfigEntry<DLKerasMonitoredQuantity> {

	private static final String CFG_KEY_QUANTITY = "quantity";

	private static final String CFG_KEY_OUTPUT = "output";

	private static final String CFG_KEY_IS_VALIDATION = "validation";

	/**
	 * See super class.
	 *
	 * @param entryKey
	 * @param entryType
	 * @param value
	 * @param enabled
	 */
	public DLKerasMonitoredQuantityConfigEntry(final String entryKey, final Class<DLKerasMonitoredQuantity> entryType,
			final DLKerasMonitoredQuantity value, final boolean enabled) {
		super(entryKey, entryType, value, enabled);
	}

	/**
	 * See super class.
	 *
	 * @param entryKey
	 * @param entryType
	 * @param value
	 */
	public DLKerasMonitoredQuantityConfigEntry(final String entryKey, final Class<DLKerasMonitoredQuantity> entryType,
			final DLKerasMonitoredQuantity value) {
		super(entryKey, entryType, value);
	}

	/**
	 * See super class.
	 *
	 * @param entryKey
	 * @param entryType
	 */
	public DLKerasMonitoredQuantityConfigEntry(final String entryKey, final Class<DLKerasMonitoredQuantity> entryType) {
		super(entryKey, entryType);
	}

	@Override
	protected void saveEntry(final NodeSettingsWO settings)
			throws InvalidSettingsException, UnsupportedOperationException {
		if (m_value != null) {
			settings.addString(CFG_KEY_QUANTITY,
					m_value.getQuantity() != null ? m_value.getQuantity().getIdentifier() : "loss");
			settings.addString(CFG_KEY_OUTPUT,
					m_value.getOutput() != null ? m_value.getOutput().getIdentifierString() : "network");
			settings.addBoolean(CFG_KEY_IS_VALIDATION, m_value.isValidationQuantity());
		} else {
			settings.addString(CFG_KEY_QUANTITY, "null");
			settings.addString(CFG_KEY_OUTPUT, "null");
			settings.addString(CFG_KEY_IS_VALIDATION, "null");
		}
	}

	@Override
	protected void loadEntry(final NodeSettingsRO settings)
			throws InvalidSettingsException, IllegalStateException, UnsupportedOperationException {
		final String quantityIdentifier = settings.getString(CFG_KEY_QUANTITY);
		if (!quantityIdentifier.equals("null")) {
			// quantity
			DLKerasMetrics quantity = null;
			if (!quantityIdentifier.equals("loss")) {

				// FIXME: we need the training context...
				// final DLKerasTrainingContext<DLKerasNetwork> dummy = null;
				// quantity = dummy.createMetrics().stream() //
				// .filter(m -> m.getIdentifier().equals(quantityIdentifier)) //
				// .findFirst() //
				// .orElseThrow(() -> new InvalidSettingsException("Monitored quantity '" + quantityIdentifier
				// + "' could not be found. Are you missing a KNIME Deep Learning extension?"));

				quantity = new DLKerasAccuracy();
			}
			// output
			final String outputIdentifier = settings.getString(CFG_KEY_OUTPUT);
			final DLTensorId output = !outputIdentifier.equals("network") ? new DLDefaultTensorId(outputIdentifier)
					: null;
			// validation
			final boolean isValidation = settings.getBoolean(CFG_KEY_IS_VALIDATION);
			// We do not need to create the specific implementation, see DLKerasMonitoredQuantity#equals(Object).
			m_value = new DLKerasAbstractMonitoredQuantity(quantity, output, isValidation) {
			};
		}
	}
}
