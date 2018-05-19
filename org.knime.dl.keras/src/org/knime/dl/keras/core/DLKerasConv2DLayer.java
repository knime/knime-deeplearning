package org.knime.dl.keras.core;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.knime.core.node.InvalidSettingsException;
import org.knime.dl.keras.core.layers.DLInvalidTensorSpecException;
import org.knime.dl.keras.core.layers.DLKerasAbstractUnaryInnerLayer;
import org.knime.dl.python.util.DLPythonUtils;
import org.scijava.param2.Parameter;

public class DLKerasConv2DLayer extends DLKerasAbstractUnaryInnerLayer {
    @Parameter(label = "Filters", min = "1", max = "1000000", stepSize = "1")
    int m_filters = 1;

    @Parameter(label = "Kernel size")
    String m_kernelSize = "1, 1";

    @Parameter(label = "Strides")
    String m_strides = "1, 1";

    @Parameter(label = "Padding", choices = {"same", "valid"})
    String m_padding = "valid";

    @Parameter(label = "Activation function", choices = {"elu", "hard_sigmoid", "linear", "relu", "selu", "sigmoid",
        "softmax", "softplus", "softsign", "tanh"})
    String m_activation = "linear";

    @Parameter(label = "Use bias?")
    boolean m_useBias = true;

    public DLKerasConv2DLayer() {
        super("keras.layers.Conv2D");
    }

    @Override
    public void validateParameters() throws InvalidSettingsException {
        throw new RuntimeException("not yet implemented"); // TODO: NYI
    }

    @Override
    protected void validateInputSpec(final Class<?> inputElementType, final Long[] inputShape)
        throws DLInvalidTensorSpecException {
        throw new RuntimeException("not yet implemented"); // TODO: NYI
    }

    @Override
    protected Long[] inferOutputShape(final Long[] inputShape) {
        throw new RuntimeException("not yet implemented"); // TODO: NYI
    }

    @Override
    protected void populateParameters(final List<String> positionalParams, final Map<String, String> namedParams) {
        positionalParams.add(DLPythonUtils.toPython(m_filters));
        final int[] kernelSize = getKernelSize();
        positionalParams.add(kernelSize.length == 1 //
            ? DLPythonUtils.toPython(kernelSize[0]) : DLPythonUtils.toPython(kernelSize));
        final int[] strides = getStrides();
        namedParams.put("strides", strides.length == 1 //
            ? DLPythonUtils.toPython(strides[0]) : DLPythonUtils.toPython(strides));
        namedParams.put("padding", DLPythonUtils.toPython(m_padding));
        namedParams.put("activation", DLPythonUtils.toPython(m_activation));
        namedParams.put("use_bias", DLPythonUtils.toPython(m_useBias));
    }

    private int[] getKernelSize() {
        return Arrays.stream(m_kernelSize.split(",")).mapToInt(Integer::parseInt).toArray();
    }

    private int[] getStrides() {
        return Arrays.stream(m_strides.split(",")).mapToInt(Integer::parseInt).toArray();
    }
}