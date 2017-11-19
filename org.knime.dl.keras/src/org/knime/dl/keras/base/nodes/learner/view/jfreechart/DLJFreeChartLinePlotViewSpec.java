package org.knime.dl.keras.base.nodes.learner.view.jfreechart;

import org.knime.dl.keras.base.nodes.learner.view.DLLinePlotViewSpec;

public interface DLJFreeChartLinePlotViewSpec extends DLLinePlotViewSpec {

	String labelY();

	String labelX();

	String getLineLabel(int i);
}
