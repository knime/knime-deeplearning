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
package org.knime.dl.python.envconfigs;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.knime.core.util.FileUtil;
import org.knime.python2.envconfigs.CondaEnvironments;

/**
 * Gives programmatic access to the Conda environment configuration files contained in this plugin. The files can be
 * used to create Conda environments that contain all packages required by the KNIME Python integration.
 *
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public final class DLCondaEnvironments {

    private static String cpuEnvFile = null;

    private static String gpuEnvFile = null;

    private DLCondaEnvironments() {
        // Utility class.
    }

    public static String getPathToDLCondaConfigFile(final boolean gpu) {
        if (gpu) {
            return getPathToDLGPUCondaConfigFile();
        } else {
            return getPathToDLCPUCondaConfigFile();
        }
    }

    public static String getPathToDLGPUCondaConfigFile() {
        if (null == gpuEnvFile) {
            try {
                final String envConfig = appendDLPackages(getPython3CondaConfigFileContent(), true);
                gpuEnvFile = writeToTempFile(envConfig, "py3_knime_dl_gpu");
            } catch (final IOException e) {
                throw new IllegalStateException("Cannot get the deep learning envioronment configuration file.", e);
            }
        }
        return gpuEnvFile;
    }

    public static String getPathToDLCPUCondaConfigFile() {
        if (null == cpuEnvFile) {
            try {
                final String envConfig = appendDLPackages(getPython3CondaConfigFileContent(), false);
                cpuEnvFile = writeToTempFile(envConfig, "py3_knime_dl_cpu");
            } catch (final IOException e) {
                throw new IllegalStateException("Cannot get the deep learning envioronment configuration file.", e);
            }
        }
        return cpuEnvFile;
    }

    private static String getPython3CondaConfigFileContent() throws IOException {
        return FileUtils.readFileToString(new File(CondaEnvironments.getPathToPython3CondaConfigFile()),
            StandardCharsets.UTF_8);
    }

    private static String writeToTempFile(final String content, final String prefix) throws IOException {
        final File tmpFile = FileUtil.createTempFile(prefix, ".yml");
        FileUtils.write(tmpFile, content, StandardCharsets.UTF_8);
        return tmpFile.getAbsolutePath();
    }

    private static String appendDLPackages(final String envConfig, final boolean gpu) {
        // TODO get conda packages from extension points or something?
        final StringBuilder builder = new StringBuilder(envConfig);

        // TensorFlow
        if (gpu && (SystemUtils.IS_OS_WINDOWS || SystemUtils.IS_OS_LINUX)) {
            builder.append("- tensorflow-gpu=1.12.0\n");
        } else {
            builder.append("- tensorflow-mkl=1.12.0\n");
        }

        // Keras
        builder.append("- keras=2.2.4\n");
        builder.append("- h5py=2.8\n");

        // ONNX
        builder.append("- pip:\n");
        builder.append("  - onnx==1.4.1\n");
        builder.append("  - onnx-tf==1.2.1\n");

        return builder.toString();
    }
}
