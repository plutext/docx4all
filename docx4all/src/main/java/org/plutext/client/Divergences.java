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

package org.plutext.client;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.plutext.client.diffengine.DiffEngine;
import org.plutext.client.diffengine.DiffResultSpan;

/* This class keeps track of the divergences between 
 * 2 document states.
 * 
 * Its primary purpose is to make it possible to 
 * adjust the position for inserts/moves.
 * 
 * One use is for applying server transforms.
 * 
 * In this case, you want to be able to make allowance
 * for any local insertions/
 * deletions which haven't yet been transmitted to 
 * the server, or if they have, have/will come back with
 * a sequence number which we haven't processed yet.
 * 
 * The other use is calculating positions to be
 * included in insert|move transforms to be 
 * transmitted to the server. (Each transform has
 * to be relative to the state the preceding transform
 * would produce).
 * 
 * The class is basically an ordered list of sdt id's,
 * each of which has associated with it an int:
 * 
 * -1 : this sdt not present (or deleted) on LHS
 *  0 : no change
 * +1 : this sdt inserted on LHS
 * 
 * (In the case of a move, there will be two entries,
 *  one with +1, and one with -1)
 * 
 * As updates are applied, we have to keep this object
 * up to date.  Insert transforms result in
 * new entries in the appropriate position, but their
 * associated int is *always 0*.  
 * 
 * If the thing being inserted is already present
 * (ie the transform insert is reflecting a change
 * local in origin), then the relevant int just changes
 * from 1 to 0.
 * 
 * Delete transforms result in the deletion of the appropriate entry.
 */
public class Divergences
{
	private static Logger log = LoggerFactory.getLogger(Divergences.class);
    
    boolean debug = true;

    ArrayList<Entry> entries = null;  // Model as a list, since a move will be 2 entries

    public Divergences(DiffEngine de) //, Skeleton source, Skeleton destination) //, ArrayList DiffLines)
    {

        ArrayList<DiffResultSpan> DiffLines = de.getDiffLines();
        Skeleton source = (Skeleton) de.getSource();
        Skeleton destination = (Skeleton) de.getDestination();

        entries = new ArrayList<Entry>();

        String result = "";
        int i;

        for (DiffResultSpan drs : DiffLines)
        {
            switch (drs.getDiffResultSpanStatus())
            {
                case DELETE_SOURCE:
                    for (i = 0; i < drs.getLength(); i++)
                    {
                        result += "\n" + ((TextLine)source.getByIndex(drs.getSourceIndex() + i)).getLine() 
                            + " not at this location in dest, so will add ";

                        entries.add(
                            new Entry(  ((TextLine)source.getByIndex(drs.getSourceIndex() + i)).getLine(), 
                                        +1));
                    }

                    break;
                case NOCHANGE:
                    for (i = 0; i < drs.getLength(); i++)
                    {
                        result += "\n" + ((TextLine)source.getByIndex(drs.getSourceIndex() + i)).getLine() 
                            + "\t" + ((TextLine)destination.getByIndex(drs.getDestIndex() + i)).getLine() + " (no change)";

                        entries.add(
                            new Entry( ((TextLine)destination.getByIndex(drs.getDestIndex() + i)).getLine(), 
                                        0) ); // source = dest

                    }

                    break;
                case ADD_DESTINATION:
                    for (i = 0; i < drs.getLength(); i++)
                    {
                        result += "\n" + "---" + "\t" + ((TextLine)destination.getByIndex(drs.getDestIndex() + i)).getLine() 
                            + " not at this location in source, so though currently present in dest, will subtract ";

                        entries.add(
                            new Entry( ((TextLine)destination.getByIndex(drs.getDestIndex() + i)).getLine(), 
                                        -1) );

                    }

                    break;
            }

        }

        log.debug( result );
        log.debug("Divergences object set up");
    }

     /* As updates are applied, we have to keep this object
     * up to date.  Insert transforms result in
     * a +1 entry changing to 0 (to signify source and
      * dest are now the same).
     */ 
    public void insert(String id)
    {

        log.debug("Insert " + id + " entry in divergences ");

        for (Entry e : entries)
        {

            if (e.getSdtId().equals(id))
            {
                if (e.getAdj() == 1)
                {
                    e.setAdj(0);
                    log.debug("OK");
                    return;
                } else {
                    /* SPECIAL CASE =-0: this is 
                     * marked as no change.  Shouldn't
                     * happen, since any transforms local
                     * in origin won't be formally applied.
                     * 
                     * SPECIAL CASE =-1: this is 
                     * marked as deleted
                     * In a move case, we wouldn't expect
                     * to encounter this, since we do the
                     * delete before the insert (so the -1 
                     * would be gone).
                     * 
                     * In case you are wondering, a reinstate 
                     * looks just like an 
                     * insert (ie there won't be a -1 entry)
                     * 
                    */
                    log.debug("ERROR: detected unexpected " + e.getAdj() + " entry for " + id);
                    return;
                }
            }
        }
    }

