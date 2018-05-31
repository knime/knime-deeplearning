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

import org.apache.commons.lang3.ClassUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.dl.keras.core.struct.Member;
import org.knime.dl.keras.core.struct.Struct;
import org.knime.dl.keras.core.struct.Structs;
import org.knime.dl.keras.core.struct.access.AbstractStructAccess;
import org.knime.dl.keras.core.struct.access.DefaultMemberReadAccess;
import org.knime.dl.keras.core.struct.access.MemberReadAccess;
import org.knime.dl.keras.core.struct.access.MemberReadWriteAccess;
import org.knime.dl.keras.core.struct.access.StructAccess;
import org.knime.dl.keras.core.struct.access.StructReadAccess;
import org.knime.dl.keras.core.struct.access.ValueReadAccess;
import org.knime.dl.keras.core.struct.instance.StructInstances;
import org.knime.dl.keras.core.struct.param.ParameterStructs;

/**
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
class NodeSettingsReadAccess extends AbstractStructAccess<MemberReadAccess<?, NodeSettingsRO>>
    implements StructReadAccess<NodeSettingsRO, MemberReadAccess<?, NodeSettingsRO>> {
    public NodeSettingsReadAccess(Struct struct) {
        super(struct);
        for (final Member<?> member : struct) {
            addMemberInstance(createMemberInstancesRO(member));
        }
    }

    private static <T> MemberReadAccess<T, NodeSettingsRO> createMemberInstancesRO(Member<T> member) {
        final Class<T> rawType = member.getRawType();
        final ValueReadAccess<T, NodeSettingsRO> readAccess;
        if (ClassUtils.isPrimitiveOrWrapper(rawType)) {
            readAccess = createPrimitiveAccessRO(member);
        } else if (rawType.isArray() && rawType.getComponentType().isPrimitive()) {
            readAccess = createPrimitiveArrayAccessRO(member);
        } else if (rawType.equals(String.class)) {
            @SuppressWarnings("unchecked")
            final ValueReadAccess<T, NodeSettingsRO> casted =
                (ValueReadAccess<T, NodeSettingsRO>)new NodeSettingsStringAccessRO((Member<String>)member);
            readAccess = casted;
        } else if (rawType.equals(String[].class)) {
            readAccess = createPrimitiveArrayAccessRO(member);
        } else if (rawType.isEnum()) {
            readAccess = createEnumAccessRO(member);
        } else {
            readAccess = createObjectAccessRO(member);
        }
        return new DefaultMemberReadAccess<>(member, readAccess);

    }

    private static <T> ValueReadAccess<T, NodeSettingsRO> createEnumAccessRO(Member<T> member) {
        return new ValueReadAccess<T, NodeSettingsRO>() {

            @Override
            public T get(NodeSettingsRO storage) throws InvalidSettingsException {
                final String key = member.getKey();
                if (storage.containsKey(key)) {
                    return enumValue(storage.getString(key), member.getRawType());
                }
                return null;
            }

            public <V extends Enum<V>> V enumOf(final Class<V> type, final String value) {
                return Enum.valueOf(type, value);
            }
        };
    }

    private static <T> ValueReadAccess<T, NodeSettingsRO> createObjectAccessRO(Member<T> member) {
        return new ValueReadAccess<T, NodeSettingsRO>() {
            @Override
            public T get(NodeSettingsRO settings) throws InvalidSettingsException {
                // String ODO Caching of accesses
                try {
                    if (settings.containsKey(member.getKey())) {
                        NodeSettingsRO nestedSettings = settings.getNodeSettings(member.getKey());
                        @SuppressWarnings("unchecked")
                        final Class<T> type =
                            (Class<T>)Class.forName(nestedSettings.getString(NodeSettingsStructs.STRUCT_TYPE_KEY));
                        final T obj = type.newInstance();

                        final StructAccess<MemberReadWriteAccess<?, T>> objAccess =
                            ParameterStructs.createStructAccess(type);
                        Structs.shallowCopyUnsafe(
                            StructInstances.createReadInstance(nestedSettings,
                                NodeSettingsStructs.createStructROAccess(objAccess.struct())),
                            StructInstances.createReadWriteInstance(obj, objAccess));
                        return obj;
                    } else {
                        return null;
                    }
                } catch (InstantiationException | ClassNotFoundException | IllegalAccessException e) {
                    throw new InvalidSettingsException("Can't create read access for member " + member.toString()
                        + ". Most likely an implementation error.", e);
                }
            }
        };
    }

    private static <T> ValueReadAccess<T, NodeSettingsRO> createPrimitiveAccessRO(Member<T> member) {
        return new NodeSettingsPrimitiveAccessRO<>(member);
    }

    private static <T> ValueReadAccess<T, NodeSettingsRO> createPrimitiveArrayAccessRO(Member<T> member) {
        return new NodeSettingsPrimitiveArrayAccessRO<>(member);
    }

    // TODO use types version
    private static <T> T enumValue(final String name, final Class<T> dest) {
        @SuppressWarnings({"rawtypes", "unchecked"})
        final Enum result = Enum.valueOf((Class)dest, name);
        @SuppressWarnings("unchecked")
        final T typedResult = (T)result;
        return typedResult;
    }

}