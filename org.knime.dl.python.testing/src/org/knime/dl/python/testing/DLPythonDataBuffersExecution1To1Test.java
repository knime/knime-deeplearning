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
 * Jun 21, 2017 (marcel): created
 */
package org.knime.dl.python.testing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.knime.dl.core.DLDefaultFixedLayerDataShape;
import org.knime.dl.core.DLDefaultLayerData;
import org.knime.dl.core.DLDefaultLayerDataSpec;
import org.knime.dl.core.DLLayerData;
import org.knime.dl.core.DLLayerDataShape;
import org.knime.dl.core.DLLayerDataSpec;
import org.knime.dl.core.data.DLReadableBuffer;
import org.knime.dl.core.data.DLReadableDoubleBuffer;
import org.knime.dl.core.data.DLReadableFloatBuffer;
import org.knime.dl.core.data.DLReadableIntBuffer;
import org.knime.dl.core.data.DLReadableLongBuffer;
import org.knime.dl.core.data.DLWritableBuffer;
import org.knime.dl.core.execution.DLDefaultLayerDataBatch;
import org.knime.dl.core.execution.DLLayerDataBatch;
import org.knime.dl.python.core.data.DLPythonDoubleBuffer;
import org.knime.dl.python.core.data.DLPythonFloatBuffer;
import org.knime.dl.python.core.data.DLPythonIntBuffer;
import org.knime.dl.python.core.data.DLPythonLongBuffer;
import org.knime.dl.python.core.kernel.DLPythonCommands;
import org.knime.dl.util.DLUtils;

/**
 * @author Marcel Wiedenmann,KNIME,Konstanz,Germany
 * @author Christian Dietz,KNIME,Konstanz,Germany
 */
public class DLPythonDataBuffersExecution1To1Test {

	private static final int IN_LAYER_DATA_NUM = 1;

	private static final String IN_LAYER_DATA_NAME = "test_in_data";

	private static final DLLayerDataShape IN_LAYER_DATA_SHAPE = new DLDefaultFixedLayerDataShape(new long[] { 11, 10 });

	private static final String OUT_LAYER_DATA_NAME = "test_out_data";

	private static final DLLayerDataShape OUT_LAYER_DATA_SHAPE =
			new DLDefaultFixedLayerDataShape(new long[] { 11, 10 });

	private static final String[] OUT_LAYER_DATA_SELECTED = { OUT_LAYER_DATA_NAME };

	private static final String BUNDLE_ID = "org.knime.dl.python.testing";

	private DLPythonCommands m_commands;

	private Random m_rng;

	@Before
	public void setUp() throws IOException {
		m_commands = new DLPythonCommands();
		m_rng = new Random(543677);
	}

	@After
	public void tearDown() throws Exception {
		m_commands.close();
	}

