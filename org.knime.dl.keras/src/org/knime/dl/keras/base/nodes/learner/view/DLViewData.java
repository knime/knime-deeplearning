package org.knime.dl.keras.base.nodes.learner.view;

public interface DLViewData<S extends DLViewSpec> {

	S getViewSpec();

}
