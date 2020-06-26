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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.knime.dl.keras.core.layers.DLKerasNetworkGraphIterator.DLKerasLayerVisitor;
import org.knime.dl.keras.core.layers.impl.core.DLKerasDefaultInputLayer;
import org.knime.dl.keras.core.layers.impl.core.DLKerasDenseLayer;
import org.knime.dl.keras.core.layers.impl.merge.DLKerasAddLayer;
import org.knime.python2.testing.PreferencesSetup;

/**
 * Also see {@link DLKerasNetworkGraphTopologicalOrderIteratorTest}. Test cases should be kept in sync.
 *
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public final class DLKerasNetworkGraphDepthFirstIteratorTest {

    @ClassRule
    public static final TestRule preferencesSetup = new PreferencesSetup("org.knime.dl.keras.tests");

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

        new DLKerasNetworkGraphDepthFirstIterator(Arrays.asList(inout0)).visitAll(new DLKerasLayerVisitor() {

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
                assertEquals(0, m_counter.getAndIncrement());
                assertSame(inout0, inputOutputLayer);
            }

            @Override
            public void visitBaseNetworkOutput(final DLKerasBaseNetworkTensorSpecOutput baseNetworkOutput) {
                Assert.fail("Network is not connected to a base network.");
            }

            @Override
            public void noteLayerDepths(final Map<DLKerasTensorSpecsOutput, Integer> maxDepthsFromOutputs) {
                final Integer inout0Depth = maxDepthsFromOutputs.get(inout0);
                assertNotNull(inout0Depth);
                assertEquals(0, inout0Depth.intValue());
                m_noteLayerDepthsCalledAfterCounter.set(m_counter.get());
            }
        });

        checkCommonPostconditions(1);
    }

    @Test
    public void testLinearGraphIteration() {
        final DLKerasDefaultInputLayer in0 = new DLKerasDefaultInputLayer();
        in0.setRuntimeId("in0");

        final DLKerasDenseLayer hidden0 = new DLKerasDenseLayer();
        hidden0.setParent(0, in0);
        hidden0.setRuntimeId("0");

        final DLKerasDenseLayer hidden1 = new DLKerasDenseLayer();
        hidden1.setParent(0, hidden0);
        hidden1.setRuntimeId("1");

        final DLKerasDenseLayer hidden2 = new DLKerasDenseLayer();
        hidden2.setParent(0, hidden1);
        hidden2.setRuntimeId("2");

        final DLKerasDenseLayer out0 = new DLKerasDenseLayer();
        out0.setParent(0, hidden2);
        out0.setRuntimeId("3");

        new DLKerasNetworkGraphDepthFirstIterator(Arrays.asList(out0)).visitAll(new DLKerasLayerVisitor() {

            @Override
            public void visitOutput(final DLKerasInnerLayer outputLayer) throws Exception {
                assertEquals(0, m_counter.getAndIncrement());
                assertSame(out0, outputLayer);
                assertSame(hidden2, outputLayer.getParent(0));
            }

            @Override
            public void visitHidden(final DLKerasInnerLayer hiddenLayer) throws Exception {
                final int i = m_counter.getAndIncrement();
                if (i == 3) {
                    assertSame(hidden0, hiddenLayer);
                    assertEquals(1, hiddenLayer.getNumParents());
                    assertSame(in0, hiddenLayer.getParent(0));
                } else if (i == 2) {
                    assertSame(hidden1, hiddenLayer);
                    assertEquals(1, hiddenLayer.getNumParents());
                    assertSame(hidden0, hiddenLayer.getParent(0));
                } else if (i == 1) {
                    assertSame(hidden2, hiddenLayer);
                    assertEquals(1, hiddenLayer.getNumParents());
                    assertSame(hidden1, hiddenLayer.getParent(0));
                } else {
                    fail();
                }
            }

            @Override
            public void visitInput(final DLKerasInputLayer inputLayer) throws Exception {
                assertEquals(4, m_counter.getAndIncrement());
                assertSame(in0, inputLayer);
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
            public void noteLayerDepths(final Map<DLKerasTensorSpecsOutput, Integer> maxDepthsFromOutputs) {
                final Integer out0Depth = maxDepthsFromOutputs.get(out0);
                assertNotNull(out0Depth);
                assertEquals(0, out0Depth.intValue());
                assertEquals(1, maxDepthsFromOutputs.get(hidden2).intValue());
                assertEquals(2, maxDepthsFromOutputs.get(hidden1).intValue());
                assertEquals(3, maxDepthsFromOutputs.get(hidden0).intValue());
                assertEquals(4, maxDepthsFromOutputs.get(in0).intValue());
                m_noteLayerDepthsCalledAfterCounter.set(m_counter.get());
            }
        });

        checkCommonPostconditions(5);
    }

    @Test
    public void testMultiInputLinearGraphIteration() {
        final DLKerasDefaultInputLayer in0 = new DLKerasDefaultInputLayer();
        in0.setRuntimeId("in0");

        final DLKerasDefaultInputLayer in1 = new DLKerasDefaultInputLayer();
        in1.setRuntimeId("in1");

        final DLKerasAddLayer hidden0 = new DLKerasAddLayer();
        hidden0.setRuntimeId("0");
        hidden0.setParent(0, in0);
        hidden0.setParent(1, in1);

        final DLKerasDenseLayer hidden1 = new DLKerasDenseLayer();
        hidden1.setParent(0, hidden0);
        hidden1.setRuntimeId("1");

        final DLKerasDenseLayer hidden2 = new DLKerasDenseLayer();
        hidden2.setParent(0, hidden1);
        hidden2.setRuntimeId("2");

        final DLKerasDenseLayer out0 = new DLKerasDenseLayer();
        out0.setRuntimeId("3");
        out0.setParent(0, hidden2);

        new DLKerasNetworkGraphDepthFirstIterator(Arrays.asList(out0)).visitAll(new DLKerasLayerVisitor() {

            @Override
            public void visitOutput(final DLKerasInnerLayer outputLayer) throws Exception {
                assertEquals(0, m_counter.getAndIncrement());
                assertSame(out0, outputLayer);
                assertSame(hidden2, outputLayer.getParent(0));
            }

            @Override
            public void visitHidden(final DLKerasInnerLayer hiddenLayer) throws Exception {
                final int i = m_counter.getAndIncrement();
                if (i == 3) {
                    assertSame(hidden0, hiddenLayer);
                    assertEquals(2, hiddenLayer.getNumParents());
                    assertSame(in0, hiddenLayer.getParent(0));
                    assertSame(in1, hiddenLayer.getParent(1));
                } else if (i == 2) {
                    assertSame(hidden1, hiddenLayer);
                    assertEquals(1, hiddenLayer.getNumParents());
                    assertSame(hidden0, hiddenLayer.getParent(0));
                } else if (i == 1) {
                    assertSame(hidden2, hiddenLayer);
                    assertEquals(1, hiddenLayer.getNumParents());
                    assertSame(hidden1, hiddenLayer.getParent(0));
                } else {
                    fail();
                }
            }

            @Override
            public void visitInput(final DLKerasInputLayer inputLayer) throws Exception {
                final int i = m_counter.getAndIncrement();
                if (i == 4) {
                    assertSame(in0, inputLayer);
                } else if (i == 5) {
                    assertSame(in1, inputLayer);
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
            public void noteLayerDepths(final Map<DLKerasTensorSpecsOutput, Integer> maxDepthsFromOutputs) {
                final Integer out0Depth = maxDepthsFromOutputs.get(out0);
                assertNotNull(out0Depth);
                assertEquals(0, out0Depth.intValue());
                assertEquals(1, maxDepthsFromOutputs.get(hidden2).intValue());
                assertEquals(2, maxDepthsFromOutputs.get(hidden1).intValue());
                assertEquals(3, maxDepthsFromOutputs.get(hidden0).intValue());
                assertEquals(4, maxDepthsFromOutputs.get(in0).intValue());
                assertEquals(4, maxDepthsFromOutputs.get(in1).intValue());
                m_noteLayerDepthsCalledAfterCounter.set(m_counter.get());
            }
        });

        checkCommonPostconditions(6);
    }

    @Test
    public void testMultiOutputLinearGraphIteration() {
        final DLKerasDefaultInputLayer in0 = new DLKerasDefaultInputLayer();
        in0.setRuntimeId("in0");

        final DLKerasDenseLayer hidden0 = new DLKerasDenseLayer();
        hidden0.setRuntimeId("0");
        hidden0.setParent(0, in0);

        final DLKerasDenseLayer hidden1 = new DLKerasDenseLayer();
        hidden1.setRuntimeId("1");
        hidden1.setParent(0, hidden0);

        final DLKerasDenseLayer hidden2 = new DLKerasDenseLayer();
        hidden2.setRuntimeId("2");
        hidden2.setParent(0, hidden1);

        final DLKerasDenseLayer out0 = new DLKerasDenseLayer();
        out0.setRuntimeId("3");
        out0.setParent(0, hidden2);

        final DLKerasDenseLayer out1 = new DLKerasDenseLayer();
        out1.setRuntimeId("4");
        out1.setParent(0, hidden2);

        new DLKerasNetworkGraphDepthFirstIterator(Arrays.asList(out0, out1)).visitAll(new DLKerasLayerVisitor() {

            @Override
            public void visitOutput(final DLKerasInnerLayer outputLayer) throws Exception {
                final int i = m_counter.getAndIncrement();
                if (i == 0) {
                    assertSame(out0, outputLayer);
                    assertSame(hidden2, outputLayer.getParent(0));
                } else if (i == 5) {
                    assertSame(out1, outputLayer);
                    assertSame(hidden2, outputLayer.getParent(0));
                } else {
                    fail();
                }
            }

            @Override
            public void visitHidden(final DLKerasInnerLayer hiddenLayer) throws Exception {
                final int i = m_counter.getAndIncrement();
                if (i == 3) {
                    assertSame(hidden0, hiddenLayer);
                    assertEquals(1, hiddenLayer.getNumParents());
                    assertSame(in0, hiddenLayer.getParent(0));
                } else if (i == 2) {
                    assertSame(hidden1, hiddenLayer);
                    assertEquals(1, hiddenLayer.getNumParents());
                    assertSame(hidden0, hiddenLayer.getParent(0));
                } else if (i == 1) {
                    assertSame(hidden2, hiddenLayer);
                    assertEquals(1, hiddenLayer.getNumParents());
                    assertSame(hidden1, hiddenLayer.getParent(0));
                } else {
                    fail();
                }
            }

            @Override
            public void visitInput(final DLKerasInputLayer inputLayer) throws Exception {
                assertEquals(4, m_counter.getAndIncrement());
                assertSame(in0, inputLayer);
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
            public void noteLayerDepths(final Map<DLKerasTensorSpecsOutput, Integer> maxDepthsFromOutputs) {
                final Integer out0Depth = maxDepthsFromOutputs.get(out0);
                assertNotNull(out0Depth);
                assertEquals(0, out0Depth.intValue());
                final Integer out1Depth = maxDepthsFromOutputs.get(out1);
                assertNotNull(out1Depth);
                assertEquals(0, out1Depth.intValue());
                assertEquals(1, maxDepthsFromOutputs.get(hidden2).intValue());
                assertEquals(2, maxDepthsFromOutputs.get(hidden1).intValue());
                assertEquals(3, maxDepthsFromOutputs.get(hidden0).intValue());
                assertEquals(4, maxDepthsFromOutputs.get(in0).intValue());
                m_noteLayerDepthsCalledAfterCounter.set(m_counter.get());
            }
        });

        checkCommonPostconditions(6);
    }

    @Test
    public void testMultiInputMultiOutputLinearGraphIteration() {
        final DLKerasDefaultInputLayer in0 = new DLKerasDefaultInputLayer();
        in0.setRuntimeId("in0");

        final DLKerasDefaultInputLayer in1 = new DLKerasDefaultInputLayer();
        in1.setRuntimeId("in1");

        final DLKerasAddLayer hidden0 = new DLKerasAddLayer();
        hidden0.setRuntimeId("0");

        hidden0.setParent(0, in0);
        hidden0.setParent(1, in1);

        final DLKerasDenseLayer hidden1 = new DLKerasDenseLayer();
        hidden1.setRuntimeId("1");
        hidden1.setParent(0, hidden0);

        final DLKerasDenseLayer hidden2 = new DLKerasDenseLayer();
        hidden2.setRuntimeId("2");
        hidden2.setParent(0, hidden1);

        final DLKerasDenseLayer out0 = new DLKerasDenseLayer();
        out0.setRuntimeId("3");
        out0.setParent(0, hidden2);

        final DLKerasDenseLayer out1 = new DLKerasDenseLayer();
        out1.setRuntimeId("4");
        out1.setParent(0, hidden2);

        new DLKerasNetworkGraphDepthFirstIterator(Arrays.asList(out0, out1)).visitAll(new DLKerasLayerVisitor() {

            @Override
            public void visitOutput(final DLKerasInnerLayer outputLayer) throws Exception {
                final int i = m_counter.getAndIncrement();
                if (i == 0) {
                    assertSame(out0, outputLayer);
                    assertSame(hidden2, outputLayer.getParent(0));
                } else if (i == 6) {
                    assertSame(out1, outputLayer);
                    assertSame(hidden2, outputLayer.getParent(0));
                } else {
                    fail();
                }
            }

            @Override
            public void visitHidden(final DLKerasInnerLayer hiddenLayer) throws Exception {
                final int i = m_counter.getAndIncrement();
                if (i == 3) {
                    assertSame(hidden0, hiddenLayer);
                    assertEquals(2, hiddenLayer.getNumParents());
                    assertSame(in0, hiddenLayer.getParent(0));
                    assertSame(in1, hiddenLayer.getParent(1));
                } else if (i == 2) {
                    assertSame(hidden1, hiddenLayer);
                    assertEquals(1, hiddenLayer.getNumParents());
                    assertSame(hidden0, hiddenLayer.getParent(0));
                } else if (i == 1) {
                    assertSame(hidden2, hiddenLayer);
                    assertEquals(1, hiddenLayer.getNumParents());
                    assertSame(hidden1, hiddenLayer.getParent(0));
                } else {
                    fail();
                }
            }

            @Override
            public void visitInput(final DLKerasInputLayer inputLayer) throws Exception {
                final int i = m_counter.getAndIncrement();
                if (i == 4) {
                    assertSame(in0, inputLayer);
                } else if (i == 5) {
                    assertSame(in1, inputLayer);
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
            public void noteLayerDepths(final Map<DLKerasTensorSpecsOutput, Integer> maxDepthsFromOutputs) {
                final Integer out0Depth = maxDepthsFromOutputs.get(out0);
                assertNotNull(out0Depth);
                assertEquals(0, out0Depth.intValue());
                final Integer out1Depth = maxDepthsFromOutputs.get(out1);
                assertNotNull(out1Depth);
                assertEquals(0, out1Depth.intValue());
                assertEquals(1, maxDepthsFromOutputs.get(hidden2).intValue());
                assertEquals(2, maxDepthsFromOutputs.get(hidden1).intValue());
                assertEquals(3, maxDepthsFromOutputs.get(hidden0).intValue());
                assertEquals(4, maxDepthsFromOutputs.get(in0).intValue());
                assertEquals(4, maxDepthsFromOutputs.get(in1).intValue());
                m_noteLayerDepthsCalledAfterCounter.set(m_counter.get());
            }
        });

        checkCommonPostconditions(7);
    }

    @Test
    public void testMultiInputMultiOutputForkJoinGraphIteration() {
        final DLKerasDefaultInputLayer in0 = new DLKerasDefaultInputLayer();
        in0.setRuntimeId("in0");

        final DLKerasDefaultInputLayer in1 = new DLKerasDefaultInputLayer();
        in1.setRuntimeId("in1");

        final DLKerasDefaultInputLayer in2 = new DLKerasDefaultInputLayer();
        in2.setRuntimeId("in2");

        final DLKerasDenseLayer hidden0 = new DLKerasDenseLayer();
        hidden0.setRuntimeId("hidden0");
        hidden0.setParent(0, in0);

        final DLKerasAddLayer hidden1 = new DLKerasAddLayer();
        hidden1.setRuntimeId("hidden1");
        hidden1.setParent(0, hidden0);
        hidden1.setParent(1, in1);

        final DLKerasAddLayer hidden2 = new DLKerasAddLayer();
        hidden2.setRuntimeId("hidden2");
        hidden2.setParent(0, hidden1);
        hidden2.setParent(1, in2);

        final DLKerasAddLayer hidden3 = new DLKerasAddLayer();
        hidden3.setRuntimeId("hidden3");
        hidden3.setParent(0, hidden0);
        hidden3.setParent(1, hidden2);

        final DLKerasDenseLayer hidden4 = new DLKerasDenseLayer();
        hidden4.setRuntimeId("hidden4");
        hidden4.setParent(0, hidden2);

        final DLKerasDenseLayer hidden5 = new DLKerasDenseLayer();
        hidden5.setRuntimeId("hidden5");
        hidden5.setParent(0, hidden3);

        final DLKerasDenseLayer hidden6 = new DLKerasDenseLayer();
        hidden6.setRuntimeId("hidden6");
        hidden6.setParent(0, hidden3);

        final DLKerasDenseLayer hidden7 = new DLKerasDenseLayer();
        hidden7.setRuntimeId("hidden7");
        hidden7.setParent(0, hidden4);

        final DLKerasDenseLayer out0 = new DLKerasDenseLayer();
        out0.setRuntimeId("out0");
        out0.setParent(0, hidden5);

        final DLKerasDenseLayer out1 = new DLKerasDenseLayer();
        out1.setRuntimeId("out1");
        out1.setParent(0, hidden5);

        final DLKerasAddLayer out2 = new DLKerasAddLayer();
        out2.setRuntimeId("out2");
        out2.setParent(0, hidden6);
        out2.setParent(1, hidden7);

        new DLKerasNetworkGraphDepthFirstIterator(Arrays.asList(out0, out1, out2))
            .visitAll(new DLKerasLayerVisitor() {

                @Override
                public void visitOutput(final DLKerasInnerLayer outputLayer) throws Exception {
                    final int i = m_counter.getAndIncrement();
                    if (i == 0) {
                        assertSame(out0, outputLayer);
                        assertSame(hidden5, outputLayer.getParent(0));
                    } else if (i == 9) {
                        assertSame(out1, outputLayer);
                        assertSame(hidden5, outputLayer.getParent(0));
                    } else if (i == 10) {
                        assertSame(out2, outputLayer);
                        assertSame(hidden6, outputLayer.getParent(0));
                        assertSame(hidden7, outputLayer.getParent(1));
                    } else {
                        fail();
                    }
                }

                @Override
                public void visitHidden(final DLKerasInnerLayer hiddenLayer) throws Exception {
                    final int i = m_counter.getAndIncrement();
                    if (i == 1) {
                        assertSame(hidden5, hiddenLayer);
                        assertEquals(1, hiddenLayer.getNumParents());
                        assertSame(hidden3, hiddenLayer.getParent(0));
                    } else if (i == 11) {
                        assertSame(hidden6, hiddenLayer);
                        assertEquals(1, hiddenLayer.getNumParents());
                        assertSame(hidden3, hiddenLayer.getParent(0));
                    } else if (i == 12) {
                        assertSame(hidden7, hiddenLayer);
                        assertEquals(1, hiddenLayer.getNumParents());
                        assertSame(hidden4, hiddenLayer.getParent(0));
                    } else if (i == 2) {
                        assertSame(hidden3, hiddenLayer);
                        assertEquals(2, hiddenLayer.getNumParents());
                        assertSame(hidden0, hiddenLayer.getParent(0));
                        assertSame(hidden2, hiddenLayer.getParent(1));
                    } else if (i == 13) {
                        assertSame(hidden4, hiddenLayer);
                        assertEquals(1, hiddenLayer.getNumParents());
                        assertSame(hidden2, hiddenLayer.getParent(0));
                    } else if (i == 5) {
                        assertSame(hidden2, hiddenLayer);
                        assertEquals(2, hiddenLayer.getNumParents());
                        assertSame(hidden1, hiddenLayer.getParent(0));
                        assertSame(in2, hiddenLayer.getParent(1));
                    } else if (i == 6) {
                        assertSame(hidden1, hiddenLayer);
                        assertEquals(2, hiddenLayer.getNumParents());
                        assertSame(hidden0, hiddenLayer.getParent(0));
                        assertSame(in1, hiddenLayer.getParent(1));
                    } else if (i == 3) {
                        assertSame(hidden0, hiddenLayer);
                        assertEquals(1, hiddenLayer.getNumParents());
                        assertSame(in0, hiddenLayer.getParent(0));
                    } else {
                        fail();
                    }
                }

                @Override
                public void visitInput(final DLKerasInputLayer inputLayer) throws Exception {
                    final int i = m_counter.getAndIncrement();
                    if (i == 4) {
                        assertSame(in0, inputLayer);
                    } else if (i == 7) {
                        assertSame(in1, inputLayer);
                    } else if (i == 8) {
                        assertSame(in2, inputLayer);
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
                    noteLayerDepths(final Map<DLKerasTensorSpecsOutput, Integer> maxDepthsFromOutputs) {
                    final Integer out0Depth = maxDepthsFromOutputs.get(out0);
                    assertNotNull(out0Depth);
                    assertEquals(0, out0Depth.intValue());
                    final Integer out1Depth = maxDepthsFromOutputs.get(out1);
                    assertNotNull(out1Depth);
                    assertEquals(0, out1Depth.intValue());
                    final Integer out2Depth = maxDepthsFromOutputs.get(out2);
                    assertNotNull(out2Depth);
                    assertEquals(0, out2Depth.intValue());
                    assertEquals(1, maxDepthsFromOutputs.get(hidden5).intValue());
                    assertEquals(1, maxDepthsFromOutputs.get(hidden6).intValue());
                    assertEquals(1, maxDepthsFromOutputs.get(hidden7).intValue());
                    assertEquals(2, maxDepthsFromOutputs.get(hidden3).intValue());
                    assertEquals(2, maxDepthsFromOutputs.get(hidden4).intValue());
                    assertEquals(3, maxDepthsFromOutputs.get(hidden2).intValue());
                    assertEquals(4, maxDepthsFromOutputs.get(hidden1).intValue());
                    assertEquals(4, maxDepthsFromOutputs.get(in2).intValue());
                    assertEquals(5, maxDepthsFromOutputs.get(hidden0).intValue());
                    assertEquals(5, maxDepthsFromOutputs.get(in1).intValue());
                    assertEquals(6, maxDepthsFromOutputs.get(in0).intValue());
                    m_noteLayerDepthsCalledAfterCounter.set(m_counter.get());
                }
            });
        assertEquals(14, m_counter.get());
    }

    private void checkCommonPostconditions(final int expectedCounter) {
        assertEquals(expectedCounter, m_counter.get());
        assertEquals(expectedCounter, m_noteLayerDepthsCalledAfterCounter.get());
    }
}
