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

package org.docx4all.swing;

import java.awt.Cursor;
import java.util.Hashtable;

import javax.swing.SwingWorker;

import org.docx4all.ui.main.WordMLEditor;
import org.plutext.client.Mediator;

/**
 *	@author Jojada Tirtowidjojo - 09/09/2008
 */
public class FetchRemoteEditsWorker extends SwingWorker<Void, Void> implements IProgressBarWorker {
	private final WordMLEditor wmlEditor;
	private final Mediator plutextClient;
	private final Hashtable<Integer, String> messageTable;
	
	private Cursor origCursor;	
	private Exception exc;
	
	public FetchRemoteEditsWorker(Mediator plutextClient, WordMLEditor wmlEditor) {
		this.wmlEditor = wmlEditor;
		this.plutextClient = plutextClient;
		this.messageTable = new Hashtable<Integer, String>(6);
		this.exc = null;
	}
	
    /*
     * Main task. Executed in background thread.
     */
    @Override
    public Void doInBackground() {
    	try {
        	WordMLTextPane editor = this.plutextClient.getWordMLTextPane();
        	setOrigCursor(editor.getCursor());
        	editor.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        	
    		plutextClient.startSession();
    		plutextClient.fetchUpdates(this);
    		
        } catch (Exception exc) {
        	exc.printStackTrace();
        	setException(exc);
        	setProgress(
        		FetchProgress.DONE, 
        		"There was an error while fetching remote edits. Please see logs for details.");
    	}
    	
    	return null;
    }
    
    private final synchronized void setException(Exception exc) {
    	this.exc = exc;
    }
    
    private final synchronized Exception getException() {
    	return this.exc;
    }
    
    private final synchronized void setOrigCursor(Cursor c) {
    	this.origCursor = c;
    }
    
    private final synchronized Cursor getOrigCursor() {
    	return this.origCursor;
    }
    
    /*
     * Executed in event dispatching thread
     */
    @Override
    public void done() {
    	if (getException() != null) {
    		return;
    	}
    	
    	WordMLTextPane editor = this.plutextClient.getWordMLTextPane();
    	try {
    		editor.beginContentControlEdit();
    		
    		plutextClient.applyRemoteChanges(this);
    		
    		//Complete the overall progress but
    		//retain the last message posted by FetchProgress.APPLYING_DONE.
    		//This is because the last message is meaningful to user.
    		//See: plutextClient.applyRemoteChanges();
    		setProgress(FetchProgress.DONE, getProgressMessage(FetchProgress.APPLYING_DONE));
    	} finally {
    		plutextClient.endSession();
    		editor.endContentControlEdit();
    		editor.setCursor(getOrigCursor());
    	}
    }

    public void setProgress(FetchProgress progress, String message) {
    	setProgress(progress.value(), message);
    }
    
    public void setProgress(Integer progress, String message) {
    	//String old = this.messageTable.get(progress);
        this.messageTable.put(progress, message);
        //firePropertyChange("message", old, message);

       	setProgress(progress.intValue());
    }
    
    public String getProgressMessage(FetchProgress progress) {
    	return getProgressMessage(progress.value());
    }
    
    public String getProgressMessage(Integer progressValue) {
    	return this.messageTable.get(progressValue);
    }
    
    public void setChangeSetsDisplay(Object obj) {
    	firePropertyChange("changesets", null, obj);
    }
    
    public enum FetchProgress {
    	START_FETCHING (10),
    	REGISTERING_UPDATES (20),
    	FETCHING_REMOTE_DOC_STRUCTURE (30),
    	FETCHING_DONE (40),
    	START_APPLYING_UPDATES (50),
    	COMPARING_DOC_STRUCTURES (60),
    	APPLYING_UPDATES (70),
    	APPLYING_DONE (90),
    	
    	DONE (100);
    	
    	private final Integer value;
    	FetchProgress(int value) {
    		this.value = Integer.valueOf(value);
    	}
    	
    	public Integer value() {
    		return value;
    	}
    }
}// FetchRemoteEditsWorker class



















