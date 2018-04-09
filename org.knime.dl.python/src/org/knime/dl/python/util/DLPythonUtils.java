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
