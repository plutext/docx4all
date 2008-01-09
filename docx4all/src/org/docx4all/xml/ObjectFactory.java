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

import javax.swing.text.StyleConstants;
import javax.xml.bind.JAXBElement;

import org.docx4all.ui.main.Constants;
import org.docx4j.jaxb.document.BooleanDefaultTrue;
import org.docx4j.jaxb.document.P;
import org.docx4j.jaxb.document.STJc;
import org.docx4j.jaxb.document.Text;
import org.docx4j.openpackaging.contenttype.ContentTypeManager;
import org.docx4j.openpackaging.contenttype.ContentTypeManagerImpl;
import org.docx4j.openpackaging.exceptions.InvalidFormatException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.Part;
import org.docx4j.openpackaging.parts.PartName;
import org.docx4j.openpackaging.parts.relationships.Namespaces;
import org.docx4j.openpackaging.parts.relationships.RelationshipsPart;


/**
 *	@author Jojada Tirtowidjojo - 02/01/2008
 */
public class ObjectFactory {
	private final static org.docx4j.jaxb.document.ObjectFactory _jaxbFactory = 
		new org.docx4j.jaxb.document.ObjectFactory();

	public final static JAXBElement<P> createPara(String textContent) {
		org.docx4j.jaxb.document.P p = createP(textContent);
		return _jaxbFactory.createP(p);
	}
	
	public final static org.docx4j.jaxb.document.P createP(String textContent) {
		org.docx4j.jaxb.document.P p = _jaxbFactory.createP();
		if (textContent != null) {
			org.docx4j.jaxb.document.R r = createR(textContent);
			p.getParagraphContent().add(r);
			r.setParent(p);
		}
		return p;
	}
	
	public final static org.docx4j.jaxb.document.R createR(String textContent) {
		org.docx4j.jaxb.document.R r = _jaxbFactory.createR();
		
		if (org.docx4all.ui.main.Constants.NEWLINE.equals(textContent)) {
			org.docx4j.jaxb.document.Cr cr = _jaxbFactory.createCr();
			r.getRunContent().add(cr);
			cr.setParent(r);
		} else {
			org.docx4j.jaxb.document.Text text = _jaxbFactory.createText();
			text.setValue(textContent);
			r.getRunContent().add(_jaxbFactory.createT(text));
			text.setParent(r);
		}
		return r;
	}
	
	public final static JAXBElement<Text> createT(String textContent) {
		org.docx4j.jaxb.document.Text text = _jaxbFactory.createText();
		text.setValue(textContent);
		return _jaxbFactory.createT(text);
	}
	
	public final static WordprocessingMLPackage createDocumentPackage(
			org.docx4j.jaxb.document.Document doc) {

		// Create a package
		WordprocessingMLPackage thePackage = new WordprocessingMLPackage();

		// Add a ContentTypeManager to it
		ContentTypeManager ctm = new ContentTypeManagerImpl();
		thePackage.setContentTypeManager(ctm);

		try {
			// Create main document part
			Part corePart = new org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart(
					new PartName("/word/document.xml"));

			// Put the content in the part
			((org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart) corePart)
					.setJaxbElement(doc);

			corePart.setContentType(new org.docx4j.openpackaging.contenttype.ContentType(
							org.docx4j.openpackaging.contenttype.ContentTypes.WORDPROCESSINGML_DOCUMENT));
			corePart.setRelationshipType(Namespaces.DOCUMENT);

			// Make getMainDocumentPart() work
			thePackage.setPartShortcut(corePart, corePart.getRelationshipType());

			// Create the PackageRelationships part
			RelationshipsPart rp = new RelationshipsPart(new PartName(
					"/_rels/.rels"), thePackage);

			// Make sure content manager knows how to handle .rels
			ctm.addDefaultContentType(
					"rels",
					org.docx4j.openpackaging.contenttype.ContentTypes.RELATIONSHIPS_PART);

			// Add it to the package
			thePackage.setRelationships(rp);

			// Add it to the collection of parts
			rp.addPart(corePart, thePackage.getContentTypeManager());
		} catch (InvalidFormatException exc) {
			;// do nothing
		}

		return thePackage;

	}
	
	public final static WordprocessingMLPackage createEmptyDocumentPackage() {
		org.docx4j.jaxb.document.P  para = createP(Constants.NEWLINE);
		
		org.docx4j.jaxb.document.Body  body = _jaxbFactory.createBody();
		body.getBlockLevelElements().add(_jaxbFactory.createP(para));
		para.setParent(body);
		
		org.docx4j.jaxb.document.Document doc = _jaxbFactory.createDocument();
		doc.setBody(body);
		body.setParent(doc);

		return createDocumentPackage(doc);
	}
	
	public final static org.docx4j.jaxb.document.Jc createJc(Integer align) {
		org.docx4j.jaxb.document.Jc theJc = null;
		
        if (align != null) {
        	theJc = _jaxbFactory.createJc();
			if (align.intValue() == StyleConstants.ALIGN_LEFT) {
				theJc.setVal(STJc.LEFT);
			} else if (align.intValue() == StyleConstants.ALIGN_RIGHT) {
				theJc.setVal(STJc.RIGHT);
			} else if (align.intValue() == StyleConstants.ALIGN_CENTER) {
				theJc.setVal(STJc.CENTER);
			} else if (align.intValue() == StyleConstants.ALIGN_JUSTIFIED) {
				theJc.setVal(STJc.BOTH);
			} else {
				theJc = null;
			}
		}

        return theJc;
	}
	
	public final static org.docx4j.jaxb.document.BooleanDefaultTrue createBooleanDefaultTrue(Boolean b) {
		BooleanDefaultTrue bdt = _jaxbFactory.createBooleanDefaultTrue();
		bdt.setVal(b);
		return bdt;
	}
	
	public final static org.docx4j.jaxb.document.Underline createUnderline(String value, String color) {
		org.docx4j.jaxb.document.Underline u = _jaxbFactory.createUnderline();
		u.getVal().add(value);
		u.setColor(color);
		return u;
	}
	
	private ObjectFactory() {
		;//uninstantiable
	}
}// ObjectFactory class



















