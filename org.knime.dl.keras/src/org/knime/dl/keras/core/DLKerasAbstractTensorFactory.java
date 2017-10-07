/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
package org.knime.dl.keras.core;

import static com.google.common.base.Preconditions.checkArgument;

import java.lang.reflect.Array;
import java.util.function.Supplier;

import org.knime.dl.core.DLDefaultTensor;
import org.knime.dl.core.DLTensor;
import org.knime.dl.core.DLTensorFactory;
import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.core.data.DLBuffer;
import org.knime.dl.core.data.DLReadableBuffer;
import org.knime.dl.core.data.DLReadableDoubleBuffer;
import org.knime.dl.core.data.DLReadableFloatBuffer;
import org.knime.dl.core.data.DLReadableIntBuffer;
import org.knime.dl.core.data.DLReadableLongBuffer;
import org.knime.dl.core.data.DLWritableBuffer;
import org.knime.dl.core.data.DLWritableDoubleBuffer;
import org.knime.dl.core.data.DLWritableFloatBuffer;
import org.knime.dl.core.data.DLWritableIntBuffer;
import org.knime.dl.core.data.DLWritableLongBuffer;
import org.knime.dl.core.execution.DLDefaultTensorBatch;
import org.knime.dl.core.execution.DLTensorBatch;
import org.knime.dl.python.core.data.DLPythonDoubleBuffer;
import org.knime.dl.python.core.data.DLPythonFloatBuffer;
import org.knime.dl.python.core.data.DLPythonIntBuffer;
import org.knime.dl.python.core.data.DLPythonLongBuffer;
import org.knime.dl.util.DLUtils;

/**
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
public abstract class DLKerasAbstractTensorFactory implements DLTensorFactory {

	@Override
	public Class<? extends DLWritableBuffer> getWritableBufferType(final DLTensorSpec spec) {
		final Class<?> t = spec.getElementType();
		if (t.equals(double.class)) {
			return DLWritableDoubleBuffer.class;
		} else if (t.equals(float.class)) {
			return DLWritableFloatBuffer.class;
		} else if (t.equals(int.class)) {
			return DLWritableIntBuffer.class;
		} else if (t.equals(long.class)) {
			return DLWritableLongBuffer.class;
		} else {
			throw new IllegalArgumentException("No matching buffer type.");
		}
	}

	@Override
	public DLTensor<? extends DLWritableBuffer> createWritableTensor(final DLTensorSpec spec)
			throws IllegalArgumentException {
		return createTensorInternal(spec, 1, DLWritableBuffer.class)[0];
	}

	@Override
	public DLTensorBatch<? extends DLWritableBuffer> createWritableTensorBatch(final DLTensorSpec spec,
			final long batchSize) throws IllegalArgumentException {
		final DLTensor<DLWritableBuffer>[] layerData =
				createTensorInternal(spec, batchSize, DLWritableBuffer.class);
		return new DLDefaultTensorBatch<>(layerData);
	}

	@Override
	public Class<? extends DLReadableBuffer> getReadableBufferType(final DLTensorSpec spec) {
		final Class<?> t = spec.getElementType();
		if (t.equals(double.class)) {
			return DLReadableDoubleBuffer.class;
		} else if (t.equals(float.class)) {
			return DLReadableFloatBuffer.class;
		} else if (t.equals(int.class)) {
			return DLReadableIntBuffer.class;
		} else if (t.equals(long.class)) {
			return DLReadableLongBuffer.class;
		} else {
			throw new IllegalArgumentException("No matching buffer type.");
		}
	}

	@Override
	public DLTensor<? extends DLReadableBuffer> createReadableTensor(final DLTensorSpec spec)
			throws IllegalArgumentException {
		return createTensorInternal(spec, 1, DLReadableBuffer.class)[0];
	}

	@Override
	public DLTensorBatch<? extends DLReadableBuffer> createReadableTensorBatch(final DLTensorSpec spec,
			final long batchSize) throws IllegalArgumentException {
		final DLTensor<DLReadableBuffer>[] layerData =
				createTensorInternal(spec, batchSize, DLReadableBuffer.class);
		return new DLDefaultTensorBatch<>(layerData);
	}

	private <B extends DLBuffer> DLTensor<B>[] createTensorInternal(final DLTensorSpec spec,
			final long batchSize, final Class<B> readableOrWritableBufferType) {
		final long[] shape =
				DLUtils.Shapes.getFixedShape(spec.getShape()).orElseThrow(() -> new IllegalArgumentException(
						"Tensor spec does not provide a shape. Tensor cannot be created."));
		checkArgument(batchSize <= Integer.MAX_VALUE,
				"Invalid batch size. Factory only supports capacities up to " + Integer.MAX_VALUE + ".");
		final Class<?> t = spec.getElementType();
		final long size = DLUtils.Shapes.getSize(shape);
		// TODO: handle unsafe casts
		final Supplier<B> s;
		if (t.equals(double.class)) {
			s = () -> (B) new DLPythonDoubleBuffer(size);
		} else if (t.equals(float.class)) {
			s = () -> (B) new DLPythonFloatBuffer(size);
		} else if (t.equals(int.class)) {
			s = () -> (B) new DLPythonIntBuffer(size);
		} else if (t.equals(long.class)) {
			s = () -> (B) new DLPythonLongBuffer(size);
		} else {
			throw new IllegalArgumentException("No matching tensor type.");
		}
		final DLTensor<B>[] layerData = (DLTensor<B>[]) Array.newInstance(DLTensor.class, (int) batchSize);
		for (int i = 0; i < batchSize; i++) {
			layerData[i] = new DLDefaultTensor<>(spec, s.get());
		}
		return layerData;
	}
}
