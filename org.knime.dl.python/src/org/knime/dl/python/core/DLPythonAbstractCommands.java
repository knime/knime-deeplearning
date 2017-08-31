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

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import org.knime.core.node.NodeLogger;
import org.knime.dl.core.DLLayerDataSpec;
import org.knime.dl.core.data.DLReadableBuffer;
import org.knime.dl.core.data.DLWritableBuffer;
import org.knime.dl.core.execution.DLLayerDataBatch;
import org.knime.dl.python.core.data.DLPythonDataBuffer;
import org.knime.dl.python.core.data.DLPythonTypeMap;
import org.knime.dl.python.core.data.serde.DLPythonDeserializer;
import org.knime.dl.python.core.data.serde.DLPythonDeserializerFactory;
import org.knime.dl.python.core.data.serde.DLSerializerFactory;
import org.knime.python.typeextension.Deserializer;
import org.knime.python.typeextension.DeserializerFactory;
import org.knime.python.typeextension.KnimeToPythonExtension;
import org.knime.python.typeextension.KnimeToPythonExtensions;
import org.knime.python.typeextension.PythonToKnimeExtensions;
import org.knime.python.typeextension.Serializer;
import org.knime.python2.extensions.serializationlibrary.interfaces.Cell;
import org.knime.python2.extensions.serializationlibrary.interfaces.Row;
import org.knime.python2.extensions.serializationlibrary.interfaces.TableCreator;
import org.knime.python2.extensions.serializationlibrary.interfaces.TableCreatorFactory;
import org.knime.python2.extensions.serializationlibrary.interfaces.TableIterator;
import org.knime.python2.extensions.serializationlibrary.interfaces.TableSpec;
import org.knime.python2.extensions.serializationlibrary.interfaces.Type;
import org.knime.python2.extensions.serializationlibrary.interfaces.impl.CellImpl;
import org.knime.python2.extensions.serializationlibrary.interfaces.impl.KeyValueTableIterator;
import org.knime.python2.extensions.serializationlibrary.interfaces.impl.RowImpl;
import org.knime.python2.extensions.serializationlibrary.interfaces.impl.TableSpecImpl;
import org.knime.python2.generic.ScriptingNodeUtils;
import org.knime.python2.kernel.PythonKernel;
import org.knime.python2.kernel.PythonKernelOptions;

