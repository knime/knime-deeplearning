package org.knime.dl.python.util;

/**
 * Various Python specific utility methods and classes.
 *
 * @author Marcel Wiedenmann, KNIME, Konstanz, Germany
 * @author Christian Dietz, KNIME, Konstanz, Germany
 */
public final class DLPythonUtils {

	private static final String TRUE = "True";
	private static final String FALSE = "False";

	private static final String[] FORMATTED_STRING_PREFIXES = new String[] { "f", "F" };
	private static final String[] RAW_STRING_PREFIXES = new String[] { "r", "R" };

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
		return String.valueOf(d);
	}

	public static String toPython(final float f) {
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
		return '"' + s + '"';
	}

	public static String toPythonFormattedString(final String s) {
		return FORMATTED_STRING_PREFIXES[0] + toPython(s);
	}

	public static String toPythonRawString(final String s) {
		return RAW_STRING_PREFIXES[0] + toPython(s);
	}
}
