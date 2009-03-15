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

package org.docx4all.xml.drawing;

import java.awt.Toolkit;

import org.apache.log4j.Logger;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;

/**
 *	@author Jojada Tirtowidjojo - 15/12/2008
 */
public class Graphic {
	
	private static Logger log = Logger.getLogger(Graphic.class);
	
	private org.docx4j.dml.Graphic graphic;
	
	public Graphic(org.docx4j.dml.Graphic graphic) {
		this.graphic = graphic;
	}
	
	public Object getDocxObject() {
		return this.graphic;
	}
	
	public java.awt.Image getImage(WordprocessingMLPackage wmlPkg) {
		if (wmlPkg == null 
			|| wmlPkg.getMainDocumentPart() == null
			|| wmlPkg.getMainDocumentPart().getRelationshipsPart() == null) {
			return null;
		}
		
		java.awt.Image theImage = null;
		
		org.docx4j.dml.Pic pic = getPic();
		String rId = pic.getBlipFill().getBlip().getEmbed();
		if (rId.equals("")) {
			rId = pic.getBlipFill().getBlip().getLink();
		}
		log.debug("Image rel id: " + rId);
		org.docx4j.relationships.Relationship rel = 
			wmlPkg.getMainDocumentPart().getRelationshipsPart().getRelationshipByID(rId);
		if (rel != null) {
			org.docx4j.openpackaging.parts.Part part = 
				wmlPkg.getMainDocumentPart().getRelationshipsPart().getPart(rel);
			if (part == null) {
				log.error("Couldn't get Part!");
			} else if (part instanceof org.docx4j.openpackaging.parts.WordprocessingML.BinaryPart) {
				log.debug("getting bytes...");
				org.docx4j.openpackaging.parts.WordprocessingML.BinaryPart binaryPart =
					(org.docx4j.openpackaging.parts.WordprocessingML.BinaryPart) part;
				java.nio.ByteBuffer bb = binaryPart.getBuffer();
    	        bb.clear();
    	        byte[] bytes = new byte[bb.capacity()];
    	        bb.get(bytes, 0, bytes.length);

				theImage = Toolkit.getDefaultToolkit().createImage(bytes);
			} else {				
				log.error("Part was a " + part.getClass().getName() );
			}
		} else {
			log.error("Couldn't find rel " + rId);
		}
		
		return theImage;
	}

	private org.docx4j.dml.Pic getPic() {
		return this.graphic.getGraphicData().getPic();
	}
}// Graphic class



















