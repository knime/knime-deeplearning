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
 * History
 *   May 23, 2017 (marcel): created
 */
package org.knime.dl.keras.tensorflow.testing;

import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.junit.Test;
import org.knime.core.util.FileUtil;
import org.knime.dl.core.DLCanceledExecutionException;
import org.knime.dl.core.DLInvalidNetworkInputException;
import org.knime.dl.core.DLNetworkInputPreparer;
import org.knime.dl.core.DLTensor;
import org.knime.dl.core.DLTensorId;
import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.core.data.DLWritableBuffer;
import org.knime.dl.core.data.DLWritableFloatBuffer;
import org.knime.dl.core.training.DLTrainingMonitor;
import org.knime.dl.keras.core.training.DLKerasCallback;
import org.knime.dl.keras.core.training.DLKerasDefaultTrainingConfig;
import org.knime.dl.keras.core.training.DLKerasDefaultTrainingStatus;
import org.knime.dl.keras.core.training.DLKerasLossFunction;
import org.knime.dl.keras.core.training.DLKerasOptimizer;
import org.knime.dl.keras.core.training.DLKerasTrainingConfig;
import org.knime.dl.keras.core.training.DLKerasTrainingStatus;
import org.knime.dl.keras.tensorflow.core.DLKerasTensorFlowNetwork;
import org.knime.dl.keras.tensorflow.core.DLKerasTensorFlowNetworkLoader;
import org.knime.dl.keras.tensorflow.core.training.DLKerasTensorFlowDefaultTrainingContext;
import org.knime.dl.keras.tensorflow.core.training.DLKerasTensorFlowNetworkTrainingSession;
import org.knime.dl.python.core.DLPythonDefaultNetworkReader;
import org.knime.dl.testing.DLTestTrainingMonitor;
import org.knime.dl.util.DLUtils;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
// TODO: introduce abstract base class for back end tests
public class DLKerasTensorFlowNetworkLearnerTest {

	private static final String BUNDLE_ID = "org.knime.dl.keras.testing";

	@Test
	public void test1To1() throws Exception {
		final URL source = FileUtil
				.toURL(DLUtils.Files.getFileFromBundle(BUNDLE_ID, "data/simple_test_model.h5").getAbsolutePath());
		final DLKerasTensorFlowDefaultTrainingContext ctx = new DLKerasTensorFlowDefaultTrainingContext();
		final DLPythonDefaultNetworkReader<DLKerasTensorFlowNetwork> reader = new DLPythonDefaultNetworkReader<>(
				new DLKerasTensorFlowNetworkLoader());
		final DLKerasTensorFlowNetwork network = reader.read(source, true);

		final int dataSetSize = 10;
		final int batchSize = 1;

		final DLTensorSpec inputSpec = network.getSpec().getInputSpecs()[0];
		final DLTensorSpec targetSpec = network.getSpec().getOutputSpecs()[0];

		final Set<DLTensorSpec> executionInputSpecs = new HashSet<>(Arrays.asList(
				ctx.getTensorFactory().createExecutionTensorSpec(inputSpec, batchSize,
						DLUtils.Shapes.getFixedShape(inputSpec.getShape()).get()),
				ctx.getTensorFactory().createExecutionTensorSpec(targetSpec, batchSize,
						DLUtils.Shapes.getFixedShape(targetSpec.getShape()).get())));

		// training:

		final int epochs = 2;
		final DLKerasOptimizer optimizer = ctx.createOptimizers().iterator().next();
		final DLKerasLossFunction loss = ctx.createLossFunctions().iterator().next();
		final Map<DLTensorSpec, DLKerasLossFunction> losses = new HashMap<>(network.getSpec().getOutputSpecs().length);
		for (int i = 0; i < network.getSpec().getOutputSpecs().length; i++) {
			losses.put(network.getSpec().getOutputSpecs()[i], loss);
		}

		final List<DLKerasCallback> callbacks = Collections.emptyList();
		final DLKerasTrainingConfig config = new DLKerasDefaultTrainingConfig(epochs, batchSize, null, optimizer,
				losses, callbacks);
		final DLTrainingMonitor<DLKerasTrainingStatus> monitor = new DLTestTrainingMonitor<>(
				new DLKerasDefaultTrainingStatus(1, dataSetSize / batchSize));

		try (final DLKerasTensorFlowNetworkTrainingSession session = ctx.createTrainingSession(network, config,
				executionInputSpecs, new DLNetworkInputPreparer() {

					@Override
					public long getNumBatches() {
						return dataSetSize / batchSize;
					}

					@Override
					public void prepare(Map<DLTensorId, DLTensor<? extends DLWritableBuffer>> input, long batchIndex)
							throws DLCanceledExecutionException, DLInvalidNetworkInputException {
						for (final Entry<DLTensorId, DLTensor<? extends DLWritableBuffer>> entry : input.entrySet()) {
							populate(entry.getValue());
						}
					}

					@Override
					public void close() throws Exception {
						// no op
					}
				}, null)) {
			session.run(monitor);
			// TODO: test if training worked
		}
	}

