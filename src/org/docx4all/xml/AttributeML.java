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

/**
 *	@author Jojada Tirtowidjojo - 30/11/2007
 */
public class AttributeML {
	private static Logger log = Logger.getLogger(AttributeML.class);
	
	private final WordML.Attribute key;
	private final String value;
	
	public AttributeML(WordML.Attribute key, String value) {
		this.key = key;
		this.value = value;
	}
	
	public WordML.Attribute getKey() {
		return this.key;
	}
	
	public String getValue() {
		return this.value;
	}
}// AttributeML class
