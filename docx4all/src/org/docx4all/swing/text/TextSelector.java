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

import java.util.ArrayList;
import java.util.List;

import javax.swing.text.BadLocationException;
import javax.swing.text.Element;

import org.apache.log4j.Logger;
import org.docx4all.swing.text.WordMLFragment.ElementMLRecord;
import org.docx4all.util.DocUtil;
import org.docx4all.xml.ElementML;
import org.docx4all.xml.ElementMLFactory;
import org.docx4all.xml.ObjectFactory;
import org.docx4all.xml.ParagraphML;
import org.docx4all.xml.ParagraphPropertiesML;
import org.docx4all.xml.RunContentML;
import org.docx4all.xml.RunML;
import org.docx4all.xml.RunPropertiesML;

/**
 *	@author Jojada Tirtowidjojo - 19/12/2007
 */
public class TextSelector {
	private static Logger log = Logger.getLogger(TextSelector.class);

	protected final WordMLDocument doc;
	protected final int offset, length;
	
	public TextSelector(WordMLDocument doc, int offset, int length) throws BadSelectionException {
		select(doc, offset, offset + length);
		this.doc = doc;
		this.offset = offset;
		this.length = length;
	}
	
	public int getOffset() {
		return offset;
	}
	
	public int getLength() {
		return length;
	}
	
	public WordMLDocument getDocument() {
		return doc;
	}
	
	public String getText() {
		String theText = null;
		try {
			theText = doc.getText(offset, length);
		} catch (BadLocationException exc) {
			;//ignore
		}
		return theText;
	}
	
	public List<ElementMLRecord> getElementMLRecords() {
		List<ElementMLRecord> theList = new ArrayList<ElementMLRecord>();
		
		DocumentElement root = (DocumentElement) doc.getDefaultRootElement();
		
		int startIdx = root.getElementIndex(offset);
		int endIdx = root.getElementIndex(offset + length - 1);
		
		for (int idx=startIdx; idx <= endIdx; idx++) {
			DocumentElement para = (DocumentElement) root.getElement(idx);
			if (isFullySelected(para)) {
				ElementML ml = (ElementML) para.getElementML().clone();
				ElementMLRecord rec = new ElementMLRecord(ml, false);
				theList.add(rec);
				
			} else if (idx == startIdx) {
				//The first partially selected paragraph
				theList.addAll(
					getParagraphContents(
						para, offset, length, ElementMLRecord.class));
				
			} else {
				//The last partially selected paragraph
				ElementML tempML = para.getElementML();
	        	ParagraphPropertiesML pPr =
	        		(ParagraphPropertiesML) 
	        		((ParagraphML) tempML).getParagraphProperties();
	        	if (pPr != null) {
	        		pPr = (ParagraphPropertiesML) pPr.clone();
	        	}
	        	
	        	DocumentElement runE = 
	        		(DocumentElement) doc.getRunMLElement(offset + length - 1);
	        	tempML = runE.getElementML();
	        	RunPropertiesML rPr = 
	        		(RunPropertiesML)
	        		((RunML) tempML).getRunProperties();
	        	if (rPr != null) {
	        		rPr = (RunPropertiesML) rPr.clone();
	        	}
	        	
				List<ElementML> children = 
					getParagraphContents(para, offset, length, ElementML.class);
	        	tempML = 
	        		ElementMLFactory.createParagraphML(children, pPr, rPr);
	        	ElementMLRecord rec = new ElementMLRecord(tempML, true);
				theList.add(rec);
			}
		} //for (idx) loop
		
		return theList;
	}
	
	private <T> List<T> getParagraphContents(
			DocumentElement paraE,
			int offset,
			int length,
			Class<T> returnedType) {
		
		List<T> theList = new ArrayList<T>();
		
		int startIdx = paraE.getElementIndex(Math.max(offset, paraE.getStartOffset()));
		int endIdx = paraE.getElementIndex(Math.min(offset + length - 1, paraE.getEndOffset() - 1));
		
		while (startIdx <= endIdx) {
			DocumentElement dummyPara = 
				(DocumentElement) paraE.getElement(startIdx++);
			theList.addAll(
				getImpliedParagraphContents(
					dummyPara, offset, length, returnedType));
		}
		
		return theList;
	}
	
	private <T> List<T> getImpliedParagraphContents(
			DocumentElement impliedParaE,
			int offset,
			int length,
			Class<T> returnedType) {
		
			List<T> theList = new ArrayList<T>();
			
			int startIdx = impliedParaE.getElementIndex(Math.max(offset, impliedParaE.getStartOffset()));
			int endIdx = impliedParaE.getElementIndex(Math.min(offset + length - 1, impliedParaE.getEndOffset() - 1));
			
			for (int idx=startIdx; idx <= endIdx; idx++) {
				DocumentElement run = (DocumentElement) impliedParaE.getElement(idx);
				RunML runML = (RunML) run.getElementML();
				
				if (offset <= run.getStartOffset()
					&& run.getEndOffset() <= offset + length) {
					if (!runML.isImplied()) {
						T obj = null;
						ElementML ml = (ElementML) runML.clone();
						if (returnedType == ElementMLRecord.class) {
							obj = (T) new ElementMLRecord(ml, false);
						} else if (returnedType == ElementML.class) {
							obj = (T) ml;
						}
						theList.add(obj);
					}
				//} else if (idx == startIdx) {
					//The first partially selected run
					//theList.addAll(getRunContents(run, offset, length, returnedType));
					
				} else {
					//partially selected run
		        	RunPropertiesML rPr = 
		        		(RunPropertiesML) runML.getRunProperties();
		        	if (rPr != null) {
		        		rPr = (RunPropertiesML) rPr.clone();
		        	}
		        	
					List<ElementML> children = 
						getRunContents(run, offset, length, ElementML.class);
		        	ElementML newRunML = 
		        		ElementMLFactory.createRunML(children, rPr);
		        	
					T obj = null;;
					if (returnedType == ElementMLRecord.class) {
						obj = (T) new ElementMLRecord(newRunML, true);
					} else if (returnedType == ElementML.class) {
						obj = (T) newRunML;
					}
					theList.add(obj);
				}
			} //for (idx) loop
			
			return theList;
		}
		
