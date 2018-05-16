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
package org.knime.dl.keras.core.training;

import static org.knime.dl.util.DLUtils.Preconditions.checkNotNullOrEmpty;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.knime.dl.core.training.DLMetric;
import org.knime.dl.python.util.DLPythonUtils;
import org.knime.dl.util.DLUtils.Preconditions;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public interface DLKerasMetric extends DLMetric {

	/**
	 * @return the identifier for this metric on Python side
	 */
	String getKerasIdentifier();

	/**
	 * Keras metrics are nothing more than a name and a back end string identifier. It does not make sense to give them
	 * an additional, artificial identity on Java side. Therefore, this method delegates to
	 * {@link #getKerasIdentifier()}.
	 */
	@Override
	default String getIdentifier() {
		return getKerasIdentifier();
	}

	@Override
	default String getBackendRepresentation() {
		return getKerasIdentifier();
	}

	/**
	 * Keras metrics are nothing more than a name and a back end string identifier. Therefore, implementing classes
	 * should omit a strict type check like <code>obj.getClass() == getClass()</code> when testing for equality and
	 * instead just check for this interface.
	 */
	@Override
	boolean equals(Object obj);

	/**
	 * Abstract base class for implementations of {@link DLKerasMetric}.
	 */
	public abstract static class DLKerasAbstractMetric implements DLKerasMetric {

		// TODO: we should add a "since" attribute to this class (or even interface) to enable checking if deriving
		// classes are available for the local Keras installation. This implies changes in the installation testers on
		// Python side as they have to extract the libs' versions.

		private final String m_name;

		private final String m_kerasIdentifier;

		/**
		 * @param name the friendly name of the metric, not null, not empty, suitable to be displayed to the user
		 * @param kerasIdentifier the identifier for this metric on Python side
		 */
		protected DLKerasAbstractMetric(final String name, final String kerasIdentifier) {
			m_name = checkNotNullOrEmpty(name);
			m_kerasIdentifier = checkNotNullOrEmpty(kerasIdentifier);
		}

		@Override
		public String getName() {
			return m_name;
		}

		@Override
		public String getKerasIdentifier() {
			return m_kerasIdentifier;
		}

		@Override
		public int hashCode() {
			final HashCodeBuilder b = new HashCodeBuilder(17, 37);
			b.append(m_name);
			b.append(m_kerasIdentifier);
			return b.toHashCode();
		}

		@Override
		public boolean equals(final Object obj) {
			if (obj == this) {
				return true;
			}
			/* no strict type check, see documentation of DLKerasMetric#equals(Object) */
			if (obj == null || !(obj instanceof DLKerasMetric)) {
				return false;
			}
			final DLKerasMetric other = (DLKerasMetric) obj;
			return other.getName().equals(m_name) //
					&& other.getKerasIdentifier().equals(m_kerasIdentifier) //
					// We care about identity on the back end side, so this check is also needed. Check for
					// getIdentifier() is not.
					&& other.getBackendRepresentation().equals(m_kerasIdentifier);
		}

		@Override
		public String toString() {
			return getName() + " (" + getBackendRepresentation() + ")";
		}
	}
	
	public abstract static class DLKerasAbstractCustomMetric extends DLKerasAbstractMetric {
	    
	    private final String m_functionIdentifier;

	    private String m_customCode;
	    
	    private final String m_functionMatcher;
	    
        /**
         * @param name
         * @param kerasIdentifier
         */
        protected DLKerasAbstractCustomMetric(String name, String identifier, String tensorIdentifier) {
            super(name, identifier + "_" + replaceIncompatibleCharsWithUnderscores(tensorIdentifier));
            m_functionIdentifier = identifier;
            m_functionMatcher = "def " + m_functionIdentifier + "(";
            m_customCode = getDefaultCode();
        }
        
        protected String getDefaultCode() {
            return DLPythonUtils.createSourceCodeBuilder()
                    .a("import keras.backend as K")
                    .n()
                    .n("def ").a(m_functionIdentifier).a("(y_true, y_pred):")
                    .n().t().a("# insert your custom code here")
                    .n().t().a("return K.categorical_crossentropy(y_true, y_pred)")
                    .toString();
        }
        
        // TODO figure out which other characters are not allowed and replace them as well
        public static String replaceIncompatibleCharsWithUnderscores(String string) {
            return string.replaceAll("[^\\w_]", "_");
        }
        
        public String getCustomCodeDialog() {
            return m_customCode;
        }
        
        public String getCustomCodeExecution() {
            String executionCode = m_customCode.replaceAll(m_functionIdentifier, getBackendRepresentation());
            return executionCode;
        }
        
        public void setCustomCode(String customCode) {
            Preconditions.checkNotNullOrEmpty(customCode);
            if (!customCode.contains(m_functionMatcher)) {
                throw new IllegalArgumentException("The provided code does not contain the required function '"
            + m_functionIdentifier + "'.");
            }
            m_customCode = customCode;
        }
        
	    
	}

	public static final class DLKerasAccuracy extends DLKerasAbstractMetric {

		public DLKerasAccuracy() {
			super("Accuracy", "acc");
		}
	}
}
