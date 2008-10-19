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
import javax.swing.text.Element;

import org.apache.log4j.Logger;
import org.docx4all.swing.WordMLTextPane;
import org.docx4all.swing.text.WordMLDocument;
import org.docx4all.ui.main.ToolBarStates;
import org.docx4all.ui.main.WordMLEditor;
import org.docx4all.ui.menu.enabler.CurrentEditorBasedEnabler;
import org.jdesktop.application.Action;
import org.jdesktop.application.ResourceMap;

/**
 *	@author Jojada Tirtowidjojo - 27/02/2008
 */
public class ViewMenu extends UIMenu {
	private static Logger log = Logger.getLogger(ViewMenu.class);
	
	private final static ViewMenu _instance = new ViewMenu();
	
	/**
	 * The binding key used for this ViewMenu object 
	 * when passed into scripting environment
	 */
	public final static String SCRIPT_BINDING_KEY = "viewMenu:org.docx4all.ui.menu.ViewMenu";
	
	//==========
	//MENU Names
	//==========
	//Used as an argument to JMenu.setName().
	//Therefore it can be used in .properties file 
	//to configure View Menu property in the menu bar
	/**
	 * The name of JMenu object that hosts View menu in the menu bar
	 */
	public final static String VIEW_MENU_NAME = "viewMenu";
	
	//============
	//ACTION Names
	//============
	//The string value of each action name must be the same as 
	//the method name annotated by @Action tag.
	//Action name is used to configure Menu/Button Action property
	//in .properties file and get an Action object out of 
	//Swing Application Framework
	
	/**
	 * The action name of View Source menu
	 */
	public final static String VIEW_SOURCE_ACTION_NAME = "viewSource";
	
	public final static String VIEW_EDITOR_ACTION_NAME = "viewEditor";
	
	public final static String VIEW_VERSION_HISTORY_ACTION_NAME = "viewVersionHistory";
	
	public final static String VIEW_RECENT_CHANGES_ACTION_NAME = "viewRecentChanges";
	
	public final static String CLOSE_SOURCE_VIEW_ACTION_NAME = "closeSourceView";
	
	public final static String CLOSE_VERSION_HISTORY_VIEW_ACTION_NAME = "closeVersionHistoryView";
	
	public final static String CLOSE_RECENT_CHANGES_VIEW_ACTION_NAME = "closeRecentChangesView";
	
	public final static String APPLY_FILTER_ACTION_NAME = "applyFilter";
	
	private static final String[] _menuItemActionNames = {
		VIEW_EDITOR_ACTION_NAME,
		VIEW_SOURCE_ACTION_NAME,
		VIEW_VERSION_HISTORY_ACTION_NAME,
		VIEW_RECENT_CHANGES_ACTION_NAME,
		SEPARATOR_CODE,
		CLOSE_SOURCE_VIEW_ACTION_NAME,
		CLOSE_VERSION_HISTORY_VIEW_ACTION_NAME,
		CLOSE_RECENT_CHANGES_VIEW_ACTION_NAME,
		SEPARATOR_CODE,
		APPLY_FILTER_ACTION_NAME
	};
	
	public static ViewMenu getInstance() {
		return _instance;
	}
	
	private ViewMenu() {
		;//singleton
	}
	
	public String[] getMenuItemActionNames() {
		String[] names = new String[_menuItemActionNames.length];
		System.arraycopy(_menuItemActionNames, 0, names, 0, _menuItemActionNames.length);
		return names;
	}

	public String getMenuName() {
		return VIEW_MENU_NAME;
	}
	
	@Action public void viewSource() {
        WordMLEditor editor = WordMLEditor.getInstance(WordMLEditor.class);
        editor.showViewInTab(editor.getSourceViewTabTitle());
	}
	
	@Action public void viewEditor() {
        WordMLEditor editor = WordMLEditor.getInstance(WordMLEditor.class);
        editor.showViewInTab(editor.getEditorViewTabTitle());
	}

