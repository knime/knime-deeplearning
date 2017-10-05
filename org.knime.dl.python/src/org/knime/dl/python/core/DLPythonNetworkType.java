/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
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
package org.knime.dl.python.core;

import java.net.URL;

import org.knime.dl.core.DLExternalNetworkType;
import org.knime.python2.kernel.PythonModuleExtensions;

/**
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
public interface DLPythonNetworkType<N extends DLPythonNetwork<S>, S extends DLPythonNetworkSpec>
		extends DLExternalNetworkType<N, S, URL> {

	@Override
	DLPythonNetworkLoader<N> getLoader();

	/**
	 * Returns the name of the module that contains this network type's counterpart on Python side. The module must be
	 * discoverable via the PYTHONPATH (this can be ensured by registration at extension point
	 * {@link PythonModuleExtensions}).
	 * <P>
	 * The network type class on Python side has to extend the abstract base class <code>DLPythonNetworkType</code> from
	 * module <code>DLPythonNetworkType</code> and implement its abstract properties and methods. A singleton instance
	 * of the class has to be created and registered via <code>DLPythonNetworkType.add_network_type(instance)</code>.
	 * <P>
	 * The network type's module must not import any third party modules (i.e. no modules that are not part of the
	 * Python standard library or not provided by KNIME - especially no modules that belong to the network type's back
	 * end).<br>
	 * Thus, the actual implementation of the network type's functionality must be kept within a separate module
	 * (recommended naming scheme is <code>DL*NetworkType</code> for the network type's module and
	 * <code>DL*Network</code> for the module that contains the implementation). The network type class may only import
	 * the implementation module lazily (i.e. in method scope).
	 *
	 * @return the module name
	 */
	String getPythonModuleName();
}