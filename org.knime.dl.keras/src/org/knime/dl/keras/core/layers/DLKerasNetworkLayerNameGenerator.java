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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.knime.core.util.Version;
import org.knime.dl.core.DLUncheckedException;
import org.knime.dl.keras.cntk.core.DLKerasCNTKNetworkSpec;
import org.knime.dl.keras.core.DLKerasNetworkSpec;
import org.knime.dl.keras.tensorflow.core.DLKerasTensorFlowNetworkSpec;
import org.knime.dl.keras.theano.core.DLKerasTheanoNetworkSpec;
import org.knime.dl.keras.util.DLKerasUtils;

import gnu.trove.TObjectIntHashMap;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public final class DLKerasNetworkLayerNameGenerator {

    private static final Pattern TO_SNAKE_CASE_INTERMEDIATE = Pattern.compile("(.)([A-Z][a-z0-9]+)");

    private static final Pattern TO_SNAKE_CASE_INSECURE = Pattern.compile("([a-z])([A-Z])");

    // "prefix_layerindex:tensorindex",
    // "prefix_layerindex/opname:tensorindex",
    // "prefix_layerindex_opname:tensorindex"
    // Group 1 is prefix, group 2 is layer index, the rest is not of interest.
    private static final Pattern TF_TENSOR_NAME = Pattern.compile("(^.*?)_(\\d+)" + // Prefix and layer index
        "((:\\d+)" + //
        "|(/.+:\\d+)" + //
        "|(_.+:\\d+))");

    public static String getLayerNamePrefix(final DLKerasLayer layer) {
        if (layer.getNamePrefix().isPresent()) {
            return layer.getNamePrefix().get();
        } else {
            final String identifier = layer.getKerasIdentifier();
            final int prefixStartIndex = identifier.lastIndexOf('.') + 1;
            return prefixStartIndex != 0 ? toSnakeCase(identifier.substring(prefixStartIndex).toLowerCase()) : "layer";
        }
    }

    public static DLKerasNetworkLayerNameGenerator
        createFromBaseNetworks(final Collection<DLKerasNetworkSpec> baseNetworkSpecs) {
        final Version newTensorIdsVersion = new Version(3, 6, 0);
        final List<String> layerNames = new ArrayList<>();
        for (final DLKerasNetworkSpec networkSpec : baseNetworkSpecs) {
            Stream<String> tensorIds = Arrays
                .asList(networkSpec.getInputSpecs(), networkSpec.getHiddenOutputSpecs(), networkSpec.getOutputSpecs())
                .stream().flatMap(Arrays::stream) //
                .map(spec -> spec.getIdentifier().getIdentifierString());
            if (networkSpec.getBundleVersion().compareTo(newTensorIdsVersion) < 0) {
                // Legacy tensor ids. Try to convert.
                tensorIds = tensorIds.map(id -> convertLegacyTensorNameToLayerName(id, networkSpec.getClass()));
            }
            tensorIds.forEach(layerNames::add);
        }
        return new DLKerasNetworkLayerNameGenerator(layerNames);
    }

    /**
     * Consider private. Package-private to enable testing.
     */
    /* private */ static final String convertLegacyTensorNameToLayerName(final String tensorName,
        final Class<? extends DLKerasNetworkSpec> networkSpecClass) {
        final String trimmed = tensorName.trim();
        if (networkSpecClass == DLKerasCNTKNetworkSpec.class) {
            throw new DLUncheckedException("You tried to append a Keras layer node to a Keras (CNTK) "
                + "network specification that was created using a previous version of KNIME.\n"
                + "Backward compatibility cannot be ensured for CNTK.\n"
                + "Please reconfigure/execute the respective preceding nodes that created a CNTK network "
                + "specification.");
        } else if (networkSpecClass == DLKerasTensorFlowNetworkSpec.class) {
            final Matcher matcher = TF_TENSOR_NAME.matcher(trimmed);
            try {
                if (matcher.matches()) {
                    final String layerPrefix = matcher.group(1);
                    final int layerIndex = Integer.parseInt(matcher.group(2));
                    return createLayerName(layerPrefix, layerIndex);
                }
            } catch (final NumberFormatException ex) {
                // ignore, we'll throw an exception anyway
            }
            throw new DLUncheckedException("You tried to append a Keras layer node to a Keras (TensorFlow) "
                + "network specification that was created using a previous version of KNIME.\n"
                + "Backward compatibility could not be ensured for tensor name '" + trimmed + "'.\n"
                + "Please reconfigure/re-execute the respective preceding nodes that created a TensorFlow network "
                + "specification.");
        } else if (networkSpecClass == DLKerasTheanoNetworkSpec.class) {
            throw new DLUncheckedException("You tried to append a Keras layer node to a Keras (Theano) "
                + "network specification that was created using a previous version of KNIME.\n"
                + "Backward compatibility cannot be ensured for Theano.\n"
                + "Please reconfigure/execute the respective preceding nodes that created a Theano network "
                + "specification and consider switching to a Keras back end that is under active development "
                + "(TensorFlow, CNTK).");
        } else {
            throw new DLUncheckedException("You tried to append a Keras layer node to a Keras network "
                + "specification that features an unknown back end (" + networkSpecClass.getCanonicalName()
                + "). Backward compatibility cannot be ensured for third party back ends.");
        }
    }

    private static String createLayerName(final String layerPrefix, final int layerIndex) {
        return layerPrefix + "_" + layerIndex;
    }

    // Mimics
    // https://github.com/keras-team/keras/blob/d673afd5979a4e541266763f65bbd65fabf20b0b/keras/engine/topology.py#L2854
    private static final String toSnakeCase(final String name) {
        final String intermediate = TO_SNAKE_CASE_INTERMEDIATE.matcher(name).replaceAll("$1_$2");
        final String insecure = TO_SNAKE_CASE_INSECURE.matcher(intermediate).replaceAll("$1_$2").toLowerCase();
        if (insecure.charAt(0) != '_') {
            return insecure;
        }
        return "private" + insecure;
    }

    private final TObjectIntHashMap<String> m_prefixCounts;

    /**
     * @param reservedNames list of reserved names that each must be either of the form <tt>prefix_index</tt> (layer
     *            name) or <tt>prefix_index_nodeindex:tensorindex</tt> (tensor name). The (corresponding) layer names
     *            will not be generated by this instance. May be <code>null</code> in which case no name is considered
     *            reserved.
     */
    public DLKerasNetworkLayerNameGenerator(final Collection<String> reservedNames) {
        m_prefixCounts = new TObjectIntHashMap<>(reservedNames.size() > 10 ? reservedNames.size() : 10);
        // cf. UniqueNameGenerator
        for (final String reservedName : reservedNames) {
            final String layerName;
            if (reservedName.indexOf(':') > -1) {
                // tensor name - cut off node index and tensor index
                final int nodeIndexDelimiterIndex = reservedName.lastIndexOf('_');
                layerName = reservedName.substring(0, nodeIndexDelimiterIndex);
            } else {
                layerName = reservedName;
            }
            final int prefixDelimiterIndex = layerName.lastIndexOf('_');
            final String layerPrefix = layerName.substring(0, prefixDelimiterIndex);
            int layerIndex = 0;
            try {
                final String layerIndexString = layerName.substring(prefixDelimiterIndex + 1);
                layerIndex = Integer.parseInt(layerIndexString);
            } catch (final NumberFormatException nfe) {
                // ignore, must be out of range
            }
            final int prefixCount = m_prefixCounts.get(layerPrefix);
            if (layerIndex > prefixCount) {
                m_prefixCounts.put(layerPrefix, layerIndex);
            }
        }
    }

    public String getNextLayerName(final DLKerasLayer layer) {
        final String layerNamePrefix = getLayerNamePrefix(layer);
        return getNextLayerName(layerNamePrefix);
    }

    public String getNextLayerName(final String layerPrefix) {
        final int layerIndex = m_prefixCounts.adjustOrPutValue(layerPrefix, 1, 1);
        return createLayerName(layerPrefix, layerIndex);
    }

    public String getOutputTensorName(final String layerName, final int nodeIndex, final int tensorIndex) {
        return DLKerasUtils.Tensors.createTensorName(layerName, nodeIndex, tensorIndex);
    }
}
