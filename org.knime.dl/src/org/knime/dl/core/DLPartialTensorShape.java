package org.knime.dl.core;

import java.util.OptionalLong;

public interface DLPartialTensorShape extends DLTensorShape {

	/**
	 * 
	 * @param i dimension index
	 * @return the size of dimension i if known
	 * @throws IndexOutOfBoundsException if i < 0 or i >= numDimensions
	 */
	OptionalLong getDimension(final int i);
	
	int getNumUnknownDimensions();
	
	long getKnownSize();
}
