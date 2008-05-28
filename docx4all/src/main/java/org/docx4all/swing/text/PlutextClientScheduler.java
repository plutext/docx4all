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

package org.docx4all.swing.text;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.xml.bind.JAXBIntrospector;

import org.apache.log4j.Logger;
import org.docx4all.swing.PlutextClientWorker;
import org.docx4all.swing.WordMLTextPane;
import org.docx4all.swing.event.WordMLDocumentEvent;
import org.docx4all.swing.event.WordMLDocumentListener;
import org.docx4all.util.SwingUtil;
import org.docx4j.XmlUtils;
import org.docx4j.wml.SdtBlock;
import org.plutext.client.ServerTo;
import org.plutext.client.state.ContentControlSnapshot;

/**
 *  This class is used to schedule the periodic execution of plutext client's communication
 *  with plutext server in a background process. As such, it needs a TimerTask and a SwingWorker.
 *  The TimerTask is implemented in PlutextWorkerTask and the SwingWorker in PlutextClientWorker.
 *  
 *  At the time of execution, plutext client which is an instance of org.plutext.client.ServerTo
 *  needs to know:
 *  - SdtBlocks that have been updated, inserted, and deleted
 *  - A SdtBlock that is currently on focus or being worked on.
 *  To supply these information this class employs two collections: 'dirtySdtBlocks' 
 *  and 'deletedSdtBlocks' and 'sdtBlockAtCaret' property. 
 *  'dirtySdtBlocks' will collect newly inserted and updated SdtBlocks, 
 *  'deletedSdtBlocks' collects those SdtBlocks that have been deleted and 'sdtBlockAtCaret' 
 *  keeps track of SdtBlock on focus.
 *  
 *  Being a WordMLDocumentListener this class is able to collect those SdtBlocks that are dirty
 *  and have been deleted and being a CaretListener it is able to keep track of SdtBlock on focus.
 *  
 *	@author Jojada Tirtowidjojo - 16/05/2008
 */
public class PlutextClientScheduler extends Timer implements WordMLDocumentListener, CaretListener {
	private static Logger log = Logger.getLogger(PlutextClientScheduler.class);
	
	private ServerTo plutextClient;
	private TimerTask timerTask;
	
	//Dirty SdtBlocks include newly inserted and updated SdtBlocks.
	private Map<BigInteger, SdtBlock> dirtySdtBlocks = 
		new HashMap<BigInteger, SdtBlock>();
	
	//Deleted SdtBlocks are those SdtBlocks that have been deleted.
	private Map<BigInteger, SdtBlock> deletedSdtBlocks = 
		new HashMap<BigInteger, SdtBlock>();
	
	//SdtBlock that is hosting current caret position 
	//is kept in 'sdtBlockAtCaret'.
	private SdtBlock sdtBlockAtCaret = null;
	
	//The last recorded 'sdtBlockAtCaret'
	private SdtBlock lastSdtBlockAtCaret = null;
	
	public PlutextClientScheduler(ServerTo plutextClient) {
		super();
		this.plutextClient = plutextClient;
		this.timerTask = new PlutextWorkerTask();
	}
	
	public ServerTo getPlutextClient() {
		return plutextClient;
	}
	
	public void schedule(long delay, long period) {
		super.schedule(this.timerTask, delay, period);
	}
	
	/**
	 * A factory method that creates a PlutextClientWorker.
	 * PlutextClientWorker is a SwingWorker and hence, 
	 * the class that performs the background process of
	 * plutext client communication with plutext server.
	 * 
	 * As this background process is designed to be executed
	 * periodically, this PlutextClientScheduler will wraps
	 * a PlutextClientWorker inside a TimerTask; 
	 * ie: PlutextWorkerTask.
	 * 
	 * @return An instance of PlutextClientWorker that will be
	 * wrapped by a TimerTask.
	 */
    private PlutextClientWorker createPlutextClientWorker() {
		PlutextClientWorker worker = 
			new PlutextClientWorker(this.plutextClient);
		
		List<SdtBlock> dirtyList = null;
		List<SdtBlock> deletedList = null;
		
		synchronized (this) {
			boolean isDirtyAndRemoved = false;
			if (this.sdtBlockAtCaret != null) {
				//As sdtBlockAtCaret is a SdtBlock which is currently being worked on
				//it should not be a dirty SdtBlock YET; ie: in this round of execution.
				//Once it has released the caret, it will be entitled to. This may
				//happen in future round of execution.
				BigInteger id = this.sdtBlockAtCaret.getSdtPr().getId().getVal();
				if (this.dirtySdtBlocks.get(id) != null) {
					this.dirtySdtBlocks.remove(id);
					isDirtyAndRemoved = true;
				}
			}			
			
			//Pass on 'dirtySdtBlocks' and 'deletedSdtBlocks' 
			//to PlutextClientWorker as lists.
			dirtyList = new ArrayList<SdtBlock>(this.dirtySdtBlocks.values());
			deletedList = new ArrayList<SdtBlock>(this.deletedSdtBlocks.values());
			
			worker.setDeletedSdtBlocks(deletedList);
			worker.setDirtySdtBlocks(dirtyList);
			worker.setSdtBlockAtWork(this.sdtBlockAtCaret);
			
			//Both 'dirtySdtBlocks' and 'deletedSdtBlocks' have been
			//passed on to PlutextClientWorker. It is time to prepare
			//for the next registration by clearing them.
			this.dirtySdtBlocks.clear();
			this.deletedSdtBlocks.clear();
			
			if (this.sdtBlockAtCaret != null && isDirtyAndRemoved) {
				//Keep this dirty 'sdtBlockAtCaret' for the next round of execution.
				//See the comment above where isDirtyAndRemoved flag is set.
				BigInteger id = this.sdtBlockAtCaret.getSdtPr().getId().getVal();
				this.dirtySdtBlocks.put(id, this.sdtBlockAtCaret);
			}
		}
		
		return worker;
    }
    
