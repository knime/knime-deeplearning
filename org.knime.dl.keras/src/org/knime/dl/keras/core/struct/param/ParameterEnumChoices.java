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

/**
 * Represents an {@link Enum} from which a particular {@link Enum} value can be selected e.g. in a dialog.
 * 
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 * @param <T> type of {@link Enum}.
 */
public class ParameterEnumChoices<T extends Enum<?>> implements ParameterChoices<T> {

    private final ParameterChoice<T>[] m_choices;

    private T[] m_constants;

    /**
     * @param in of {@link Enum}. Expected to be an {@link Enum}.
     */
    @SuppressWarnings("unchecked")
    public ParameterEnumChoices(T[] in) {
        m_constants = in;
        m_choices = new ParameterChoice[m_constants.length];
        for (int i = 0; i < m_choices.length; i++) {
            final int idx = i;
            m_choices[i] = new ParameterChoice<T>() {

                @Override
                public Class<T> getRawType() {
                    return (Class<T>)m_constants.getClass().getComponentType();
                }

                @Override
                public String getKey() {
                    return m_constants[idx].name();
                }

                @Override
                public T get() {
                    return m_constants[idx];
                }
            };
        }
    }

    @Override
    public ParameterChoice<T>[] choices() {
        return m_choices;
    }

    @Override
    public ParameterChoice<? extends T> fromKey(String key) {
        for (ParameterChoice<T> choice : m_choices) {
            if (choice.getKey().equals(key)) {
                return choice;
            }
        }
        return null;
    }

    @Override
    public ParameterChoice<? extends T> fromObject(T obj) {
        for (ParameterChoice<T> choice : m_choices) {
            if (choice.getKey().equals(obj.name())) {
                return choice;
            }
        }
        return null;
    }
}
