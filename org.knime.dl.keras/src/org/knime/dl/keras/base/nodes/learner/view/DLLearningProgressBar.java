package org.knime.dl.keras.base.nodes.learner.view;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;

public class DLLearningProgressBar extends JPanel{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private final JProgressBar m_progressBar;
	private final JLabel m_progressCounter;
	private final JLabel m_timeCounter;
	
	private static final String PROGRESS_COUNTER_FORMAT = "%d / %d";
	
	public DLLearningProgressBar(final String progressLabel, final String timeLabel) {
		super(new GridBagLayout());
		
		m_progressBar = new JProgressBar();
		m_progressBar.setPreferredSize(new Dimension(100, 30));
		
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(5, 5, 5, 5);
		gbc.gridx = 0;
		gbc.gridy = 0;
        gbc.weightx = 0.8;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(m_progressBar, gbc);
        
        m_progressCounter = new JLabel();
        m_progressCounter.setText(String.format(PROGRESS_COUNTER_FORMAT, 0, 0));
        JPanel progressCounterBox = new JPanel(new GridLayout(0, 1));
        progressCounterBox.setBorder(BorderFactory.createTitledBorder(progressLabel + ":"));
        progressCounterBox.add(m_progressCounter);
        progressCounterBox.setPreferredSize(new Dimension(100, 40));
        progressCounterBox.setMinimumSize(new Dimension(100, 40));
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.weightx = 0.1;
        gbc.gridx++;
        add(progressCounterBox, gbc);
        
        m_timeCounter = new JLabel();
        m_timeCounter.setText("0");
        JPanel timeCounterBox = new JPanel(new GridLayout(0, 1));
        timeCounterBox.setBorder(BorderFactory.createTitledBorder(timeLabel + ":"));
        timeCounterBox.add(m_timeCounter);
        timeCounterBox.setPreferredSize(new Dimension(100, 40));
        timeCounterBox.setMinimumSize(new Dimension(100, 40));
        gbc.gridx++;
        add(timeCounterBox, gbc);
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
	
	public void setMaxProgress(int maxProgress){
		m_progressBar.setMaximum(maxProgress);
	}

}
