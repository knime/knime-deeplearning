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
 * Jun 21, 2017 (marcel): created
 */
package org.knime.dl.python.testing;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.knime.dl.core.DLDefaultDimensionOrder;
import org.knime.dl.core.DLDefaultFixedTensorShape;
import org.knime.dl.core.DLDefaultTensor;
import org.knime.dl.core.DLDefaultTensorId;
import org.knime.dl.core.DLDefaultTensorSpec;
import org.knime.dl.core.DLInvalidEnvironmentException;
import org.knime.dl.core.DLNetworkSpec;
import org.knime.dl.core.DLTensor;
import org.knime.dl.core.DLTensorId;
import org.knime.dl.core.DLTensorShape;
import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.core.data.DLReadableBuffer;
import org.knime.dl.core.data.DLReadableDoubleBuffer;
import org.knime.dl.core.data.DLReadableFloatBuffer;
import org.knime.dl.core.data.DLReadableIntBuffer;
import org.knime.dl.core.data.DLReadableLongBuffer;
import org.knime.dl.core.data.DLWritableBuffer;
import org.knime.dl.python.core.DLPythonAbstractCommands;
import org.knime.dl.python.core.DLPythonNetworkHandle;
import org.knime.dl.python.core.data.DLPythonDoubleBuffer;
import org.knime.dl.python.core.data.DLPythonFloatBuffer;
import org.knime.dl.python.core.data.DLPythonIntBuffer;
import org.knime.dl.python.core.data.DLPythonLongBuffer;
import org.knime.dl.util.DLUtils;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public class DLPythonDataBuffersExecution1To1Test {

	private static final DLPythonNetworkHandle HANDLE = new DLPythonNetworkHandle("dummy");

	private static final int NUM_IN_TENSORS = 1;

	private static final DLTensorId IN_TENSOR_ID = new DLDefaultTensorId("test_in_data");

	private static final String IN_TENSOR_NAME = "test_in_data";

	private static final DLTensorShape IN_TENSOR_SHAPE = new DLDefaultFixedTensorShape(new long[] { 11, 10 });

	private static final DLTensorId OUT_TENSOR_ID = new DLDefaultTensorId("test_out_data");

	private static final String OUT_TENSOR_NAME = "test_out_data";

	private static final DLTensorShape OUT_TENSOR_SHAPE = new DLDefaultFixedTensorShape(new long[] { 11, 10 });

	private static final String[] REQUESTED_OUT_TENSORS = { OUT_TENSOR_NAME };

	private static final String BUNDLE_ID = "org.knime.dl.python.testing";

	private DLPythonAbstractCommands m_commands;

	private Random m_rng;

	@Before
	public void setUp() throws Exception {
		m_commands = new DLPythonAbstractCommands() {

			@Override
			public DLNetworkSpec extractNetworkSpec(final DLPythonNetworkHandle network)
					throws DLInvalidEnvironmentException, IOException {
				return null;
			}

			@Override
			protected File getInstallationTestFile() {
				return null;
			}

			@Override
			protected String getSetupEnvironmentCode() {
				return "";
			}

			@Override
			protected String getSetupBackendCode() {
				return "";
			}

			@Override
			protected String getLoadNetworkCode(final String path, final boolean loadTrainingConfig) {
				return "";
			}
		};
		m_rng = new Random(543677);
	}

	@After
	public void tearDown() throws Exception {
		m_commands.close();
	}

	@Test
	public void testDouble() throws Exception {
		final ArrayList<DLTensor<? extends DLWritableBuffer>> layerData = new ArrayList<>(NUM_IN_TENSORS);
		for (int i = 0; i < NUM_IN_TENSORS; i++) {
			final DLTensorSpec spec = new DLDefaultTensorSpec(IN_TENSOR_ID, IN_TENSOR_NAME, IN_TENSOR_SHAPE,
					double.class, DLDefaultDimensionOrder.TDHWC);
			final long exampleSize = DLUtils.Shapes.getSize(DLUtils.Shapes.getFixedShape(spec.getShape()).get());
			final DLPythonDoubleBuffer buff = new DLPythonDoubleBuffer(exampleSize);
			for (int j = 0; j < buff.getCapacity(); j++) {
				final double val = m_rng.nextDouble() * m_rng.nextInt(Integer.MAX_VALUE / 5);
				buff.put(val);
			}
			layerData.add(new DLDefaultTensor<>(spec, buff, exampleSize));
		}

		final HashMap<DLTensorId, DLTensor<? extends DLWritableBuffer>> networkInput = new HashMap<>();
		for (final DLTensor<? extends DLWritableBuffer> input : layerData) {
			// TODO: typing (also in the other test* methods)
			networkInput.put(input.getSpec().getIdentifier(), input);
		}

		m_commands.setNetworkInputs(HANDLE, networkInput);
		final String code = DLUtils.Files.readAllUTF8(
				DLUtils.Files.getFileFromBundle(BUNDLE_ID, "py/DLPythonDataBuffers1To1ExecutionTest_testDouble.py"));
		m_commands.getContext().executeInKernel(code);

		final HashMap<DLTensorId, DLTensor<? extends DLReadableBuffer>> outputTensorSpecs = new HashMap<>();
		for (final String outputTensorName : REQUESTED_OUT_TENSORS) {
			final DLDefaultTensorSpec spec = new DLDefaultTensorSpec(OUT_TENSOR_ID, outputTensorName, OUT_TENSOR_SHAPE,
					double.class, DLDefaultDimensionOrder.TDHWC);
			// TODO: typing (also in the other test* methods)
			final long exampleSize = DLUtils.Shapes.getSize(DLUtils.Shapes.getFixedShape(spec.getShape()).get());
			outputTensorSpecs.put(spec.getIdentifier(),
					new DLDefaultTensor<>(spec, new DLPythonDoubleBuffer(exampleSize), exampleSize));
		}
		m_commands.getNetworkOutputs(HANDLE, outputTensorSpecs);

		final DLTensor<?> input = networkInput.values().iterator().next();
		final DLTensor<?> output = outputTensorSpecs.values().iterator().next();
		Assert.assertArrayEquals(DLUtils.Shapes.getFixedShape(input.getSpec().getShape()).get(),
				DLUtils.Shapes.getFixedShape(output.getSpec().getShape()).get());
		Assert.assertEquals(input.getSpec().getElementType(), output.getSpec().getElementType());
		final DLReadableDoubleBuffer inputData = (DLReadableDoubleBuffer) input.getBuffer();
		final DLReadableDoubleBuffer outputData = (DLReadableDoubleBuffer) output.getBuffer();

		for (int i = 0; i < inputData.size(); i++) {
			Assert.assertEquals(inputData.readNextDouble() * 5, outputData.readNextDouble(), 0.0);
		}
	}

	@Test
	public void testFloat() throws Exception {
		final ArrayList<DLTensor<? extends DLWritableBuffer>> layerData = new ArrayList<>(NUM_IN_TENSORS);
		for (int i = 0; i < NUM_IN_TENSORS; i++) {
			final DLTensorSpec spec = new DLDefaultTensorSpec(IN_TENSOR_ID, IN_TENSOR_NAME, IN_TENSOR_SHAPE,
					float.class, DLDefaultDimensionOrder.TDHWC);
			final long exampleSize = DLUtils.Shapes.getSize(DLUtils.Shapes.getFixedShape(spec.getShape()).get());
			final DLPythonFloatBuffer buff = new DLPythonFloatBuffer(exampleSize);
			for (int j = 0; j < buff.getCapacity(); j++) {
				final float val = m_rng.nextFloat() * m_rng.nextInt(Short.MAX_VALUE / 5);
				buff.put(val);
			}
			layerData.add(new DLDefaultTensor<>(spec, buff, exampleSize));
		}

		final HashMap<DLTensorId, DLTensor<? extends DLWritableBuffer>> networkInput = new HashMap<>();
		for (final DLTensor<? extends DLWritableBuffer> input : layerData) {
			networkInput.put(input.getSpec().getIdentifier(), input);
		}

		m_commands.setNetworkInputs(HANDLE, networkInput);
		final String code = DLUtils.Files.readAllUTF8(
				DLUtils.Files.getFileFromBundle(BUNDLE_ID, "py/DLPythonDataBuffers1To1ExecutionTest_testFloat.py"));
		m_commands.getContext().executeInKernel(code);

		final HashMap<DLTensorId, DLTensor<? extends DLReadableBuffer>> outputTensorSpecs = new HashMap<>();
		for (final String outputTensorName : REQUESTED_OUT_TENSORS) {
			final DLDefaultTensorSpec spec = new DLDefaultTensorSpec(OUT_TENSOR_ID, outputTensorName, OUT_TENSOR_SHAPE,
					float.class, DLDefaultDimensionOrder.TDHWC);
			final long exampleSize = DLUtils.Shapes.getSize(DLUtils.Shapes.getFixedShape(spec.getShape()).get());
			outputTensorSpecs.put(spec.getIdentifier(),
					new DLDefaultTensor<>(spec, new DLPythonFloatBuffer(exampleSize), exampleSize));
		}
		m_commands.getNetworkOutputs(HANDLE, outputTensorSpecs);

		final DLTensor<?> input = networkInput.values().iterator().next();
		final DLTensor<?> output = outputTensorSpecs.values().iterator().next();
		Assert.assertArrayEquals(DLUtils.Shapes.getFixedShape(input.getSpec().getShape()).get(),
				DLUtils.Shapes.getFixedShape(output.getSpec().getShape()).get());
		Assert.assertEquals(input.getSpec().getElementType(), output.getSpec().getElementType());
		final DLReadableFloatBuffer inputData = (DLReadableFloatBuffer) input.getBuffer();
		final DLReadableFloatBuffer outputData = (DLReadableFloatBuffer) output.getBuffer();

		for (int i = 0; i < inputData.size(); i++) {
			Assert.assertEquals(inputData.readNextFloat() * 5, outputData.readNextFloat(), 0.0f);
		}
	}

	@Test
	public void testInt() throws IOException, DLInvalidEnvironmentException {
		final ArrayList<DLTensor<? extends DLWritableBuffer>> layerData = new ArrayList<>(NUM_IN_TENSORS);
		for (int i = 0; i < NUM_IN_TENSORS; i++) {
			final DLTensorSpec spec = new DLDefaultTensorSpec(IN_TENSOR_ID, IN_TENSOR_NAME, IN_TENSOR_SHAPE, int.class, DLDefaultDimensionOrder.TDHWC);
			final long exampleSize = DLUtils.Shapes.getSize(DLUtils.Shapes.getFixedShape(spec.getShape()).get());
			final DLPythonIntBuffer buff = new DLPythonIntBuffer(exampleSize);
			for (int j = 0; j < buff.getCapacity(); j++) {
				final int val = m_rng.nextInt(Integer.MAX_VALUE / 5);
				buff.put(val);
			}
			layerData.add(new DLDefaultTensor<>(spec, buff, exampleSize));
		}

		final HashMap<DLTensorId, DLTensor<? extends DLWritableBuffer>> networkInput = new HashMap<>();
		for (final DLTensor<? extends DLWritableBuffer> input : layerData) {
			networkInput.put(input.getSpec().getIdentifier(), input);
		}

		m_commands.setNetworkInputs(HANDLE, networkInput);
		final String code = DLUtils.Files.readAllUTF8(
				DLUtils.Files.getFileFromBundle(BUNDLE_ID, "py/DLPythonDataBuffers1To1ExecutionTest_testInt.py"));
		m_commands.getContext().executeInKernel(code);

		final HashMap<DLTensorId, DLTensor<? extends DLReadableBuffer>> outputTensorSpecs = new HashMap<>();
		for (final String outputTensorName : REQUESTED_OUT_TENSORS) {
			final DLDefaultTensorSpec spec = new DLDefaultTensorSpec(OUT_TENSOR_ID, outputTensorName, OUT_TENSOR_SHAPE,
					int.class, DLDefaultDimensionOrder.TDHWC);
			final long exampleSize = DLUtils.Shapes.getSize(DLUtils.Shapes.getFixedShape(spec.getShape()).get());
			outputTensorSpecs.put(spec.getIdentifier(),
					new DLDefaultTensor<>(spec, new DLPythonIntBuffer(exampleSize), exampleSize));
		}
		m_commands.getNetworkOutputs(HANDLE, outputTensorSpecs);

		final DLTensor<?> input = networkInput.values().iterator().next();
		final DLTensor<?> output = outputTensorSpecs.values().iterator().next();
		Assert.assertArrayEquals(DLUtils.Shapes.getFixedShape(input.getSpec().getShape()).get(),
				DLUtils.Shapes.getFixedShape(output.getSpec().getShape()).get());
		Assert.assertEquals(input.getSpec().getElementType(), output.getSpec().getElementType());
		final DLReadableIntBuffer inputData = (DLReadableIntBuffer) input.getBuffer();
		final DLReadableIntBuffer outputData = (DLReadableIntBuffer) output.getBuffer();

		for (int i = 0; i < inputData.size(); i++) {
			Assert.assertEquals(inputData.readNextInt() * 5, outputData.readNextInt());
		}
	}

	@Test
	public void testLong() throws Exception {
		final ArrayList<DLTensor<? extends DLWritableBuffer>> layerData = new ArrayList<>(NUM_IN_TENSORS);
		for (int i = 0; i < NUM_IN_TENSORS; i++) {
			final DLTensorSpec spec = new DLDefaultTensorSpec(IN_TENSOR_ID, IN_TENSOR_NAME, IN_TENSOR_SHAPE,
					long.class, DLDefaultDimensionOrder.TDHWC);
			final long exampleSize = DLUtils.Shapes.getSize(DLUtils.Shapes.getFixedShape(spec.getShape()).get());
			final DLPythonLongBuffer buff = new DLPythonLongBuffer(exampleSize);
			for (int j = 0; j < buff.getCapacity(); j++) {
				final long val = m_rng.nextLong() / 5;
				buff.put(val);
			}
			layerData.add(new DLDefaultTensor<>(spec, buff, exampleSize));
		}

		final HashMap<DLTensorId, DLTensor<? extends DLWritableBuffer>> networkInput = new HashMap<>();
		for (final DLTensor<? extends DLWritableBuffer> input : layerData) {
			networkInput.put(input.getSpec().getIdentifier(), input);
		}

		m_commands.setNetworkInputs(HANDLE, networkInput);
		final String code = DLUtils.Files.readAllUTF8(
				DLUtils.Files.getFileFromBundle(BUNDLE_ID, "py/DLPythonDataBuffers1To1ExecutionTest_testLong.py"));
		m_commands.getContext().executeInKernel(code);

		final HashMap<DLTensorId, DLTensor<? extends DLReadableBuffer>> outputTensorSpecs = new HashMap<>();
		for (final String outputTensorName : REQUESTED_OUT_TENSORS) {
			final DLDefaultTensorSpec spec = new DLDefaultTensorSpec(OUT_TENSOR_ID, outputTensorName, OUT_TENSOR_SHAPE,
					long.class, DLDefaultDimensionOrder.TDHWC);
			final long exampleSize = DLUtils.Shapes.getSize(DLUtils.Shapes.getFixedShape(spec.getShape()).get());
			outputTensorSpecs.put(spec.getIdentifier(),
					new DLDefaultTensor<>(spec, new DLPythonLongBuffer(exampleSize), exampleSize));
		}
		m_commands.getNetworkOutputs(HANDLE, outputTensorSpecs);

		final DLTensor<?> input = networkInput.values().iterator().next();
		final DLTensor<?> output = outputTensorSpecs.values().iterator().next();
		Assert.assertArrayEquals(DLUtils.Shapes.getFixedShape(input.getSpec().getShape()).get(),
				DLUtils.Shapes.getFixedShape(output.getSpec().getShape()).get());
		Assert.assertEquals(input.getSpec().getElementType(), output.getSpec().getElementType());
		final DLReadableLongBuffer inputData = (DLReadableLongBuffer) input.getBuffer();
		final DLReadableLongBuffer outputData = (DLReadableLongBuffer) output.getBuffer();

		for (int i = 0; i < inputData.size(); i++) {
			Assert.assertEquals(inputData.readNextLong() * 5, outputData.readNextLong());
		}
	}
}
