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
package org.knime.dl.testing.backend;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;
import org.knime.dl.core.DLCanceledExecutionException;
import org.knime.dl.core.DLNetworkInputPreparer;
import org.knime.dl.core.DLTensor;
import org.knime.dl.core.DLTensorFactory;
import org.knime.dl.core.DLTensorId;
import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.core.data.DLWrappingDataBuffer;
import org.knime.dl.core.data.DLWritableBuffer;
import org.knime.dl.core.data.DLWritableDoubleBuffer;
import org.knime.dl.core.execution.DLAbstractNetworkExecutionSession;
import org.knime.dl.core.execution.DLExecutionMonitor;
import org.knime.dl.core.execution.DLNetworkOutputConsumer;
import org.knime.dl.util.DLUtils;

/**
 * @author Lukas Siedentop, KNIME GmbH, Konstanz, Germany
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 */
public class DLTestingBackendNetworkExecutionSession
    extends DLAbstractNetworkExecutionSession<DLTestingBackendNetwork> {

    public DLTestingBackendNetworkExecutionSession(final DLTestingBackendNetwork network,
        final Set<DLTensorSpec> executionInputSpecs, final Set<DLTensorId> requestedOutputs,
        final DLNetworkInputPreparer inputPreparer, final DLNetworkOutputConsumer outputConsumer,
        final DLTensorFactory tensorFactory) {
        super(network, executionInputSpecs, requestedOutputs, inputPreparer, outputConsumer, tensorFactory);
    }

    @Override
    protected void executeInternal(final DLExecutionMonitor monitor) throws DLCanceledExecutionException, Exception {
        // we fake some network activity here: unwrap floats, calc some stuff, create doubles...
        for (long i = 0; i < m_inputPreparer.getNumBatches(); i++) {
            m_inputPreparer.prepare(m_input, i);
            for (final Entry<DLTensorId, DLTensor<? extends DLWritableBuffer>> in : m_input.entrySet()) {
                // TODO: we can't be sure that casting will work here
                final DLWrappingDataBuffer<float[]> buffer = (DLWrappingDataBuffer<float[]>)in.getValue().getBuffer();
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
                            final long[] shape =
                                    DLUtils.Shapes.getFixedShape(spec.getShape()).orElseThrow(RuntimeException::new);
                            final DLTensorSpec executionSpec =
                                    m_tensorFactory.createExecutionTensorSpec(spec, batchSize, shape);
                            m_output.put(spec.getIdentifier(), m_tensorFactory.createReadableTensor(executionSpec));
                        }
                    }
                }
                final DLTensorId outId = m_output.keySet().stream().findFirst().get();
                ((DLWritableDoubleBuffer)m_output.get(outId).getBuffer()).putAll(outArr);
            }
        }
    }

    @Override
    public void close() throws Exception {
        super.close();
        // no-op
    }
}
