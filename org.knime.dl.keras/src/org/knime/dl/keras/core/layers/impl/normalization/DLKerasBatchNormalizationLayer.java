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
package org.knime.dl.keras.core.layers.impl.normalization;

import java.util.List;
import java.util.Map;

import org.knime.core.node.InvalidSettingsException;
import org.knime.dl.keras.core.config.DLKerasConfigObjectUtils;
import org.knime.dl.keras.core.config.constraint.DLKerasConstraint;
import org.knime.dl.keras.core.config.constraint.DLKerasConstraintChoices;
import org.knime.dl.keras.core.config.initializer.DLKerasInitializer;
import org.knime.dl.keras.core.config.initializer.DLKerasInitializerChoices;
import org.knime.dl.keras.core.config.initializer.DLKerasOnesInitializer;
import org.knime.dl.keras.core.config.initializer.DLKerasZerosInitializer;
import org.knime.dl.keras.core.config.regularizer.DLKerasRegularizer;
import org.knime.dl.keras.core.config.regularizer.DLKerasRegularizerChoices;
import org.knime.dl.keras.core.layers.DLInvalidTensorSpecException;
import org.knime.dl.keras.core.layers.DLKerasAbstractUnaryLayer;
import org.knime.dl.keras.core.struct.param.Parameter;
import org.knime.dl.python.util.DLPythonUtils;

/**
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 */
public class DLKerasBatchNormalizationLayer extends DLKerasAbstractUnaryLayer {

    @Parameter(label = "Axis", min = "1", max = "100", stepSize = "1")
    private int m_axis = 1;

    @Parameter(label = "Momentum", min = "0", max = "1", stepSize = "0.01")
    private double m_momentum = 0.99;

    // TODO smaller values get rounded to zero when the dialog opens
    @Parameter(label = "Epsilon", min = "0", max = "1", stepSize = "0.001")
    private double m_epsilon = 0.001;

    // TODO default is not selected
    @Parameter(label = "Center")
    private boolean m_center = true;

    // TODO default is not selected
    @Parameter(label = "Scale")
    private boolean m_scale = true;

    @Parameter(label = "Beta Initializer", choices = DLKerasInitializerChoices.class)
    private DLKerasInitializer m_betaInitializer = new DLKerasZerosInitializer();

    @Parameter(label = "Gamma Initializer", choices = DLKerasInitializerChoices.class)
    private DLKerasInitializer m_gammaInitializer = new DLKerasOnesInitializer();

    @Parameter(label = "Moving Mean Initializer", choices = DLKerasInitializerChoices.class)
    private DLKerasInitializer m_movingMeanInitializer = new DLKerasZerosInitializer();

    @Parameter(label = "Moving Variance Initializer", choices = DLKerasInitializerChoices.class)
    private DLKerasInitializer m_movingVarianceInitializer = new DLKerasOnesInitializer();

    @Parameter(label = "Beta Regularizer", required = false, choices = DLKerasRegularizerChoices.class)
    private DLKerasRegularizer m_betaRegularizer = null;

    @Parameter(label = "Gamma Regularizer", required = false, choices = DLKerasRegularizerChoices.class)
    private DLKerasRegularizer m_gammaRegularizer = null;

    @Parameter(label = "Beta Constraint", required = false, choices = DLKerasConstraintChoices.class)
    private DLKerasConstraint m_betaConstraint = null;

    @Parameter(label = "Gamma Constraint", required = false, choices = DLKerasConstraintChoices.class)
    private DLKerasConstraint m_gammaConstraint = null;

    /**
     * Constructor
     */
    public DLKerasBatchNormalizationLayer() {
        super("keras.layers.BatchNormalization");
    }

    @Override
    public void validateParameters() throws InvalidSettingsException {
        if (m_epsilon <= 0) {
            throw new InvalidSettingsException("Epsilon must be larger than 0");
        }
        // TODO what are allowed values for the momentum?
    }

    @Override
    protected void validateInputSpec(final Class<?> inputElementType, final Long[] inputShape)
        throws DLInvalidTensorSpecException {
        if (m_axis > inputShape.length) {
            // Note that axis 1 corresponds to the axis with index 0 in inputShape
            throw new DLInvalidTensorSpecException(
                "Normalization axis " + m_axis + " not avaiable in input of rank " + inputShape.length);
        }
    }

    @Override
    protected Long[] inferOutputShape(final Long[] inputShape) {
        // Dosn't change the shape
        return inputShape;
    }

    @Override
    protected void populateParameters(final List<String> positionalParams, final Map<String, String> namedParams) {
        namedParams.put("axis", DLPythonUtils.toPython(m_axis));
        namedParams.put("momentum", DLPythonUtils.toPython(m_momentum));
        namedParams.put("epsilon", DLPythonUtils.toPython(m_epsilon));
        namedParams.put("center", DLPythonUtils.toPython(m_center));
        namedParams.put("scale", DLPythonUtils.toPython(m_scale));
        namedParams.put("beta_initializer", DLKerasConfigObjectUtils.toPython(m_betaInitializer));
        namedParams.put("gamma_initializer", DLKerasConfigObjectUtils.toPython(m_gammaInitializer));
        namedParams.put("moving_mean_initializer", DLKerasConfigObjectUtils.toPython(m_movingMeanInitializer));
        namedParams.put("moving_variance_initializer", DLKerasConfigObjectUtils.toPython(m_movingVarianceInitializer));
        namedParams.put("beta_regularizer", DLKerasConfigObjectUtils.toPython(m_betaRegularizer));
        namedParams.put("gamma_regularizer", DLKerasConfigObjectUtils.toPython(m_gammaRegularizer));
        namedParams.put("beta_constraint", DLKerasConfigObjectUtils.toPython(m_betaConstraint));
        namedParams.put("gamma_constraint", DLKerasConfigObjectUtils.toPython(m_gammaConstraint));
    }
}
