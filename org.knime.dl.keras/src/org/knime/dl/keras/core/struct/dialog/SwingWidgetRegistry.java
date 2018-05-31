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
package org.knime.dl.keras.core.struct.dialog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.knime.dl.keras.core.struct.Member;
import org.knime.dl.keras.core.struct.Struct;

/**
 * Registry for {@link SwingWidget}s.
 * 
 * NB: Heavily inspired by work of Curtis Rueden.
 * 
 * @author Christian, KNIME GmbH, Konstanz, Germany
 */
class SwingWidgetRegistry {
    private static SwingWidgetRegistry instance;

    private final List<SwingWidgetFactory<?>> m_factories;

    private SwingWidgetRegistry() {
        m_factories = new ArrayList<>();

        m_factories.add(new SwingNumberWidgetFactory());
        m_factories.add(new SwingTextWidgetFactory());
        m_factories.add(new SwingStructWidgetFactory<>());
        m_factories.add(new SwingObjectChoiceWidgetFactory<>());
        m_factories.add(new SwingCheckboxWidgetFactory());
        m_factories.add(new SwingOptionalWidgetFactory<>());
    }

    /**
     * @return singleton instance.
     */
    public static synchronized SwingWidgetRegistry getInstance() {
        if (instance == null)
            instance = new SwingWidgetRegistry();

        return instance;
    }

    /**
     * Create a map of {@link SwingWidget}s based on the {@link Struct}.
     * 
     * @param struct for which {@link SwingWidget}s are created.
     * @return {@link Map} of {@link SwingWidget}s.
     */
    public Map<String, SwingWidget<?>> createWidgets(final Struct struct) {
        final Map<String, SwingWidget<?>> widgets = new HashMap<>();

        for (final Member<?> model : struct) {
            final SwingWidget<?> widget = createWidget(model);
            if (widget == null) {
                // fail - FIXME
                throw new RuntimeException(model + " is required but none exist.");
            }
            widgets.put(model.getKey(), widget);
        }
        return widgets;
    }

    /**
     * @param member
     * @return {@link SwingWidget} for {@link Member}
     */
    public <T> SwingWidget<T> createWidget(final Member<T> member) {
        for (final SwingWidgetFactory<?> factory : m_factories) {
            if (!factory.supports(member))
                continue;
            @SuppressWarnings("unchecked")
            final SwingWidgetFactory<T> typedFactory = (SwingWidgetFactory<T>)factory;
            return typedFactory.create(member);
        }
        return null;
    }
}
