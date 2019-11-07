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
 * History
 *   Sep 6, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.dl.core.data.convert;

import java.util.List;
import java.util.OptionalLong;
import java.util.Set;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.ExtensibleUtilityFactory;
import org.knime.core.data.probability.nominal.NominalDistributionValue;
import org.knime.core.data.probability.nominal.NominalDistributionValueMetaData;
import org.knime.core.node.util.CheckUtils;
import org.knime.dl.core.DLTensor;
import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.core.data.DLWritableBuffer;

/**
 * Handles conversion of {@link NominalDistributionValue NominalDistributionValues} to floating point tensors on
 * an abstract level. Implementing classes only have to handle the placement of the individual probabilities into their
 * respective buffer.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <O> The type of target {@link DLWritableBuffer buffer}
 */
abstract class DLAbstractProbabilityDistributionToTensorConverterFactory<O extends DLWritableBuffer>
    extends DLAbstractTensorDataValueToTensorConverterFactory<NominalDistributionValue, O> {

    // FIXME: This class is currently broken until AP-13009 is implemented. DO NOT REGISTER ANY SUBCLASSES AS CONVERTERS IN THE FRAMEWORK!

    /**
     * {@inheritDoc}
     */
    @Override
    public final String getName() {
        return ((ExtensibleUtilityFactory)NominalDistributionValue.UTILITY).getName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Class<NominalDistributionValue> getSourceType() {
        return NominalDistributionValue.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final OptionalLong getDestCount(final List<DataColumnSpec> spec) {
        CheckUtils.checkArgument(spec.size() == 1, "Multiple probability distributions are not supported as input.");
        final DataColumnSpec colSpec = spec.get(0);
        final NominalDistributionValueMetaData metaData =
            colSpec.getMetaDataOfType(NominalDistributionValueMetaData.class).orElseThrow(
                () -> new IllegalStateException("Nominal distributions without meta data are not supported."));
        return OptionalLong.of(metaData.size());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final DLDataValueToTensorConverter<NominalDistributionValue, O> createConverter() {
        return new DLAbstractTensorDataValueToTensorConverter<NominalDistributionValue, O>() {

            @Override
            protected void convertInternal(final NominalDistributionValue element, final DLTensor<O> output) {
                @SuppressWarnings("resource") // the buffer is managed by the framework
                final O buffer = output.getBuffer();
                // FIXME get the values from the column spec instead
                final Set<String> knownValues = element.getKnownValues();
                for (String value : knownValues) {
                    put(element.getProbability(value), buffer);
                }
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final long[] getDataShapeInternal(final NominalDistributionValue element, final DLTensorSpec tensorSpec) {
        // FIXME get the size from the column spec instead
        return new long[]{element.getKnownValues().size()};
    }

    protected abstract void put(final double probability, final O buffer);
}
