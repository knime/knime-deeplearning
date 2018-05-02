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

import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.knime.dl.keras.core.layers.DLKerasNetworkLayerGraphIterator.DLKerasLayerVisitor;
import org.knime.dl.keras.core.layers.impl.DLKerasAddLayer;
import org.knime.dl.keras.core.layers.impl.DLKerasDefaultInputLayer;
import org.knime.dl.keras.core.layers.impl.DLKerasDenseLayer;

/**
 * Also see {@link DLKerasNetworkLayerGraphDepthFirstIteratorTest}. Test cases should be kept in sync.
 *
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public final class DLKerasNetworkLayerGraphTopologicalOrderIteratorTest {

    private AtomicInteger m_counter;

    private AtomicInteger m_noteLayerDepthsCalledAfterCounter;

    @Before
    public void setup() {
        m_counter = new AtomicInteger();
        m_noteLayerDepthsCalledAfterCounter = new AtomicInteger();
    }

    @Test
    public void testSingleLayerGraphIteration() {
        final DLKerasDefaultInputLayer inout0 = new DLKerasDefaultInputLayer();

        new DLKerasNetworkLayerGraphTopologicalOrderIterator(Arrays.asList(inout0)).visitAll(new DLKerasLayerVisitor() {

            @Override
            public void visitOutput(final DLKerasInnerLayer outputLayer) throws Exception {
                Assert.fail("Network does not contain pure outputs.");
            }

            @Override
            public void visitHidden(final DLKerasInnerLayer hiddenLayer) throws Exception {
                Assert.fail("Network does not contain hidden layers.");
            }

            @Override
            public void visitInput(final DLKerasInputLayer inputLayer) throws Exception {
                Assert.fail("Network does not contain pure inputs.");
            }

            @Override
            public void visitInputOutput(final DLKerasInputLayer inputOutputLayer) throws Exception {
                assert m_counter.getAndIncrement() == 0;
                assert inputOutputLayer == inout0;
            }

            @Override
            public void visitBaseNetworkOutput(final DLKerasBaseNetworkTensorSpecOutput baseNetworkOutput) {
                Assert.fail("Network is not connected to a base network.");
            }

            @Override
            public void noteLayerDepths(final LinkedHashMap<DLKerasTensorSpecsOutput, Integer> maxDepthsFromOutputs) {
                final Integer inout0Depth = maxDepthsFromOutputs.get(inout0);
                assert inout0Depth != null;
                assert inout0Depth == 0;
                m_noteLayerDepthsCalledAfterCounter.set(m_counter.get());
            }
        });

        checkCommonPostconditions(1);
    }

    @Test
    public void testLinearGraphIteration() {
        final DLKerasDefaultInputLayer in0 = new DLKerasDefaultInputLayer();

        final DLKerasDenseLayer hidden0 = new DLKerasDenseLayer();
        hidden0.setParent(0, in0);

        final DLKerasDenseLayer hidden1 = new DLKerasDenseLayer();
        hidden1.setParent(0, hidden0);

        final DLKerasDenseLayer hidden2 = new DLKerasDenseLayer();
        hidden2.setParent(0, hidden1);

        final DLKerasDenseLayer out0 = new DLKerasDenseLayer();
        out0.setParent(0, hidden2);

        new DLKerasNetworkLayerGraphTopologicalOrderIterator(Arrays.asList(out0)).visitAll(new DLKerasLayerVisitor() {

            @Override
            public void visitOutput(final DLKerasInnerLayer outputLayer) throws Exception {
                assert m_counter.getAndIncrement() == 4;
                assert outputLayer == out0;
                assert outputLayer.getParent(0) == hidden2;
            }

            @Override
            public void visitHidden(final DLKerasInnerLayer hiddenLayer) throws Exception {
                final int i = m_counter.getAndIncrement();
                if (i == 1) {
                    assert hiddenLayer == hidden0;
                    assert hiddenLayer.getNumParents() == 1;
                    assert hiddenLayer.getParent(0) == in0;
                } else if (i == 2) {
                    assert hiddenLayer == hidden1;
                    assert hiddenLayer.getNumParents() == 1;
                    assert hiddenLayer.getParent(0) == hidden0;
                } else if (i == 3) {
                    assert hiddenLayer == hidden2;
                    assert hiddenLayer.getNumParents() == 1;
                    assert hiddenLayer.getParent(0) == hidden1;
                } else {
                    fail();
                }
            }

            @Override
            public void visitInput(final DLKerasInputLayer inputLayer) throws Exception {
                assert m_counter.getAndIncrement() == 0;
                assert inputLayer == in0;
            }

            @Override
            public void visitInputOutput(final DLKerasInputLayer inputOutputLayer) throws Exception {
                Assert.fail("Network does not contain input layers that are also outputs.");
            }

            @Override
            public void visitBaseNetworkOutput(final DLKerasBaseNetworkTensorSpecOutput baseNetworkOutput) {
                Assert.fail("Network is not connected to a base network.");
            }

            @Override
            public void noteLayerDepths(final LinkedHashMap<DLKerasTensorSpecsOutput, Integer> maxDepthsFromOutputs) {
                final Integer out0Depth = maxDepthsFromOutputs.get(out0);
                assert out0Depth != null;
                assert out0Depth == 0;
                assert maxDepthsFromOutputs.get(hidden2) == 1;
                assert maxDepthsFromOutputs.get(hidden1) == 2;
                assert maxDepthsFromOutputs.get(hidden0) == 3;
                assert maxDepthsFromOutputs.get(in0) == 4;
                m_noteLayerDepthsCalledAfterCounter.set(m_counter.get());
            }
        });

        checkCommonPostconditions(5);
    }

    @Test
    public void testMultiInputLinearGraphIteration() {
        final DLKerasDefaultInputLayer in0 = new DLKerasDefaultInputLayer();

        final DLKerasDefaultInputLayer in1 = new DLKerasDefaultInputLayer();

        final DLKerasAddLayer hidden0 = new DLKerasAddLayer();
        hidden0.setParent(0, in0);
        hidden0.setParent(1, in1);

        final DLKerasDenseLayer hidden1 = new DLKerasDenseLayer();
        hidden1.setParent(0, hidden0);

        final DLKerasDenseLayer hidden2 = new DLKerasDenseLayer();
        hidden2.setParent(0, hidden1);

        final DLKerasDenseLayer out0 = new DLKerasDenseLayer();
        out0.setParent(0, hidden2);

        new DLKerasNetworkLayerGraphTopologicalOrderIterator(Arrays.asList(out0)).visitAll(new DLKerasLayerVisitor() {

            @Override
            public void visitOutput(final DLKerasInnerLayer outputLayer) throws Exception {
                assert m_counter.getAndIncrement() == 5;
                assert outputLayer == out0;
                assert outputLayer.getParent(0) == hidden2;
            }

            @Override
            public void visitHidden(final DLKerasInnerLayer hiddenLayer) throws Exception {
                final int i = m_counter.getAndIncrement();
                if (i == 2) {
                    assert hiddenLayer == hidden0;
                    assert hiddenLayer.getNumParents() == 2;
                    assert hiddenLayer.getParent(0) == in0;
                    assert hiddenLayer.getParent(1) == in1;
                } else if (i == 3) {
                    assert hiddenLayer == hidden1;
                    assert hiddenLayer.getNumParents() == 1;
                    assert hiddenLayer.getParent(0) == hidden0;
                } else if (i == 4) {
                    assert hiddenLayer == hidden2;
                    assert hiddenLayer.getNumParents() == 1;
                    assert hiddenLayer.getParent(0) == hidden1;
                } else {
                    fail();
                }
            }

            @Override
            public void visitInput(final DLKerasInputLayer inputLayer) throws Exception {
                final int i = m_counter.getAndIncrement();
                if (i == 0) {
                    assert inputLayer == in0;
                } else if (i == 1) {
                    assert inputLayer == in1;
                } else {
                    fail();
                }
            }

            @Override
            public void visitInputOutput(final DLKerasInputLayer inputOutputLayer) throws Exception {
                Assert.fail("Network does not contain input layers that are also outputs.");
            }

            @Override
            public void visitBaseNetworkOutput(final DLKerasBaseNetworkTensorSpecOutput baseNetworkOutput) {
                Assert.fail("Network is not connected to a base network.");
            }

            @Override
            public void noteLayerDepths(final LinkedHashMap<DLKerasTensorSpecsOutput, Integer> maxDepthsFromOutputs) {
                final Integer out0Depth = maxDepthsFromOutputs.get(out0);
                assert out0Depth != null;
                assert out0Depth == 0;
                assert maxDepthsFromOutputs.get(hidden2) == 1;
                assert maxDepthsFromOutputs.get(hidden1) == 2;
                assert maxDepthsFromOutputs.get(hidden0) == 3;
                assert maxDepthsFromOutputs.get(in0) == 4;
                assert maxDepthsFromOutputs.get(in1) == 4;
                m_noteLayerDepthsCalledAfterCounter.set(m_counter.get());
            }
        });

        checkCommonPostconditions(6);
    }

    @Test
    public void testMultiOutputLinearGraphIteration() {
        final DLKerasDefaultInputLayer in0 = new DLKerasDefaultInputLayer();

        final DLKerasDenseLayer hidden0 = new DLKerasDenseLayer();
        hidden0.setParent(0, in0);

        final DLKerasDenseLayer hidden1 = new DLKerasDenseLayer();
        hidden1.setParent(0, hidden0);

        final DLKerasDenseLayer hidden2 = new DLKerasDenseLayer();
        hidden2.setParent(0, hidden1);

        final DLKerasDenseLayer out0 = new DLKerasDenseLayer();
        out0.setParent(0, hidden2);

        final DLKerasDenseLayer out1 = new DLKerasDenseLayer();
        out1.setParent(0, hidden2);

        new DLKerasNetworkLayerGraphTopologicalOrderIterator(Arrays.asList(out0, out1))
            .visitAll(new DLKerasLayerVisitor() {

                @Override
                public void visitOutput(final DLKerasInnerLayer outputLayer) throws Exception {
                    final int i = m_counter.getAndIncrement();
                    if (i == 4) {
                        assert outputLayer == out0;
                        assert outputLayer.getParent(0) == hidden2;
                    } else if (i == 5) {
                        assert outputLayer == out1;
                        assert outputLayer.getParent(0) == hidden2;
                    } else {
                        fail();
                    }
                }

                @Override
                public void visitHidden(final DLKerasInnerLayer hiddenLayer) throws Exception {
                    final int i = m_counter.getAndIncrement();
                    if (i == 1) {
                        assert hiddenLayer == hidden0;
                        assert hiddenLayer.getNumParents() == 1;
                        assert hiddenLayer.getParent(0) == in0;
                    } else if (i == 2) {
                        assert hiddenLayer == hidden1;
                        assert hiddenLayer.getNumParents() == 1;
                        assert hiddenLayer.getParent(0) == hidden0;
                    } else if (i == 3) {
                        assert hiddenLayer == hidden2;
                        assert hiddenLayer.getNumParents() == 1;
                        assert hiddenLayer.getParent(0) == hidden1;
                    } else {
                        fail();
                    }
                }

                @Override
                public void visitInput(final DLKerasInputLayer inputLayer) throws Exception {
                    assert m_counter.getAndIncrement() == 0;
                    assert inputLayer == in0;
                }

                @Override
                public void visitInputOutput(final DLKerasInputLayer inputOutputLayer) throws Exception {
                    Assert.fail("Network does not contain input layers that are also outputs.");
                }

                @Override
                public void visitBaseNetworkOutput(final DLKerasBaseNetworkTensorSpecOutput baseNetworkOutput) {
                    Assert.fail("Network is not connected to a base network.");
                }

                @Override
                public void
                    noteLayerDepths(final LinkedHashMap<DLKerasTensorSpecsOutput, Integer> maxDepthsFromOutputs) {
                    final Integer out0Depth = maxDepthsFromOutputs.get(out0);
                    assert out0Depth != null;
                    assert out0Depth == 0;
                    final Integer out1Depth = maxDepthsFromOutputs.get(out1);
                    assert out1Depth != null;
                    assert out1Depth == 0;
                    assert maxDepthsFromOutputs.get(hidden2) == 1;
                    assert maxDepthsFromOutputs.get(hidden1) == 2;
                    assert maxDepthsFromOutputs.get(hidden0) == 3;
                    assert maxDepthsFromOutputs.get(in0) == 4;
                    m_noteLayerDepthsCalledAfterCounter.set(m_counter.get());
                }
            });

        checkCommonPostconditions(6);
    }

    @Test
    public void testMultiInputMultiOutputLinearGraphIteration() {
        final DLKerasDefaultInputLayer in0 = new DLKerasDefaultInputLayer();

        final DLKerasDefaultInputLayer in1 = new DLKerasDefaultInputLayer();

        final DLKerasAddLayer hidden0 = new DLKerasAddLayer();
        hidden0.setParent(0, in0);
        hidden0.setParent(1, in1);

        final DLKerasDenseLayer hidden1 = new DLKerasDenseLayer();
        hidden1.setParent(0, hidden0);

        final DLKerasDenseLayer hidden2 = new DLKerasDenseLayer();
        hidden2.setParent(0, hidden1);

        final DLKerasDenseLayer out0 = new DLKerasDenseLayer();
        out0.setParent(0, hidden2);

        final DLKerasDenseLayer out1 = new DLKerasDenseLayer();
        out1.setParent(0, hidden2);

        new DLKerasNetworkLayerGraphTopologicalOrderIterator(Arrays.asList(out0, out1))
            .visitAll(new DLKerasLayerVisitor() {

                @Override
                public void visitOutput(final DLKerasInnerLayer outputLayer) throws Exception {
                    final int i = m_counter.getAndIncrement();
                    if (i == 5) {
                        assert outputLayer == out0;
                        assert outputLayer.getParent(0) == hidden2;
                    } else if (i == 6) {
                        assert outputLayer == out1;
                        assert outputLayer.getParent(0) == hidden2;
                    } else {
                        fail();
                    }
                }

                @Override
                public void visitHidden(final DLKerasInnerLayer hiddenLayer) throws Exception {
                    final int i = m_counter.getAndIncrement();
                    if (i == 2) {
                        assert hiddenLayer == hidden0;
                        assert hiddenLayer.getNumParents() == 2;
                        assert hiddenLayer.getParent(0) == in0;
                        assert hiddenLayer.getParent(1) == in1;
                    } else if (i == 3) {
                        assert hiddenLayer == hidden1;
                        assert hiddenLayer.getNumParents() == 1;
                        assert hiddenLayer.getParent(0) == hidden0;
                    } else if (i == 4) {
                        assert hiddenLayer == hidden2;
                        assert hiddenLayer.getNumParents() == 1;
                        assert hiddenLayer.getParent(0) == hidden1;
                    } else {
                        fail();
                    }
                }

                @Override
                public void visitInput(final DLKerasInputLayer inputLayer) throws Exception {
                    final int i = m_counter.getAndIncrement();
                    if (i == 0) {
                        assert inputLayer == in0;
                    } else if (i == 1) {
                        assert inputLayer == in1;
                    } else {
                        fail();
                    }
                }

                @Override
                public void visitInputOutput(final DLKerasInputLayer inputOutputLayer) throws Exception {
                    Assert.fail("Network does not contain input layers that are also outputs.");
                }

                @Override
                public void visitBaseNetworkOutput(final DLKerasBaseNetworkTensorSpecOutput baseNetworkOutput) {
                    Assert.fail("Network is not connected to a base network.");
                }

                @Override
                public void
                    noteLayerDepths(final LinkedHashMap<DLKerasTensorSpecsOutput, Integer> maxDepthsFromOutputs) {
                    final Integer out0Depth = maxDepthsFromOutputs.get(out0);
                    assert out0Depth != null;
                    assert out0Depth == 0;
                    final Integer out1Depth = maxDepthsFromOutputs.get(out1);
                    assert out1Depth != null;
                    assert out1Depth == 0;
                    assert maxDepthsFromOutputs.get(hidden2) == 1;
                    assert maxDepthsFromOutputs.get(hidden1) == 2;
                    assert maxDepthsFromOutputs.get(hidden0) == 3;
                    assert maxDepthsFromOutputs.get(in0) == 4;
                    assert maxDepthsFromOutputs.get(in1) == 4;
                    m_noteLayerDepthsCalledAfterCounter.set(m_counter.get());
                }
            });

        checkCommonPostconditions(7);
    }

    @Test
    public void testMultiInputMultiOutputForkJoinGraphIteration() {
        final DLKerasDefaultInputLayer in0 = new DLKerasDefaultInputLayer();

        final DLKerasDefaultInputLayer in1 = new DLKerasDefaultInputLayer();

        final DLKerasDefaultInputLayer in2 = new DLKerasDefaultInputLayer();

        final DLKerasDenseLayer hidden0 = new DLKerasDenseLayer();
        hidden0.setParent(0, in0);

        final DLKerasAddLayer hidden1 = new DLKerasAddLayer();
        hidden1.setParent(0, hidden0);
        hidden1.setParent(1, in1);

        final DLKerasAddLayer hidden2 = new DLKerasAddLayer();
        hidden2.setParent(0, hidden1);
        hidden2.setParent(1, in2);

        final DLKerasAddLayer hidden3 = new DLKerasAddLayer();
        hidden3.setParent(0, hidden0);
        hidden3.setParent(1, hidden2);

        final DLKerasDenseLayer hidden4 = new DLKerasDenseLayer();
        hidden4.setParent(0, hidden2);

        final DLKerasDenseLayer hidden5 = new DLKerasDenseLayer();
        hidden5.setParent(0, hidden3);

        final DLKerasDenseLayer hidden6 = new DLKerasDenseLayer();
        hidden6.setParent(0, hidden3);

        final DLKerasDenseLayer hidden7 = new DLKerasDenseLayer();
        hidden7.setParent(0, hidden4);

        final DLKerasDenseLayer out0 = new DLKerasDenseLayer();
        out0.setParent(0, hidden5);

        final DLKerasDenseLayer out1 = new DLKerasDenseLayer();
        out1.setParent(0, hidden5);

        final DLKerasAddLayer out2 = new DLKerasAddLayer();
        out2.setParent(0, hidden6);
        out2.setParent(1, hidden7);

        new DLKerasNetworkLayerGraphTopologicalOrderIterator(Arrays.asList(out0, out1, out2))
            .visitAll(new DLKerasLayerVisitor() {

                @Override
                public void visitOutput(final DLKerasInnerLayer outputLayer) throws Exception {
                    final int i = m_counter.getAndIncrement();
                    if (i == 11) {
                        assert outputLayer == out0;
                        assert outputLayer.getParent(0) == hidden5;
                    } else if (i == 12) {
                        assert outputLayer == out1;
                        assert outputLayer.getParent(0) == hidden5;
                    } else if (i == 13) {
                        assert outputLayer == out2;
                        assert outputLayer.getParent(0) == hidden6;
                        assert outputLayer.getParent(1) == hidden7;
                    } else {
                        fail();
                    }
                }

                @Override
                public void visitHidden(final DLKerasInnerLayer hiddenLayer) throws Exception {
                    final int i = m_counter.getAndIncrement();
                    if (i == 8) {
                        assert hiddenLayer == hidden5;
                        assert hiddenLayer.getNumParents() == 1;
                        assert hiddenLayer.getParent(0) == hidden3;
                    } else if (i == 9) {
                        assert hiddenLayer == hidden6;
                        assert hiddenLayer.getNumParents() == 1;
                        assert hiddenLayer.getParent(0) == hidden3;
                    } else if (i == 10) {
                        assert hiddenLayer == hidden7;
                        assert hiddenLayer.getNumParents() == 1;
                        assert hiddenLayer.getParent(0) == hidden4;
                    } else if (i == 6) {
                        assert hiddenLayer == hidden3;
                        assert hiddenLayer.getNumParents() == 2;
                        assert hiddenLayer.getParent(0) == hidden0;
                        assert hiddenLayer.getParent(1) == hidden2;
                    } else if (i == 7) {
                        assert hiddenLayer == hidden4;
                        assert hiddenLayer.getNumParents() == 1;
                        assert hiddenLayer.getParent(0) == hidden2;
                    } else if (i == 5) {
                        assert hiddenLayer == hidden2;
                        assert hiddenLayer.getNumParents() == 2;
                        assert hiddenLayer.getParent(0) == hidden1;
                        assert hiddenLayer.getParent(1) == in2;
                    } else if (i == 3) {
                        assert hiddenLayer == hidden1;
                        assert hiddenLayer.getNumParents() == 2;
                        assert hiddenLayer.getParent(0) == hidden0;
                        assert hiddenLayer.getParent(1) == in1;
                    } else if (i == 1) {
                        assert hiddenLayer == hidden0;
                        assert hiddenLayer.getNumParents() == 1;
                        assert hiddenLayer.getParent(0) == in0;
                    } else {
                        fail();
                    }
                }

                @Override
                public void visitInput(final DLKerasInputLayer inputLayer) throws Exception {
                    final int i = m_counter.getAndIncrement();
                    if (i == 0) {
                        assert inputLayer == in0;
                    } else if (i == 2) {
                        assert inputLayer == in1;
                    } else if (i == 4) {
                        assert inputLayer == in2;
                    } else {
                        fail();
                    }
                }

                @Override
                public void visitInputOutput(final DLKerasInputLayer inputOutputLayer) throws Exception {
                    Assert.fail("Network does not contain input layers that are also outputs.");
                }

                @Override
                public void visitBaseNetworkOutput(final DLKerasBaseNetworkTensorSpecOutput baseNetworkOutput) {
                    Assert.fail("Network is not connected to a base network.");
                }

                @Override
                public void
                    noteLayerDepths(final LinkedHashMap<DLKerasTensorSpecsOutput, Integer> maxDepthsFromOutputs) {
                    final Integer out0Depth = maxDepthsFromOutputs.get(out0);
                    assert out0Depth != null;
                    assert out0Depth == 0;
                    final Integer out1Depth = maxDepthsFromOutputs.get(out1);
                    assert out1Depth != null;
                    assert out1Depth == 0;
                    final Integer out2Depth = maxDepthsFromOutputs.get(out2);
                    assert out2Depth != null;
                    assert out2Depth == 0;
                    assert maxDepthsFromOutputs.get(hidden5) == 1;
                    assert maxDepthsFromOutputs.get(hidden6) == 1;
                    assert maxDepthsFromOutputs.get(hidden7) == 1;
                    assert maxDepthsFromOutputs.get(hidden3) == 2;
                    assert maxDepthsFromOutputs.get(hidden4) == 2;
                    assert maxDepthsFromOutputs.get(hidden2) == 3;
                    assert maxDepthsFromOutputs.get(hidden1) == 4;
                    assert maxDepthsFromOutputs.get(in2) == 4;
                    assert maxDepthsFromOutputs.get(hidden0) == 5;
                    assert maxDepthsFromOutputs.get(in1) == 5;
                    assert maxDepthsFromOutputs.get(in0) == 6;
                    m_noteLayerDepthsCalledAfterCounter.set(m_counter.get());
                }
            });
        assert m_counter.get() == 14;
    }

    private void checkCommonPostconditions(final int expectedCounter) {
        assert m_counter.get() == expectedCounter;
        assert m_noteLayerDepthsCalledAfterCounter.get() == expectedCounter;
    }
}
