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
package org.knime.dl.keras.core.struct;

import org.knime.core.node.InvalidSettingsException;
import org.knime.dl.keras.core.struct.instance.MemberInstance;
import org.knime.dl.keras.core.struct.instance.MemberReadInstance;
import org.knime.dl.keras.core.struct.instance.MemberWriteInstance;
import org.knime.dl.keras.core.struct.instance.NestedMemberReadInstance;
import org.knime.dl.keras.core.struct.instance.NestedMemberWriteInstance;
import org.knime.dl.keras.core.struct.instance.StructInstance;

/**
 * Utility class to work with {@link Struct}s.
 * 
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public class Structs {
    private Structs() {
        // NB: avoid instantiation
    }

    /**
     * Creates a shallow copy of from and stores it into to.
     * 
     * @param from struct instance to copy from
     * @param to struct instance to copy to
     * 
     * @throws InvalidSettingsException
     */
    public static void shallowCopyUnsafe(StructInstance<? extends MemberReadInstance<?>, ?> from,
        StructInstance<? extends MemberWriteInstance<?>, ?> to) throws InvalidSettingsException {
        // TODO we could do tons of sanity checking here...
        for (final MemberReadInstance<?> fromMember : from) {
            final MemberWriteInstance<?> toMember = to.member(fromMember.member().getKey());
            if (toMember == null) {
                throw new InvalidSettingsException("Incompatible StructInstances in Structs.shallowCopy(...)!");
            }
            // we can test a lot ...
            copy(fromMember, toMember);
        }
    }

    private static <T> void copy(MemberReadInstance<?> fromMember, MemberWriteInstance<T> toMember)
        throws InvalidSettingsException {
        // TODO Avoid redundant loading
        Object object = fromMember.get();
        toMember.setEnabled(fromMember.isEnabled());
        if (object != null && fromMember instanceof NestedMemberReadInstance
            && toMember instanceof NestedMemberWriteInstance) {
            // TODO Unsafe. need to check
            // TODO efficiency
            StructInstance<MemberReadInstance<?>, ?> nested = ((NestedMemberReadInstance)fromMember).getStructInstance();
            shallowCopyUnsafe(nested,
                ((NestedMemberWriteInstance)toMember).getWritableStructInstance(object.getClass()));
        } else {
            toMember.set(object);
        }
    }
}
