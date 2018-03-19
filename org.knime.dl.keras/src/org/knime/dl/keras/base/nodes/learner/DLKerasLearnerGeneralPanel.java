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
package org.knime.dl.keras.base.nodes.learner;

import java.awt.Component;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.dl.base.nodes.AbstractGridBagDialogComponentGroup;
import org.knime.dl.base.nodes.DialogComponentObjectSelection;
import org.knime.dl.base.nodes.DialogComponentRandomSeed;
import org.knime.dl.base.portobjects.DLNetworkPortObjectSpec;
import org.knime.dl.base.settings.ConfigEntry;
import org.knime.dl.base.settings.ConfigUtil;
import org.knime.dl.core.DLNetwork;
import org.knime.dl.core.DLNetworkSpec;
import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.core.training.DLTrainingContext;
import org.knime.dl.keras.core.training.DLKerasTrainingContext;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
class DLKerasLearnerGeneralPanel extends AbstractGridBagDialogComponentGroup {

	private final DLKerasLearnerGeneralConfig m_cfg;

	private final DialogComponentObjectSelection<DLKerasTrainingContext<?>> m_dcBackend;

	DLKerasLearnerGeneralPanel(final DLKerasLearnerGeneralConfig cfg, final DLNetworkSpec networkSpec,
			final Class<? extends DLNetwork> networkType) {
		m_cfg = cfg;

		m_dcBackend = new DialogComponentObjectSelection<>(m_cfg.getTrainingContextEntry(), DLTrainingContext::getName,
				"Back end");
		addDoubleColumnRow(getFirstComponent(m_dcBackend, JLabel.class),
				getFirstComponent(m_dcBackend, JComboBox.class));

		addNumberSpinnerRowComponent(
				ConfigUtil.toSettingsModelIntegerBounded(m_cfg.getEpochsEntry(), 1, Integer.MAX_VALUE), "Epochs", 1);

		addNumberSpinnerRowComponent(
				ConfigUtil.toSettingsModelIntegerBounded(m_cfg.getBatchSizeEntry(), 1, Integer.MAX_VALUE),
				"Training batch size", 1);

		addNumberSpinnerRowComponent(
				ConfigUtil.toSettingsModelIntegerBounded(m_cfg.getValidationBatchSizeEntry(), 1, Integer.MAX_VALUE),
				"Validation batch size", 1);
		
		ConfigEntry<Boolean> shuffleEntry = m_cfg.getShuffleTrainingData();
		addCheckboxRow(ConfigUtil.toSettingsModelBoolean(shuffleEntry),
				"Shuffle training data before each epoch", true);
		
		ConfigEntry<Long> randomSeedConfig = m_cfg.getRandomSeed();
		DialogComponentRandomSeed randomSeed = 
				new DialogComponentRandomSeed(ConfigUtil.toSettingsModelLong(randomSeedConfig));
		addToggleComponentGroup(randomSeedConfig, "Use random seed", randomSeed);
		JCheckBox toggleCheckBox = getLastCheckBox();
		// TODO: Once we use the seed also for other purposes, we need to cut the enable/disable
		// connection between the shuffle checkbox and the seed settings
		toggleCheckBox.setEnabled(shuffleEntry.getValue());
		shuffleEntry.addValueChangeListener((e, v) -> {
			randomSeedConfig.setEnabled(!v);
			toggleCheckBox.setEnabled(!v);
		});
	}
	
	private JCheckBox getLastCheckBox() {
		Component[] components = getComponentGroupPanel().getComponents();
		JCheckBox[] checkBoxes = Arrays.stream(components).filter(c -> c instanceof JPanel)
				.map(c -> (JPanel)c).flatMap(p -> Arrays.stream(p.getComponents()))
				.filter(c -> c instanceof JCheckBox).map(c -> (JCheckBox)c).toArray(i -> new JCheckBox[i]);
		return checkBoxes[checkBoxes.length - 1];
	}

	@Override
	public void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
		// no op
	}

	@Override
	public void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
			throws NotConfigurableException {
		final DLNetworkPortObjectSpec portObjectSpec = (DLNetworkPortObjectSpec) specs[DLKerasLearnerNodeModel.IN_NETWORK_PORT_IDX];
		final Class<? extends DLNetwork> networkType = portObjectSpec.getNetworkType();
		final DLNetworkSpec networkSpec = portObjectSpec.getNetworkSpec();

		refreshAvailableBackends(networkType);
		// Check if the network has pre-defined input batch sizes. Note that different batch sizes for the same network
		// are not supported (for networks with multiple inputs).
		long batchSize = -1;
		for (final DLTensorSpec inputSpec : networkSpec.getInputSpecs()) {
			if (inputSpec.getBatchSize().isPresent()) {
				final long bs = inputSpec.getBatchSize().getAsLong();
				if (batchSize == -1) {
					batchSize = bs;
				} else {
					if (batchSize != bs) {
						throw new NotConfigurableException(
								"The input network has multiple inputs with different pre-defined batch sizes. "
										+ "This is not supported. Please make sure to use a network with uniform "
										+ "input batch sizes or no pre-defined batch size at all.");
					}
				}
			}
		}
		if (batchSize != -1) {
			m_cfg.getBatchSizeEntry().setValue((int) batchSize);
			m_cfg.getBatchSizeEntry().setEnabled(false);
			m_cfg.getValidationBatchSizeEntry().setValue((int) batchSize);
			m_cfg.getValidationBatchSizeEntry().setEnabled(false);
		} else {
			m_cfg.getBatchSizeEntry().setEnabled(true);
			m_cfg.getValidationBatchSizeEntry()
					.setEnabled(specs[DLKerasLearnerNodeModel.IN_VALIDATION_DATA_PORT_IDX] != null);
		}
	}

	private void refreshAvailableBackends(final Class<? extends DLNetwork> networkType)
			throws NotConfigurableException {
		// refresh available back ends
		final List<DLKerasTrainingContext<?>> availableTrainingContexts = DLKerasLearnerGeneralConfig
				.getAvailableTrainingContexts(networkType).stream()
				.sorted(Comparator.comparing(DLKerasTrainingContext::getName)) //
				.collect(Collectors.toList());

		final DLKerasTrainingContext<?> value = m_cfg.getTrainingContextEntry().getValue();
		final DLKerasTrainingContext<?> selectedTrainingContext;
		if (availableTrainingContexts.isEmpty()) {
			throw new NotConfigurableException("There is no available back end that supports the input network.");
		} else if (value != null && containsContext(availableTrainingContexts, value)) {
			selectedTrainingContext = m_cfg.getTrainingContextEntry().getValue();
		} else {
			m_cfg.getTrainingContextEntry().setValue(value);
			selectedTrainingContext = availableTrainingContexts.get(0);
		}
		m_dcBackend.replaceListItems(availableTrainingContexts, selectedTrainingContext);
	}

	private boolean containsContext(final List<DLKerasTrainingContext<?>> contexts,
			final DLKerasTrainingContext<?> context) {
		for (final DLKerasTrainingContext<?> check : contexts) {
			if (check.getNetworkType().isAssignableFrom(context.getNetworkType())) {
				return true;
			}
		}
		return false;
	}
}
