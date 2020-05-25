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

import static org.knime.dl.keras.core.layers.DLKerasLayerTestSetups.testOnMultiInputModelSetup;
import static org.knime.dl.keras.core.layers.DLKerasLayerTestSetups.testOnMultiInputMultiOutputForkJoinModelSetup;
import static org.knime.dl.keras.core.layers.DLKerasLayerTestSetups.testOnMultiInputMultiOutputModelAppendedBinaryLayerSetup;
import static org.knime.dl.keras.core.layers.DLKerasLayerTestSetups.testOnMultiInputMultiOutputModelAppendedUnaryLayerSetup;
import static org.knime.dl.keras.core.layers.DLKerasLayerTestSetups.testOnMultiInputMultiOutputModelSetup;
import static org.knime.dl.keras.core.layers.DLKerasLayerTestSetups.testOnMultiOutputModelSetup;
import static org.knime.dl.keras.core.layers.DLKerasLayerTestSetups.testOnSequentialModelAppendedBinaryLayerSetup;
import static org.knime.dl.keras.core.layers.DLKerasLayerTestSetups.testOnSequentialModelAppendedUnaryLayerSetup;
import static org.knime.dl.keras.core.layers.DLKerasLayerTestSetups.testOnSequentialModelSetup;
import static org.knime.dl.keras.core.layers.DLKerasLayerTestSetups.testOnSingleLayerSetup;
import static org.knime.dl.keras.core.layers.DLKerasLayerTestSetups.testOnTwoMultiInputMultiOutputModelsAppendedBinaryLayerSetup;

import java.io.IOException;
import java.util.List;
import java.util.function.Function;

import org.junit.Test;
import org.knime.dl.core.DLCanceledExecutionException;
import org.knime.dl.core.DLInvalidEnvironmentException;
import org.knime.dl.core.DLInvalidSourceException;
import org.knime.dl.keras.core.DLKerasNetworkSpec;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public final class DLKerasNetworkSpecInferrerTest {

    @Test
    public void testInferSingleLayer() {
        testOnSingleLayerSetup(this::inferSpecs, Function.identity());
    }

    @Test
    public void testInferSequentialModel() {
        testOnSequentialModelSetup(this::inferSpecs, Function.identity());
    }

    @Test
    public void testInferMultiInputModel() {
        testOnMultiInputModelSetup(this::inferSpecs, Function.identity());
    }

    @Test
    public void testInferMultiOutputModel() {
        testOnMultiOutputModelSetup(this::inferSpecs, Function.identity());
    }

    @Test
    public void testInferMultiInputMultiOutputModel() {
        testOnMultiInputMultiOutputModelSetup(this::inferSpecs, Function.identity());
    }

    @Test
    public void testInferMultiInputMultiOutputForkJoinModel() {
        testOnMultiInputMultiOutputForkJoinModelSetup(this::inferSpecs, Function.identity());
    }

    @Test
    public void testAppendUnaryLayerToSequentialModel()
        throws DLInvalidSourceException, DLInvalidEnvironmentException, IOException, DLCanceledExecutionException {
        testOnSequentialModelAppendedUnaryLayerSetup(this::inferSpecs, Function.identity());
    }

    @Test
    public void testAppendUnaryLayerToMultiInputMultiOutputModel()
        throws DLInvalidSourceException, DLInvalidEnvironmentException, IOException, DLCanceledExecutionException {
        testOnMultiInputMultiOutputModelAppendedUnaryLayerSetup(this::inferSpecs, Function.identity());
    }

    @Test
    public void testAppendBinaryLayerToSequentialModel()
        throws DLInvalidSourceException, DLInvalidEnvironmentException, IOException, DLCanceledExecutionException {
        testOnSequentialModelAppendedBinaryLayerSetup(this::inferSpecs, Function.identity());
    }

    @Test
    public void testAppendBinaryLayerToMultiInputMultiOutputModel()
        throws DLInvalidSourceException, DLInvalidEnvironmentException, IOException, DLCanceledExecutionException {
        testOnMultiInputMultiOutputModelAppendedBinaryLayerSetup(this::inferSpecs, Function.identity());
    }

    @Test
    public void testAppendBinaryLayerToTwoMultiInputMultiOutputModels()
        throws DLInvalidSourceException, DLInvalidEnvironmentException, IOException, DLCanceledExecutionException {
        testOnTwoMultiInputMultiOutputModelsAppendedBinaryLayerSetup(this::inferSpecs, Function.identity());
    }

    private DLKerasNetworkSpec inferSpecs(final List<DLKerasLayer> outputLayers) {
        return new DLKerasNetworkSpecInferrer(outputLayers).inferNetworkSpec();
    }
}
