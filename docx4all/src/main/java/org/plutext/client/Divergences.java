/*
 *  Copyright 2007, Plutext Pty Ltd.
 *   
 *  This file is part of plutext-client-word2007.

    plutext-client-word2007 is free software: you can redistribute it and/or 
    modify it under the terms of version 3 of the GNU General Public License
    as published by the Free Software Foundation.

    plutext-client-word2007 is distributed in the hope that it will be 
    useful, but WITHOUT ANY WARRANTY; without even the implied warranty 
    of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License   
    along with plutext-client-word2007.  If not, see 
    <http://www.gnu.org/licenses/>.
   
 */
using System;
using System.Collections;
using System.Collections.Generic;
using System.Text;
using log4net;
using plutext.client.word2007.DifferenceEngine;

namespace plutext.client.word2007
{
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
        private static readonly ILog log = LogManager.GetLogger(typeof(Divergences));

        Boolean debug = true;

        List<Entry> entries = null;  // Model as a list, since a move will be 2 entries

        public Divergences(DiffEngine de) //, Skeleton source, Skeleton destination) //, ArrayList DiffLines)
        {

            ArrayList DiffLines = de.DiffLines;
            Skeleton source = (Skeleton)de.Source;
            Skeleton destination = (Skeleton)de.Dest;

            entries = new List<Entry>();

            String result = "";
            int i;

            foreach (DiffResultSpan drs in DiffLines)
            {
                switch (drs.Status)
                {
                    case DiffResultSpanStatus.DeleteSource:
                        for (i = 0; i < drs.Length; i++)
                        {
                            result += "\n" + ((TextLine)source.GetByIndex(drs.SourceIndex + i)).Line 
                                + " not at this location in dest, so will add ";

                            entries.Add(
                                new Entry(  ((TextLine)source.GetByIndex(drs.SourceIndex + i)).Line, 
                                            +1));
                        }

                        break;
                    case DiffResultSpanStatus.NoChange:
                        for (i = 0; i < drs.Length; i++)
                        {
                            result += "\n" + ((TextLine)source.GetByIndex(drs.SourceIndex + i)).Line 
                                + "\t" + ((TextLine)destination.GetByIndex(drs.DestIndex + i)).Line + " (no change)";

                            entries.Add(
                                new Entry( ((TextLine)destination.GetByIndex(drs.DestIndex + i)).Line, 
                                            0) ); // source = dest

                        }

                        break;
                    case DiffResultSpanStatus.AddDestination:
                        for (i = 0; i < drs.Length; i++)
                        {
                            result += "\n" + "---" + "\t" + ((TextLine)destination.GetByIndex(drs.DestIndex + i)).Line 
                                + " not at this location in source, so though currently present in dest, will subtract ";

                            entries.Add(
                                new Entry( ((TextLine)destination.GetByIndex(drs.DestIndex + i)).Line, 
                                            -1) );

                        }

                        break;
                }

            }

            log.Debug( result );
            log.Debug("Divergences object set up");
        }

