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

import org.docx4all.ui.main.Constants;
import org.docx4j.jaxb.document.Br;

/**
 *	@author Jojada Tirtowidjojo - 21/12/2007
 */
public class BrML extends RunContentML {
	private Br br;
	
	public BrML(Br br) {
		this(br, false);
	}
	
	public BrML(Br br, boolean isDummy) {
		super(null, Constants.NEWLINE, isDummy);
		this.br = br;
		this.tag = WordML.Tag.BR;
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
		return this.br == null;
	}
	
}// BrML class



















