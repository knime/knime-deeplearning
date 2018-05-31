package org.knime.dl.keras.core.struct.param;

import org.knime.dl.keras.core.struct.Struct;

/**
 * Exception thrown in case something goes wrong during construction of a {@link Struct} from {@link Parameter}
 * annotations.
 * 
 * @author Christian Dietz, KNIME GmbH, Konstanz, Germany
 */
public class ValidityException extends Exception {

    /**
     * UID
     */
    private static final long serialVersionUID = 1L;

    /**
     * @param msg exception message
     * @param ex parent
     */
    public ValidityException(String msg, Exception ex) {
        super(msg, ex);
    }

    /**
     * @param msg exception message
     */
    public ValidityException(String msg) {
        super(msg);
    }

}
