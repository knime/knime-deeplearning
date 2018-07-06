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
import org.knime.core.node.NodeSettingsWO;
import org.knime.dl.keras.core.struct.Member;

/**
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
class NodeSettingsPrimitiveAccessWO<T> extends AbstractNodeSettingsWriteAccess<T> {

    public NodeSettingsPrimitiveAccessWO(Member<T> member) {
        super(member);
    }

    @Override
    public void setValue(NodeSettingsWO settings, T value) throws InvalidSettingsException {
        final Class<?> rawType = ClassUtils.primitiveToWrapper(m_member.getRawType());
        if (Boolean.class.equals(rawType)) {
            settings.addBoolean(m_member.getKey(), (Boolean)value);
        } else if (Double.class.equals(rawType)) {
            settings.addDouble(m_member.getKey(), (Double)value);
        } else if (Float.class.equals(rawType)) {
            settings.addFloat(m_member.getKey(), (Float)value);
        } else if (Byte.class.equals(rawType)) {
            settings.addByte(m_member.getKey(), (Byte)value);
        } else if (Short.class.equals(rawType)) {
            settings.addShort(m_member.getKey(), (Short)value);
        } else if (Integer.class.equals(rawType)) {
            settings.addInt(m_member.getKey(), (Integer)value);
        } else if (Long.class.equals(rawType)) {
            settings.addLong(m_member.getKey(), (Long)value);
        } else {
            throw new InvalidSettingsException(
                "Unknown primitive type " + rawType + " found when trying to save settings for member " + m_member
                    + ". Most likely an implementation error!");
        }
    }
}