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
	//Spring Application Framework
	
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
	public final static String PARAGRAPH_STYLE_ACTION_NAME = "paragraphStyle";
	
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
	};
	
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
	
	@Action public void font() {
		//TODO:Font Menu Dialog
	}
	
	@Action public void bold() {
		//TODO:Bold action
	}
	
	@Action public void italic() {
		//TODO:Italic action
	}
	
	@Action public void underline() {
		//TODO:Underline action
		System.out.println("FormatMenu.underline():");
	}
	
	@Action public void alignLeft() {
		//TODO:Align Left paragraph
		System.out.println("FormatMenu.alignLeft():");
	}
	
	@Action public void alignCenter() {
		//TODO:Align Center paragraph
	}
	
	@Action public void alignRight() {
		//TODO:Align Right paragraph
	}
	
	@Action public void paragraphStyle(ActionEvent actionEvent) {
		System.out.println("FormatMenu: paragraphStyle() - source=" + actionEvent.getSource());
		(new Exception()).printStackTrace();
	}
	
	@Action public void fontFamily(ActionEvent actionEvent) {
		System.out.println("FormatMenu: fontFamily() - source=" + actionEvent.getSource());
	}
	
	@Action public void fontSize(ActionEvent actionEvent) {
		System.out.println("FormatMenu: fontSize() - source=" + actionEvent.getSource());
	}
	
}// FormatMenu class



















