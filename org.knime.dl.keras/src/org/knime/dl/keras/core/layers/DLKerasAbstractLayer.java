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
import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.stream.Collectors;

import org.knime.dl.keras.core.struct.param.Parameter;
import org.knime.dl.python.util.DLPythonUtils;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public abstract class DLKerasAbstractLayer extends DLKerasAbstractObject implements DLKerasLayer {

    @Parameter(label = "Name prefix", required = false)
    private String m_namePrefix = null;

    private String m_runtimeId;

    protected DLKerasAbstractLayer(final String kerasIdentifier) {
        super(kerasIdentifier);
    }

    @Override
    public Optional<String> getNamePrefix() {
        return Optional.ofNullable(m_namePrefix);
    }

    @Override
    public String getBackendRepresentation(final String layerName) {
        final ArrayList<String> positionalParams = new ArrayList<>();
        final LinkedHashMap<String, String> namedParams = new LinkedHashMap<>();
        populateParameters(positionalParams, namedParams);
        if (layerName != null) {
            namedParams.put("name", DLPythonUtils.toPython(layerName));
        }
        return getKerasIdentifier() + "(" //
            + String.join(", ", positionalParams) + (positionalParams.isEmpty() ? "" : ", ")
            + namedParams.entrySet().stream().map(np -> np.getKey() + "=" + np.getValue())
                .collect(Collectors.joining(", ")) //
            + ")";
    }

    @Override
    public String toString() {
        return getBackendRepresentation(null);
    }

    /**
     * @param runtimeId the runtimeId to set
     */
    @Override
    public void setRuntimeId(String runtimeId) {
        m_runtimeId = runtimeId;
    }

    @Override
    public String getRuntimeId() {
        return m_runtimeId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((m_namePrefix == null) ? 0 : m_namePrefix.hashCode());
        result = prime * result + ((m_runtimeId == null) ? 0 : m_runtimeId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        DLKerasAbstractLayer other = (DLKerasAbstractLayer)obj;
        if (m_namePrefix == null) {
            if (other.m_namePrefix != null)
                return false;
        } else if (!m_namePrefix.equals(other.m_namePrefix))
            return false;
        if (m_runtimeId == null) {
            if (other.m_runtimeId != null)
                return false;
        } else if (!m_runtimeId.equals(other.m_runtimeId))
            return false;
        return true;
    }

}
