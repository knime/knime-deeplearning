package org.knime.dl.core.modelling;

import org.knime.dl.core.DLLayerOp;
import org.knime.dl.core.DLNetwork;

public interface DLEditableNetwork extends DLNetwork {

	DLEditableNetwork apply(DLLayerOp op);
}
