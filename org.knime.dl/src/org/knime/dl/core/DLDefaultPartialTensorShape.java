package org.knime.dl.core;

import java.util.OptionalLong;

/**
 * Default implementation of a {@link DLPartialTensorShape partially defined tensor shape}.
 * 
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 *
 */
public class DLDefaultPartialTensorShape extends DLAbstractPartialTensorShape {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public DLDefaultPartialTensorShape(OptionalLong[] shape) {
		super(shape);
	}

}