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

import javax.swing.JEditorPane;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import org.docx4all.swing.text.WordMLEditorKit;
import org.docx4all.ui.main.ToolBarStates;
import org.docx4all.ui.main.WordMLEditor;
import org.jdesktop.application.Action;
import org.jdesktop.application.ResourceMap;

/**
 *	@author Jojada Tirtowidjojo - 26/06/2008
 */
public class ReviewMenu extends UIMenu {
	private final static ReviewMenu _instance = new ReviewMenu();
	
	/**
	 * The binding key used for this ReviewMenu object 
	 * when passed into scripting environment
	 */
	public final static String SCRIPT_BINDING_KEY = "reviewMenu:org.docx4all.ui.menu.ReviewMenu";
	
	//==========
	//MENU Names
	//==========
	//Used as an argument to JMenu.setName().
	//Therefore it can be used in .properties file 
	//to configure Review Menu property in the menu bar
	/**
	 * The name of JMenu object that hosts Review menu in the menu bar
	 */
	public final static String REVIEW_MENU_NAME = "reviewMenu";
	
	
	//============
	//ACTION Names
	//============
	//The string value of each action name must be the same as 
	//the method name annotated by @Action tag.
	//Action name is used to configure Menu/Button Action property
	//in .properties file and get an Action object out of 
	//Swing Application Framework
	
	/**
	 * The action name of 'Accept Revision' Review menu
	 */
	public final static String ACCEPT_REVISION_ACTION_NAME = "acceptRevision";
	
	/**
	 * The action name of 'Accept Revision and Move Next' Review menu
	 */
	public final static String ACCEPT_REVISION_MOVE_NEXT_ACTION_NAME = "acceptRevisionMoveNext";
	
	/**
	 * The action name of 'Reject Revision' Review menu
	 */
	public final static String REJECT_REVISION_ACTION_NAME = "rejectRevision";
	
	/**
	 * The action name of 'Reject Revision and Move Next' Review menu
	 */
	public final static String REJECT_REVISION_MOVE_NEXT_ACTION_NAME = "rejectRevisionMoveNext";
	
	/**
	 * The action name of 'Apply Remote Revisions in Current Paragraph' Review menu
	 */
	public final static String APPLY_REMOTE_REVISIONS_IN_CURRENT_PARA_ACTION_NAME = "applyRemoteRevisionsInCurrentPara";

	/**
	 * The action name of 'Apply All Remote Revisions' Review menu
	 */
	public final static String APPLY_ALL_REMOTE_REVISIONS_ACTION_NAME = "applyAllRemoteRevisions";
	
	/**
	 * The action name of 'Discard Remote Revisions in Current Paragraph' Review menu
	 */
	public final static String DISCARD_REMOTE_REVISIONS_IN_CURRENT_PARA_ACTION_NAME = "discardRemoteRevisionsInCurrentPara";
	
	/**
	 * The action name of 'Discard All Remote Revisions' Review menu
	 */
	public final static String DISCARD_ALL_REMOTE_REVISIONS_ACTION_NAME = "discardAllRemoteRevisions";
	
	/**
	 * The action name of 'Goto Next Revision'  Review menu
	 */
	public final static String GOTO_NEXT_REVISION_ACTION_NAME = "gotoNextRevision";

	/**
	 * The action name of 'Goto Previous Revision'  Review menu
	 */
	public final static String GOTO_PREV_REVISION_ACTION_NAME = "gotoPrevRevision";

	
	private static final String[] _menuItemActionNames = {
		ACCEPT_REVISION_MOVE_NEXT_ACTION_NAME,
		ACCEPT_REVISION_ACTION_NAME,
		REJECT_REVISION_MOVE_NEXT_ACTION_NAME,
		REJECT_REVISION_ACTION_NAME,
		SEPARATOR_CODE,
		APPLY_REMOTE_REVISIONS_IN_CURRENT_PARA_ACTION_NAME,
		APPLY_ALL_REMOTE_REVISIONS_ACTION_NAME,
		DISCARD_REMOTE_REVISIONS_IN_CURRENT_PARA_ACTION_NAME,
		DISCARD_ALL_REMOTE_REVISIONS_ACTION_NAME,
		SEPARATOR_CODE,
		GOTO_NEXT_REVISION_ACTION_NAME,
		GOTO_PREV_REVISION_ACTION_NAME
	};
	
