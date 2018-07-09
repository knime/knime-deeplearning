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
package org.knime.dl.keras.core.struct.nodesettings;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.dl.keras.core.struct.Member;
import org.knime.dl.keras.core.struct.access.MemberReadAccess;
import org.knime.dl.keras.core.struct.access.StructAccess;
import org.knime.dl.keras.core.struct.access.ValueWriteAccess;
import org.knime.dl.keras.core.struct.param.DefaultParameterMember;
import org.knime.dl.keras.core.struct.param.ParameterMember;

/**
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 * @param <T>
 */
public abstract class AbstractNodeSettingsWriteAccess<T> implements ValueWriteAccess<T, NodeSettingsWO> {

    private Member<T> m_member;

    private boolean m_isRequired;

    private boolean m_isEnabled;

    public AbstractNodeSettingsWriteAccess(Member<T> member) {
        m_member = member;
        m_isRequired = member instanceof ParameterMember && ((ParameterMember<T>)member).isRequired();
    }

    @Override
    public void set(NodeSettingsWO settings, T value) throws InvalidSettingsException {
        if (m_isRequired) {
            setInternal(settings, value);
        } else {
            NodeSettingsWO nested = null;
            if (settings instanceof NodeSettings) {
                if (((NodeSettings)settings).containsKey(m_member.getKey())) {
                    nested = ((NodeSettings)settings).getNodeSettings(m_member.getKey());
                }
            }
            if (nested == null) {
                nested = settings.addNodeSettings(m_member.getKey());
            }
            nested.addBoolean(DefaultParameterMember.SETTINGS_KEY_ENABLED, m_isEnabled);
            setInternal(nested, value);
        }

    }

    private void setInternal(NodeSettingsWO settings, T value) throws InvalidSettingsException {
        if (value != null) {
            setValue(settings, value);
        }
    }

    @Override
    public void setEnabled(boolean isEnabled) {
        m_isEnabled = isEnabled;
    }

    protected Member<T> member() {
        return m_member;
    }

    protected abstract void setValue(NodeSettingsWO settings, T value) throws InvalidSettingsException;
}
