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
import org.docx4j.openpackaging.parts.WordprocessingML.BinaryPartAbstractImage;

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
		
		java.awt.Image theImage = null;	
    	byte[] bytes = BinaryPartAbstractImage.getImage(wmlPkg, graphic);
    	if (bytes!=null) {
    		theImage = Toolkit.getDefaultToolkit().createImage(bytes);		
    		return theImage;
    	} else {
    		return null;
    	}
	}

//	private org.docx4j.dml.Pic getPic() {
//		return this.graphic.getGraphicData().getPic();
//	}
}// Graphic class



















