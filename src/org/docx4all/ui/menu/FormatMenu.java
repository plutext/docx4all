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

package org.docx4all.ui.menu;

import java.awt.event.ActionEvent;

import javax.swing.ButtonGroup;
import javax.swing.JMenuItem;
import javax.swing.JToggleButton;
import javax.swing.text.StyleConstants;

import org.docx4all.swing.text.WordMLEditorKit;
import org.docx4all.swing.text.WordMLEditorKit.StyledTextAction;
import org.docx4all.ui.main.ToolBarStates;
import org.docx4all.ui.main.WordMLEditor;
import org.jdesktop.application.Action;

/**
 *	@author Jojada Tirtowidjojo - 28/11/2007
 */
public class FormatMenu extends UIMenu {
	private final static FormatMenu _instance = new FormatMenu();
	
	/**
	 * The binding key used for this FormatMenu object 
	 * when passed into scripting environment
	 */
	public final static String SCRIPT_BINDING_KEY = "formatMenu:org.docx4all.ui.menu.FormatMenu";
	
	//==========
	//MENU Names
	//==========
	//Used as an argument to JMenu.setName().
	//Therefore it can be used in .properties file 
	//to configure Format Menu property in the menu bar
	/**
	 * The name of JMenu object that hosts Format menu in the menu bar
	 */
	public final static String FORMAT_MENU_NAME = "formatMenu";
	
	
	//============
	//ACTION Names
	//============
	//The string value of each action name must be the same as 
	//the method name annotated by @Action tag.
	//Action name is used to configure Menu/Button Action property
	//in .properties file and get an Action object out of 
	//Swing Application Framework
	
	/**
	 * The action name of Align Left Format menu
	 */
	public final static String ALIGN_LEFT_ACTION_NAME = "alignLeft";
	
	/**
	 * The action name of Align Center  Format menu
	 */
	public final static String ALIGN_CENTER_ACTION_NAME = "alignCenter";
	
	/**
	 * The action name of Align Right Format menu
	 */
	public final static String ALIGN_RIGHT_ACTION_NAME = "alignRight";
	
	/**
	 * The action name of Align BOTH Format menu
	 */
	public final static String ALIGN_JUSTIFIED_ACTION_NAME = "alignJustified";
	
	/**
	 * The action name of Bold Font toobar button
	 */
	public final static String BOLD_ACTION_NAME = "bold";
	
	/**
	 * The action name of Font Format menu
	 */
	public final static String FONT_ACTION_NAME = "font";
	
	/**
	 * The action name of Font Family Format menu
	 */
	public final static String FONT_FAMILY_ACTION_NAME = "fontFamily";
	
	/**
	 * The action name of Font Size Format menu
	 */
	public final static String FONT_SIZE_ACTION_NAME = "fontSize";
	
	/**
	 * The action name of Italic Font toobar button
	 */
	public final static String ITALIC_ACTION_NAME = "italic";
	
	/**
	 * The action name of Paragraph Style Format menu
	 */
	public final static String APPLY_STYLE_ACTION_NAME = "applyStyle";
	
	/**
	 * The action name of Underline Font toobar button
	 */
	public final static String UNDERLINE_ACTION_NAME = "underline";
	
	private static final String[] _menuItemActionNames = {
		FONT_ACTION_NAME,
		SEPARATOR_CODE,
		ALIGN_LEFT_ACTION_NAME,
		ALIGN_CENTER_ACTION_NAME,
		ALIGN_RIGHT_ACTION_NAME,
		ALIGN_JUSTIFIED_ACTION_NAME
	};
	
	private final ButtonGroup _alignmentButtonGroup = new ButtonGroup();  
	
	public static FormatMenu getInstance() {
		return _instance;
	}
	
	private FormatMenu() {
		;//singleton
	}
	
	public String[] getMenuItemActionNames() {
		String[] names = new String[_menuItemActionNames.length];
		System.arraycopy(_menuItemActionNames, 0, names, 0, _menuItemActionNames.length);
		return names;
	}
	
	public String getMenuName() {
		return FORMAT_MENU_NAME;
	}
	
