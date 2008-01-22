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
import java.util.List;

import org.docx4j.wml.Document;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.io.LoadFromZipFile;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;


/**
 *	@author Jojada Tirtowidjojo - 30/11/2007
 */
public class ElementMLFactory {
	public final static DocumentML createEmptyDocumentML() {
		WordprocessingMLPackage docPackage = ObjectFactory.createEmptyDocumentPackage();
		return new DocumentML(docPackage);
	}
	
	public final static DocumentML createDocumentML(Document doc) {
		WordprocessingMLPackage docPackage = ObjectFactory.createDocumentPackage(doc);
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
	

	/**
	 * Creates a ParagraphML whose children are specified in 'contents' param.
	 * 
	 * @param contents A list of RunContentML and/or RunML objects.
	 * @param newPPr A ParagraphPropertiesML given to the resulting new ParagraphML
	 * @param newRPr A RunPropertiesML given to a newly created RunML. 
	 * A RunML is newly created if 'contents' parameter contains RunContentML.
	 * @return the newly created ParagraphML
	 */
	public final static ParagraphML createParagraphML(
		List<ElementML> contents,
		ParagraphPropertiesML newPPr,
		RunPropertiesML newRPr) {
		
    	ParagraphML thePara = new ParagraphML(ObjectFactory.createPara(null));
    	thePara.setParagraphProperties(newPPr);
    	
		RunML newRunML = null;

		for (ElementML ml : contents) {
			if (ml instanceof RunContentML) {
				// collect in one new RunML
				if (newRunML == null) {
					newRunML = new RunML(ObjectFactory.createR(null));
					newRunML.setRunProperties(newRPr);
					thePara.addChild(newRunML);
				}
				newRunML.addChild(ml);
			} else {
				if (newRunML != null) {
					newRunML = null;
				}
				thePara.addChild(ml);
			}
		}

		return thePara;
	}
	
	/**
	 * Creates a RunML whose children are specified in 'contents' param.
	 * 
	 * @param contents
	 *            A list of RunContentML objects.
	 * @param newRPr
	 *            A RunPropertiesML given to the resulting RunML.
	 * @return the newly created RunML
	 * @throws IllegalArgumentException
	 */
	public final static RunML createRunML(
		List<ElementML> contents,
		RunPropertiesML newRPr) {
		
		RunML theRun = new RunML(ObjectFactory.createR(null));
		theRun.setRunProperties(newRPr);
    	
		for (ElementML ml : contents) {
			theRun.addChild(ml);
		}
    	
    	return theRun;
	}
	
	private ElementMLFactory() {
		;//uninstantiable
	}
}// ElementMLFactory class



























