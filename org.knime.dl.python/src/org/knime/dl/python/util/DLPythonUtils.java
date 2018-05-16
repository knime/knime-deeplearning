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
package org.knime.dl.python.util;

/**
 * Various Python specific utility methods and classes.
 *
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public final class DLPythonUtils {

	public static final String TRUE = "True";
	public static final String FALSE = "False";

	public static final String NONE = "None";

	public static final String INFINITY = "inf";
	public static final String NAN = "NaN";

	private static final char QUOTE = '"';

	private static final char[] FORMATTED_STRING_PREFIXES = new char[] { 'f', 'F' };
	private static final char[] RAW_STRING_PREFIXES = new char[] { 'r', 'R' };

	private DLPythonUtils() {
	}

	public static DLPythonSourceCodeBuilder createSourceCodeBuilder() {
		return new DLPythonSourceCodeBuilder();
	}

	public static DLPythonSourceCodeBuilder createSourceCodeBuilder(final String firstCodeLine) {
		return createSourceCodeBuilder().a(firstCodeLine).n();
	}

	public static String toPython(final boolean b) {
		return b ? TRUE : FALSE;
	}

	public static String toPython(final double d) {
		if (Double.isInfinite(d)) {
			if (d > 0) {
				return INFINITY;
			} else {
				return "-" + INFINITY;
			}
		}
		if (Double.isNaN(d)) {
			return NAN;
		}
		return String.valueOf(d);
	}

	public static String toPython(final float f) {
		if (Float.isInfinite(f)) {
			if (f > 0) {
				return INFINITY;
			} else {
				return "-" + INFINITY;
			}
		}
		if (Float.isNaN(f)) {
			return NAN;
		}
		return String.valueOf(f);
	}

	public static String toPython(final int i) {
		return String.valueOf(i);
	}

	public static String toPython(final long l) {
		return String.valueOf(l);
	}

	public static String toPython(final String s) {
		// TODO: check if already in quotes etc.
		return QUOTE + s + QUOTE;
	}

	public static String toPythonFormattedString(final String s) {
		return FORMATTED_STRING_PREFIXES[0] + toPython(s);
	}

	public static String toPythonRawString(final String s) {
		return RAW_STRING_PREFIXES[0] + toPython(s);
	}

	// Arrays:

	public static String toPython(final boolean[] ba) {
		final String[] str = new String[ba.length];
		for (int i = 0; i < str.length; i++) {
			str[i] = toPython(ba[i]);
		}
		return toPythonList(str);
	}

	public static String toPython(final double[] da) {
		final String[] str = new String[da.length];
		for (int i = 0; i < str.length; i++) {
			str[i] = toPython(da[i]);
		}
		return toPythonList(str);
	}

	public static String toPython(final float[] fa) {
		final String[] str = new String[fa.length];
		for (int i = 0; i < str.length; i++) {
			str[i] = toPython(fa[i]);
		}
		return toPythonList(str);
	}

	public static String toPython(final int[] ia) {
		final String[] str = new String[ia.length];
		for (int i = 0; i < str.length; i++) {
			str[i] = toPython(ia[i]);
		}
		return toPythonList(str);
	}

	public static String toPython(final long[] la) {
		final String[] str = new String[la.length];
		for (int i = 0; i < str.length; i++) {
			str[i] = toPython(la[i]);
		}
		return toPythonList(str);
	}

	public static String toPython(final String[] sa) {
		final String[] str = new String[sa.length];
		for (int i = 0; i < str.length; i++) {
			str[i] = toPython(sa[i]);
		}
		return toPythonList(str);
	}

	public static String toPythonFormattedStringArray(final String[] sa) {
		final String[] str = new String[sa.length];
		for (int i = 0; i < str.length; i++) {
			str[i] = toPythonFormattedString(sa[i]);
		}
		return toPythonList(str);
	}

	public static String toPythonRawStringArray(final String[] sa) {
		final String[] str = new String[sa.length];
		for (int i = 0; i < str.length; i++) {
			str[i] = toPythonRawString(sa[i]);
		}
		return toPythonList(str);
	}

	private static String toPythonList(final String[] elements) {
		return "[" + String.join(",", elements) + "]";
	}
}
