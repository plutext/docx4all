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

import org.docx4all.swing.text.WordMLFragment.ElementMLRecord;
import org.docx4all.util.XmlUtil;
import org.docx4all.xml.ElementML;
import org.docx4all.xml.ElementMLFactory;
import org.docx4all.xml.ElementMLIterator;
import org.docx4all.xml.ObjectFactory;
import org.docx4all.xml.ParagraphML;
import org.docx4all.xml.RunContentML;
import org.docx4all.xml.RunML;
import org.docx4all.xml.RunPropertiesML;
import org.docx4all.xml.SdtBlockML;

/**
 *	@author Jojada Tirtowidjojo - 17/04/2008
 */
public class ElementSelector {
	private DocumentElement _elem;
	private int _startOffset, _endOffset;
	
	public ElementSelector(DocumentElement elem, int startOffset, int endOffset) {
		if (elem.getStartOffset() <= startOffset
			&& endOffset <= elem.getEndOffset()) {
			this._elem = elem;
			this._startOffset = startOffset;
			this._endOffset = endOffset;
		} else {
			throw new IllegalArgumentException(
				"[startOffset, endOffset]=[" + startOffset + "," + endOffset + "]");
		}
	}
	
	/**
	 * Returns a list of DocumentElements that lie within this instance's selection area; 
	 * ie: [_startOffset, _endOffset].
	 * 
	 * In general DocumentElements that fully lie within the selection area will be 
	 * included in the returned list. An exception is given to DocumentElement that
	 * holds an implied ParagraphML.
	 *  
	 * A DocumentElement that holds an implied ParagraphML is replaced with 
	 * the list of DocumentElements obtained from processing each of its children
	 * regardless whether it fully or partially lies within the selection area.
	 * 
	 * A block/non-leaf DocumentElement that partially lies within the selection area
	 * will be replaced with the list of DocumentElements obtained from processing 
	 * each of its children.
	 * 
	 * A leaf DocumentElement that partially lies within the selection area is treated
	 * as if it did not. This means it will be included in the returned list.
	 * 
	 * @return a List<DocumentElement> whose members lie within instance's selection area.  
	 */
	public List<DocumentElement> getDocumentElements() {
		List<DocumentElement> theList = selectElements(_elem, _startOffset, _endOffset);
		return theList;
	}
	
	
	/**
	 * Returns a list of DocumentElements that lie within the selection area as defined by
	 * 'start' and 'end' arguments.
	 * 
	 * In general DocumentElements that fully lie within the selection area will be 
	 * included in the returned list. An exception is given to DocumentElement that
	 * holds an implied ParagraphML.
	 *  
	 * A DocumentElement that holds an implied ParagraphML is replaced with 
	 * the list of DocumentElements obtained from processing each of its children
	 * regardless whether it fully or partially lies within the selection area.
	 * 
	 * A block/non-leaf DocumentElement that partially lies within the selection area
	 * will be replaced with the list of DocumentElements obtained from processing 
	 * each of its children.
	 * 
	 * A leaf DocumentElement that partially lies within the selection area is treated
	 * as if it did not. This means it will be included in the returned list.
	 * 
	 * @param start the start offset position in the document.
	 * @param end the end offset position in the document.
	 * 
	 * @return a List<DocumentElement> whose members lie within instance's selection area.  
	 */
	private List<DocumentElement> selectElements(DocumentElement elem, int start, int end) {
		List<DocumentElement> theElements = null;
		
		start = Math.max(start, elem.getStartOffset());
		end = Math.min(end, elem.getEndOffset());
		
		if (start <= elem.getStartOffset() && elem.getEndOffset() <= end) {
			//elem is fully selected
			ElementML ml = elem.getElementML();
			if ((ml instanceof ParagraphML && ml.isImplied())
				|| (ml instanceof SdtBlockML)) {
				theElements = selectElementsFromChildren(elem, start, end);
			} else {
				theElements = new ArrayList<DocumentElement>(1);
				theElements.add(elem);
			}
		} else if (!elem.isLeaf()) {
			//partially selected block
			theElements = selectElementsFromChildren(elem, start, end);
			
		} else {
			//partially selected leaf
			theElements = new ArrayList<DocumentElement>(1);
			theElements.add(elem);
		}
		
		return theElements;
	}
	
