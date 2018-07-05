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
package org.knime.dl.keras.core.struct.param;

import org.knime.core.node.InvalidSettingsException;
import org.knime.dl.keras.core.struct.access.NestedValueReadWriteAccess;
import org.knime.dl.keras.core.struct.instance.MemberReadInstance;
import org.knime.dl.keras.core.struct.instance.MemberReadWriteInstance;
import org.knime.dl.keras.core.struct.instance.MemberWriteInstance;
import org.knime.dl.keras.core.struct.instance.StructInstance;
import org.knime.dl.keras.core.struct.instance.StructInstances;

/**
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 * @param <T> type of object
 * @param <S> type of storage
 */
public class NestedFieldValueAccess<T, S> extends FieldValueAccess<S, T> implements NestedValueReadWriteAccess<T, S> {

    private FieldValueAccess<S, T> m_access;

    private StructInstance<MemberReadWriteInstance<?>, T> m_proxyInstance;

    private Class<? extends Object> m_prevType;

    public NestedFieldValueAccess(FieldValueAccess<S, T> access) {
        super(access.field());
        m_access = access;
    }

    @Override
    public StructInstance<? extends MemberReadInstance<?>, ?> getStructInstance(S storage)
        throws InvalidSettingsException {
        try {
            initProxyInstance((T)m_access.field().get(storage));
            return m_proxyInstance;

        } catch (IllegalArgumentException | IllegalAccessException e) {
            throw new InvalidSettingsException(e);
        }
    }

    @Override
    public Class<T> getType(S storage) throws InvalidSettingsException {
        return (Class<T>)field().getType();
    }

    @Override
    public StructInstance<? extends MemberWriteInstance<?>, ?> getWritableStructInstance(S storage, Class<T> type)
        throws InvalidSettingsException {
        try {
            T newInstance = type.newInstance();
            m_access.set(storage, newInstance);
            initProxyInstance(newInstance);
            return m_proxyInstance;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private void initProxyInstance(T obj) {
        if (m_proxyInstance == null || m_prevType != obj.getClass()) {
            m_prevType = obj.getClass();
            m_proxyInstance =
                StructInstances.createReadWriteInstance(obj, ParameterStructs.createStructAccess(obj.getClass()));
        }
    }

}
