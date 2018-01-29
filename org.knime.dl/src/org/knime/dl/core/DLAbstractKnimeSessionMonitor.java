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
package org.knime.dl.core;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.OptionalDouble;
import java.util.function.Supplier;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public abstract class DLAbstractKnimeSessionMonitor implements DLSessionMonitor {

	protected final ExecutionMonitor m_knimeMonitor;

	protected DLAbstractKnimeSessionMonitor(final ExecutionMonitor knimeMonitor) {
		m_knimeMonitor = checkNotNull(knimeMonitor);
	}

	public ExecutionMonitor getKnimeMonitor() {
		return m_knimeMonitor;
	}

	@Override
	public OptionalDouble getProgress() {
		final Double progress = m_knimeMonitor.getProgressMonitor().getProgress();
		return progress != null ? OptionalDouble.of(progress) : OptionalDouble.empty();
	}

	@Override
	public void setProgress(final double progress) {
		m_knimeMonitor.setProgress(progress);
	}

	@Override
	public void setProgress(final double progress, final String message) {
		m_knimeMonitor.setProgress(progress, message);
	}

	@Override
	public void setProgress(final double progress, final Supplier<String> message) {
		m_knimeMonitor.setProgress(progress, message);
	}

	@Override
	public String getMessage() {
		return m_knimeMonitor.getProgressMonitor().getMessage();
	}

	@Override
	public void setMessage(final String message) {
		m_knimeMonitor.setMessage(message);
	}

	@Override
	public void setMessage(final Supplier<String> message) {
		m_knimeMonitor.setMessage(message);
	}

	@Override
	public void checkCanceled() throws DLCanceledExecutionException {
		try {
			m_knimeMonitor.checkCanceled();
		} catch (final CanceledExecutionException e) {
			final String message = e.getMessage();
			if (message != null && !message.isEmpty()) {
				throw new DLCanceledExecutionException(message);
			} else {
				throw new DLCanceledExecutionException();
			}
		}
	}

	@Override
	public void cancel() {
		m_knimeMonitor.getProgressMonitor().setExecuteCanceled();

	}

	@Override
	public void reset() {
		m_knimeMonitor.getProgressMonitor().reset();
	}
}
