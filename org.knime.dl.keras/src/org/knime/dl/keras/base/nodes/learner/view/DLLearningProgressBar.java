package org.knime.dl.keras.base.nodes.learner.view;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

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
	private final JTextField m_progressCounter;
	private final JTextField m_timeCounter;
	
	private static final String PROGRESS_COUNTER_FORMAT = "%d / %d";
	
	public DLLearningProgressBar(final String progressLabel, final String timeLabel) {
		super(new GridBagLayout());
		
		m_progressBar = new JProgressBar();
		m_progressBar.setPreferredSize(new Dimension(100, 20));
		
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(5, 5, 5, 5);
		gbc.gridx = 0;
		gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(m_progressBar, gbc);
        
        m_progressCounter = new JTextField(8);
        m_progressCounter.setEnabled(false);
        m_progressCounter.setText(String.format(PROGRESS_COUNTER_FORMAT, 0, 0));
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.weightx = 0;
        gbc.gridx++;
        add(m_progressCounter, gbc);
        
        gbc.gridx++;
        gbc.insets = new Insets(5, 5, 5, 5);
        JLabel pLabel = new JLabel(progressLabel);
        pLabel.setPreferredSize(new Dimension(100, 20));
        add(pLabel, gbc);
        
        m_timeCounter = new JTextField(8);
        m_timeCounter.setEnabled(false);
        m_timeCounter.setText("0");
        gbc.gridx++;
        add(m_timeCounter, gbc);
        
        gbc.gridx++;
        gbc.insets = new Insets(5, 5, 5, 50);
        JLabel tLabel = new JLabel(timeLabel);
        tLabel.setPreferredSize(new Dimension(100, 20));
        add(tLabel, gbc);
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
