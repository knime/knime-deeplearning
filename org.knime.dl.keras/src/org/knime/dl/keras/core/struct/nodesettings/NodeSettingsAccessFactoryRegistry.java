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

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.knime.dl.keras.core.layers.dialog.seed.DLKerasSeedNodeSettingsAccessROFactory;
import org.knime.dl.keras.core.layers.dialog.seed.DLKerasSeedNodeSettingsAccessWOFactory;
import org.knime.dl.keras.core.layers.dialog.tuple.DLKerasTupleNodeSettingsAccessROFactory;
import org.knime.dl.keras.core.layers.dialog.tuple.DLKerasTupleNodeSettingsAccessWOFactory;

/**
 * Registry for {@link NodeSettingsReadAccessFactory}s.
 * 
 * @author David Kolb, KNIME GmbH, Konstanz, Germany
 */
class NodeSettingsAccessFactoryRegistry {

    private static NodeSettingsAccessFactoryRegistry instance;

    private List<NodeSettingsReadAccessFactory<?, ?>> m_readAccesses = new ArrayList<>();

    private List<NodeSettingsWriteAccessFactory<?, ?>> m_writeAccesses = new ArrayList<>();

    private NodeSettingsAccessFactoryRegistry() {
        // read accesses
        m_readAccesses.add(new DLKerasSeedNodeSettingsAccessROFactory());
        m_readAccesses.add(new DLKerasTupleNodeSettingsAccessROFactory());
        // write accesses
        m_writeAccesses.add(new DLKerasSeedNodeSettingsAccessWOFactory());
        m_writeAccesses.add(new DLKerasTupleNodeSettingsAccessWOFactory());
    }

    /**
     * @return singleton instance.
     */
    public static synchronized NodeSettingsAccessFactoryRegistry getInstance() {
        if (instance == null)
            instance = new NodeSettingsAccessFactoryRegistry();
        return instance;
    }

    /**
     * Get the first read access associated with the specified type from the registry.
     * 
     * @param type the type to get the access for
     * 
     * @return the read access for the specified type
     * 
     * @throws NoSuchElementException if no factory with the specified type is present in the registry
     */
    @SuppressWarnings("unchecked")
    public <T> NodeSettingsReadAccessFactory<?, T> getReadAccessFactoryFor(final Class<T> type) {
        return (NodeSettingsReadAccessFactory<?, T>)m_readAccesses.stream().filter(f -> f.getType().equals(type))
            .findFirst().get();
    }

    /**
     * Checks if the registry contains a factory with the specified type.
     * 
     * @param type the type to search in the registry
     * 
     * @return true if the registry contains a factory with the specified type, false otherwise
     */
    public boolean hasReadAccessFactoryFor(final Class<?> type) {
        return m_readAccesses.stream().anyMatch(f -> f.getType().equals(type));
    }

    /**
     * Get the first write access associated with the specified type from the registry.
     * 
     * @param type the type to get the access for
     * 
     * @return the write access for the specified type
     * 
     * @throws NoSuchElementException if no factory with the specified type is present in the registry
     */
    @SuppressWarnings("unchecked")
    public <T> NodeSettingsWriteAccessFactory<?, T> getWriteAccessFactoryFor(final Class<T> type) {
        return (NodeSettingsWriteAccessFactory<?, T>)m_writeAccesses.stream().filter(f -> f.getType().equals(type))
            .findFirst().get();
    }

    /**
     * Checks if the registry contains a factory with the specified type.
     * 
     * @param type the type to search in the registry
     * 
     * @return true if the registry contains a factory with the specified type, false otherwise
     */
    public boolean hasWriteAccessFactoryFor(final Class<?> type) {
        return m_writeAccesses.stream().anyMatch(f -> f.getType().equals(type));
    }
}
