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
using System.Collections.Generic;
using System.Text;
using System.Xml;
using System.IO;
using plutext.client.word2007.DifferenceEngine;
using log4net;
using System.Collections;

namespace plutext.client.word2007
{

    public class TextLine : IComparable
    {
        public string Line;
        public int _hash;

        public TextLine(string str)
        {
            Line = str.Replace("\t", "    ");
            _hash = str.GetHashCode();
        }
        #region IComparable Members

        public int CompareTo(object obj)
        {
            return _hash.CompareTo(((TextLine)obj)._hash);
        }

        #endregion
    }

    public class Skeleton : IDiffList
    {
        private static readonly ILog log = LogManager.GetLogger(typeof(Skeleton));

        // Constructor - make object from string
        public Skeleton(String doc)
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

            try {

                XmlDocument xmlDoc = new XmlDocument();
                xmlDoc.Load(new StringReader(doc));

                init(xmlDoc);

            }
            catch (Exception ex)
            {
                log.Debug(ex.ToString());
            }

        }

        public Skeleton(XmlDocument xmlDoc)
        {
            init(xmlDoc);
        }

        private void init(XmlDocument xmlDoc)
        {
            try
            {
                log.Debug(xmlDoc.OuterXml);

                XmlNamespaceManager nsmgr = new XmlNamespaceManager(xmlDoc.NameTable);
                nsmgr.AddNamespace(Namespaces.PLUTEXT_TRANSITIONS_NS_PREFIX, 
                                   Namespaces.PLUTEXT_TRANSITIONS_NS);

                XmlNode ribsNode = xmlDoc.SelectSingleNode("/dst:transitions/dst:ribs", nsmgr);

                foreach (XmlNode nodex in ribsNode.ChildNodes)
                {
                    String id = nodex.Attributes.GetNamedItem("id", Namespaces.PLUTEXT_TRANSITIONS_NS).Value;
                    if (id == null)
                    {
                        log.Debug("Encountered unexpected: " + nodex.OuterXml);

                        /* eg
                         * 
                         * <w:sectPr xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"><w:pgSz w:w="12240" w:h="15840"></w:pgSz><w:pgMar w:top="1440" w:right="1440" w:bottom="1440" w:left="1440" w:header="708" w:footer="708" w:gutter="0"></w:pgMar><w:cols w:space="708"></w:cols><w:docGrid w:linePitch="360"></w:docGrid></w:sectPr>
                         * 
                         */
                        continue;
                    }

                    if (nodex.Attributes.GetNamedItem("deleted", Namespaces.PLUTEXT_TRANSITIONS_NS) !=null
                          && nodex.Attributes.GetNamedItem("deleted", Namespaces.PLUTEXT_TRANSITIONS_NS).Value.Equals("true"))
                    {
                        log.Debug("Rib " + id + " deleted, so ignoring.");
                    }
                    else
                    {
                        ribs.Add(new TextLine(id));
                        log.Debug(id);
                    }
                }
            }
            catch (Exception ex)
            {
                log.Debug(ex.ToString());
            }
        }

        // Version number
        int version;

        // Ordered list of ribs
        System.Collections.IList ribs = new System.Collections.ArrayList();

        public System.Collections.IList Ribs
        {
            get { return ribs; }
            set { ribs = value; }
        }

