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
package org.knime.dl.keras.core.training;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.text.StrSubstitutor;
import org.knime.dl.core.training.DLOptimizer;

/**
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
public interface DLKerasOptimizer extends DLOptimizer<String> {

	// NB: marker interface

	// TODO: we should add a "since" attribute to these optimizers to enable checking if they're available for the local
	// Keras installation

	public static class DLKerasRMSProp implements DLKerasOptimizer {

		private final float lr = 0.001f;

		private final float rho = 0.9f;

		private final float epsilon = 1e-8f;

		private final float decay = 0f;

		@Override
		public String get() {
			final String template = "keras.optimizers.RMSprop(lr=${lr}, rho=${rho}, epsilon=${epsilon}, decay=${decay})";
			final Map<String, String> values = new HashMap<>();
			values.put("lr", String.valueOf(lr));
			values.put("rho", String.valueOf(rho));
			values.put("epsilon", String.valueOf(epsilon));
			values.put("decay", String.valueOf(decay));
			return StrSubstitutor.replace(template, values);
		}
	}

	public static class DLKerasStochasticGradientDescent implements DLKerasOptimizer {

		private final float lr = 0.01f;

		private final float momentum = 0f;

		private final float decay = 0f;

		private final boolean nesterov = false;

		@Override
		public String get() {
			final String template = "keras.optimizers.SGD(lr=${lr}, momentum=${momentum}, decay=${decay}, nesterov=${nesterov})";
			final Map<String, String> values = new HashMap<>();
			values.put("lr", String.valueOf(lr));
			values.put("momentum", String.valueOf(momentum));
			values.put("decay", String.valueOf(decay));
			values.put("nesterov", String.valueOf(nesterov));
			return StrSubstitutor.replace(template, values);
		}
	}
}
