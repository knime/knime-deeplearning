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

import java.util.Iterator;

import org.knime.dl.keras.core.struct.Struct;
import org.knime.dl.keras.core.struct.access.MemberWriteAccess;
import org.knime.dl.keras.core.struct.access.StructAccess;

/**
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
class StructWriteInstance<S> implements StructInstance<MemberWriteInstance<?>, S> {

    private StructAccess<? extends MemberWriteAccess<?, S>> m_access;

    private S m_storage;

    public StructWriteInstance(S storage, StructAccess<? extends MemberWriteAccess<?, S>> access) {
        m_storage = storage;
        m_access = access;
    }

    @Override
    public MemberWriteInstance<?> member(String key) {
        return create(m_access.member(key), m_storage);
    }

    @Override
    public Iterator<MemberWriteInstance<?>> iterator() {
        final Iterator<? extends MemberWriteAccess<?, S>> del = m_access.members().iterator();

        return new Iterator<MemberWriteInstance<?>>() {

            @Override
            public boolean hasNext() {
                return del.hasNext();
            }

            @Override
            public MemberWriteInstance<?> next() {
                return create(del.next(), m_storage);
            }
        };
    }

    @Override
    public S storage() {
        return m_storage;
    }

    @Override
    public Struct struct() {
        return m_access.struct();
    }

    private <T> MemberWriteInstance<T> create(MemberWriteAccess<T, S> access, S storage) {
        if (access.member().getRawType().isInterface()) {
            return new DefaultNestedMemberWriteInstance<T, S>(access, storage);
        } else {
            return new DefaultMemberWriteInstance<T, S>(access, storage);
        }
    }
}
