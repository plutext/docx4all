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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.docx4all.ui.main.WordMLEditor;

/**
 *	@author Jojada Tirtowidjojo - 01/05/2008
 */
public class ContentGroupingDialog extends JDialog 
	implements PropertyChangeListener, ActionListener {
	//====================
	//ACTION COMMAND NAMES
	//====================
	//These names are used for detecting answers given by user to dialog panel.
	private final static String AT_EACH_PARAGRAPH_ACTION_COMMAND = "atEachParagraph";
	private final static String AT_STYLES_ACTION_COMMAND = "atStyles";
	private final static String AT_SIGNED_PARAGRAPH_ACTION_COMMAND = "atSignedParagraph";
	
    public final static String OK_BUTTON_TEXT = "OK";
    public final static String CANCEL_BUTTON_TEXT = "Cancel";
    
    private JOptionPane optionPane;
    
    private ButtonGroup contentGroupOptions;
    
    private List<JCheckBox> definedStyles;
    private JCheckBox mergeSingleParagraphs;
    
    private Object value;
    
	public ContentGroupingDialog(WordMLEditor wmlEditor, List<String> definedStyles) {
		super(wmlEditor.getWindowFrame(), true);
		
		this.definedStyles = new ArrayList<JCheckBox>(definedStyles.size());
		for (String style:definedStyles) {
			JCheckBox box = new JCheckBox(style);
			box.setEnabled(false);
			this.definedStyles.add(box);
		}

		this.value = CANCEL_BUTTON_TEXT;
		setTitle("Setup Units");
		
		JPanel contentGroupPanel = createContentGroupPanel(wmlEditor);
		
		Object[] options = {OK_BUTTON_TEXT, CANCEL_BUTTON_TEXT};
		Object[] array = {contentGroupPanel};
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
				ContentGroupingDialog.this.optionPane.setValue(new Integer(JOptionPane.CLOSED_OPTION));
			}
		});
        
        //Register an event handler that reacts to option pane state changes.
        this.optionPane.addPropertyChangeListener(this);
	}
	
	public boolean isGroupOnEachParagraph() {
		return (AT_EACH_PARAGRAPH_ACTION_COMMAND 
			== contentGroupOptions.getSelection().getActionCommand());
	}
	
	public boolean isGroupOnStyles() {
		return (AT_STYLES_ACTION_COMMAND 
			== contentGroupOptions.getSelection().getActionCommand());
	}
	
	public boolean isGroupOnSignedParagraphs() {
		return (AT_SIGNED_PARAGRAPH_ACTION_COMMAND 
			== contentGroupOptions.getSelection().getActionCommand());
	}
	
	public boolean isMergeSingleParagraphs() {
		return mergeSingleParagraphs.isSelected();
	}
	
	public List<String> getSelectedStyles() {
		List<String> theList = null;
		
		if (isGroupOnStyles()) {
			theList = new ArrayList<String>(this.definedStyles.size());
			for (JCheckBox box: this.definedStyles) {
				if (box.isSelected()) {
					theList.add(box.getText());
				}
			}
		}
		
		return theList;
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
            
            if (OK_BUTTON_TEXT.equals(userAnswer)) {
            	this.value = OK_BUTTON_TEXT;
            } else {
            	//User closed dialog or clicked cancel
            	this.value = CANCEL_BUTTON_TEXT;
            }
        }
    }

    /** 
     * This method reacts to state changes in one of radio buttons. 
     */
    public void actionPerformed(ActionEvent e) {
    	AbstractButton b = (AbstractButton) e.getSource();
    	boolean enabled = false;
    	if (b.getActionCommand() == AT_STYLES_ACTION_COMMAND) {
    		enabled = b.isSelected();
    	}
    	
		for (JCheckBox box: this.definedStyles) {
			box.setEnabled(enabled);
			this.mergeSingleParagraphs.setEnabled(enabled);
		}
    }
        
	private JPanel createContentGroupPanel(WordMLEditor wmlEditor) {
		// Content Groups Panel with its all radio buttons
		JPanel thePanel = new JPanel();
		thePanel.setBorder(BorderFactory.createTitledBorder("Collaboration Granularity"));
		thePanel.setAlignmentX(LEFT_ALIGNMENT);
		thePanel.setLayout(new BoxLayout(thePanel, BoxLayout.Y_AXIS));
		
		JLabel startLabel = new JLabel("Create Control Units:");
		startLabel.setAlignmentX(LEFT_ALIGNMENT);
		
		JRadioButton atEachParaRadio = new JRadioButton("at each paragraph");
		atEachParaRadio.setAlignmentX(LEFT_ALIGNMENT);
		atEachParaRadio.setActionCommand(AT_EACH_PARAGRAPH_ACTION_COMMAND);
		atEachParaRadio.addActionListener(this);
		
		JRadioButton atStylesRadio = new JRadioButton("at Styles");
		atStylesRadio.setAlignmentX(LEFT_ALIGNMENT);
		atStylesRadio.setActionCommand(AT_STYLES_ACTION_COMMAND);
		atStylesRadio.addActionListener(this);
		
		JRadioButton atSignedParaRadio = new JRadioButton("at paragraphs starting with \"<<\"");
		atSignedParaRadio.setAlignmentX(LEFT_ALIGNMENT);
		atSignedParaRadio.setActionCommand(AT_SIGNED_PARAGRAPH_ACTION_COMMAND);
		atSignedParaRadio.addActionListener(this);
		
		this.contentGroupOptions = new ButtonGroup();
		this.contentGroupOptions.add(atEachParaRadio);
		this.contentGroupOptions.add(atStylesRadio);
		this.contentGroupOptions.add(atSignedParaRadio);
		
		atEachParaRadio.setSelected(true);
		
		thePanel.add(Box.createVerticalStrut(15));
		thePanel.add(startLabel);
		thePanel.add(Box.createVerticalStrut(10));
		thePanel.add(atEachParaRadio);
		thePanel.add(Box.createVerticalStrut(10));
		thePanel.add(atStylesRadio);
		thePanel.add(createStylesSubPanel(wmlEditor));
		thePanel.add(Box.createVerticalStrut(10));
		thePanel.add(atSignedParaRadio);
		
		return thePanel;
	}
	
	private JPanel createStylesSubPanel(WordMLEditor wmlEditor) {
		JPanel listPanel = new JPanel();
		Color bg = listPanel.getBackground().brighter().brighter();
		
		listPanel.setAlignmentX(LEFT_ALIGNMENT);
		listPanel.setBorder(BorderFactory.createLineBorder(Color.black));
		listPanel.setBackground(bg);
		listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
		
		for (JCheckBox style:this.definedStyles) {
			style.setAlignmentX(LEFT_ALIGNMENT);
			style.setBackground(bg);
			style.setMinimumSize(new Dimension(300, 25));
			style.setPreferredSize(new Dimension(300, 25));
			style.setMaximumSize(new Dimension(300, 25));
			listPanel.add(style);
		}
		
		mergeSingleParagraphs = new JCheckBox("Merge single paragraphs into next group");
		mergeSingleParagraphs.setAlignmentX(LEFT_ALIGNMENT);
		mergeSingleParagraphs.setEnabled(false);
		
		//listPanel and mergeSingleParagraphs check box
		//are contained in one right panel.
		JPanel rightPanel = new JPanel();
		rightPanel.setAlignmentX(LEFT_ALIGNMENT);
		rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
		
		rightPanel.add(listPanel);
		rightPanel.add(Box.createVerticalStrut(5));
		rightPanel.add(mergeSingleParagraphs);
		
		//The returned panel 
		JPanel thePanel = new JPanel();
		thePanel.setAlignmentX(LEFT_ALIGNMENT);
		thePanel.setLayout(new BoxLayout(thePanel, BoxLayout.X_AXIS));
		
		//The returned panel consists of a tab, rightPanel, and right margin.
		thePanel.add(Box.createHorizontalStrut(50));
		thePanel.add(rightPanel);
		thePanel.add(Box.createHorizontalStrut(20));
		
		return thePanel;
	}
	
}// ContentGroupingDialog class



















