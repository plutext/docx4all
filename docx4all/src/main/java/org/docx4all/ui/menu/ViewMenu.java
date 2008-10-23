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
import javax.swing.event.CaretEvent;

import org.apache.log4j.Logger;
import org.docx4all.swing.WordMLTextPane;
import org.docx4all.ui.main.ToolBarStates;
import org.docx4all.ui.main.WordMLEditor;
import org.docx4all.ui.menu.enabler.CaretUpdateEnabler;
import org.docx4all.ui.menu.enabler.CurrentEditorBasedEnabler;
import org.jdesktop.application.Action;

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
	
	public final static String CLOSE_SOURCE_VIEW_ACTION_NAME = "closeSourceView";
	
	public final static String APPLY_FILTER_ACTION_NAME = "applyFilter";
	
	private static final String[] _menuItemActionNames = {
		VIEW_EDITOR_ACTION_NAME,
		VIEW_SOURCE_ACTION_NAME,
		SEPARATOR_CODE,
		CLOSE_SOURCE_VIEW_ACTION_NAME,
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

	@Action public void closeSourceView() {
        WordMLEditor editor = WordMLEditor.getInstance(WordMLEditor.class);
        editor.closeViewTab(editor.getSourceViewTabTitle());
	}
	
	@Action public void applyFilter(ActionEvent evt) {
        WordMLEditor editor = WordMLEditor.getInstance(WordMLEditor.class);
        
        JEditorPane view = editor.getCurrentEditor();
        if (view instanceof WordMLTextPane) {
        	WordMLTextPane tp = (WordMLTextPane) view;
        	tp.applyFilter();
        	tp.getDocument().addDocumentListener(editor.getToolbarStates());
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
        	
        } else if (CLOSE_SOURCE_VIEW_ACTION_NAME.equals(actionName)) {
    		theItem.setEnabled(false);
    		toolbarStates.addPropertyChangeListener(
        			ToolBarStates.CURRENT_EDITOR_PROPERTY_NAME,
        			new CloseViewEnabler(theItem, editor.getSourceViewTabTitle()));
    		
        } else if (APPLY_FILTER_ACTION_NAME.equals(actionName)) {
    		theItem.setEnabled(false);
    		toolbarStates.addPropertyChangeListener(
        			ToolBarStates.CARET_UPDATE_PROPERTY_NAME,
        			new ApplyFilterEnabler(theItem));
        }
        
		return theItem;
    }
    
    static class OpenViewEnabler extends CurrentEditorBasedEnabler {
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
            	|| editor.getContentControlHistoryViewTabTitle().equals(_openedViewTabTitle)) {
        		WordMLTextPane editorView = 
        			(WordMLTextPane) editor.getView(editor.getEditorViewTabTitle());
               	isEnabled = 
               			editorView.getWordMLEditorKit().getPlutextClient() != null;
            }
            return isEnabled;
    	}
    } //OpenViewEnabler inner class
    
    static class CloseViewEnabler extends CurrentEditorBasedEnabler {
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

    private static class ApplyFilterEnabler extends CaretUpdateEnabler {
    	ApplyFilterEnabler(JMenuItem item) {
    		super(item);
    	}
    	
    	protected boolean isMenuEnabled(CaretEvent caretEvent) {
    		boolean isEnabled = false;
    		if (caretEvent != null) {
    			WordMLEditor wmlEditor = WordMLEditor.getInstance(WordMLEditor.class);
    			JEditorPane source = (JEditorPane) caretEvent.getSource();
    			if (source != null
    					&& source == wmlEditor.getView(wmlEditor.getEditorViewTabTitle())) {
    				isEnabled = !((WordMLTextPane) source).isFilterApplied();
    			}
    		} //if (caretEvent != null)
    		return isEnabled;
    	} //isMenuEnabled()
    	
    } //ApplyFilterEnabler inner class

}// ViewMenu class

















































