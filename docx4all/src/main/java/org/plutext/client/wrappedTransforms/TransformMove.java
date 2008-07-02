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

import javax.swing.SwingUtilities;

import org.apache.log4j.Logger;
import org.plutext.transforms.Transforms.T;

import org.plutext.client.Mediator;
import org.plutext.client.Pkg;
import org.plutext.client.wrappedTransforms.TransformInsert.InsertAtRunnable;

public class TransformMove extends TransformAbstract
{
	private static Logger log = Logger.getLogger(TransformMove.class);

    
    public TransformMove(T t)
    {
    	super(t);
    }
    



    public long apply(Mediator mediator, Pkg pkg)
    {


        // Semantics of move are
        // 1 remove existing element from list
        // 2 insert new element

        // So
		TransformDelete.apply(mediator.getWordMLTextPane(), getId().getVal());


            // No need to 
            // pkg.StateChunks.Remove(id);

            // QUESTION: interaction between divergences object and existing entry??
            // - Probably have to do divergences.delete?
        mediator.getDivergences().delete(id.getVal().toString());


        // Second, re-insert it at the correct location


        // if user has locally inserted/deleted sdt's
        // we need to adjust the specified position ...

    	Long pos = t.getPosition();
    	Long insertAtIndex = pos + mediator.getDivergences().getOffset(pos);

        log.debug("Move location " + pos + " adjusted to " + insertAtIndex);

        
		Runnable runnable = new TransformInsert.InsertAtRunnable(mediator.getWordMLTextPane(), 
				getSdt(), insertAtIndex.intValue());
		
		SwingUtilities.invokeLater(runnable);
    

        //pkg.StateChunks.Add(id, new StateChunk(sdt));
        mediator.getDivergences().insert(id.getVal().toString(), insertAtIndex);

        log.debug("Moved sdt " + id + " in pkg");
        return sequenceNumber;
    }

}