	private <T> List<T> getRunContents(DocumentElement runE, int offset,
			int length, Class<T> returnedType) {

		List<T> theList = new ArrayList<T>();

		int startIdx = runE.getElementIndex(Math.max(offset, runE
				.getStartOffset()));
		int endIdx = runE.getElementIndex(Math.min(offset + length - 1, runE
				.getEndOffset() - 1));

		for (int idx = startIdx; idx <= endIdx; idx++) {
			DocumentElement leafE = (DocumentElement) runE.getElement(idx);
			RunContentML leafML = (RunContentML) leafE.getElementML();

			if (offset <= leafE.getStartOffset()
					&& leafE.getEndOffset() <= offset + length) {
				if (!leafML.isImplied()) {
					T obj = null;
					ElementML ml = (ElementML) leafML.clone();
					if (returnedType == ElementMLRecord.class) {
						obj = (T) new ElementMLRecord(ml, false);
					} else {
						obj = (T) ml;
					}
					theList.add(obj);
				}
			} else {
				// partially selected leaf
				try {
					int x = Math.max(offset, leafE.getStartOffset());
					int y = Math.min(offset + length, leafE.getEndOffset());
					String text = doc.getText(x, y - x);
					RunContentML rcml = new RunContentML(ObjectFactory
							.createT(text));

					T obj = null;
					if (returnedType == ElementMLRecord.class) {
						obj = (T) new ElementMLRecord(rcml, true);
					} else {
						obj = (T) rcml;
					}
					theList.add(obj);
				} catch (BadLocationException exc) {
					;// ignore
				}
			}
		} // for (idx) loop

		return theList;
	}
		
	public List<DocumentElement> getDocumentElements() {
		DocumentElement root = (DocumentElement) doc.getDefaultRootElement();
		return getChildren(root, offset, length);
	}
	
    public boolean isFullySelected(Element elem) {
    	return (offset <= elem.getStartOffset() 
    			&& elem.getEndOffset() <= offset + length);
    }

	private List<DocumentElement> getChildren(DocumentElement elem, int offset, int length) {
		List<DocumentElement> theChildren = new ArrayList<DocumentElement>();
		
		int startIdx = elem.getElementIndex(Math.max(offset, elem.getStartOffset()));
		int endIdx = elem.getElementIndex(Math.min(offset + length - 1, elem.getEndOffset() - 1));
		
		while (startIdx <= endIdx) {
			DocumentElement child = (DocumentElement) elem.getElement(startIdx++);
			ElementML childML = child.getElementML();
				
			if (offset <= child.getStartOffset() 
	    			&& child.getEndOffset() <= offset + length) {
				//child is fully selected
				if (childML instanceof ParagraphML && childML.isImplied()) {
					theChildren.addAll(getChildren(child, offset, length));
				} else {
					theChildren.add(child);
				}
			} else if (!child.isLeaf()) {
				//partially selected block
				theChildren.addAll(getChildren(child, offset, length));
				
			} else {
				//partially selected leaf
				theChildren.add(child);
			}
		}
		
		return theChildren;
	}
	
	private void select(WordMLDocument doc, int p0, int p1) throws BadSelectionException {
    	if (p0 >= p1) {
    		throw new BadSelectionException("Bad Selection", p0, p1-p0);
    	}
    	
    	DocumentElement firstLeaf = (DocumentElement) doc.getCharacterElement(p0);
    	
		if (log.isDebugEnabled()) {
			log.debug("select(): [p0, p1] = [" + p0 + ", " + p1 + "]");
			log.debug("select(): Leaf Element at p0 = " + firstLeaf);
		}
		
		if (firstLeaf.getStartOffset() < p0 && !firstLeaf.isEditable()) {
			firstLeaf = null;
			throw new BadSelectionException("Bad Start Position", p0, p1 - p0);
		}

		DocumentElement lastLeaf = (DocumentElement) doc.getCharacterElement(p1 - 1);

		if (log.isDebugEnabled()) {
			log.debug("select(): [p0, p1] = [" + p0 + ", " + p1 + "]");
			log.debug("select(): Leaf Element at p1 = " + lastLeaf);
		}

		if (p1 < lastLeaf.getEndOffset() && !lastLeaf.isEditable()) {
			lastLeaf = null;
			throw new BadSelectionException("Bad End Position", p0, p1 - p0);
		}

		if (firstLeaf != lastLeaf
			&& (firstLeaf.getStartOffset() != p0
					|| lastLeaf.getEndOffset() != p1)) {
			DocumentElement root = 
				(DocumentElement) doc.getDefaultRootElement();
			List<String> path0 = DocUtil.getElementNamePath(root, p0);
			List<String> path1 = DocUtil.getElementNamePath(root, p1);

			if (path0 != null && !path0.equals(path1)) {
				throw new BadSelectionException("Bad Selection", p0, p1 - p0);
			}
		}
    }
    
}// TextSelector class



















