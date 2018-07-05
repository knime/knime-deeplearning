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
package org.knime.dl.keras.core.struct.instance;

import org.knime.core.node.InvalidSettingsException;
import org.knime.dl.keras.core.struct.Member;
import org.knime.dl.keras.core.struct.access.MemberReadWriteAccess;

/**
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
class DefaultMemberReadWriteInstance<T, S> implements MemberReadWriteInstance<T> {

    private final MemberReadWriteAccess<T, S> m_access;

    private S m_storage;

    public DefaultMemberReadWriteInstance(MemberReadWriteAccess<T, S> access, S storage) {
        m_access = access;
        m_storage = storage;
    }

    @Override
    public Member<T> member() {
        return m_access.member();
    }

    @Override
    public T get() throws InvalidSettingsException {
        return m_access.get(m_storage);
    }

    @Override
    public void set(Object obj) throws InvalidSettingsException {
        if (get() == null || !get().equals(obj)) {
            m_access.set(m_storage, obj);
        }
    }

    @Override
    public boolean isEnabled() {
        return m_access.isEnabled();
    }

    @Override
    public void setEnabled(boolean isEnabled) {
        m_access.setEnabled(isEnabled);
    }

    protected S storage() {
        return m_storage;
    }
}
