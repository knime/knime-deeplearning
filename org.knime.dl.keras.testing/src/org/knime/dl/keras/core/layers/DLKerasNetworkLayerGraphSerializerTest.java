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
package org.knime.dl.keras.core.layers;

import static org.knime.dl.keras.core.layers.DLKerasLayerTestSetups.createMultiInputModelTestSetup;
import static org.knime.dl.keras.core.layers.DLKerasLayerTestSetups.createMultiInputMultiOutputForkJoinModelTestSetup;
import static org.knime.dl.keras.core.layers.DLKerasLayerTestSetups.createMultiInputMultiOutputModelTestSetup;
import static org.knime.dl.keras.core.layers.DLKerasLayerTestSetups.createMultiOutputModelTestSetup;
import static org.knime.dl.keras.core.layers.DLKerasLayerTestSetups.createSequentialModelTestSetup;
import static org.knime.dl.keras.core.layers.DLKerasLayerTestSetups.createSingleLayerTestSetup;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public final class DLKerasNetworkLayerGraphSerializerTest {

    private ObjectOutputStream m_outStream;

    private ByteArrayOutputStream m_outStreamBase;

    private ObjectInputStream m_inStream;

    @Before
    public void setup() throws IOException {
        m_outStreamBase = new ByteArrayOutputStream();
        m_outStream = new ObjectOutputStream(m_outStreamBase);
    }

    @After
    public void cleanup() throws IOException {
        IOException closeException = null;
        if (m_outStream != null) {
            try {
                m_outStream.close();
            } catch (final IOException e) {
                closeException = e;
            }
        }
        if (m_inStream != null) {
            m_inStream.close();
        }
        if (closeException != null) {
            throw closeException;
        }
    }

    @Test
    public void testSerializeSingleLayer() throws IOException, ClassNotFoundException {
        testSerialize(createSingleLayerTestSetup());
    }

    @Test
    public void testSerializeSequentialModel() throws IOException, ClassNotFoundException {
        testSerialize(createSequentialModelTestSetup());
    }

    @Test
    public void testSerializeMultiInputModel() throws IOException, ClassNotFoundException {
        testSerialize(createMultiInputModelTestSetup());
    }

    @Test
    public void testSerializeMultiOutputModel() throws IOException, ClassNotFoundException {
        testSerialize(createMultiOutputModelTestSetup());
    }

    @Test
    public void testSerializeMultiInputMultiOutputModel() throws IOException, ClassNotFoundException {
        testSerialize(createMultiInputMultiOutputModelTestSetup());
    }

    @Test
    public void testSerializeMultiInputMultiOutputForkJoinModel() throws IOException, ClassNotFoundException {
        testSerialize(createMultiInputMultiOutputForkJoinModelTestSetup());
    }

    private void testSerialize(final List<DLKerasLayer> outputLayers) throws IOException, ClassNotFoundException {
        new DLKerasNetworkLayerGraphSerializer().writeGraphTo(outputLayers, m_outStream);
        m_inStream = outStreamToInStream();
        final List<DLKerasLayer> deserializedOutputLayers =
            new DLKerasNetworkLayerGraphSerializer().readGraphFrom(m_inStream);
        assertGraphEquals(outputLayers, deserializedOutputLayers);
    }

    private void assertGraphEquals(final List<DLKerasLayer> a, final List<DLKerasLayer> b) {
        assert a.size() == b.size();
        for (int i = 0; i < a.size(); i++) {
            assertLayerEquals(a.get(i), b.get(i));
        }
        final Map<DLKerasTensorSpecsOutput, Integer> layersA =
            new DLKerasNetworkLayerGraphDepthFirstIterator(a).visitAll(null);
        final Map<DLKerasTensorSpecsOutput, Integer> layersB =
            new DLKerasNetworkLayerGraphDepthFirstIterator(b).visitAll(null);
        assert layersA.size() == layersB.size();
        final Iterator<Entry<DLKerasTensorSpecsOutput, Integer>> layersBIterator = layersB.entrySet().iterator();
        for (final Entry<DLKerasTensorSpecsOutput, Integer> entryA : layersA.entrySet()) {
            final Entry<DLKerasTensorSpecsOutput, Integer> entryB = layersBIterator.next();
            assertLayerEquals(entryA.getKey(), entryB.getKey());
            assert entryA.getValue().equals(entryB.getValue());
        }
    }

    private void assertLayerEquals(final DLKerasTensorSpecsOutput a, final DLKerasTensorSpecsOutput b) {
        assert a.equalsIgnoreName(b);
    }

    private ObjectInputStream outStreamToInStream() throws IOException {
        m_inStream = new ObjectInputStream(new ByteArrayInputStream(m_outStreamBase.toByteArray()));
        return m_inStream;
    }
}
