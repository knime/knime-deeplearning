package org.knime.dl.core.data.convert;

import static org.junit.Assert.assertEquals;
import static org.knime.dl.testing.DLTestUtil.createTensor;

import java.util.Collections;

import org.junit.Test;
import org.knime.core.data.vector.bitvector.BitVectorValue;
import org.knime.core.data.vector.bitvector.DenseBitVector;
import org.knime.core.data.vector.bitvector.DenseBitVectorCellFactory;
import org.knime.dl.core.DLTensor;
import org.knime.dl.core.data.DLReadableBitBuffer;
import org.knime.dl.core.data.DLWritableBitBuffer;

/**
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 * @author Lukas Siedentop, KNIME GmbH, Konstanz, Germany
 */

public class DLBitVectorToBitTensorConverterFactoryTest {

    @Test
    public void testConvert() {
        final DLBitVectorToBitTensorConverterFactory factory = new DLBitVectorToBitTensorConverterFactory();
        final DLDataValueToTensorConverter<BitVectorValue, DLWritableBitBuffer> converter = factory.createConverter();
        final DenseBitVectorCellFactory cellFactory =
            new DenseBitVectorCellFactory(new DenseBitVector(new long[]{0xABCDEF0123456789l}, 5));
        final BitVectorValue input = cellFactory.createDataCell();

        final DLTensor<DLWritableBitBuffer> output = (DLTensor<DLWritableBitBuffer>)createTensor(Boolean.class, 1, 5);
        converter.convert(Collections.singletonList(input), output);
        final DLReadableBitBuffer outputAsReadable = (DLReadableBitBuffer)output.getBuffer();

        assertEquals(input.length(), outputAsReadable.size());
        assertEquals(input.get(0), outputAsReadable.readNextBit());
        assertEquals(input.get(1), outputAsReadable.readNextBit());
        assertEquals(input.get(2), outputAsReadable.readNextBit());
        assertEquals(input.get(3), outputAsReadable.readNextBit());
        assertEquals(input.get(4), outputAsReadable.readNextBit());
    }

    @Test
    public void testGetName() {
        final DLBitVectorToBitTensorConverterFactory factory = new DLBitVectorToBitTensorConverterFactory();
        assertEquals("Bit vector", factory.getName());
    }

    @Test
    public void testGetSourceType() {
        final DLBitVectorToBitTensorConverterFactory factory = new DLBitVectorToBitTensorConverterFactory();
        assertEquals(BitVectorValue.class, factory.getSourceType());
    }
}
