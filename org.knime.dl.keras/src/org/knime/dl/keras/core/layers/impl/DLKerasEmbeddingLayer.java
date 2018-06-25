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
package org.knime.dl.keras.core.layers.impl;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.knime.core.node.InvalidSettingsException;
import org.knime.dl.keras.core.config.DLKerasConfigObjectUtils;
import org.knime.dl.keras.core.config.constraint.DLKerasConstraint;
import org.knime.dl.keras.core.config.constraint.DLKerasConstraintChoices;
import org.knime.dl.keras.core.config.initializer.DLKerasInitializer;
import org.knime.dl.keras.core.config.initializer.DLKerasInitializerChoices;
import org.knime.dl.keras.core.config.initializer.DLKerasRandomUniformInitializer;
import org.knime.dl.keras.core.config.regularizer.DLKerasRegularizer;
import org.knime.dl.keras.core.config.regularizer.DLKerasRegularizerChoices;
import org.knime.dl.keras.core.layers.DLInvalidTensorSpecException;
import org.knime.dl.keras.core.layers.DLKerasAbstractUnaryLayer;
import org.knime.dl.keras.core.layers.dialog.tuple.DLKerasTuple;
import org.knime.dl.keras.core.layers.dialog.tuple.DLKerasTuple.Constraint;
import org.knime.dl.keras.core.struct.param.OptionalStatus;
import org.knime.dl.keras.core.struct.param.Parameter;
import org.knime.dl.python.util.DLPythonUtils;

import com.google.common.collect.ImmutableSet;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class DLKerasEmbeddingLayer extends DLKerasAbstractUnaryLayer {
    
    private static final Set<Class<?>> ALLOWED_DTYPE = ImmutableSet.of(int.class);

    @Parameter(label = "Input dimension", min = "1")
    private int m_inputDim = 1;

    @Parameter(label = "Output dimension", min = "1")
    private int m_outputDim = 1;

    @Parameter(label = "Initializer", choices = DLKerasInitializerChoices.class, tab = "Advanced")
    private DLKerasInitializer m_initializer = new DLKerasRandomUniformInitializer();

    @Parameter(label = "Embedding regularizer", optionalStatus = OptionalStatus.OptionalAndNotEnabled, choices = DLKerasRegularizerChoices.class, tab = "Advanced")
    private DLKerasRegularizer m_embeddingRegularizer = null;

    @Parameter(label = "Constraint", optionalStatus = OptionalStatus.OptionalAndNotEnabled, choices = DLKerasConstraintChoices.class, tab = "Advanced")
    private DLKerasConstraint m_constraint = null;

    @Parameter(label = "Mask zero")
    private boolean m_maskZero = false;

    @Parameter(label = "Input length", optionalStatus = OptionalStatus.OptionalAndNotEnabled)
    private DLKerasTuple m_inputLength = new DLKerasTuple("1", 1, 1000,
        EnumSet.complementOf(EnumSet.of(Constraint.PARTIAL, Constraint.EMPTY)));

    /**
     * Constructor for embedding layers.
     */
    public DLKerasEmbeddingLayer() {
        super("keras.layers.Embedding", ALLOWED_DTYPE);
    }

    @Override
    public void validateParameters() throws InvalidSettingsException {
        m_initializer.validateParameters();

        if (m_embeddingRegularizer != null) {
            m_embeddingRegularizer.validateParameters();
        }

        if (m_constraint != null) {
            m_constraint.validateParameters();
        }

        if (hasInputLength()) {
            Long[] inputLength;
            try {
                inputLength = m_inputLength.getTuple();
            } catch (NumberFormatException e) {
                throw new InvalidSettingsException("The provided input length is invalid.", e);
            }
            for (Long dim : inputLength) {
                if (dim == null) {
                    throw new InvalidSettingsException("If input length is given it must not contain null dimensions.");
                }
                if (dim < 1) {
                    throw new InvalidSettingsException("Invalid input length dimension: " + dim);
                }
            }
        }
    }

    private boolean hasInputLength() {
        return m_inputLength.getTuple() != null;
    }

    @Override
    protected void validateInputShape(final Long[] inputShape)
        throws DLInvalidTensorSpecException {
        if (hasInputLength()) {
            checkInputLength(inputShape);
        }
    }

    private void checkInputLength(Long[] inputShape) throws DLInvalidTensorSpecException {
        Long[] inputLength = m_inputLength.getTuple();
        if (inputLength.length != inputShape.length) {
            throw createInvalidInputShapeException(inputLength, inputShape);
        }
        for (int i = 0; i < inputLength.length; i++) {
            Long l = inputLength[i];
            Long incoming = inputShape[i];
            if (l != null && incoming != null && !l.equals(incoming)) {
                throw createInvalidInputShapeException(inputLength, inputShape);
            }
        }
    }

    private static DLInvalidTensorSpecException createInvalidInputShapeException(Long[] inputLength,
        Long[] inputShape) {
        return new DLInvalidTensorSpecException("'Input length' is " + Arrays.deepToString(inputLength)
            + ", but received input has shape " + Arrays.deepToString(inputShape)
            + ". If specified input length must refine input shape.");
    }

    @Override
    protected Long[] inferOutputShape(final Long[] inputShape) {
        if (!hasInputLength()) {
            return Stream.concat(Arrays.stream(inputShape), Stream.of(Long.valueOf(m_outputDim))).toArray(Long[]::new);
        }
        Long[] inLength = m_inputLength.getTuple();
        for (int i = 0; i < inLength.length; i++) {
            if (inLength[i] == 0) {
                inLength[i] = inputShape[i + 1];
            }
        }
        return Stream.concat(Arrays.stream(inLength), Stream.of(Long.valueOf(m_outputDim)))
            .toArray(Long[]::new);
    }
    
    @Override
    protected Class<?> inferOutputElementType(Class<?> inputElementType) {
        return float.class;
    }

    @Override
    protected void populateParameters(final List<String> positionalParams, final Map<String, String> namedParams) {
        positionalParams.add(DLPythonUtils.toPython(m_inputDim));
        positionalParams.add(DLPythonUtils.toPython(m_outputDim));
        namedParams.put("embeddings_initializer", DLKerasConfigObjectUtils.toPython(m_initializer));
        namedParams.put("embeddings_regularizer", DLKerasConfigObjectUtils.toPython(m_embeddingRegularizer));
        namedParams.put("embeddings_constraint", DLKerasConfigObjectUtils.toPython(m_constraint));
        namedParams.put("mask_zero", DLPythonUtils.toPython(m_maskZero));
        namedParams.put("input_length", m_inputLength.toPytonTuple());
    }
}
