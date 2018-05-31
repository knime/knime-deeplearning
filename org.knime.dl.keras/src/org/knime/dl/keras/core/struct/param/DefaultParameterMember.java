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

import java.lang.reflect.Type;

/**
 * Default implemenation of a {@link ParameterMember}.
 * 
 * NB: Heavily inspired by work of Curtis Rueden.
 * 
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
abstract class DefaultParameterMember<T> implements ParameterMember<T> {

    private final Type m_itemType;

    private final Parameter m_annotation;

    private ParameterChoices<T> m_choices;

    /**
     * Constructor.
     * 
     * @param itemType type of the underlying parameter
     * @param annotation associcated annotation
     */
    public DefaultParameterMember(final Type itemType, final Parameter annotation) {
        m_itemType = itemType;
        m_annotation = annotation;
    }

    // -- AnnotatedParameterMember methods --
    public Parameter getAnnotation() {
        return m_annotation;
    }

    // -- ParameterMember methods --

    @SuppressWarnings("unchecked")
    @Override
    public ParameterChoices<T> choices() {
        if (m_choices != null) {
            return m_choices;
        }

        final Class<T> type = (Class<T>)getAnnotation().choices();
        if (!type.equals(ParameterChoices.class)) {
            try {
                return m_choices = (ParameterChoices<T>)type.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new IllegalStateException(
                    "Can't create instance of a choice. Most likely empty constructor is missing.", e);
            }
        } else if (getRawType().isEnum()) {
            // TODO urks
            return createEnumChoices((Class)getRawType());
        }
        return null;
    }

    private <V extends Enum<V>> ParameterChoices<V> createEnumChoices(Class<V> rawType) {
        return new ParameterEnumChoices<>(rawType.getEnumConstants());
    }

    @Override
    public boolean isRequired() {
        return getAnnotation().required();
    }

    @Override
    public String getWidgetStyle() {
        return getAnnotation().style();
    }

    @Override
    public Object getMinimumValue() {
        return getAnnotation().min();
    }

    @Override
    public Object getMaximumValue() {
        return getAnnotation().max();
    }

    @Override
    public Object getStepSize() {
        return getAnnotation().stepSize();
    }

    @Override
    public String getLabel() {
        return getAnnotation().label();
    }

    // -- Member methods --
    @Override
    public String getKey() {
        return getAnnotation().key();
    }

    @Override
    public Type getType() {
        return m_itemType;
    }
}
