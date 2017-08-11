package org.knime.dl.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface DLNetworkSpecSerializer<T> {
	void serialize(OutputStream out, T t) throws IOException;

	T deserialize(InputStream in) throws IOException;
}