	private List<DocumentElement> selectElementsFromChildren(DocumentElement elem, int start, int end) {
		List<DocumentElement> theChildren = new ArrayList<DocumentElement>();
		
		start = Math.max(start, elem.getStartOffset());
		end = Math.min(end, elem.getEndOffset());
		
		int startIdx = elem.getElementIndex(start);
		int endIdx = elem.getElementIndex(end - 1);
		
		while (startIdx <= endIdx) {
			DocumentElement child = (DocumentElement) elem.getElement(startIdx++);
			theChildren.addAll(selectElements(child, start, end));
		}
		
		return theChildren;
	}
	

	public List<ElementMLRecord> getElementMLRecords() {
		return selectType(ElementMLRecord.class);
	}
	
	/**
	 * This method is used internally for creating copies of ElementML or 
	 * generating ElementMLRecord objects out of DocumentElements that lie
	 * within this instance's selection area; ie: [_startOffset, _endOffset]. 
	 * 
	 * @param returnedType This is either ElementML.class or ElementMLRecord.class.
	 * 
	 * @return a list of Objects whose class is determined by 'returnedType' argument. 
	 */
	private <T> List<T> selectType(Class<T> returnedType) {
		List<T> theList = new ArrayList<T>();
		
		ElementML elemML = _elem.getElementML();
		
		if (elemML.isImplied()) {
			if (elemML instanceof ParagraphML) {
				int startIdx = _elem.getElementIndex(_startOffset);
				int endIdx = _elem.getElementIndex(_endOffset - 1);
				for (int i=startIdx; i <= endIdx; i++) {
					DocumentElement child = 
						(DocumentElement) _elem.getElement(i);
					int x = Math.max(child.getStartOffset(), _startOffset);
					int y = Math.min(child.getEndOffset(), _endOffset);
					ElementSelector es = new ElementSelector(child, x, y);
					theList.addAll(es.selectType(returnedType));
				}				
			} else {
				;//Exclude all implied ElementML(s) except for implied ParagraphML
			}
		} else if (elemML instanceof SdtBlockML) {
			int startIdx = _elem.getElementIndex(_startOffset);
			int endIdx = _elem.getElementIndex(_endOffset - 1);
			for (int i=startIdx; i <= endIdx; i++) {
				DocumentElement child = 
					(DocumentElement) _elem.getElement(i);
				int x = Math.max(child.getStartOffset(), _startOffset);
				int y = Math.min(child.getEndOffset(), _endOffset);
				ElementSelector es = new ElementSelector(child, x, y);
				theList.addAll(es.selectType(returnedType));
			}				
			
		} else if (_startOffset == _elem.getStartOffset()
					&& _elem.getEndOffset() == _endOffset) {
			// _elem is fully selected 
			theList.add(copyFully(_elem, returnedType));
		
		} else if (_startOffset == _elem.getStartOffset()) {
			// _elem is partially selected; ie: _endOffset < _elem.getEndOffset()
			theList.add(copyPartially(_elem, _endOffset, returnedType));
			
		} else {
			// _elem is partially selected and
			// _elem is hosting selection range (_startOffset, _endOffset]
			if (elemML instanceof RunContentML) {
				ElementML ml = copyRunContentML(_elem, _startOffset, _endOffset);
				theList.add(createReturnedObject(ml, true, returnedType));
				
			} else if (elemML instanceof RunML) {
				ElementML ml = copyRunML(_elem, _startOffset, _endOffset);
				theList.add(createReturnedObject(ml, true, returnedType));
				
			} else {
				int startIdx = _elem.getElementIndex(_startOffset);
				int endIdx = _elem.getElementIndex(_endOffset - 1);
				for (int i = startIdx; i <= endIdx; i++) {
					DocumentElement child = 
						(DocumentElement) _elem.getElement(i);
					int x = Math.max(child.getStartOffset(), _startOffset);
					int y = Math.min(child.getEndOffset(), _endOffset);
					ElementSelector es = new ElementSelector(child, x, y);
					theList.addAll(es.selectType(returnedType));
				}
			}
		}
		return theList;
	}
	
