package org.knime.dl.python.core;

import java.io.IOException;

import org.knime.core.node.port.PortType;
import org.knime.core.node.port.PortTypeRegistry;
import org.knime.dl.base.portobjects.DLNetworkPortObject;
import org.knime.dl.core.DLInvalidSourceException;
import org.knime.dl.core.DLNetwork;

/**
 * Base interface for all Python deep learning port objects.
 *
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 * @param <N> the type of the contained Python network
 */
public interface DLPythonNetworkPortObject<N extends DLNetwork & DLPythonNetwork> extends DLNetworkPortObject {

	/**
	 * The Python deep learning network port type.
	 */
	public static final PortType TYPE = PortTypeRegistry.getInstance().getPortType(DLPythonNetworkPortObject.class);

	@Override
	N getNetwork() throws DLInvalidSourceException, IOException;
}
