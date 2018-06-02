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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.knime.dl.keras.core.struct.Member;
import org.knime.dl.keras.core.struct.Struct;
import org.knime.dl.keras.core.struct.access.MemberReadWriteAccess;
import org.knime.dl.keras.core.struct.access.StructAccess;
import org.knime.dl.keras.core.struct.instance.MemberReadWriteInstance;
import org.knime.dl.keras.core.struct.instance.StructInstance;
import org.knime.dl.keras.core.struct.instance.StructInstances;
import org.scijava.util.ClassUtils;

/**
 * Helper class to deal with ParameterStructs.
 * 
 * NB: Heavily inspired by the work of Curtis Rueden.
 * 
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public final class ParameterStructs {

    /**
     * Derive a {@link Struct} of the provided type. The type is expected to comprise fields annotated with @Parameter
     * describing it's input and therefore defining the {@link Struct}. If no fields are annotated the number of
     * {@link Member}s of the resulting {@link Struct} will be zero.
     * 
     * @param type to derive {@link Struct}.
     * 
     * @return {@link Struct} derived from type
     */
    public static Struct structOf(final Class<?> type) {
        final List<Member<?>> items = parse(type);
        return () -> items;
    }

    private static List<Member<?>> parse(final Class<?> type) {
        if (type == null)
            return null;

        if (type.isPrimitive() || type.isArray()) {
            return Collections.emptyList();
        }

        final ArrayList<Member<?>> items = new ArrayList<>();
        final Set<String> names = new HashSet<>();

        // Parse field level @Parameter annotations.
        final List<Field> fields = ClassUtils.getAnnotatedFields(type, Parameter.class);

        for (final Field f : fields) {
            f.setAccessible(true); // expose private fields

            final String name = f.getName();
            final boolean isFinal = Modifier.isFinal(f.getModifiers());
            if (isFinal)
                continue;

            // add item to the list
            final ParameterMember<?> item = new FieldParameterMember<>(f, type);
            names.add(name);
            items.add(item);
        }

        return items;
    }

    /**
     * @param type with @Parameter annotated fields.
     * @return a {@link StructAccess} over the provided type.
     */
    public static <S> StructAccess<MemberReadWriteAccess<?, S>> createStructAccess(Class<S> type) {
        try {
            return new ParameterStructAccess<S>(ParameterStructs.structOf(type), type);
        } catch (ValidityException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * @param obj with @Parameter annotated fields.
     * @return {@link StructInstance} wrapping obj.
     */
    public static <T> StructInstance<MemberReadWriteInstance<?>, T> createInstance(T obj) {
        @SuppressWarnings("unchecked")
        final Class<T> type = (Class<T>)obj.getClass();
        return StructInstances.createReadWriteInstance(obj, createStructAccess(type));
    }
}