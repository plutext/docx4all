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

import org.apache.log4j.Logger;
import org.plutext.client.ServerFrom;
import org.plutext.client.state.Controls;
import org.docx4j.wml.SdtBlock;
import org.plutext.transforms.Transforms.T;


public class TransformDelete extends TransformAbstract {
	
	private static Logger log = Logger.getLogger(TransformDelete.class);	

    public TransformDelete(T t)
    {
    	super(t);
    }

    /* delete the SDT given its ID. */
    public int apply(ServerFrom serverFrom)
    {
        // Remove the ContentControlSnapshot representing the content control
    	
    	Controls controls = serverFrom.getStateDocx().getControlMap();
    	
    	if (controls.remove(id)) {
            log.warn("Deleted SDT ID " + id);
             return sequenceNumber;    		
    	} else {
	        // couldn't find!
	        // TODO - throw error
	        return -1;
    	}

    }



}

