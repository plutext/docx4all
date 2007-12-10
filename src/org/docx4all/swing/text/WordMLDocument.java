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

import java.util.List;

import javax.swing.text.AttributeSet;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Element;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;

import org.apache.log4j.Logger;
import org.docx4all.xml.DocumentML;
import org.docx4all.xml.ElementML;
import org.docx4all.xml.ElementMLFactory;
import org.docx4all.xml.ParagraphML;
import org.docx4all.xml.RunContentML;
import org.docx4all.xml.RunML;

public class WordMLDocument extends DefaultStyledDocument {
	private static Logger log = Logger.getLogger(WordMLDocument.class);

	public final static String FILE_PATH_PROPERTY = "filePathProperty";
	
	public WordMLDocument() {
		super();
	}
	
	protected void createElementStructure(List<ElementSpec> list) {
		ElementSpec[] specs = new ElementSpec[list.size()];
		list.toArray(specs);
		super.create(specs);
	}
	
    /**
     * Creates the root element to be used to represent the
     * default document structure.
     *
     * @return the element base
     */
	@Override
    protected AbstractElement createDefaultRoot() {
		DocumentML docML = ElementMLFactory.createEmptyDocumentML();
		
		ParagraphML paraML = (ParagraphML) docML.getChild(0);
		RunML runML = null;
		for (ElementML elem: paraML.getChildren()) {
			if (elem instanceof RunML) {
				runML = (RunML) elem;
				break;
			}
		}
		
		RunContentML rcML = null;
		for (ElementML elem: runML.getChildren()) {
			if (elem instanceof RunContentML) {
				rcML = (RunContentML) elem;
				break;
			}
		}
		
		if (runML == null || rcML == null) {
			//Very unlikely but just in case
			throw new RuntimeException("Invalid default DocumentML");
		}
		
		writeLock();
		MutableAttributeSet a = new SimpleAttributeSet();
		
		//Document
		a.addAttribute(WordMLStyleConstants.ElementMLAttribute, docML);
		BlockElement document = new BlockElement(null, a.copyAttributes());
		a.removeAttributes(a);
		
		//Body
		//a.addAttribute(WordMLStyleConstants.ElementMLAttribute, bodyML);
		//BlockElement body = new BlockElement(document, a.copyAttributes());
		//a.removeAttributes(a);
		
		//Paragraph
		a.addAttribute(WordMLStyleConstants.ElementMLAttribute, paraML);
		//BlockElement paragraph = new BlockElement(body, a.copyAttributes());
		BlockElement paragraph = new BlockElement(document, a.copyAttributes());
		a.removeAttributes(a);
		
		//Dummy Paragraph
		a.addAttribute(WordMLStyleConstants.ElementMLAttribute, ElementML.DUMMY_PARAGRAPH);
		BlockElement dummyParagraph = new BlockElement(paragraph, a.copyAttributes());
		a.removeAttributes(a);
		
		//Run
		a.addAttribute(WordMLStyleConstants.ElementMLAttribute, runML);
		BlockElement run = new BlockElement(dummyParagraph, a.copyAttributes());
		a.removeAttributes(a);

		//Text
		a.addAttribute(WordMLStyleConstants.ElementMLAttribute, rcML);
		TextElement text = new TextElement(paragraph, a, 0, 1);

		Element[] buff = new Element[1];
		buff[0] = text;
		run.replace(0, 0, buff);
		
		buff[0] = run;
		dummyParagraph.replace(0, 0, buff);
		
		buff[0] = dummyParagraph;
		paragraph.replace(0, 0, buff);
		
		buff[0] = paragraph;
		document.replace(0, 0, buff);
		//body.replace(0, 0, buff);
		
		//buff[0] = body;
		//document.replace(0, 0, buff);
		
		writeUnlock();
		return document;
	}
	
    /**
     * Creates a document branch element, that can contain other elements.
     * This is implemented to return an element of type 
     * <code>WordMLDocument.BlockElement</code> or
     * <code>WordMLDocument.RunElement</code>
     *
     * @param parent the parent element
     * @param a the attributes
     * @return the element
     */
	@Override
    protected Element createBranchElement(Element parent, AttributeSet a) {
		return new BlockElement(parent, a);
	}

    /**
     * Creates a document leaf element that directly represents
     * text (doesn't have any children).  This is implemented
     * to return an element of type 
     * <code>WordMLDocument.TextElement</code>.
     *
     * @param parent the parent element
     * @param a the attributes for the element
     * @param p0 the beginning of the range (must be at least 0)
     * @param p1 the end of the range (must be at least p0)
     * @return the new element
     */
    protected Element createLeafElement(Element parent, AttributeSet a, int p0, int p1) {
    	return new TextElement(parent, a, p0, p1);
    }

	//============= INNER CLASS SECTION =============

    public class BlockElement extends BranchElement {
		public BlockElement(Element parent, AttributeSet a) {
			super(parent, a);
		}

		public String getName() {
			ElementML elem = getElementML();
			if (elem != null) {
				StringBuffer sb = new StringBuffer();
				sb.append("Tag <");
				sb.append(elem.getTag().getTagName());
				sb.append(">");
				return sb.toString();
			}
			return super.getName();
		}

		public ElementML getElementML() {
			return (ElementML) getAttribute(WordMLStyleConstants.ElementMLAttribute);
		}
		
		public AttributeSet getResolveParent() {
			return null;
		}
	}// BlockElement inner class

    public class TextElement extends LeafElement {
		public TextElement(Element parent, AttributeSet a, int offs0, int offs1) {
			super(parent, a, offs0, offs1);
		}

		public ElementML getElementML() {
			return (ElementML) getAttribute(WordMLStyleConstants.ElementMLAttribute);
		}
		
		public String getName() {
			ElementML elem = getElementML();
			if (elem != null) {
				StringBuffer sb = new StringBuffer();
				sb.append("Tag <");
				sb.append(elem.getTag().getTagName());
				sb.append(">");
				return sb.toString();
			}
			return super.getName();
		}

	}// TextElement inner class


}// WordMLDocument class



























