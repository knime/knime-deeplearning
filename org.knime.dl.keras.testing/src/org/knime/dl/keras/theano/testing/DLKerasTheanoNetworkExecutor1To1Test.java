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
 *   May 23, 2017 (marcel): created
 */
package org.knime.dl.keras.theano.testing;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Map.Entry;
import java.util.Set;

import org.junit.Test;
import org.knime.core.util.FileUtil;
import org.knime.dl.core.DLLayerData;
import org.knime.dl.core.DLLayerDataSpec;
import org.knime.dl.core.DLNetworkSpec;
import org.knime.dl.core.data.DLWritableBuffer;
import org.knime.dl.core.data.DLWritableFloatBuffer;
import org.knime.dl.core.execution.DLExecutableNetworkAdapter;
import org.knime.dl.core.execution.DLLayerDataBatch;
import org.knime.dl.keras.theano.core.DLKerasTheanoNetwork;
import org.knime.dl.keras.theano.core.DLKerasTheanoNetworkSpec;
import org.knime.dl.keras.theano.core.DLKerasTheanoNetworkType;
import org.knime.dl.keras.theano.core.execution.DLKerasTheanoDefaultExecutionContext;
import org.knime.dl.python.core.DLPythonDefaultNetworkReader;
import org.knime.dl.util.DLUtils;

/**
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
public class DLKerasTheanoNetworkExecutor1To1Test {

	private static final String BUNDLE_ID = "org.knime.dl.keras.testing";

	@Test
	public void test() throws Exception {
		final URL source = FileUtil
				.toURL(DLUtils.Files.getFileFromBundle(BUNDLE_ID, "data/my_2d_input_model.h5").getAbsolutePath());
		final DLKerasTheanoDefaultExecutionContext exec = new DLKerasTheanoDefaultExecutionContext();
		final DLPythonDefaultNetworkReader<DLKerasTheanoNetwork, DLKerasTheanoNetworkSpec> reader =
				new DLPythonDefaultNetworkReader<>(DLKerasTheanoNetworkType.INSTANCE.getLoader());
		DLKerasTheanoNetwork network;
		try {
			network = reader.read(source);
		} catch (IllegalArgumentException | IOException e) {
			throw new RuntimeException(e);
		}
		final DLNetworkSpec networkSpec = network.getSpec();
		final Set<DLLayerDataSpec> selectedOutputs = Collections.singleton(networkSpec.getOutputSpecs()[0]);
		final DLExecutableNetworkAdapter execNetwork = exec.executable(network, selectedOutputs);
		execNetwork.execute(in -> {
			for (final Entry<DLLayerDataSpec, DLLayerDataBatch<? extends DLWritableBuffer>> entry : in.entrySet()) {
				populate(entry.getValue().getBatch()[0]);
			}
		}, out -> {
			// TODO: test against known results - this is sth. that should rather be tested via a test workflow
		}, 1);
	}

	private static void populate(final DLLayerData<?> data) {
		if (data.getBuffer() instanceof DLWritableFloatBuffer) {
			final DLWritableFloatBuffer buffer = (DLWritableFloatBuffer) data.getBuffer();
			buffer.resetWrite();
			for (int i = 0; i < buffer.getCapacity(); i++) {
				buffer.put(5f);
			}
		} else {
			throw new IllegalStateException("Unexpected input buffer type.");
		}
	}
}
