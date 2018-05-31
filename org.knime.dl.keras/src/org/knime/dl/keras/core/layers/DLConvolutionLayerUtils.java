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
package org.knime.dl.keras.core.layers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.knime.core.node.InvalidSettingsException;
import org.knime.dl.python.util.DLPythonUtils;

import com.google.common.collect.Sets;

/**
 * @author David Kolb, KNIME GmbH, Konstanz, Germany
 */
public final class DLConvolutionLayerUtils {

    /**
     * Supported data formats.
     */
    public static final Set<String> DATA_FORMATS =
        Collections.unmodifiableSet(Sets.newHashSet("channels_last", "channels_first"));

    /**
     * Supported padding modes.
     */
    public static final Set<String> PADDINGS = Collections.unmodifiableSet(Sets.newHashSet("valid", "same", "full"));

    /**
     * Default dilations for pooling layers.
     */
    public static final Long[] DEFAULT_1D_DILATION = new Long[]{1L};
    @SuppressWarnings("javadoc")
    public static final Long[] DEFAULT_2D_DILATION = new Long[]{1L, 1L};
    @SuppressWarnings("javadoc")
    public static final Long[] DEFAULT_3D_DILATION = new Long[]{1L, 1L, 1L};

    private DLConvolutionLayerUtils() {
        // static utility class
    }

    /**
     * Convenience method to validate the string representation of several tuple parameters at once.
     * 
     * @param tuples the string representation of the tuples to validate
     * @param parameterNames the parameter names used for reporting
     * @param n the expected number of elements in each tuple
     * @throws InvalidSettingsException if the String format of tuple is not supported, if the tuple cannot be parsed,
     *             if the tuple length does not match the specified length, if tuple values are negative or zero
     */
    public static void validateTupleStrings(final String[] tuples, final String[] parameterNames, final int n)
        throws InvalidSettingsException {
        List<InvalidSettingsException> problems = new ArrayList<>();

        int i = 0;
        for (String tuple : tuples) {
            try {
                DLParameterValidationUtils.checkTupleString(tuple, false);
                Long[] parsedTuple = DLPythonUtils.parseShape(tuple);
                DLParameterValidationUtils.checkTupleLength(parsedTuple, n, parameterNames[i]);
                DLParameterValidationUtils.checkTupleNotZeroNotNegative(parsedTuple, parameterNames[i]);
            } catch (InvalidSettingsException e) {
                problems.add(e);
            } finally {
                i++;
            }
        }

        if (!problems.isEmpty()) {
            throw mergeExceptions(problems);
        }
    }

    private static InvalidSettingsException mergeExceptions(List<InvalidSettingsException> exceptions) {
        String message = "";
        for (Exception e : exceptions) {
            message += e.getMessage() + "\n";
        }
        return new InvalidSettingsException(message.trim());
    }

    /**
     * Computes the output shape for convolution like layers. The dilation rate is hardcoded to 1.
     * 
     * @param inputShape the 1-D, 2-D or 3-D input shape + channel, also works for n-D
     * @param filterSize the filter in each spatial dimension
     * @param stride the stride in each spatial dimension
     * @param dialtion the dilation in each spatial dimension
     * @param padding the padding mode to use
     * @param dataFormat the used data format, i.e. "channels_first" or "channels_last"
     * @return resulting output shape after convolution operation with specified parameters
     */
    public static Long[] computeOutputShape(final Long[] inputShape, final Long[] filterSize, final Long[] stride,
        final Long[] dialtion, final String padding, final String dataFormat) {

        if ((inputShape.length - 1) != filterSize.length || filterSize.length != stride.length) {
            throw new RuntimeException("Convolutional parameters not specified for each dimension.");
        }

        int channelIndex = findChannelIndex(inputShape, dataFormat);
        Long[] newDims = IntStream.range(0, inputShape.length - 1).filter(i -> i != channelIndex)
            .mapToLong(i -> inputShape[i]).boxed().toArray(Long[]::new);

        Stream<Long> outputShape = IntStream.range(0, newDims.length - 1)
            .mapToLong(i -> computeOutputLength(newDims[i], filterSize[i], stride[i], dialtion[i], padding)).boxed();

        if (dataFormat.equals("channels_first")) {
            return Stream.concat(Stream.of(inputShape[channelIndex]), outputShape).toArray(Long[]::new);
        } else {
            return Stream.concat(outputShape, Stream.of(inputShape[channelIndex])).toArray(Long[]::new);
        }
    }

    private static int findChannelIndex(final Long[] inputShape, final String dataFormat) {
        int channelIndex;
        if (dataFormat.equals("channels_first")) {
            channelIndex = 0;
        } else if (dataFormat.equals("channels_last")) {
            channelIndex = inputShape.length - 1;
        } else {
            throw new RuntimeException("Value " + dataFormat + " for data format parameter is not supported.");
        }
        return channelIndex;
    }

    /**
     * Computes the output shape for global pooling layers. Hence, just returning the value of the channel dimension.
     * 
     * @param inputShape the 1-D, 2-D or 3-D input shape + channel, also works for n-D
     * @param dataFormat the used data format, i.e. "channels_first" or "channels_last"
     * @return resulting output shape after global pooling operation
     */
    public static Long[] computeGlobalPoolingOutputShape(final Long[] inputShape, final String dataFormat) {
        return new Long[]{inputShape[findChannelIndex(inputShape, dataFormat)]};
    }

    /**
     * Computes the convolutional output shape for a single dimension.
     * 
     * @param inputLength the 2-D or 3-D convolutional input shape, also works for n-D
     * @param filterSize the filter in each spatial dimension
     * @param stride the stride in each spatial dimension
     * @param dilation the dilation rate
     * @param padding the padding mode to use
     * @return resulting output shape after convolution operation with specified parameters
     */
    public static Long computeOutputLength(final Long inputLength, final Long filterSize, final Long stride,
        final Long dilation, final String padding) {
        Long outputLength = null;

        if (inputLength == null) {
            return null;
        }

        Long dilatedFilterSize = filterSize + (filterSize - 1) * (dilation - 1);
        switch (padding) {
            case "same":
                outputLength = inputLength;
                break;
            case "valid":
                outputLength = inputLength - dilatedFilterSize + 1;
                break;
            case "full":
                outputLength = inputLength + dilatedFilterSize - 1;
            default:
                throw new RuntimeException("Value " + padding + " for padding parameter is not supported.");
        }

        return (outputLength + stride - 1) / stride;
    }

}
