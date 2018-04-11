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

import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.knime.dl.keras.core.layers.DLKerasInnerLayer;
import org.knime.dl.keras.core.layers.DLKerasInputLayer;
import org.knime.dl.keras.core.layers.DLKerasNetworkLayerGraphIterator;
import org.knime.dl.keras.core.layers.DLKerasNetworkLayerGraphIterator.DLKerasLayerVisitor;
import org.knime.dl.keras.core.layers.impl.DLKerasAddLayer;
import org.knime.dl.keras.core.layers.impl.DLKerasDefaultInputLayer;
import org.knime.dl.keras.core.layers.impl.DLKerasDenseLayer;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public class DLKerasLayerGraphIteratorTest {

    @Test
    public void testLinearGraphIteration() {
        final DLKerasDefaultInputLayer in1 = new DLKerasDefaultInputLayer();

        final DLKerasDenseLayer hidden1 = new DLKerasDenseLayer();
        hidden1.getParents()[0] = in1;

        final DLKerasDenseLayer hidden2 = new DLKerasDenseLayer();
        hidden2.getParents()[0] = hidden1;

        final DLKerasDenseLayer hidden3 = new DLKerasDenseLayer();
        hidden3.getParents()[0] = hidden2;

        final DLKerasDenseLayer out1 = new DLKerasDenseLayer();
        out1.getParents()[0] = hidden3;

        final AtomicInteger counter = new AtomicInteger();

        new DLKerasNetworkLayerGraphIterator(Arrays.asList(out1)).visitAll(new DLKerasLayerVisitor() {

            @Override
            public void visitOutput(final DLKerasInnerLayer outputLayer) throws Exception {
                assert counter.getAndIncrement() == 0;
                assert outputLayer == out1;
                assert outputLayer.getParents()[0] == hidden3;
            }

            @Override
            public void visitHidden(final DLKerasInnerLayer hiddenLayer) throws Exception {
                final int i = counter.getAndIncrement();
                if (i == 3) {
                    assert hiddenLayer == hidden1;
                    assert hiddenLayer.getParents().length == 1;
                    assert hiddenLayer.getParents()[0] == in1;
                } else if (i == 2) {
                    assert hiddenLayer == hidden2;
                    assert hiddenLayer.getParents().length == 1;
                    assert hiddenLayer.getParents()[0] == hidden1;
                } else if (i == 1) {
                    assert hiddenLayer == hidden3;
                    assert hiddenLayer.getParents().length == 1;
                    assert hiddenLayer.getParents()[0] == hidden2;
                } else {
                    fail();
                }
            }

            @Override
            public void visitInput(final DLKerasInputLayer inputLayer) throws Exception {
                assert counter.get() == 4;
                assert inputLayer == in1;
            }
        });
    }

    @Test
    public void testMultiInputLinearGraphIteration() {
        final DLKerasDefaultInputLayer in1 = new DLKerasDefaultInputLayer();

        final DLKerasDefaultInputLayer in2 = new DLKerasDefaultInputLayer();

        final DLKerasAddLayer hidden1 = new DLKerasAddLayer();
        hidden1.getParents()[0] = in1;
        hidden1.getParents()[1] = in2;

        final DLKerasDenseLayer hidden2 = new DLKerasDenseLayer();
        hidden2.getParents()[0] = hidden1;

        final DLKerasDenseLayer hidden3 = new DLKerasDenseLayer();
        hidden3.getParents()[0] = hidden2;

        final DLKerasDenseLayer out1 = new DLKerasDenseLayer();
        out1.getParents()[0] = hidden3;

        final AtomicInteger counter = new AtomicInteger();

        new DLKerasNetworkLayerGraphIterator(Arrays.asList(out1)).visitAll(new DLKerasLayerVisitor() {

            @Override
            public void visitOutput(final DLKerasInnerLayer outputLayer) throws Exception {
                assert counter.getAndIncrement() == 0;
                assert outputLayer == out1;
                assert outputLayer.getParents()[0] == hidden3;
            }

            @Override
            public void visitHidden(final DLKerasInnerLayer hiddenLayer) throws Exception {
                final int i = counter.getAndIncrement();
                if (i == 3) {
                    assert hiddenLayer == hidden1;
                    assert hiddenLayer.getParents().length == 2;
                    assert hiddenLayer.getParents()[0] == in1;
                    assert hiddenLayer.getParents()[1] == in2;
                } else if (i == 2) {
                    assert hiddenLayer == hidden2;
                    assert hiddenLayer.getParents().length == 1;
                    assert hiddenLayer.getParents()[0] == hidden1;
                } else if (i == 1) {
                    assert hiddenLayer == hidden3;
                    assert hiddenLayer.getParents().length == 1;
                    assert hiddenLayer.getParents()[0] == hidden2;
                } else {
                    fail();
                }
            }

            @Override
            public void visitInput(final DLKerasInputLayer inputLayer) throws Exception {
                final int i = counter.getAndIncrement();
                if (i == 4) {
                    assert inputLayer == in1;
                } else if (i == 5) {
                    assert inputLayer == in2;
                } else {
                    fail();
                }
            }
        });
    }

    @Test
    public void testMultiOutputLinearGraphIteration() {
        final DLKerasDefaultInputLayer in1 = new DLKerasDefaultInputLayer();

        final DLKerasDenseLayer hidden1 = new DLKerasDenseLayer();
        hidden1.getParents()[0] = in1;

        final DLKerasDenseLayer hidden2 = new DLKerasDenseLayer();
        hidden2.getParents()[0] = hidden1;

        final DLKerasDenseLayer hidden3 = new DLKerasDenseLayer();
        hidden3.getParents()[0] = hidden2;

        final DLKerasDenseLayer out1 = new DLKerasDenseLayer();
        out1.getParents()[0] = hidden3;

        final DLKerasDenseLayer out2 = new DLKerasDenseLayer();
        out2.getParents()[0] = hidden3;

        final AtomicInteger counter = new AtomicInteger();

        new DLKerasNetworkLayerGraphIterator(Arrays.asList(out1, out2)).visitAll(new DLKerasLayerVisitor() {

            @Override
            public void visitOutput(final DLKerasInnerLayer outputLayer) throws Exception {
                final int i = counter.getAndIncrement();
                if (i == 0) {
                    assert outputLayer == out1;
                    assert outputLayer.getParents()[0] == hidden3;
                } else if (i == 5) {
                    assert outputLayer == out2;
                    assert outputLayer.getParents()[0] == hidden3;
                } else {
                    fail();
                }
            }

            @Override
            public void visitHidden(final DLKerasInnerLayer hiddenLayer) throws Exception {
                final int i = counter.getAndIncrement();
                if (i == 3) {
                    assert hiddenLayer == hidden1;
                    assert hiddenLayer.getParents().length == 1;
                    assert hiddenLayer.getParents()[0] == in1;
                } else if (i == 2) {
                    assert hiddenLayer == hidden2;
                    assert hiddenLayer.getParents().length == 1;
                    assert hiddenLayer.getParents()[0] == hidden1;
                } else if (i == 1) {
                    assert hiddenLayer == hidden3;
                    assert hiddenLayer.getParents().length == 1;
                    assert hiddenLayer.getParents()[0] == hidden2;
                } else {
                    fail();
                }
            }

            @Override
            public void visitInput(final DLKerasInputLayer inputLayer) throws Exception {
                assert counter.getAndIncrement() == 4;
                assert inputLayer == in1;
            }
        });
    }

    @Test
    public void testMultiInputMultiOutputLinearGraphIteration() {
        final DLKerasDefaultInputLayer in1 = new DLKerasDefaultInputLayer();

        final DLKerasDefaultInputLayer in2 = new DLKerasDefaultInputLayer();

        final DLKerasAddLayer hidden1 = new DLKerasAddLayer();
        hidden1.getParents()[0] = in1;
        hidden1.getParents()[1] = in2;

        final DLKerasDenseLayer hidden2 = new DLKerasDenseLayer();
        hidden2.getParents()[0] = hidden1;

        final DLKerasDenseLayer hidden3 = new DLKerasDenseLayer();
        hidden3.getParents()[0] = hidden2;

        final DLKerasDenseLayer out1 = new DLKerasDenseLayer();
        out1.getParents()[0] = hidden3;

        final DLKerasDenseLayer out2 = new DLKerasDenseLayer();
        out2.getParents()[0] = hidden3;

        final AtomicInteger counter = new AtomicInteger();

        new DLKerasNetworkLayerGraphIterator(Arrays.asList(out1, out2)).visitAll(new DLKerasLayerVisitor() {

            @Override
            public void visitOutput(final DLKerasInnerLayer outputLayer) throws Exception {
                final int i = counter.getAndIncrement();
                if (i == 0) {
                    assert outputLayer == out1;
                    assert outputLayer.getParents()[0] == hidden3;
                } else if (i == 6) {
                    assert outputLayer == out2;
                    assert outputLayer.getParents()[0] == hidden3;
                } else {
                    fail();
                }
            }

            @Override
            public void visitHidden(final DLKerasInnerLayer hiddenLayer) throws Exception {
                final int i = counter.getAndIncrement();
                if (i == 3) {
                    assert hiddenLayer == hidden1;
                    assert hiddenLayer.getParents().length == 2;
                    assert hiddenLayer.getParents()[0] == in1;
                    assert hiddenLayer.getParents()[1] == in2;
                } else if (i == 2) {
                    assert hiddenLayer == hidden2;
                    assert hiddenLayer.getParents().length == 1;
                    assert hiddenLayer.getParents()[0] == hidden1;
                } else if (i == 1) {
                    assert hiddenLayer == hidden3;
                    assert hiddenLayer.getParents().length == 1;
                    assert hiddenLayer.getParents()[0] == hidden2;
                } else {
                    fail();
                }
            }

            @Override
            public void visitInput(final DLKerasInputLayer inputLayer) throws Exception {
                final int i = counter.getAndIncrement();
                if (i == 4) {
                    assert inputLayer == in1;
                } else if (i == 5) {
                    assert inputLayer == in2;
                } else {
                    fail();
                }
            }
        });
    }

    @Test
    public void testMultiInputMultiOutputForkJoinGraphIteration() {
        final DLKerasDefaultInputLayer in1 = new DLKerasDefaultInputLayer();

        final DLKerasDefaultInputLayer in2 = new DLKerasDefaultInputLayer();

        final DLKerasDefaultInputLayer in3 = new DLKerasDefaultInputLayer();

        final DLKerasDenseLayer hidden1 = new DLKerasDenseLayer();
        hidden1.getParents()[0] = in1;

        final DLKerasAddLayer hidden2 = new DLKerasAddLayer();
        hidden2.getParents()[0] = hidden1;
        hidden2.getParents()[1] = in2;

        final DLKerasAddLayer hidden3 = new DLKerasAddLayer();
        hidden3.getParents()[0] = hidden2;
        hidden3.getParents()[1] = in3;

        final DLKerasAddLayer hidden4 = new DLKerasAddLayer();
        hidden4.getParents()[0] = hidden1;
        hidden4.getParents()[1] = hidden3;

        final DLKerasDenseLayer hidden5 = new DLKerasDenseLayer();
        hidden5.getParents()[0] = hidden3;

        final DLKerasDenseLayer hidden6 = new DLKerasDenseLayer();
        hidden6.getParents()[0] = hidden4;

        final DLKerasDenseLayer hidden7 = new DLKerasDenseLayer();
        hidden7.getParents()[0] = hidden4;

        final DLKerasDenseLayer hidden8 = new DLKerasDenseLayer();
        hidden8.getParents()[0] = hidden5;

        final DLKerasDenseLayer out1 = new DLKerasDenseLayer();
        out1.getParents()[0] = hidden6;

        final DLKerasDenseLayer out2 = new DLKerasDenseLayer();
        out2.getParents()[0] = hidden6;

        final DLKerasAddLayer out3 = new DLKerasAddLayer();
        out3.getParents()[0] = hidden7;
        out3.getParents()[1] = hidden8;

        final AtomicInteger counter = new AtomicInteger();

        new DLKerasNetworkLayerGraphIterator(Arrays.asList(out1, out2, out3)).visitAll(new DLKerasLayerVisitor() {

            @Override
            public void visitOutput(final DLKerasInnerLayer outputLayer) throws Exception {
                final int i = counter.getAndIncrement();
                if (i == 0) {
                    assert outputLayer == out1;
                    assert outputLayer.getParents()[0] == hidden6;
                } else if (i == 9) {
                    assert outputLayer == out2;
                    assert outputLayer.getParents()[0] == hidden6;
                } else if (i == 10) {
                    assert outputLayer == out3;
                    assert outputLayer.getParents()[0] == hidden7;
                    assert outputLayer.getParents()[1] == hidden8;
                } else {
                    fail();
                }
            }

            @Override
            public void visitHidden(final DLKerasInnerLayer hiddenLayer) throws Exception {
                final int i = counter.getAndIncrement();
                if (i == 1) {
                    assert hiddenLayer == hidden6;
                    assert hiddenLayer.getParents().length == 1;
                    assert hiddenLayer.getParents()[0] == hidden4;
                } else if (i == 11) {
                    assert hiddenLayer == hidden7;
                    assert hiddenLayer.getParents().length == 1;
                    assert hiddenLayer.getParents()[0] == hidden4;
                } else if (i == 12) {
                    assert hiddenLayer == hidden8;
                    assert hiddenLayer.getParents().length == 1;
                    assert hiddenLayer.getParents()[0] == hidden5;
                } else if (i == 2) {
                    assert hiddenLayer == hidden4;
                    assert hiddenLayer.getParents().length == 2;
                    assert hiddenLayer.getParents()[0] == hidden1;
                    assert hiddenLayer.getParents()[1] == hidden3;
                } else if (i == 13) {
                    assert hiddenLayer == hidden5;
                    assert hiddenLayer.getParents().length == 1;
                    assert hiddenLayer.getParents()[0] == hidden3;
                } else if (i == 5) {
                    assert hiddenLayer == hidden3;
                    assert hiddenLayer.getParents().length == 2;
                    assert hiddenLayer.getParents()[0] == hidden2;
                    assert hiddenLayer.getParents()[1] == in3;
                } else if (i == 6) {
                    assert hiddenLayer == hidden2;
                    assert hiddenLayer.getParents().length == 2;
                    assert hiddenLayer.getParents()[0] == hidden1;
                    assert hiddenLayer.getParents()[1] == in2;
                } else if (i == 3) {
                    assert hiddenLayer == hidden1;
                    assert hiddenLayer.getParents().length == 1;
                    assert hiddenLayer.getParents()[0] == in1;
                } else {
                    fail();
                }
            }

            @Override
            public void visitInput(final DLKerasInputLayer inputLayer) throws Exception {
                final int i = counter.getAndIncrement();
                if (i == 4) {
                    assert inputLayer == in1;
                } else if (i == 7) {
                    assert inputLayer == in2;
                } else if (i == 8) {
                    assert inputLayer == in3;
                } else {
                    fail();
                }
            }
        });
    }
}