	public static ReviewMenu getInstance() {
		return _instance;
	}
	
	private ReviewMenu() {
		;//singleton
	}
	
	public String[] getMenuItemActionNames() {
		String[] names = new String[_menuItemActionNames.length];
		System.arraycopy(_menuItemActionNames, 0, names, 0, _menuItemActionNames.length);
		return names;
	}
	
	public String getMenuName() {
		return REVIEW_MENU_NAME;
	}
	
    protected JMenuItem createMenuItem(String actionName) {
    	JMenuItem theItem = super.createMenuItem(actionName);
    	
		WordMLEditor editor = WordMLEditor.getInstance(WordMLEditor.class);
		ToolBarStates toolbarStates = editor.getToolbarStates();
    	if (ACCEPT_REVISION_MOVE_NEXT_ACTION_NAME.equals(actionName)
    		|| ACCEPT_REVISION_ACTION_NAME.equals(actionName)
    		|| REJECT_REVISION_MOVE_NEXT_ACTION_NAME.equals(actionName)
    		|| REJECT_REVISION_ACTION_NAME.equals(actionName)) {
    		theItem.setEnabled(toolbarStates.isRevisionSelected());
			toolbarStates.addPropertyChangeListener(
				ToolBarStates.REVISION_SELECTED_PROPERTY_NAME, 
				new EnableOnEqual(theItem, Boolean.TRUE));
			
    	} else if (APPLY_REMOTE_REVISIONS_IN_CURRENT_PARA_ACTION_NAME.equals(actionName)
    			|| DISCARD_REMOTE_REVISIONS_IN_CURRENT_PARA_ACTION_NAME.equals(actionName)) {
    		theItem.setEnabled(toolbarStates.isRemoteRevisionInPara());
			toolbarStates.addPropertyChangeListener(
				ToolBarStates.REMOTE_REVISION_IN_PARA_PROPERTY_NAME, 
				new EnableOnEqual(theItem, Boolean.TRUE));
			
		} else if (APPLY_ALL_REMOTE_REVISIONS_ACTION_NAME.equals(actionName)
				|| DISCARD_ALL_REMOTE_REVISIONS_ACTION_NAME.equals(actionName)) {
    		theItem.setEnabled(toolbarStates.isRemoteRevisionInDoc());
			toolbarStates.addPropertyChangeListener(
				ToolBarStates.REMOTE_REVISION_IN_DOC_PROPERTY_NAME, 
				new EnableOnEqual(theItem, Boolean.TRUE));
			
		}
    	
    	return theItem;
    }
    
	@Action public void acceptRevision(ActionEvent evt) {
		WordMLEditorKit.AcceptRevisionAction action = 
			new WordMLEditorKit.AcceptRevisionAction();
		action.actionPerformed(evt);
		
		if (!action.success()) {
			WordMLEditor wmlEditor = WordMLEditor.getInstance(WordMLEditor.class);
        	ResourceMap rm = wmlEditor.getContext().getResourceMap(getClass());
            String title = 
            	rm.getString(ACCEPT_REVISION_ACTION_NAME + ".Action.text");
            String message = null;
            
    		JEditorPane pane = wmlEditor.getCurrentEditor();
    		if (pane.getSelectionStart() == pane.getSelectionEnd()) {
    			message = 
    				rm.getString(
    					ACCEPT_REVISION_ACTION_NAME + ".Action.noSelectionMessage");
    		} else {
    			message = 
    				rm.getString(
    					ACCEPT_REVISION_ACTION_NAME + ".Action.wrongSelectionMessage");
    		}
			wmlEditor.showMessageDialog(title, message, JOptionPane.INFORMATION_MESSAGE);	
		}
	}
	
	@Action public void acceptRevisionMoveNext(ActionEvent evt) {
		WordMLEditorKit.AcceptRevisionAction action = 
			new WordMLEditorKit.AcceptRevisionAction();
		action.actionPerformed(evt);
		gotoNextRevision(evt);
	}
	
