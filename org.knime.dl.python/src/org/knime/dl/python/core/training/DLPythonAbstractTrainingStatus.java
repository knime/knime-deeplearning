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
package org.knime.dl.python.core.training;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.knime.dl.core.training.DLAbstractTrainingStatus;

/**
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 */
public abstract class DLPythonAbstractTrainingStatus extends DLAbstractTrainingStatus implements DLPythonTrainingStatus {

    private String m_stdOut = "";

    private String m_stdErr = "";

    /**
     * @param numEpochs must be greater than zero
     * @param numBatchesPerEpoch must be greater than zero
     */
    public DLPythonAbstractTrainingStatus(final int numEpochs, final int numBatchesPerEpoch) {
        super(numEpochs, numBatchesPerEpoch);
    }

    /**
     * Empty framework constructor. Must not be called by client code.
     */
    public DLPythonAbstractTrainingStatus() {
    }

    @Override
    public String getStdOutOutput() {
        return m_stdOut;
    }

    @Override
    public String getStdErrOutput() {
        return m_stdErr;
    }

    @Override
    public void setStdOutOutput(final String stdOut) {
        m_stdOut = stdOut;
    }

    @Override
    public void setStdErrOutput(final String stdErr) {
        m_stdErr = stdErr;
    }

    @Override
    public void writeExternal(final ObjectOutput objOut) throws IOException {
        super.writeExternal(objOut);
        objOut.writeObject(m_stdOut);
        objOut.writeObject(m_stdErr);
    }

    @Override
    public void readExternal(final ObjectInput objIn) throws IOException, ClassNotFoundException {
        super.readExternal(objIn);
        try {
            m_stdOut = (String)objIn.readObject();
            m_stdErr = (String)objIn.readObject();
        } catch (final IOException e) {
            // Backwards compatibility
        }
    }
}
