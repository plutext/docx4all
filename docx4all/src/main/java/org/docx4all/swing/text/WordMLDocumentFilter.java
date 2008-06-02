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
import java.util.Map;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.Position;

import org.apache.log4j.Logger;
import org.docx4all.swing.event.WordMLDocumentEvent;
import org.docx4j.wml.SdtBlock;

/**
 *	@author Jojada Tirtowidjojo - 10/12/2007
 */
public class WordMLDocumentFilter extends DocumentFilter {
	private static Logger log = Logger.getLogger(WordMLDocumentFilter.class);
	private boolean enabled = true;
	
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	
	public boolean isEnabled() {
		return this.enabled;
	}
	
    public void remove(FilterBypass fb, int offset, int length)
			throws BadLocationException {

    	WordMLDocument doc = (WordMLDocument) fb.getDocument();
    	
		if (log.isDebugEnabled()) {
			log.debug("remove(): isEnabled()=" + isEnabled()
				+ " offset=" + offset 
				+ " length=" + length
				+ " doc.getLength()=" + doc.getLength());
		}

		if (length == 0 || offset >= doc.getLength()) {
    		//Cannot delete the end of document
			return;
		}

		if (!isEnabled()) {
			super.remove(fb, offset, length);
			return;
		}
		
		Map<BigInteger, SdtBlock> snapshots = null;
		Position blockStart = null;
		Position blockEnd = null;
		
		if (!doc.isSnapshotFireBan()) {
			DocumentElement rootE = (DocumentElement) doc.getDefaultRootElement();
			
			DocumentElement elem = (DocumentElement)
				rootE.getElement(rootE.getElementIndex(offset));
			blockStart = doc.createPosition(elem.getStartOffset());
			
			elem = (DocumentElement) rootE.getElement(rootE.getElementIndex(offset + length - 1));
			blockEnd = doc.createPosition(elem.getEndOffset());
			
			snapshots = 
				doc.getSnapshots(
					blockStart.getOffset(), 
					blockEnd.getOffset() - blockStart.getOffset());
		}
		
		try {
			TextRemover tr = new TextRemover(fb, offset, length);
			tr.doAction();
		} catch (BadSelectionException exc) {
			//Don't need to fire snapshot change event.
			blockStart = null;
			blockEnd = null;
			throw new BadLocationException("Unselectable range: offset="
				+ offset + " length=" + length, offset);
		} finally {
			if (!doc.isSnapshotFireBan() && blockStart != null && blockEnd != null) {
				WordMLDocument.WordMLDefaultDocumentEvent evt = 
					doc.new WordMLDefaultDocumentEvent(
						blockStart.getOffset(),
						blockEnd.getOffset() - blockStart.getOffset(),
						null,
						WordMLDocumentEvent.SNAPSHOT_CHANGED_EVT_NAME);
				evt.setInitialSnapshots(snapshots);
				doc.fireSnapshotChanged(evt);
			}
		}
	}
    
