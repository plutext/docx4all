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

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.plutext.client.Mediator;
import org.plutext.client.state.StateChunk;
import org.plutext.transforms.Changesets.Changeset;
import org.plutext.transforms.Transforms.T;


    /* This class allows the server to return the details of an
     * update which was attempted, but which failed. */
public class TransformFailed extends TransformAbstract
{
	private static Logger log = Logger.getLogger(TransformFailed.class);

    public TransformFailed(T t)
    {
    	super(t);
    }

	 /* Compare the updated sdt to the original, replacing the
     * updated one with containing w:ins and w:del */
	@Override
    public String markupChanges(String original, Changeset changeset) {
        // Do nothing.
        // How best to indicate to the user that something
        // has moved?  Just in a dialog box?
		return null;
    }

    public long apply(Mediator mediator, HashMap<String, StateChunk> stateChunks)
    {
        log.debug("TransformFailed not fully implemented!");

        return sequenceNumber;
    }


}
