package org.knime.dl.keras.core.struct.dialog;

import org.knime.dl.keras.core.struct.param.FieldParameterMember;
import org.knime.dl.keras.core.struct.param.Required;

/**
 * A wrapper for {@link FieldParameterMember}s which are hardcoded to be required.
 * Hence, {@link #getOptionalStatus()} will always return {@link Required#Required}.
 * 
 * @author David Kolb, KNIME GmbH, Konstanz, Germany
 * @param <T>
 */
public class RequiredFieldParameterMember<T> extends FieldParameterMember<T> {

    /**
     * @param field
     * @param structType
     */
    RequiredFieldParameterMember(final FieldParameterMember<T> member) {
        super(member.getField(), member.getStructType());
    }

    @Override
    public Required getOptionalStatus() {
        return Required.Required;
    }
}