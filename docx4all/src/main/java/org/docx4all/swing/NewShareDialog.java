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

import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/**
 *	@author Jojada Tirtowidjojo - 01/05/2008
 */
public class NewShareDialog extends JDialog implements PropertyChangeListener {
	//====================
	//ACTION COMMAND NAMES
	//====================
	//These names are used for detecting answers given by user to dialog panel.
	private final static String GROUP_ON_PARAGRAPH_ACTION_COMMAND = "groupOnParagraph";
	private final static String GROUP_ON_HEADING1_ACTION_COMMAND = "groupOnHeading1";
	private final static String COMMENT_ON_EVERY_CHANGE_ACTION_COMMAND = "commentOnEveryChange";
	
    public final static String NEXT_BUTTON_TEXT = "Next";
    public final static String CANCEL_BUTTON_TEXT = "Cancel";
    
    private JOptionPane optionPane;
    
    private ButtonGroup contentGroupOptions;
    private JCheckBox commentOnEveryChange;
    
    private Object value;
    
	public NewShareDialog(Frame owner) {
		super(owner, true);
		this.value = CANCEL_BUTTON_TEXT;
		setTitle("New Share");
		
		JPanel saveOptionsPanel = createAuditTrailOptionsPanel();
		
		Object[] options = {NEXT_BUTTON_TEXT, CANCEL_BUTTON_TEXT};
		Object[] array = {saveOptionsPanel};
		this.optionPane = 
			new JOptionPane(
				  array,
                  JOptionPane.PLAIN_MESSAGE,
                  JOptionPane.YES_NO_OPTION,
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
				NewShareDialog.this.optionPane.setValue(new Integer(JOptionPane.CLOSED_OPTION));
			}
		});
        
        //Register an event handler that reacts to option pane state changes.
        this.optionPane.addPropertyChangeListener(this);
	}
	
	public boolean isCommentOnEveryChange() {
		return commentOnEveryChange.isSelected();
	}
	
    public Object getValue() {
    	return this.value;
    }
    
	 /** This method reacts to state changes in the option pane. */
    public void propertyChange(PropertyChangeEvent e) {
        String prop = e.getPropertyName();

        if (isVisible()
        	&& (e.getSource() == this.optionPane)
        	&& (JOptionPane.VALUE_PROPERTY.equals(prop) 
        		|| JOptionPane.INPUT_VALUE_PROPERTY.equals(prop))) {
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
            
            if (NEXT_BUTTON_TEXT.equals(userAnswer)) {
            	this.value = NEXT_BUTTON_TEXT;
            } else {
            	//User closed dialog or clicked cancel
            	this.value = CANCEL_BUTTON_TEXT;
            }
        }
    }

	private JPanel createAuditTrailOptionsPanel() {
		//Audit Trail Options Panel with its check box
		JPanel thePanel = new JPanel();
		thePanel.setBorder(BorderFactory.createTitledBorder("Audit Trail"));
		thePanel.setLayout(new BoxLayout(thePanel, BoxLayout.Y_AXIS));
		
		this.commentOnEveryChange = new JCheckBox("Comment on every change");
		this.commentOnEveryChange.setActionCommand(COMMENT_ON_EVERY_CHANGE_ACTION_COMMAND);
		
		thePanel.add(this.commentOnEveryChange);
		return thePanel;
	}
	
}// NewShareDialog class



















