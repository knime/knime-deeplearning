package org.knime.dl.keras.base.nodes.learner.view;

import java.awt.Component;
import java.util.Iterator;

public interface DLView<D extends DLLinePlotViewSpec> {

	Component getComponent();

	void update(D spec, Iterator<DLFloatData>[] iterators);

}