    public void replace(FilterBypass fb, int offset, int length, String text,
            AttributeSet attrs) throws BadLocationException {

    	WordMLDocument doc = (WordMLDocument) fb.getDocument();
    	
		if (log.isDebugEnabled()) {
			log.debug("replace(): offset=" + offset 
				+ " length=" + length
				+ " text=" + text
				+ " attrs=" + attrs
				+ " doc.getLength()=" + doc.getLength());
		}

		if (offset < 0 
				|| offset > doc.getLength() 
				|| length < 0 
				|| offset + length > doc.getLength()) {
			throw new BadLocationException("Invalid replace", offset);
		}
			
		if (!isEnabled()) {
			super.replace(fb, offset, length, text, attrs);
			return;
		}
		
		Map<BigInteger, SdtBlock> snapshots = null;
		Position blockStart = null;
		Position blockEnd = null;
		
		if (!doc.isSnapshotFireBan()) {
			DocumentElement rootE = (DocumentElement) doc.getDefaultRootElement();
			
			int idx = rootE.getElementIndex(offset);
			if (idx == rootE.getElementCount() - 1) {
				//if 'offset' points to the last element,
				//include the snapshot of the last element's 
				//immediate older sibling.
				idx = Math.max(0, idx-1);
			}
			
			DocumentElement elem = (DocumentElement) rootE.getElement(idx);
			blockStart = doc.createPosition(elem.getStartOffset());
			
			//Remember that (offset <= doc.getLength() - length) or otherwise
			//BadLocationException must have been thrown. 
			//length == 0 indicates no text deletion/replacement.
			idx = (length == 0) ? idx : rootE.getElementIndex(offset + length - 1);
			
			elem = (DocumentElement) rootE.getElement(idx);
			blockEnd = doc.createPosition(elem.getEndOffset());
			
			if (blockStart.getOffset() == offset
				&& blockEnd.getOffset() == offset + length) {
				//Deleting text within [offset, offset+length]
				//will cause blockStart to be equal to blockEnd,
				//hence result in wrong final snapshot.
				//We extend blockEnd to the end of next element 
				//to avoid this.
				elem = (DocumentElement) rootE.getElement(idx + 1);
				blockEnd = doc.createPosition(elem.getEndOffset());
			}
			
			snapshots = 
				doc.getSnapshots(
					blockStart.getOffset(), 
					blockEnd.getOffset() - blockStart.getOffset());
		}
		
		try {
			TextReplacer tr = new TextReplacer(fb, offset, length, text, attrs);
			tr.doAction();
			
		} catch (BadSelectionException exc) {
			//Don't need to fire snapshot change event.
			blockStart = null;
			blockEnd = null;
			throw new BadLocationException("Unselectable range: offset="
				+ offset + " length=" + length, offset);
			
		} finally {
			if (!doc.isSnapshotFireBan() && blockStart != null && blockEnd != null) {
				WordMLDocument.WordMLDefaultDocumentEvent evt = 
					doc.new WordMLDefaultDocumentEvent(
						blockStart.getOffset(),
						blockEnd.getOffset() - blockStart.getOffset(),
						null,
						WordMLDocumentEvent.SNAPSHOT_CHANGED_EVT_NAME);
				evt.setInitialSnapshots(snapshots);
				doc.fireSnapshotChanged(evt);
			}
		}
    }

    public void insertString(FilterBypass fb, int offset, String text,
            AttributeSet attrs) throws BadLocationException {
    	
    	WordMLDocument doc = (WordMLDocument) fb.getDocument();
    	
		if (log.isDebugEnabled()) {
			log.debug("insertString(): offset=" + offset 
				+ " text=" + text
				+ " attrs=" + attrs
				+ " doc.getLength()=" + doc.getLength());
		}

		if (!isEnabled()) {
			super.insertString(fb, offset, text, attrs);
			return;
		}
		
		Map<BigInteger, SdtBlock> snapshots = null;
		Position blockStart = null;
		Position blockEnd = null;
		
		if (!doc.isSnapshotFireBan()) {
			DocumentElement rootE = (DocumentElement) doc.getDefaultRootElement();
			
			DocumentElement elem = (DocumentElement)
				rootE.getElement(rootE.getElementIndex(offset));
			blockStart = doc.createPosition(elem.getStartOffset());
			blockEnd = doc.createPosition(elem.getEndOffset());
			
			snapshots = 
				doc.getSnapshots(
					blockStart.getOffset(), 
					blockEnd.getOffset() - blockStart.getOffset());
		}
		
		try {
			TextInserter tr = new TextInserter(fb, offset, text, attrs);
			tr.doAction();
		} finally {
			if (!doc.isSnapshotFireBan() && blockStart != null && blockEnd != null) {
				WordMLDocument.WordMLDefaultDocumentEvent evt = 
					doc.new WordMLDefaultDocumentEvent(
						blockStart.getOffset(),
						blockEnd.getOffset() - blockStart.getOffset(),
						null,
						WordMLDocumentEvent.SNAPSHOT_CHANGED_EVT_NAME);
				evt.setInitialSnapshots(snapshots);
				doc.fireSnapshotChanged(evt);
			}
		}
    } //insertString()
    
}// DocumentFilter class



















