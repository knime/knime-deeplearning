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
package org.knime.dl.keras.util;

import org.knime.core.util.Version;
import org.knime.dl.core.DLTensorId;
import org.knime.dl.keras.core.config.DLKerasConfigObject;
import org.knime.dl.python.util.DLPythonUtils;

/**
 * Various Keras specific utility methods and classes.
 *
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public final class DLKerasUtils {

    /** The Keras version that is currently preferred by the integration. */
    public static final Version PREFERRED_KERAS_VERSION = new Version(2, 1, 6);

    /** The TensorFlow version that is currently preferred by the integration. */
    public static final Version PREFERRED_TF_VERSION = new Version(1, 8, 0);

    private DLKerasUtils() {
    }

    /** Utility functions related to layers */
    public static final class Layers {

        private Layers() {
        }

        /**
         * Helper function that retrieves the backend representation of a {@link DLKerasConfigObject}
         * or returns the None representation if <b>obj</b> is null.
         *
         * @param obj a {@link DLKerasConfigObject}, may be null
         * @return the backend representation of <b>obj</b> or None if <b>obj</b> is null
         */
        public static String toPython(DLKerasConfigObject obj) {
            return obj == null ? DLPythonUtils.NONE : obj.getBackendRepresentation();
        }

        /**
         * Extracts the layer name of the given tensor id.
         *
         * @param id the identifier of the tensor
         * @return the name of the layer
         */
        public static String getLayerName(final DLTensorId id) {
            return getLayerName(id.getIdentifierString());
        }

        /**
         * Extracts the layer name of the given tensor id.
         *
         * @param id the identifier of the tensor
         * @return the name of the layer
         */
        public static String getLayerName(final String id) {
            return id.substring(0, id.lastIndexOf('_'));
        }
    }

    /** Utility functions related to tensors */
    public static final class Tensors {

        private Tensors() {
        }

        /**
         * @param layerName the full layer name of the form <tt>prefix_index</tt>
         * @param nodeIndex the node index
         * @param layerIndex the layerIndex
         * @return the created tensor name
         */
        public static String createTensorName(final String layerName, final int nodeIndex, final int layerIndex) {
            // Equals the naming scheme in DLKerasNetworkSpecExtractor on Python side.
            return layerName + "_" + nodeIndex + ":" + layerIndex;
        }

        /**
         * Extracts the node index of the given tensor id.
         *
         * @param id the identifier of the tensor
         * @return the node index
         */
        public static int getNodeIndex(final DLTensorId id) {
            return getNodeIndex(id.getIdentifierString());
        }

        /**
         * Extracts the node index of the given tensor id.
         *
         * @param id the identifier of the tensor
         * @return the node index
         */
        public static int getNodeIndex(final String id) {
            return Integer.parseInt(id.substring(id.lastIndexOf('_') + 1, id.lastIndexOf(':')));
        }

        /**
         * Extracts the tensor index of the given tensor id.
         *
         * @param id the identifier of the tensor
         * @return the tensor index
         */
        public static int getTensorIndex(final DLTensorId id) {
            return getTensorIndex(id.getIdentifierString());
        }

        /**
         * Extracts the tensor index of the given tensor id.
         *
         * @param id the identifier of the tensor
         * @return the tensor index
         */
        public static int getTensorIndex(final String id) {
            return Integer.parseInt(id.substring(id.lastIndexOf(':') + 1));
        }
    }
}
