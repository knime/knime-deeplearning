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
import org.knime.core.node.NodeSettingsRO;
import org.knime.dl.keras.core.struct.Member;
import org.knime.dl.keras.core.struct.access.ValueReadAccess;
import org.knime.dl.keras.core.struct.param.DefaultParameterMember;
import org.knime.dl.keras.core.struct.param.FieldParameterMember;
import org.knime.dl.keras.core.struct.param.ParameterMember;

/**
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 * @param <T>
 */
public abstract class AbstractNodeSettingsReadAccess<T> implements ValueReadAccess<T, NodeSettingsRO> {

    private Member<T> m_member;

    private boolean m_isRequired;

    private boolean m_isEnabled;

    protected AbstractNodeSettingsReadAccess(Member<T> member) {
        m_member = member;
        m_isRequired = member instanceof ParameterMember && ((ParameterMember<T>)member).isRequired();
    }

    @Override
    public T get(NodeSettingsRO settings) throws InvalidSettingsException {
        if (m_isRequired) {
            return getInternal(settings);
        } else {
            NodeSettingsRO nested = settings.getNodeSettings(m_member.getKey());
            m_isEnabled = nested.getBoolean(DefaultParameterMember.SETTINGS_KEY_ENABLED);
            return getInternal(nested);
        }
    }

    private T getInternal(NodeSettingsRO settings) throws InvalidSettingsException {
        final String key = m_member.getKey();

        // there are settings available
        if (settings.containsKey(key)) {
            return get(settings, m_member.getKey());
            // no settings available
        } else if (m_member instanceof FieldParameterMember) {
            // and no enabled status yet set, so the dialog is opened the first time
            // TODO check if parameter could also have a default...
            return ((FieldParameterMember<T>)m_member).getDefault();
        }
        return null;
    }

    @Override
    public boolean isEnabled() {
        return m_isEnabled || m_isRequired;
    }

    protected Member<T> member() {
        return m_member;
    }

    protected abstract T get(NodeSettingsRO settings, String key) throws InvalidSettingsException;
}
