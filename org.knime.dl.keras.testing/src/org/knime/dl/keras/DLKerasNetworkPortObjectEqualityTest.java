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
package org.knime.dl.keras;

import java.io.IOException;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;
import org.knime.dl.keras.base.portobjects.DLKerasNetworkPortObject;
import org.knime.dl.keras.tensorflow.core.DLKerasTensorFlowNetwork;
import org.knime.dl.keras.testing.DLKerasTestUtil;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public class DLKerasNetworkPortObjectEqualityTest {

	// TODO: These are just randomized coverage tests. Some more targeted tests would be useful (e.g. tests that check
	// the inequality of networks that only differ in one property).

	@Test
    public void testEquals() throws IOException {
		final DLKerasTensorFlowNetwork net1 = DLKerasTestUtil.randomNetwork(new Random(1234));
		final DLKerasTensorFlowNetwork net2 = DLKerasTestUtil.randomNetwork(new Random(1234));
		final DLKerasTensorFlowNetwork net3 = DLKerasTestUtil.randomNetwork(new Random(1235));

		final DLKerasNetworkPortObject po1 = new DLKerasNetworkPortObject(net1);
		final DLKerasNetworkPortObject po2 = new DLKerasNetworkPortObject(net2);
		final DLKerasNetworkPortObject po3 = new DLKerasNetworkPortObject(net3);

		Assert.assertEquals(po1, po2);
		Assert.assertNotEquals(po1, po3);
		Assert.assertNotEquals(po2, po3);
	}

	@Test
    public void testHashCode() throws IOException {
		final DLKerasTensorFlowNetwork net1 = DLKerasTestUtil.randomNetwork(new Random(1234));
		final DLKerasTensorFlowNetwork net2 = DLKerasTestUtil.randomNetwork(new Random(1234));
		final DLKerasTensorFlowNetwork net3 = DLKerasTestUtil.randomNetwork(new Random(1235));

		final DLKerasNetworkPortObject po1 = new DLKerasNetworkPortObject(net1);
		final DLKerasNetworkPortObject po2 = new DLKerasNetworkPortObject(net2);
		final DLKerasNetworkPortObject po3 = new DLKerasNetworkPortObject(net3);

		Assert.assertEquals(po1.hashCode(), po2.hashCode());
		Assert.assertNotEquals(po1.hashCode(), po3.hashCode());
		Assert.assertNotEquals(po2.hashCode(), po3.hashCode());
	}
}