	// ============================
	// CaretListener Implementation
	// ============================
    
    /**
     * This implementation is to keep track of SdtBlock that is hosting caret.
     * This SdtBlock is considered as an SdtBlock that is currently
     * on focus and being worked on.
     * 
     * This method will be called when the caret position is updated.
     * 
     * @param e the caret event
     */
    public void caretUpdate(CaretEvent evt) {			
    	WordMLTextPane editor = (WordMLTextPane) evt.getSource();
		int pos = evt.getDot();
		SdtBlockView v = SwingUtil.getSdtBlockView(editor, pos);
		
		synchronized(this) {
			this.sdtBlockAtCaret = null;
			if (v != null) {
				DocumentElement elem = (DocumentElement) v.getElement();
				this.sdtBlockAtCaret = 
					(SdtBlock) elem.getElementML().getDocxObject();
			}
			
			if (log.isDebugEnabled()) {
				if (this.sdtBlockAtCaret == null) {
					log.debug("caretUpdate(): New sdtBlockAtCaret = NULL");
				} else {
					log.debug("caretUpdate(): New sdtBlockAtCaret="
							+ this.sdtBlockAtCaret
							+ " - Id="
							+ this.sdtBlockAtCaret.getSdtPr().getId().getVal()
							+ " - Tag="
							+ this.sdtBlockAtCaret.getSdtPr().getTag().getVal());
				}
			
				if (this.lastSdtBlockAtCaret == null) {
					log.debug("caretUpdate(): Last sdtBlockAtCaret = NULL");
				} else {
					log.debug("caretUpdate(): Last sdtBlockAtCaret="
							+ this.lastSdtBlockAtCaret
							+ " - Id="
							+ this.lastSdtBlockAtCaret.getSdtPr().getId().getVal()
							+ " - Tag="
							+ this.lastSdtBlockAtCaret.getSdtPr().getTag().getVal());
				}
			}
			
			if (this.lastSdtBlockAtCaret != this.sdtBlockAtCaret) {
				log.debug("caretUpdate(): New sdtBlockAtCaret != Last sdtBlockAtCaret");
				log.debug("New sdtBlockAtCaret.xml=" 
						+ ((this.sdtBlockAtCaret == null) 
							? "NULL" 
							: ContentControlSnapshot.getContentControlXML(this.sdtBlockAtCaret)));
				
				//There has been a change of 'sdtBlockAtCaret'.
				//Note that when a user inserted a string of text (not a WordMLFragment) 
				//either by typing or pasting, the text is not saved into ElementML.
				//Calling editor.saveCaretText() will ensure that text content be
				//saved into ElementML.
				editor.saveCaretText();
				
				//Either 'dirtySdtBlocks' or 'deletedSdtBlocks' may have recorded
				//a snapshot of 'lastSdtBlockAtCaret' which may have become invalid
				//at this time of 'sdtBlockAtCaret' change. Especially after
				//editor.saveCaretText() has just been called.
				//Therefore, refresh the snapshot of 'lastSdtBlockAtCaret'.
				if (this.lastSdtBlockAtCaret != null
					&& isInDocument(editor, this.lastSdtBlockAtCaret)) {
					
					//A snapshot is a clone. See:WordMLDocument.getSnapshots().
					Object cloneObj = XmlUtils.deepCopy(this.lastSdtBlockAtCaret);
					org.docx4j.wml.SdtBlock refreshedLastSdtBlock = 
						(org.docx4j.wml.SdtBlock) JAXBIntrospector.getValue(cloneObj);
					BigInteger id = 
						refreshedLastSdtBlock.getSdtPr().getId().getVal();
					if (this.deletedSdtBlocks.get(id) != null) {
						this.deletedSdtBlocks.put(id, refreshedLastSdtBlock);
						//this.lastSdtBlockAtCaret should not be in 'dirtySdtBlocks'
						//but just in case
						this.dirtySdtBlocks.remove(id); 
					} else {
						//if 'dirtySdtBlocks' has no record of this.lastSdtBlockAtCaret
						//then it needs to have now so that Plutext client may register
						//this.lastSdtBlockAtCaret as an SdtBlock that has been exited.
						this.dirtySdtBlocks.put(id, refreshedLastSdtBlock);						
					}
					log.debug("Refreshed LAST sdtBlockAtCaret.xml="
						+ ContentControlSnapshot.getContentControlXML(refreshedLastSdtBlock));
				}
				
				this.lastSdtBlockAtCaret = this.sdtBlockAtCaret;
				
			} else {
				log.debug("caretUpdate(): New sdtBlockAtCaret == Last sdtBlockAtCaret");
			}
		}
    }
    
