/*
 *  Copyright 2008, Plutext Pty Ltd.
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

package org.plutext.client;

import org.apache.log4j.Logger;
import org.docx4all.swing.text.DocumentElement;
import org.docx4all.swing.text.WordMLDocument;
import org.docx4all.xml.ElementML;
import org.docx4all.xml.SdtBlockML;
import org.docx4j.openpackaging.parts.DocPropsCustomPart;
import org.docx4j.wml.Id;
import org.docx4j.wml.SdtBlock;
import org.plutext.client.state.StateChunk;


public class Util {
	
	private static Logger log = Logger.getLogger(Util.class);	
	
	public static String getCustomDocumentProperty(DocPropsCustomPart docPropsCustomPart, 
			String propName) {
		
		org.docx4j.docProps.custom.Properties customProps = (org.docx4j.docProps.custom.Properties)docPropsCustomPart.getJaxbElement();
		
		for (org.docx4j.docProps.custom.Properties.Property prop: customProps.getProperty() ) {
			
			if (prop.getName().equals(propName)) {
				// At the moment, you need to know what sort of value it has.
				// Could create a generic Object getValue() method.
				return prop.getLpwstr();
			}
		}
		log.error("Property '" + propName + "' not found!");
		return null;
		
	}
	
	/** Gets the text contents of the Sdt */
	public static String getSdtText(SdtBlock cc) {
		
		// TODO - implement
		
		return null;
	}

    public static String getChunkId(Id id)
    {
    	return id.getVal().toString();
    }
    
    
    public static DocumentElement getDocumentElement(
    	WordMLDocument doc,
    	String sdtBlockId) {
    	
		DocumentElement elem = null;

		try {
			doc.readLock();

			DocumentElement root = (DocumentElement) doc
					.getDefaultRootElement();

			for (int i = 0; i < root.getElementCount() - 1 && elem == null; i++) {
				elem = (DocumentElement) root.getElement(i);
				ElementML ml = elem.getElementML();
				if (ml instanceof SdtBlockML) {
					SdtBlockML sdtBlockML = (SdtBlockML) ml;
					if (sdtBlockId.equals(sdtBlockML.getSdtProperties()
							.getIdValue().toString())) {
						;// got it
					} else {
						elem = null;
					}
				} else {
					elem = null;
				}
			}
		} finally {
			doc.readUnlock();
		}

		return elem;
	}

    public static StateChunk getStateChunk(
    	WordMLDocument doc,
    	String sdtBlockId) {
    	
    	StateChunk theChunk = null;
    	
    	DocumentElement elem = getDocumentElement(doc, sdtBlockId);
    	if (elem != null) {
    		ElementML ml = elem.getElementML();
    		theChunk = new StateChunk((org.docx4j.wml.SdtBlock) ml.getDocxObject());
    	}
    	
    	return theChunk;
    }
    
    public static Skeleton createSkeleton(WordMLDocument doc) {
    	Skeleton skeleton = new Skeleton();
  		DocumentElement root = (DocumentElement) doc.getDefaultRootElement();    	
  		for (int i=0; i < root.getElementCount(); i++) {
  			DocumentElement elem = (DocumentElement) root.getElement(i);
  			ElementML ml = elem.getElementML();
  			if (ml instanceof SdtBlockML) {
  				SdtBlockML sdt = (SdtBlockML) ml;
  				String id = sdt.getSdtProperties().getIdValue().toString();
				skeleton.getRibs().add(new TextLine(id)); 				
  			}
  		}
  		return skeleton;
    }
    
	public static String getContentControlXML(SdtBlock cc) {

		boolean suppressDeclaration = true;
		return org.docx4j.XmlUtils.marshaltoString(cc, suppressDeclaration);

		// return "<w:sdt
		// xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">"
		// + node.InnerXml + "</w:sdt>";
	}
    

}// Util class


