	private <T> T createReturnedObject(ElementML elem, boolean isFragmented, Class<T> returnedType) {
		T obj = null;
		if (elem != null && returnedType == ElementMLRecord.class) {
			obj = (T) new ElementMLRecord(elem, isFragmented);
		} else if (elem != null && returnedType == ElementML.class){
			obj = (T) elem;
		} else {
			throw new IllegalArgumentException("elem=" + elem + " returnedType=" + returnedType.getSimpleName());
		}
		return obj;		
	}
	
	private <T> T copyFully(DocumentElement elem, Class<T> returnedType) {
		ElementML copyML = (ElementML) elem.getElementML().clone();
		return createReturnedObject(copyML, false, returnedType);
	}
	
	private <T> T copyPartially(
		DocumentElement elem, 
		int endOffset, 
		Class<T> returnedType) {
		
		WordMLDocument doc = (WordMLDocument) elem.getDocument();
		ElementML elemML = elem.getElementML();
		
		DocumentElement runContentE = 
			(DocumentElement) doc.getCharacterElement(endOffset - 1);
		RunContentML rcml = (RunContentML) runContentE.getElementML();
		String text = rcml.getTextContent();
		try {
			int offs = runContentE.getStartOffset();
			int len = endOffset - offs;
			text = doc.getText(offs, len);
		} catch (BadLocationException exc) {
			;// shouldn't happen
		}
		int idx = XmlUtil.getIteratedIndex(elemML, rcml);

		ElementML copyML = (ElementML) elemML.clone();

		List<ElementML> elemsToDelete = new ArrayList<ElementML>();
		ElementMLIterator it = new ElementMLIterator(copyML);
		int i = -1;
		while (it.hasNext()) {
			ElementML eml = it.next();
			i++;
			if (i == idx) {
				((RunContentML) eml).setTextContent(text);
			} else if (idx < i) {
				elemsToDelete.add(eml);
			}
		}

		if (!elemsToDelete.isEmpty()) {
			for (ElementML eml : elemsToDelete) {
				eml.delete();
			}
		}
		elemsToDelete = null;

		return createReturnedObject(copyML, true, returnedType);
	}
	
	private RunML copyRunML(DocumentElement runE, int start, int end) {
		RunML runML = (RunML) runE.getElementML();
		
    	RunPropertiesML rPr = (RunPropertiesML) runML.getRunProperties();
    	if (rPr != null) {
    		rPr = (RunPropertiesML) rPr.clone();
    	}
    	
		int startIdx = _elem.getElementIndex(_startOffset);
		int endIdx = _elem.getElementIndex(_endOffset - 1);
    	List<ElementML> children = 
    		new ArrayList<ElementML>(endIdx - startIdx + 1);
		for (int i = startIdx; i <= endIdx; i++) {
			DocumentElement child = 
				(DocumentElement) _elem.getElement(i);
			int x = Math.max(child.getStartOffset(), _startOffset);
			int y = Math.min(child.getEndOffset(), _endOffset);
			ElementSelector es = new ElementSelector(child, x, y);
			children.addAll(es.selectType(ElementML.class));
		}

		runML = ElementMLFactory.createRunML(children, rPr);
		return runML;
	}
	
	private RunContentML copyRunContentML(DocumentElement runContentE, int start, int end) {
		RunContentML runContentML = null;
		
		try {
			WordMLDocument doc = (WordMLDocument) runContentE.getDocument();
			String text = doc.getText(_startOffset, _endOffset - _startOffset);
			runContentML = new RunContentML(ObjectFactory.createT(text));
		} catch (BadLocationException exc) {
			;// ignore
		}
		
		return runContentML;
	}
	
}// ElementSelector class



















