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

package org.docx4all.xml;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.docx4j.document.wordprocessingml.Paragraph;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;
import org.dom4j.Element;

/**
 *	@author Jojada Tirtowidjojo - 30/11/2007
 */
public class DocumentML extends ElementML {
	private static Logger log = Logger.getLogger(DocumentML.class);
	
	private final MainDocumentPart doc;
	
	public DocumentML(MainDocumentPart doc) {
		this.doc = doc;
		this.tag = WordML.Tag.DOCUMENT;
		
		initChildren();
	}

	/**
	 * An implied ElementML is an ElementML that
	 * does not have a DOM element associated with it.
	 * This kind of ElementML may still have a WordML.Tag.
	 * 
	 * @return true, if this is an implied ElementML
	 *         false, otherwise
	 */
	public boolean isImplied() {
		return this.doc == null;
	}

	private void initChildren() {
		List bodyKids = doc.getBody();
		if (!bodyKids.isEmpty()) {
			this.children = new ArrayList<ElementML>(bodyKids.size());
			
			for (Object o : bodyKids) {
				// TODO: Currently 'bodyKids' may contain:
				// org.docx4j.document.wordprocessingml.Paragraph
				// and/or org.dom4j.Element objects
				// Watch any future change in MainDocumentPart class
				ParagraphML p;
				if (o instanceof Paragraph) {
					p = new ParagraphML((Paragraph) o);
				} else {
					p = createDummyParagraph((Element) o);
				}

				p.setParent(DocumentML.this);
				children.add(p);
			}// for (Object o:)
		}// if (!bodyKids.isEmpty())
	}// initChildren()
	
	private ParagraphML createDummyParagraph(Element unsupportedElem) {
		//Create a Dummy Paragraph that contains a piece of information
		//about this unsupported element
		//TODO: A more informative text content in the dummy paragraph.
		StringBuffer text = new StringBuffer();
		text.append("UNSUPPORTED '");
		text.append(unsupportedElem.getName());
		text.append("' tag element");
		
		return ElementMLFactory.createParagraphML(text.toString(), true);
	}
	

}// DocumentML class




















