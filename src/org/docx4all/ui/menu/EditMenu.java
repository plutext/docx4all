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

import org.jdesktop.application.Action;

/**
 *	@author Jojada Tirtowidjojo - 28/11/2007
 */
public class EditMenu extends UIMenu {
	private final static EditMenu _instance = new EditMenu();
	
	/**
	 * The binding key used for this EditMenu object 
	 * when passed into scripting environment
	 */
	public final static String SCRIPT_BINDING_KEY = "editMenu:org.docx4all.ui.menu.EditMenu";
	
	//==========
	//MENU Names
	//==========
	//Used as an argument to JMenu.setName().
	//Therefore it can be used in .properties file 
	//to configure Edit Menu property in the menu bar
	/**
	 * The name of JMenu object that hosts Edit menu in the menu bar
	 */
	public final static String EDIT_MENU_NAME = "editMenu";
	
	
	//============
	//ACTION Names
	//============
	//The string value of each action name must be the same as 
	//the method name annotated by @Action tag.
	//Action name is used to configure Menu/Button Action property
	//in .properties file and get an Action object out of 
	//Spring Application Framework
	
	/**
	 * The action name of Cut edit menu
	 */
	public final static String CUT_ACTION_NAME = "cut";
	
	/**
	 * The action name of Copy edit menu
	 */
	public final static String COPY_ACTION_NAME = "copy";
	
	/**
	 * The action name of Paste edit menu
	 */
	public final static String PASTE_ACTION_NAME = "paste";
	
	private static final String[] _menuItemActionNames = {
		CUT_ACTION_NAME,
		COPY_ACTION_NAME,
		PASTE_ACTION_NAME
	};
	
	public static EditMenu getInstance() {
		return _instance;
	}
	
	private EditMenu() {
		;//singleton
	}
	
	public String[] getMenuItemActionNames() {
		String[] names = new String[_menuItemActionNames.length];
		System.arraycopy(_menuItemActionNames, 0, names, 0, _menuItemActionNames.length);
		return names;
	}
	
	public String getMenuName() {
		return EDIT_MENU_NAME;
	}
	
	@Action public void cut() {
	}
	
	@Action public void copy() {
	}
	
	@Action public void paste() {
	}
	
}// EditMenu class



















