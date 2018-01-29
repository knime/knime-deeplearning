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
 *   May 2, 2017 (dietzc): created
 */
package org.knime.dl.keras.base.nodes.learner;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;
import org.knime.dl.base.portobjects.DLNetworkPortObject;
import org.knime.dl.base.portobjects.DLNetworkPortObjectSpec;
import org.knime.dl.core.DLCanceledExecutionException;
import org.knime.dl.core.DLDataTableRowIterator;
import org.knime.dl.core.DLException;
import org.knime.dl.core.DLMissingDependencyException;
import org.knime.dl.core.DLNetwork;
import org.knime.dl.core.DLNetworkSpec;
import org.knime.dl.core.DLTensorId;
import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.core.data.convert.DLCollectionDataValueToTensorConverterFactory;
import org.knime.dl.core.data.convert.DLDataValueToTensorConverterFactory;
import org.knime.dl.core.training.DLKnimeNetworkTrainingInputPreparer;
import org.knime.dl.core.training.DLKnimeTrainingMonitor;
import org.knime.dl.core.training.DLMetrics;
import org.knime.dl.core.training.DLTrainingContext;
import org.knime.dl.core.training.DLTrainingStatus.Status;
import org.knime.dl.keras.base.nodes.learner.view.DLInteractiveLearnerNodeModel;
import org.knime.dl.keras.base.nodes.learner.view.DLLinePlotViewData;
import org.knime.dl.keras.base.nodes.learner.view.DLProgressMonitor;
import org.knime.dl.keras.base.nodes.learner.view.DLStaticLinePlotViewData;
import org.knime.dl.keras.base.nodes.learner.view.DLUpdatableLinePlotViewData;
import org.knime.dl.keras.base.nodes.learner.view.DLViewSpec;
import org.knime.dl.keras.base.nodes.learner.view.jfreechart.DLDefaultJFreeChartLinePlotViewSpec;
import org.knime.dl.keras.base.nodes.learner.view.jfreechart.DLJFreeChartLinePlotViewSpec;
import org.knime.dl.keras.base.portobjects.DLKerasNetworkPortObject;
import org.knime.dl.keras.base.portobjects.DLKerasNetworkPortObjectSpec;
import org.knime.dl.keras.core.DLKerasNetwork;
import org.knime.dl.keras.core.DLKerasNetworkSpec;
import org.knime.dl.keras.core.training.DLKerasCallback;
import org.knime.dl.keras.core.training.DLKerasDefaultTrainingConfig;
import org.knime.dl.keras.core.training.DLKerasDefaultTrainingStatus;
import org.knime.dl.keras.core.training.DLKerasLossFunction;
import org.knime.dl.keras.core.training.DLKerasNetworkTrainingSession;
import org.knime.dl.keras.core.training.DLKerasOptimizer;
import org.knime.dl.keras.core.training.DLKerasTrainingConfig;
import org.knime.dl.keras.core.training.DLKerasTrainingContext;
import org.knime.dl.keras.core.training.DLKerasTrainingStatus;
import org.knime.dl.util.DLUtils;

import com.google.common.base.Strings;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
final class DLKerasLearnerNodeModel extends NodeModel implements DLInteractiveLearnerNodeModel {

	static final int IN_NETWORK_PORT_IDX = 0;

	static final int IN_DATA_PORT_IDX = 1;

	static final int OUT_NETWORK_PORT_IDX = 0;

	static final String CFG_KEY_INPUT = "training";

	static final String CFG_KEY_TARGET = "target";

	static final String INTERNAL_FILENAME = "view.data";

	private static final NodeLogger LOGGER = NodeLogger.getLogger(DLKerasLearnerNodeModel.class);

	static DLKerasLearnerGeneralConfig createGeneralModelConfig() {
		return new DLKerasLearnerGeneralConfig();
	}

	static DLKerasLearnerInputConfig createInputTensorModelConfig(final String inputTensorName,
			final DLKerasLearnerGeneralConfig generalCfg) {
		return new DLKerasLearnerInputConfig(inputTensorName, generalCfg);
	}

