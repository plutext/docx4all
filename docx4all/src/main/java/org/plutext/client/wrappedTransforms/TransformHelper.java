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

package org.plutext.client.wrappedTransforms;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.log4j.Logger;
import org.plutext.transforms.Context;
import org.plutext.transforms.Transforms.T;

public class TransformHelper
{
	private static Logger log = Logger.getLogger(TransformHelper.class);

	
//    public static TransformAbstract construct(  java.io.InputStream is )
//    {
//
//    	T t = null;
//    	
//		try {
//			Unmarshaller u = Context.jcTransforms.createUnmarshaller();
//			u.setEventHandler(new org.docx4j.jaxb.JaxbValidationEventHandler());					
//			t = (org.plutext.transforms.Transforms.T)u.unmarshal( is );
//		} catch (JAXBException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		construct(t);
//	}
	
    public static TransformAbstract construct(  T t )
    {

        String operation = t.getOp();

        if (operation.equals("update"))
        {
            return new TransformUpdate(t);
        }
        else if (operation.equals("delete"))
        {
            return new TransformDelete(t);
        }
        else if (operation.equals("insert"))
        {
            return new TransformInsert(t);
        }
        else if (operation.equals("style"))
        {
            return new TransformStyle(t);
        }
        else if (operation.equals("failed"))
        {
            return new TransformFailed(t);
        }
        else {
        	log.error("Unrecognised transform!!!");
        	// TODO - throw exception
        	return null;
        }

    }
}
