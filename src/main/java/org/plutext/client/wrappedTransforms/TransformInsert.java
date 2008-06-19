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

import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;
import org.docx4all.swing.WordMLTextPane;
import org.docx4all.swing.text.DocumentElement;
import org.docx4all.swing.text.WordMLDocument;
import org.docx4all.swing.text.WordMLFragment;
import org.docx4all.swing.text.WordMLFragment.ElementMLRecord;
import org.docx4all.xml.SdtBlockML;
import org.plutext.client.ServerFrom;
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
	protected BigInteger insertAtIndex;
	
	public TransformInsert(T t) {
		super(t);
		insertAfterControlId = null;
		insertBeforeControlId = null;
		insertAtIndex = null;
	}
	
	public long apply(ServerFrom serverFrom) {
		//Plutext server is trying to use absolute index position for
		//locating the insert positon.
		//TODO: The following code is subject to change.
		if (this.t.getPosition() == null || this.t.getPosition() < 0) {
			this.insertAtIndex = null;
		} else {
			this.insertAtIndex = BigInteger.valueOf(this.t.getPosition());
		}
		apply(serverFrom.getWordMLTextPane());
		
		return sequenceNumber;
	}
	
	protected void apply(WordMLTextPane editor) {
		Runnable runnable = null;
		
		if (this.insertAtIndex != null) {
			runnable = new InsertAtRunnable(editor, this.insertAtIndex.intValue());
		} else if (this.insertAfterControlId != null) {
			runnable = new InsertAfterRunnable(editor, this.insertAfterControlId);
		} else if (this.insertBeforeControlId != null) {
			runnable = new InsertBeforeRunnable(editor, this.insertBeforeControlId);
		} else {
			//Become the only SdtBlock in the document
			runnable = new InsertNewRunnable(editor);
		}
		
		SwingUtilities.invokeLater(runnable);
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

	private class InsertAtRunnable implements Runnable {
		private WordMLTextPane editor;
		private int insertAtIdx;
		
		private InsertAtRunnable(WordMLTextPane editor, int insertAtIdx) {
			this.editor = editor;
			this.insertAtIdx = insertAtIdx;
		}
		
		public void run() {
			BigInteger id = getSdt().getSdtPr().getId().getVal();
			
			log.debug("InsertAtRunnable.run(): Inserting SdtBlock Id=" 
					+ id 
					+ " into Editor at insertAtIdx=" 
					+ this.insertAtIdx);
			
			int origPos = editor.getCaretPosition();
			boolean forward = true;
			
			try {
				editor.beginContentControlEdit();
				
				WordMLDocument doc = (WordMLDocument) editor.getDocument();
				DocumentElement elem = (DocumentElement) doc.getDefaultRootElement();
				
				int idx = Math.min(elem.getElementCount()-1, this.insertAtIdx);
				idx = Math.max(this.insertAtIdx, 0);
				
				log.debug("InsertAtRunnable.run(): SdtBlock will be inserted at idx="
					+ idx
					+ " in document.");
				
				elem = (DocumentElement) elem.getElement(idx);
								
				log.debug("InsertAtRunnable.run(): DocumentElement at idx=" + idx
					+ " is " + elem);
				
				log.debug("InsertAtRunnable.run(): Current caret position=" + origPos);
					
				if (elem.getStartOffset() <= origPos) {
					origPos = doc.getLength() - origPos;
					forward = false;
				}
					
				ElementMLRecord[] recs = { new ElementMLRecord(
						new SdtBlockML(getSdt()), false) };
				WordMLFragment frag = new WordMLFragment(recs);

				editor.setCaretPosition(elem.getStartOffset());
				editor.replaceSelection(frag);
				
			} finally {
				if (!forward) {
					origPos = editor.getDocument().getLength() - origPos;
				}
				
				log.debug("InsertAtRunnable.run(): Set caret position to " + origPos);
				editor.setCaretPosition(origPos);
				
				editor.endContentControlEdit();
			}
		}
	} //InsertAtRunnable inner class


} //TransformInsert class

























