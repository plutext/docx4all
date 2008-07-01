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

import java.util.HashMap;
import java.util.ArrayList;

import javax.xml.bind.Unmarshaller;

import org.apache.log4j.Logger;
import org.plutext.Context;
import org.plutext.server.transitions.Transitions;



public class Skeleton implements IDiffList
{
	private static Logger log = Logger.getLogger(Skeleton.class);
	
    public Skeleton(Transitions t)
    {
        /*
         * <dst:transitions>
         *   <dst:ribs>
         *      <dst:rib id="54989358" />
         *      <dst:rib id="1447653797" />
         *        :
         * </dst:transitions>
         * 
         * */

    	for ( Transitions.Ribs.Rib r : t.getRibs().getRib() ) {
    		
    		ribs.add( new TextLine( Long.toString(r.getId() ) ) );
    		
    	}

    }

    
    public Skeleton() {
    	
    }
    

    // Version number
    int version;

    // Ordered list of ribs
    ArrayList ribs = new ArrayList();
    public ArrayList getRibs() {
		return ribs;
	}
	public void setRibs(ArrayList ribs) {
		this.ribs = ribs;
	}
    

    public static void difftest()
    {

        String BASE_DIR = "/home/dev/workspace/plutext-client-word2007/tests/diff-transitions/";
        String[] tests = { "base", "deleted", "inserted", "moved", "complex", "unrelated", "random" };


        for (int h = 0; h < tests.length; h++)
        {
        	
            String filename = BASE_DIR + tests[h] + ".xml";

            Unmarshaller u = Context.jcTransitions.createUnmarshaller();
    		u.setEventHandler(new org.docx4j.jaxb.JaxbValidationEventHandler());					
    		org.plutext.server.transitions.Transitions left = 
    			(org.plutext.server.transitions.Transitions)u.unmarshal( 
    					new java.io.File(filename) );
            Skeleton inferredSkeleton = new Skeleton(left);
        	

            for (int j = 0; j < tests.length; j++)
            {

                log.debug("\n\r \n\r Testing " + tests[h] + " against " + tests[j]);

        		org.plutext.server.transitions.Transitions right = 
        			(org.plutext.server.transitions.Transitions)u.unmarshal( 
        					new java.io.File(BASE_DIR + tests[j] + ".xml") );
                
                Skeleton serverSkeleton = new Skeleton(right);

                DiffEngine de = new DiffEngine();
                de.ProcessDiff(inferredSkeleton, serverSkeleton);

                ArrayList diffLines = de.DiffLines;

                /* Detect moves
                 * 
                 * In order to detect moves, we have to be able to
                 * identify whether a delete has a corresponding
                 * insert (and vice versa).
                 * 
                 * These Dictionary objects facilitate this. */
                HashMap<String, Integer> notHereInDest = new HashMap<String, Integer>();
                HashMap<String, Integer> notHereInSource = new HashMap<String, Integer>();
                //Populate the dictionaries
                int insertPos = -1;
                int i;
                log.debug("\n\r");
                for (DiffResultSpan drs : diffLines)
                {
                    switch (drs.Status)
                    {
                        case DiffResultSpanStatus.DeleteSource:
                            for (i = 0; i < drs.Length; i++)
                            {
                                insertPos++;
                                // Must be a new local insertion
                                log.debug(insertPos + ": " + ((TextLine)inferredSkeleton.GetByIndex(drs.SourceIndex + i)).getLine()
                                    + " not at this location in dest");
                                String insertionId = ((TextLine)inferredSkeleton.GetByIndex(drs.SourceIndex + i)).getLine();
                                notHereInDest.put(insertionId, insertPos);
                            }

                            break;
                        case DiffResultSpanStatus.NoChange:
                            for (i = 0; i < drs.Length; i++)
                            {
                                insertPos++;
                                log.debug(insertPos + ": " + ((TextLine)inferredSkeleton.GetByIndex(drs.SourceIndex + i)).getLine()
                                    + "\t" + ((TextLine)serverSkeleton.GetByIndex(drs.DestIndex + i)).getLine() + " (no change)");

                                // Nothing to do
                            }

                            break;
                        case DiffResultSpanStatus.AddDestination:
                            for (i = 0; i < drs.Length; i++)
                            {
                                //insertPos++; // Not for a delete
                                log.debug(insertPos + ": " + ((TextLine)serverSkeleton.GetByIndex(drs.DestIndex + i)).getLine()
                                    + " not at this location in source");
                                String deletionId = ((TextLine)serverSkeleton.GetByIndex(drs.DestIndex + i)).getLine();
                                notHereInSource.put(deletionId, insertPos);

                            }

                            break;
                    }
                }


                Divergences divergences = new Divergences(de);

                log.debug("\n\r");


                // How to make the dest (right) like the source (left)

                for (DiffResultSpan drs : diffLines)
                {
                    switch (drs.Status)
                    {
                        case DiffResultSpanStatus.DeleteSource:  // Means we're doing an insertion
                            for (i = 0; i < drs.Length; i++)
                            {
                                String insertionId = ((TextLine)inferredSkeleton.GetByIndex(drs.SourceIndex + i)).getLine();
                                log.debug(insertPos + ": " + insertionId
                                    + " is at this location in src but not dest, so needs to be inserted");

                                Integer dicVal = notHereInSource.get(insertionId);
                                
                                if (dicVal==null) {

                                    // Just a new local insertion

                                    int adjPos = divergences.getTargetLocation(insertionId);

                                    log.debug("<transform op=insert id=" + insertionId + "  pos=" + adjPos);

                                    divergences.insert(insertionId); // change +1 to 0

                                    divergences.debugInferred();
                                	
                                } else {
                                	// there is a corresponding delete, so this is really a move
                                    log.debug("   " + insertionId + " is a MOVE "); 

                                    //if (toPosition[insertionId] == divergences.currentPosition(insertionId))  //rhsPosition[insertionId])
                                    //{
                                    //    // currentPosition is the position in the inferred point-in-time 
                                    //    // server skeleton (ie as it would be with transforms 
                                    //    // generated so far applied)

                                    //    log.Debug("Discarding <transform op=move id=" + insertionId + "  pos=" + toPosition[insertionId]);
                                    //}
                                    //else
                                    //{

                                        /* Semantics of move will be as follows:
                                         * 
                                         * (i) removed the identified item,
                                         * 
                                         * (ii) then insert the new item at the specified position.
                                         * 
                                         * This way, the position you specify is the position it
                                         * ends up in (ie irrespective of whether the original
                                         * position was earlier or later).  
                                         */ 

                                        // therefore:
                                        // delete first
                                         divergences.delete(insertionId); // remove -1

                                         int adjPos = divergences.getTargetLocation(insertionId);
                                        
                                        log.debug("<transform op=move id=" + insertionId + "  pos=" + adjPos);  

                                        divergences.insert(insertionId); // change +1 to 0

                                        divergences.debugInferred();

                                        //if (rawPos + adjPos == divergences.currentPosition(insertionId))
                                        //{`
                                        //    log.Debug(".. that transform could be DISCARDED.");
                                        //}

                                        //divergences.move(insertionId, rawPos + adjPos);
                                    //}
                                }

                            }

                            break;
                        case DiffResultSpanStatus.NoChange:
                            for (i = 0; i < drs.Length; i++)
                            {

                                log.debug(insertPos + ": " + ((TextLine)inferredSkeleton.GetByIndex(drs.SourceIndex + i)).getLine()
                                    + "\t" + ((TextLine)serverSkeleton.GetByIndex(drs.DestIndex + i)).getLine() + " (no change)");

                                //String id = ((TextLine)inferredSkeleton.GetByIndex(drs.SourceIndex + i)).Line;


                            }

                            break;
                        case DiffResultSpanStatus.AddDestination:
                            for (i = 0; i < drs.Length; i++)
                            {
                                String deletionId = ((TextLine)serverSkeleton.GetByIndex(drs.DestIndex + i)).getLine();
                                log.debug(insertPos + ": " + deletionId
                                    + " present at this location in dest but not source, so needs to be deleted");

                                Integer dicVal = notHereInDest.get(deletionId);
                                
                                if (dicVal==null) {

                                    // Just a new local deletion

                                    log.debug("Couldn't find " + deletionId + " so deleting");
                                    divergences.delete(deletionId);

                                    divergences.debugInferred();
                                	
                                } else {
                                    // there is a corresponding insert, so this is really a move
                                    log.debug("   " + deletionId + " is a MOVE to elsewhere (" + dicVal + ")");
                                                                            
                                    // DO NOTHING

                                }

                            }

                            break;
                    }
                }
            }
        }

        

    }

    private static void TextDiff(Skeleton source, Skeleton dest)
    {

        try
        {
            double time = 0;
            DiffEngine de = new DiffEngine();
            time = de.ProcessDiff(source, dest, DiffEngineLevel.Medium);

            System.Collections.ArrayList rep = de.DiffLines;

            //log.Debug(de.Results(source, dest, rep));
        }
        catch (Exception ex)
        {
            string tmp = string.Format("{0}{1}{1}***STACK***{1}{2}",
                ex.Message,
                Environment.NewLine,
                ex.StackTrace);
            log.Debug(tmp);
            return;
        }
    }

    //#region IDiffList Members

    public int Count()
    {
        return ribs.Count;
    }

    public IComparable GetByIndex(int index)
    {
        return (TextLine)ribs[index];
    }

    //#endregion

    public class TextLine implements IComparable
    {
        public String line;
		public String getLine() {
			return line;
		}
		
        public int _hash;

        public TextLine(String str)
        {
            line = str.Replace("\t", "    ");
            _hash = str.GetHashCode();
        }
        //#region IComparable Members

        public int CompareTo(object obj)
        {
            return _hash.CompareTo(((TextLine)obj)._hash);
        }


        //#endregion
    }

}
