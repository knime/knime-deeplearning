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
 *   Jun 28, 2017 (marcel): created
 */
package org.knime.dl.python.testing;

import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.dl.core.DLAbstractNetworkSpec;
import org.knime.dl.core.DLCanceledExecutionException;
import org.knime.dl.core.DLDefaultFixedTensorShape;
import org.knime.dl.core.DLDefaultTensor;
import org.knime.dl.core.DLDefaultTensorId;
import org.knime.dl.core.DLDefaultTensorSpec;
import org.knime.dl.core.DLNetworkSpec;
import org.knime.dl.core.DLTensor;
import org.knime.dl.core.DLTensorFactory;
import org.knime.dl.core.DLTensorId;
import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.core.data.DLBuffer;
import org.knime.dl.core.data.DLReadableBuffer;
import org.knime.dl.core.data.DLReadableDoubleBuffer;
import org.knime.dl.core.data.DLReadableFloatBuffer;
import org.knime.dl.core.data.DLReadableIntBuffer;
import org.knime.dl.core.data.DLReadableLongBuffer;
import org.knime.dl.core.data.DLWritableBuffer;
import org.knime.dl.core.data.DLWritableDoubleBuffer;
import org.knime.dl.core.data.DLWritableFloatBuffer;
import org.knime.dl.core.data.DLWritableIntBuffer;
import org.knime.dl.core.data.DLWritableLongBuffer;
import org.knime.dl.core.data.convert.DLAbstractTensorDataValueToTensorConverter;
import org.knime.dl.core.data.convert.DLAbstractTensorDataValueToTensorConverterFactory;
import org.knime.dl.core.data.convert.DLDataValueToTensorConverter;
import org.knime.dl.core.data.convert.DLDataValueToTensorConverterFactory;
import org.knime.dl.core.data.convert.DLDataValueToTensorConverterRegistry;
import org.knime.dl.core.data.convert.DLTensorToDataCellConverter;
import org.knime.dl.core.data.convert.DLTensorToDataCellConverterFactory;
import org.knime.dl.core.data.convert.DLTensorToDataCellConverterRegistry;
import org.knime.dl.core.execution.DLAbstractNetworkExecutionSession;
import org.knime.dl.core.execution.DLExecutionContext;
import org.knime.dl.core.execution.DLExecutionMonitor;
import org.knime.dl.core.DLInvalidNetworkInputException;
import org.knime.dl.core.DLInvalidNetworkOutputException;
import org.knime.dl.core.execution.DLNetworkExecutionSession;
import org.knime.dl.core.DLNetworkInputPreparer;
import org.knime.dl.core.execution.DLNetworkOutputConsumer;
import org.knime.dl.core.training.DLTrainingConfig;
import org.knime.dl.python.core.DLPythonNetwork;
import org.knime.dl.python.core.data.DLPythonDoubleBuffer;
import org.knime.dl.python.core.data.DLPythonFloatBuffer;
import org.knime.dl.python.core.data.DLPythonIntBuffer;
import org.knime.dl.python.core.data.DLPythonLongBuffer;
import org.knime.dl.testing.DLTestExecutionMonitor;
import org.knime.dl.util.DLUtils;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public class DLPythonConverterTest {

	@Test
	public void testFooToBar() throws Exception {
		// register converters:
		DLDataValueToTensorConverterRegistry.getInstance()
				.registerConverter(new DLFooDataValueToFloatTensorConverterFactory());
		DLTensorToDataCellConverterRegistry.getInstance()
				.registerConverter(new DLDoubleBufferToBarDataCellConverterFactory());

		// network:

		final DLTensorSpec[] inputSpecs = new DLTensorSpec[1];
		inputSpecs[0] = new DLDefaultTensorSpec(new DLDefaultTensorId("in0"), "in0",
				new DLDefaultFixedTensorShape(new long[] { 10, 10 }), float.class);
		final DLTensorSpec[] intermediateOutputSpecs = new DLTensorSpec[0];
		// intermediate outputs stay empty
		final DLTensorSpec[] outputSpecs = new DLTensorSpec[1];
		outputSpecs[0] = new DLDefaultTensorSpec(new DLDefaultTensorId("out0"), "out0",
				new DLDefaultFixedTensorShape(new long[] { 10, 10 }), double.class);
		final DLBazNetworkSpec networkSpec = new DLBazNetworkSpec(inputSpecs, intermediateOutputSpecs, outputSpecs);
		final DLBazNetwork network = new DLBazNetwork(networkSpec, null);

		// input data:

		final Random rng = new Random(543653);
		final HashMap<String, DataCell[]> inputCells = new HashMap<>(network.getSpec().getInputSpecs().length);
		final FooDataCell[] input0Cells = new FooDataCell[1];
		for (int i = 0; i < input0Cells.length; i++) {
			final float[] arr = new float[10 * 10];
			for (int j = 0; j < arr.length; j++) {
				arr[j] = rng.nextFloat() * rng.nextInt(Short.MAX_VALUE);
			}
			input0Cells[i] = new FooDataCell(arr);
		}
		inputCells.put(network.getSpec().getInputSpecs()[0].getName(), input0Cells);

		// "configure":

		final DLBazExecutionContext exec = new DLBazExecutionContext();

		// input converters:
		final LinkedHashSet<DLTensorSpec> executionInputSpecs = new LinkedHashSet<>();
		final HashMap<DLTensorId, DLDataValueToTensorConverterFactory<?, ?>> inputConverters = new HashMap<>(
				networkSpec.getInputSpecs().length);
		for (final DLTensorSpec inputSpec : networkSpec.getInputSpecs()) {
			executionInputSpecs.add(inputSpec);
			final DLDataValueToTensorConverterFactory<?, ?> converter = DLDataValueToTensorConverterRegistry
					.getInstance().getPreferredConverterFactory(FooDataCell.TYPE,
							exec.getTensorFactory().getWritableBufferType(inputSpec))
					.get();
			inputConverters.put(inputSpec.getIdentifier(), converter);
		}
		// output converters:
		final Map<DLTensorId, DLTensorToDataCellConverterFactory<?, ?>> outputConverters = new HashMap<>(
				networkSpec.getOutputSpecs().length + networkSpec.getHiddenOutputSpecs().length);
		for (final DLTensorSpec outputSpec : networkSpec.getOutputSpecs()) {
			final DLTensorToDataCellConverterFactory<?, ?> converter = DLTensorToDataCellConverterRegistry.getInstance()
					.getFactoriesForSourceType(exec.getTensorFactory().getReadableBufferType(outputSpec), outputSpec)
					.stream().filter(c -> {
						return c.getDestType().getCellClass() == BarDataCell.class;
					}).findFirst().get();
			outputConverters.put(outputSpec.getIdentifier(), converter);
		}
		for (final DLTensorSpec outputSpec : networkSpec.getHiddenOutputSpecs()) {
			final DLTensorToDataCellConverterFactory<?, ?> converter = DLTensorToDataCellConverterRegistry.getInstance()
					.getFactoriesForSourceType(exec.getTensorFactory().getReadableBufferType(outputSpec), outputSpec)
					.stream().filter(c -> c.getDestType().equals(BarDataCell.class)).findFirst().get();
			outputConverters.put(outputSpec.getIdentifier(), converter);
		}

		// "execute":

		// assign inputs to 'network input ports'/specs:
		final Map<DLTensorId, Iterable<DataValue>> inputs = new HashMap<>(inputConverters.size());
		for (final Entry<String, DataCell[]> input : inputCells.entrySet()) {
			final Optional<DLTensorSpec> inputSpec = Arrays.stream(network.getSpec().getInputSpecs())
					.filter(i -> i.getName().equals(input.getKey())).findFirst();
			final List<DataValue> val = Arrays.asList(input.getValue());
			inputs.put(inputSpec.get().getIdentifier(), val);
		}

		// pre-allocate output map
		final HashMap<DLTensorId, DataCell[]> outputs = new HashMap<>(outputConverters.size());

		try (final DLNetworkExecutionSession session = exec.createExecutionSession(network, executionInputSpecs,
				outputConverters.keySet(), new DLNetworkInputPreparer() {

					@Override
					public long getNumBatches() {
						return 1;
					}

					@Override
					public void prepare(Map<DLTensorId, DLTensor<? extends DLWritableBuffer>> input, long batchIndex)
							throws DLCanceledExecutionException, DLInvalidNetworkInputException {
						for (Entry<DLTensorId, DLTensor<? extends DLWritableBuffer>> entry : input.entrySet()) {
							final DLTensorId identifier = entry.getKey();
							final DLTensor<? extends DLWritableBuffer> tensor = entry.getValue();
							final DLDataValueToTensorConverter converter = inputConverters.get(identifier)
									.createConverter();
							converter.convert(inputs.get(identifier), tensor);
						}
					}

					@Override
					public void close() throws Exception {
						// no op
					}
				}, new DLNetworkOutputConsumer() {

					@Override
					public void accept(final Map<DLTensorId, DLTensor<? extends DLReadableBuffer>> output)
							throws DLCanceledExecutionException, DLInvalidNetworkOutputException {
						for (final Entry<DLTensorId, DLTensor<? extends DLReadableBuffer>> entry : output.entrySet()) {
							DLTensorId identifier = entry.getKey();
							DLTensor<? extends DLReadableBuffer> tensor = entry.getValue();
							DLTensorToDataCellConverter converter = outputConverters.get(identifier).createConverter();
							DataCell[] dataCells = outputs.computeIfAbsent(entry.getKey(), k -> new DataCell[1]);
							converter.convert(tensor, dataCells, null);
						}

						// check if conversion succeeded
						for (final Entry<DLTensorId, DLTensorToDataCellConverterFactory<?, ?>> outputSpecPair : outputConverters
								.entrySet()) {
							final Iterable<DataValue> inputsForSpec = inputs.get(networkSpec.getInputSpecs()[0]);
							final DataCell[] outputsForSpec = outputs.get(outputSpecPair.getKey());
							int i = 0;
							for (final DataValue input : inputsForSpec) {
								final DataCell o = outputsForSpec[i++];
								final float[] in = ((FooDataCell) input).getFloatArray();
								Assert.assertTrue(o instanceof BarDataCell);
								final double[] out = ((BarDataCell) o).getDoubleArray();
								for (int j = 0; j < out.length; j++) {
									Assert.assertEquals(out[j], in[j] * 5.0, 0.0);
								}
							}
						}
					}

					@Override
					public void close() throws Exception {
						// no op
					}
				})) {
			session.run(new DLTestExecutionMonitor());
		}
	}

	static class DLBazNetwork implements DLPythonNetwork {

		private final URL m_source;

		private final DLBazNetworkSpec m_spec;

		private DLBazNetwork(final DLBazNetworkSpec spec, final URL source) {
			m_source = source;
			m_spec = spec;
		}

		@Override
		public URL getSource() {
			return m_source;
		}

		@Override
		public DLBazNetworkSpec getSpec() {
			return m_spec;
		}
	}

	static class DLBazNetworkSpec extends DLAbstractNetworkSpec<DLTrainingConfig> {

		private static final long serialVersionUID = 1L;

		public DLBazNetworkSpec(final DLTensorSpec[] inputSpecs, final DLTensorSpec[] intermediateOutputSpecs,
				final DLTensorSpec[] outputSpecs) {
			super(inputSpecs, intermediateOutputSpecs, outputSpecs);
		}

		@Override
		protected void hashCodeInternal(final HashCodeBuilder b) {
			// no op
		}

		@Override
		protected boolean equalsInternal(final DLNetworkSpec other) {
			// no op
			return true;
		}
	}

	static class DLBazExecutionContext implements DLExecutionContext<DLBazNetwork> {

		private final DLTensorFactory m_tensorFactory = new DLBazTensorFactory();

		@Override
		public Class<DLBazNetwork> getNetworkType() {
			return DLBazNetwork.class;
		}

		@Override
		public String getName() {
			return "Baz";
		}

		@Override
		public DLTensorFactory getTensorFactory() {
			return m_tensorFactory;
		}

		@Override
		public DLBazNetworkExecutionSession createExecutionSession(final DLBazNetwork network,
				final Set<DLTensorSpec> executionInputSpecs, final Set<DLTensorId> requestedOutputs,
				final DLNetworkInputPreparer inputPreparer, final DLNetworkOutputConsumer outputConsumer)
				throws RuntimeException {
			return new DLBazNetworkExecutionSession(network, executionInputSpecs, requestedOutputs, inputPreparer,
					outputConsumer, m_tensorFactory);
		}
	}

	static class DLBazNetworkExecutionSession extends DLAbstractNetworkExecutionSession<DLBazNetwork> {

		public DLBazNetworkExecutionSession(final DLBazNetwork network, final Set<DLTensorSpec> executionInputSpecs,
				final Set<DLTensorId> requestedOutputs, final DLNetworkInputPreparer inputPreparer,
				final DLNetworkOutputConsumer outputConsumer, final DLTensorFactory tensorFactory) {
			super(network, executionInputSpecs, requestedOutputs, inputPreparer, outputConsumer, tensorFactory);
		}

		@Override
		protected void executeInternal(final DLExecutionMonitor monitor)
				throws DLCanceledExecutionException, Exception {
			// we fake some network activity here: unwrap floats, calc some stuff, create doubles...
			for (long i = 0; i < m_inputPreparer.getNumBatches(); i++) {
				m_inputPreparer.prepare(m_input, i);
				for (final Entry<DLTensorId, DLTensor<? extends DLWritableBuffer>> in : m_input.entrySet()) {
					// TODO: we can't be sure that casting will work here
					final DLPythonFloatBuffer buffer = (DLPythonFloatBuffer) in.getValue().getBuffer();
					final float[] inArr = buffer.getStorageForReading(0, buffer.size());
					final double[] outArr = new double[inArr.length];
					for (int j = 0; j < inArr.length; j++) {
						outArr[j] = inArr[j] * 5.0;
					}
					if (m_output == null) {
						m_output = new HashMap<>(m_requestedOutputs.size());
						final DLTensorSpec[] outputSpecs = ArrayUtils.addAll(m_network.getSpec().getOutputSpecs(),
								m_network.getSpec().getHiddenOutputSpecs());
						for (final DLTensorSpec spec : outputSpecs) {
							if (m_requestedOutputs.contains(spec.getIdentifier())) {
								final long batchSize = 1;
								final long[] shape = DLUtils.Shapes.getFixedShape(spec.getShape())
										.orElseThrow(RuntimeException::new);
								final DLTensorSpec executionSpec = m_tensorFactory.createExecutionTensorSpec(spec,
										batchSize, shape);
								m_output.put(spec.getIdentifier(), m_tensorFactory.createReadableTensor(executionSpec));
							}
						}
					}
					final DLTensorId outId = m_output.keySet().stream().findFirst().get();
					((DLWritableDoubleBuffer) m_output.get(outId).getBuffer()).putAll(outArr);
				}
			}
		}

		@Override
		public void close() throws Exception {
			super.close();
			// no-op
		}
	}

	static class DLBazTensorFactory implements DLTensorFactory {

		@Override
		public Class<?> getNetworkType() {
			return DLBazNetwork.class;
		}

		@Override
		public Class<? extends DLWritableBuffer> getWritableBufferType(final DLTensorSpec spec) {
			final Class<?> t = spec.getElementType();
			if (t.equals(double.class)) {
				return DLWritableDoubleBuffer.class;
			} else if (t.equals(float.class)) {
				return DLWritableFloatBuffer.class;
			} else if (t.equals(int.class)) {
				return DLWritableIntBuffer.class;
			} else if (t.equals(long.class)) {
				return DLWritableLongBuffer.class;
			} else {
				throw new IllegalArgumentException("No matching buffer type.");
			}
		}

		@Override
		public Class<? extends DLReadableBuffer> getReadableBufferType(final DLTensorSpec spec) {
			final Class<?> t = spec.getElementType();
			if (t.equals(double.class)) {
				return DLReadableDoubleBuffer.class;
			} else if (t.equals(float.class)) {
				return DLReadableFloatBuffer.class;
			} else if (t.equals(int.class)) {
				return DLReadableIntBuffer.class;
			} else if (t.equals(long.class)) {
				return DLReadableLongBuffer.class;
			} else {
				throw new IllegalArgumentException("No matching buffer type.");
			}
		}

		@Override
		public DLTensorSpec createExecutionTensorSpec(final DLTensorSpec spec, final long batchSize,
				final long[] shape) {
			throw new RuntimeException("not yet implemented");
		}

		@Override
		public DLTensor<? extends DLWritableBuffer> createWritableTensor(final DLTensorSpec spec) {
			return createTensorInternal(spec);
		}

		@Override
		public DLTensor<? extends DLReadableBuffer> createReadableTensor(final DLTensorSpec spec) {
			return createTensorInternal(spec);
		}

		private <B extends DLBuffer> DLTensor<B> createTensorInternal(final DLTensorSpec spec) {
			final long[] shape = DLUtils.Shapes.getFixedShape(spec.getShape())
					.orElseThrow(() -> new IllegalArgumentException("Tensor spec '" + spec.getName()
							+ "' does not provide a shape. Tensor cannot be created."));
			if (!spec.getBatchSize().isPresent()) {
				throw new IllegalArgumentException("Tensor spec '" + spec.getName()
						+ "' does not provide a batch size. Tensor cannot be created.");
			}
			final long batchSize = spec.getBatchSize().getAsLong();
			final Class<?> t = spec.getElementType();
			final long size = DLUtils.Shapes.getSize(shape) * batchSize;
			// TODO: handle unsafe casts
			final Supplier<B> s;
			if (t.equals(double.class)) {
				s = () -> (B) new DLPythonDoubleBuffer(size);
			} else if (t.equals(float.class)) {
				s = () -> (B) new DLPythonFloatBuffer(size);
			} else if (t.equals(int.class)) {
				s = () -> (B) new DLPythonIntBuffer(size);
			} else if (t.equals(long.class)) {
				s = () -> (B) new DLPythonLongBuffer(size);
			} else {
				throw new IllegalArgumentException("No matching tensor type for tensor spec '" + spec.getName() + "'.");
			}
			return new DLDefaultTensor<>(spec, s.get());
		}
	}

	static class DLFooDataValueToFloatTensorConverterFactory
			extends DLAbstractTensorDataValueToTensorConverterFactory<FooDataValue, DLWritableFloatBuffer> {

		@Override
		public String getName() {
			return "From FooDataValue";
		}

		@Override
		public Class<FooDataValue> getSourceType() {
			return FooDataValue.class;
		}

		@Override
		public Class<DLWritableFloatBuffer> getBufferType() {
			return DLWritableFloatBuffer.class;
		}

		@Override
		public OptionalLong getDestCount(final List<DataColumnSpec> spec) {
			return OptionalLong.empty();
		}

		@Override
		public DLDataValueToTensorConverter<FooDataValue, DLWritableFloatBuffer> createConverter() {

			return new DLAbstractTensorDataValueToTensorConverter<DLPythonConverterTest.FooDataValue, DLWritableFloatBuffer>() {

				@Override
				protected void convertInternal(final FooDataValue element,
						final DLTensor<DLWritableFloatBuffer> output) {
					final DLWritableFloatBuffer buf = output.getBuffer();
					buf.putAll(element.getFloatArray());
				}
			};
		}

		@Override
		protected long[] getDataShapeInternal(FooDataValue input) {
			return new long[] { input.getFloatArray().length };
		}

	}

	static class DLDoubleBufferToBarDataCellConverterFactory
			implements DLTensorToDataCellConverterFactory<DLReadableDoubleBuffer, BarDataCell> {

		private static final OptionalLong DEST_COUNT = OptionalLong.of(1);

		@Override
		public String getName() {
			return "To BarDataCell";
		}

		@Override
		public Class<DLReadableDoubleBuffer> getBufferType() {
			return DLReadableDoubleBuffer.class;
		}

		@Override
		public DataType getDestType() {
			return DataType.getType(BarDataCell.class);
		}

		@Override
		public DLTensorToDataCellConverter<DLReadableDoubleBuffer, BarDataCell> createConverter() {
			return (input, out, exec) -> {
				final DLReadableDoubleBuffer buf = input.getBuffer();
				out[0] = new BarDataCell(buf.toDoubleArray());
			};
		}

		@Override
		public OptionalLong getDestCount(final DLTensorSpec spec) {
			return DEST_COUNT;
		}
	}

	static class FooDataCell extends DataCell implements FooDataValue {

		private static final long serialVersionUID = 1L;

		private static final DataType TYPE = DataType.getType(FooDataCell.class);

		private final float[] m_floats;

		private FooDataCell(final float[] floats) {
			m_floats = floats;
		}

		@Override
		public float[] getFloatArray() {
			return m_floats;
		}

		@Override
		public String toString() {
			return Arrays.toString(m_floats);
		}

		@Override
		protected boolean equalsDataCell(final DataCell dc) {
			return Arrays.equals(m_floats, ((FooDataCell) dc).m_floats);
		}

		@Override
		public int hashCode() {
			return m_floats.hashCode();
		}
	}

	static interface FooDataValue extends DataValue {
		float[] getFloatArray();
	}

	static class BarDataCell extends DataCell {

		private static final long serialVersionUID = 1L;

		private static final DataType TYPE = DataType.getType(BarDataCell.class);

		private final double[] m_doubles;

		private BarDataCell(final double[] doubles) {
			m_doubles = doubles;
		}

		private double[] getDoubleArray() {
			return m_doubles;
		}

		@Override
		public String toString() {
			return Arrays.toString(m_doubles);
		}

		@Override
		protected boolean equalsDataCell(final DataCell dc) {
			return Arrays.equals(m_doubles, ((BarDataCell) dc).m_doubles);
		}

		@Override
		public int hashCode() {
			return m_doubles.hashCode();
		}
	}
}
