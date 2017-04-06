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
 * History
 *   May 22, 2017 (marcel): created
 */
package org.knime.dl.base.portobjects;

import java.net.MalformedURLException;
import java.net.URL;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.port.AbstractSimplePortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.PortTypeRegistry;
import org.knime.dl.core.DLNetwork;

/**
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
public final class DLNetworkReferencePortObject extends AbstractSimplePortObject implements DLNetworkPortObject {

	/**
	 * The deep learning port type.
	 */
	public static final PortType TYPE = PortTypeRegistry.getInstance().getPortType(DLNetworkReferencePortObject.class);

	private static final String SUMMARY = "Deep Learning Network Model";

	private static final String CFG_KEY_NETWORK_REF = "network_ref";

	private URL m_networkReference;

	private DLNetworkPortObjectSpec m_spec;

	private DLNetwork m_network;

	/**
	 * Creates a new instance of this port object.
	 *
	 * @param networkReference
	 *            the source of the network
	 * @param spec
	 *            the corresponding port object spec
	 */
	public DLNetworkReferencePortObject(final URL networkReference, final DLNetworkPortObjectSpec spec) {
		m_networkReference = networkReference;
		m_spec = spec;
	}

	/**
	 * Empty framework constructor. Must not be called by client code.
	 */
	public DLNetworkReferencePortObject() {
		// fields get populated in load(..)
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getSummary() {
		return SUMMARY;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DLNetwork getNetwork() {
		if (m_network == null) {
			try {
				// FIXME later, with new backends we have to change that.
				m_network = ((DLDefaultNetworkPortObjectSpec) m_spec).getNetwork();
			} catch (IllegalArgumentException e) {
				throw new IllegalStateException(
						"Deep learning network at location '" + m_networkReference.toString() + "' could not be read.",
						e);
			}
		}
		return m_network;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DLNetworkPortObjectSpec getSpec() {
		return m_spec;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void load(final ModelContentRO model, final PortObjectSpec spec, final ExecutionMonitor exec)
			throws InvalidSettingsException, CanceledExecutionException {
		try {
			m_networkReference = new URL(model.getString(CFG_KEY_NETWORK_REF));
		} catch (final MalformedURLException e) {
			throw new InvalidSettingsException("Failed to load deep learning port object.", e);
		}
		m_spec = (DLNetworkPortObjectSpec) spec;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void save(final ModelContentWO model, final ExecutionMonitor exec) throws CanceledExecutionException {
		model.addString(CFG_KEY_NETWORK_REF, m_networkReference.toString());
	}

	/**
	 * The serializer of {@link DLNetworkReferencePortObject}.
	 */
	public static final class DLNetworkReferencePortObjectSerializer
			extends AbstractSimplePortObjectSerializer<DLNetworkReferencePortObject> {
	}
}
