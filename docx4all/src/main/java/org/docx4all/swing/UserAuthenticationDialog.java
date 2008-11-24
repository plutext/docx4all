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

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.docx4all.ui.main.WordMLEditor;

/**
 *	@author Jojada Tirtowidjojo - 24/11/2008
 */
public class UserAuthenticationDialog extends JDialog implements PropertyChangeListener {
    public final static String OK_BUTTON_TEXT = "OK";
    public final static String CANCEL_BUTTON_TEXT = "Cancel";
    
    private JOptionPane optionPane;
    
    private JLabel urlLabel;
    private JLabel usernameLabel;
    private JLabel passwordLabel;
    private JTextField usernameTextField;
    private JPasswordField passwordTextField;
    
    private String url;
    private Object value;

	public UserAuthenticationDialog(WordMLEditor wmlEditor, String url) {
		super(wmlEditor.getWindowFrame(), true);
		this.url = url;
		this.value = CANCEL_BUTTON_TEXT;
		setTitle("User Authentication");
		
		Object[] options = {OK_BUTTON_TEXT, CANCEL_BUTTON_TEXT};
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
				UserAuthenticationDialog.this.optionPane.setValue(new Integer(JOptionPane.CLOSED_OPTION));
			}
		});
        
        //Register an event handler that reacts to option pane state changes.
        this.optionPane.addPropertyChangeListener(this);
	}
	
    public Object getValue() {
    	return this.value;
    }
    
    public String getUsername() {
    	return usernameTextField.getText().trim();
    }
    
    public String getPassword() {
    	return new String(passwordTextField.getPassword());
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
            
            if (OK_BUTTON_TEXT.equals(userAnswer)) {
            	this.value = OK_BUTTON_TEXT;
            } else {
            	//User closed dialog or clicked cancel
            	this.value = CANCEL_BUTTON_TEXT;
            }
        }
    }
    
	private JPanel createContentPanel() {
		// Description Panel with its all radio buttons
		JPanel thePanel = new JPanel(new GridBagLayout());
        thePanel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
		
		this.urlLabel = new JLabel("Location");
		JTextArea urlField = new JTextArea(url);
		urlField.setEditable(false);
		urlField.setEnabled(false);
		urlField.setLineWrap(true);
				
        this.usernameLabel = new JLabel("User Name");
        this.usernameTextField = new JTextField(20);
        
        this.passwordLabel = new JLabel("Password");
        this.passwordTextField = new JPasswordField(12);

        fillGrid(thePanel, new Component[] {
        		urlLabel, urlField,
        		usernameLabel, usernameTextField,
        		passwordLabel, passwordTextField
        });
        
        return thePanel;
	}

    private void fillGrid(JPanel host, Component[] components) {
        final Insets insets = new Insets(2, 2, 2, 2);
        final GridBagConstraints gbc = new GridBagConstraints();

        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.insets = insets;
        gbc.ipadx = 0;
        gbc.ipady = 0;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int i = 0;
        int j = 0;

        for (Component component : components)
        {
            gbc.gridx = i;
            gbc.gridy = j;
            gbc.gridwidth = 1;
            gbc.gridheight = 1;

            host.add(component, gbc);

            i++;

            // 2 components per row
            if ((i % 2) == 0)
            {
                j++;
                i = 0;
            }
        }
    }

}// UserAuthenticationDialog class



















