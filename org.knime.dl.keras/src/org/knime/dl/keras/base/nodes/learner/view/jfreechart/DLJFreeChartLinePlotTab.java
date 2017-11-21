package org.knime.dl.keras.base.nodes.learner.view.jfreechart;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.text.DefaultCaret;

import org.knime.dl.keras.base.nodes.learner.view.DLFloatData;
import org.knime.dl.keras.base.nodes.learner.view.DLLinePlotViewData;
import org.knime.dl.keras.base.nodes.learner.view.DLView;

public class DLJFreeChartLinePlotTab extends JPanel {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private final DLJFreeChartLinePlotView m_linePlotView;
	private final JTextArea m_historyArea = new JTextArea();
	
	public DLJFreeChartLinePlotTab(final DLJFreeChartLinePlotViewSpec plotViewSpec) {
		super(new GridBagLayout());
		
		m_linePlotView = new DLJFreeChartLinePlotView(plotViewSpec);
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
        gbc.weightx = 2;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.BOTH;
        add(m_linePlotView.getComponent(), gbc);
        
        
        JScrollPane historyScroller = new JScrollPane(m_historyArea);
        historyScroller.setBorder(BorderFactory.createTitledBorder(plotViewSpec.title() + ":"));
        m_historyArea.setEditable(false);
        m_historyArea.setColumns(20);
        m_historyArea.setRows(20);

        //enable automatic to bottom scrolling
        DefaultCaret caret = (DefaultCaret)m_historyArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        
        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.weighty = 1;
        add(historyScroller, gbc);
	}
	
	public void update(DLLinePlotViewData<DLJFreeChartLinePlotViewSpec> data) {		
		List<List<String>> historyForPlots = new ArrayList<List<String>>();
		for (int i = 0; i < data.getViewSpec().numPlots(); i++) {
			ArrayList<String> plotHistory = new ArrayList<String>();
			String plotLabel = data.getViewSpec().getLineLabel(i);
			
			Iterator<DLFloatData> it = data.getData(i);
			while (it.hasNext()) {
				float value = it.next().get();
				m_linePlotView.plotNext(i, value);
				plotHistory.add(plotLabel + ": " + value);
			}
			historyForPlots.add(plotHistory);
		}
		
		
		m_historyArea.append(historyForPlots.toString() + "\n");
		
	}
	
	public DLView<?> getDLView(){
		return m_linePlotView;
	}
	
	public JTextArea getHistoryArea(){
		return m_historyArea;
	}

}
