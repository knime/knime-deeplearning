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
package org.knime.dl.keras.testing;

import static org.knime.dl.testing.DLTestUtil.randomTensorSpec;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.nio.file.InvalidPathException;
import java.util.Random;

import org.apache.commons.lang3.RandomStringUtils;
import org.knime.core.util.FileUtil;
import org.knime.core.util.Version;
import org.knime.dl.core.DLNetworkLocation;
import org.knime.dl.core.DLNetworkReferenceLocation;
import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.keras.tensorflow.core.DLKerasTensorFlowNetwork;
import org.knime.dl.keras.tensorflow.core.DLKerasTensorFlowNetworkSpec;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public final class DLKerasTestUtil {

	private static final int MAX_NETWORK_INPUTS = 10;
	private static final int MAX_NETWORK_HIDDEN_OUTPUTS = 10;
	private static final int MAX_NETWORK_OUTPUTS = 10;

	private DLKerasTestUtil() {
		// utility class
	}

	public static DLKerasTensorFlowNetwork randomNetwork(final Random random) {
		final DLKerasTensorFlowNetworkSpec spec = randomNetworkSpec(random);
        final DLNetworkLocation source = randomNetworkSource(random);
		return new DLKerasTensorFlowNetwork(spec, source);
	}

	public static DLKerasTensorFlowNetworkSpec randomNetworkSpec(final Random random) {
		final int numInputs = random.nextInt(MAX_NETWORK_INPUTS) + 1;
		final DLTensorSpec[] inputs = new DLTensorSpec[numInputs];
		for (int i = 0; i < numInputs; i++) {
			inputs[i] = randomTensorSpec(random);
		}
		final int numHidden = random.nextInt(MAX_NETWORK_HIDDEN_OUTPUTS) + 1;
		final DLTensorSpec[] hidden = new DLTensorSpec[numHidden];
		for (int i = 0; i < numHidden; i++) {
			hidden[i] = randomTensorSpec(random);
		}
		final int numOutputs = random.nextInt(MAX_NETWORK_OUTPUTS) + 1;
		final DLTensorSpec[] outputs = new DLTensorSpec[numOutputs];
		for (int i = 0; i < numOutputs; i++) {
			outputs[i] = randomTensorSpec(random);
		}
        final Version pythonVersion = new Version(random.nextInt(), random.nextInt(), random.nextInt());
        final Version kerasVersion = new Version(random.nextInt(), random.nextInt(), random.nextInt());
        return new DLKerasTensorFlowNetworkSpec(pythonVersion, kerasVersion, inputs, hidden, outputs);
	}

    public static DLNetworkLocation randomNetworkSource(final Random random) {
        try {
            return new DLNetworkReferenceLocation(
                FileUtil.toURL(RandomStringUtils.random(10, 0, 0, true, true, null, random) + ".h5").toURI());
        } catch (InvalidPathException | MalformedURLException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