    /* As updates are applied, we have to keep this object
    * up to date.  Insert transforms ordinarily result in
    * a +1 entry changing to 0 (to signify source and
     * dest are now the same).
     * 
     * However, where this divergence object represents 
     * the differences between an old server skeleton,
     * and the current local actuals, and what we are
     * trying to do is maintain its state as new 
     * transforms (from the server) are applied, we have 
     * to do some work to ensure the insertion occurs
     * in the correct position.
     * 
    */
    public void insert(String id, Long pos)
    {

        log.debug("Insert " + id + " entry in divergences, at explicit pos " + pos);

        // First, get rid of any existing +1 entry
        for (Entry e : entries)
        {

            if (e.getSdtId().equals(id))
            {
                if (e.getAdj() == 1)
                {
                    entries.remove(e);
                    log.debug("Removed existing +1 entry");
                    break;
                }
            }
        }

        // Now, add a 0 entry at the correct location
        // Correct location being the list index
        // of pos live entries
        int live = 0;
        int index = 0;
        for (Entry e : entries)
        {

            if (live == pos)  // will the conversion be done automatically?
            {
                break;
            }

            if (e.getAdj() <= 0) // live in dest (ie actual local)
            {
                live++;
            }

            index++;
        }

        // 
        log.debug(".. which is " + index + "th in divergences");

        entries.add(index, new Entry(id, 0)); // 0, since this entry 

        // is now a "no change" against the server.


    }


    public void delete(String id)
    {
        log.debug("Removing " + id + " from divergences");

        //log.debug("\n\r Before ");
        //foreach (Entry e2 in entries)
        //{
        //    log.debug(e2.SdtId + "\t" + e2.Adj);
        //}

        boolean result = false;
        //List<Entry> deletions = new List<Entry>();
        for (Entry e : entries)
        {
            if (e.getSdtId().equals(id) )
            {
                log.debug("Found" + id  + " with value " + e.getAdj());
                
                // We expect Adj = -1.
                // But in the 3 way case (ie applying remote edits)
                // it  may be a zero entry 

                if (e.getAdj() <= 0)
                {
                    // Should be the only one
                    entries.remove(e);
                    return;
                }
            }

        }

        //foreach (Entry e in deletions)
        //{
        //    result = entries.Remove(e);                     
        //    log.debug("and removing ..." + result);
        //}


            log.debug("Couldn't find " + id + " to remove !!!");
            //log.debug("\n\r After ");
            //foreach (Entry e2 in entries)
            //{
            //    log.debug(e2.SdtId + "\t" + e2.Adj);
            //}

    }


    /* Find the location for this move/insertion.  */
    public int getTargetLocation(String id)
    {
        /* Want to count the number of 
         * preceding live locations in dest.
         * These are entries with either 
         * a zero or -1 value. */

        log.debug("Looking for id " + id);

        log.debug("in  ");
        for (Entry e2 : entries)
        {
            log.debug(e2.getSdtId() + "\t" + e2.getAdj());
        }


        int count = 0;
        for (Entry e : entries)
        {
            if (e.getSdtId().equals(id))
            {
                return count;
            }

            if (e.getAdj() <= 0)
            {
                count++;
            }
        }

        log.debug("ERROR! Couldn't find insertion point for id " + id);
        return -1;

    }

    /* Given an absolute position, return the adjustment necessary
     * to insert in the correct spot in this document. 
     * 
     * In this case:
     * - the LHS is a previous server state applied
     * on the client.
     * - the RHS is the current client state
     * 
     * Progressively applying transforms to the
     * current client state will move it towards 
     * the new server state (while preserving local
     * client changes).
     * 
     * What we are looking to do is to calculate the 
     * changes which are necessary to move|insert positions
     * received from server, to make allowance for 
     * local changes.
     */
    public Long getOffset(Long pos)
    {
        // Each LHS deletion in an earlier pos means subtract one

        // Each LHS insertion in an earlier pos means add one

        // Go through entries until we have counted pos entries
        // which are live in the LHS (ie have with *zero* or +1  value)
        // Along the way, sum the pluses and minues,
        // and return that sum.

        log.debug("Looking for offset " + pos);

        log.debug("in  ");
        for (Entry e2 : entries)
        {
            log.debug(e2.getSdtId() + "\t" + e2.getAdj());
        }

        int count = 0;
        int result = 0;
        for (Entry e : entries)
        {

            if (count == pos)
            {
                return Long.valueOf(result);
            }

            if (e.getAdj() >= 0)
            {
                count++;
            }

            result -= e.getAdj();
        }

        // Hmm, ran out of entries.  
        // eg if all n sdts were deleted, we'd get here
        // .. which is ok.
        log.debug("Mildly noteworthy: ran out of Divergences entries! Returning " + result);
        return Long.valueOf(result);

    }

    public int currentPosition(String sdtId)
    {
        int count = 0;
        for (Entry e : entries)
        {

            if (e.getSdtId().equals(sdtId) )
            {
                return count;
            }
            count++;
        }
        return count;
    }

    public void debugInferred()
    {
        log.debug("\n\r Currently inferred skeleton" );

        int count = 0;
        for (Entry e : entries)
        {

            if (e.getAdj() >= 0)
            {
                log.debug(count + " : " + e.getSdtId());
                count++;
            }
        }

    }


    class Entry {

        public Entry(String sdtId, int adj)
        {
            this.sdtId = sdtId;
            this.adj = adj;
        }

        String sdtId;
		public String getSdtId() {
			return sdtId;
		}

        // -1, 0, or +1
        int adj;

		public int getAdj() {
			return adj;
		}

		public void setAdj(int adj) {
			this.adj = adj;
		}


    }
}
