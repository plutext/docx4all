/*
 *  Copyright 2007-2008, Plutext Pty Ltd.
 *   
 *  This file is part of docx4j.

    docx4j is licensed under the Apache License, Version 2.0 (the "License"); 
    you may not use this file except in compliance with the License. 

    You may obtain a copy of the License at 

        http://www.apache.org/licenses/LICENSE-2.0 

    Unless required by applicable law or agreed to in writing, software 
    distributed under the License is distributed on an "AS IS" BASIS, 
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
    See the License for the specific language governing permissions and 
    limitations under the License.

 */
package org.plutext.client;

import java.util.ArrayList;
import java.util.HashMap;

import javax.xml.bind.Unmarshaller;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.plutext.Context;
import org.plutext.client.diffengine.DiffEngine;
import org.plutext.client.diffengine.DiffResultSpan;

public class DivergencesTransmitTest {
	
	private static Logger log = Logger.getLogger(DivergencesTransmitTest.class);	
	
    static String BASE_DIR = "/home/dev/workspace/docx4all/src/tests/org/plutext/client/diff-transitions/";
    
    static String[] tests = { "base", "deleted", "inserted", "moved", "complex", "unrelated", "random" };
	
	
	public static void testDiff() throws Exception {
		
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
                
                testDivergence(inferredSkeleton, serverSkeleton);
            
            }
        }
	}

	
	public static void main(String[] args) throws Exception {

		testDiff();
	}
	
	@Test
	public static void testDivergence(Skeleton inferredSkeleton, Skeleton serverSkeleton) throws Exception {
		
        DiffEngine de = new DiffEngine();
        de.processDiff(inferredSkeleton, serverSkeleton);

        ArrayList<DiffResultSpan> diffLines = de.getDiffLines();

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
            switch (drs.getDiffResultSpanStatus())
            {
                case DELETE_SOURCE:
                    for (i = 0; i < drs.getLength(); i++)
                    {
                        insertPos++;
                        // Must be a new local insertion
                        log.debug(insertPos + ": " + ((TextLine)inferredSkeleton.getByIndex(drs.getSourceIndex() + i)).getLine()
                            + " not at this location in dest");
                        String insertionId = ((TextLine)inferredSkeleton.getByIndex(drs.getSourceIndex() + i)).getLine();
                        notHereInDest.put(insertionId, insertPos);
                    }

                    break;
                case NOCHANGE:
                    for (i = 0; i < drs.getLength(); i++)
                    {
                        insertPos++;
                        log.debug(insertPos + ": " + ((TextLine)inferredSkeleton.getByIndex(drs.getSourceIndex() + i)).getLine()
                            + "\t" + ((TextLine)serverSkeleton.getByIndex(drs.getDestIndex() + i)).getLine() + " (no change)");

                        // Nothing to do
                    }

                    break;
                case ADD_DESTINATION:
                    for (i = 0; i < drs.getLength(); i++)
                    {
                        //insertPos++; // Not for a delete
                        log.debug(insertPos + ": " + ((TextLine)serverSkeleton.getByIndex(drs.getDestIndex() + i)).getLine()
                            + " not at this location in source");
                        String deletionId = ((TextLine)serverSkeleton.getByIndex(drs.getDestIndex() + i)).getLine();
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
            switch (drs.getDiffResultSpanStatus())
            {
                case DELETE_SOURCE:  // Means we're doing an insertion
                    for (i = 0; i < drs.getLength(); i++)
                    {
                        String insertionId = ((TextLine)inferredSkeleton.getByIndex(drs.getSourceIndex() + i)).getLine();
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
                case NOCHANGE:
                    for (i = 0; i < drs.getLength(); i++)
                    {

                        log.debug(insertPos + ": " + ((TextLine)inferredSkeleton.getByIndex(drs.getSourceIndex() + i)).getLine()
                            + "\t" + ((TextLine)serverSkeleton.getByIndex(drs.getDestIndex() + i)).getLine() + " (no change)");

                        //String id = ((TextLine)inferredSkeleton.getByIndex(drs.getSourceIndex() + i)).Line;


                    }

                    break;
                case ADD_DESTINATION:
                    for (i = 0; i < drs.getLength(); i++)
                    {
                        String deletionId = ((TextLine)serverSkeleton.getByIndex(drs.getDestIndex() + i)).getLine();
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