	static DLKerasLearnerTargetConfig createOutputTensorModelConfig(final String outputTensorName,
			final DLKerasLearnerGeneralConfig generalCfg) {
		return new DLKerasLearnerTargetConfig(outputTensorName, generalCfg);
	}

	private final DLKerasLearnerGeneralConfig m_generalCfg;

	private final HashMap<String, DLKerasLearnerInputConfig> m_inputCfgs;

	private final HashMap<String, DLKerasLearnerTargetConfig> m_targetCfgs;

	private LinkedHashMap<DLTensorSpec, DLDataValueToTensorConverterFactory<?, ?>> m_converters;

	private DLNetworkSpec m_lastIncomingNetworkSpec;

	private DLNetworkSpec m_lastConfiguredNetworkSpec;

	private DataTableSpec m_lastIncomingTableSpec;

	private DataTableSpec m_lastConfiguredTableSpec;

	private boolean m_initialLoaded;

	private final DLJFreeChartLinePlotViewSpec[] m_viewSpecs;

	private final DLLinePlotViewData<?>[] m_viewData;

	private final DLKerasDefaultTrainingStatus m_status;

	DLKerasLearnerNodeModel() {
		super(new PortType[] { DLKerasNetworkPortObject.TYPE, BufferedDataTable.TYPE },
				new PortType[] { DLKerasNetworkPortObject.TYPE });
		m_generalCfg = createGeneralModelConfig();
		m_inputCfgs = new HashMap<>();
		m_targetCfgs = new HashMap<>();

		// as soon as we want to be more dynamic e.g. make views configurable we
		// have to move this somewhere else..
		m_viewSpecs = new DLJFreeChartLinePlotViewSpec[2];
		m_viewSpecs[0] = new DLDefaultJFreeChartLinePlotViewSpec("accuracy", "Accuracy", "Accuracy", "Batches",
				new String[] { "Training data" });
		m_viewSpecs[1] = new DLDefaultJFreeChartLinePlotViewSpec("loss", "Loss", "Loss", "Batches",
				new String[] { "Training data" });
		m_viewData = new DLLinePlotViewData[2];
		m_status = new DLKerasDefaultTrainingStatus();
	}

	@Override
	public DLProgressMonitor getProgressMonitor() {
		return m_status;
	}

	@Override
	public void stopLearning() {
		m_status.setStatus(Status.STOPPED_EARLY);
	}

	protected DLViewSpec[] getViewSpecs() {
		return m_viewSpecs;
	}

	@Override
	protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
		if (inSpecs[DLKerasLearnerNodeModel.IN_NETWORK_PORT_IDX] == null) {
			throw new InvalidSettingsException("Input deep learning network is missing.");
		}
		if (inSpecs[DLKerasLearnerNodeModel.IN_DATA_PORT_IDX] == null) {
			throw new InvalidSettingsException("Input data table is missing.");
		}
		if (!DLKerasNetworkPortObject.TYPE
				.acceptsPortObjectSpec(inSpecs[DLKerasLearnerNodeModel.IN_NETWORK_PORT_IDX])) {
			throw new InvalidSettingsException(
					"Input port object is not a valid Keras deep learning network port object.");
		}

		final DLKerasNetworkPortObjectSpec inPortObjectSpec = ((DLKerasNetworkPortObjectSpec) inSpecs[IN_NETWORK_PORT_IDX]);
		final DLKerasNetworkSpec inNetworkSpec = inPortObjectSpec.getNetworkSpec();
		final Class<? extends DLNetwork> inNetworkType = inPortObjectSpec.getNetworkType();
		final DataTableSpec inTableSpec = ((DataTableSpec) inSpecs[IN_DATA_PORT_IDX]);

		if (inNetworkSpec == null) {
			throw new InvalidSettingsException("Input port object's deep learning network specs are missing.");
		}

		m_lastIncomingNetworkSpec = inNetworkSpec;
		m_lastIncomingTableSpec = inTableSpec;