	@Test
	public void testDouble() throws IOException {
		final ArrayList<DLLayerData<? extends DLWritableBuffer>> layerData = new ArrayList<>(IN_LAYER_DATA_NUM);
		for (int i = 0; i < IN_LAYER_DATA_NUM; i++) {
			final DLLayerDataSpec spec =
					new DLDefaultLayerDataSpec(IN_LAYER_DATA_NAME, IN_LAYER_DATA_SHAPE, double.class) {
					};
			final DLPythonDoubleBuffer buff = new DLPythonDoubleBuffer(
					DLUtils.Shapes.getSize(DLUtils.Shapes.getFixedShape(spec.getShape()).get()));
			for (int j = 0; j < buff.getCapacity(); j++) {
				final double val = m_rng.nextDouble() * m_rng.nextInt(Integer.MAX_VALUE / 5);
				buff.put(val);
			}
			layerData.add(new DLDefaultLayerData<>(spec, buff));
		}

		final HashMap<DLLayerDataSpec, DLLayerDataBatch<? extends DLWritableBuffer>> networkInput = new HashMap<>();
		for (final DLLayerData<? extends DLWritableBuffer> input : layerData) {
			// TODO: typing (also in the other test* methods)
			networkInput.put(input.getSpec(), new DLDefaultLayerDataBatch(new DLLayerData<?>[] { input }));
		}

		m_commands.setNetworkInputs(networkInput, 1);
		final String code = DLUtils.Files.readAllUTF8(
				DLUtils.Files.getFileFromBundle(BUNDLE_ID, "py/DLPythonDataBuffers1To1ExecutionTest_testDouble.py"));
		m_commands.getKernel().execute(code);

		final HashMap<DLLayerDataSpec, DLLayerDataBatch<? extends DLReadableBuffer>> outputLayerDataSpecs =
				new HashMap<>();
		for (final String outputLayerDataName : OUT_LAYER_DATA_SELECTED) {
			final DLDefaultLayerDataSpec spec =
					new DLDefaultLayerDataSpec(outputLayerDataName, OUT_LAYER_DATA_SHAPE, double.class);
			// TODO: typing (also in the other test* methods)
			outputLayerDataSpecs.put(spec,
					new DLDefaultLayerDataBatch(
							new DLLayerData[] { new DLDefaultLayerData<>(spec, new DLPythonDoubleBuffer(
									DLUtils.Shapes.getSize(DLUtils.Shapes.getFixedShape(spec.getShape()).get()))) }));
		}
		m_commands.getNetworkOutputs(outputLayerDataSpecs);

		final DLLayerData<?> input = networkInput.values().iterator().next().getBatch()[0];
		final DLLayerData<?> output = outputLayerDataSpecs.values().iterator().next().getBatch()[0];
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
	public void testFloat() throws IOException {
		final ArrayList<DLLayerData<?>> layerData = new ArrayList<>(IN_LAYER_DATA_NUM);
		for (int i = 0; i < IN_LAYER_DATA_NUM; i++) {
			final DLLayerDataSpec spec =
					new DLDefaultLayerDataSpec(IN_LAYER_DATA_NAME, IN_LAYER_DATA_SHAPE, float.class) {
					};
			final DLPythonFloatBuffer buff = new DLPythonFloatBuffer(
					DLUtils.Shapes.getSize(DLUtils.Shapes.getFixedShape(spec.getShape()).get()));
			for (int j = 0; j < buff.getCapacity(); j++) {
				final float val = m_rng.nextFloat() * m_rng.nextInt(Short.MAX_VALUE / 5);
				buff.put(val);
			}
			layerData.add(new DLDefaultLayerData<>(spec, buff));
		}

		final HashMap<DLLayerDataSpec, DLLayerDataBatch<? extends DLWritableBuffer>> networkInput = new HashMap<>();
		for (final DLLayerData<?> input : layerData) {
			networkInput.put(input.getSpec(), new DLDefaultLayerDataBatch(new DLLayerData<?>[] { input }));
		}

		m_commands.setNetworkInputs(networkInput, 1);
		final String code = DLUtils.Files.readAllUTF8(
				DLUtils.Files.getFileFromBundle(BUNDLE_ID, "py/DLPythonDataBuffers1To1ExecutionTest_testFloat.py"));
		m_commands.getKernel().execute(code);

		final HashMap<DLLayerDataSpec, DLLayerDataBatch<? extends DLReadableBuffer>> outputLayerDataSpecs =
				new HashMap<>();
		for (final String outputLayerDataName : OUT_LAYER_DATA_SELECTED) {
			final DLDefaultLayerDataSpec spec =
					new DLDefaultLayerDataSpec(outputLayerDataName, OUT_LAYER_DATA_SHAPE, float.class);
			outputLayerDataSpecs.put(spec,
					new DLDefaultLayerDataBatch(
							new DLLayerData[] { new DLDefaultLayerData<>(spec, new DLPythonFloatBuffer(
									DLUtils.Shapes.getSize(DLUtils.Shapes.getFixedShape(spec.getShape()).get()))) }));
		}
		m_commands.getNetworkOutputs(outputLayerDataSpecs);

		final DLLayerData<?> input = networkInput.values().iterator().next().getBatch()[0];
		final DLLayerData<?> output = outputLayerDataSpecs.values().iterator().next().getBatch()[0];
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
	public void testInt() throws IOException {
		final ArrayList<DLLayerData<?>> layerData = new ArrayList<>(IN_LAYER_DATA_NUM);
		for (int i = 0; i < IN_LAYER_DATA_NUM; i++) {
			final DLLayerDataSpec spec =
					new DLDefaultLayerDataSpec(IN_LAYER_DATA_NAME, IN_LAYER_DATA_SHAPE, int.class) {
					};
			final DLPythonIntBuffer buff =
					new DLPythonIntBuffer(DLUtils.Shapes.getSize(DLUtils.Shapes.getFixedShape(spec.getShape()).get()));
			for (int j = 0; j < buff.getCapacity(); j++) {
				final int val = m_rng.nextInt(Integer.MAX_VALUE / 5);
				buff.put(val);
			}
			layerData.add(new DLDefaultLayerData<>(spec, buff));
		}

		final HashMap<DLLayerDataSpec, DLLayerDataBatch<? extends DLWritableBuffer>> networkInput = new HashMap<>();
		for (final DLLayerData<?> input : layerData) {
			networkInput.put(input.getSpec(), new DLDefaultLayerDataBatch(new DLLayerData<?>[] { input }));
		}

		m_commands.setNetworkInputs(networkInput, 1);
		final String code = DLUtils.Files.readAllUTF8(
				DLUtils.Files.getFileFromBundle(BUNDLE_ID, "py/DLPythonDataBuffers1To1ExecutionTest_testInt.py"));
		m_commands.getKernel().execute(code);

		final HashMap<DLLayerDataSpec, DLLayerDataBatch<? extends DLReadableBuffer>> outputLayerDataSpecs =
				new HashMap<>();
		for (final String outputLayerDataName : OUT_LAYER_DATA_SELECTED) {
			final DLDefaultLayerDataSpec spec =
					new DLDefaultLayerDataSpec(outputLayerDataName, OUT_LAYER_DATA_SHAPE, int.class);
			outputLayerDataSpecs.put(spec,
					new DLDefaultLayerDataBatch(
							new DLLayerData[] { new DLDefaultLayerData<>(spec, new DLPythonIntBuffer(
									DLUtils.Shapes.getSize(DLUtils.Shapes.getFixedShape(spec.getShape()).get()))) }));
		}
		m_commands.getNetworkOutputs(outputLayerDataSpecs);

		final DLLayerData<?> input = networkInput.values().iterator().next().getBatch()[0];
		final DLLayerData<?> output = outputLayerDataSpecs.values().iterator().next().getBatch()[0];
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
	public void testLong() throws IOException {
		final ArrayList<DLLayerData<?>> layerData = new ArrayList<>(IN_LAYER_DATA_NUM);
		for (int i = 0; i < IN_LAYER_DATA_NUM; i++) {
			final DLLayerDataSpec spec =
					new DLDefaultLayerDataSpec(IN_LAYER_DATA_NAME, IN_LAYER_DATA_SHAPE, long.class) {
					};
			final DLPythonLongBuffer buff =
					new DLPythonLongBuffer(DLUtils.Shapes.getSize(DLUtils.Shapes.getFixedShape(spec.getShape()).get()));
			for (int j = 0; j < buff.getCapacity(); j++) {
				final long val = m_rng.nextLong() / 5;
				buff.put(val);
			}
			layerData.add(new DLDefaultLayerData<>(spec, buff));
		}

		final HashMap<DLLayerDataSpec, DLLayerDataBatch<? extends DLWritableBuffer>> networkInput = new HashMap<>();
		for (final DLLayerData<?> input : layerData) {
			networkInput.put(input.getSpec(), new DLDefaultLayerDataBatch(new DLLayerData<?>[] { input }));
		}

		m_commands.setNetworkInputs(networkInput, 1);
		final String code = DLUtils.Files.readAllUTF8(
				DLUtils.Files.getFileFromBundle(BUNDLE_ID, "py/DLPythonDataBuffers1To1ExecutionTest_testLong.py"));
		m_commands.getKernel().execute(code);

		final HashMap<DLLayerDataSpec, DLLayerDataBatch<? extends DLReadableBuffer>> outputLayerDataSpecs =
				new HashMap<>();
		for (final String outputLayerDataName : OUT_LAYER_DATA_SELECTED) {
			final DLDefaultLayerDataSpec spec =
					new DLDefaultLayerDataSpec(outputLayerDataName, OUT_LAYER_DATA_SHAPE, long.class);
			outputLayerDataSpecs.put(spec,
					new DLDefaultLayerDataBatch(
							new DLLayerData[] { new DLDefaultLayerData<>(spec, new DLPythonLongBuffer(
									DLUtils.Shapes.getSize(DLUtils.Shapes.getFixedShape(spec.getShape()).get()))) }));
		}
		m_commands.getNetworkOutputs(outputLayerDataSpecs);

		final DLLayerData<?> input = networkInput.values().iterator().next().getBatch()[0];
		final DLLayerData<?> output = outputLayerDataSpecs.values().iterator().next().getBatch()[0];
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
