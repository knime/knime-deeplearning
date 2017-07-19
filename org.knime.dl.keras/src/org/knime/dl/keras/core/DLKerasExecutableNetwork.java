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
 *   May 3, 2017 (marcel): created
 */
package org.knime.dl.keras.core;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.knime.dl.core.DLAbstractExecutableNetwork;
import org.knime.dl.core.DLAbstractExecutableNetworkSpec;
import org.knime.dl.core.DLLayerData;
import org.knime.dl.core.DLLayerDataSpec;
import org.knime.dl.core.data.DLReadableBuffer;
import org.knime.dl.core.data.writables.DLWritableBuffer;
import org.knime.dl.core.execution.DLDefaultLayerDataInput;
import org.knime.dl.core.execution.DLDefaultLayerDataOutput;
import org.knime.dl.core.execution.DLLayerDataInput;
import org.knime.dl.core.execution.DLLayerDataOutput;
import org.knime.dl.keras.core.execution.DLKerasNetworkExecutor;

/**
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
public class DLKerasExecutableNetwork extends DLAbstractExecutableNetwork {

    private final URL m_source;

    private DLKerasNetworkExecutor m_executor;

    private final HashMap<DLLayerDataSpec, DLLayerDataInput<?>> m_inputs;

    private final HashMap<DLLayerDataSpec, DLLayerDataOutput<?>> m_outputs;

    public DLKerasExecutableNetwork(final DLKerasBackend backend, final DLKerasExecutableNetworkSpec spec,
        final URL source) {
        super(backend, spec);
        m_source = source;
        m_inputs = new HashMap<>(spec.getInputSpecs().length);
        m_outputs = new HashMap<>(spec.getOutputSpecs().length);
    }

    public URL getSource() {
        return m_source;
    }

    @Override
    public DLKerasBackend getBackend() {
        return (DLKerasBackend)super.getBackend();
    }

    @Override
    public DLKerasExecutableNetworkSpec getSpec() {
        return (DLKerasExecutableNetworkSpec)super.getSpec();
    }

    // TODO abstract class
    @Override
    public DLLayerDataInput<?> getInputForSpec(final DLLayerDataSpec spec, final int batchSize) {
        DLLayerDataInput<?> input = m_inputs.get(spec);
        if (input == null || input.getBatchSize() != batchSize) {
            final DLLayerData[] batch = new DLLayerData[batchSize];
            for (int i = 0; i < batchSize; i++) {
                batch[i] = getBackend().createLayerData(spec);
            }
            input = new DLDefaultLayerDataInput<>(batch);
            m_inputs.put(spec, input);
        }
        return input;
    }

    private DLLayerDataOutput<?> getOutputForSpec(final DLLayerDataSpec spec, final int batchSize) {
        DLLayerDataOutput<?> output = m_outputs.get(spec);
        if (output == null || output.getBatchSize() != batchSize) {
            final DLLayerData[] batch = new DLLayerData[batchSize];
            for (int i = 0; i < batchSize; i++) {
                batch[i] = getBackend().createLayerData(spec);
            }
            output = new DLDefaultLayerDataOutput(batch);
            m_outputs.put(spec, output);
        }
        return output;
    }

    @Override
    public void execute(final Set<DLLayerDataSpec> selectedOutputs,
        final Consumer<Map<DLLayerDataSpec, DLLayerDataOutput<?>>> outputConsumer) throws IOException {
        if (m_executor == null) {
            m_executor = new DLKerasNetworkExecutor(getBackend());
        }

        // TODO
        final int outputBatchSize = (int)m_inputs.values().stream().findFirst().get().getBatchSize();

        final HashMap<DLLayerDataSpec, DLLayerDataOutput<?>> outputs = new HashMap<>(selectedOutputs.size());
        for (final DLLayerDataSpec output : selectedOutputs) {
            outputs.put(output, getOutputForSpec(output, outputBatchSize));
        }
        m_executor.execute(this, m_inputs, outputs);

        outputConsumer.accept(outputs);

        // TODO efficiency
        for (final DLLayerDataInput<?> input : m_inputs.values()) {
            // TODO consider making one single buffer. however, type hierarchy
            // important for matching
            final DLLayerData[] buf = input.getBatch();
            for (final DLLayerData layer : buf) {
                ((DLWritableBuffer)layer.getBuffer()).resetWrite();
                ((DLReadableBuffer)layer.getBuffer()).resetRead();
            }
        }
        for (final DLLayerDataOutput<?> output : m_outputs.values()) {
            final DLLayerData[] buf = output.getBatch();
            for (final DLLayerData layer : buf) {
                ((DLWritableBuffer)layer.getBuffer()).resetWrite();
                ((DLReadableBuffer)layer.getBuffer()).resetRead();
            }
        }
    }

    @Override
    public void close() throws Exception {
        if (m_executor != null) {
            m_executor.close();
            m_executor = null;
            m_inputs.clear();
            m_outputs.clear();
        }
    }

    public static class DLKerasExecutableNetworkSpec extends DLAbstractExecutableNetworkSpec {

        public DLKerasExecutableNetworkSpec(final DLLayerDataSpec[] inputSpecs,
            final DLLayerDataSpec[] intermediateOutputSpecs, final DLLayerDataSpec[] outputSpecs) {
            super(inputSpecs, intermediateOutputSpecs, outputSpecs);
        }
    }
}
