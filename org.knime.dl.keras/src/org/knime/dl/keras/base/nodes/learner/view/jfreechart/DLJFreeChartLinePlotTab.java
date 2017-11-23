package org.knime.dl.keras.base.nodes.learner.view.jfreechart;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.Timer;
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
	private final List<JTextArea> m_historyAreas = new ArrayList<JTextArea>();
	private final List<JLabel> m_currentValueLabels = new ArrayList<JLabel>();
	private final float[] m_currentValues;
	
	private final Timer m_currentValueUpdateTimer = new Timer(1000, (e) -> updateCurrentValueLabels());
	
	public DLJFreeChartLinePlotTab(final DLJFreeChartLinePlotViewSpec plotViewSpec) {
		super(new GridBagLayout());
		
		m_currentValues = new float[plotViewSpec.numPlots()];
		
        JTabbedPane historyTabsPane = new JTabbedPane();
        GridBagConstraints gbc;
        
        for (int i = 0; i < plotViewSpec.numPlots(); i++) {
        	JTextArea historyArea = new JTextArea();
        	DefaultCaret caret = (DefaultCaret)historyArea.getCaret();
        	//enable automatic to bottom scrolling
            caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        	historyArea.setEditable(false);
        	m_historyAreas.add(historyArea);
        	
        	JScrollPane historyScroller = new JScrollPane(historyArea);
        	JPanel historyWrapper = new JPanel(new GridBagLayout());
        	gbc = new GridBagConstraints();
        	gbc.gridx = 0;
    		gbc.gridy = 0;
            gbc.weightx = 1;
            gbc.weighty = 1;
            gbc.fill = GridBagConstraints.BOTH;
            historyWrapper.add(historyScroller, gbc);
            
            JLabel currentValue = new JLabel("-");
            currentValue.setFont(new Font(currentValue.getFont().getName(), currentValue.getFont().getStyle(), 18));
            
            JPanel valueWrapperWithBorder = new JPanel(new GridLayout(0, 1));
            valueWrapperWithBorder.setBorder(BorderFactory.createTitledBorder("Current Value:"));
            valueWrapperWithBorder.add(currentValue);
            m_currentValueLabels.add(currentValue);
            
            gbc.gridy++;
            gbc.weighty = 0;
            gbc.insets = new Insets(10, 10, 10, 10);
            historyWrapper.add(valueWrapperWithBorder, gbc);
        	
        	historyTabsPane.addTab(plotViewSpec.getLineLabel(i), historyWrapper);
        }
		
		m_linePlotView = new DLJFreeChartLinePlotView(plotViewSpec);
		gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.gridy = 0;
        gbc.weightx = 0.6;
        gbc.weighty = 1;
        gbc.insets = new Insets(0, 0, 0, 10);
        gbc.fill = GridBagConstraints.BOTH;
        add(m_linePlotView.getComponent(), gbc);
        
        historyTabsPane.setPreferredSize(new Dimension(250, 500));
        historyTabsPane.setMinimumSize(new Dimension(250, 500));
        
        gbc.gridx = 1;
        gbc.weightx = 0.4;
        gbc.weighty = 1;
        add(historyTabsPane, gbc);
	}
	
	private void updateCurrentValueLabels(){
		for(int i = 0; i < m_currentValues.length; i++){
			m_currentValueLabels.get(i).setText(m_currentValues[i] + "");
		}
	}
	
	public void update(DLLinePlotViewData<DLJFreeChartLinePlotViewSpec> data) {		
		for (int i = 0; i < data.getViewSpec().numPlots(); i++) {			
			Iterator<DLFloatData> it = data.getData(i);
			while (it.hasNext()) {
				float value = it.next().get();
				m_linePlotView.plotNext(i, value);
				m_historyAreas.get(i).append(value + "\n");
				m_currentValues[i] = value;
			}
		}
	}
	
	public DLView<?> getDLView(){
		return m_linePlotView;
	}
	
	public void setCurrentValueTimerUpdateDelay(int miliseconds){
		m_currentValueUpdateTimer.setDelay(miliseconds);
	}
	
	public void startCurrentValueUpdate(){
		if(!m_currentValueUpdateTimer.isRunning()){
			m_currentValueUpdateTimer.start();
		}
	}
	
	public void stopCurrentValueUpdate(){
		if(m_currentValueUpdateTimer.isRunning()){
			m_currentValueUpdateTimer.stop();
		}
	}
	
}
