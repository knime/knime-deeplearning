package org.knime.dl.keras.base.nodes.learner.view;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

public class DLLearningProgressBar extends JPanel{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private final JProgressBar m_progressBar;
	private final JLabel m_progressCounter;
	private final JLabel m_timeCounter;
	
	private static final String PROGRESS_COUNTER_FORMAT = "%d / %d";
	
	public DLLearningProgressBar(final int maxProgress, final String progressLabel, final String timeLabel) {
		super(new GridBagLayout());
		
		m_progressBar = new JProgressBar(0, maxProgress);
		m_progressBar.setPreferredSize(new Dimension(0, 20));
		
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(5, 5, 5, 50);
		gbc.gridx = 0;
		gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(m_progressBar, gbc);
        
        m_progressCounter = new JLabel("- / -");
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.weightx = 0;
        gbc.gridx++;
        add(m_progressCounter, gbc);
        
        gbc.gridx++;
        gbc.insets = new Insets(5, 5, 5, 20);
        add(new JLabel(progressLabel), gbc);
        
        gbc.gridx++;
        gbc.insets = new Insets(5, 20, 5, 20);
        add(new JLabel("|"));
        
        m_timeCounter = new JLabel("-");
        gbc.gridx++;
        add(m_timeCounter, gbc);
        
        gbc.gridx++;
        gbc.insets = new Insets(5, 5, 5, 50);
        add(new JLabel(timeLabel), gbc);
	}
	
	public void setProgress(int progress){
		m_progressBar.setValue(progress);
	}
	
	public void setProgressText(int current, int max) {
		m_progressCounter.setText(String.format(PROGRESS_COUNTER_FORMAT, current, max));
	}
	
	public void setTime(int timeInSec) {
		m_timeCounter.setText(timeInSec + "");
	}

}
