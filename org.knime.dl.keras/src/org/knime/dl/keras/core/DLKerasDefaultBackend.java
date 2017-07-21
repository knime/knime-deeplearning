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
 *   May 23, 2017 (marcel): created
 */
package org.knime.dl.keras.core;

import org.knime.dl.core.DLDefaultLayerData;
import org.knime.dl.core.DLLayerData;
import org.knime.dl.core.DLLayerDataSpec;
import org.knime.dl.core.DLNetwork;
import org.knime.dl.core.DLNetworkSpec;
import org.knime.dl.core.backend.DLBackendRegistry;
import org.knime.dl.core.data.DLReadableBuffer;
import org.knime.dl.core.data.DLReadableDoubleBuffer;
import org.knime.dl.core.data.DLReadableFloatBuffer;
import org.knime.dl.core.data.DLReadableIntBuffer;
import org.knime.dl.core.data.DLReadableLongBuffer;
import org.knime.dl.core.data.writables.DLWritableBuffer;
import org.knime.dl.core.data.writables.DLWritableDoubleBuffer;
import org.knime.dl.core.data.writables.DLWritableFloatBuffer;
import org.knime.dl.core.data.writables.DLWritableIntBuffer;
import org.knime.dl.core.data.writables.DLWritableLongBuffer;
import org.knime.dl.keras.core.DLKerasExecutableNetwork.DLKerasExecutableNetworkSpec;
import org.knime.dl.keras.core.execution.DLKerasNetworkExecutor;
import org.knime.dl.keras.core.io.DLKerasNetworkReader;
import org.knime.dl.python.core.data.DLPythonDataBuffer;
import org.knime.dl.python.core.data.DLPythonDoubleBuffer;
import org.knime.dl.python.core.data.DLPythonFloatBuffer;
import org.knime.dl.python.core.data.DLPythonIntBuffer;
import org.knime.dl.python.core.data.DLPythonLongBuffer;
import org.knime.dl.util.DLUtils;

/**
 * The default implementation of the {@link DLKerasBackend Keras back end}.
 *
 * @noinstantiate there is usually no need to instantiate this class directly as a common instance is accessible via
 *                {@link DLBackendRegistry}.
 *
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
public final class DLKerasDefaultBackend implements DLKerasBackend {

    /**
     * The friendly name of this back end implementation.
     */
    public static final String NAME = "Keras";

    /**
     * The unique identifier of this back end implementation.
     */
    public static final String IDENTIFIER = "Keras";

    private final DLKerasTypeMap m_typeMap;

    /**
     * Creates a new instance of the default Keras back end.
     *
     */
    public DLKerasDefaultBackend() {
        m_typeMap = new DLKerasTypeMap();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DLKerasTypeMap getTypeMap() {
        return m_typeMap;
    }

    @Override
    public DLLayerData<DLPythonDataBuffer<?>> createLayerData(final DLLayerDataSpec spec)
        throws IllegalArgumentException {
        // TODO: This will be an extension point or at least an internal
        // registry if necessary.
        // Either way, a more sophisticated matching mechanism is needed.
        final long[] shape =
            DLUtils.Shapes.getFixedShape(spec.getShape()).orElseThrow(() -> new IllegalArgumentException(
                "Layer data spec does not provide a shape. Layer data cannot be created."));
        DLPythonDataBuffer<?> data;
        final Class<?> t = spec.getElementType();
        final long size = DLUtils.Shapes.getSize(shape);
        if (t.equals(double.class)) {
            data = new DLPythonDoubleBuffer(size);
        } else if (t.equals(float.class)) {
            data = new DLPythonFloatBuffer(size);
        } else if (t.equals(int.class)) {
            data = new DLPythonIntBuffer(size);
        } else if (t.equals(long.class)) {
            data = new DLPythonLongBuffer(size);
        } else {
            throw new IllegalArgumentException("No matching layer data type.");
        }
        return new DLDefaultLayerData<>(spec, data);
    }

    @Override
    public Class<? extends DLReadableBuffer> getReadableBufferType(final DLLayerDataSpec spec) {
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
    public Class<? extends DLWritableBuffer> getWritableBufferType(final DLLayerDataSpec spec) {
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
    public DLKerasNetworkReader createReader() {
        return new DLKerasNetworkReader(this);
    }

    @Override
    public DLKerasNetworkExecutor createExecutor() throws Exception {
        return new DLKerasNetworkExecutor(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DLKerasExecutableNetwork toExecutableNetwork(final DLNetwork network) {
        // TODO: generic typing?
        if (!(network instanceof DLKerasNetwork)) {
            throw new IllegalArgumentException("Input must be a Keras network.");
        }
        final DLNetworkSpec networkSpec = network.getSpec();
        return new DLKerasExecutableNetwork(
            this, new DLKerasExecutableNetworkSpec(networkSpec.getInputSpecs(),
                networkSpec.getIntermediateOutputSpecs(), networkSpec.getOutputSpecs()),
            ((DLKerasNetwork)network).getSource());
    }
}
