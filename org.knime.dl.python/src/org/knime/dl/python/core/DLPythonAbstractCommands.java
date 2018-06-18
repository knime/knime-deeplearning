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
 *   Jun 26, 2017 (marcel): created
 */
package org.knime.dl.python.core;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.knime.core.node.NodeLogger;
import org.knime.core.util.Version;
import org.knime.dl.core.DLCancelable;
import org.knime.dl.core.DLCanceledExecutionException;
import org.knime.dl.core.DLInvalidEnvironmentException;
import org.knime.dl.core.DLNetworkInputProvider;
import org.knime.dl.core.DLTensor;
import org.knime.dl.core.DLTensorId;
import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.core.data.DLReadableBuffer;
import org.knime.dl.core.data.DLWritableBuffer;
import org.knime.dl.core.training.DLReportedMetric;
import org.knime.dl.core.training.DLTrainingMonitor;
import org.knime.dl.core.training.DLTrainingStatus.Status;
import org.knime.dl.python.core.data.DLPythonDataBuffer;
import org.knime.dl.python.core.data.serde.DLPythonDeserializer;
import org.knime.dl.python.core.data.serde.DLPythonDeserializerFactory;
import org.knime.dl.python.core.data.serde.DLSerializerFactory;
import org.knime.dl.python.core.training.DLPythonTrainingStatus;
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
import org.knime.python2.extensions.serializationlibrary.interfaces.TableChunker;
import org.knime.python2.extensions.serializationlibrary.interfaces.TableCreator;
import org.knime.python2.extensions.serializationlibrary.interfaces.TableIterator;
import org.knime.python2.extensions.serializationlibrary.interfaces.TableSpec;
import org.knime.python2.extensions.serializationlibrary.interfaces.Type;
import org.knime.python2.extensions.serializationlibrary.interfaces.impl.CellImpl;
import org.knime.python2.extensions.serializationlibrary.interfaces.impl.RowImpl;
import org.knime.python2.extensions.serializationlibrary.interfaces.impl.TableSpecImpl;
import org.knime.python2.kernel.AbstractPythonToJavaMessageHandler;
import org.knime.python2.kernel.DefaultJavaToPythonResponse;
import org.knime.python2.kernel.Messages;
import org.knime.python2.kernel.PythonOutputListener;
import org.knime.python2.kernel.PythonToJavaMessage;
import org.knime.python2.kernel.PythonToJavaMessageHandler;

