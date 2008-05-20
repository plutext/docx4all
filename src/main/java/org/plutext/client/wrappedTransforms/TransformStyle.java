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
import org.plutext.transforms.Transforms.T;

public class TransformStyle extends TransformAbstract {
	
	private static Logger log = Logger.getLogger(TransformStyle.class);
	

        public TransformStyle(T t)
        {
        	super(t);
        }

        public int apply(ServerFrom serverFrom)
        {
            log.warn("TransformStyle not fully implemented!");

            // TODO - implement this in docx4all

            return sequenceNumber;
        }



    }

