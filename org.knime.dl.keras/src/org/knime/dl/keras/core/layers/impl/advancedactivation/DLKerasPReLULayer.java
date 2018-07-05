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
package org.knime.dl.keras.core.layers.impl.advancedactivation;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import org.knime.core.node.InvalidSettingsException;
import org.knime.dl.keras.core.config.constraint.DLKerasConstraint;
import org.knime.dl.keras.core.config.constraint.DLKerasConstraintChoices;
import org.knime.dl.keras.core.config.initializer.DLKerasInitializer;
import org.knime.dl.keras.core.config.initializer.DLKerasInitializerChoices;
import org.knime.dl.keras.core.config.initializer.DLKerasZerosInitializer;
import org.knime.dl.keras.core.config.regularizer.DLKerasRegularizer;
import org.knime.dl.keras.core.config.regularizer.DLKerasRegularizerChoices;
import org.knime.dl.keras.core.layers.DLLayerUtils;
import org.knime.dl.keras.core.layers.dialog.tuple.DLKerasTuple;
import org.knime.dl.keras.core.layers.dialog.tuple.DLKerasTuple.Constraint;
import org.knime.dl.keras.core.struct.param.Required;
import org.knime.dl.keras.core.struct.param.Parameter;
import org.knime.dl.keras.util.DLKerasUtils;
import org.knime.dl.python.util.DLPythonUtils;

/**
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class DLKerasPReLULayer extends DLKerasAbstractAdvancedActivationLayer {

    @Parameter(label = "Alpha initializer", choices = DLKerasInitializerChoices.class)
    private DLKerasInitializer m_alphaInitializer = new DLKerasZerosInitializer();

    @Parameter(label = "Alpha regularizer", required = Required.OptionalAndNotEnabled, choices = DLKerasRegularizerChoices.class,
        tab = "Advanced")
    private DLKerasRegularizer m_alphaRegularizer = null;

    @Parameter(label = "Alpha constraint", required = Required.OptionalAndNotEnabled, choices = DLKerasConstraintChoices.class, tab = "Advanced")
    private DLKerasConstraint m_alphaConstraint = null;

    @Parameter(label = "Shared axes", required = Required.OptionalAndNotEnabled)
    private DLKerasTuple m_sharedAxes = new DLKerasTuple("1", 1, 1000, EnumSet.of(Constraint.ZERO, Constraint.NEGATIVE), false);

    /**
     */
    public DLKerasPReLULayer() {
        super("keras.layers.PReLU", DLLayerUtils.NUMERICAL_DTYPES);
    }

    @Override
    public void validateParameters() throws InvalidSettingsException {
        m_alphaInitializer.validateParameters();
        if (m_alphaRegularizer != null) {
            m_alphaRegularizer.validateParameters();
        }
        if (m_alphaConstraint != null) {
            m_alphaConstraint.validateParameters();
        }
    }

    @Override
    protected void populateParameters(List<String> positionalParams, Map<String, String> namedParams) {
        namedParams.put("alpha_initializer", m_alphaInitializer.getBackendRepresentation());
        namedParams.put("alpha_regularizer", DLKerasUtils.Layers.toPython(m_alphaRegularizer));
        namedParams.put("alpha_constraint", DLKerasUtils.Layers.toPython(m_alphaConstraint));
        namedParams.put("shared_axes", m_sharedAxes == null ? DLPythonUtils.NONE : m_sharedAxes.toPytonTuple());
    }

}
