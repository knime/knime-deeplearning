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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.stream.Collectors;

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
import org.knime.dl.base.nodes.DLConfigurationUtility;
import org.knime.dl.base.portobjects.DLNetworkPortObject;
import org.knime.dl.base.portobjects.DLNetworkPortObjectSpec;
import org.knime.dl.base.settings.ConfigEntry;
import org.knime.dl.base.settings.DLDataTypeColumnFilter;
import org.knime.dl.core.DLCanceledExecutionException;
import org.knime.dl.core.DLDataTableRowIterator;
import org.knime.dl.core.DLException;
import org.knime.dl.core.DLExecutionSpecCreator;
import org.knime.dl.core.DLMissingDependencyException;
import org.knime.dl.core.DLNetwork;
import org.knime.dl.core.DLNetworkSpec;
import org.knime.dl.core.DLRowIterator;
import org.knime.dl.core.DLShuffleDataTableRowIterator;
import org.knime.dl.core.DLTensorId;
import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.core.data.convert.DLDataValueToTensorConverterFactory;
import org.knime.dl.core.training.DLKnimeNetworkTrainingInputPreparer;
import org.knime.dl.core.training.DLKnimeNetworkValidationInputPreparer;
import org.knime.dl.core.training.DLKnimeTrainingMonitor;
import org.knime.dl.core.training.DLTrainingContext;
import org.knime.dl.core.training.DLTrainingStatus.Status;
import org.knime.dl.keras.base.nodes.learner.view.DLDefaultLinePlotViewDataCollection;
import org.knime.dl.keras.base.nodes.learner.view.DLDenseLinePlotViewData;
import org.knime.dl.keras.base.nodes.learner.view.DLInteractiveLearnerNodeModel;
import org.knime.dl.keras.base.nodes.learner.view.DLLinePlotViewDataCollection;
import org.knime.dl.keras.base.nodes.learner.view.DLProgressMonitor;
import org.knime.dl.keras.base.nodes.learner.view.DLSparseLinePlotViewData;
import org.knime.dl.keras.base.nodes.learner.view.jfreechart.DLDefaultJFreeChartLinePlotViewSpec;
import org.knime.dl.keras.base.nodes.learner.view.jfreechart.DLJFreeChartLinePlotViewSpec;
import org.knime.dl.keras.base.portobjects.DLKerasNetworkPortObjectBase;
import org.knime.dl.keras.base.portobjects.DLKerasNetworkPortObjectSpecBase;
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

