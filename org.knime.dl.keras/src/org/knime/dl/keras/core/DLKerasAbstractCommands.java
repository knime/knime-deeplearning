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
package org.knime.dl.keras.core;

import java.io.IOException;
import java.util.Collection;
import java.util.stream.Collectors;

import org.knime.core.util.Version;
import org.knime.dl.core.DLCancelable;
import org.knime.dl.core.DLCanceledExecutionException;
import org.knime.dl.core.DLInvalidEnvironmentException;
import org.knime.dl.core.DLNetworkInputProvider;
import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.core.training.DLTrainingMonitor;
import org.knime.dl.keras.core.training.DLKerasLossFunction;
import org.knime.dl.keras.core.training.DLKerasLossFunction.DLKerasCustomLoss;
import org.knime.dl.keras.core.training.DLKerasTrainingConfig;
import org.knime.dl.keras.core.training.DLKerasTrainingStatus;
import org.knime.dl.python.core.DLPythonAbstractCommands;
import org.knime.dl.python.core.DLPythonContext;
import org.knime.dl.python.core.DLPythonNetworkHandle;
import org.knime.dl.python.core.DLPythonNetworkHandleTableCreatorFactory;
import org.knime.dl.python.core.DLPythonNumPyTypeMap;
import org.knime.dl.python.core.DLPythonTensorSpecTableCreatorFactory;
import org.knime.dl.python.core.SingleValueTableCreator;
import org.knime.dl.python.core.training.DLPythonTrainingStatus;
import org.knime.dl.python.util.DLPythonSourceCodeBuilder;
import org.knime.dl.python.util.DLPythonUtils;
import org.knime.python2.extensions.serializationlibrary.interfaces.Cell;
import org.knime.python2.kernel.AbstractPythonToJavaMessageHandler;
import org.knime.python2.kernel.Messages;
import org.knime.python2.kernel.PythonToJavaMessage;
import org.knime.python2.kernel.PythonToJavaMessageHandler;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public abstract class DLKerasAbstractCommands extends DLPythonAbstractCommands {

    private static final String KERAS_VERSION_NAME = "keras_version";

	protected DLKerasAbstractCommands() {
	}

	protected DLKerasAbstractCommands(final DLPythonContext context) {
		super(context);
	}

    @Override
    protected abstract DLKerasAbstractNetworkReaderCommands getNetworkReaderCommands();

    @Override
    public abstract DLKerasNetworkSpec extractNetworkSpec(DLPythonNetworkHandle network, DLCancelable cancelable)
        throws DLInvalidEnvironmentException, IOException, DLCanceledExecutionException;

    public DLPythonNetworkHandle loadNetworkFromJson(final String path, final DLCancelable cancelable)
        throws DLInvalidEnvironmentException, IOException, DLCanceledExecutionException {
        final DLKerasAbstractNetworkReaderCommands reader = getNetworkReaderCommands();
        final DLPythonSourceCodeBuilder b = DLPythonUtils.createSourceCodeBuilder() //
            .a(reader.importReader()) //
            .n("reader = ").a(reader.createReader()) //
            .n("network = ").a("reader.").a(reader.readFromJson(path)) //
            .n(getRegisterNetworkCode("network", null));
        getContext(cancelable).executeInKernel(b.toString(), cancelable);
        return (DLPythonNetworkHandle)getContext(cancelable)
            .getDataFromKernel(CURRENT_NETWORK_NAME, new DLPythonNetworkHandleTableCreatorFactory(), cancelable).getTable();
    }

    public DLPythonNetworkHandle loadNetworkFromYaml(final String path, final DLCancelable cancelable)
        throws DLInvalidEnvironmentException, IOException, DLCanceledExecutionException {
        final DLKerasAbstractNetworkReaderCommands reader = getNetworkReaderCommands();
        final DLPythonSourceCodeBuilder b = DLPythonUtils.createSourceCodeBuilder() //
            .a(reader.importReader()) //
            .n("reader = ").a(reader.createReader()) //
            .n("network = ").a("reader.").a(reader.readFromYaml(path)) //
            .n(getRegisterNetworkCode("network", null));
        getContext(cancelable).executeInKernel(b.toString(), cancelable);
        return (DLPythonNetworkHandle)getContext(cancelable)
            .getDataFromKernel(CURRENT_NETWORK_NAME, new DLPythonNetworkHandleTableCreatorFactory(), cancelable).getTable();
    }

	public void setNetworkTrainingConfig(final DLPythonNetworkHandle handle, final DLKerasTrainingConfig config, final DLCancelable cancelable)
			throws DLInvalidEnvironmentException, IOException, DLCanceledExecutionException {
	    final Collection<DLKerasCustomLoss> customLosses = getCustomLosses(config.getLosses().values());
		final DLPythonSourceCodeBuilder b = DLPythonUtils.createSourceCodeBuilder() //
				.a("from DLKerasNetwork import DLKerasTrainingConfig");
		if (!customLosses.isEmpty()) {
		    for (final DLKerasCustomLoss customLoss : customLosses) {
		        b.n().a(customLoss.getCustomCodeExecution());
		    }
		}
		
		b.n("config = DLKerasTrainingConfig()") //
		.n("config.epochs = ").a(config.getEpochs()) //
		.n("config.batch_size = ").a(config.getBatchSize()) //
		.n("config.validation_batch_size = ").a(config.getValidationBatchSize()) //
		// TODO: how to import dependencies (here: of optimizer and losses) in a generic way?
		.n("import keras") //
		.n("config.optimizer = ").a(config.getOptimizer().getBackendRepresentation()) //
		.n(config.getLosses().entrySet(),
		    e -> "config.loss[" + DLPythonUtils.toPython(e.getKey().getIdentifierString()) + "] = "
		            + e.getValue().getBackendRepresentation()) //
		.n("import DLKerasTrainingCallbacks") //
		.n(config.getCallbacks(), c -> "config.callbacks.append(" + c.getBackendRepresentation() + ")")
		.n("import DLPythonNetwork") //
		.n("network = DLPythonNetwork.get_network(").as(handle.getIdentifier()).a(")")
		.n("network.spec.training_config = config");
		getContext(cancelable).executeInKernel(b.toString(), cancelable);
	}

	private Collection<DLKerasCustomLoss> getCustomLosses(final Collection<DLKerasLossFunction> lossFunctions) {
	    return lossFunctions.stream()
	            .filter(l -> l instanceof DLKerasCustomLoss)
	            .map(l -> (DLKerasCustomLoss)l)
	            .collect(Collectors.toList());
	}

	@Override
	public void trainNetwork(final DLPythonNetworkHandle network, final DLNetworkInputProvider trainingInputProvider,
			final DLNetworkInputProvider validationInputProvider, final DLTrainingMonitor<? extends DLPythonTrainingStatus> monitor)
			throws DLInvalidEnvironmentException, IOException, DLCanceledExecutionException {
		final Messages messages = getContext(monitor).getKernel().getMessages();

		PythonToJavaMessageHandler terminateOnNaNHandler = null;
		PythonToJavaMessageHandler earlyStoppingHandler = null;

		try {
			if (monitor.getTrainingStatus() instanceof DLKerasTrainingStatus) {
				final DLKerasTrainingStatus status = (DLKerasTrainingStatus) monitor.getTrainingStatus();
				terminateOnNaNHandler = new AbstractPythonToJavaMessageHandler("terminate_on_nan") {

					@Override
					protected void handle(final PythonToJavaMessage msg) throws Exception {
						final long batch = Long.parseLong(msg.getValue());
						status.terminatedOnNaNLoss().raise(batch);
					}
				};
				messages.registerMessageHandler(terminateOnNaNHandler);

				earlyStoppingHandler = new AbstractPythonToJavaMessageHandler("early_stopping") {

					@Override
					protected void handle(final PythonToJavaMessage msg) throws Exception {
						final int epoch = Integer.parseInt(msg.getValue());
						status.stoppedEarly().raise(epoch);
					}
				};
				messages.registerMessageHandler(earlyStoppingHandler);
			}
			super.trainNetwork(network, trainingInputProvider, validationInputProvider, monitor);
		} finally {
			if (terminateOnNaNHandler != null) {
				messages.unregisterMessageHandler(terminateOnNaNHandler);
			}
			if (earlyStoppingHandler != null) {
				messages.unregisterMessageHandler(earlyStoppingHandler);
			}
		}
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
     * @return the keras version
     * @throws DLCanceledExecutionException if the execution has been canceled
     * @throws DLInvalidEnvironmentException if failed to properly setup the Python context
     * @throws IOException if getting the data from python failed
     */
    protected Version getKerasVersion(final DLCancelable cancelable)
        throws DLCanceledExecutionException, DLInvalidEnvironmentException, IOException {
        final DLPythonSourceCodeBuilder b = DLPythonUtils.createSourceCodeBuilder() //
            .a("import keras") //
            .n("import pandas as pd") //
            .n("global ").a(KERAS_VERSION_NAME) //
            .n(KERAS_VERSION_NAME).a(" = pd.DataFrame([keras.__version__])"); //
        getContext(cancelable).executeInKernel(b.toString(), cancelable);
        final String kerasVersion = (String)getContext(cancelable).getDataFromKernel(KERAS_VERSION_NAME,
            (s, ts) -> new SingleValueTableCreator<>(s, Cell::getStringValue), cancelable).getTable();
        return new Version(kerasVersion);
    }

    protected abstract static class DLKerasAbstractNetworkReaderCommands extends DLPythonAbstractNetworkReaderCommands {

        protected DLKerasAbstractNetworkReaderCommands(final String importStatement,
            final String createReaderStatement) {
            super(importStatement, createReaderStatement);
        }

        @Override
        public String read(final String path, final boolean loadTrainingConfig) {
            final DLPythonSourceCodeBuilder b = DLPythonUtils.createSourceCodeBuilder() //
                .a("read(").asr(path).a(", compile=").a(loadTrainingConfig).a(")");
            return b.toString();
        }

        public String readFromJson(final String path) {
            final DLPythonSourceCodeBuilder b = DLPythonUtils.createSourceCodeBuilder() //
                .a("readFromJson(").asr(path).a(")");
            return b.toString();
        }

        public String readFromYaml(final String path) {
            final DLPythonSourceCodeBuilder b = DLPythonUtils.createSourceCodeBuilder() //
                .a("readFromYaml(").asr(path).a(")");
            return b.toString();
        }
    }
}