/**
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
public abstract class DLPythonAbstractCommands<CFG extends DLPythonAbstractCommandsConfig> implements AutoCloseable {

	private static final NodeLogger LOGGER = NodeLogger.getLogger(DLPythonAbstractCommands.class);

	// TODO: for long running KNIME instances, a static flag might not be the best idea
	private static boolean testedInstallation = false;

	public static PythonKernel createKernel() throws IOException {
		return new PythonKernel(new PythonKernelOptions());
	}

	protected final PythonKernel m_kernel;

	protected final CFG m_config;

	protected DLPythonAbstractCommands(final CFG config) throws IOException {
		this(config, createKernel());
	}

	protected DLPythonAbstractCommands(final CFG config, final PythonKernel kernel) throws IOException {
		m_kernel = kernel;
		m_config = config;
		setupEnvironment();
		if (!testedInstallation) {
			if (!testInstallation()) {
				throw new IOException("Python installation tests failed. "
						+ "Please ensure that Python and all required packages are properly installed. "
						+ "See log for details.");
			}
			testedInstallation = true;
		}
	}

	public abstract DLPythonNetworkSpec extractNetworkSpec(final DLPythonNetworkHandle handle,
			final DLPythonTypeMap typeMap) throws IOException;

	public PythonKernel getKernel() {
		return m_kernel;
	}

	// TODO: we should get the model name (= handle identifier) from Python
	public DLPythonNetworkHandle loadNetwork(final String path) throws IOException {
		m_kernel.execute(m_config.getLoadCode(path));
		return new DLPythonNetworkHandle(DLPythonAbstractCommandsConfig.DEFAULT_MODEL_NAME);
	}

	public void saveNetwork(final DLPythonNetworkHandle handle, final String path) throws IOException {
		m_kernel.execute(m_config.getSaveCode(handle, path));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public synchronized void setNetworkInputs(final DLPythonNetworkHandle handle, // TODO: implement handle
			final Map<DLLayerDataSpec, DLLayerDataBatch<? extends DLWritableBuffer>> input, final long batchSize)
			throws IOException {
		for (final Entry<DLLayerDataSpec, DLLayerDataBatch<? extends DLWritableBuffer>> in : input.entrySet()) {
			m_kernel.putData(in.getKey().getName(), new DLSingletonTableChunker(new TableIterator() {

				int i = 0;

				int numRemaining = (int) batchSize;

				private Serializer<DLPythonDataBuffer> m_serializer;
				{
					final Optional<KnimeToPythonExtension> extensions = KnimeToPythonExtensions.getExtensions().stream()
							.filter(new Predicate<KnimeToPythonExtension>() {

								@Override
								public boolean test(final KnimeToPythonExtension ext) {
									return (ext.getJavaSerializerFactory() instanceof DLSerializerFactory)
											&& ((DLSerializerFactory) ext.getJavaSerializerFactory()).getBufferType()
													.isAssignableFrom(
															in.getValue().getBatch()[i].getBuffer().getClass());
								}
							}).findFirst();

					m_serializer = (Serializer<DLPythonDataBuffer>) extensions
							.orElseThrow(() -> new RuntimeException(
									"Transmitting input data to Python failed. No matching serializer available."))
							.getJavaSerializerFactory().createSerializer();
				}

				@Override
				public Row next() {
					final Row row = new RowImpl(in.getKey().getName() + "[" + i + "]", 1);
					// TODO: if nothing found, we should also try to match
					// primitive types with their wrapper types
					// (guava or apache) (and print a warning in such cases)

					try {
						final Cell cell = new CellImpl(
								m_serializer.serialize((DLPythonDataBuffer) in.getValue().getBatch()[i].getBuffer()));
						row.setCell(cell, 0);
					} catch (final IOException ex) {
						throw new RuntimeException("Transmitting input data to Keras failed.", ex);
					}
					i++;
					numRemaining--;
					return row;
				}

				@Override
				public boolean hasNext() {
					return i < batchSize;
				}

				@Override
				public TableSpec getTableSpec() {
					final Optional<KnimeToPythonExtension> extensions = KnimeToPythonExtensions.getExtensions().stream()
							.filter(new Predicate<KnimeToPythonExtension>() {
								/**
								 * {@inheritDoc}
								 */
								@Override
								public boolean test(final KnimeToPythonExtension ext) {
									// TODO check if switch
									return (ext.getJavaSerializerFactory() instanceof DLSerializerFactory)
											&& ((DLSerializerFactory) ext.getJavaSerializerFactory()).getBufferType()
													.isAssignableFrom(
															in.getValue().getBatch()[0].getBuffer().getClass());
								}
							}).findFirst();
					// TODO: if nothing found, we should also try to match
					// primitive types with their wrapper types
					// (guava Primitives.wrap etc.)
					final String serializerId = extensions
							.orElseThrow(() -> new RuntimeException(
									"Transmitting input data to Python failed. No matching serializer available."))
							.getId();
					return new TableSpecImpl(new Type[] { Type.BYTES }, new String[] { in.getKey().getName() },
							Collections.singletonMap(in.getKey().getName(), serializerId));
				}

				@Override
				public int getNumberRemainingRows() {
					return numRemaining;
				}
			}), (int) batchSize);
		}
	}

	public void executeNetwork(final DLPythonNetworkHandle handle, final Set<DLLayerDataSpec> requestedOutputs)
			throws IOException {
		final String[] output = m_kernel.execute(m_config.getExecuteCode(handle, requestedOutputs));
		if (!output[1].isEmpty()) {
			LOGGER.debug(ScriptingNodeUtils.shortenString(output[1], 1000));
		}
	}

	public synchronized void getNetworkOutputs(final DLPythonNetworkHandle handle, // TODO: implement handle
			final Map<DLLayerDataSpec, DLLayerDataBatch<? extends DLReadableBuffer>> output) throws IOException {
		for (final Entry<DLLayerDataSpec, DLLayerDataBatch<? extends DLReadableBuffer>> out : output.entrySet()) {
			final DLLayerDataSpec spec = out.getKey();
			final DLLayerDataBatch<? extends DLReadableBuffer> batch = out.getValue();
			m_kernel.getData(spec.getName(), new TableCreatorFactory() {

				@Override
				public TableCreator<DLLayerDataBatch<? extends DLReadableBuffer>> createTableCreator(
						final TableSpec tableSpec, final int tableSize) {
					return new TableCreator<DLLayerDataBatch<? extends DLReadableBuffer>>() {

						private Deserializer m_deserializer;

						int i = 0;
						{
							final String deserializerId = tableSpec.getColumnSerializers().get(spec.getName());
							final DeserializerFactory deserializerFactory =
									PythonToKnimeExtensions.getExtension(deserializerId).getJavaDeserializerFactory();
							if (!(deserializerFactory instanceof DLPythonDeserializerFactory)) {
								LOGGER.coding(
										"Deep learning Python to KNIME serialization factory must implement DLSerializerFactory.");
							}
							m_deserializer = deserializerFactory.createDeserializer();
							if (!(m_deserializer instanceof DLPythonDeserializer)) {
								final String msg =
										"An exception occurred while collecting network output from Python. Unsupported deserializer.";
								LOGGER.error(msg);
								// TODO
								throw new RuntimeException(msg);
							}
						}

						@Override
						public void addRow(final Row row) {
							final Cell cell = row.getCell(0);

							try {
								((DLPythonDeserializer) m_deserializer).deserialize(cell.getBytesValue(),
										batch.getBatch()[i]);
							} catch (final IllegalStateException e) {
								LOGGER.error("An exception occurred while collecting network output from Python: "
										+ e.getMessage(), e);
							}
							i++;
						}

						@Override
						public TableSpec getTableSpec() {
							return tableSpec;
						}

						@Override
						public DLLayerDataBatch<? extends DLReadableBuffer> getTable() {
							return batch;
						}
					};
				}
			});
		}
	}

	public void putParameter(final String key, final String parameter) throws IOException {
		putParameter(key, new CellImpl(parameter));
	}

	public void putParameter(final String key, final int parameter) throws IOException {
		putParameter(key, new CellImpl(parameter));
	}

	public void putParameter(final String key, final long parameter) throws IOException {
		putParameter(key, new CellImpl(parameter));
	}

	/**
	 * Closes the underlying {@link PythonKernel Python kernel}.
	 */
	@Override
	public void close() throws Exception {
		m_kernel.close();
	}

	private void putParameter(final String key, final Cell cell) throws IOException {
		final TableSpec spec =
				new TableSpecImpl(new Type[] { cell.getColumnType() }, new String[] { key }, new HashMap<>(0));
		final RowImpl row = new RowImpl(key, 1);
		row.setCell(cell, 0);
		m_kernel.putData(key, new DLSingletonTableChunker(new KeyValueTableIterator(spec, row)), 1);
	}

	private void setupEnvironment() throws IOException {
		m_kernel.execute(m_config.getSetupEnvironmentCode());
	}

	private boolean testInstallation() {
		try {
			final String[] output = m_kernel.execute(m_config.getTestInstallationCode());
			if (!output[1].isEmpty()) {
				LOGGER.error("Python installation tests failed. "
						+ "Please ensure that Python and all required packages are properly installed. Test results: "
						+ output[1]);
				return false;
			}
			return true;
		} catch (final Exception e) {
			return false;
		}
	}
}
