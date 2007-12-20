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

import javax.xml.bind.JAXBElement;

import org.apache.log4j.Logger;

/**
 *	@author Jojada Tirtowidjojo - 06/12/2007
 */
public class RunContentML extends ElementML {
	private static Logger log = Logger.getLogger(RunContentML.class);
	
	private JAXBElement<?> jaxbElem;
	protected String textContent;
	
	public RunContentML(JAXBElement<?> elem, String textContent) {
		this(elem, textContent, false);
	}
	
	public RunContentML(JAXBElement<?> elem, String textContent, boolean isDummy) {
		this.jaxbElem = elem;
		if (elem != null) {
			this.tag = WordML.getTag(elem.getName().getLocalPart());
		}
		this.isDummy = isDummy;
		this.textContent = textContent;
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
		return this.jaxbElem == null;
	}

	public String getTextContent() {
		return this.textContent;
	}
	
	public void setTextContent(String textContent) {
		this.textContent = textContent;
	}
	
}// RunContentML class



















