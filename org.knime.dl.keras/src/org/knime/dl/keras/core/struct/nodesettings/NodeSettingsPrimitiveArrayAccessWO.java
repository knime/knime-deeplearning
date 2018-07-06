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
class NodeSettingsPrimitiveArrayAccessWO<T> extends AbstractNodeSettingsWriteAccess<T> {

    public NodeSettingsPrimitiveArrayAccessWO(Member<T> member) {
        super(member);
    }

    @Override
    public void setValue(NodeSettingsWO settings, T value) throws InvalidSettingsException {
        final Class<?> componentType = ClassUtils.primitiveToWrapper(member().getRawType().getComponentType());
        if (Boolean.class.equals(componentType)) {
            settings.addBooleanArray(member().getKey(), (boolean[])value);
        } else if (Double.class.equals(componentType)) {
            settings.addDoubleArray(member().getKey(), (double[])value);
        } else if (Float.class.equals(componentType)) {
            settings.addFloatArray(member().getKey(), (float[])value);
        } else if (Byte.class.equals(componentType)) {
            settings.addByteArray(member().getKey(), (byte[])value);
        } else if (Short.class.equals(componentType)) {
            settings.addShortArray(member().getKey(), (short[])value);
        } else if (Integer.class.equals(componentType)) {
            settings.addIntArray(member().getKey(), (int[])value);
        } else if (Long.class.equals(componentType)) {
            settings.addLongArray(member().getKey(), (long[])value);
        } else {
            throw new InvalidSettingsException(
                "Unknown primitive type " + componentType + " for array found when trying to save settings for member "
                    + member() + ". Most likely an implementation error!");
        }
    }
}