	@Action public void rejectRevision(ActionEvent evt) {
		WordMLEditorKit.RejectRevisionAction action = 
			new WordMLEditorKit.RejectRevisionAction();
		action.actionPerformed(evt);
		
		if (!action.success()) {
			WordMLEditor wmlEditor = WordMLEditor.getInstance(WordMLEditor.class);
        	ResourceMap rm = wmlEditor.getContext().getResourceMap(getClass());
            String title = 
            	rm.getString(REJECT_REVISION_ACTION_NAME + ".Action.text");
            String message = null;
            
    		JEditorPane pane = wmlEditor.getCurrentEditor();
    		if (pane.getSelectionStart() == pane.getSelectionEnd()) {
    			message = 
    				rm.getString(
    					REJECT_REVISION_ACTION_NAME + ".Action.noSelectionMessage");
    		} else {
    			message = 
    				rm.getString(
    					REJECT_REVISION_ACTION_NAME + ".Action.wrongSelectionMessage");
    		}
			wmlEditor.showMessageDialog(title, message, JOptionPane.INFORMATION_MESSAGE);	
		}
	}
	
	@Action public void rejectRevisionMoveNext(ActionEvent evt) {
		WordMLEditorKit.RejectRevisionAction action = 
			new WordMLEditorKit.RejectRevisionAction();
		action.actionPerformed(evt);
		gotoNextRevision(evt);
	}
	
	@Action public void applyRemoteRevisionsInCurrentPara(ActionEvent evt) {
		WordMLEditorKit.ApplyRemoteRevisionsInParaAction action = 
			new WordMLEditorKit.ApplyRemoteRevisionsInParaAction();
		action.actionPerformed(evt);
	}
	
	@Action public void applyAllRemoteRevisions(ActionEvent evt) {
		WordMLEditorKit.ApplyAllRemoteRevisionsAction action = 
			new WordMLEditorKit.ApplyAllRemoteRevisionsAction();
		action.actionPerformed(evt);
	}
	
	@Action public void discardRemoteRevisionsInCurrentPara(ActionEvent evt) {
		WordMLEditorKit.DiscardRemoteRevisionsInParaAction action = 
			new WordMLEditorKit.DiscardRemoteRevisionsInParaAction();
		action.actionPerformed(evt);
	}
	
	@Action public void discardAllRemoteRevisions(ActionEvent evt) {
		WordMLEditorKit.DiscardAllRemoteRevisionsAction action = 
			new WordMLEditorKit.DiscardAllRemoteRevisionsAction();
		action.actionPerformed(evt);
	}
	
	@Action public void gotoNextRevision(ActionEvent evt) {
		WordMLEditorKit.SelectNextRevisionAction action = 
			new WordMLEditorKit.SelectNextRevisionAction();
		action.actionPerformed(evt);
		
		WordMLEditor wmlEditor = WordMLEditor.getInstance(WordMLEditor.class);
		JEditorPane pane = wmlEditor.getCurrentEditor();
		if (pane.getSelectionStart() == pane.getSelectionEnd()) {
        	ResourceMap rm = wmlEditor.getContext().getResourceMap(getClass());

            String title = 
            	rm.getString(GOTO_NEXT_REVISION_ACTION_NAME + ".Action.text");
			String message = 
				rm.getString(GOTO_NEXT_REVISION_ACTION_NAME + ".Action.notFoundMessage");
			wmlEditor.showMessageDialog(title, message, JOptionPane.INFORMATION_MESSAGE);	
		}
	}

	@Action public void gotoPrevRevision(ActionEvent evt) {		
		WordMLEditorKit.SelectPrevRevisionAction action = 
			new WordMLEditorKit.SelectPrevRevisionAction();
		action.actionPerformed(evt);
		
		WordMLEditor wmlEditor = WordMLEditor.getInstance(WordMLEditor.class);
		JEditorPane pane = wmlEditor.getCurrentEditor();
		if (pane.getSelectionStart() == pane.getSelectionEnd()) {
        	ResourceMap rm = wmlEditor.getContext().getResourceMap(getClass());

            String title = 
            	rm.getString(GOTO_PREV_REVISION_ACTION_NAME + ".Action.text");
			String message = 
				rm.getString(GOTO_PREV_REVISION_ACTION_NAME + ".Action.notFoundMessage");
			wmlEditor.showMessageDialog(title, message, JOptionPane.INFORMATION_MESSAGE);	
		}
	}

}// ReviewMenu class



















