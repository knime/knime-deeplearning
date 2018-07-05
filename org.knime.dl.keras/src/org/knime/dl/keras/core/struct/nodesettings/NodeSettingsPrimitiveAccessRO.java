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

/**
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
class NodeSettingsPrimitiveAccessRO<T> extends AbstractNodeSettingsReadAccess<T> {

    public NodeSettingsPrimitiveAccessRO(Member<T> member) {
        super(member);
    }

    @Override
    public T get(NodeSettingsRO storage, String key) {
        final Class<?> rawType = ClassUtils.primitiveToWrapper(member().getRawType());
        try {
            final Object value;
            if (Boolean.class.equals(rawType)) {
                value = storage.getBoolean(key);
            } else if (Double.class.equals(rawType)) {
                value = storage.getDouble(key);
            } else if (Float.class.equals(rawType)) {
                value = storage.getFloat(key);
            } else if (Byte.class.equals(rawType)) {
                value = storage.getByte(key);
            } else if (Short.class.equals(rawType)) {
                value = storage.getShort(key);
            } else if (Integer.class.equals(rawType)) {
                value = storage.getInt(key);
            } else if (Long.class.equals(rawType)) {
                value = storage.getLong(key);
            } else {
                throw new InvalidSettingsException(
                    "No primitive serializer for " + rawType + " could be found. Most likely an implementation error.");
            }
            @SuppressWarnings("unchecked")
            T casted = (T)value;
            return casted;
        } catch (InvalidSettingsException ex) {
            throw new IllegalStateException("NodeSetting for key " + key + " couldn't be found.", ex);
        }
    }
}