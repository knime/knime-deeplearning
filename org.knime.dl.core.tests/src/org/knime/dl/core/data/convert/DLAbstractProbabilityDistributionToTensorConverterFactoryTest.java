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
 *   Sep 12, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.dl.core.data.convert;

import static org.junit.Assert.assertEquals;
import static org.knime.dl.testing.DLTestUtil.createTensor;

import java.util.Collections;

import org.knime.core.data.probability.nominal.NominalDistributionCellFactory;
import org.knime.core.data.probability.nominal.NominalDistributionValue;
import org.knime.dl.core.DLTensor;
import org.knime.dl.core.data.DLReadableDoubleBuffer;
import org.knime.dl.core.data.DLWritableBuffer;
import org.knime.dl.core.data.convert.DLAbstractProbabilityDistributionToTensorConverterFactory;
import org.knime.dl.core.data.convert.DLDataValueToTensorConverter;

/**
 * FIXME: The tested class is broken until AP-13009 is implemented.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public abstract class DLAbstractProbabilityDistributionToTensorConverterFactoryTest<T extends DLWritableBuffer> {

    protected final DLAbstractProbabilityDistributionToTensorConverterFactory<T> m_factory = createFactory();

    protected abstract DLAbstractProbabilityDistributionToTensorConverterFactory<T> createFactory();

    protected abstract Class<?> getElementType();

    public void testConvert() throws Exception {
        final DLDataValueToTensorConverter<NominalDistributionValue, T> converter = m_factory.createConverter();
        final String[] values = new String[] {"A", "B", "C"};
        // FIXME
        final NominalDistributionCellFactory factory = null;
        final NominalDistributionValue input =
                factory.createCell(new double[]{0.3, 0.4, 0.3}, 0);
        final DLTensor<T> output = (DLTensor<T>)createTensor(getElementType(), 1, 3);
        converter.convert(Collections.singletonList(input), output);
        final DLReadableDoubleBuffer outputAsReadable = (DLReadableDoubleBuffer)output.getBuffer();
        assertEquals(values.length, outputAsReadable.size());
        for (String value : values) {
            assertEquals(input.getProbability(value), outputAsReadable.readNextDouble(), 1e-6);
        }
    }

    public void testGetName() throws Exception {
        assertEquals("Probability Distribution", m_factory.getName());
    }

    public void testGetSourceType() throws Exception {
        assertEquals(NominalDistributionValue.class, m_factory.getSourceType());
    }
}