		if (m_lastConfiguredNetworkSpec != null && m_lastConfiguredTableSpec != null) {
			if (!m_lastConfiguredNetworkSpec.equals(m_lastIncomingNetworkSpec)) {
				throw new InvalidSettingsException("Input deep learning network changed. Please reconfigure the node.");
			}
		} else if (m_initialLoaded) {
			// loaded from saved workflow
			m_lastConfiguredNetworkSpec = m_lastIncomingNetworkSpec;
			m_lastConfiguredTableSpec = m_lastIncomingTableSpec;
		}

		try {
			configureGeneral(inNetworkType);
			configureInputs(inNetworkSpec, inTableSpec);
		} catch (final Exception e) {
			throw new InvalidSettingsException(e.getMessage(), e);
		}

		final DLNetworkPortObjectSpec outDataSpec = createOutputSpec(inPortObjectSpec);
		return new PortObjectSpec[] { outDataSpec };
	}

	@Override
	protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
		final PortObject inPortObject = inObjects[IN_NETWORK_PORT_IDX];
		final BufferedDataTable inTable = (BufferedDataTable) inObjects[IN_DATA_PORT_IDX];

		final PortObject outPortObject = executeInternal(inPortObject, inTable, exec);

		return new PortObject[] { outPortObject };
	}

	@Override
	protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		final File f = new File(nodeInternDir, INTERNAL_FILENAME);
		try (ObjectInputStream objIn = new ObjectInputStream(new FileInputStream(f))) {
			objIn.readInt(); // reads m_viewSpecs.length, this is redundant at the moment but might become useful
			// if stream.writeObject is too slow, we need to do something smarter
			for (int i = 0; i < m_viewSpecs.length; i++) {
				m_viewData[i] = new DLStaticLinePlotViewData<>(m_viewSpecs[i], (float[][]) objIn.readObject());
			}
			m_status.readExternal(objIn);
			m_status.setDataUpdate(m_viewData);
		} catch (final ClassNotFoundException e) {
			throw new IOException("View data could not be restored.");
		}
	}

	@Override
	protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		final File f = new File(nodeInternDir, INTERNAL_FILENAME);
		try (ObjectOutputStream objOut = new ObjectOutputStream(new FileOutputStream(f))) {
			objOut.writeInt(m_viewSpecs.length);
			// if stream.writeObject is too slow, we need to do something smarter
			for (int i = 0; i < m_viewSpecs.length; i++) {
				objOut.writeObject(m_viewData[i].asArray());
			}
			m_status.writeExternal(objOut);
		}
	}

	@Override
	protected void saveSettingsTo(final NodeSettingsWO settings) {
		try {
			m_generalCfg.copyClipSettingsToOptimizer();
			m_generalCfg.saveToSettings(settings);

			final NodeSettingsWO inputSettings = settings.addNodeSettings(CFG_KEY_INPUT);
			for (final DLKerasLearnerInputConfig inputCfg : m_inputCfgs.values()) {
				inputCfg.saveToSettings(inputSettings);
			}

			final NodeSettingsWO outputSettings = settings.addNodeSettings(CFG_KEY_TARGET);
			for (final DLKerasLearnerTargetConfig outputCfg : m_targetCfgs.values()) {
				outputCfg.saveToSettings(outputSettings);
			}
		} catch (final InvalidSettingsException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	@Override
	protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
		m_inputCfgs.clear();
		final NodeSettingsRO inputSettings = settings.getNodeSettings(CFG_KEY_INPUT);
		for (final String layerName : inputSettings) {
			final DLKerasLearnerInputConfig inputCfg = createInputTensorModelConfig(layerName, m_generalCfg);
			m_inputCfgs.put(layerName, inputCfg);
		}

		m_targetCfgs.clear();
		final NodeSettingsRO outputSettings = settings.getNodeSettings(CFG_KEY_TARGET);
		for (final String layerName : outputSettings) {
			final DLKerasLearnerTargetConfig outputCfg = createOutputTensorModelConfig(layerName, m_generalCfg);
			m_targetCfgs.put(layerName, outputCfg);
		}
	}

	@Override
	protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
		m_generalCfg.loadFromSettings(settings);
		m_generalCfg.copyClipSettingsToOptimizer();

		final NodeSettingsRO inputSettings = settings.getNodeSettings(CFG_KEY_INPUT);
		for (final DLKerasLearnerInputConfig inputCfg : m_inputCfgs.values()) {
			inputCfg.loadFromSettingsInModel(inputSettings);
		}

		final NodeSettingsRO outputSettings = settings.getNodeSettings(CFG_KEY_TARGET);
		for (final DLKerasLearnerTargetConfig outputCfg : m_targetCfgs.values()) {
			outputCfg.loadFromSettingsInModel(outputSettings);
		}

		m_lastConfiguredNetworkSpec = m_lastIncomingNetworkSpec;
		m_lastConfiguredTableSpec = m_lastIncomingTableSpec;
		m_initialLoaded = true;
	}

	@Override
	protected void reset() {
		for (int i = 0; i < m_viewData.length; i++) {
			m_viewData[i] = null;
		}
		m_status.setHasData(false);
		// reset views
		notifyViews(null);
	}

	private void configureGeneral(final Class<? extends DLNetwork> inNetworkType) throws Exception {
		DLKerasTrainingContext<?> backend = m_generalCfg.getTrainingContextEntry().getValue();
		if (backend == null) {
			final List<DLKerasTrainingContext<?>> availableBackends = DLKerasLearnerGeneralConfig
					.getAvailableTrainingContexts(inNetworkType).stream()
					.sorted(Comparator.comparing(DLTrainingContext::getName)) //
					.collect(Collectors.toList());
			if (availableBackends.isEmpty()) {
				throw new DLMissingDependencyException("No compatible training back end available. "
						+ "Are you missing a KNIME Deep Learning extension?");
			}
			backend = availableBackends.get(0);
			m_generalCfg.getTrainingContextEntry().setValue(backend);
		}
		if (!backend.getNetworkType().isAssignableFrom(inNetworkType)) {
			throw new InvalidSettingsException(
					"Selected training back end is not compatible to the input deep learning network. "
							+ "Please reconfigure the node.");
		}
		DLKerasOptimizer optimizer = m_generalCfg.getOptimizerEntry().getValue();
		if (optimizer == null) {
			final List<DLKerasOptimizer> availableOptimizers = backend.createOptimizers().stream() //
					.sorted(Comparator.comparing(DLKerasOptimizer::getName)) //
					.collect(Collectors.toList());
			if (availableOptimizers.isEmpty()) {
				throw new DLMissingDependencyException(
						"No compatible optimizers available. " + "Are you missing a KNIME Deep Learning extension?");
			}
			optimizer = availableOptimizers.get(0);
			m_generalCfg.getOptimizerEntry().setValue(optimizer);
		}
		m_generalCfg.copyClipSettingsToOptimizer();
	}

	private void configureInputs(final DLNetworkSpec inNetworkSpec, final DataTableSpec inTableSpec)
			throws InvalidSettingsException {
		if (inTableSpec.getNumColumns() == 0) {
			setWarningMessage("Input table has no columns. Output network will equal input network.");
		}
		final DLTensorSpec[] inputSpecs = inNetworkSpec.getInputSpecs();
		final DLTensorSpec[] targetSpecs = inNetworkSpec.getOutputSpecs();
		m_converters = new LinkedHashMap<>(inputSpecs.length + targetSpecs.length);
		if (inputSpecs.length == 0) {
			setWarningMessage("Input deep learning network has no input specs.");
		}
		for (final DLTensorSpec tensorSpec : inputSpecs) {
			final DLKerasLearnerInputConfig inputCfg = m_inputCfgs.computeIfAbsent(tensorSpec.getName(),
					name -> DLKerasLearnerNodeModel.createInputTensorModelConfig(name, m_generalCfg));
			// validate layer spec
			if (!DLUtils.Shapes.isFixed(tensorSpec.getShape())) {
				throw new InvalidSettingsException("Input '" + tensorSpec.getName()
						+ "' has an (at least partially) unknown shape. This is not supported, yet.");
			}
			// get selected converter
			DLDataValueToTensorConverterFactory<?, ?> converter = inputCfg.getConverterEntry().getValue();
			if (converter == null) {
				final Collection<DLDataValueToTensorConverterFactory<?, ?>> availableConverters = DLKerasLearnerInputConfig
						.getAvailableConverters(m_generalCfg.getTrainingContextEntry().getValue(), inTableSpec,
								tensorSpec);
				if (availableConverters.isEmpty()) {
					throw new InvalidSettingsException(
							"No converters available for input '" + tensorSpec.getName() + "'.");
				}
				final Set<DLDataValueToTensorConverterFactory<?, ?>> builtInElement = new HashSet<>(1);
				final Set<DLDataValueToTensorConverterFactory<?, ?>> builtInCollection = new HashSet<>(1);
				final Set<DLDataValueToTensorConverterFactory<?, ?>> extensionElement = new HashSet<>(1);
				final Set<DLDataValueToTensorConverterFactory<?, ?>> extensionCollection = new HashSet<>(1);
				for (final DLDataValueToTensorConverterFactory<?, ?> conv : availableConverters) {
					if (conv.getClass().getCanonicalName().contains("org.knime.dl.core.data.convert")) {
						if (conv instanceof DLCollectionDataValueToTensorConverterFactory) {
							builtInCollection.add(conv);
						} else {
							builtInElement.add(conv);
						}
					} else {
						if (conv instanceof DLCollectionDataValueToTensorConverterFactory) {
							extensionCollection.add(conv);
						} else {
							extensionElement.add(conv);
						}
					}
				}
				final Comparator<DLDataValueToTensorConverterFactory<?, ?>> nameComparator = Comparator
						.comparing(DLDataValueToTensorConverterFactory::getName);
				final List<DLDataValueToTensorConverterFactory<?, ?>> availableConvertersSorted = Stream.concat(
						Stream.concat(builtInElement.stream().sorted(nameComparator),
								extensionElement.stream().sorted(nameComparator)),
						Stream.concat(builtInCollection.stream().sorted(nameComparator),
								extensionCollection.stream().sorted(nameComparator)))
						.collect(Collectors.toList());
				converter = availableConvertersSorted.get(0);
				inputCfg.getConverterEntry().setValue(converter);
			}
			m_converters.put(tensorSpec, converter);
			final DataColumnSpecFilterConfiguration filterConfig = inputCfg.getInputColumnsEntry().getValue();
			((DLDataTypeColumnFilter) filterConfig.getFilter()).setFilterClasses(converter.getSourceType());
			// check if selected columns are still in input table
			if (m_lastConfiguredTableSpec != null) {
				final String[] missingColumns = filterConfig.applyTo(inTableSpec).getRemovedFromIncludes();
				if (missingColumns.length != 0) {
					throw new InvalidSettingsException(
							"Selected column '" + missingColumns[0] + "' of input '" + tensorSpec.getName()
									+ "' is missing in the node's input table. Please reconfigure the node.");
				}
			}
			// TODO: check column selection (see dialog)!
		}
		if (targetSpecs.length == 0) {
			setWarningMessage("Input deep learning network has no target specs.");
		}
		for (final DLTensorSpec tensorSpec : targetSpecs) {
			final DLKerasLearnerTargetConfig targetCfg = m_targetCfgs.computeIfAbsent(tensorSpec.getName(),
					name -> DLKerasLearnerNodeModel.createOutputTensorModelConfig(name, m_generalCfg));
			// validate layer spec
			if (!DLUtils.Shapes.isFixed(tensorSpec.getShape())) {
				throw new InvalidSettingsException("Target '" + tensorSpec.getName()
						+ "' has an (at least partially) unknown shape. This is not supported, yet.");
			}
			// get selected converter
			DLDataValueToTensorConverterFactory<?, ?> converter = targetCfg.getConverterEntry().getValue();
			if (converter == null) {
				final Collection<DLDataValueToTensorConverterFactory<?, ?>> availableConverters = DLKerasLearnerTargetConfig
						.getAvailableConverters(m_generalCfg.getTrainingContextEntry().getValue(), inTableSpec,
								tensorSpec);
				if (availableConverters.isEmpty()) {
					throw new InvalidSettingsException(
							"No converters available for target '" + tensorSpec.getName() + "'.");
				}
				final Set<DLDataValueToTensorConverterFactory<?, ?>> builtInElement = new HashSet<>(1);
				final Set<DLDataValueToTensorConverterFactory<?, ?>> builtInCollection = new HashSet<>(1);
				final Set<DLDataValueToTensorConverterFactory<?, ?>> extensionElement = new HashSet<>(1);
				final Set<DLDataValueToTensorConverterFactory<?, ?>> extensionCollection = new HashSet<>(1);
				for (final DLDataValueToTensorConverterFactory<?, ?> conv : availableConverters) {
					if (conv.getClass().getCanonicalName().contains("org.knime.dl.core.data.convert")) {
						if (conv instanceof DLCollectionDataValueToTensorConverterFactory) {
							builtInCollection.add(conv);
						} else {
							builtInElement.add(conv);
						}
					} else {
						if (conv instanceof DLCollectionDataValueToTensorConverterFactory) {
							extensionCollection.add(conv);
						} else {
							extensionElement.add(conv);
						}
					}
				}
				final Comparator<DLDataValueToTensorConverterFactory<?, ?>> nameComparator = Comparator
						.comparing(DLDataValueToTensorConverterFactory::getName);
				final List<DLDataValueToTensorConverterFactory<?, ?>> availableConvertersSorted = Stream.concat(
						Stream.concat(builtInElement.stream().sorted(nameComparator),
								extensionElement.stream().sorted(nameComparator)),
						Stream.concat(builtInCollection.stream().sorted(nameComparator),
								extensionCollection.stream().sorted(nameComparator)))
						.collect(Collectors.toList());
				converter = availableConvertersSorted.get(0);
				targetCfg.getConverterEntry().setValue(converter);
			}
			m_converters.put(tensorSpec, converter);
			final DataColumnSpecFilterConfiguration filterConfig = targetCfg.getInputColumnsEntry().getValue();
			((DLDataTypeColumnFilter) filterConfig.getFilter()).setFilterClasses(converter.getSourceType());
			// check if selected columns are still in input table
			if (m_lastConfiguredTableSpec != null) {
				final String[] missingColumns = filterConfig.applyTo(inTableSpec).getRemovedFromIncludes();
				if (missingColumns.length != 0) {
					throw new InvalidSettingsException(
							"Selected column '" + missingColumns[0] + "' of target '" + tensorSpec.getName()
									+ "' is missing in the node's input table. Please reconfigure the node.");
				}
			}
			// TODO: check column selection (see dialog)!
			DLKerasLossFunction lossFunction = targetCfg.getLossFunctionEntry().getValue();
			if (lossFunction == null) {
				final List<DLKerasLossFunction> availableLossFunctions = m_generalCfg.getTrainingContextEntry()
						.getValue().createLossFunctions().stream() //
						.sorted(Comparator.comparing(DLKerasLossFunction::getName)) //
						.collect(Collectors.toList());
				if (availableLossFunctions.isEmpty()) {
					throw new InvalidSettingsException("No loss functions available for target '" + tensorSpec.getName()
							+ "' (with training context '" + m_generalCfg.getTrainingContextEntry().getValue().getName()
							+ "').");
				}
				lossFunction = availableLossFunctions.get(0);
				targetCfg.getLossFunctionEntry().setValue(lossFunction);
			}
		}
	}

	private DLNetworkPortObjectSpec createOutputSpec(final DLNetworkPortObjectSpec inPortObjectSpec)
			throws InvalidSettingsException {
		// TODO: create new network spec with updated training config
		return inPortObjectSpec;
	}

	@SuppressWarnings("unchecked")
	private <N extends DLKerasNetwork> PortObject executeInternal(final PortObject inPortObject,
			final BufferedDataTable inTable, final ExecutionContext exec) throws Exception {

		final N inNetwork = (N) ((DLNetworkPortObject) inPortObject).getNetwork();
		final DLKerasNetworkSpec inNetworkSpec = inNetwork.getSpec();
		final DataTableSpec inTableSpec = inTable.getDataTableSpec();

		if (inTableSpec.getNumColumns() == 0 || inTable.size() == 0) {
			setWarningMessage("Input table is empty. Output network equals input network.");
			return inPortObject;
		}

		final DLKerasTrainingContext<N> ctx = (DLKerasTrainingContext<N>) m_generalCfg.getTrainingContextEntry()
				.getValue();

		// training configuration
		final int batchSize = m_generalCfg.getBatchSizeEntry().getValue();
		final int epochs = m_generalCfg.getEpochsEntry().getValue();
		final DLKerasOptimizer optimizer = m_generalCfg.getOptimizerEntry().getValue();
		final Map<DLTensorSpec, DLKerasLossFunction> lossFunctions = new HashMap<>();
		for (final DLTensorSpec targetSpec : inNetworkSpec.getOutputSpecs()) {
			final DLKerasLossFunction lossFunction = m_targetCfgs.get(targetSpec.getName()).getLossFunctionEntry()
					.getValue();
			lossFunctions.put(targetSpec, lossFunction);
		}
		final ArrayList<DLKerasCallback> callbacks = new ArrayList<>(3);
		if (m_generalCfg.getTerminateOnNaNEntry().getEnabled()) {
			callbacks.add(m_generalCfg.getTerminateOnNaNEntry().getValue());
		}
		if (m_generalCfg.getEarlyStoppingEntry().getEnabled()) {
			callbacks.add(m_generalCfg.getEarlyStoppingEntry().getValue());
		}
		if (m_generalCfg.getReduceLROnPlateauEntry().getEnabled()) {
			callbacks.add(m_generalCfg.getReduceLROnPlateauEntry().getValue());
		}
		final DLKerasTrainingConfig trainingConfig = new DLKerasDefaultTrainingConfig(batchSize, epochs, optimizer,
				lossFunctions, callbacks);

		final Map<DLTensorId, int[]> columnsForTensorId = new HashMap<>(
				inNetworkSpec.getInputSpecs().length + inNetworkSpec.getOutputSpecs().length);
		final LinkedHashMap<DLTensorId, DLDataValueToTensorConverterFactory<?, ?>> converterForTensorId = new LinkedHashMap<>(
				columnsForTensorId.size());
		final LinkedHashSet<DLTensorSpec> executionInputSpecs = new LinkedHashSet<>(
				inNetworkSpec.getInputSpecs().length);
		for (final Entry<DLTensorSpec, DLDataValueToTensorConverterFactory<?, ?>> entry : m_converters.entrySet()) {
			final DLTensorSpec spec = entry.getKey();
			final DataColumnSpecFilterConfiguration filterConfig;
			final DLKerasLearnerInputConfig inputCfg = m_inputCfgs.get(spec.getName());
			if (inputCfg != null) {
				filterConfig = inputCfg.getInputColumnsEntry().getValue();
			} else {
				filterConfig = m_targetCfgs.get(spec.getName()).getInputColumnsEntry().getValue();
			}
			((DLDataTypeColumnFilter) filterConfig.getFilter()).setFilterClasses(entry.getValue().getSourceType());
			// the input columns that will be used to fill the current spec's tensor
			final int[] indices = Arrays.stream(filterConfig.applyTo(inTableSpec).getIncludes()).mapToInt(column -> {
				final int idx = inTableSpec.findColumnIndex(column);
				if (idx == -1) {
					throw new IllegalStateException("Selected input/target column '" + column
							+ "' could not be found in the node's input table.");
				}
				return idx;
			}).toArray();
			columnsForTensorId.put(spec.getIdentifier(), indices);
			converterForTensorId.put(spec.getIdentifier(), entry.getValue());

			// TODO: execution shape inference
			executionInputSpecs.add(ctx.getTensorFactory().createExecutionTensorSpec(spec, batchSize,
					DLUtils.Shapes.getFixedShape(spec.getShape())
							.orElseThrow(() -> new RuntimeException("execution shape inference not yet implemented"))));
		}

		// TODO: only valid if we don't crop the last batch. This has to be considered if we want to add 'crop' as an
		// alternative strategy for handling incomplete batches.
		final int numBatchesPerEpoch = (int) Math.ceil(inTable.size() / (double) batchSize);
		final int totalNumBatches = epochs * numBatchesPerEpoch;
		m_viewData[0] = new DLUpdatableLinePlotViewData<>(m_viewSpecs[0], totalNumBatches);
		m_viewData[1] = new DLUpdatableLinePlotViewData<>(m_viewSpecs[1], totalNumBatches);

		try (final DLKnimeNetworkTrainingInputPreparer inputPreparer = new DLKnimeNetworkTrainingInputPreparer(
				new DLDataTableRowIterator(inTable, columnsForTensorId), batchSize, converterForTensorId);
				final DLKerasNetworkTrainingSession session = ctx.createTrainingSession(inNetwork, trainingConfig,
						executionInputSpecs, inputPreparer);) {
			final DLKnimeTrainingMonitor<DLKerasTrainingStatus> monitor = new DLKnimeTrainingMonitor<>(exec, m_status);
			m_status.setNumEpochs(epochs);
			m_status.setNumBatchesPerEpoch(numBatchesPerEpoch);
			m_status.setStatus(Status.RUNNING);
			m_status.setHasData(true);
			m_status.setStartDateTime(null);
			m_status.setEndDateTime(null);
			m_status.setCurrentEpoch(0);
			m_status.setCurrentBatchInEpoch(0);
			m_status.setDataUpdate(m_viewData);
			final AtomicInteger currentEpoch = new AtomicInteger();
			final AtomicInteger currentBatchInEpoch = new AtomicInteger();
			m_status.trainingStarted().addListener((src, v) -> m_status.setStartDateTime(LocalDateTime.now()));
			m_status.trainingEnded().addListener((src, v) -> {
				m_status.setEndDateTime(LocalDateTime.now());
				// there might be a significant time difference between last batch end and training end, so let's update
				// the view one more time
				notifyViews(m_status);
			});
			m_status.batchEnded().addListener((src, v) -> {
				if (currentBatchInEpoch.get() + 1 == numBatchesPerEpoch) {
					m_status.setCurrentEpoch(currentEpoch.incrementAndGet());
					if (currentEpoch.get() < epochs) {
						currentBatchInEpoch.set(0);
					} else {
						currentBatchInEpoch.set(numBatchesPerEpoch);
					}
					m_status.setCurrentBatchInEpoch(currentBatchInEpoch.get());
				} else {
					m_status.setCurrentBatchInEpoch(currentBatchInEpoch.incrementAndGet());
				}
				// update accuracy
				final Map<String, DLMetrics> metrics = m_status.getMetrics();
				((DLUpdatableLinePlotViewData<?>) m_viewData[0]).add(metrics.get("accuracy").getValue());
				// update loss
				((DLUpdatableLinePlotViewData<?>) m_viewData[1]).add(metrics.get("loss").getValue());
				notifyViews(m_status);
			});
			session.run(monitor);
			m_status.setStatus(Status.FINISHED);
			exec.setMessage("Saving trained Keras deep learning network...");
			return session.getTrainedNetwork(exec);
		} catch (final CanceledExecutionException | DLCanceledExecutionException e) {
			m_status.setStatus(Status.USER_INTERRUPTED);
			throw e;
		} catch (final Exception e) {
			final Throwable cause = e.getCause();
			if (cause != null) {
				if (cause instanceof CanceledExecutionException) {
					m_status.setStatus(Status.USER_INTERRUPTED);
					throw (CanceledExecutionException) cause;
				} else if (cause instanceof DLCanceledExecutionException) {
					m_status.setStatus(Status.USER_INTERRUPTED);
					throw new CanceledExecutionException(e.getMessage());
				}
			}
			String message;
			if (e instanceof DLException) {
				message = e.getMessage();
			} else {
				if (!Strings.isNullOrEmpty(e.getMessage())) {
					LOGGER.error(e.getMessage());
				}
				message = "An error occured during training of the Keras deep learning network. See log for details.";
			}
			m_status.setStatus(Status.EXCEPTION);
			throw new RuntimeException(message, e);
		}
	}
}
