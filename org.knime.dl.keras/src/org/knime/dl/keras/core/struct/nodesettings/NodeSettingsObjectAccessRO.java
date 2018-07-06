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
import org.knime.dl.keras.core.struct.Structs;
import org.knime.dl.keras.core.struct.access.MemberReadWriteAccess;
import org.knime.dl.keras.core.struct.access.StructAccess;
import org.knime.dl.keras.core.struct.instance.StructInstances;
import org.knime.dl.keras.core.struct.param.ParameterStructs;

/**
 * @author David Kolb, KNIME GmbH, Konstanz, Germany
 * @param <T>
 */
public class NodeSettingsObjectAccessRO<T> extends AbstractNodeSettingsReadAccess<T> {

    /**
     * @param member
     */
    protected NodeSettingsObjectAccessRO(Member<T> member) {
        super(member);
    }

    @Override
    public T getValue(NodeSettingsRO settings) throws InvalidSettingsException {
        try {
            final Class<T> type = getType(settings);
            final T obj = type.newInstance();
            final StructAccess<MemberReadWriteAccess<?, T>> objAccess = ParameterStructs.createStructAccess(type);
            Structs.shallowCopyUnsafe(
                StructInstances.createReadInstance(settings,
                    NodeSettingsStructs.createStructROAccess(objAccess.struct())),
                StructInstances.createReadWriteInstance(obj, objAccess));
            return obj;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new InvalidSettingsException(
                "Can't create read access for member " + member().toString() + ". Most likely an implementation error.",
                e);
        }
    }

    @SuppressWarnings("unchecked")
    private Class<T> getType(NodeSettingsRO settings) throws InvalidSettingsException {
        try {
            return (Class<T>)Class.forName(settings.getString(NodeSettingsStructs.STRUCT_TYPE_KEY));
        } catch (ClassNotFoundException e) {
            throw new InvalidSettingsException(e);
        }
    }
}