package org.knime.dl.python.util;

/**
 * Various Python specific utility methods and classes.
 *
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public final class DLPythonUtils {

	private static final String TRUE = "True";
	private static final String FALSE = "False";

	private static final String INFINITY = "inf";
	private static final String NAN = "NaN";

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
}
