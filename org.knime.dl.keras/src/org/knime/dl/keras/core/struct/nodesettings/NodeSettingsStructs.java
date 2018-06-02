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

import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.dl.keras.core.struct.Struct;
import org.knime.dl.keras.core.struct.access.MemberReadAccess;
import org.knime.dl.keras.core.struct.access.MemberWriteAccess;
import org.knime.dl.keras.core.struct.access.StructAccess;
import org.knime.dl.keras.core.struct.instance.MemberReadInstance;
import org.knime.dl.keras.core.struct.instance.MemberWriteInstance;
import org.knime.dl.keras.core.struct.instance.StructInstance;
import org.knime.dl.keras.core.struct.instance.StructInstances;

/**
 * Utility class to create {@link StructInstance}s and {@link StructAccess}es from {@link NodeSettingsRO} or
 * {@link NodeSettingsWO}.
 * 
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public final class NodeSettingsStructs {

    final static String STRUCT_TYPE_KEY = "struct-type";

    private NodeSettingsStructs() {
        // NB: Avoid object instantiation
    }

    /**
     * Creates a {@link StructInstance} with {@link MemberWriteInstance}s. Write only.
     * 
     * @param settings {@link NodeSettingsWO} to persist settings.
     * @param struct {@link Struct} defining the settings layout.
     * 
     * @return {@link StructInstance} to write to.
     * 
     */
    public static StructInstance<MemberWriteInstance<?>, NodeSettingsWO>
        createNodeSettingsInstance(NodeSettingsWO settings, Struct struct) {
        return StructInstances.createWriteInstance(settings, new NodeSettingsAccessWO(struct));
    }

    /**
     * @param settings the underlying {@link NodeSettingsRO}.
     * @param struct the {@link Struct} defining the settings layout.
     * 
     * @return {@link StructInstance} to access {@link NodeSettingsRO}
     */
    public static StructInstance<MemberReadInstance<?>, NodeSettingsRO>
        createNodeSettingsInstance(NodeSettingsRO settings, Struct struct) {
        return StructInstances.createReadInstance(settings, new NodeSettingsAccessRO(struct));
    }

    /**
     * Creates a {@link StructAccess} for {@link NodeSettingsWO}
     * 
     * @param struct the {@link Struct} defining the settings layout.
     * 
     * @return {@link StructAccess} on {@link NodeSettingsWO}.
     */
    public static StructAccess<MemberWriteAccess<?, NodeSettingsWO>> createStructWOAccess(Struct struct) {
        return new NodeSettingsAccessWO(struct);
    }

    /**
     * @param struct the {@link Struct} defining the settings layout.
     * 
     * @return {@link StructAccess} on {@link NodeSettingsRO}.
     */
    public static StructAccess<MemberReadAccess<?, NodeSettingsRO>> createStructROAccess(Struct struct) {
        return new NodeSettingsAccessRO(struct);
    }

}
