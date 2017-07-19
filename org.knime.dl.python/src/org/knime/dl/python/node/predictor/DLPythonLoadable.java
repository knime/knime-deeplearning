package org.knime.dl.python.node.predictor;

import org.knime.python2.kernel.PythonKernel;

// TODO
/**
 * Workaround. Will be replaced with handle framework (backwards-compatible).
 *
 * @author Christian Dietz, KNIME
 *
 */
@Deprecated
public interface DLPythonLoadable {

    /**
     * @param kernel representing the Python process where object is loaded.
     * @return variable name of object
     * @throws Exception
     */
    @Deprecated
    String load(final PythonKernel kernel) throws Exception;
}
