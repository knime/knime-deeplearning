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
 *   Jun 12, 2017 (marcel): created
 */
package org.knime.dl.base.portobjects;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Optional;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.ModelContentRO;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.AbstractSimplePortObjectSpec;
import org.knime.dl.core.DLNetwork;
import org.knime.dl.core.DLNetworkSpec;
import org.knime.dl.core.backend.DLBackend;
import org.knime.dl.core.backend.DLBackendRegistry;
import org.knime.dl.core.backend.DLProfile;

/**
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
public class DLDefaultNetworkPortObjectSpec extends AbstractSimplePortObjectSpec implements DLNetworkPortObjectSpec {

	private static final NodeLogger LOGGER = NodeLogger.getLogger(DLNetworkPortObject.class);

	private static final String CFG_KEY_PROFILE = "profile";

	private static final String CFG_KEY_NETWORK_REF = "network_ref";

	private DLNetwork m_network;

	private DLProfile m_profile;

	private URL m_networkReference;

	/**
	 * Creates a new instance of this port object spec.
	 *
	 * @param networkSpec
	 *            the network spec
	 * @param profile
	 *            the profile
	 * @param the
	 *            source of the network - TODO: this is temporary and has to be
	 *            removed as it does not apply to e.g. networks that were
	 *            created within KNIME (it's currently just easier to read the
	 *            entire file from source than (de)serializing the network specs
	 *            ourselves)
	 */
	public DLDefaultNetworkPortObjectSpec(final DLNetwork network, final DLProfile profile,
			final URL networkReference) {
		m_network = network;
		m_profile = profile;
		m_networkReference = networkReference;
	}

	/**
	 * Empty framework constructor. Must not be called by client code.
	 */
	public DLDefaultNetworkPortObjectSpec() {
		// fields get populated in load(..)
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DLNetworkSpec getNetworkSpec() {
		return m_network.getSpec();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DLProfile getProfile() {
		return m_profile;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void load(final ModelContentRO model) throws InvalidSettingsException {
		final String[] backendIds = model.getStringArray(CFG_KEY_PROFILE);
		final DLBackend[] backends = new DLBackend[backendIds.length];
		boolean backendAvailable = false;
		for (int i = 0; i < backendIds.length; i++) {
			final Optional<DLBackend> be = DLBackendRegistry.getBackend(backendIds[i]);
			if (be.isPresent()) {
				backends[i] = be.get();
				backendAvailable = true;
			} else {
				LOGGER.warn("Back end '" + backendIds[i]
						+ "' could not be found. Try to continue with a different compatible back end...");
			}
		}
		if (!backendAvailable) {
			final String msg = "No compatible back end could be loaded.";
			LOGGER.error(msg);
			throw new InvalidSettingsException(msg);
		}
		m_profile = new DLProfile() {

			@Override
			public Iterator<DLBackend> iterator() {
				return Arrays.asList(backends).iterator();
			}

			@Override
			public int size() {
				return backends.length;
			}
		};
		try {
			m_networkReference = new URL(model.getString(CFG_KEY_NETWORK_REF));
		} catch (final MalformedURLException e) {
			throw new InvalidSettingsException("Failed to load deep learning port object spec.", e);
		}
		final DLBackend be = DLBackendRegistry.getPreferredBackend(m_profile);
		try {
			// TODO write cache for spec.
			m_network = be.createReader().readNetwork(m_networkReference);
		} catch (IllegalArgumentException | IOException e) {
			throw new InvalidSettingsException("Failed to load deep learning port object spec.", e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void save(final ModelContentWO model) {
		final String[] backendIds = new String[m_profile.size()];
		int i = 0;
		for (final DLBackend be : m_profile) {
			backendIds[i++] = be.getIdentifier();
		}
		model.addStringArray(CFG_KEY_PROFILE, backendIds);
		model.addString(CFG_KEY_NETWORK_REF, m_networkReference.toString());
	}

	/**
	 * The serializer of {@link DLDefaultNetworkPortObjectSpec}.
	 */
	public static final class DLDefaultNetworkPortObjectSpecSerializer
			extends AbstractSimplePortObjectSpecSerializer<DLDefaultNetworkPortObjectSpec> {
	}

	// TODO remove later. NON API
	DLNetwork getNetwork() {
		return m_network;
	}
}
