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
package org.knime.dl.base.nodes;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;
import org.knime.dl.base.nodes.DLConverterRefresher.DLNoConverterAvailableException;
import org.knime.dl.base.settings.DLDataTypeColumnFilter;
import org.knime.dl.base.settings.DLInputConfig;
import org.knime.dl.core.DLContext;
import org.knime.dl.core.DLTensorSpec;
import org.knime.dl.core.data.convert.DLDataValueToTensorConverterFactory;
import org.knime.dl.util.DLUtils;

import com.google.common.collect.Sets;

/**
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public class DLConfigurationUtility {

    private DLConfigurationUtility() {
        // utility class
    }

    /**
     * Configures a single input tensor. Note that this method <b>has</b> side effects:</br>
     * -The converter for the tensor is set in the input config</br>
     * -The column filter config is updated to comply to the converter selection
     * 
     * 
     * @param cfg the config of the input tensor
     * @param tensorSpec the spec of the input tensor
     * @param context the {@link DLContext} i.e. the backend used
     * @param tableSpec the current data table spec
     * @param lastConfiguredTableSpec the last configured data table spec
     * @param tensorRole the role of the tensor e.g. "Input" or "Target"
     * @return the converter for the tensor described by <b>tensorSpec</b>
     * @throws InvalidSettingsException if the tensor has an invalid shape or if a column is missing
     */
    public static DLDataValueToTensorConverterFactory<?, ?> configureInput(DLInputConfig<?> cfg,
        DLTensorSpec tensorSpec, DLContext<?> context, DataTableSpec tableSpec, DataTableSpec lastConfiguredTableSpec,
        String tensorRole) throws InvalidSettingsException {
        validateTensorSpec(tensorSpec, tensorRole);
        DLDataValueToTensorConverterFactory<?, ?> converter = getConverter(tableSpec, context, tensorSpec, cfg);
        updateColFilterConfig(tableSpec, tensorSpec, cfg, converter, lastConfiguredTableSpec, tensorRole);
        return converter;
    }

    private static void validateTensorSpec(DLTensorSpec tensorSpec, String tensorPurpose)
        throws InvalidSettingsException {
        if (!DLUtils.Shapes.isKnown(tensorSpec.getShape())) {
            throw new InvalidSettingsException(
                tensorPurpose + " '" + tensorSpec.getName() + "' has an unknown shape. This is not supported, yet.");
        }
    }

    private static DLDataValueToTensorConverterFactory<?, ?> getConverter(final DataTableSpec inTableSpec,
        DLContext<?> context, final DLTensorSpec tensorSpec, final DLInputConfig<?> inputCfg)
        throws DLNoConverterAvailableException {
        DLDataValueToTensorConverterFactory<?, ?> converter = inputCfg.getConverterEntry().getValue();
        if (converter == null) { // TODO: or if table changed
            final Comparator<DLDataValueToTensorConverterFactory<?, ?>> nameComparator =
                Comparator.comparing(DLDataValueToTensorConverterFactory::getName);
            final DLConverterRefresher converterRefresher = new DLConverterRefresher(inTableSpec,
                context.getTensorFactory().getWritableBufferType(tensorSpec), tensorSpec, false, nameComparator);
            final List<DLDataValueToTensorConverterFactory<?, ?>> converterFactories =
                converterRefresher.getConverters();
            converter = converterFactories.get(0);
            inputCfg.getConverterEntry().setValue(converter);
        }
        return converter;
    }

    private static void updateColFilterConfig(final DataTableSpec inTableSpec, final DLTensorSpec tensorSpec,
        final DLInputConfig<?> inputCfg, DLDataValueToTensorConverterFactory<?, ?> converter,
        final DataTableSpec lastConfiguredTableSpec, String tensorRole) throws InvalidSettingsException {
        final DataColumnSpecFilterConfiguration filterConfig = inputCfg.getInputColumnsEntry().getValue();
        ((DLDataTypeColumnFilter)filterConfig.getFilter()).setFilterClasses(converter.getSourceType());
        // check if selected columns are still in input table
        if (lastConfiguredTableSpec != null) {
            if (includesChanged(inTableSpec, lastConfiguredTableSpec, filterConfig)) {
                throw new InvalidSettingsException("The included columns for " + tensorRole.toLowerCase() + " '"
                    + tensorSpec.getName() + "' changed. Please reconfigure the node.");
            }
            final String[] missingColumns = filterConfig.applyTo(inTableSpec).getRemovedFromIncludes();
            if (missingColumns.length != 0) {
                throw new InvalidSettingsException("Selected column '" + missingColumns[0] + "' of "
                    + tensorRole.toLowerCase() + " '" + tensorSpec.getName()
                    + "' is missing in the training data table. Please reconfigure the node.");
            }
        }
    }

    private static boolean includesChanged(DataTableSpec inTableSpec, DataTableSpec lastConfiguredTableSpec,
        DataColumnSpecFilterConfiguration filterConfig) {
        Set<String> includesOld = Sets.newHashSet(filterConfig.applyTo(lastConfiguredTableSpec).getIncludes());
        Set<String> includesNew = Sets.newHashSet(filterConfig.applyTo(inTableSpec).getIncludes());
        return !includesOld.equals(includesNew);
    }
}
