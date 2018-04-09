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
import java.util.function.Function;

/**
 * @author Marcel Wiedenmann, KNIME GmbH, Konstanz, Germany
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public abstract class DLPythonAbstractSourceCodeBuilder<B extends DLPythonAbstractSourceCodeBuilder<B>> {

	private static final char NEW_LINE = '\n';

	private static final char TAB = '\t';

	private final StringBuilder sb = new StringBuilder();

	protected StringBuilder getInnerStringBuilder() {
		return sb;
	}

	// Elements:

	public B a(final boolean b) {
		a(DLPythonUtils.toPython(b));
		return thisCasted();
	}

	public B a(final double d) {
		a(DLPythonUtils.toPython(d));
		return thisCasted();
	}

	public B a(final float f) {
		a(DLPythonUtils.toPython(f));
		return thisCasted();
	}

	public B a(final int i) {
		a(DLPythonUtils.toPython(i));
		return thisCasted();
	}

	public B a(final long l) {
		a(DLPythonUtils.toPython(l));
		return thisCasted();
	}

	public B as(final String s) {
		sb.append(DLPythonUtils.toPython(s));
		return thisCasted();
	}

	public B asf(final String s) {
		sb.append(DLPythonUtils.toPythonFormattedString(s));
		return thisCasted();
	}

	public B asr(final String s) {
		sb.append(DLPythonUtils.toPythonRawString(s));
		return thisCasted();
	}

	// Arrays:

	public B a(final boolean[] ba) {
		a(DLPythonUtils.toPython(ba));
		return thisCasted();
	}

	public B a(final double[] da) {
		a(DLPythonUtils.toPython(da));
		return thisCasted();
	}

	public B a(final float[] fa) {
		a(DLPythonUtils.toPython(fa));
		return thisCasted();
	}

	public B a(final int[] ia) {
		a(DLPythonUtils.toPython(ia));
		return thisCasted();
	}

	public B a(final long[] la) {
		a(DLPythonUtils.toPython(la));
		return thisCasted();
	}

	public B as(final String[] sa) {
		sb.append(DLPythonUtils.toPython(sa));
		return thisCasted();
	}

	public B asf(final String[] sa) {
		sb.append(DLPythonUtils.toPythonFormattedStringArray(sa));
		return thisCasted();
	}

	public B asr(final String[] sa) {
		sb.append(DLPythonUtils.toPythonRawStringArray(sa));
		return thisCasted();
	}

	// Code:

	public B a(final String code) {
		sb.append(code);
		return thisCasted();
	}

	public B n(final String line) {
		n();
		sb.append(line);
		return thisCasted();
	}

	public B n(final String... lines) {
		n(Arrays.asList(lines));
		return thisCasted();
	}

	public B n(final Iterable<String> lines) {
		for (final String line : lines) {
			n(line);
		}
		return thisCasted();
	}

	public <T> B n(final Iterable<T> lines, final Function<T, String> toString) {
		for (final T line : lines) {
			n(toString.apply(line));
		}
		return thisCasted();
	}

	public B n() {
		sb.append(NEW_LINE);
		return thisCasted();
	}

	public B t() {
		sb.append(TAB);
		return thisCasted();
	}

	@Override
	public String toString() {
		return sb.toString();
	}

	@SuppressWarnings("unchecked")
	private B thisCasted() {
		return (B) this;
	}
}
