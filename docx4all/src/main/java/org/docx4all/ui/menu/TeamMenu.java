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
import java.util.List;

import javax.swing.JEditorPane;
import javax.swing.JInternalFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.event.CaretEvent;

import net.sf.vfsjfilechooser.utils.VFSUtils;

import org.apache.log4j.Logger;
import org.docx4all.swing.ContentGroupingDialog;
import org.docx4all.swing.FetchRemoteEditsWorker;
import org.docx4all.swing.ProgressBarDialog;
import org.docx4all.swing.TransmitLocalEditsWorker;
import org.docx4all.swing.WordMLTextPane;
import org.docx4all.swing.text.WordMLDocument;
import org.docx4all.swing.text.WordMLEditorKit;
import org.docx4all.ui.main.ToolBarStates;
import org.docx4all.ui.main.WordMLEditor;
import org.docx4all.ui.menu.enabler.CaretUpdateEnabler;
import org.docx4all.util.DocUtil;
import org.jdesktop.application.Action;
import org.jdesktop.application.ResourceMap;
import org.plutext.client.Mediator;

/**
 * @author Jojada Tirtowidjojo - 10/07/2008
 */
public class TeamMenu extends UIMenu {
	private static Logger log = Logger.getLogger(TeamMenu.class);
	
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
	 * The action name of Setup Sdt edit menu
	 */
	public final static String SETUP_SDT_ACTION_NAME = "setupSdt";
	
	/**
	 * The action name of Clean Sdt edit menu
	 */
	public final static String CLEAN_SDT_ACTION_NAME = "cleanSdt";
	
	/**
	 * The action name of Insert Empty Sdt edit menu
	 */
	public final static String INSERT_EMPTY_SDT_ACTION_NAME = "insertEmptySdt";
	
	/**
	 * The action name of Change Into Sdt edit menu
	 */
	public final static String CHANGE_INTO_SDT_ACTION_NAME = "changeIntoSdt";
	
	/**
	 * The action name of Remove Sdt edit menu
	 */
	public final static String REMOVE_SDT_ACTION_NAME = "removeSdt";
	
	/**
	 * The action name of Merge Sdt edit menu
	 */
	public final static String MERGE_SDT_ACTION_NAME = "mergeSdt";
	
	/**
	 * The action name of Split Sdt edit menu
	 */
	public final static String SPLIT_SDT_ACTION_NAME = "splitSdt";
	
	/**
	 * The action name of 'Fetch Remote Edits' Team menu
	 */
	public final static String FETCH_REMOTE_EDITS_ACTION_NAME = "fetchRemoteEdits";

	/**
	 * The action name of 'Commit Local Edits' Team menu
	 */
	public final static String COMMIT_LOCAL_EDITS_ACTION_NAME = "commitLocalEdits";

