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

import org.docx4j.Namespaces;
import org.docx4j.document.wordprocessingml.Constants;
import org.docx4j.document.wordprocessingml.Paragraph;
import org.docx4j.document.wordprocessingml.Run;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.exceptions.InvalidFormatException;
import org.docx4j.openpackaging.io.LoadFromZipFile;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.PartName;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;
import org.dom4j.tree.DefaultElement;


/**
 *	@author Jojada Tirtowidjojo - 30/11/2007
 */
public class ElementMLFactory {
	public final static RunML IMPLIED_NEWLINE_RUNML;
	
	static {
		IMPLIED_NEWLINE_RUNML = createRunML(org.docx4all.ui.main.Constants.NEWLINE, true);
	}
	
	
	public final static DocumentML createEmptyDocumentML() {
		//====== Create Document ======
		Document doc = DocumentHelper.createDocument();
		Namespace nsWordprocessingML = new Namespace("w",
				"http://schemas.openxmlformats.org/wordprocessingml/2006/main");
		Element elDocument = doc.addElement(new QName("document",
				nsWordprocessingML));
		Element elBody = elDocument.addElement(new QName(Constants.WORD_DOC_BODY_TAG_NAME,
				Namespaces.namespaceWord));
		elBody.add(createParagraphElement(org.docx4all.ui.main.Constants.NEWLINE));
		
		//====== Create MainDocumentPart ======
		MainDocumentPart docPart = null;
		try {
			docPart = new MainDocumentPart( new PartName( "/word/document.xml" ));
		} catch (InvalidFormatException exc) {
			;//do nothing
		}
		docPart.setDocument(doc);
		return new DocumentML(docPart);
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
			MainDocumentPart documentPart = wordMLPackage.getMainDocumentPart();
			docML = new DocumentML(documentPart);
		} catch (Docx4JException exc) {
			throw new IOException(exc);
		}
		
		return docML;
	}
	
	public final static ParagraphML createParagraphML(String textContent, boolean isDummy) {
		Paragraph p = new Paragraph(createParagraphElement(textContent));
		return new ParagraphML(p, isDummy);
	}
	
	public final static RunML createRunML(String textContent, boolean isDummy) {
		Run r = new Run(createRunElement(textContent));
		return new RunML(r, isDummy);
	}
	
	private final static Element createParagraphElement(String textContent) {
		Element p = new DefaultElement(new QName(Constants.PARAGRAPH_BODY_TAG_NAME,
				Namespaces.namespaceWord));
		
		Element r = createRunElement(textContent);	

		p.content().add(r);
		
		return p;
	}
	
	private final static Element createRunElement(String textContent) {
		Element r = new DefaultElement(new QName(Constants.PARAGRAPH_RUN_TAG_NAME,
				Namespaces.namespaceWord));

		Element rcontent = null;
		if (org.docx4all.ui.main.Constants.NEWLINE.equals(textContent)) {
			rcontent = 
				new DefaultElement(
						new QName(Constants.CR, Namespaces.namespaceWord));
		} else {
			rcontent = 
				new DefaultElement(
						new QName(Constants.RUN_TEXT,	Namespaces.namespaceWord));
			rcontent.setText(textContent);		
		}
		r.content().add(rcontent);
		
		return r;
	}

	private ElementMLFactory() {
		;//uninstantiable
	}
}// ElementMLFactory class



























