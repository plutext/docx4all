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

package org.docx4all.util;

import javax.xml.namespace.QName;

/**
 *	@author Jojada Tirtowidjojo - 04/01/2008
 */
public class XmlUtil {

	public final static String getEnclosingTagPair(QName qname) {
		String prefix = qname.getPrefix();
		if (prefix != null && prefix.length() > 0) {
			prefix = prefix + ":";
		}
		
		StringBuffer sb = new StringBuffer();
		sb.append("<");
		sb.append(prefix);
		sb.append(qname.getLocalPart());
		sb.append(">");
		sb.append("</");
		sb.append(prefix);
		sb.append(qname.getLocalPart());
		sb.append(">");
		
		return sb.toString();
	}
	
	private XmlUtil() {
		;//uninstantiable
	}
}// XmlUtil class



















