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

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.apache.log4j.Logger;
import org.docx4j.jaxb.document.Body;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.MainDocumentPart;

/**
 *	@author Jojada Tirtowidjojo - 30/11/2007
 */
public class DocumentML extends ElementML {
	private static Logger log = Logger.getLogger(DocumentML.class);
	
	private final WordprocessingMLPackage docPackage;
	
	public DocumentML(WordprocessingMLPackage docPackage) {
		this.docPackage = docPackage;
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
		return this.docPackage == null;
	}

	private void initChildren() {
		MainDocumentPart documentPart = this.docPackage.getMainDocumentPart();			
		
		org.docx4j.jaxb.document.Document doc = documentPart.getDocumentObj();
		Body body = doc.getBody();

		List <Object> bodyChildren = body.getBlockLevelElements();
		if (!bodyChildren.isEmpty()) {
			this.children = new ArrayList<ElementML>(bodyChildren.size());
			
			for (Object o : bodyChildren) {
				if (o instanceof JAXBElement<?>) {
					JAXBElement<?> jaxbElem = (JAXBElement<?>) o;

					ParagraphML paraML;

					String typeName = jaxbElem.getDeclaredType().getName();
					if ("org.docx4j.jaxb.document.P".equals(typeName)) {
						org.docx4j.jaxb.document.P p = (org.docx4j.jaxb.document.P) jaxbElem
								.getValue();
						paraML = new ParagraphML(p);

					} else {
						// TODO: A more informative text content in the dummy
						// paragraph.
						QName name = jaxbElem.getName();
						StringBuffer sb = new StringBuffer();
						sb.append("<");
						sb.append(name.getNamespaceURI());
						sb.append(":");
						sb.append(name.getLocalPart());
						sb.append(">");
						sb.append("</");
						sb.append(name.getNamespaceURI());
						sb.append(":");
						sb.append(name.getLocalPart());
						sb.append(">");
						paraML = ElementMLFactory.createParagraphML(sb.toString(),
								true);
					}

					paraML.setParent(DocumentML.this);
					this.children.add(paraML);
					
				} else if (o instanceof org.docx4j.jaxb.document.Sdt) {
					String s = "<w:sdt></w:sdt>";
					ParagraphML paraML = 
						ElementMLFactory.createParagraphML(s, true);
					paraML.setParent(DocumentML.this);
					this.children.add(paraML);
					
				}// if (o instanceof JAXBElement<?>)
			}// for (Object o : bodyChildren )
		}
	}// initChildren()
	
}// DocumentML class




















