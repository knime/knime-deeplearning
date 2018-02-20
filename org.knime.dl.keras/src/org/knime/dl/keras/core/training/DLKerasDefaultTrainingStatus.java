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

import org.knime.dl.core.DLDefaultEvent;
import org.knime.dl.core.DLEvent;
import org.knime.dl.core.training.DLAbstractTrainingStatus;
import org.knime.dl.keras.base.nodes.learner.view.DLProgressMonitor;
import org.knime.dl.keras.base.nodes.learner.view.DLViewDataCollection;
import org.knime.dl.keras.base.nodes.learner.view.DLViewSpec;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public final class DLKerasDefaultTrainingStatus extends DLAbstractTrainingStatus
		implements DLKerasTrainingStatus, DLProgressMonitor {

	private DLViewSpec[] m_viewSpecs;

	private DLViewDataCollection[] m_viewData;

	private final DLEvent<Integer> m_stoppedEarly = new DLDefaultEvent<>();

	private final DLEvent<Long> m_terminatedOnNaNLoss = new DLDefaultEvent<>();

	public DLKerasDefaultTrainingStatus(final int numEpochs, final int numBatchesPerEpoch) {
		super(numEpochs, numBatchesPerEpoch);
		m_stoppedEarly.addListener((src, epoch) -> setStatus(Status.STOPPED_EARLY));
		m_terminatedOnNaNLoss.addListener((src, batch) -> setStatus(Status.STOPPED_EARLY));
	}

	/**
	 * Empty framework constructor. Must not be called by client code.
	 */
	public DLKerasDefaultTrainingStatus() {
		m_stoppedEarly.addListener((src, epoch) -> setStatus(Status.STOPPED_EARLY));
		m_terminatedOnNaNLoss.addListener((src, batch) -> setStatus(Status.STOPPED_EARLY));
	}

	@Override
	public boolean isRunning() {
		return getStatus() == Status.RUNNING;
	}

	@Override
	public boolean hasStoppedEarly() {
		return getStatus() == Status.STOPPED_EARLY;
	}

	@Override
	public boolean hasFinished() {
		return getStatus() == Status.FINISHED;
	}

	@Override
	public boolean hasData() {
		return m_viewData != null;
	}

	@Override
	public DLViewSpec[] getViewSpecs() {
		return m_viewSpecs;
	}

	@Override
	public void setViewSpecs(final DLViewSpec[] viewSpecs) {
		m_viewSpecs = viewSpecs;
	}

	@Override
	public DLViewDataCollection[] getViewData() {
		return m_viewData;
	}

	@Override
	public void setViewData(final DLViewDataCollection[] viewData) {
		m_viewData = viewData;
	}

	@Override
	public DLEvent<Integer> stoppedEarly() {
		return m_stoppedEarly;
	}

	@Override
	public DLEvent<Long> terminatedOnNaNLoss() {
		return m_terminatedOnNaNLoss;
	}
}