	@Action public void viewVersionHistory() {
        WordMLEditor editor = WordMLEditor.getInstance(WordMLEditor.class);
        WordMLTextPane editorView =
        	(WordMLTextPane) editor.getView(editor.getEditorViewTabTitle());
        if (editorView == null) {
        	return;
        }
        
        int pos = editorView.getCaretPosition();
        WordMLDocument doc = (WordMLDocument) editorView.getDocument();
        Element sdt = null;
        try {
        	doc.readLock();
        	
        	sdt = doc.getSdtBlockMLElement(pos);
        } finally {
        	doc.readUnlock();
        }
        if (sdt != null) {
        	String title = editor.getVersionHistoryViewTabTitle();
        	editor.showViewInTab(title);
        } else {
	    	ResourceMap rm = editor.getContext().getResourceMap(getClass());
	        String title = 
	        	rm.getString(
	        		ViewMenu.VIEW_VERSION_HISTORY_ACTION_NAME + ".Action.text");
	        String message =
	        	rm.getString(
	        		ViewMenu.VIEW_VERSION_HISTORY_ACTION_NAME + ".Action.selectSdtMessage");
	    	editor.showMessageDialog(title, message, JOptionPane.INFORMATION_MESSAGE);
        }
	}
	
	@Action public void viewRecentChanges() {
        WordMLEditor editor = WordMLEditor.getInstance(WordMLEditor.class);
        editor.showViewInTab(editor.getRecentChangesViewTabTitle());
	}
	
	@Action public void closeSourceView() {
        WordMLEditor editor = WordMLEditor.getInstance(WordMLEditor.class);
        editor.closeViewTab(editor.getSourceViewTabTitle());
	}
	
	@Action public void closeVersionHistoryView() {
        WordMLEditor editor = WordMLEditor.getInstance(WordMLEditor.class);
        editor.closeViewTab(editor.getVersionHistoryViewTabTitle());
	}
	
	@Action public void closeRecentChangesView() {
        WordMLEditor editor = WordMLEditor.getInstance(WordMLEditor.class);
        editor.closeViewTab(editor.getRecentChangesViewTabTitle());
	}
	
	@Action public void applyFilter(ActionEvent evt) {
        WordMLEditor editor = WordMLEditor.getInstance(WordMLEditor.class);
        
        JEditorPane view = editor.getCurrentEditor();
        if (view instanceof WordMLTextPane) {
        	WordMLTextPane tp = (WordMLTextPane) view;
        	tp.applyFilter();
        	editor.getToolbarStates().setFilterApplied(tp.isFilterApplied());
        }		
	}
	
	public void setEnabled(String menuItemActionName, boolean isEnabled) {
		javax.swing.Action itemAction = getAction(menuItemActionName);
		itemAction.setEnabled(isEnabled);
	}