         /* As updates are applied, we have to keep this object
         * up to date.  Insert transforms result in
         * a +1 entry changing to 0 (to signify source and
          * dest are now the same).
         */ 
        public void insert(String id)
        {

            log.Debug("Insert " + id + " entry in divergences ");

            foreach (Entry e in entries)
            {

                if (e.SdtId.Equals(id))
                {
                    if (e.Adj == 1)
                    {
                        e.Adj = 0;
                        log.Debug("OK");
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
                        log.Debug("ERROR: detected unexpected " + e.Adj + " entry for " + id);
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
        public void insert(String id, int pos)
        {

            log.Debug("Insert " + id + " entry in divergences, at explicit pos " + pos);

            // First, get rid of any existing +1 entry
            foreach (Entry e in entries)
            {

                if (e.SdtId.Equals(id))
                {
                    if (e.Adj == 1)
                    {
                        entries.Remove(e);
                        log.Debug("Removed existing +1 entry");
                        break;
                    }
                }
            }

            // Now, add a 0 entry at the correct location
            // Correct location being the list index
            // of pos live entries
            int live = 0;
            int index = 0;
            foreach (Entry e in entries)
            {

                if (live == pos)
                {
                    break;
                }

                if (e.Adj <= 0) // live in dest (ie actual local)
                {
                    live++;
                }

                index++;
            }

            // 
            log.Debug(".. which is " + index + "th in divergences");

            entries.Insert(index, new Entry(id, 0)); // 0, since this entry 
            // is now a "no change" against the server.


        }


        public void delete(String id)
        {
            log.Debug("Removing " + id + " from divergences");

            //log.Debug("\n\r Before ");
            //foreach (Entry e2 in entries)
            //{
            //    log.Debug(e2.SdtId + "\t" + e2.Adj);
            //}

            bool result = false;
            //List<Entry> deletions = new List<Entry>();
            foreach (Entry e in entries)
            {
                if (e.SdtId.Equals(id) )
                {
                    log.Debug("Found" + id  + " with value " + e.Adj);
                    
                    // We expect Adj = -1.
                    // But in the 3 way case (ie applying remote edits)
                    // it  may be a zero entry 

                    if (e.Adj <= 0)
                    {
                        // Should be the only one
                        entries.Remove(e);
                        return;
                    }
                }

            }

            //foreach (Entry e in deletions)
            //{
            //    result = entries.Remove(e);                     
            //    log.Debug("and removing ..." + result);
            //}


                log.Debug("Couldn't find " + id + " to remove !!!");
                //log.Debug("\n\r After ");
                //foreach (Entry e2 in entries)
                //{
                //    log.Debug(e2.SdtId + "\t" + e2.Adj);
                //}

        }


        /* Find the location for this move/insertion.  */
        public int getTargetLocation(String id)
        {
            /* Want to count the number of 
             * preceding live locations in dest.
             * These are entries with either 
             * a zero or -1 value. */

            log.Debug("Looking for id " + id);

            log.Debug("in  ");
            foreach (Entry e2 in entries)
            {
                log.Debug(e2.SdtId + "\t" + e2.Adj);
            }


            int count = 0;
            foreach (Entry e in entries)
            {
                if (e.SdtId.Equals(id))
                {
                    return count;
                }

                if (e.Adj <= 0)
                {
                    count++;
                }
            }

            log.Debug("ERROR! Couldn't find insertion point for id " + id);
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
        public int getOffset(int pos)
        {
            // Each LHS deletion in an earlier pos means subtract one

            // Each LHS insertion in an earlier pos means add one

            // Go through entries until we have counted pos entries
            // which are live in the LHS (ie have with *zero* or +1  value)
            // Along the way, sum the pluses and minues,
            // and return that sum.

            log.Debug("Looking for offset " + pos);

            log.Debug("in  ");
            foreach (Entry e2 in entries)
            {
                log.Debug(e2.SdtId + "\t" + e2.Adj);
            }

            int count = 0;
            int result = 0;
            foreach (Entry e in entries)
            {

                if (count == pos)
                {
                    return result;
                }

                if (e.Adj >= 0)
                {
                    count++;
                }

                result -= e.Adj;
            }

            // Hmm, ran out of entries.  
            // eg if all n sdts were deleted, we'd get here
            // .. which is ok.
            log.Debug("Mildly noteworthy: ran out of Divergences entries! Returning " + result);
            return result;

        }

        public int currentPosition(String sdtId)
        {
            int count = 0;
            foreach (Entry e in entries)
            {

                if (e.SdtId.Equals(sdtId) )
                {
                    return count;
                }
                count++;
            }
            return count;
        }

        public void debugInferred()
        {
            log.Debug("\n\r Currently inferred skeleton" );

            int count = 0;
            foreach (Entry e in entries)
            {

                if (e.Adj >= 0)
                {
                    log.Debug(count + " : " + e.SdtId);
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
            public String SdtId
            {
                get { return sdtId; }
            }

            // -1, 0, or +1
            int adj;
            public int Adj
            {
                get { return adj; }
                set { adj = value; }
            }

        }
    }
}
