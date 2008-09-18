/*
 *  Copyright 2007, Plutext Pty Ltd.
 *   
 *  This file is part of Docx4all.

    Docx4all is free software: you can redistribute it and/or modify
    it under the terms of version 3 of the GNU General Public License 
    as published by the Free Software Foundation.

    Docx4all is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License   
    along with Docx4all.  If not, see <http://www.gnu.org/licenses/>.
    
 */

package org.docx4all.swing;

import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.font.TextLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 *	@author Jojada Tirtowidjojo - 10/09/2008
 */
public class ProgressBarDialog extends JDialog implements PropertyChangeListener {
    private JLabel message;
    private JPanel extraMessage;
    private JProgressBar progressBar;
    private JButton ok_button;
    private Object endResult;
    
	public ProgressBarDialog(Frame owner, String title) {
		super(owner, true);
		setTitle(title);
				
	    //Make this dialog display it.
		JPanel content = createContentPanel();
        setContentPane(content);

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);        
	}

	 /** This method reacts to state changes in the option pane. */
    public void propertyChange(PropertyChangeEvent e) {
        if (isVisible()) {
        	if ("progress".equals(e.getPropertyName())) {
                final Integer progress = (Integer) e.getNewValue();
                progressBar.setValue(progress.intValue());
                
               	IProgressBarWorker worker = (IProgressBarWorker) e.getSource();
               	String s = worker.getProgressMessage(progress);
               	message.setText(s);
        		message.invalidate();
        		
                if (progress == progressBar.getMaximum()) {
                	setVisible(false);
                	setModalityType(java.awt.Dialog.ModalityType.APPLICATION_MODAL);
                	ok_button.setEnabled(true);
                	
                	if (worker.getInsertedEndMessage() != null) {
                		JComponent c = worker.getInsertedEndMessage();
                		extraMessage.setVisible(true);
                		extraMessage.add(c);
                		extraMessage.invalidate();
                	}
                }       
                
                validate();
                pack();
               	setVisible(true);                		                		
                               	
        	} else if ("endResult".equals(e.getPropertyName())) {
        		endResult = e.getNewValue();
        	}
        }
    }

    public Object getEndResult() {
    	return this.endResult;
    }
    
	private JPanel createContentPanel() {
		// Description Panel with its all radio buttons
		JPanel thePanel = new JPanel();
		thePanel.setLayout(new BoxLayout(thePanel, BoxLayout.PAGE_AXIS));
		
		message = new JLabel();
		Font font = message.getFont().deriveFont(Font.BOLD, 16);
		font = font.deriveFont(Font.ITALIC);
		message.setFont(font);
		message.setText("Initialising...");
		message.setAlignmentX(CENTER_ALIGNMENT);
		
		extraMessage = new JPanel();
		extraMessage.setVisible(false);
		//extraMessage.setEnabled(false);
		
		progressBar = new JProgressBar(0, 100);
        progressBar.setValue(0);
        progressBar.setStringPainted(true);
        progressBar.setAlignmentX(CENTER_ALIGNMENT);
        progressBar.setPreferredSize(new Dimension(400, 30));
        
        ok_button = new JButton("OK");
        ok_button.setEnabled(false);
        ok_button.setAlignmentX(CENTER_ALIGNMENT);
        
        ok_button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	setVisible(false);
            }
        });
        
		JPanel progressPanel = new JPanel();
		progressPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
		progressPanel.setComponentOrientation(ComponentOrientation.LEFT_TO_RIGHT);
        progressPanel.add(Box.createRigidArea(new Dimension(20, 30)));
        progressPanel.add(progressBar);
        progressPanel.add(Box.createRigidArea(new Dimension(20, 30)));
       
		thePanel.add(Box.createRigidArea(new Dimension(20, 20)));
		thePanel.add(message);
		thePanel.add(Box.createRigidArea(new Dimension(20, 10)));
		thePanel.add(extraMessage);
		thePanel.add(Box.createRigidArea(new Dimension(20, 10)));
		thePanel.add(progressPanel);
		thePanel.add(Box.createRigidArea(new Dimension(20, 20)));
		thePanel.add(ok_button);
		thePanel.add(Box.createRigidArea(new Dimension(20, 20)));
		
		return thePanel;
	}

}// ProgressBarDialog class



