import com.google.common.collect.Sets;
import com.google.common.primitives.UnsignedBytes;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public abstract class DLPythonAbstractCommands implements DLPythonCommands {

	// String constants that are used on Python side:

    /**
     * Not the actual network name but the entry in Python's global namespace under which the current (i.e. the last
     * loaded) model's name can be found.
     */
    public static final String CURRENT_NETWORK_NAME = "current_network_name";

	public static final String INPUT_SPECS_NAME = "input_specs";

	public static final String HIDDEN_OUTPUT_SPECS_NAME = "intermediate_output_specs";

	public static final String OUTPUT_SPECS_NAME = "output_specs";

	public static final String OPTIMIZER_SPECS = "optimizer_specs";

	public static final String LOSS_SPECS = "loss_specs";

	public static final String METRICS_SPECS = "metrics_specs";

	public static final String INPUT_TABLE_NAME = "input_table";

	public static final String OUTPUT_TABLE_NAME = "output_table";

	public static final String OUTPUT_SHAPES_NAME = "output_shapes";

	/** Name of the 'python version' DataFrame in python */
	public static final String PYTHON_VERSION_NAME = "python_version";

	private static final String INSTALLATION_TEST_OK_MSG = "[DL Python installation test: OK]";

	private static final String INSTALLATION_TEST_FAIL_MSG = "[DL Python installation test: FAIL]";

	// --

	private static final NodeLogger LOGGER = NodeLogger.getLogger(DLPythonAbstractCommands.class);

	/**
	 * Methods that require a properly setup Python environment should not access this field directly. Instead, they
	 * should use {@link #getContext()}.
	 */
	protected final DLPythonContext m_context;

	private final Map<DLTensorId, DLPythonTableChunker> m_tableChunkers = new HashMap<>();

	/**
	 * Set to <code>true</code> if the setup steps in {@link #getContext()} were successful.
	 */
	private boolean m_contextSetup = false;

	/**
	 * Creates a new instance of this commands class.
	 */
	protected DLPythonAbstractCommands() {
		this(new DLPythonDefaultContext());
	}

	/**
	 * Creates a new instance of this commands class that uses the given context to communicate with Python.
	 *
	 * @param context the Python context
	 */
	protected DLPythonAbstractCommands(final DLPythonContext context) {
		m_context = context;
	}

	protected abstract String getSetupEnvironmentCode();

	protected abstract File getInstallationTestFile() throws IOException;

	protected abstract String getSetupBackendCode();

    protected abstract DLPythonAbstractNetworkReaderCommands getNetworkReaderCommands();

	@Override
	public final synchronized DLPythonContext getContext(final DLCancelable cancelable) throws DLInvalidEnvironmentException, DLCanceledExecutionException {
		if (!m_contextSetup) {
			// setup Python process environment
			try {
				final String setupGatewayCode = DLPythonUtils.createSourceCodeBuilder() //
						.a("import DLPythonKernelGateway") //
						.n("DLPythonKernelGateway._instance = ")
						/**/ .a("DLPythonKernelGateway.DLPythonKernelGateway(globals(), request_from_java)").n()
						.toString();
				final String error = m_context.executeInKernel(setupGatewayCode + getSetupEnvironmentCode(), cancelable)[1];
				if (!error.isEmpty()) {
					throw new DLInvalidEnvironmentException(
							"Deep learning Python back end environment could not be set up.\nCause: " + error);
				}
			} catch (final IOException e) {
				throw new DLInvalidEnvironmentException("An error occurred while communicating with Python "
						+ "(while setting up the Python back end environment)."
						+ (e.getMessage() != null ? "\nCause: " + e.getMessage() : ""), e);
			}
			// register all back ends
			try {
				final String error = m_context
						.executeInKernel(DLPythonNetworkLoaderRegistry.getInstance().getAllNetworkLoaders() //
								.stream() //
								.map(nl -> "import " + nl.getPythonModuleName() + "\n") //
								.collect(Collectors.joining()), cancelable)[1];
				if (!error.isEmpty()) {
					throw new DLInvalidEnvironmentException(
							"Deep learning Python back ends could not be registered.\nCause: " + error);
				}
			} catch (final IOException e) {
				throw new DLInvalidEnvironmentException(
						"An error occurred while communicating with Python (while registering the Python back ends)."
								+ (e.getMessage() != null ? "\nCause: " + e.getMessage() : ""),
						e);
			}
			// setup the actual back end
			try {
				final String error = m_context.executeInKernel(getSetupBackendCode(), cancelable)[1];
				if (!error.isEmpty()) {
					throw new DLInvalidEnvironmentException(
							"Deep learning Python back end could not be set up.\nCause: " + error);
				}
			} catch (final IOException e) {
				throw new DLInvalidEnvironmentException(
						"An error occurred while communicating with Python (while setting up the Python back end)."
								+ (e.getMessage() != null ? "\nCause: " + e.getMessage() : ""),
						e);
			}

			m_contextSetup = true;
		}
		return m_context;
	}

	@Override
	public synchronized void testInstallation(final DLCancelable cancelable) throws DLInvalidEnvironmentException, DLCanceledExecutionException {
		try {
			final File script = getInstallationTestFile();
			final String[] output = m_context.isKernelOpen()
					? m_context.executeInKernel(DLUtils.Files.readAllUTF8(script), cancelable)
					: m_context.execute(cancelable, script);
			if (!output[0].contains(INSTALLATION_TEST_OK_MSG)) {
				final int idx = output[0].indexOf(INSTALLATION_TEST_FAIL_MSG);
				final String cause = idx != -1 //
						? "\nCause: " + output[0].substring(idx + INSTALLATION_TEST_FAIL_MSG.length())
						: "";
				final String further = !output[1].isEmpty() ? "\nFurther output: " + output[1] : "";
				if (!cause.isEmpty()) {
					throw new DLInvalidEnvironmentException(
							"Deep learning Python back end installation tests failed." + cause + further);
				} else {
					throw new DLInvalidEnvironmentException(
							"Deep learning Python back end installation tests failed for unknown reasons." + further);
				}
			}
		} catch (final IOException e) {
			throw new DLInvalidEnvironmentException("An error occurred while communicating with Python "
					+ "(while testing the installation of the Python back end)."
					+ (e.getMessage() != null ? "\nCause: " + e.getMessage() : ""), e);
		}
	}

    @Override
    public DLPythonNetworkHandle loadNetwork(final String path, final boolean loadTrainingConfig, final DLCancelable cancelable)
        throws DLInvalidEnvironmentException, IOException, DLCanceledExecutionException {
        final DLPythonAbstractNetworkReaderCommands reader = getNetworkReaderCommands();
        final DLPythonSourceCodeBuilder b = DLPythonUtils.createSourceCodeBuilder() //
            .a(reader.importReader()) //
            .n("reader = ").a(reader.createReader()) //
            .n("network = ").a("reader.").a(reader.read(path, loadTrainingConfig)) //
            .n(getRegisterNetworkCode("network", null));
        getContext(cancelable).executeInKernel(b.toString(), cancelable);
        return (DLPythonNetworkHandle)getContext(cancelable)
            .getDataFromKernel(CURRENT_NETWORK_NAME, new DLPythonNetworkHandleTableCreatorFactory(), cancelable).getTable();
    }

	@Override
	public void saveNetwork(final DLPythonNetworkHandle network, final String path, final DLCancelable cancelable)
			throws DLInvalidEnvironmentException, IOException, DLCanceledExecutionException {
		final DLPythonSourceCodeBuilder b = DLPythonUtils.createSourceCodeBuilder() //
				.a("import DLPythonNetwork") //
				.n("network = DLPythonNetwork.get_network(").as(network.getIdentifier()).a(")") //
				.n("network.save(").asr(path).a(")");
		getContext(cancelable).executeInKernel(b.toString(), cancelable);
	}

	// TODO: implement network handle
	@Override
	public void setNetworkInputs(final DLPythonNetworkHandle network,
			final Map<? extends DLTensorId, ? extends DLTensor<? extends DLWritableBuffer>> inputs, final DLCancelable cancelable)
			throws DLInvalidEnvironmentException, IOException, DLCanceledExecutionException {
		for (final Entry<? extends DLTensorId, ? extends DLTensor<? extends DLWritableBuffer>> input : inputs
				.entrySet()) {
			final DLTensorId tensorIdentifier = input.getKey();
			final DLTensor<? extends DLWritableBuffer> tensor = input.getValue();
			final TableChunker tableChunker = createSingleTensorTableChunker(tensorIdentifier, tensor);
			try {
				getContext(cancelable).putDataInKernel(tensorIdentifier.getIdentifierString(), tableChunker, 1, cancelable);
			} catch (final IOException ex) {
				throw new RuntimeException("Transmitting input data to Python failed.", ex);
			}
		}
	}

	@Override
	public void executeNetwork(final DLPythonNetworkHandle network, final Set<? extends DLTensorId> requestedOutputs,
			final long batchSize, final DLCancelable cancelable) throws DLInvalidEnvironmentException, IOException, DLCanceledExecutionException {
        final String outputIdentifiers = requestedOutputs.stream().map((id) -> "'" + id.getIdentifierString() + "'")
            .collect(Collectors.joining(", ", "[", "]"));
		final DLPythonSourceCodeBuilder b = DLPythonUtils.createSourceCodeBuilder() //
				.a("import DLPythonNetwork") //
				.n("network = DLPythonNetwork.get_network(").as(network.getIdentifier()).a(")") //
				.n("in_data = {}") //
				.n("for input_spec in network.spec.input_specs:") //
				.n().t().a("in_data[input_spec.identifier] = globals()[input_spec.identifier]") //
				.n("out_data = network.execute(in_data, ").a(batchSize).a(", ").a(outputIdentifiers).a(")") //
				.n("import pandas as pd") //
				.n("output_shapes = {}") //
				.n("for name, data in out_data.items():") //
				.n().t().a("shape = [list(data.iloc[0][0].array.shape)]").n().t()
				.a("output_shapes[name] = [-1 if d is None else d for d in shape]") // replace None with -1
				.n().t().a("globals()[name] = data").n("globals()[").as(OUTPUT_SHAPES_NAME)
				.a("] = pd.DataFrame(output_shapes)");
		getContext(cancelable).executeInKernel(b.toString(), cancelable);
	}

	@Override
	public <T extends DLTensorId> Map<T, long[]> getNetworkOutputShapes(final DLPythonNetworkHandle network,
			final Set<T> outputs, final DLCancelable cancelable) throws DLInvalidEnvironmentException, IOException, DLCanceledExecutionException {
		final Map<T, long[]> shapes = new HashMap<>(outputs.size());
		final Map<String, T> idMap = outputs.stream()
				.collect(Collectors.toMap(DLTensorId::getIdentifierString, Function.identity()));
		getContext(cancelable).getDataFromKernel(OUTPUT_SHAPES_NAME, (tableSpec, tableSize) -> new TableCreator<Object>() {

			@Override
			public void addRow(final Row row) {
				final String[] tensorNames = tableSpec.getColumnNames();
				for (int i = 0; i < tensorNames.length; i++) {
					final Cell shapeCell = row.getCell(i);
					try {
						final int[] intShape = shapeCell.getIntegerArrayValue();
						if (idMap.containsKey(tensorNames[i])) {
							shapes.put(idMap.get(tensorNames[i]), Arrays.stream(intShape).mapToLong(d -> d).toArray());
						}
					} catch (final IllegalStateException e) {
						LOGGER.error(
								"An exception occurred while collecting output shapes from Python: " + e.getMessage(),
								e);
					}
				}
			}

			@Override
			public TableSpec getTableSpec() {
				return tableSpec;
			}

			@Override
			public Object getTable() {
				return null;
			}

		}, cancelable);
		// ensure that we have a shape for each output tensor
		if (shapes.size() != outputs.size()) {
			throw new IllegalStateException(
					"Python didn't return a shape for each output. The shape is missing for outputs "
							+ Sets.difference(outputs, shapes.keySet()) + ".");
		}
		return shapes;
	}

	// TODO: implement network handle
	@Override
	public void getNetworkOutputs(final DLPythonNetworkHandle network,
			final Map<? extends DLTensorId, ? extends DLTensor<? extends DLReadableBuffer>> outputs, final DLCancelable cancelable)
			throws DLInvalidEnvironmentException, IOException, DLCanceledExecutionException {
		for (final Entry<? extends DLTensorId, ? extends DLTensor<? extends DLReadableBuffer>> output : outputs
				.entrySet()) {
			final DLTensorId tensorIdentifier = output.getKey();
			final DLTensor<? extends DLReadableBuffer> tensor = output.getValue();

			getContext(cancelable).getDataFromKernel(tensorIdentifier.getIdentifierString(),
					(tableSpec, tableSize) -> new TableCreator<DLTensor<? extends DLReadableBuffer>>() {

						@Override
						public void addRow(final Row row) {
							final String deserializerId = tableSpec.getColumnSerializers()
									.get(tensorIdentifier.getIdentifierString());
							final DeserializerFactory deserializerFactory = PythonToKnimeExtensions
									.getExtension(deserializerId).getJavaDeserializerFactory();
							if (!(deserializerFactory instanceof DLPythonDeserializerFactory)) {
								LOGGER.coding(
										"Deep learning Python to KNIME serialization factory must implement DLSerializerFactory.");
							}
							final Deserializer deserializer = deserializerFactory.createDeserializer();
							if (!(deserializer instanceof DLPythonDeserializer)) {
								final String msg = "An exception occurred while collecting network output from Python. Unsupported deserializer.";
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
					}, cancelable);
		}
	}

	@Override
	public void trainNetwork(final DLPythonNetworkHandle network, final DLNetworkInputProvider trainingInputProvider,
			final DLNetworkInputProvider validationInputProvider, final DLTrainingMonitor<? extends DLPythonTrainingStatus> monitor)
			throws DLInvalidEnvironmentException, IOException, DLCanceledExecutionException {
		final Messages messages = getContext(monitor).getKernel().getMessages();

		PythonToJavaMessageHandler trainingDataRequestHandler = null;
		PythonToJavaMessageHandler validationDataRequestHandler = null;
		PythonToJavaMessageHandler onEpochBeginHandler = null;
		PythonToJavaMessageHandler onEpochEndHandler = null;
		PythonToJavaMessageHandler onBatchBeginHandler = null;
		PythonToJavaMessageHandler onBatchEndHandler = null;
        PythonOutputListener stdOutListener = null;
        PythonOutputListener stdErrListener = null;

		try {
			final DLPythonTrainingStatus status = monitor.getTrainingStatus();

			trainingDataRequestHandler = new AbstractPythonToJavaMessageHandler("request_training_data") {

				@Override
				protected void handle(final PythonToJavaMessage msg) throws Exception {
					monitor.checkCanceled();
					final long batchIndex = Long.parseLong(msg.getValue());
					final Map<DLTensorId, DLTensor<? extends DLWritableBuffer>> input = trainingInputProvider
							.get(batchIndex);
					monitor.checkCanceled();
					for (final Entry<DLTensorId, DLTensor<? extends DLWritableBuffer>> entry : input.entrySet()) {
						final DLTensor<? extends DLWritableBuffer> tensor = entry.getValue();
						final TableChunker tableChunker = createSingleTensorTableChunker(entry.getKey(), tensor);
						try {
							getContext(monitor).putDataInKernel(entry.getKey().getIdentifierString(), tableChunker, 1, monitor);
						} catch (final IOException ex) {
							throw new IOException("Transmitting training data to Python failed.", ex);
						} finally {
							tensor.getBuffer().reset();
						}
					}
					monitor.checkCanceled();
					messages.answer(new DefaultJavaToPythonResponse(msg, ""));
				}
			};
			messages.registerMessageHandler(trainingDataRequestHandler);

			if (validationInputProvider != null) {
				validationDataRequestHandler = new AbstractPythonToJavaMessageHandler("request_validation_data") {

					@Override
					protected void handle(final PythonToJavaMessage msg) throws Exception {
						monitor.checkCanceled();
						final long batchIndex = Long.parseLong(msg.getValue());
						final Map<DLTensorId, DLTensor<? extends DLWritableBuffer>> input = validationInputProvider
								.get(batchIndex);
						monitor.checkCanceled();
						for (final Entry<DLTensorId, DLTensor<? extends DLWritableBuffer>> entry : input.entrySet()) {
							final DLTensor<? extends DLWritableBuffer> tensor = entry.getValue();
							final TableChunker tableChunker = createSingleTensorTableChunker(entry.getKey(), tensor);
							try {
								// TODO: different identifiers for validation input? (pre-fetching on Python side...)
								getContext(monitor).putDataInKernel(entry.getKey().getIdentifierString(), tableChunker, 1, monitor);
							} catch (final IOException ex) {
								throw new IOException("Transmitting validation data to Python failed.", ex);
							} finally {
								tensor.getBuffer().reset();
							}
						}
						monitor.checkCanceled();
						messages.answer(new DefaultJavaToPythonResponse(msg, ""));
					}
				};
				messages.registerMessageHandler(validationDataRequestHandler);
			}

			onEpochBeginHandler = new AbstractPythonToJavaMessageHandler("epoch_begin") {

				@Override
				protected void handle(final PythonToJavaMessage msg) throws Exception {
					status.epochStarted().raise(null);
				}
			};
			messages.registerMessageHandler(onEpochBeginHandler);

			final LinkedHashMap<String, DLReportedMetric> epochMetrics = new LinkedHashMap<>(4);
			epochMetrics.put("val_accuracy", new DLReportedMetric("val_accuracy", 0f));
			epochMetrics.put("val_loss", new DLReportedMetric("val_loss", 0f));

			onEpochEndHandler = new AbstractPythonToJavaMessageHandler("epoch_end") {

				@Override
				protected void handle(final PythonToJavaMessage msg) throws Exception {
					final String[] metricsStr = msg.getValue().split(";");
					int i = 0;
					for (final DLReportedMetric m : epochMetrics.values()) {
						try {
							m.setValue(Float.parseFloat(metricsStr[i]));
						} catch (final NumberFormatException e) {
							m.setValue(-1f);
							LOGGER.debug(
									"Received invalid value for metric '" + m.getName() + "': " + m.getValue() + ".");
						}
						i++;
					}
					status.epochEnded().raise(epochMetrics);
				}
			};
			messages.registerMessageHandler(onEpochEndHandler);

			onBatchBeginHandler = new AbstractPythonToJavaMessageHandler("batch_begin") {

				@Override
				protected void handle(final PythonToJavaMessage msg) throws Exception {
					status.batchStarted().raise(null);
				}
			};
			messages.registerMessageHandler(onBatchBeginHandler);

			final LinkedHashMap<String, DLReportedMetric> batchMetrics = new LinkedHashMap<>(4);
			batchMetrics.put("accuracy", new DLReportedMetric("accuracy", 0f));
			batchMetrics.put("loss", new DLReportedMetric("loss", 0f));

			onBatchEndHandler = new AbstractPythonToJavaMessageHandler("batch_end") {

				@Override
				protected void handle(final PythonToJavaMessage msg) throws Exception {
					monitor.checkCanceled();
					final String[] metricsStr = msg.getValue().split(";");
					int i = 0;
					for (final DLReportedMetric m : batchMetrics.values()) {
						try {
							m.setValue(Float.parseFloat(metricsStr[i]));
						} catch (final NumberFormatException e) {
							m.setValue(-1f);
							LOGGER.debug(
									"Received invalid value for metric '" + m.getName() + "': " + m.getValue() + ".");
						}
						i++;
					}
					messages.answer(
							new DefaultJavaToPythonResponse(msg, status.getStatus() == Status.RUNNING ? "c" : "s"));
					status.batchEnded().raise(batchMetrics);
					// start validation phase if validation is enabled and we finished the last training batch of the
					// epoch
					if (validationInputProvider != null
							&& status.getCurrentBatchInEpoch() == status.getNumBatchesPerEpoch() - 1) {
						status.validationStarted().raise(null);
					}
				}
			};
			messages.registerMessageHandler(onBatchEndHandler);

            // Add log listeners
            final StringBuilder stdOut = new StringBuilder();
            final StringBuilder stdErr = new StringBuilder();
            stdOutListener = (msg) -> {
                stdOut.append(msg);
                stdOut.append("\n");
                status.setStdOutOutput(stdOut.toString());
            };
            stdErrListener = (msg) -> {
                stdErr.append(msg);
                stdErr.append("\n");
                status.setStdErrOutput(stdErr.toString());
            };
            getContext(monitor).getKernel().addStdoutListener(stdOutListener);
            getContext(monitor).getKernel().addStderrorListener(stdErrListener);

			final DLPythonSourceCodeBuilder b = DLPythonUtils.createSourceCodeBuilder() //
					.a("import DLPythonNetwork") //
					.n("network = DLPythonNetwork.get_network(").as(network.getIdentifier()).a(")") //
					.n("from DLKerasNetworkTrainingInputGenerator import DLKerasNetworkTrainingInputGenerator") //
					.n("training_data_supplier = DLKerasNetworkTrainingInputGenerator(network, ")
					/**/ .a(trainingInputProvider.getNumBatches()).a(", network.spec.training_config.batch_size, ")
					/**/ .as("request_training_data").a(")");
			if (validationInputProvider != null) {
				b.n("validation_data_supplier = DLKerasNetworkTrainingInputGenerator(network, ")
						.a(validationInputProvider.getNumBatches())
						.a(", network.spec.training_config.validation_batch_size, ").as("request_validation_data")
						.a(")");
			} else {
				b.n("validation_data_supplier = None");
			}
			b.n("network.train(training_data_supplier, validation_data_supplier=validation_data_supplier)");
			getContext(monitor).executeInKernel(b.toString(), monitor);
		} finally {
			if (trainingDataRequestHandler != null) {
				messages.unregisterMessageHandler(trainingDataRequestHandler);
			}
			if (validationDataRequestHandler != null) {
				messages.unregisterMessageHandler(validationDataRequestHandler);
			}
			if (onEpochBeginHandler != null) {
				messages.unregisterMessageHandler(onEpochBeginHandler);
			}
			if (onEpochEndHandler != null) {
				messages.unregisterMessageHandler(onEpochEndHandler);
			}
			if (onBatchBeginHandler != null) {
				messages.unregisterMessageHandler(onBatchBeginHandler);
			}
			if (onBatchEndHandler != null) {
				messages.unregisterMessageHandler(onBatchEndHandler);
			}

            // Remove log listeners
            getContext(monitor).getKernel().removeStderrorListener(stdOutListener);
            getContext(monitor).getKernel().removeStderrorListener(stdErrListener);
		}
	}

	/**
	 * Closes the underlying {@link DLPythonContext Python context}.
	 */
	@Override
	public synchronized void close() {
		m_context.close();
	}

    protected String getRegisterNetworkCode(final String networkVariable, final String networkIdentifier) {
        final DLPythonSourceCodeBuilder b = DLPythonUtils.createSourceCodeBuilder() //
            .a("import DLPythonNetwork") //
            .n("network_id = DLPythonNetwork.add_network(").a(networkVariable);
        if (networkIdentifier != null) {
            b.a(", ").as(networkIdentifier);
        }
        b.a(")") //
            .n("import pandas as pd") //
            .n("global ").a(CURRENT_NETWORK_NAME) //
            .n(CURRENT_NETWORK_NAME).a(" = ").a("pd.DataFrame.from_dict({").as(CURRENT_NETWORK_NAME).a(":[network_id]})");
        return b.toString();
    }

	protected String getExtractNetworkSpecsCode(final DLPythonNetworkHandle network) {
		return "import DLPythonNetworkSpecToDataFrameConverter\n" + //
				"global " + INPUT_SPECS_NAME + "\n" + //
				"global " + HIDDEN_OUTPUT_SPECS_NAME + "\n" + //
				"global " + OUTPUT_SPECS_NAME + "\n" + //
				INPUT_SPECS_NAME + ", " + HIDDEN_OUTPUT_SPECS_NAME + ", " + OUTPUT_SPECS_NAME + ", " + //
				OPTIMIZER_SPECS + ", " + LOSS_SPECS + ", " + METRICS_SPECS + " = " + //
            "DLPythonNetworkSpecToDataFrameConverter.get_layer_data_specs_as_data_frames('" + network.getIdentifier()
            + "')";
	}

    /**
     * Extracts the tensor spec from a pandas DataFrame.
     *
     * @param specName the name of the pandas DataFrame
     * @param cancelable to check if the execution has been canceled
     * @return the tensor spec in the DataFrame
     * @throws DLCanceledExecutionException if the execution has been canceled
     * @throws DLInvalidEnvironmentException if failed to properly setup the Python context
     * @throws IOException if getting the data from python failed
     */
    protected DLTensorSpec[] extractTensorSpec(final String specName, final DLCancelable cancelable)
        throws DLCanceledExecutionException, DLInvalidEnvironmentException, IOException {
        return (DLTensorSpec[])getContext(cancelable).getDataFromKernel(specName,
            new DLPythonTensorSpecTableCreatorFactory(DLPythonNumPyTypeMap.INSTANCE), cancelable).getTable();
    }

    /**
     * @param cancelable to check if the execution has been canceled
     * @return the python version
     * @throws DLCanceledExecutionException if the execution has been canceled
     * @throws DLInvalidEnvironmentException if failed to properly setup the Python context
     * @throws IOException if getting the data from python failed
     */
    protected Version getPythonVersion(final DLCancelable cancelable)
        throws DLCanceledExecutionException, DLInvalidEnvironmentException, IOException {
        final DLPythonSourceCodeBuilder b = DLPythonUtils.createSourceCodeBuilder() //
                .a("import sys") //
                .n("import pandas as pd") //
                .n("global ").a(PYTHON_VERSION_NAME) //
                .n(PYTHON_VERSION_NAME).a(" = pd.DataFrame(['{}.{}.{}'.format(*sys.version_info[:3])])");
        getContext(cancelable).executeInKernel(b.toString(), cancelable);
        final String pythonVersion = (String)getContext(cancelable).getDataFromKernel(PYTHON_VERSION_NAME,
            (s, ts) -> new SingleValueTableCreator<>(s, Cell::getStringValue), cancelable).getTable();
        return new Version(pythonVersion);
    }

    private TableChunker createSingleTensorTableChunker(final DLTensorId tensorId, final DLTensor<? extends DLWritableBuffer> tensor)
        throws IOException {
        DLPythonTableChunker tableChunker = m_tableChunkers.get(tensorId);
        if (tableChunker == null) {
            tableChunker = new DLPythonTableChunker(tensor);
            m_tableChunkers.put(tensorId, tableChunker);
        }
        tableChunker.resetWithNextTensor(tensor);
        return tableChunker;
    }

    private static byte[] getNotMissingForLength(final int length) {
        final int entries = length / 8 + 1;
        final byte[] missings = new byte[entries];
        Arrays.fill(missings, UnsignedBytes.MAX_VALUE);
        return missings;
    }

    private static final class DLPythonTableChunker implements TableChunker {

        private final DLPythonResetableTableIterator m_iterator;

        private boolean m_hasNextChunk = true;

        private final Serializer<DLPythonDataBuffer<?>> m_serializer;

        private final TableSpec m_tableSpec;

        private final Row m_row;

        private DLPythonTableChunker(final DLTensor<? extends DLWritableBuffer> tensor) throws IOException {
            // Create the serializer
            final KnimeToPythonExtension extension = KnimeToPythonExtensions.getExtensions().stream()
                .filter(ext -> (ext.getJavaSerializerFactory() instanceof DLSerializerFactory)
                    && ((DLSerializerFactory)ext.getJavaSerializerFactory()).getBufferType()
                        .isAssignableFrom(tensor.getBuffer().getClass()))
                .findFirst() //
                .orElseThrow(() -> new RuntimeException(
                    "Transmitting data to Python failed. No matching serializer available."));
            // TODO: if nothing found, we should also try to match primitive types with their wrapper types (guava
            // Primitives.wrap etc.)
            m_serializer = (Serializer<DLPythonDataBuffer<?>>)extension.getJavaSerializerFactory().createSerializer();

            // Create the shape cell (the same every time)
            final long[] shape = DLUtils.Shapes.getFixedShape(tensor.getSpec().getShape())
                .orElseThrow(() -> new IllegalStateException("Execution spec does not contain fixed shape."));
            final Cell shapeCell = new CellImpl(shape, getNotMissingForLength(shape.length));

            // Create the table spec
            final String identifier = tensor.getSpec().getIdentifier().getIdentifierString();
            m_tableSpec = new TableSpecImpl(new Type[]{Type.BYTES, Type.LONG_LIST}, new String[]{identifier, "shape"},
                Collections.singletonMap(identifier, extension.getId()));

            // Create the row
            m_row = new RowImpl(identifier, 2);
            m_row.setCell(shapeCell, 1);
            m_iterator = new DLPythonResetableTableIterator(m_tableSpec, m_row);
        }

        @Override
        public boolean hasNextChunk() {
            return m_hasNextChunk;
        }

        @Override
        public TableIterator nextChunk(final int numRows) {
            if (m_hasNextChunk) {
                m_hasNextChunk = false;
            }
            return m_iterator;
        }

        @Override
        public int getNumberRemainingRows() {
            return m_iterator.getNumberRemainingRows();
        }

        @Override
        public TableSpec getTableSpec() {
            return m_tableSpec;
        }

        private void resetWithNextTensor(final DLTensor<? extends DLWritableBuffer> tensor) throws IOException {
            final Cell cell = new CellImpl(m_serializer.serialize((DLPythonDataBuffer<?>)tensor.getBuffer()));
            m_row.setCell(cell, 0);
            m_iterator.reset();
            m_hasNextChunk = true;
        }
    }

    private static final class DLPythonResetableTableIterator implements TableIterator {

        private final TableSpec m_tableSpec;

        private final Row m_row;

        private boolean m_hasNext = true;

        private DLPythonResetableTableIterator(final TableSpec tableSpec, final Row row) {
            m_tableSpec = tableSpec;
            m_row = row;
        }

        @Override
        public Row next() {
            m_hasNext = false;
            return m_row;
        }

        @Override
        public boolean hasNext() {
            return m_hasNext;
        }

        @Override
        public int getNumberRemainingRows() {
            return m_hasNext ? 1 : 0;
        }

        @Override
        public TableSpec getTableSpec() {
            return m_tableSpec;
        }

        private void reset() {
            m_hasNext = true;
        }
    }

    protected abstract static class DLPythonAbstractNetworkReaderCommands {

        private final String m_importStatement;

        private final String m_createReaderStatement;

        protected DLPythonAbstractNetworkReaderCommands(final String importStatement,
            final String createReaderStatement) {
            m_importStatement = importStatement;
            m_createReaderStatement = createReaderStatement;
        }

        public abstract String read(String path, boolean loadTrainingConfig);

        public String importReader() {
            return m_importStatement;
        }

        public String createReader() {
            return m_createReaderStatement;
        }
    }
}
