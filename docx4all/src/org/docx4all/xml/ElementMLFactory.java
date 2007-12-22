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

import java.io.File;
import java.io.IOException;

import org.docx4j.openpackaging.parts.relationships.Namespaces;
import org.docx4j.jaxb.document.ObjectFactory;
import org.docx4j.openpackaging.URIHelper;
import org.docx4j.openpackaging.contenttype.ContentType;
import org.docx4j.openpackaging.contenttype.ContentTypeManager;
import org.docx4j.openpackaging.contenttype.ContentTypeManagerImpl;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.exceptions.InvalidFormatException;
import org.docx4j.openpackaging.io.LoadFromZipFile;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.Part;
import org.docx4j.openpackaging.parts.PartName;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;
import org.docx4j.openpackaging.parts.relationships.RelationshipsPart;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;


/**
 *	@author Jojada Tirtowidjojo - 30/11/2007
 */
public class ElementMLFactory {
	private final static ObjectFactory _jaxbFactory = new ObjectFactory();
	
	public final static ObjectFactory getJaxbObjectFactory() {
		return _jaxbFactory;
	}
	
	public final static DocumentML createEmptyDocumentML() {
		WordprocessingMLPackage docPackage = createEmptyDocumentPackage();
		return new DocumentML(docPackage);
	}
	
	public final static DocumentML createDocumentML(File f) throws IOException {
		if (f == null || !f.getName().endsWith(".docx")) {
			throw new IllegalArgumentException("Not a .docx file."); 
		}
		
		DocumentML docML = null;
		try {
			LoadFromZipFile loader = new LoadFromZipFile();
			WordprocessingMLPackage wordMLPackage = 
				(WordprocessingMLPackage) loader.get(f);
			docML = new DocumentML(wordMLPackage);
		} catch (Docx4JException exc) {
			throw new IOException(exc);
		}
		
		return docML;
	}
	
	public final static ParagraphML createParagraphML(String textContent, boolean isDummy) {
		org.docx4j.jaxb.document.P p = createP(textContent);
		return new ParagraphML(p, isDummy);
	}
	
	public final static RunML createRunML(String textContent, boolean isDummy) {
		org.docx4j.jaxb.document.R r = createR(textContent);
		return new RunML(r, isDummy);
	}
	
	private final static org.docx4j.jaxb.document.P createP(String textContent) {
		org.docx4j.jaxb.document.P p = _jaxbFactory.createP();
		org.docx4j.jaxb.document.R r = createR(textContent);
		p.getParagraphContent().add(r);
		r.setParent(p);
		return p;
	}
	
	private final static org.docx4j.jaxb.document.R createR(String textContent) {
		org.docx4j.jaxb.document.R r = _jaxbFactory.createR();
		
		if (org.docx4all.ui.main.Constants.NEWLINE.equals(textContent)) {
			org.docx4j.jaxb.document.Cr cr = _jaxbFactory.createCr();
			r.getRunContent().add(cr);
			cr.setParent(r);
		} else {
			org.docx4j.jaxb.document.Text text = _jaxbFactory.createText();
			text.setValue(textContent);
			r.getRunContent().add(text);
			text.setParent(r);
		}
		return r;
	}
	
	private final static WordprocessingMLPackage createEmptyDocumentPackage() {
		// Create a package
		WordprocessingMLPackage thePackage = new WordprocessingMLPackage();

		// Add a ContentTypeManager to it
		ContentTypeManager ctm = new ContentTypeManagerImpl();
		thePackage.setContentTypeManager(ctm);

		// Create main document part content
		org.docx4j.jaxb.document.ObjectFactory factory = new org.docx4j.jaxb.document.ObjectFactory();

		org.docx4j.jaxb.document.Text  t = factory.createText();
		t.setValue(org.docx4all.ui.main.Constants.NEWLINE);

		org.docx4j.jaxb.document.R  run = factory.createR();
		run.getRunContent().add(t);		
		
		org.docx4j.jaxb.document.P  para = factory.createP();
		para.getParagraphContent().add(run);
		
		org.docx4j.jaxb.document.Body  body = factory.createBody();
		body.getBlockLevelElements().add(para);
		
		org.docx4j.jaxb.document.Document wmlDocumentEl = factory.createDocument();
		wmlDocumentEl.setBody(body);		

		try {
			// Create main document part
			Part corePart = new org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart(new PartName("/word/document.xml"));
			
			// Put the content in the part
			((org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart)corePart).setDocumentObj(wmlDocumentEl);
			
			corePart.setContentType(new  org.docx4j.openpackaging.contenttype.ContentType( org.docx4j.openpackaging.contenttype.ContentTypes.WORDPROCESSINGML_DOCUMENT));
			corePart.setRelationshipType(Namespaces.DOCUMENT);
			
			// Make getMainDocumentPart() work
			thePackage.setPartShortcut(corePart, corePart.getRelationshipType());
			
			// Create the PackageRelationships part	
			RelationshipsPart rp = new RelationshipsPart( new PartName("/_rels/.rels"), thePackage );
			
			// Make sure content manager knows how to handle .rels
			ctm.addDefaultContentType("rels", org.docx4j.openpackaging.contenttype.ContentTypes.RELATIONSHIPS_PART);
			
			// Add it to the package
			thePackage.setRelationships(rp);

			// Add it to the collection of parts
			rp.addPart(corePart, thePackage.getContentTypeManager());
		} catch (InvalidFormatException exc) {
			;// do nothing
		}

		return thePackage;
	}
	
	private ElementMLFactory() {
		;//uninstantiable
	}
}// ElementMLFactory class



























