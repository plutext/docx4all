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
import org.plutext.client.state.Controls;
import org.plutext.client.ServerFrom;
import org.plutext.transforms.Transforms.T;

public class TransformInsert extends TransformAbstract {
        /*
         * This class is a basic proof of concept.
         * Deficiencies:
         * - What do you insert @after if the change
         * is at the beginning of the document.  Need @before as well?
         * - What do you do if the @after ID content control has
         * been deleted from this document?  In this case,
         * ask the server for the skeleton document, and use it to 
         * work out where to put it. [TODO]
         * 
         * */
	
	private static Logger log = Logger.getLogger(TransformInsert.class);
	
        protected long insertAfterControlId;

        public TransformInsert(T t)
        {
        	super(t);
            insertAfterControlId = t.getAfter();
        }


        public long apply(ServerFrom serverFrom)
        {

        	// TODO - implement this - docx4all specific

        	
            // Fourth, if we haven't thrown an exception, return the sequence number
            return sequenceNumber;

        }



    }

