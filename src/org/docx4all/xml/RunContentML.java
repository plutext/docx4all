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

import org.apache.log4j.Logger;
import org.dom4j.Element;

/**
 *	@author Jojada Tirtowidjojo - 06/12/2007
 */
public class RunContentML extends ElementML {
	private static Logger log = Logger.getLogger(RunContentML.class);
	
	private Element element;
	private String text;
	
	public RunContentML(Element elem, String text) {
		this(elem, text, false);
	}
	
	public RunContentML(Element elem, String text, boolean isDummy) {
		this.element = elem;
		if (elem != null) {
			this.tag = WordML.getTag(elem.getName());
		} else {
			//Dummy RunContentML is given this default tag
			this.tag = WordML.Tag.T;
		}
		this.isDummy = isDummy;
		this.text = text;
	}
	
	public String getText() {
		return this.text;
	}
	
	public void setText(String text) {
		this.text = text;
	}
	
}// RunContentML class



















