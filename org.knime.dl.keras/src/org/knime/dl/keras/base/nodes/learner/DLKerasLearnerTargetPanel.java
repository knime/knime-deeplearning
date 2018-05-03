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
 *   Jul 10, 2017 (marcel): created
 */
package org.knime.dl.keras.base.nodes.learner;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.dl.base.nodes.DLInputPanel;
import org.knime.dl.base.nodes.DialogComponentObjectSelection;
import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.core.training.DLLossFunction;
import org.knime.dl.keras.core.training.DLKerasLossFunction;
import org.knime.dl.keras.core.training.DLKerasTrainingContext;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class DLKerasLearnerTargetPanel extends DLInputPanel<DLKerasLearnerGeneralConfig, DLKerasLearnerTargetConfig> {

	private final DLTensorSpec m_targetTensorSpec;

	private final DialogComponentObjectSelection<DLKerasLossFunction> m_dcLossFunction;


	DLKerasLearnerTargetPanel(final DLKerasLearnerTargetConfig cfg, final DLTensorSpec outputDataSpec,
			final DataTableSpec tableSpec) {
	    super(cfg, outputDataSpec, tableSpec, DLKerasLearnerNodeModel.IN_DATA_PORT_IDX, "Target columns:", "target");
		m_targetTensorSpec = outputDataSpec;
		m_dcLossFunction = addObjectSelectionRow(cfg.getLossFunctionEntry(), DLLossFunction::getName, "Loss function", null);
	}

	void loadFromSettings(final NodeSettingsRO settings, final PortObjectSpec[] specs) throws NotConfigurableException {
		super.loadSettingsFrom(settings, specs);
		refreshAvailableLossFunctions();
	}
	
	private void refreshAvailableLossFunctions() throws NotConfigurableException {
		final DLKerasTrainingContext<?> trainingContext = m_cfg.getGeneralConfig().getContextEntry().getValue();
		final List<DLKerasLossFunction> availableLossFunctions = trainingContext.createLossFunctions() //
				.stream() //
				.sorted(Comparator.comparing(DLKerasLossFunction::getName)) //
				.collect(Collectors.toList());
		if (availableLossFunctions.isEmpty()) {
			throw new NotConfigurableException("No loss functions available for output '" + m_targetTensorSpec.getName()
					+ "' (with training context '" + trainingContext.getName() + "').");
		}
		final DLKerasLossFunction selectedLossFunction = m_cfg.getLossFunctionEntry().getValue() != null
				? m_cfg.getLossFunctionEntry().getValue()
				: availableLossFunctions.get(0);
		for (int i = availableLossFunctions.size() - 1; i >= 0; i--) {
			if (availableLossFunctions.get(i).getClass() == selectedLossFunction.getClass()) {
				availableLossFunctions.remove(i);
				availableLossFunctions.add(i, selectedLossFunction);
			}
		}
		m_dcLossFunction.replaceListItems(availableLossFunctions, selectedLossFunction);
	}
}