	private static final String[] _menuItemActionNames = {
		CHANGE_INTO_SDT_ACTION_NAME,
		REMOVE_SDT_ACTION_NAME,
		INSERT_EMPTY_SDT_ACTION_NAME,
		SEPARATOR_CODE,
		MERGE_SDT_ACTION_NAME,
		SPLIT_SDT_ACTION_NAME,
		SEPARATOR_CODE,
		SETUP_SDT_ACTION_NAME,
		CLEAN_SDT_ACTION_NAME,
		SEPARATOR_CODE,
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
		
		if (CHANGE_INTO_SDT_ACTION_NAME.equals(actionName)) {
			theItem.setEnabled(false);
			toolbarStates.addPropertyChangeListener(
				ToolBarStates.CARET_UPDATE_PROPERTY_NAME, 
				new ChangeIntoSdtEnabler(theItem));
		} else if (REMOVE_SDT_ACTION_NAME.equals(actionName)) {
			theItem.setEnabled(false);
			toolbarStates.addPropertyChangeListener(
				ToolBarStates.CARET_UPDATE_PROPERTY_NAME, 
				new RemoveSdtEnabler(theItem));
		} else if (INSERT_EMPTY_SDT_ACTION_NAME.equals(actionName)) {
			theItem.setEnabled(false);
			toolbarStates.addPropertyChangeListener(
				ToolBarStates.CARET_UPDATE_PROPERTY_NAME, 
				new InsertEmptySdtEnabler(theItem));
		} else if (MERGE_SDT_ACTION_NAME.equals(actionName)) {
			theItem.setEnabled(false);
			toolbarStates.addPropertyChangeListener(
				ToolBarStates.CARET_UPDATE_PROPERTY_NAME, 
				new MergeSdtEnabler(theItem));
		} else if (SPLIT_SDT_ACTION_NAME.equals(actionName)) {
			theItem.setEnabled(false);
			toolbarStates.addPropertyChangeListener(
				ToolBarStates.CARET_UPDATE_PROPERTY_NAME, 
				new SplitSdtEnabler(theItem));
		} else if (SETUP_SDT_ACTION_NAME.equals(actionName)) {
			theItem.setEnabled(false);
			toolbarStates.addPropertyChangeListener(
				ToolBarStates.CARET_UPDATE_PROPERTY_NAME, 
				new SetupSdtEnabler(theItem));
		} else if (CLEAN_SDT_ACTION_NAME.equals(actionName)) {
			theItem.setEnabled(false);
			toolbarStates.addPropertyChangeListener(
				ToolBarStates.CARET_UPDATE_PROPERTY_NAME, 
				new CleanSdtEnabler(theItem));
		} else if (FETCH_REMOTE_EDITS_ACTION_NAME.equals(actionName)) {
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
    
	@Action public void changeIntoSdt(ActionEvent evt) {
		WordMLEditorKit.ChangeIntoSdtAction action =
			new WordMLEditorKit.ChangeIntoSdtAction();
		action.actionPerformed(evt);
	}
	
	@Action public void removeSdt(ActionEvent evt) {
		WordMLEditorKit.RemoveSdtAction action =
			new WordMLEditorKit.RemoveSdtAction();
		action.actionPerformed(evt);
	}
	
	@Action public void insertEmptySdt(ActionEvent evt) {
		WordMLEditorKit.InsertEmptySdtAction action =
			new WordMLEditorKit.InsertEmptySdtAction();
		action.actionPerformed(evt);
		if (!action.success()) {
	        WordMLEditor editor = WordMLEditor.getInstance(WordMLEditor.class);        
	        ResourceMap rm = editor.getContext().getResourceMap(getClass());
	        String title = 
	        	rm.getString(INSERT_EMPTY_SDT_ACTION_NAME + ".Action.text");
	        String message =
	        	rm.getString(INSERT_EMPTY_SDT_ACTION_NAME + ".Action.failureMessage");
	    	editor.showMessageDialog(title, message, JOptionPane.INFORMATION_MESSAGE);			
		}
	}
    
	@Action public void mergeSdt(ActionEvent evt) {
		WordMLEditorKit.MergeSdtAction action = 
			new WordMLEditorKit.MergeSdtAction();
		action.actionPerformed(evt);
	}
    
	@Action public void splitSdt(ActionEvent evt) {
		WordMLEditorKit.SplitSdtAction action = 
			new WordMLEditorKit.SplitSdtAction();
		action.actionPerformed(evt);
	}
	
	@Action public void setupSdt(ActionEvent evt) {
        WordMLEditor editor = WordMLEditor.getInstance(WordMLEditor.class);
        ResourceMap rm = editor.getContext().getResourceMap(getClass());
        String title = 
        	rm.getString(SETUP_SDT_ACTION_NAME + ".Action.text");
        
       	WordMLTextPane textpane = (WordMLTextPane) editor.getCurrentEditor();
       	WordMLDocument doc = (WordMLDocument) textpane.getDocument();
		textpane.getWordMLEditorKit().saveCaretText();
			
		ContentGroupingDialog d = 
			new ContentGroupingDialog(
					editor, 
					DocUtil.getDefinedParagraphStyles(doc));
		d.pack();
		d.setLocationRelativeTo(editor.getWindowFrame());
		d.setVisible(true);
			
		if (d.getValue() == ContentGroupingDialog.OK_BUTTON_TEXT) {
			if (d.isGroupOnEachParagraph()) {
				WordMLEditorKit.CreateSdtOnEachParaAction action =
					new WordMLEditorKit.CreateSdtOnEachParaAction();
				action.actionPerformed(evt);
				
			} else if (d.isGroupOnStyles()) {
				List<Integer> positions =
					DocUtil.getOffsetsOfStyledParagraphs(
						doc,
						d.getSelectedStyles());
				if (positions == null) {
					String message =
						rm.getString(SETUP_SDT_ACTION_NAME + ".Action.noStylesMessage");
					editor.showMessageDialog(title, message, JOptionPane.INFORMATION_MESSAGE);
					return;
				}
				WordMLEditorKit.CreateSdtOnStylesAction action =
					new WordMLEditorKit.CreateSdtOnStylesAction(
						positions,
						d.isMergeSingleParagraphs());
				action.actionPerformed(evt);
				
			} else {
				List<Integer> positions = DocUtil.getOffsetsOfParagraphSignature(doc);
				if (positions == null) {
					String message =
						rm.getString(SETUP_SDT_ACTION_NAME + ".Action.noSignatureMessage");
					editor.showMessageDialog(title, message, JOptionPane.INFORMATION_MESSAGE);
					return;
				}
				WordMLEditorKit.CreateSdtOnSignedParaAction action =
					new WordMLEditorKit.CreateSdtOnSignedParaAction(positions, false);
				action.actionPerformed(evt);
			}
		}
	} //setupSdt()
	
	@Action public void cleanSdt() {
        WordMLEditor editor = WordMLEditor.getInstance(WordMLEditor.class);
       	WordMLTextPane textpane = (WordMLTextPane) editor.getCurrentEditor();;
		textpane.getWordMLEditorKit().saveCaretText();
		textpane.cleanControlUnits();
		textpane.getDocument().addDocumentListener(editor.getToolbarStates());
	} //cleanSdt()
	
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
	
    private static class ChangeIntoSdtEnabler extends CaretUpdateEnabler {
    	ChangeIntoSdtEnabler(JMenuItem item) {
    		super(item);
    	}
    	
    	protected boolean isMenuEnabled(CaretEvent caretEvent) {
    		boolean isEnabled = false;
    		if (caretEvent != null) {
    			WordMLEditor wmlEditor = WordMLEditor.getInstance(WordMLEditor.class);
    			JEditorPane source = (JEditorPane) caretEvent.getSource();
    			if (source != null
    				&& source == wmlEditor.getView(wmlEditor.getEditorViewTabTitle())
    				&& ((WordMLTextPane) source).getWordMLEditorKit().getPlutextClient() == null) {
    				WordMLDocument doc = (WordMLDocument) source.getDocument();
    				if (DocUtil.getChunkingStrategy(doc) != null) {
    					int start = Math.min(caretEvent.getDot(), caretEvent.getMark());
    					int end =  Math.max(caretEvent.getDot(), caretEvent.getMark());
    					isEnabled = DocUtil.canChangeIntoSdt(doc, start, (end-start));
    				}
    			}
    		}
    		return isEnabled;
    	}
    } //ChangeIntoSdtEnabler inner class
    
    private static class RemoveSdtEnabler extends CaretUpdateEnabler {
    	RemoveSdtEnabler(JMenuItem item) {
    		super(item);
    	}
    	
    	protected boolean isMenuEnabled(CaretEvent caretEvent) {
    		boolean isEnabled = false;
    		if (caretEvent != null) {
    			WordMLEditor wmlEditor = WordMLEditor.getInstance(WordMLEditor.class);
    			JEditorPane source = (JEditorPane) caretEvent.getSource();
    			if (source != null
    				&& source == wmlEditor.getView(wmlEditor.getEditorViewTabTitle())
    				&& ((WordMLTextPane) source).getWordMLEditorKit().getPlutextClient() == null) {
    				WordMLDocument doc = (WordMLDocument) source.getDocument();
    				int start = Math.min(caretEvent.getDot(), caretEvent.getMark());
    				int end =  Math.max(caretEvent.getDot(), caretEvent.getMark());
    				isEnabled = DocUtil.canRemoveSdt(doc, start, (end-start));
    			}
    		}
    		return isEnabled;
    	}
    } //RemoveSdtEnabler inner class
    
    private static class InsertEmptySdtEnabler extends CaretUpdateEnabler {
    	InsertEmptySdtEnabler(JMenuItem item) {
    		super(item);
    	}
    	
    	protected boolean isMenuEnabled(CaretEvent caretEvent) {
    		boolean isEnabled = false;
    		if (caretEvent != null) {
    			WordMLEditor wmlEditor = WordMLEditor.getInstance(WordMLEditor.class);
    			JEditorPane source = (JEditorPane) caretEvent.getSource();
    			if (source != null
    					&& source == wmlEditor.getView(wmlEditor.getEditorViewTabTitle())) {
    				WordMLDocument doc = (WordMLDocument) source.getDocument();
    				if (DocUtil.getChunkingStrategy(doc) != null) {
    					int dot = caretEvent.getDot();
    					int mark = caretEvent.getMark();
    					isEnabled = (dot == mark); //no selection
    				}
    			}
    		}
    		return isEnabled;
    	}
    } //InsertEmptySdtEnabler inner class
    
    private static class MergeSdtEnabler extends CaretUpdateEnabler {
    	MergeSdtEnabler(JMenuItem item) {
    		super(item);
    	}
    	
    	protected boolean isMenuEnabled(CaretEvent caretEvent) {
    		boolean isEnabled = false;
    		if (caretEvent != null) {
    			WordMLEditor wmlEditor = WordMLEditor.getInstance(WordMLEditor.class);
    			JEditorPane source = (JEditorPane) caretEvent.getSource();
    			if (source != null
    					&& source == wmlEditor.getView(wmlEditor.getEditorViewTabTitle())) {
    				int start = Math.min(caretEvent.getDot(), caretEvent.getMark());
    				int end = Math.max(caretEvent.getDot(), caretEvent.getMark());
    				if (start < end) {
    					//no selection
    					WordMLDocument doc = (WordMLDocument) source.getDocument();
    					isEnabled = DocUtil.canMergeSdt(doc, start, end-start);
    				}
    			}
    		}
    		return isEnabled;
    	}
    } //MergeSdtEnabler inner class

    private static class SplitSdtEnabler extends CaretUpdateEnabler {
    	SplitSdtEnabler(JMenuItem item) {
    		super(item);
    	}
    	
    	protected boolean isMenuEnabled(CaretEvent caretEvent) {
    		boolean isEnabled = false;
    		if (caretEvent != null) {
    			WordMLEditor wmlEditor = WordMLEditor.getInstance(WordMLEditor.class);
    			JEditorPane source = (JEditorPane) caretEvent.getSource();
    			if (source != null
    					&& source == wmlEditor.getView(wmlEditor.getEditorViewTabTitle())) {
    				int dot = caretEvent.getDot();
    				int mark = caretEvent.getMark();
    				if (dot == mark) {
    					//no selection
    					WordMLDocument doc = (WordMLDocument) source.getDocument();
    					isEnabled = DocUtil.canSplitSdt(doc, mark);
    				}
    			}
    		}
    		return isEnabled;
    	}
    } //SplitSdtEnabler inner class

    private static class SetupSdtEnabler extends CaretUpdateEnabler {
    	SetupSdtEnabler(JMenuItem item) {
    		super(item);
    	}
    	
    	protected boolean isMenuEnabled(CaretEvent caretEvent) {
    		boolean isEnabled = false;
    		log.debug("SetupSdtEnabler.isMenuEnabled(): caretEvent=" + caretEvent);
    		if (caretEvent != null) {
    			WordMLEditor wmlEditor = WordMLEditor.getInstance(WordMLEditor.class);
    			JEditorPane source = (JEditorPane) caretEvent.getSource();
    			JEditorPane editorView = 
    				wmlEditor.getView(wmlEditor.getEditorViewTabTitle());
        		log.debug("SetupSdtEnabler.isMenuEnabled(): source=" + source);
        		log.debug("SetupSdtEnabler.isMenuEnabled(): EditorView=" + editorView);
    			if (source != null	&& source == editorView) {
    				WordMLDocument doc = (WordMLDocument) source.getDocument();
    				isEnabled = (DocUtil.getChunkingStrategy(doc) == null);
    			}
    		}
    		log.debug("SetupSdtEnabler.isMenuEnabled(): isEnabled=" + isEnabled);
    		return isEnabled;
    	}
    } //SetupSdtEnabler inner class

    private static class CleanSdtEnabler extends CaretUpdateEnabler {
    	CleanSdtEnabler(JMenuItem item) {
    		super(item);
    	}
    	
    	protected boolean isMenuEnabled(CaretEvent caretEvent) {
    		boolean isEnabled = false;
    		if (caretEvent != null) {
    			WordMLEditor wmlEditor = WordMLEditor.getInstance(WordMLEditor.class);
    			JEditorPane source = (JEditorPane) caretEvent.getSource();
    			if (source != null
    				&& source == wmlEditor.getView(wmlEditor.getEditorViewTabTitle())
        			&& ((WordMLTextPane) source).getWordMLEditorKit().getPlutextClient() == null) {
    				WordMLDocument doc = (WordMLDocument) source.getDocument();
    				if (DocUtil.getChunkingStrategy(doc) != null) {
    					isEnabled = DocUtil.hasSdt(doc, 0, doc.getLength());
    				}
    			}
    		} //if (caretEvent != null)
    		return isEnabled;
    	} //isMenuEnabled()
    	
    } //CleanSdtEnabler inner class

}// TeamMenu class


