	@Test
	public void test2To2() throws Exception {
		final URL source = FileUtil
				.toURL(DLUtils.Files.getFileFromBundle(BUNDLE_ID, "data/multi_in_out.h5").getAbsolutePath());
		final DLKerasTensorFlowDefaultTrainingContext ctx = new DLKerasTensorFlowDefaultTrainingContext();
		final DLPythonDefaultNetworkReader<DLKerasTensorFlowNetwork> reader = new DLPythonDefaultNetworkReader<>(
				new DLKerasTensorFlowNetworkLoader());
		final DLKerasTensorFlowNetwork network = reader.read(source, true);

		final int dataSetSize = 10;
		final int batchSize = 1;

		final DLTensorSpec inputSpec0 = network.getSpec().getInputSpecs()[0];
		final DLTensorSpec inputSpec1 = network.getSpec().getInputSpecs()[1];
		final DLTensorSpec targetSpec0 = network.getSpec().getOutputSpecs()[0];
		final DLTensorSpec targetSpec1 = network.getSpec().getOutputSpecs()[1];

		final Set<DLTensorSpec> executionInputSpecs = new HashSet<>(Arrays.asList(
				ctx.getTensorFactory().createExecutionTensorSpec(inputSpec0, batchSize,
						DLUtils.Shapes.getFixedShape(inputSpec0.getShape()).get()),
				ctx.getTensorFactory().createExecutionTensorSpec(inputSpec1, batchSize,
						DLUtils.Shapes.getFixedShape(inputSpec1.getShape()).get()),
				ctx.getTensorFactory().createExecutionTensorSpec(targetSpec0, batchSize,
						DLUtils.Shapes.getFixedShape(targetSpec0.getShape()).get()),
				ctx.getTensorFactory().createExecutionTensorSpec(targetSpec1, batchSize,
						DLUtils.Shapes.getFixedShape(targetSpec1.getShape()).get())));

		// training:

		final int epochs = 2;
		final DLKerasOptimizer optimizer = ctx.createOptimizers().iterator().next();
		final DLKerasLossFunction loss = ctx.createLossFunctions().iterator().next();
		final Map<DLTensorSpec, DLKerasLossFunction> losses = new HashMap<>(network.getSpec().getOutputSpecs().length);
		for (int i = 0; i < network.getSpec().getOutputSpecs().length; i++) {
			losses.put(network.getSpec().getOutputSpecs()[i], loss);
		}

		final List<DLKerasCallback> callbacks = Collections.emptyList();
		final DLKerasTrainingConfig config = new DLKerasDefaultTrainingConfig(epochs, batchSize, null, optimizer,
				losses, callbacks);
		final DLTrainingMonitor<DLKerasTrainingStatus> monitor = new DLTestTrainingMonitor<>(
				new DLKerasDefaultTrainingStatus(1, dataSetSize / batchSize));

		try (final DLKerasTensorFlowNetworkTrainingSession session = ctx.createTrainingSession(network, config,
				executionInputSpecs, new DLNetworkInputPreparer() {

					@Override
					public long getNumBatches() {
						return dataSetSize / batchSize;
					}

					@Override
					public void prepare(Map<DLTensorId, DLTensor<? extends DLWritableBuffer>> input, long batchIndex)
							throws DLCanceledExecutionException, DLInvalidNetworkInputException {
						for (final Entry<DLTensorId, DLTensor<? extends DLWritableBuffer>> entry : input.entrySet()) {
							populate(entry.getValue());
						}
					}

					@Override
					public void close() throws Exception {
						// no op

					}
				}, null)) {
			session.run(monitor);
			// TODO: test if training worked
		}
	}

	private static void populate(final DLTensor<?> data) {
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
