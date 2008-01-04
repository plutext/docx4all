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

import org.docx4j.jaxb.document.Document;
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
	
	private ElementMLFactory() {
		;//uninstantiable
	}
}// ElementMLFactory class



























