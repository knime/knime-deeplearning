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

import java.util.Arrays;

import org.junit.Test;
import org.knime.core.node.NodeLogger;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public final class DLKerasNetworkLayerNameGeneratorTest {

    @Test
    public void testReservedNames() {
        assertLayerNameEqualsForReservedNames("layer", "layer_6", "layer_5");
        assertLayerNameEqualsForReservedNames("layer", "layer_16", "layer_15");

        assertLayerNameEqualsForReservedNames("layer", "layer_3", "layer_2_3:4");
        assertLayerNameEqualsForReservedNames("layer", "layer_13", "layer_12_13:14");

        assertLayerNameEqualsForReservedNames("layer", "layer_4", "layer_3:4");
        assertLayerNameEqualsForReservedNames("layer", "layer_14", "layer_13:14");

        assertLayerNameEqualsForReservedNames("layer", "layer_7", "layer_6/op:8");
        assertLayerNameEqualsForReservedNames("layer", "layer_17", "layer_16/op:18");

        assertLayerNameEqualsForReservedNames("layer", "layer_5", "layer_4_op:6");
        assertLayerNameEqualsForReservedNames("layer", "layer_15", "layer_14_op:16");

        assertLayerNameEqualsForReservedNames("layer_snake_case", "layer_snake_case_4", "layer_snake_case_3");
        assertLayerNameEqualsForReservedNames("layer_snake_case", "layer_snake_case_14", "layer_snake_case_13");

        assertLayerNameEqualsForReservedNames("layer_snake_case_1", "layer_snake_case_1_4", "layer_snake_case_1_3");
        assertLayerNameEqualsForReservedNames("layer_snake_case_1_3", "layer_snake_case_1_3_5",
            "layer_snake_case_1_3_4");

        // Would currently fail. Pending.
        NodeLogger.getLogger(DLKerasLayerTestSetups.class)
            .warn("DL Keras: Skipping some assertions that rely on pending work.");
        // assertLayerNameEqualsForReservedNames("layer_snake_case_1", "layer_snake_case_1_3", "layer_snake_case_1_2_3:4");
    }

    private void assertLayerNameEqualsForReservedNames(final String layerPrefix, final String expectedName,
        final String... reservedNames) {
        final String layerName = getLayerNameForReservedNames(layerPrefix, reservedNames);
        assert layerName.equals(expectedName) : "layer name '" + layerName + "' vs expected '" + expectedName + "'";
    }

    private String getLayerNameForReservedNames(final String layerPrefix, final String... reservedNames) {
        return new DLKerasNetworkLayerNameGenerator(Arrays.asList(reservedNames)).getNextLayerName(layerPrefix);
    }
}
