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
 *   Jun 26, 2017 (marcel): created
 */
package org.knime.dl.python.core;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.knime.core.node.NodeLogger;
import org.knime.dl.core.DLInvalidContextException;
import org.knime.dl.core.DLNetworkTypeRegistry;
import org.knime.dl.core.DLTensor;
import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.core.data.DLReadableBuffer;
import org.knime.dl.core.data.DLWritableBuffer;
import org.knime.dl.python.core.data.DLPythonDataBuffer;
import org.knime.dl.python.core.data.DLPythonTypeMap;
import org.knime.dl.python.core.data.serde.DLPythonDeserializer;
import org.knime.dl.python.core.data.serde.DLPythonDeserializerFactory;
import org.knime.dl.python.core.data.serde.DLSerializerFactory;
import org.knime.dl.python.util.DLPythonSourceCodeBuilder;
import org.knime.dl.python.util.DLPythonUtils;
import org.knime.dl.util.DLUtils;
import org.knime.python.typeextension.Deserializer;
import org.knime.python.typeextension.DeserializerFactory;
import org.knime.python.typeextension.KnimeToPythonExtension;
import org.knime.python.typeextension.KnimeToPythonExtensions;
import org.knime.python.typeextension.PythonToKnimeExtensions;
import org.knime.python.typeextension.Serializer;
import org.knime.python2.extensions.serializationlibrary.interfaces.Cell;
import org.knime.python2.extensions.serializationlibrary.interfaces.Row;
import org.knime.python2.extensions.serializationlibrary.interfaces.TableCreator;
import org.knime.python2.extensions.serializationlibrary.interfaces.TableSpec;
import org.knime.python2.extensions.serializationlibrary.interfaces.Type;
import org.knime.python2.extensions.serializationlibrary.interfaces.impl.CellImpl;
import org.knime.python2.extensions.serializationlibrary.interfaces.impl.KeyValueTableIterator;
import org.knime.python2.extensions.serializationlibrary.interfaces.impl.RowImpl;
import org.knime.python2.extensions.serializationlibrary.interfaces.impl.TableSpecImpl;
import org.knime.python2.generic.ScriptingNodeUtils;

