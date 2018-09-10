package org.knime.dl.core.data.convert;

import static org.junit.Assert.assertEquals;
import static org.knime.dl.testing.DLTestUtil.createTensor;

import java.util.Collections;

import org.junit.Test;
import org.knime.core.data.vector.bytevector.ByteVectorValue;
import org.knime.core.data.vector.bytevector.DenseByteVector;
import org.knime.core.data.vector.bytevector.DenseByteVectorCellFactory;
import org.knime.dl.core.DLTensor;
import org.knime.dl.core.data.DLReadableByteBuffer;
import org.knime.dl.core.data.DLWritableByteBuffer;

/**
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 * @author Lukas Siedentop, KNIME GmbH, Konstanz, Germany
 */

public class DLByteVectorToByteTensorConverterFactoryTest {

    @Test
    public void testConvert() {
        final DLByteVectorToByteTensorConverterFactory factory = new DLByteVectorToByteTensorConverterFactory();
        final DLDataValueToTensorConverter<ByteVectorValue, DLWritableByteBuffer> converter = factory.createConverter();
        final DenseByteVectorCellFactory cellFactory =
            new DenseByteVectorCellFactory(new DenseByteVector(new byte[]{44, 22, 43, -17, 12}));
        final ByteVectorValue input = cellFactory.createDataCell();

        final DLTensor<DLWritableByteBuffer> output = (DLTensor<DLWritableByteBuffer>)createTensor(Byte.class, 1, 5);
        converter.convert(Collections.singletonList(input), output);
        final DLReadableByteBuffer outputAsReadable = (DLReadableByteBuffer)output.getBuffer();

        assertEquals(input.length(), outputAsReadable.size());
        assertEquals(input.get(0), outputAsReadable.readNextByte());
        assertEquals(input.get(1), outputAsReadable.readNextByte());
        assertEquals(input.get(2), outputAsReadable.readNextByte());
        assertEquals(input.get(3), outputAsReadable.readNextByte());
        assertEquals(input.get(4), outputAsReadable.readNextByte());
    }

    @Test
    public void testGetName() {
        final DLByteVectorToByteTensorConverterFactory factory = new DLByteVectorToByteTensorConverterFactory();
        assertEquals("Byte vector", factory.getName());
    }

    @Test
    public void testGetSourceType() {
        final DLByteVectorToByteTensorConverterFactory factory = new DLByteVectorToByteTensorConverterFactory();
        assertEquals(ByteVectorValue.class, factory.getSourceType());
    }
}
