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

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import org.docx4all.swing.WordMLTextPane;
import org.docx4all.swing.text.WordMLEditorKit;
import org.docx4all.ui.main.ToolBarStates;
import org.docx4all.ui.main.WordMLEditor;
import org.jdesktop.application.Action;
import org.jdesktop.application.ResourceMap;
import org.plutext.client.ClientException;

/**
 * @author Jojada Tirtowidjojo - 10/07/2008
 */
public class TeamMenu extends UIMenu {
	private final static TeamMenu _instance = new TeamMenu();

	/**
	 * The binding key used for this TeamMenu object when passed into
	 * scripting environment
	 */
	public final static String SCRIPT_BINDING_KEY = "teamMenu:org.docx4all.ui.menu.TeamMenu";

	// ==========
	// MENU Names
	// ==========
	// Used as an argument to JMenu.setName().
	// Therefore it can be used in .properties file
	// to configure Team Menu property in the menu bar
	/**
	 * The name of JMenu object that hosts Team menu in the menu bar
	 */
	public final static String TEAM_MENU_NAME = "teamMenu";

	// ============
	// ACTION Names
	// ============
	// The string value of each action name must be the same as
	// the method name annotated by @Action tag.
	// Action name is used to configure Menu/Button Action property
	// in .properties file and get an Action object out of
	// Swing Application Framework

	/**
	 * The action name of 'Fetch Remote Edits' Team menu
	 */
	public final static String FETCH_REMOTE_EDITS_ACTION_NAME = "fetchRemoteEdits";

	/**
	 * The action name of 'Commit Local Edits' Team menu
	 */
	public final static String COMMIT_LOCAL_EDITS_ACTION_NAME = "commitLocalEdits";

	private static final String[] _menuItemActionNames = {
		FETCH_REMOTE_EDITS_ACTION_NAME,
		COMMIT_LOCAL_EDITS_ACTION_NAME
	};
	
	public static TeamMenu getInstance() {
		return _instance;
	}
	
	private TeamMenu() {
		;//singleton
	}
	
	public String[] getMenuItemActionNames() {
		String[] names = new String[_menuItemActionNames.length];
		System.arraycopy(_menuItemActionNames, 0, names, 0, _menuItemActionNames.length);
		return names;
	}
	
	public String getMenuName() {
		return TEAM_MENU_NAME;
	}

    protected JMenuItem createMenuItem(String actionName) {
    	JMenuItem theItem = super.createMenuItem(actionName);
    	
		WordMLEditor editor = WordMLEditor.getInstance(WordMLEditor.class);
		ToolBarStates toolbarStates = editor.getToolbarStates();
    	if (FETCH_REMOTE_EDITS_ACTION_NAME.equals(actionName)) {
    		theItem.setEnabled(false);
    		toolbarStates.addPropertyChangeListener(
    				ToolBarStates.DOC_SHARED_PROPERTY_NAME, 
    				new EnableOnEqual(theItem, Boolean.TRUE));
    		
    	} else if (COMMIT_LOCAL_EDITS_ACTION_NAME.equals(actionName)) {
    		theItem.setEnabled(false);
    		toolbarStates.addPropertyChangeListener(
    				ToolBarStates.LOCAL_EDITS_ENABLED_PROPERTY_NAME, 
    				new EnableOnEqual(theItem, Boolean.TRUE));
    	}
		
		
		return theItem;
    }
    
	@Action public void fetchRemoteEdits(ActionEvent evt) {
		WordMLEditorKit.FetchRemoteEditsAction action = 
			new WordMLEditorKit.FetchRemoteEditsAction();
		action.actionPerformed(evt);
		
		if (!action.success()) {
            Exception exc = action.getThrownException();
            exc.printStackTrace();
            
			WordMLEditor wmlEditor = WordMLEditor.getInstance(WordMLEditor.class);
        	ResourceMap rm = wmlEditor.getContext().getResourceMap(getClass());
            String title = 
            	rm.getString(FETCH_REMOTE_EDITS_ACTION_NAME + ".Action.text");
            
            String message = rm.getString(
            		FETCH_REMOTE_EDITS_ACTION_NAME + ".Action.errorMessage");
			wmlEditor.showMessageDialog(title, message, JOptionPane.INFORMATION_MESSAGE);	
		}
	}
	
	@Action public void commitLocalEdits(ActionEvent evt) {
		WordMLEditorKit.CommitLocalEditsAction action = 
			new WordMLEditorKit.CommitLocalEditsAction();
		action.actionPerformed(evt);
		
		WordMLEditor wmlEditor = WordMLEditor.getInstance(WordMLEditor.class);
		if (action.success()) {
			if (evt.getSource() instanceof WordMLTextPane) {
				WordMLTextPane textpane = (WordMLTextPane) evt.getSource();
				wmlEditor.getToolbarStates().setLocalEditsEnabled(textpane, false);
			} else if (wmlEditor.getCurrentEditor() instanceof WordMLTextPane) {
				WordMLTextPane textpane = (WordMLTextPane) wmlEditor.getCurrentEditor();
				wmlEditor.getToolbarStates().setLocalEditsEnabled(textpane, false);
			}
		} else {
        	ResourceMap rm = wmlEditor.getContext().getResourceMap(getClass());
            String title = 
            	rm.getString(COMMIT_LOCAL_EDITS_ACTION_NAME + ".Action.text");
            
            Exception exc = action.getThrownException();
            
            String message = null;
            if (exc instanceof ClientException) {
            	message = 
            		rm.getString(
                		COMMIT_LOCAL_EDITS_ACTION_NAME + ".Action.fetchFirstMessage");
            } else {
                exc.printStackTrace();
            	message = 
            		rm.getString(
            				COMMIT_LOCAL_EDITS_ACTION_NAME + ".Action.errorMessage");
            }
			wmlEditor.showMessageDialog(title, message, JOptionPane.INFORMATION_MESSAGE);	
		}
	}
	
}// TeamMenu class


























