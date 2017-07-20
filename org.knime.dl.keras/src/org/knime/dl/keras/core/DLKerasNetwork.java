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
 * History
 *   May 2, 2017 (dietzc): created
 */
package org.knime.dl.keras.core;

import java.net.URL;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.knime.core.util.FileUtil;
import org.knime.dl.core.DLAbstractNetworkSpec;
import org.knime.dl.core.DLLayerDataSpec;
import org.knime.dl.core.DLNetwork;
import org.knime.dl.keras.core.kernel.DLKerasPythonKernelConfig;
import org.knime.dl.python.node.predictor.DLPythonLoadable;
import org.knime.python2.kernel.PythonKernel;

/**
 *
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
public class DLKerasNetwork implements DLNetwork, DLPythonLoadable {

    private final URL m_source;

    private final DLKerasNetworkSpec m_spec;

    public DLKerasNetwork(final DLKerasNetworkSpec spec, final URL source) {
        m_spec = spec;
        m_source = source;
    }

    public URL getSource() {
        return m_source;
    }

    @Override
    public DLKerasNetworkSpec getSpec() {
        return m_spec;
    }

    public static class DLKerasNetworkSpec extends DLAbstractNetworkSpec {

        public DLKerasNetworkSpec(final DLLayerDataSpec[] inputSpecs, final DLLayerDataSpec[] intermediateOutputSpecs,
            final DLLayerDataSpec[] outputSpecs) {
            super(inputSpecs, intermediateOutputSpecs, outputSpecs);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void hashCodeInternal(final HashCodeBuilder b) {
            // no op - everything's handled in abstract base class
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected boolean equalsInternal(final org.knime.dl.core.DLNetworkSpec other) {
            // no op - everything's handled in abstract base class
            return true;
        }
    }

    @Deprecated
    @Override
    public String load(final PythonKernel kernel) throws Exception {
        final DLKerasPythonKernelConfig cfg = new DLKerasPythonKernelConfig();
        final String filePath = FileUtil.getFileFromURL(getSource()).getAbsolutePath();
        final String fileExtension = FilenameUtils.getExtension(filePath);
        final String snippet;
        if (fileExtension.equals("h5")) {
            snippet = cfg.getLoadFromH5Code(filePath);
        } else if (fileExtension.equals("json")) {
            snippet = cfg.getLoadSpecFromJsonCode(filePath);
        } else if (fileExtension.equals("yaml")) {
            snippet = cfg.getLoadSpecFromYamlCode(filePath);
        } else {
            throw new IllegalArgumentException("Keras network reader only supports files of type h5, json and yaml.");
        }
        kernel.execute(snippet);
        return DLKerasPythonKernelConfig.MODEL_NAME;
    }
}
