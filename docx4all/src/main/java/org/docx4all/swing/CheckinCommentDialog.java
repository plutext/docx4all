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

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 *	@author Jojada Tirtowidjojo - 05/09/2008
 */
public class CheckinCommentDialog extends JDialog implements PropertyChangeListener {
    public final static String OK_BUTTON_TEXT = "OK";

    private JOptionPane optionPane;

    private JTextArea comment;
    
	public CheckinCommentDialog(Frame owner) {
		super(owner, true);
		setTitle("Comment On Changes");
		
		Object[] options = {OK_BUTTON_TEXT};
		Object[] array = {createContentPanel()};
		this.optionPane = 
			new JOptionPane(
				  array,
                  JOptionPane.PLAIN_MESSAGE,
                  JOptionPane.YES_OPTION,
                  null,
                  options,
                  options[0]);
		
	    //Make this dialog display it.
        setContentPane(this.optionPane);

        //Handle window closing correctly.
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent we) {
				/*
				 * Instead of directly closing the window, we're going to change
				 * the JOptionPane's value property.
				 */
				CheckinCommentDialog.this.optionPane.setValue(new Integer(JOptionPane.CLOSED_OPTION));
			}
		});
        
        //Register an event handler that reacts to option pane state changes.
        this.optionPane.addPropertyChangeListener(this);
	}

	public String getTextComment() {
		return comment.getText();
	}
	
	 /** This method reacts to state changes in the option pane. */
    public void propertyChange(PropertyChangeEvent e) {
        String prop = e.getPropertyName();

        if (isVisible()
        	&& (e.getSource() == this.optionPane)
        	&& (JOptionPane.VALUE_PROPERTY.equals(prop))) {

        	Object userAnswer = this.optionPane.getValue();

            if (userAnswer == JOptionPane.UNINITIALIZED_VALUE) {
                //ignore reset
                return;
            }

            //Reset the JOptionPane's value.
            //If you don't do this, then if the user
            //presses the same button next time, no
            //property change event will be fired.
            this.optionPane.setValue(
                    JOptionPane.UNINITIALIZED_VALUE);

            setVisible(false);
            
        }
    }

	private JPanel createContentPanel() {
		// Description Panel with its all radio buttons
		JPanel thePanel = new JPanel();
		thePanel.setBorder(BorderFactory.createTitledBorder("Description"));

		comment = new JTextArea();
		comment.setLineWrap(true);
		comment.setWrapStyleWord(true);
		
		JScrollPane sp = new JScrollPane(comment);
		sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		sp.setPreferredSize(new Dimension(500, 200));

		thePanel.add(sp);
		
		return thePanel;
	}

}// InputMessageDialog class



















