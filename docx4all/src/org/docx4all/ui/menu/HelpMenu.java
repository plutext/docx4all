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

import javax.swing.JOptionPane;

import org.docx4all.ui.main.WordMLEditor;
import org.jdesktop.application.Action;
import org.jdesktop.application.ResourceMap;

/**
 *	@author Jojada Tirtowidjojo - 30/11/2007
 */
public class HelpMenu extends UIMenu {
	private final static HelpMenu _instance = new HelpMenu();
	
	//==========
	//MENU Names
	//==========
	//Used as an argument to JMenu.setName().
	//Therefore it can be used in .properties file 
	//to configure Help Menu property in the menu bar
	/**
	 * The name of JMenu object that hosts Help menu in the menu bar
	 */
	public final static String HELP_MENU_NAME = "helpMenu";
	
	
	//============
	//ACTION Names
	//============
	//The string value of each action name must be the same as 
	//the method name annotated by @Action tag.
	//Action name is used to configure Menu/Button Action property
	//in .properties file and get an Action object out of 
	//Spring Application Framework
	
	/**
	 * The action name of About Help menu
	 */
	public final static String HELP_ABOUT_ACTION_NAME = "helpAbout";

	private static final String[] _menuItemActionNames = {
		HELP_ABOUT_ACTION_NAME
	};
	
	public static HelpMenu getInstance() {
		return _instance;
	}
	
	private HelpMenu() {
		;//singleton
	}
	
	public String[] getMenuItemActionNames() {
		String[] names = new String[_menuItemActionNames.length];
		System.arraycopy(_menuItemActionNames, 0, names, 0, _menuItemActionNames.length);
		return names;
	}
	
	public String getMenuName() {
		return HELP_MENU_NAME;
	}
	
	@Action public void helpAbout() {
        WordMLEditor editor = WordMLEditor.getInstance(WordMLEditor.class);
    	ResourceMap rm = editor.getContext().getResourceMap(HelpMenu.class);
    	String title = rm.getString(HELP_ABOUT_ACTION_NAME + ".Action.text");
    	if (title == null || title.length() == 0) {
    		title = "About Info";
    	}
    	
    	String appTitle = rm.getString("Application.title");
    	if (appTitle == null || appTitle.length() == 0) {
    		appTitle = "docx4all";
    	}
    	
    	String message = 
    		appTitle 
    		+ "\nAlpha Version\n(c)2007 Copyright Plutext Pty. Ltd.; Australia";
        editor.showMessageDialog(title, message, JOptionPane.INFORMATION_MESSAGE);
	}

}// HelpMenu class



















