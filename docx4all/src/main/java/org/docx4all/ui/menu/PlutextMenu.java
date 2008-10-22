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

import javax.swing.JInternalFrame;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

import net.sf.vfsjfilechooser.utils.VFSUtils;

import org.apache.log4j.Logger;
import org.docx4all.swing.FetchRemoteEditsWorker;
import org.docx4all.swing.ProgressBarDialog;
import org.docx4all.swing.TransmitLocalEditsWorker;
import org.docx4all.swing.WordMLTextPane;
import org.docx4all.swing.text.WordMLDocument;
import org.docx4all.ui.main.ToolBarStates;
import org.docx4all.ui.main.WordMLEditor;
import org.jdesktop.application.Action;
import org.jdesktop.application.ResourceMap;
import org.plutext.client.Mediator;

/**
 * @author Jojada Tirtowidjojo - 10/07/2008
 */
public class PlutextMenu extends UIMenu {
	private static Logger log = Logger.getLogger(PlutextMenu.class);
	
	private final static PlutextMenu _instance = new PlutextMenu();

	/**
	 * The binding key used for this PlutextMenu object when passed into
	 * scripting environment
	 */
	public final static String SCRIPT_BINDING_KEY = "plutextMenu:org.docx4all.ui.menu.PlutextMenu";

	// ==========
	// MENU Names
	// ==========
	// Used as an argument to JMenu.setName().
	// Therefore it can be used in .properties file
	// to configure Team Menu property in the menu bar
	/**
	 * The name of JMenu object that hosts Team menu in the menu bar
	 */
	public final static String PLUTEXT_MENU_NAME = "plutextMenu";

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
	
	public static PlutextMenu getInstance() {
		return _instance;
	}
	
	private PlutextMenu() {
		;//singleton
	}
	
	public String[] getMenuItemActionNames() {
		String[] names = new String[_menuItemActionNames.length];
		System.arraycopy(_menuItemActionNames, 0, names, 0, _menuItemActionNames.length);
		return names;
	}
	
	public String getMenuName() {
		return PLUTEXT_MENU_NAME;
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
    
	@Action public void fetchRemoteEdits() {		
		log.debug("fetchRemoteEdits(): Starting...");
		
		WordMLEditor wmlEditor = WordMLEditor.getInstance(WordMLEditor.class);
        WordMLTextPane textpane = (WordMLTextPane) wmlEditor.getCurrentEditor();
    	if (textpane != null) {
    		Mediator plutextClient = textpane.getWordMLEditorKit().getPlutextClient();
    		if (plutextClient != null) {
            	
            	textpane.saveCaretText();
            	
                String temp = 
                	(String) textpane.getDocument().getProperty(
                				WordMLDocument.FILE_PATH_PROPERTY);
                temp = VFSUtils.getFriendlyName(temp);
                int colon = temp.indexOf(':');
                temp = temp.substring(colon + 1);
                
            	ResourceMap rm = wmlEditor.getContext().getResourceMap(getClass());
                StringBuilder title = new StringBuilder();
                title.append(
                	rm.getString(FETCH_REMOTE_EDITS_ACTION_NAME + ".Action.text")
                );
                title.append("-");
                title.append(temp);
                
    			ProgressBarDialog d = 
    				new ProgressBarDialog(wmlEditor.getWindowFrame(), title.toString());
    			d.setModalityType(java.awt.Dialog.ModalityType.MODELESS);
    			d.pack();
    			d.setLocationRelativeTo(wmlEditor.getWindowFrame());
    			d.setVisible(true);
    			       			
       			FetchRemoteEditsWorker task = 
        			new FetchRemoteEditsWorker(plutextClient, wmlEditor);
                task.addPropertyChangeListener(d);
                task.execute();
    		}
    	}
	}
	
	@Action public boolean commitLocalEdits() {
		WordMLEditor wmlEditor = WordMLEditor.getInstance(WordMLEditor.class);
        WordMLTextPane textpane = (WordMLTextPane) wmlEditor.getCurrentEditor();
		return commitLocalEdits(textpane, COMMIT_LOCAL_EDITS_ACTION_NAME);
	}
	
	boolean commitLocalEdits(WordMLTextPane textpane, String callerActionName) {
		Boolean success = false;
		
		log.debug("commitLocalEdits(): Starting...");
		
    	if (textpane != null) {
    		Mediator plutextClient = textpane.getWordMLEditorKit().getPlutextClient();
    		if (plutextClient != null) {
            	
            	textpane.saveCaretText();
            	
                String temp = 
                	(String) textpane.getDocument().getProperty(
                				WordMLDocument.FILE_PATH_PROPERTY);
                temp = VFSUtils.getFriendlyName(temp);
                int colon = temp.indexOf(':');
                temp = temp.substring(colon + 1);
                
        		WordMLEditor wmlEditor = WordMLEditor.getInstance(WordMLEditor.class);
            	ResourceMap rm = wmlEditor.getContext().getResourceMap(getClass());
                StringBuilder title = new StringBuilder();
                title.append(
                	rm.getString(callerActionName + ".Action.text")
                );
                title.append("-");
                title.append(temp);

    			ProgressBarDialog d = 
    				new ProgressBarDialog(wmlEditor.getWindowFrame(), title.toString());
    			d.pack();
    			d.setLocationRelativeTo(wmlEditor.getWindowFrame());
    			
       			TransmitLocalEditsWorker task = 
        			new TransmitLocalEditsWorker(plutextClient, wmlEditor);
                task.addPropertyChangeListener(d);
                task.execute();
                
    			d.setVisible(true);
    			
    			success = (Boolean) d.getEndResult();
    			if (success) {
        			wmlEditor.getToolbarStates().setDocumentDirty(textpane, false);
    			
        			JInternalFrame iframe = 
        				(JInternalFrame) 
    						SwingUtilities.getAncestorOfClass(
    							JInternalFrame.class, 
    							textpane);
        			wmlEditor.getToolbarStates().setLocalEditsEnabled(iframe, false);
        		}    		
    		}
    	}
    	
    	return success.booleanValue();
 	}
	
}// PlutextMenu class


























