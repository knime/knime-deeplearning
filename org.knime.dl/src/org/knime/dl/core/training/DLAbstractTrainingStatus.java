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
 */
package org.knime.dl.core.training;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

import org.knime.dl.core.DLDefaultEvent;
import org.knime.dl.core.DLEvent;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public class DLAbstractTrainingStatus implements DLTrainingStatus {

	private int m_numEpochs;

	private int m_numBatchesPerEpoch;

	private Status m_status = Status.NOT_STARTED;

	private LocalDateTime m_startDateTime;

	private LocalDateTime m_endDateTime;

	private int m_currentEpoch;

	private int m_currentBatchInEpoch;

	private Map<String, DLMetrics> m_metrics;

	private final DLEvent<Void> m_trainingStarted = new DLDefaultEvent<>();

	private final DLEvent<Void> m_trainingEnded = new DLDefaultEvent<>();

	private final DLEvent<Void> m_batchEnded = new DLDefaultEvent<>();

	@Override
	public int getNumEpochs() {
		return m_numEpochs;
	}

	@Override
	public void setNumEpochs(final int numEpochs) {
		m_numEpochs = numEpochs;
	}

	@Override
	public int getNumBatchesPerEpoch() {
		return m_numBatchesPerEpoch;
	}

	@Override
	public void setNumBatchesPerEpoch(final int numBatchesPerEpoch) {
		m_numBatchesPerEpoch = numBatchesPerEpoch;
	}

	@Override
	public Status getStatus() {
		return m_status;
	}

	@Override
	public void setStatus(final Status status) {
		m_status = status;
	}

	@Override
	public LocalDateTime getStartDateTime() {
		return m_startDateTime;
	}

	@Override
	public void setStartDateTime(final LocalDateTime startDateTime) {
		m_startDateTime = startDateTime;
	}

	@Override
	public LocalDateTime getEndDateTime() {
		return m_endDateTime;
	}

	@Override
	public void setEndDateTime(final LocalDateTime endDateTime) {
		m_endDateTime = endDateTime;
	}

	@Override
	public int getCurrentEpoch() {
		return m_currentEpoch;
	}

	@Override
	public void setCurrentEpoch(final int currentEpoch) {
		m_currentEpoch = currentEpoch;
	}

	@Override
	public int getCurrentBatchInEpoch() {
		return m_currentBatchInEpoch;
	}

	@Override
	public void setCurrentBatchInEpoch(final int currentBatchInEpoch) {
		m_currentBatchInEpoch = currentBatchInEpoch;
	}

	@Override
	public Map<String, DLMetrics> getMetrics() {
		return m_metrics;
	}

	@Override
	public <M extends Map<String, DLMetrics> & Serializable> void setMetrics(final M metrics) {
		m_metrics = metrics;
	}

	// callbacks:

	@Override
	public DLEvent<Void> trainingStarted() {
		return m_trainingStarted;
	}

	@Override
	public DLEvent<Void> trainingEnded() {
		return m_trainingEnded;
	}

	@Override
	public DLEvent<Void> batchEnded() {
		return m_batchEnded;
	}

	// --

	@Override
	public void writeExternal(final ObjectOutput objOut) throws IOException {
		objOut.writeInt(m_numEpochs);
		objOut.writeInt(m_numBatchesPerEpoch);
		objOut.writeObject(m_status);
		objOut.writeObject(m_startDateTime);
		objOut.writeObject(m_endDateTime);
		objOut.writeInt(m_currentEpoch);
		objOut.writeInt(m_currentBatchInEpoch);
		objOut.writeObject(m_metrics);
	}

	@Override
	public void readExternal(final ObjectInput objIn) throws IOException, ClassNotFoundException {
		m_numEpochs = objIn.readInt();
		m_numBatchesPerEpoch = objIn.readInt();
		m_status = (Status) objIn.readObject();
		m_startDateTime = (LocalDateTime) objIn.readObject();
		m_endDateTime = (LocalDateTime) objIn.readObject();
		m_currentEpoch = objIn.readInt();
		m_currentBatchInEpoch = objIn.readInt();
		@SuppressWarnings("unchecked") // we know what we wrote
		final Map<String, DLMetrics> metrics = (Map<String, DLMetrics>) objIn.readObject();
		m_metrics = metrics;
	}
}