    protected JMenuItem createMenuItem(String actionName) {
		JMenuItem theItem = super.createMenuItem(actionName);
		
        WordMLEditor editor = WordMLEditor.getInstance(WordMLEditor.class);
        ToolBarStates toolbarStates = editor.getToolbarStates();
        
        if (VIEW_EDITOR_ACTION_NAME.equals(actionName)) {
        	theItem.setEnabled(false);
        	toolbarStates.addPropertyChangeListener(
    			ToolBarStates.CURRENT_EDITOR_PROPERTY_NAME,
    			new OpenViewEnabler(theItem, editor.getEditorViewTabTitle()));
   	
        } else if (VIEW_SOURCE_ACTION_NAME.equals(actionName)) {
    		theItem.setEnabled(false);
    		toolbarStates.addPropertyChangeListener(
    			ToolBarStates.CURRENT_EDITOR_PROPERTY_NAME,
    			new OpenViewEnabler(theItem, editor.getSourceViewTabTitle()));
        	
        } else if (VIEW_VERSION_HISTORY_ACTION_NAME.equals(actionName)) {
    		theItem.setEnabled(false);
    		toolbarStates.addPropertyChangeListener(
    			ToolBarStates.CURRENT_EDITOR_PROPERTY_NAME,
    			new OpenViewEnabler(theItem, editor.getVersionHistoryViewTabTitle()));
        	
        } else if (VIEW_RECENT_CHANGES_ACTION_NAME.equals(actionName)) {
    		theItem.setEnabled(false);
    		toolbarStates.addPropertyChangeListener(
    			ToolBarStates.CURRENT_EDITOR_PROPERTY_NAME,
    			new OpenViewEnabler(theItem, editor.getRecentChangesViewTabTitle()));
        	
        } else if (CLOSE_SOURCE_VIEW_ACTION_NAME.equals(actionName)) {
    		theItem.setEnabled(false);
    		toolbarStates.addPropertyChangeListener(
        			ToolBarStates.CURRENT_EDITOR_PROPERTY_NAME,
        			new CloseViewEnabler(theItem, editor.getSourceViewTabTitle()));
    		
        } else if (CLOSE_VERSION_HISTORY_VIEW_ACTION_NAME.equals(actionName)) {
    		theItem.setEnabled(false);
    		toolbarStates.addPropertyChangeListener(
        			ToolBarStates.CURRENT_EDITOR_PROPERTY_NAME,
        			new CloseViewEnabler(theItem, editor.getVersionHistoryViewTabTitle()));
    		
        } else if (CLOSE_RECENT_CHANGES_VIEW_ACTION_NAME.equals(actionName)) {
    		theItem.setEnabled(false);
    		toolbarStates.addPropertyChangeListener(
        			ToolBarStates.CURRENT_EDITOR_PROPERTY_NAME,
        			new CloseViewEnabler(theItem, editor.getRecentChangesViewTabTitle()));
    		
        } else if (APPLY_FILTER_ACTION_NAME.equals(actionName)) {
    		theItem.setEnabled(false);
    		toolbarStates.addPropertyChangeListener(
        			ToolBarStates.FILTER_APPLIED_PROPERTY_NAME,
        			new EnableOnEqual(theItem, Boolean.FALSE));
        }
        
		return theItem;
    }
    
    private static class OpenViewEnabler extends CurrentEditorBasedEnabler {
    	protected String _openedViewTabTitle;
    	
    	OpenViewEnabler(JMenuItem item, String openedViewTabTitle) {
    		super(item);
    		_openedViewTabTitle = openedViewTabTitle;
    	}
    	
    	protected boolean isMenuEnabled(JEditorPane currentEditor) {
            WordMLEditor editor = WordMLEditor.getInstance(WordMLEditor.class);
            if (currentEditor == null
            	|| currentEditor == editor.getView(_openedViewTabTitle)) {
            	return false;
            }
            
            boolean isEnabled = true;
            if (editor.getRecentChangesViewTabTitle().equals(_openedViewTabTitle)
            	|| editor.getVersionHistoryViewTabTitle().equals(_openedViewTabTitle)) {
        		WordMLTextPane editorView = 
        			(WordMLTextPane) editor.getView(editor.getEditorViewTabTitle());
               	isEnabled = 
               			editorView.getWordMLEditorKit().getPlutextClient() != null;
            }
            return isEnabled;
    	}
    } //OpenViewEnabler inner class
    
    private static class CloseViewEnabler extends CurrentEditorBasedEnabler {
    	protected String _closedViewTabTitle;
    	
    	CloseViewEnabler(JMenuItem item, String closedViewTabTitle) {
    		super(item);
    		_closedViewTabTitle = closedViewTabTitle;
    	}
    	
    	protected boolean isMenuEnabled(JEditorPane currentEditor) {
            WordMLEditor editor = WordMLEditor.getInstance(WordMLEditor.class);
            if (editor.getView(_closedViewTabTitle) != null) {
            	return true;
            }
            return false;            
    	}
    } //CloseViewEnabler inner class

}// ViewMenu class

















