    protected JMenuItem createMenuItem(String actionName) {
    	JMenuItem theItem = super.createMenuItem(actionName);
    	
        WordMLEditor editor = WordMLEditor.getInstance(WordMLEditor.class);
        ToolBarStates toolbarStates = editor.getToolbarStates();
        
    	if (ALIGN_LEFT_ACTION_NAME.equals(actionName)
    		|| ALIGN_CENTER_ACTION_NAME.equals(actionName)
    		|| ALIGN_RIGHT_ACTION_NAME.equals(actionName)
    		|| ALIGN_JUSTIFIED_ACTION_NAME.equals(actionName)) {
    		theItem = new JMenuItem();
    		theItem.setAction(getAction(actionName));
    		_alignmentButtonGroup.add(theItem);
    		
    		MenuItemStateManager listener = new MenuItemStateManager(theItem);
    		toolbarStates.addPropertyChangeListener(
    				ToolBarStates.ALIGNMENT_PROPERTY_NAME, 
    				listener);
    	}
    	
    	return theItem;
    }
    
	@Action public void font() {
		//TODO:Font Menu Dialog
	}
	
	@Action public void bold(ActionEvent evt) {
		JToggleButton btn = (JToggleButton) evt.getSource();
		boolean bold = btn.isSelected();
		StyledTextAction action = new WordMLEditorKit.BoldAction(bold);
		action.actionPerformed(evt);
	}
	
	@Action public void italic(ActionEvent evt) {
		JToggleButton btn = (JToggleButton) evt.getSource();
		boolean italic = btn.isSelected();
		StyledTextAction action = new WordMLEditorKit.ItalicAction(italic);
		action.actionPerformed(evt);
	}
	
	@Action public void underline(ActionEvent evt) {
		JToggleButton btn = (JToggleButton) evt.getSource();
		boolean underline = btn.isSelected();
		StyledTextAction action = new WordMLEditorKit.UnderlineAction(underline);
		action.actionPerformed(evt);
	}
	
	@Action public void alignLeft(ActionEvent evt) {
		StyledTextAction action = 
			new WordMLEditorKit.AlignmentAction(
				ALIGN_LEFT_ACTION_NAME, 
				StyleConstants.ALIGN_LEFT);
		action.actionPerformed(evt);
	}
	
	@Action public void alignCenter(ActionEvent evt) {
		StyledTextAction action = 
			new WordMLEditorKit.AlignmentAction(
				ALIGN_CENTER_ACTION_NAME, 
				StyleConstants.ALIGN_CENTER);
		action.actionPerformed(evt);
	}
	
	@Action public void alignRight(ActionEvent evt) {
		StyledTextAction action = 
			new WordMLEditorKit.AlignmentAction(
				ALIGN_RIGHT_ACTION_NAME,
				StyleConstants.ALIGN_RIGHT);
		action.actionPerformed(evt);
	}
	
	@Action public void alignJustified(ActionEvent evt) {
		StyledTextAction action = 
			new WordMLEditorKit.AlignmentAction(
				ALIGN_JUSTIFIED_ACTION_NAME,
				StyleConstants.ALIGN_JUSTIFIED);
		action.actionPerformed(evt);
	}
	
	@Action public void applyStyle(ActionEvent actionEvent) {
		StyledTextAction action = 
			new WordMLEditorKit.ApplyStyleAction(
				APPLY_STYLE_ACTION_NAME, actionEvent.getActionCommand());
		action.actionPerformed(actionEvent);
	}
	
	@Action public void fontFamily(ActionEvent actionEvent) {
        StyledTextAction action =
			new WordMLEditorKit.FontFamilyAction(
				FONT_FAMILY_ACTION_NAME, actionEvent.getActionCommand());
		action.actionPerformed(actionEvent); 
	}
	
	@Action public void fontSize(ActionEvent actionEvent) {
        StyledTextAction action =
			new WordMLEditorKit.FontSizeAction(
				FONT_SIZE_ACTION_NAME, Integer.parseInt(actionEvent.getActionCommand()));
		action.actionPerformed(actionEvent);
	}
	
}// FormatMenu class



















