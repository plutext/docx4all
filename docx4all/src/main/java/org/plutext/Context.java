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
package org.plutext;


import javax.xml.bind.JAXBContext;

public class Context {
	
	public static JAXBContext jcTransforms;
	public static JAXBContext jcTransitions;
	
	static {
		
		try {		
			jcTransforms = JAXBContext.newInstance("org.plutext.transforms");
			// (The following would work equally well:
			//    jcTransforms = JAXBContext.newInstance(org.plutext.transforms.Updates.class);
			
			jcTransitions = JAXBContext.newInstance("org.plutext.server.transitions");			
		} catch (Exception ex) {
			ex.printStackTrace();
		}				
	}
		
}
