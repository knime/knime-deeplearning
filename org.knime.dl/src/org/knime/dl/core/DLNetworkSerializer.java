package org.knime.dl.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface DLNetworkSerializer<N extends DLNetwork<S>, S extends DLNetworkSpec> {
	void serialize(OutputStream out, N t) throws IOException;

	N deserialize(InputStream in, S spec) throws IOException;
}