import com.google.common.base.Strings;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class DLKerasLearnerNodeModel extends NodeModel implements DLInteractiveLearnerNodeModel {

	static final int IN_NETWORK_PORT_IDX = 0;

	static final int IN_DATA_PORT_IDX = 1;

	static final int IN_VALIDATION_DATA_PORT_IDX = 2;

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

	/**
	 * <code>null</code> by default, will be populated during execution of the node or when loading an executed node
	 */
	private DLKerasDefaultTrainingStatus m_status;

	/**
	 * <code>null</code> by default, will be populated during execution of the node or when loading an executed node
	 */
	private DLJFreeChartLinePlotViewSpec[] m_viewSpecs;

	/**
	 * <code>null</code> by default, will be populated during execution of the node or when loading an executed node
	 */
	private DLLinePlotViewDataCollection[] m_viewData;

	DLKerasLearnerNodeModel() {
		super(new PortType[] { DLKerasNetworkPortObjectBase.TYPE, BufferedDataTable.TYPE, BufferedDataTable.TYPE_OPTIONAL },
				new PortType[] { DLKerasNetworkPortObjectBase.TYPE });
		m_generalCfg = createGeneralModelConfig();
		m_inputCfgs = new HashMap<>();
		m_targetCfgs = new HashMap<>();
	}

	@Override
	public DLProgressMonitor getProgressMonitor() {
		return m_status;
	}

	@Override
	public void stopLearning() {
		if (m_status != null) {
			m_status.setStatus(Status.STOPPED_EARLY);
		}
	}

	@Override
	protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
		if (inSpecs[DLKerasLearnerNodeModel.IN_NETWORK_PORT_IDX] == null) {
			throw new InvalidSettingsException("Input deep learning network is missing.");
		}
		if (inSpecs[DLKerasLearnerNodeModel.IN_DATA_PORT_IDX] == null) {
			throw new InvalidSettingsException("Input data table is missing.");
		}
		if (!DLKerasNetworkPortObjectBase.TYPE
				.acceptsPortObjectSpec(inSpecs[DLKerasLearnerNodeModel.IN_NETWORK_PORT_IDX])) {
			throw new InvalidSettingsException(
					"Input port object is not a valid Keras deep learning network port object.");
		}

		final DLKerasNetworkPortObjectSpecBase inPortObjectSpec = ((DLKerasNetworkPortObjectSpecBase) inSpecs[IN_NETWORK_PORT_IDX]);
		final DLKerasNetworkSpec inNetworkSpec = inPortObjectSpec.getNetworkSpec();
		final Class<? extends DLNetwork> inNetworkType = inPortObjectSpec.getNetworkType();
		final DataTableSpec inTableSpec = (DataTableSpec) inSpecs[IN_DATA_PORT_IDX];
		final DataTableSpec inValidationTableSpec = (DataTableSpec) inSpecs[IN_VALIDATION_DATA_PORT_IDX];

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
			final DLTensorSpec[] inputSpecs = inNetworkSpec.getInputSpecs();
	        final DLTensorSpec[] targetSpecs = inNetworkSpec.getOutputSpecs();
	        final DLKerasTrainingContext<?> trainingContext = m_generalCfg.getTrainingContext().getValue();
	        m_converters = new LinkedHashMap<>(inputSpecs.length + targetSpecs.length);
			configureInputs(inTableSpec, inValidationTableSpec, trainingContext, inputSpecs);
			configureTargets(inTableSpec, trainingContext, targetSpecs);
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
		final BufferedDataTable inValidationTable = (BufferedDataTable) inObjects[IN_VALIDATION_DATA_PORT_IDX];

		final PortObject outPortObject = executeInternal(inPortObject, inTable, inValidationTable, exec);

		return new PortObject[] { outPortObject };
	}

	@Override
	protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		final File f = new File(nodeInternDir, INTERNAL_FILENAME);
		try (final ObjectInputStream objIn = new ObjectInputStream(new FileInputStream(f))) {
			m_status = new DLKerasDefaultTrainingStatus();
			m_status.readExternal(objIn);
			final int numViewTabs = objIn.readInt();
			m_viewSpecs = new DLJFreeChartLinePlotViewSpec[numViewTabs];
			m_viewData = new DLLinePlotViewDataCollection[numViewTabs];
			for (int i = 0; i < numViewTabs; i++) {
				m_viewSpecs[i] = new DLDefaultJFreeChartLinePlotViewSpec();
				m_viewSpecs[i].readExternal(objIn);
				m_viewData[i] = new DLDefaultLinePlotViewDataCollection<>(m_viewSpecs[i]);
				m_viewData[i].readExternal(objIn);
			}
			m_status.setViewSpecs(m_viewSpecs);
			m_status.setViewData(m_viewData);
		} catch (final ClassNotFoundException e) {
			throw new IOException("View data could not be restored.");
		}
	}

	@Override
	protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		if (m_status == null) {
			throw new IllegalStateException(
					"Training status may not be null after node execution. This is an implementation error.");
		}
		final File f = new File(nodeInternDir, INTERNAL_FILENAME);
		try (ObjectOutputStream objOut = new ObjectOutputStream(new FileOutputStream(f))) {
			m_status.writeExternal(objOut);
			final int numViewTabs = m_viewSpecs != null ? m_viewSpecs.length : 0;
			objOut.writeInt(numViewTabs);
			for (int i = 0; i < numViewTabs; i++) {
				m_viewSpecs[i].writeExternal(objOut);
				m_viewData[i].writeExternal(objOut);
			}
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
		if (m_status != null) {
			m_status.setViewSpecs(null);
			m_status.setViewData(null);
		}
		if (m_viewSpecs != null) {
			for (int i = 0; i < m_viewSpecs.length; i++) {
				m_viewSpecs[i] = null;
			}
		}
		if (m_viewData != null) {
			for (int i = 0; i < m_viewData.length; i++) {
				m_viewData[i] = null;
			}
		}
		// reset views
		notifyViews(null);
	}

	private void configureGeneral(final Class<? extends DLNetwork> inNetworkType) throws Exception {
		final DLKerasTrainingContext<?> backend = configureBackend(inNetworkType);
		configureOptimizer(backend);
	}

    private void configureOptimizer(final DLKerasTrainingContext<?> backend) throws DLMissingDependencyException {
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

    private DLKerasTrainingContext<?> configureBackend(final Class<? extends DLNetwork> inNetworkType)
        throws DLMissingDependencyException, InvalidSettingsException {
        DLKerasTrainingContext<?> backend = m_generalCfg.getContextEntry().getValue();
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
			m_generalCfg.getContextEntry().setValue(backend);
		}
        if (!inNetworkType.isAssignableFrom(backend.getNetworkType())) {
            throw new InvalidSettingsException(
                "Selected training back end is not compatible to the input deep learning network. "
                    + "Please reconfigure the node.");
        }
        return backend;
    }


	private void configureInputs(final DataTableSpec inTableSpec, final DataTableSpec inValidationTableSpec,
			final DLKerasTrainingContext<?> trainingContext, final DLTensorSpec[] inputSpecs) throws InvalidSettingsException {
		if (inTableSpec.getNumColumns() == 0) {
			setWarningMessage("Training data table has no columns. Output network will equal input network.");
		}
		// TODO: We could relax the check and only enforce that columns selected in the input/target panels are present
		// in the validation spec. However, in practice, input and validation table tend to have equal structures
		// anyway. Just note that we then need two different maps (tensor id -> column indices) for training and
		// validation data.
		if (inValidationTableSpec != null && !inValidationTableSpec.equalStructure(inTableSpec)) {
			throw new InvalidSettingsException("Validation data table structure differs from training data table "
					+ "structure. Please make sure that both tables have exactly the same column names and types in "
					+ "the same order.");
		}

		if (inputSpecs.length == 0) {
			setWarningMessage("Input deep learning network has no input specs.");
		}

		for (final DLTensorSpec tensorSpec : inputSpecs) {
			final DLKerasLearnerInputConfig inputCfg = m_inputCfgs.computeIfAbsent(tensorSpec.getName(),
					name -> DLKerasLearnerNodeModel.createInputTensorModelConfig(name, m_generalCfg));
			DLDataValueToTensorConverterFactory<?, ?> converter =
                DLConfigurationUtility.configureInput(inputCfg, tensorSpec, trainingContext, inTableSpec,
                    m_lastConfiguredTableSpec, "Input");
			m_converters.put(tensorSpec, converter);
		}
	}

    private void configureTargets(final DataTableSpec inTableSpec, final DLKerasTrainingContext<?> trainingContext,
        final DLTensorSpec[] targetSpecs) throws InvalidSettingsException {
        if (targetSpecs.length == 0) {
			setWarningMessage("Input deep learning network has no target specs.");
		}
		for (final DLTensorSpec tensorSpec : targetSpecs) {
			final DLKerasLearnerTargetConfig targetCfg = m_targetCfgs.computeIfAbsent(tensorSpec.getName(),
					name -> DLKerasLearnerNodeModel.createOutputTensorModelConfig(name, m_generalCfg));
			// get selected converter
			DLDataValueToTensorConverterFactory<?, ?> converter = DLConfigurationUtility.configureInput(
			    targetCfg, tensorSpec, trainingContext, inTableSpec, m_lastConfiguredTableSpec, "Target");
			if (m_converters.containsKey(tensorSpec)) {
			    checkConverterEquality(tensorSpec, converter);
			}
			m_converters.put(tensorSpec, converter);
			setLossFunction(tensorSpec, targetCfg);
		}
    }
    
    private void checkConverterEquality(DLTensorSpec tensorSpec, DLDataValueToTensorConverterFactory<?, ?> converter) {
        DLDataValueToTensorConverterFactory<?, ?> alreadyConfiguredConverter = m_converters.get(tensorSpec);
        if (!converter.equals(alreadyConfiguredConverter)) {
            throw new IllegalStateException("You use the tensor '" 
                    + tensorSpec.getName() 
                    + "' both as input and target but with different converters ('"
                    + alreadyConfiguredConverter.getIdentifier() 
                    + "' and '" 
                    + converter.getIdentifier() 
                    + "'. This is not supported.");
        }
    }

    private void setLossFunction(final DLTensorSpec tensorSpec, final DLKerasLearnerTargetConfig targetCfg)
        throws InvalidSettingsException {
        DLKerasLossFunction lossFunction = targetCfg.getLossFunctionEntry().getValue();
        if (lossFunction == null) {
        	final List<DLKerasLossFunction> availableLossFunctions = m_generalCfg.getContextEntry()
        			.getValue().createLossFunctions().stream() //
        			.sorted(Comparator.comparing(DLKerasLossFunction::getName)) //
        			.collect(Collectors.toList());
        	if (availableLossFunctions.isEmpty()) {
        		throw new InvalidSettingsException("No loss functions available for target '" + tensorSpec.getName()
        				+ "' (with training context '" + m_generalCfg.getContextEntry().getValue().getName()
        				+ "').");
        	}
        	lossFunction = availableLossFunctions.get(0);
        	targetCfg.getLossFunctionEntry().setValue(lossFunction);
        }
    }

	private static DLNetworkPortObjectSpec createOutputSpec(final DLNetworkPortObjectSpec inPortObjectSpec) {
		// TODO: create new network spec with updated training config
		return inPortObjectSpec;
	}

	@SuppressWarnings("unchecked")
	private <N extends DLKerasNetwork> PortObject executeInternal(final PortObject inPortObject,
			final BufferedDataTable inTable, final BufferedDataTable inValidationTable, final ExecutionContext exec)
			throws Exception {
		final N inNetwork = (N) ((DLNetworkPortObject) inPortObject).getNetwork();
		final DLKerasNetworkSpec inNetworkSpec = inNetwork.getSpec();
		final DataTableSpec inTableSpec = inTable.getDataTableSpec();

		if (inTableSpec.getNumColumns() == 0 || inTable.size() == 0) {
			setWarningMessage("Training data table is empty. Output network equals input network.");
			return inPortObject;
		}

		final boolean doValidation = doValidation(inValidationTable);

		final DLKerasTrainingContext<N> ctx = (DLKerasTrainingContext<N>) m_generalCfg.getContextEntry()
				.getValue();

		// training configuration
		final DLKerasTrainingConfig trainingConfig = createTrainingConfig(inNetworkSpec);

		final Map<DLTensorId, int[]> columnsForTensorId = new HashMap<>(
				inNetworkSpec.getInputSpecs().length + inNetworkSpec.getOutputSpecs().length);
		final LinkedHashMap<DLTensorId, DLDataValueToTensorConverterFactory<?, ?>> converterForTensorId = new LinkedHashMap<>(
				columnsForTensorId.size());
		fillInputSpecificMaps(inTableSpec, columnsForTensorId, converterForTensorId);

		// TODO: only valid if we don't crop the last batch. This has to be considered if we want to add 'crop' as an
		// alternative strategy for handling incomplete batches.
		final int numTrainingBatchesPerEpoch = (int) Math.ceil(inTable.size() / (double) trainingConfig.getBatchSize());
		final int totalNumTrainingBatches = trainingConfig.getEpochs() * numTrainingBatchesPerEpoch;
		final int numBatchesPerValidation = doValidation
				? (int) Math.ceil(inValidationTable.size() / (double) trainingConfig.getValidationBatchSize())
				: 0;
		final int totalNumValidationBatches = trainingConfig.getEpochs() * numBatchesPerValidation;

		prepareView(doValidation, totalNumTrainingBatches, totalNumValidationBatches);

		final Random random = createRandom();

		m_status = new DLKerasDefaultTrainingStatus(trainingConfig.getEpochs(), numTrainingBatchesPerEpoch);
		try (final DLRowIterator rowIterator = createRowIterator(inTable, columnsForTensorId, random, exec);
				final DLKnimeNetworkTrainingInputPreparer inputPreparer = new DLKnimeNetworkTrainingInputPreparer(
						rowIterator, (int)trainingConfig.getBatchSize(), converterForTensorId);
				final DLKnimeNetworkValidationInputPreparer validationPreparer = doValidation
						? new DLKnimeNetworkValidationInputPreparer(
								new DLDataTableRowIterator(inValidationTable, columnsForTensorId), (int)trainingConfig.getValidationBatchSize(),
								converterForTensorId)
						: null;
				final DLKerasNetworkTrainingSession session = ctx.createTrainingSession(inNetwork, trainingConfig,
						DLExecutionSpecCreator.createExecutionSpecs(rowIterator.peek(), ctx.getTensorFactory(),
								trainingConfig.getBatchSize(), columnsForTensorId, m_converters),
						inputPreparer, validationPreparer);) {
			final DLKnimeTrainingMonitor<DLKerasTrainingStatus> monitor = new DLKnimeTrainingMonitor<>(exec, m_status);
			setupTrainingStatus(doValidation, trainingConfig, numTrainingBatchesPerEpoch, totalNumTrainingBatches,
                monitor);
			session.run(monitor);
			exec.setMessage("Saving trained Keras deep learning network...");
			return session.getTrainedNetwork(exec);
		} catch (final CanceledExecutionException | DLCanceledExecutionException e) {
			m_status.setStatus(Status.USER_INTERRUPTED);
			throw e;
		} catch (final Exception e) {
			throw handleGeneralException(e);
		}
	}

    private RuntimeException handleGeneralException(final Exception e) throws CanceledExecutionException {
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
        return new RuntimeException(message, e);
    }

    private void setupTrainingStatus(final boolean doValidation, final DLKerasTrainingConfig trainingConfig,
        final int numTrainingBatchesPerEpoch, final int totalNumTrainingBatches,
        final DLKnimeTrainingMonitor<DLKerasTrainingStatus> monitor) {
        m_status.setViewSpecs(m_viewSpecs);
        m_status.setViewData(m_viewData);
        m_status.trainingEnded().addListener((src, v) -> {
        	try {
        		notifyViews(m_status);
        	} catch (final Exception e) {
        		LOGGER.warn("An error occurred while updating the learner's view. "
        				+ "The actual learning process remains unaffected.", e);
        	}
        });
        m_status.epochStarted().addListener((src, v) -> {
        	try {
        		notifyViews(m_status);
        	} catch (final Exception e) {
        		LOGGER.warn("An error occurred while updating the learner's view. "
        				+ "The actual learning process remains unaffected.", e);
        	}
        });
        m_status.epochEnded().addListener((src, metrics) -> {
        	if (doValidation) {
        		final int currentBatch = m_status.getCurrentEpoch() * numTrainingBatchesPerEpoch
        				+ m_status.getCurrentBatchInEpoch();
        		// update view
        		final DLSparseLinePlotViewData accuracyPlot = (DLSparseLinePlotViewData) m_viewData[0].get(1);
        		accuracyPlot.getDataX().add(currentBatch);
        		accuracyPlot.getDataY().add(metrics.get("val_accuracy").getValue());
        		final DLSparseLinePlotViewData lossPlot = (DLSparseLinePlotViewData) m_viewData[1].get(1);
        		lossPlot.getDataX().add(currentBatch);
        		lossPlot.getDataY().add(metrics.get("val_loss").getValue());
        		try {
        			notifyViews(m_status);
        		} catch (final Exception e) {
        			LOGGER.warn("An error occurred while updating the learner's view. "
        					+ "The actual learning process remains unaffected.", e);
        		}
        	}
        });
        m_status.batchStarted().addListener((src, v) -> {
        	// update progress
        	final int currentBatch = m_status.getCurrentBatchInEpoch() + 1;
        	final int currentEpoch = m_status.getCurrentEpoch() + 1;
        	final double progress = ((currentEpoch - 1) * numTrainingBatchesPerEpoch + currentBatch)
        			/ (double) totalNumTrainingBatches;
        	monitor.setProgress(progress, "Processing batch " + currentBatch + " of " + numTrainingBatchesPerEpoch
        			+ " in epoch " + currentEpoch + " of " + trainingConfig.getEpochs() + "...");
        });
        m_status.batchEnded().addListener((src, metrics) -> {
        	// update view
        	((DLDenseLinePlotViewData) m_viewData[0].get(0)).getDataY().add(metrics.get("accuracy").getValue());
        	((DLDenseLinePlotViewData) m_viewData[1].get(0)).getDataY().add(metrics.get("loss").getValue());
        	try {
        		notifyViews(m_status);
        	} catch (final Exception e) {
        		LOGGER.warn("An error occurred while updating the learner's view. "
        				+ "The actual learning process remains unaffected.", e);
        	}
        });
        m_status.validationStarted().addListener((src, v) -> monitor.setMessage(
        		"Validating model in epoch " + (m_status.getCurrentEpoch() + 1) + " of " + trainingConfig.getEpochs() + "..."));
        if (m_generalCfg.getEarlyStoppingEntry().getEnabled()) {
        	m_status.stoppedEarly()
        			.addListener((src,
        					epoch) -> setWarningMessage("Training stopped in epoch "
        							+ (m_status.getCurrentEpoch() + 1)
        							+ " as the monitored quantity has stopped improving (early stopping)."));
        }
        if (m_generalCfg.getTerminateOnNaNEntry().getEnabled()) {
        	m_status.terminatedOnNaNLoss().addListener(
        			(src, batch) -> setWarningMessage("Training terminated in batch " + (batch + 1) + " of epoch "
        					+ (m_status.getCurrentEpoch() + 1) + " due to a NaN (not a number) loss."));
        }
    }

    private void prepareView(final boolean doValidation, final int totalNumTrainingBatches,
        final int totalNumValidationBatches) {
        m_viewSpecs = new DLDefaultJFreeChartLinePlotViewSpec[2];
		m_viewData = new DLLinePlotViewDataCollection[2];
		if (doValidation) {
			m_viewSpecs[0] = new DLDefaultJFreeChartLinePlotViewSpec("accuracy", "Accuracy", "Accuracy", "Batches",
					new String[] { "Training data", "Validation data" });
			m_viewSpecs[1] = new DLDefaultJFreeChartLinePlotViewSpec("loss", "Loss", "Loss", "Batches",
					new String[] { "Training data", "Validation data" });
			m_viewData[0] = new DLDefaultLinePlotViewDataCollection<>(m_viewSpecs[0],
					new DLDenseLinePlotViewData(totalNumTrainingBatches),
					new DLSparseLinePlotViewData(totalNumValidationBatches));
			m_viewData[1] = new DLDefaultLinePlotViewDataCollection<>(m_viewSpecs[1],
					new DLDenseLinePlotViewData(totalNumTrainingBatches),
					new DLSparseLinePlotViewData(totalNumValidationBatches));
		} else {
			m_viewSpecs[0] = new DLDefaultJFreeChartLinePlotViewSpec("accuracy", "Accuracy", "Accuracy", "Batches",
					new String[] { "Training data" });
			m_viewSpecs[1] = new DLDefaultJFreeChartLinePlotViewSpec("loss", "Loss", "Loss", "Batches",
					new String[] { "Training data" });
			m_viewData[0] = new DLDefaultLinePlotViewDataCollection<>(m_viewSpecs[0],
					new DLDenseLinePlotViewData(totalNumTrainingBatches));
			m_viewData[1] = new DLDefaultLinePlotViewDataCollection<>(m_viewSpecs[1],
					new DLDenseLinePlotViewData(totalNumTrainingBatches));
		}
    }

    private void fillInputSpecificMaps(final DataTableSpec inTableSpec, final Map<DLTensorId, int[]> columnsForTensorId,
        final LinkedHashMap<DLTensorId, DLDataValueToTensorConverterFactory<?, ?>> converterForTensorId) {
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
							+ "' could not be found in the training data table.");
				}
				return idx;
			}).toArray();
			columnsForTensorId.put(spec.getIdentifier(), indices);
			converterForTensorId.put(spec.getIdentifier(), entry.getValue());
		}
    }

    private DLKerasTrainingConfig createTrainingConfig(final DLKerasNetworkSpec inNetworkSpec) {
        final int trainingBatchSize = m_generalCfg.getBatchSizeEntry().getValue();
		final int numEpochs = m_generalCfg.getEpochsEntry().getValue();
		final int validationBatchSize = m_generalCfg.getValidationBatchSizeEntry().getValue();
		final DLKerasOptimizer optimizer = m_generalCfg.getOptimizerEntry().getValue();
        final Map<DLTensorId, DLKerasLossFunction> lossFunctions = createLossFunctionMap(inNetworkSpec);
		final ArrayList<DLKerasCallback> callbacks = createCallbackList();
		return new DLKerasDefaultTrainingConfig(numEpochs, trainingBatchSize,
				validationBatchSize, optimizer, lossFunctions, callbacks);
    }

    private ArrayList<DLKerasCallback> createCallbackList() {
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
        return callbacks;
    }

    private Map<DLTensorId, DLKerasLossFunction> createLossFunctionMap(final DLKerasNetworkSpec inNetworkSpec) {
        final Map<DLTensorId, DLKerasLossFunction> lossFunctions = new HashMap<>();
		for (final DLTensorSpec targetSpec : inNetworkSpec.getOutputSpecs()) {
			final DLKerasLossFunction lossFunction;
			DLKerasLearnerTargetConfig cfg = m_targetCfgs.get(targetSpec.getName());
			ConfigEntry<DLKerasLossFunction> lossEntry = cfg.getLossFunctionEntry();
			if (cfg.getUseCustomLossEntry().getValue()) {
			    lossFunction = cfg.getCustomLossFunctionEntry().getValue();
			} else {
			    lossFunction = lossEntry.getValue();
			}
            lossFunctions.put(targetSpec.getIdentifier(), lossFunction);
		}
        return lossFunctions;
    }

    private boolean doValidation(final BufferedDataTable inValidationTable) {
        final boolean doValidation;
		if (inValidationTable != null) {
			if (inValidationTable.size() == 0) {
				setWarningMessage("Validation data table is empty. No validation will be performed.");
				doValidation = false;
			} else {
				doValidation = true;
			}
		} else {
			doValidation = false;
		}
        return doValidation;
    }

	private Random createRandom() {
		final ConfigEntry<Long> seedCfg = m_generalCfg.getRandomSeed();
		return seedCfg.getEnabled() ? new Random(seedCfg.getValue()) : new Random();
	}

	private DLRowIterator createRowIterator(final BufferedDataTable inTable,
			final Map<DLTensorId, int[]> columnsForTensorId, final Random random, final ExecutionContext exec) {
		final boolean doShuffle = m_generalCfg.getShuffleTrainingData().getValue();
		if (doShuffle) {
			return new DLShuffleDataTableRowIterator(inTable, columnsForTensorId, random.nextLong(),
					exec.createSubExecutionContext(0));
		}
		return new DLDataTableRowIterator(inTable, columnsForTensorId);
	}
}
