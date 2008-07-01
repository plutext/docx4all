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
package org.plutext.transforms;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.docx4j.XmlUtils;
import org.plutext.Context;

public class Test {
	
	// To test properly, make sure logging is enabled:
	//    -Dlog4j.configuration=conf/log4j.properties

	public final static String filename="/home/dev/workspace/docx4all/src/org/plutext/transforms/transforms-sample.xml";
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
        org.plutext.transforms.Transforms transformsObj = null;
		try {
			// Unmarshall
			Unmarshaller u = Context.jcTransforms.createUnmarshaller();
			u.setEventHandler(new org.docx4j.jaxb.JaxbValidationEventHandler());					
			transformsObj = (org.plutext.transforms.Transforms)u.unmarshal( new java.io.File(filename) );
			
			// Manipulate
			for (Transforms.T t : transformsObj.t) {
				System.out.println(t.getSnum() + " : " + t.getOp() );
			}
			
			// Marshall again
			boolean suppressDeclaration = true;
			System.out.println( XmlUtils.marshaltoString(transformsObj, suppressDeclaration, Context.jcTransforms) ); 
			
		} catch (JAXBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	
}
