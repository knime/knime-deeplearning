package org.knime.dl.keras.base.nodes.learner.view;

import java.awt.Component;

public interface DLView<D extends DLViewData<?>> {

	Component getComponent();

	void update(D data);

}
