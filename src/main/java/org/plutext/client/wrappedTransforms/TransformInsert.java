/*
 *  Copyright 2008, Plutext Pty Ltd.
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

package org.plutext.client.wrappedTransforms;

import java.math.BigInteger;
import java.util.Map;

import javax.swing.SwingUtilities;

import org.alfresco.webservice.util.AuthenticationUtils;
import org.apache.log4j.Logger;
import org.docx4all.swing.WordMLTextPane;
import org.docx4all.swing.text.DocumentElement;
import org.docx4all.swing.text.WordMLFragment;
import org.docx4all.swing.text.WordMLFragment.ElementMLRecord;
import org.docx4all.xml.ObjectFactory;
import org.docx4all.xml.SdtBlockML;
import org.docx4j.wml.Id;
import org.plutext.client.ServerFrom;
import org.plutext.client.ServerTo;
import org.plutext.client.state.ContentControlSnapshot;
import org.plutext.client.webservice.PlutextService_ServiceLocator;
import org.plutext.client.webservice.PlutextWebService;
import org.plutext.transforms.Transforms.T;

public class TransformInsert extends TransformAbstract {
	/*
	 * This class is a basic proof of concept.
	 * Deficiencies:
	 * - What do you insert @after if the change
	 * is at the beginning of the document.  Need @before as well?
	 * - What do you do if the @after ID content control has
	 * been deleted from this document?  In this case,
	 * ask the server for the skeleton document, and use it to 
	 * work out where to put it. [TODO]
	 * 
	 * */

	private static Logger log = Logger.getLogger(TransformInsert.class);
	
	protected BigInteger insertAfterControlId;
	protected BigInteger insertBeforeControlId;
	
	public TransformInsert(T t) {
		super(t);
		insertAfterControlId = null;
		insertBeforeControlId = null;
	}
	
	public long apply(ServerFrom serverFrom) {
		log.debug("apply(ServerFrom): sdtBolck = " + getSdt() 
				+ " - ID=" + getSdt().getSdtPr().getId().getVal()
				+ " - TAG=" + getSdt().getSdtPr().getTag().getVal());

		boolean isTheOnlySdtBlockInSkeleton = false;
		
		if (t.getAfter() != null) {
			this.insertAfterControlId = BigInteger.valueOf(t.getAfter());
			
		} else {
			//Try to consult skeleton document.
	        try {
				AuthenticationUtils.startSession(ServerTo.USERNAME, ServerTo.PASSWORD);
				
				PlutextService_ServiceLocator locator = 
					new PlutextService_ServiceLocator(
						AuthenticationUtils.getEngineConfiguration());
				locator.setPlutextServiceEndpointAddress(
						org.alfresco.webservice.util.WebServiceFactory.getEndpointAddress()
						+ "/" 
						+ locator.getPlutextServiceWSDDServiceName());
				PlutextWebService ws = locator.getPlutextService();
				
				String skeleton = ws.getSkeletonDocument(serverFrom.getStateDocx().getDocID());
				
				log.debug("apply(ServerFrom): skeleton=" + skeleton);
				
				BigInteger id = getSdt().getSdtPr().getId().getVal();
				
				char quotationChar = '\"';
				StringBuffer searchedId = new StringBuffer();
				searchedId.append(quotationChar);
				searchedId.append(id.toString());
				searchedId.append(quotationChar);
				
				this.insertAfterControlId = getOlderSiblingId(skeleton, searchedId.toString());
				this.insertBeforeControlId = getYoungerSiblingId(skeleton, searchedId.toString());
				
				if (this.insertAfterControlId == null && this.insertBeforeControlId == null) {
					isTheOnlySdtBlockInSkeleton = (skeleton.indexOf(searchedId.toString()) > -1);
				}
				
				// End the current session
				AuthenticationUtils.endSession();
				
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
    	
		Map<Id, ContentControlSnapshot> snapshots = 
    		serverFrom.getStateDocx().getContentControlSnapshots();
    	
		ContentControlSnapshot insertAfterSnapshot = null;
		if (this.insertAfterControlId != null) {
			Id key = ObjectFactory.createId(this.insertAfterControlId);
			insertAfterSnapshot = snapshots.get(key);
		}
		
		ContentControlSnapshot insertBeforeSnapshot = null;
		if (this.insertBeforeControlId != null) {
			Id key = ObjectFactory.createId(this.insertBeforeControlId);
			insertBeforeSnapshot = snapshots.get(key);
		}

		if (insertAfterSnapshot == null 
			&& insertBeforeSnapshot == null
			&& !isTheOnlySdtBlockInSkeleton) {
			//Do not know where to insert this TransformInsert
	        // TODO - throw error
			return -1;
		}
		
		apply(serverFrom.getWordMLTextPane());

		return sequenceNumber;
	}

	protected void apply(WordMLTextPane editor) {
		Runnable runnable = null;
		
		if (this.insertAfterControlId != null) {
			runnable = new InsertAfterRunnable(editor, this.insertAfterControlId);
		} else if (this.insertBeforeControlId != null) {
			runnable = new InsertBeforeRunnable(editor, this.insertBeforeControlId);
		} else {
			//Become the only SdtBlock in the document
			runnable = new InsertNewRunnable(editor);
		}
		
		SwingUtilities.invokeLater(runnable);
	}
	
	private BigInteger getOlderSiblingId(String skeleton, String id) {
		//As an example, skeleton may be like the following:  
		//"<skeleton><rib id=\"731393367\" /><rib id=\"901835978\" /><rib id=\"820155394\" /></skeleton>";
		int idx = skeleton.indexOf(id);
		if (idx == -1) {
			return null;
		}
		
		char quotationChar = '\"';
		
		//Older sibling is on the left
		String temp = skeleton.substring(0, idx);
		int endIdx = temp.lastIndexOf(quotationChar);
		if (endIdx == -1) {
			return null;
		}
		
		int startIdx = temp.lastIndexOf(quotationChar, endIdx - 1);
		temp = temp.substring(startIdx + 1, endIdx);
		
		return new BigInteger(temp);
	}
	
	private BigInteger getYoungerSiblingId(String skeleton, String id) {
		//As an example, skeleton may be like the following:  
		//"<skeleton><rib id=\"731393367\" /><rib id=\"901835978\" /><rib id=\"820155394\" /></skeleton>";
		
		int idx = skeleton.indexOf(id);
		if (idx == -1) {
			return null;
		}
		
		char quotationChar = '\"';
		
		//Younger sibling is on the right
		String temp = skeleton.substring(idx + id.length());
		
		int startIdx = temp.indexOf(quotationChar);
		if (startIdx == -1) {
			return null;
		}
		
		int endIdx = temp.indexOf(quotationChar, startIdx + 1);
		temp = temp.substring(startIdx + 1, endIdx);
		
		return new BigInteger(temp);
	}
	
	private class InsertAfterRunnable implements Runnable {
		private WordMLTextPane editor;
		private BigInteger afterId;
		
		private InsertAfterRunnable(WordMLTextPane editor, BigInteger afterId) {
			this.editor = editor;
			this.afterId = afterId;
		}
		
		public void run() {
			BigInteger id = getSdt().getSdtPr().getId().getVal();
			
			log.debug("InsertAfterRunnable.run(): Inserting SdtBlock Id=" 
					+ id + " into Editor.");
			log.debug("InsertAfterRunnable.run(): afterId=" + afterId);
			
			int origPos = editor.getCaretPosition();
			boolean forward = true;
			
			try {
				editor.beginContentControlEdit();
				
				DocumentElement elem = getDocumentElement(editor, afterId);
				
				log.debug("InsertAfterRunnable.run(): SdtBlock Element at afterId=" + afterId
					+ " is " + elem);
				
				if (elem != null) {
					log.debug("InsertAfterRunnable.run(): Current caret position=" + origPos);
					
					if (elem.getEndOffset() <= origPos) {
						origPos = editor.getDocument().getLength() - origPos;
						forward = false;
					}
					
					ElementMLRecord[] recs = { new ElementMLRecord(
							new SdtBlockML(getSdt()), false) };
					WordMLFragment frag = new WordMLFragment(recs);

					editor.setCaretPosition(elem.getEndOffset());
					editor.replaceSelection(frag);

				} else {
					//silently ignore
					log.warn("InsertAfterRunnable.run(): Failed to insert.");
				}
			} finally {
				if (!forward) {
					origPos = editor.getDocument().getLength() - origPos;
				}
				
				log.debug("InsertAfterRunnable.run(): Set caret position to " + origPos);
				editor.setCaretPosition(origPos);
				
				editor.endContentControlEdit();
			}
		}
	} //InsertAfterRunnable inner class

	private class InsertBeforeRunnable implements Runnable {
		private WordMLTextPane editor;
		private BigInteger beforeId;
		
		private InsertBeforeRunnable(WordMLTextPane editor, BigInteger beforeId) {
			this.editor = editor;
			this.beforeId = beforeId;
		}
		
		public void run() {
			BigInteger id = getSdt().getSdtPr().getId().getVal();
			
			log.debug("InsertBeforeRunnable.run(): Inserting SdtBlock Id=" 
					+ id + " into Editor.");
			log.debug("InsertBeforeRunnable.run(): beforeId=" + beforeId);
			
			int origPos = editor.getCaretPosition();
			boolean forward = true;
			
			try {
				editor.beginContentControlEdit();
				
				DocumentElement elem = getDocumentElement(editor, beforeId);
				
				log.debug("InsertBeforeRunnable.run(): SdtBlock Element at beforeId=" + beforeId
					+ " is " + elem);
				
				if (elem != null) {
					log.debug("InsertBeforeRunnable.run(): Current caret position=" + origPos);
					
					if (elem.getStartOffset() <= origPos) {
						origPos = editor.getDocument().getLength() - origPos;
						forward = false;
					}
					
					ElementMLRecord[] recs = { new ElementMLRecord(
							new SdtBlockML(getSdt()), false) };
					WordMLFragment frag = new WordMLFragment(recs);

					editor.setCaretPosition(elem.getStartOffset());
					editor.replaceSelection(frag);

				} else {
					//silently ignore
					log.warn("InsertBeforeRunnable.run(): Failed to insert.");
				}
			} finally {
				if (!forward) {
					origPos = editor.getDocument().getLength() - origPos;
				}
				
				log.debug("InsertBeforeRunnable.run(): Set caret position to " + origPos);
				editor.setCaretPosition(origPos);
				
				editor.endContentControlEdit();
			}
		}
	} //InsertBeforeRunnable inner class

	private class InsertNewRunnable implements Runnable {
		private WordMLTextPane editor;
		
		private InsertNewRunnable(WordMLTextPane editor) {
			this.editor = editor;
		}
		
		public void run() {
			BigInteger id = getSdt().getSdtPr().getId().getVal();
			
			log.debug("InsertNewRunnable.run(): Inserting SdtBlock Id=" 
					+ id + " into Editor.");
			
			int origPos = editor.getCaretPosition();
			boolean forward = true;
			
			try {
				editor.beginContentControlEdit();
				
				log.debug("InsertNewRunnable.run(): Current caret position=" + origPos);
					
				if (0 <= origPos) {
					origPos = editor.getDocument().getLength() - origPos;
					forward = false;
				}
				
				ElementMLRecord[] recs = { new ElementMLRecord(
						new SdtBlockML(getSdt()), false) };
				WordMLFragment frag = new WordMLFragment(recs);

				editor.setCaretPosition(0);
				editor.replaceSelection(frag);
				
			} finally {
				if (!forward) {
					origPos = editor.getDocument().getLength() - origPos;
				}
				
				log.debug("InsertNewRunnable.run(): Set caret position to " + origPos);
				editor.setCaretPosition(origPos);
				
				editor.endContentControlEdit();
			}
		}
	} //InsertNewRunnable inner class

} //TransformInsert class

























