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
package org.knime.dl.testing.backend;

import java.util.List;
import java.util.OptionalLong;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.dl.core.DLTensor;
import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.core.data.DLReadableDoubleBuffer;
import org.knime.dl.core.data.DLWritableFloatBuffer;
import org.knime.dl.core.data.convert.DLAbstractTensorDataValueToTensorConverter;
import org.knime.dl.core.data.convert.DLAbstractTensorDataValueToTensorConverterFactory;
import org.knime.dl.core.data.convert.DLDataValueToTensorConverter;
import org.knime.dl.core.data.convert.DLTensorToDataCellConverter;
import org.knime.dl.core.data.convert.DLTensorToDataCellConverterFactory;
import org.knime.dl.testing.DLTestingTensorFactory;
import org.knime.dl.testing.backend.DLTestingDataCells.DoubleDataCell;
import org.knime.dl.testing.backend.DLTestingDataCells.TestingDataValue;

/**
 * @author Lukas Siedentop, KNIME GmbH, Konstanz, Germany
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 */
public class DLTestingBackendFactories {

    public static class DLTestingBackendTensorFactory extends DLTestingTensorFactory {
        @Override
        public Class<?> getNetworkType() {
            return DLTestingBackendNetwork.class;
        }
    }

    public static class DLTestingDataValueToFloatTensorConverterFactory
    extends
    DLAbstractTensorDataValueToTensorConverterFactory<DLTestingDataCells.TestingDataValue, DLWritableFloatBuffer> {

        @Override
        public String getName() {
            return "From FooDataValue";
        }

        @Override
        public Class<DLTestingDataCells.TestingDataValue> getSourceType() {
            return DLTestingDataCells.TestingDataValue.class;
        }

        @Override
        public Class<DLWritableFloatBuffer> getBufferType() {
            return DLWritableFloatBuffer.class;
        }

        @Override
        public OptionalLong getDestCount(final List<DataColumnSpec> spec) {
            return OptionalLong.empty();
        }

        @Override
        public DLDataValueToTensorConverter<TestingDataValue, DLWritableFloatBuffer> createConverter() {

            return new DLAbstractTensorDataValueToTensorConverter<DLTestingDataCells.TestingDataValue, DLWritableFloatBuffer>() {

                @Override
                protected void convertInternal(final DLTestingDataCells.TestingDataValue element,
                    final DLTensor<DLWritableFloatBuffer> output) {
                    final DLWritableFloatBuffer buf = output.getBuffer();
                    buf.putAll(element.getFloatArray());
                }
            };
        }

        @Override
        protected long[] getDataShapeInternal(final DLTestingDataCells.TestingDataValue input,
            final DLTensorSpec tensorSpec) {
            return new long[]{input.getFloatArray().length};
        }

    }

    public static class DLDoubleBufferToDoubleDataCellDataCellConverterFactory
    implements
    DLTensorToDataCellConverterFactory<DLReadableDoubleBuffer, DLTestingDataCells.DoubleDataCell> {

        private static final OptionalLong DEST_COUNT = OptionalLong.of(1);

        @Override
        public String getName() {
            return "To BarDataCell";
        }

        @Override
        public Class<DLReadableDoubleBuffer> getBufferType() {
            return DLReadableDoubleBuffer.class;
        }

        @Override
        public DataType getDestType() {
            return DataType.getType(DLTestingDataCells.DoubleDataCell.class);
        }

        @Override
        public DLTensorToDataCellConverter<DLReadableDoubleBuffer, DLTestingDataCells.DoubleDataCell>
        createConverter() {
            return (input, out, exec) -> {
                final DLReadableDoubleBuffer buf = input.getBuffer();
                out[0] = new DoubleDataCell(buf.toDoubleArray());
            };
        }

        @Override
        public OptionalLong getDestCount(final DLTensorSpec spec) {
            return DEST_COUNT;
        }
    }

}