        public static void difftest()
        {

            string BASE_DIR = @"C:\Documents and Settings\Jason Harrop\My Documents\plutext-word2007\plutext-word2007-solution\plutext-client-word2007\tests\diff-transitions\";
            string[] tests = { "base", "deleted", "inserted", "moved", "complex", "unrelated", "random" };


            for (int h = 0; h < tests.Length; h++)
            {

                XmlDocument left = new XmlDocument();
                left.Load(BASE_DIR + tests[h] + ".xml");

                for (int j = 0; j < tests.Length; j++)
                {

                    log.Debug("\n\r \n\r Testing " + tests[h] + " against " + tests[j]);

                    XmlDocument right = new XmlDocument();
                    right.Load(BASE_DIR + tests[j] + ".xml");

                    Skeleton inferredSkeleton = new Skeleton(left);
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
                    Dictionary<String, int> notHereInDest   = new Dictionary<String, int>();
                    Dictionary<String, int> notHereInSource = new Dictionary<String, int>();
                    //Populate the dictionaries
                    int insertPos = -1;
                    int i;
                    log.Debug("\n\r");
                    foreach (DiffResultSpan drs in diffLines)
                    {
                        switch (drs.Status)
                        {
                            case DiffResultSpanStatus.DeleteSource:
                                for (i = 0; i < drs.Length; i++)
                                {
                                    insertPos++;
                                    // Must be a new local insertion
                                    log.Debug(insertPos + ": " + ((TextLine)inferredSkeleton.GetByIndex(drs.SourceIndex + i)).Line
                                        + " not at this location in dest");
                                    String insertionId = ((TextLine)inferredSkeleton.GetByIndex(drs.SourceIndex + i)).Line;
                                    notHereInDest.Add(insertionId, insertPos);
                                }

                                break;
                            case DiffResultSpanStatus.NoChange:
                                for (i = 0; i < drs.Length; i++)
                                {
                                    insertPos++;
                                    log.Debug(insertPos + ": " + ((TextLine)inferredSkeleton.GetByIndex(drs.SourceIndex + i)).Line
                                        + "\t" + ((TextLine)serverSkeleton.GetByIndex(drs.DestIndex + i)).Line + " (no change)");

                                    // Nothing to do
                                }

                                break;
                            case DiffResultSpanStatus.AddDestination:
                                for (i = 0; i < drs.Length; i++)
                                {
                                    //insertPos++; // Not for a delete
                                    log.Debug(insertPos + ": " + ((TextLine)serverSkeleton.GetByIndex(drs.DestIndex + i)).Line
                                        + " not at this location in source");
                                    String deletionId = ((TextLine)serverSkeleton.GetByIndex(drs.DestIndex + i)).Line;
                                    notHereInSource.Add(deletionId, insertPos);

                                }

                                break;
                        }
                    }


                    Divergences divergences = new Divergences(de);

                    log.Debug("\n\r");


                    // How to make the dest (right) like the source (left)

                    foreach (DiffResultSpan drs in diffLines)
                    {
                        switch (drs.Status)
                        {
                            case DiffResultSpanStatus.DeleteSource:  // Means we're doing an insertion
                                for (i = 0; i < drs.Length; i++)
                                {
                                    String insertionId = ((TextLine)inferredSkeleton.GetByIndex(drs.SourceIndex + i)).Line;
                                    log.Debug(insertPos + ": " + insertionId
                                        + " is at this location in src but not dest, so needs to be inserted");

                                    try {

                                        int dicVal = notHereInSource[insertionId];
                                        // there is a corresponding delete, so this is really a move
                                        log.Debug("   " + insertionId + " is a MOVE "); 

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
                                            
                                            log.Debug("<transform op=move id=" + insertionId + "  pos=" + adjPos);  

                                            divergences.insert(insertionId); // change +1 to 0

                                            divergences.debugInferred();

                                            //if (rawPos + adjPos == divergences.currentPosition(insertionId))
                                            //{`
                                            //    log.Debug(".. that transform could be DISCARDED.");
                                            //}

                                            //divergences.move(insertionId, rawPos + adjPos);
                                        //}
                                    }
                                    catch (KeyNotFoundException knf)
                                    {
                                        // Just a new local insertion

                                        int adjPos = divergences.getTargetLocation(insertionId);

                                        log.Debug("<transform op=insert id=" + insertionId + "  pos=" + adjPos);

                                        divergences.insert(insertionId); // change +1 to 0

                                        divergences.debugInferred();


                                    }

                                }

                                break;
                            case DiffResultSpanStatus.NoChange:
                                for (i = 0; i < drs.Length; i++)
                                {

                                    log.Debug(insertPos + ": " + ((TextLine)inferredSkeleton.GetByIndex(drs.SourceIndex + i)).Line
                                        + "\t" + ((TextLine)serverSkeleton.GetByIndex(drs.DestIndex + i)).Line + " (no change)");

                                    //String id = ((TextLine)inferredSkeleton.GetByIndex(drs.SourceIndex + i)).Line;


                                }

                                break;
                            case DiffResultSpanStatus.AddDestination:
                                for (i = 0; i < drs.Length; i++)
                                {
                                    String deletionId = ((TextLine)serverSkeleton.GetByIndex(drs.DestIndex + i)).Line;
                                    log.Debug(insertPos + ": " + deletionId
                                        + " present at this location in dest but not source, so needs to be deleted");

                                    try
                                    {

                                        int dicVal = notHereInDest[deletionId];
                                        // there is a corresponding insert, so this is really a move
                                        log.Debug("   " + deletionId + " is a MOVE to elsewhere (" + dicVal + ")");
                                                                                
                                        // DO NOTHING

                                    }
                                    catch (KeyNotFoundException knf)
                                    {
                                        // Just a new local deletion

                                        log.Debug("Couldn't find " + deletionId + " so deleting");
                                        divergences.delete(deletionId);

                                        divergences.debugInferred();

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

        #region IDiffList Members

        public int Count()
        {
            return ribs.Count;
        }

        public IComparable GetByIndex(int index)
        {
            return (TextLine)ribs[index];
        }

        #endregion


    }
}