/**
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
public abstract class DLPythonAbstractCommands<CFG extends DLPythonAbstractCommandsConfig> implements AutoCloseable {

	private static final NodeLogger LOGGER = NodeLogger.getLogger(DLPythonAbstractCommands.class);

	private static final String INSTALLATION_TEST_OK_MSG = "[DL Python installation test: OK]";

	private static final String INSTALLATION_TEST_FAIL_MSG = "[DL Python installation test: FAIL]";

	protected final DLPythonContext m_context;

	protected final CFG m_config;

	/**
	 * @param config the config
	 * @throws DLInvalidContextException if failed to create a valid Python back end. This includes failures during the
	 *             setup of the Python kernel as well as the deep learning specific setup of the Python environment,
	 *             installation tests, registration of all Python deep learning back ends and the setup of the Python
	 *             back end that corresponds to this commands instance. The thrown exception contains a detailed error
	 *             message that is suitable to be displayed to the user.
	 */
	protected DLPythonAbstractCommands(final CFG config) throws DLInvalidContextException {
		this(config, new DLPythonDefaultContext());
	}

	/**
	 * @param config the config
	 * @param context the Python kernel
	 * @throws DLInvalidContextException if failed to create a valid Python back end. This includes failures during the
	 *             deep learning specific setup of the Python environment, installation tests, registration of all
	 *             Python deep learning back ends and the setup of the Python back end that corresponds to this commands
	 *             instance. The thrown exception contains a detailed error message that is suitable to be displayed to
	 *             the user.
	 */
	protected DLPythonAbstractCommands(final CFG config, final DLPythonContext context)
			throws DLInvalidContextException {
		m_context = context;
		m_config = config;
	}

	public abstract DLPythonNetworkSpec extractNetworkSpec(final DLPythonNetworkHandle handle,
			final DLPythonTypeMap typeMap) throws DLInvalidContextException, IOException;

	public DLPythonContext getContext() {
		return m_context;
	}

	public void setupEnvironment() throws DLInvalidContextException {
		try {
			final String error = m_context.getKernel().execute(m_config.getSetupEnvironmentCode())[1];
			if (!error.isEmpty()) {
				throw new DLInvalidContextException(
						"Deep learning Python back end environment could not be set up.\nCause: " + error);
			}
		} catch (final IOException e) {
			throw new DLInvalidContextException("An error occurred while communicating with Python "
					+ "(while setting up the Python back end environment)." + e.getMessage() != null
							? "\nCause: " + e.getMessage()
							: "",
					e);
		}
	}

	public void testInstallation() throws DLInvalidContextException {
		try {
			final File script = m_config.getInstallationTestScript();
			final String[] output = m_context.isKernelOpen()
					? m_context.getKernel().execute(DLUtils.Files.readAllUTF8(script))
					: m_context.execute(script);
			if (!output[0].contains(INSTALLATION_TEST_OK_MSG)) {
				final int idx = output[0].indexOf(INSTALLATION_TEST_FAIL_MSG);
				final String cause = idx != -1 //
						? "\nCause: " + output[0].substring(idx + INSTALLATION_TEST_FAIL_MSG.length())
						: "";
				final String further = !output[1].isEmpty() ? "\nFurther output: " + output[1] : "";
				if (!cause.isEmpty()) {
					throw new DLInvalidContextException(
							"Deep learning Python back end installation tests failed." + cause + further);
				} else {
					throw new DLInvalidContextException(
							"Deep learning Python back end installation tests failed for unknown reasons." + further);
				}
			}
		} catch (final IOException e) {
			throw new DLInvalidContextException("An error occurred while communicating with Python "
					+ "(while testing the installation of the Python back end)." + e.getMessage() != null
							? "\nCause: " + e.getMessage()
							: "",
					e);
		}
	}

	public void registerBackends() throws DLInvalidContextException {
		try {
			final String error = m_context.getKernel().execute(DLNetworkTypeRegistry.getInstance().getAllNetworkTypes() //
					.stream() //
					.filter(nt -> nt instanceof DLPythonNetworkType) //
					.map(nt -> "import " + ((DLPythonNetworkType<?, ?>) nt).getPythonModuleName() + "\n") //
					.collect(Collectors.joining()))[1];
			if (!error.isEmpty()) {
				throw new DLInvalidContextException(
						"Deep learning Python back ends could not be registered.\nCause: " + error);
			}
		} catch (final IOException e) {
			throw new DLInvalidContextException(
					"An error occurred while communicating with Python (while registering the Python back ends)."
							+ e.getMessage() != null ? "\nCause: " + e.getMessage() : "",
					e);
		}
	}

	public void setupBackend() throws DLInvalidContextException {
		try {
			final String error = m_context.getKernel().execute(m_config.getSetupBackendCode())[1];
			if (!error.isEmpty()) {
				throw new DLInvalidContextException(
						"Deep learning Python back end could not be set up.\nCause: " + error);
			}
		} catch (final IOException e) {
			throw new DLInvalidContextException(
					"An error occurred while communicating with Python (while setting up the Python back end)."
							+ e.getMessage() != null ? "\nCause: " + e.getMessage() : "",
					e);
		}
	}

	// TODO: we should get the model name (= handle identifier) from Python
	public DLPythonNetworkHandle loadNetwork(final String path) throws DLInvalidContextException, IOException {
		m_context.getKernel().execute(m_config.getLoadNetworkCode(path));
		return new DLPythonNetworkHandle(DLPythonAbstractCommandsConfig.DEFAULT_MODEL_NAME);
	}

	public void saveNetwork(final DLPythonNetworkHandle handle, final String path)
			throws DLInvalidContextException, IOException {
		m_context.getKernel().execute(m_config.getSaveNetworkCode(handle, path));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public synchronized void setNetworkInputs(final DLPythonNetworkHandle handle, // TODO: implement handle
			final Map<DLTensorSpec, DLTensor<? extends DLWritableBuffer>> input, final long batchSize)
			throws DLInvalidContextException, IOException {
		for (final Entry<DLTensorSpec, DLTensor<? extends DLWritableBuffer>> in : input.entrySet()) {
			final DLTensorSpec spec = in.getKey();
			final DLTensor<? extends DLWritableBuffer> tensor = in.getValue();
			final Optional<KnimeToPythonExtension> extensions = KnimeToPythonExtensions.getExtensions().stream()
					.filter(ext -> (ext.getJavaSerializerFactory() instanceof DLSerializerFactory)
							&& ((DLSerializerFactory) ext.getJavaSerializerFactory()).getBufferType()
									.isAssignableFrom(tensor.getBuffer().getClass()))
					.findFirst();
			// TODO: if nothing found, we should also try to match
			// primitive types with their wrapper types
			// (guava Primitives.wrap etc.)
			final KnimeToPythonExtension extension = extensions.orElseThrow(() -> new RuntimeException(
					"Transmitting input data to Python failed. No matching serializer available."));
			final Serializer<DLPythonDataBuffer> serializer =
					(Serializer<DLPythonDataBuffer>) extension.getJavaSerializerFactory().createSerializer();
			final TableSpec tableSpec = new TableSpecImpl(new Type[] { Type.BYTES }, new String[] { spec.getName() },
					Collections.singletonMap(spec.getName(), extension.getId()));
			final Row row = new RowImpl(spec.getName(), 1);
			try {
				final Cell cell = new CellImpl(serializer.serialize((DLPythonDataBuffer) tensor.getBuffer()));
				row.setCell(cell, 0);
				final KeyValueTableIterator iterator = new KeyValueTableIterator(tableSpec, row);
				m_context.getKernel().putData(spec.getName(), new DLSingletonTableChunker(iterator), 1);
			} catch (final IOException ex) {
				throw new RuntimeException("Transmitting input data to Python failed.", ex);
			}
		}
	}

	public void executeNetwork(final DLPythonNetworkHandle handle, final Set<DLTensorSpec> requestedOutputs,
			final long batchSize) throws DLInvalidContextException, IOException {
		final String[] output =
				m_context.getKernel().execute(m_config.getExecuteNetworkCode(handle, requestedOutputs, batchSize));
		if (!output[1].isEmpty()) {
			LOGGER.debug(ScriptingNodeUtils.shortenString(output[1], 1000));
		}
	}

	public synchronized void getNetworkOutputs(final DLPythonNetworkHandle handle, // TODO: implement handle
			final Map<DLTensorSpec, DLTensor<? extends DLReadableBuffer>> output)
			throws DLInvalidContextException, IOException {
		for (final Entry<DLTensorSpec, DLTensor<? extends DLReadableBuffer>> out : output.entrySet()) {
			final DLTensorSpec spec = out.getKey();
			final DLTensor<? extends DLReadableBuffer> tensor = out.getValue();

			m_context.getKernel().getData(spec.getName(),
					(tableSpec, tableSize) -> new TableCreator<DLTensor<? extends DLReadableBuffer>>() {

						@Override
						public void addRow(final Row row) {
							final String deserializerId = tableSpec.getColumnSerializers().get(spec.getName());
							final DeserializerFactory deserializerFactory =
									PythonToKnimeExtensions.getExtension(deserializerId).getJavaDeserializerFactory();
							if (!(deserializerFactory instanceof DLPythonDeserializerFactory)) {
								LOGGER.coding(
										"Deep learning Python to KNIME serialization factory must implement DLSerializerFactory.");
							}
							final Deserializer deserializer = deserializerFactory.createDeserializer();
							if (!(deserializer instanceof DLPythonDeserializer)) {
								final String msg =
										"An exception occurred while collecting network output from Python. Unsupported deserializer.";
								LOGGER.error(msg);
								// TODO
								throw new RuntimeException(msg);
							}
							final Cell cell = row.getCell(0);
							try {
								((DLPythonDeserializer) deserializer).deserialize(cell.getBytesValue(), tensor);
							} catch (final IllegalStateException e) {
								LOGGER.error("An exception occurred while collecting network output from Python: "
										+ e.getMessage(), e);
							}
						}

						@Override
						public TableSpec getTableSpec() {
							return tableSpec;
						}

						@Override
						public DLTensor<? extends DLReadableBuffer> getTable() {
							return tensor;
						}
					});
		}
	}

	public synchronized void setNetworkTrainingInputs(final DLPythonNetworkHandle handle,
			final Map<DLTensorSpec, DLTensor<? extends DLWritableBuffer>> trainingData,
			final Map<DLTensorSpec, DLTensor<? extends DLWritableBuffer>> targetData, final long batchSize)
			throws DLInvalidContextException, IOException {
		// training data:
		for (final Entry<DLTensorSpec, DLTensor<? extends DLWritableBuffer>> in : trainingData.entrySet()) {
			final DLTensorSpec spec = in.getKey();
			final DLTensor<? extends DLWritableBuffer> tensor = in.getValue();
			final Optional<KnimeToPythonExtension> extensions = KnimeToPythonExtensions.getExtensions().stream()
					.filter(ext -> (ext.getJavaSerializerFactory() instanceof DLSerializerFactory)
							&& ((DLSerializerFactory) ext.getJavaSerializerFactory()).getBufferType()
									.isAssignableFrom(tensor.getBuffer().getClass()))
					.findFirst();
			// TODO: if nothing found, we should also try to match
			// primitive types with their wrapper types
			// (guava Primitives.wrap etc.)
			final KnimeToPythonExtension extension = extensions.orElseThrow(() -> new RuntimeException(
					"Transmitting input data to Python failed. No matching serializer available."));
			final Serializer<DLPythonDataBuffer> serializer =
					(Serializer<DLPythonDataBuffer>) extension.getJavaSerializerFactory().createSerializer();
			final TableSpec tableSpec = new TableSpecImpl(new Type[] { Type.BYTES }, new String[] { spec.getName() },
					Collections.singletonMap(spec.getName(), extension.getId()));
			final Row row = new RowImpl(spec.getName(), 1);
			try {
				final Cell cell = new CellImpl(serializer.serialize((DLPythonDataBuffer) tensor.getBuffer()));
				row.setCell(cell, 0);
				final KeyValueTableIterator iterator = new KeyValueTableIterator(tableSpec, row);
				m_context.getKernel().putData(spec.getName(), new DLSingletonTableChunker(iterator), 1);
			} catch (final IOException ex) {
				throw new RuntimeException("Transmitting input data to Python failed.", ex);
			}
		}
		// target data:
		for (final Entry<DLTensorSpec, DLTensor<? extends DLWritableBuffer>> in : targetData.entrySet()) {
			final DLTensorSpec spec = in.getKey();
			final DLTensor<? extends DLWritableBuffer> tensor = in.getValue();
			final Optional<KnimeToPythonExtension> extensions = KnimeToPythonExtensions.getExtensions().stream()
					.filter(ext -> (ext.getJavaSerializerFactory() instanceof DLSerializerFactory)
							&& ((DLSerializerFactory) ext.getJavaSerializerFactory()).getBufferType()
									.isAssignableFrom(tensor.getBuffer().getClass()))
					.findFirst();
			// TODO: if nothing found, we should also try to match
			// primitive types with their wrapper types
			// (guava Primitives.wrap etc.)
			final KnimeToPythonExtension extension = extensions.orElseThrow(() -> new RuntimeException(
					"Transmitting input data to Python failed. No matching serializer available."));
			final Serializer<DLPythonDataBuffer> serializer =
					(Serializer<DLPythonDataBuffer>) extension.getJavaSerializerFactory().createSerializer();
			final TableSpec tableSpec = new TableSpecImpl(new Type[] { Type.BYTES }, new String[] { spec.getName() },
					Collections.singletonMap(spec.getName(), extension.getId()));
			final Row row = new RowImpl(spec.getName(), 1);
			try {
				final Cell cell = new CellImpl(serializer.serialize((DLPythonDataBuffer) tensor.getBuffer()));
				row.setCell(cell, 0);
				final KeyValueTableIterator iterator = new KeyValueTableIterator(tableSpec, row);
				m_context.getKernel().putData(spec.getName(), new DLSingletonTableChunker(iterator), 1);
			} catch (final IOException ex) {
				throw new RuntimeException("Transmitting input data to Python failed.", ex);
			}
		}
	}

	public synchronized void trainNetwork(final DLPythonNetworkHandle handle, final long batchSize)
			throws DLInvalidContextException, IOException {
		final DLPythonSourceCodeBuilder b = DLPythonUtils.createSourceCodeBuilder() //
				.a("import DLPythonNetwork") //
				.n("network = DLPythonNetwork.get_network(").as(handle.getIdentifier()).a(")") //
				.n("training_data = {}") //
				.n("for input_spec in network.spec.input_specs:") //
				.n().t().a("training_data[input_spec.name] = globals()[input_spec.name]") //
				.n("target_data = {}") //
				.n("for output_spec in network.spec.output_specs:") //
				.n().t().a("target_data[output_spec.name] = globals()[output_spec.name]") //
				.n("network.train(training_data, target_data)");
		final String[] output = m_context.getKernel().execute(b.toString());
		if (!output[1].isEmpty()) {
			LOGGER.debug(ScriptingNodeUtils.shortenString(output[1], 1000));
		}
	}

	public synchronized void getTrainingResults(final DLPythonNetworkHandle handle) {
		// TODO
	}

	public void putParameter(final String key, final String parameter) throws DLInvalidContextException, IOException {
		putParameter(key, new CellImpl(parameter));
	}

	public void putParameter(final String key, final int parameter) throws DLInvalidContextException, IOException {
		putParameter(key, new CellImpl(parameter));
	}

	public void putParameter(final String key, final long parameter) throws DLInvalidContextException, IOException {
		putParameter(key, new CellImpl(parameter));
	}

	/**
	 * Closes the underlying {@link DLPythonContext Python context}.
	 */
	@Override
	public void close() throws Exception {
		m_context.close();
	}

	private void putParameter(final String key, final Cell cell) throws DLInvalidContextException, IOException {
		final TableSpec spec =
				new TableSpecImpl(new Type[] { cell.getColumnType() }, new String[] { key }, new HashMap<>(0));
		final RowImpl row = new RowImpl(key, 1);
		row.setCell(cell, 0);
		m_context.getKernel().putData(key, new DLSingletonTableChunker(new KeyValueTableIterator(spec, row)), 1);
	}
}
