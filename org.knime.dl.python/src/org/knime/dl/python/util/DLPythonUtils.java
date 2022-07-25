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

import java.util.Arrays;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.function.Function;

import org.knime.python2.util.AbstractPythonSourceCodeBuilder;

/**
 * Various Python specific utility methods and classes.
 *
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 * @author David Kolb, KNIME GmbH, Konstanz, Germany
 */
public final class DLPythonUtils {

	public static final String TRUE = AbstractPythonSourceCodeBuilder.TRUE;
	public static final String FALSE = AbstractPythonSourceCodeBuilder.FALSE;

	public static final String NONE = AbstractPythonSourceCodeBuilder.NONE;

	public static final String INFINITY = AbstractPythonSourceCodeBuilder.INFINITY;
	public static final String NAN = AbstractPythonSourceCodeBuilder.NAN;

	private DLPythonUtils() {
	}

	public static DLPythonSourceCodeBuilder createSourceCodeBuilder() {
		return new DLPythonSourceCodeBuilder();
	}

	public static DLPythonSourceCodeBuilder createSourceCodeBuilder(final String firstCodeLine) {
		return createSourceCodeBuilder().a(firstCodeLine).n();
	}

	public static String toPython(final boolean b) {
		return AbstractPythonSourceCodeBuilder.toPython(b);
	}

	public static String toPython(final double d) {
	    return AbstractPythonSourceCodeBuilder.toPython(d);
	}

	public static String toPython(final float f) {
	    return AbstractPythonSourceCodeBuilder.toPython(f);
	}

	public static String toPython(final int i) {
		return AbstractPythonSourceCodeBuilder.toPython(i);
	}

	public static String toPython(final long l) {
	    return AbstractPythonSourceCodeBuilder.toPython(l);
	}

    public static String toPython(final Long l) {
        return AbstractPythonSourceCodeBuilder.toPython(l);
    }

	public static String toPython(final Float f) {
	    return AbstractPythonSourceCodeBuilder.toPython(f);
    }

	public static String toPython(final Double d) {
	    return AbstractPythonSourceCodeBuilder.toPython(d);
	}

	public static String toPython(final String s) {
	    return AbstractPythonSourceCodeBuilder.toPython(s);
	}

	public static String toPythonFormattedString(final String s) {
	    return AbstractPythonSourceCodeBuilder.toPythonFormattedString(s);
	}

	public static String toPythonRawString(final String s) {
	    return AbstractPythonSourceCodeBuilder.toPythonRawString(s);
	}

	// Arrays:

	public static String toPython(final boolean[] ba) {
	    return AbstractPythonSourceCodeBuilder.toPython(ba);
	}

	public static String toPython(final double[] da) {
	    return AbstractPythonSourceCodeBuilder.toPython(da);
	}

	public static String toPython(final float[] fa) {
	    return AbstractPythonSourceCodeBuilder.toPython(fa);
	}

	public static String toPython(final int[] ia) {
	    return AbstractPythonSourceCodeBuilder.toPython(ia);
	}

	public static String toPython(final long[] la) {
	    return AbstractPythonSourceCodeBuilder.toPython(la);
	}

	public static String toPython(final Long[] la) {
	    return AbstractPythonSourceCodeBuilder.toPython(la);
    }

    public static String toPython(final Long[][] la) {
        return AbstractPythonSourceCodeBuilder.toPython(la);
    }

	public static String toPython(final String[] sa) {
	    return AbstractPythonSourceCodeBuilder.toPython(sa);
	}

	public static String toPython(final OptionalLong ol) {
	    return AbstractPythonSourceCodeBuilder.toPython(ol);
	}

	public static String toPython(final OptionalInt oi) {
	    return AbstractPythonSourceCodeBuilder.toPython(oi);
    }

	public static String toPython(final OptionalDouble od) {
	    return AbstractPythonSourceCodeBuilder.toPython(od);
    }

	public static String toPython(final Optional<String> os) {
	    return AbstractPythonSourceCodeBuilder.toPython(os);
	}

	public static <T> String toPython(final Optional<T> oo, final Function<T, String> toString) {
	    return AbstractPythonSourceCodeBuilder.toPython(oo, toString);
	}

	public static String toPythonFormattedStringArray(final String[] sa) {
	    return AbstractPythonSourceCodeBuilder.toPythonFormattedStringArray(sa);
	}

	public static String toPythonRawStringArray(final String[] sa) {
	    return AbstractPythonSourceCodeBuilder.toPythonRawStringArray(sa);
	}

	public static String toPythonTuple(final String[] elements) {
	    return AbstractPythonSourceCodeBuilder.toPythonTuple(elements);
    }

	public static String toPythonTuple(final String elements) {
	    return AbstractPythonSourceCodeBuilder.toPythonTuple(elements);
    }

	public static Long[] parseShape(final String shapeString) {
	    return Arrays.stream(shapeString.split(",")) //
	            .map(String::trim) //
	            .map(s -> s.equals("?") ? null : Long.parseLong(s)) //
	            .toArray(Long[]::new);
	}

}
