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
package org.knime.dl.base.portobjects;

import java.util.OptionalLong;

import org.knime.core.node.ModelContent;
import org.knime.core.node.ModelContentWO;
import org.knime.core.node.port.AbstractSimplePortObject;
import org.knime.core.node.workflow.ModelContentOutPortView;
import org.knime.dl.core.DLDimensionOrder;
import org.knime.dl.core.DLNetworkSpec;
import org.knime.dl.core.DLTensorSpec;

/**
 * @author David Kolb, KNIME GmbH, Konstanz, Germany
 */
public class DLNetworkPortObjectSpecUtils {

    private DLNetworkPortObjectSpecUtils() {
        // Utils class
    }

    /**
     * Writes the meta info contained in the specified {@link DLNetworkSpec} to a newly created {@link ModelContent} (to
     * abuse the {@link ModelContentOutPortView}, which is e.g. used in {@link AbstractSimplePortObject}) in order to
     * create a nice tree view.
     * 
     * @param dlSpecs the specs to convert
     * @param name the name of the tree root
     * @return {@link ModelContent} containing meta info of the specified {@link DLNetworkSpec}
     */
    public static ModelContent networkSpecToModelContent(final DLNetworkSpec dlSpecs, final String name) {
        final ModelContent model = new ModelContent(name);
        addDLTensorSpecsToModelContent(model, dlSpecs.getInputSpecs(), "Input Specs");
        addDLTensorSpecsToModelContent(model, dlSpecs.getHiddenOutputSpecs(), "Hidden Specs");
        addDLTensorSpecsToModelContent(model, dlSpecs.getOutputSpecs(), "Output Specs");
        return model;
    }

    private static void addDLTensorSpecsToModelContent(final ModelContent model, final DLTensorSpec[] tensorSpecs,
        String id) {
        ModelContentWO content = model.addModelContent(id);

        for (DLTensorSpec spec : tensorSpecs) {
            final String tensorSummary = spec.getIdentifier().getIdentifierString() + " " + spec.getShape().toString();
            final ModelContentWO specContent = content.addModelContent(tensorSummary);
            specContent.addString("Name", spec.getName());
            specContent.addString("Id", spec.getIdentifier().getIdentifierString());
            specContent.addString("Shape", spec.getShape().toString());
            final OptionalLong ol = spec.getBatchSize();
            specContent.addString("Batch Size", ol.isPresent() ? ol.getAsLong() + "" : "<none>");
            specContent.addString("Element Type", spec.getElementType().getSimpleName());
            final DLDimensionOrder dimo = spec.getDimensionOrder();
            specContent.addString("Dimension Order", dimo != null ? dimo.name() : "<none>");
        }
    }
}