    private boolean isInDocument(WordMLTextPane editor, SdtBlock sdt) {
    	boolean isInDocument = false;
    	
		DocumentElement root = (DocumentElement) editor.getDocument().getDefaultRootElement();
		for (int i = 0; i < root.getElementCount() - 1 && !isInDocument; i++) {
			DocumentElement elem = (DocumentElement) root.getElement(i);
			isInDocument = (sdt == elem.getElementML().getDocxObject());
		}
		
		return isInDocument;
    }
    
	// =====================================
	// WordMLDocumentListener Implementation
	// =====================================
    public void insertUpdate(DocumentEvent e) {}
    public void removeUpdate(DocumentEvent e) {}
    public void changedUpdate(DocumentEvent e) {}
    
    public void snapshotChangedUpdate(WordMLDocumentEvent e) {
    	WordMLDocument doc = (WordMLDocument) e.getDocument();
    	
    	Map<BigInteger, SdtBlock> initialSnapshots = 
    		e.getInitialSnapshots();
    	Map<BigInteger, SdtBlock> currentSnapshots = 
    		(e.getLength() > 0) 
    			? doc.getSnapshots(e.getOffset(), e.getLength())
    			: null;
    			
    	synchronized (this) {
    		if (currentSnapshots != null) {
    			int i=0;
	    		for (SdtBlock sdt:currentSnapshots.values()) {
	    			BigInteger id = sdt.getSdtPr().getId().getVal();
    				this.dirtySdtBlocks.put(id, sdt);
    				
    				log.debug("snapshotChangedUpdate(): CurrentSnapshots[" + (i++) + "]="
    						+ sdt
    						+ " - Id="
    						+ id
    						+ " - Tag="
    						+ sdt.getSdtPr().getTag().getVal());
    				log.debug("snapshotChangedUpdate(): CurrentSnapshots[" + (i++) + "].xml=" 
    						+ ContentControlSnapshot.getContentControlXML(sdt));
	    		}		    		
    		}
    		
    		if (initialSnapshots != null) {
    			int i=0;
	    		for (SdtBlock sdt:initialSnapshots.values()) {
	    			BigInteger id = sdt.getSdtPr().getId().getVal();
	    			
	    			boolean hasBeenDeleted = true;
	    			if (currentSnapshots != null) {
	    				hasBeenDeleted = (currentSnapshots.get(id) == null);
	    			}
	    			
	    			if (hasBeenDeleted) {
	    				this.deletedSdtBlocks.put(id, sdt);
	    			}
	    			
    				log.debug("snapshotChangedUpdate(): InitialSnapshots[" + (i++) + "]="
    						+ sdt
    						+ " - Id="
    						+ id
    						+ " - Tag="
    						+ sdt.getSdtPr().getTag().getVal());
    				log.debug("snapshotChangedUpdate(): InitialSnapshots[" + (i++) + "].xml=" 
    						+ ContentControlSnapshot.getContentControlXML(sdt));
	    		}
    		}
    		
    		if (log.isDebugEnabled() && this.deletedSdtBlocks != null) {
    			int i=0;
    			for (SdtBlock sdt:this.deletedSdtBlocks.values()) {
    				log.debug("snapshotChangedUpdate(): Deleted SdtBlock[" + (i++) + "]="
    						+ sdt
    						+ " - Id="
    						+ sdt.getSdtPr().getId().getVal()
    						+ " - Tag="
    						+ sdt.getSdtPr().getTag().getVal());
    				log.debug("snapshotChangedUpdate(): Deleted SdtBlock[" + (i++) + "].xml=" 
    						+ ContentControlSnapshot.getContentControlXML(sdt));    				
    			}
    		}
		}
	}
	
	private class PlutextWorkerTask extends TimerTask {
		public void run() {
			PlutextClientWorker worker = createPlutextClientWorker();	
			if (worker.hasWorkToDo()) {
				SdtBlock sdt = worker.getSdtBlockAtWork();
				
				if (log.isDebugEnabled()) {
					if (sdt == null) {
						log.debug(
							"PlutextClientWorkerTask.run(): worker.getSdtBlockAtWork() == NULL");					
					} else {
						log.debug("PlutextClientWorkerTask.run(): "
								+ "worker.getSdtBlockAtWork()="
								+ sdt
								+ " - Id="
								+ sdt.getSdtPr().getId().getVal()
								+ " - Tag="
								+ sdt.getSdtPr().getTag().getVal());
					}
				}
				
				worker.execute();
	    	}
		}
	}// PlutextWorkerTask inner class
	
}// PlutextClientScheduler class



















