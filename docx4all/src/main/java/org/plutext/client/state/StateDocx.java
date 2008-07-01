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
using Word = Microsoft.Office.Interop.Word;
using log4net;

namespace plutext.client.word2007
{
    /* Represent the configuration of the
     * document, and certain transient
     * document-level state information.
     */ 
    public class StateDocx
    {
        private static readonly ILog log = LogManager.GetLogger(typeof(StateDocx));

        public StateDocx(String docID)
        {
            log.Debug("StateDocx constructor fired");

            try
            {
                pkg = new Pkg(Globals.ThisAddIn.Application.ActiveDocument);

                // Get initial skeletonSnapshot
                ChunkServiceOverride ws = ChunkServiceOverride.getWebService();

                this.docID = docID;
                transforms = new TransformsCollection();
                skeletonSnapshot = ws.getSkeletonDocument(docID); // TODO - do this async

                // Set up the prepend and append strings
                // we need in order to use InsertXML method
                insertionHelper = new InsertionHelper();
            } 
            catch (Exception ex)
            {
                log.Debug(ex.Message + ex.StackTrace);
            }

        }

        private String skeletonSnapshot;


        // Set from App_DocumentOpen
        private String docID = null;
        public String DocID
        {
            get { return docID; }
            set { docID = value; }
        }

        // These three variables are read from document properties
        // TODO:- how to prevent user from changing these values
        // from the Word interface?

        private Boolean promptForCheckinMessage;
        public  Boolean PromptForCheckinMessage
        {
            get { return promptForCheckinMessage;}
            set { promptForCheckinMessage=value;}
        }

        private String chunkingStrategy;
        public String ChunkingStrategy
        {
            get { return chunkingStrategy; }
            set { chunkingStrategy = value; }
        }

        // Whether the cursor is currently in a 
        // content control
        // EXPERIMENTAL - see whether we can track this!
/*        private Boolean inControl = false;
        public Boolean InControl
        {
            get { return inControl; }
            set { inControl = value; }
        }
*/

        private Word.ContentControl currentCC = null;
        public Word.ContentControl CurrentCC
        {
            get { return currentCC; }
            set { currentCC = value; }
        }


        private Boolean initialized = false;
        public Boolean Initialized
        {
            get { return initialized; }
            set { initialized = value; }
        }


        Styles stylemap = new Styles();
        public Styles StyleMap
        {
            get { return stylemap; }
        }

        Pkg pkg = null;
        public Pkg Pkg
        {
            get { return pkg; }
            set { pkg = value; }
        }


        /* Maintain a collection of Transforms keyed by tSequenceNumber, so we 
         * can keep track of which ones have been applied.  */
        TransformsCollection transforms;
        public TransformsCollection Transforms
        {
            get { return transforms; }
        }

        //List<DeletedContentControl> deletedContentControls = new List<DeletedContentControl>();
        //public List<DeletedContentControl> DeletedContentControls
        //{
        //    get { return deletedContentControls; }
        //}



        //Boolean uptodate = true;
        //public Boolean Uptodate
        //{
        //    get { return (transforms.TSequenceNumberApplied == transforms.TSequenceNumberHighestFetched); }
        //    //set { uptodate = value; }
        //}

        private InsertionHelper insertionHelper;
        public InsertionHelper XmlInsertion
        {
            get { return insertionHelper; }
        }

        // Return the content control containing the cursor, or
        // null if we are outside a content control or the selection
        // spans the content control boundary
        public Word.ContentControl getActiveContentControl()
        {
            Word.Selection selection = Globals.ThisAddIn.Application.Selection;

            // Word.ContentControls ccs = selection.ContentControls;
            // only has a value if the selection contains an entire ContentControl

            // so how do you expand a selection to include the entire content control?
            // or can we ask whether a content control is active,
            // ie the selection is in it?

            // or iterate through the content controls in the active doc,
            // asking whether their range contains my range? YES

            foreach (Word.ContentControl ctrl in Globals.ThisAddIn.Application.ActiveDocument.ContentControls)
            {
                if (selection.InRange(ctrl.Range))
                {
                    //diagnostics("DEBUG - Got control");
                    return ctrl;
                }
                // else user's selection is totally outside the content control, or crosses its boundary
            }
            //if (stateDocx.InControl == true)

            return null;

        }

        /* Look at this document, and derive a skeleton from it. 
           The objective is to be able to compare the order of this document
           to the order known to the server.  */
        public String constructSkeleton()
        {
            /* There are several ways we could seek to do this.  
             * 
             * One would be to loop through the content controls (which assumes
             * they are ordered).
             * 
             * Another would be to transform the document via XSLT, into the
             * form we want.  This has the advantage of also identifying text
             * which is not in a content control.
             * 
             
             */

            return null;


        }

        public class TransformsCollection
        {
            // can I extend dictionary?

            private Int32 tSequenceNumberAtLoadTime = Int32.Parse(Util.getCustomDocumentProperty(Globals.ThisAddIn.Application.ActiveDocument, CustomProperties.DOCUMENT_TRANSFORM_SEQUENCENUMBER));

            //private Int32 tSequenceNumberApplied = Int32.Parse(Util.getCustomDocumentProperty(Globals.ThisAddIn.Application.ActiveDocument, CustomProperties.DOCUMENT_TRANSFORM_SEQUENCENUMBER));
            //public Int32 TSequenceNumberApplied
            //{
            //    get { return tSequenceNumberApplied; }
            //    set { tSequenceNumberApplied = value; }
            //}

            private Int32 tSequenceNumberHighestFetched = Int32.Parse(Util.getCustomDocumentProperty(Globals.ThisAddIn.Application.ActiveDocument, CustomProperties.DOCUMENT_TRANSFORM_SEQUENCENUMBER));
            public Int32 TSequenceNumberHighestFetched
            {
                get { return tSequenceNumberHighestFetched; }
                set { tSequenceNumberHighestFetched = value; }
            }


            /* Maintain a collection of Transforms keyed by tSequenceNumber, so we 
             * can keep track of which ones have been applied. 
             * 
               Key is SequenceNumber, not t.ID, since TransformStyle type doesn't have an 
               underlying SDT.  Besides, if 2 additions related to the same SDT, the
               keys would collide.
             */
            List<TransformAbstract> transformsBySeqNum = new List<TransformAbstract>();
            public List<TransformAbstract> TransformsBySeqNum
            {
                get { return transformsBySeqNum; }
            }

            public void add(TransformAbstract t, Boolean updateHighestFetched)
            {
                // Check it is not already present before adding
                // This list should be pretty short, so there is
                // little harm in just iterating through it
                foreach (TransformAbstract ta in TransformsBySeqNum)
                {
                    if (ta.SequenceNumber == t.SequenceNumber)
                    {
                        log.Debug(t.SequenceNumber + " already registered.  Ignoring.");
                        return;
                    }
                }

                transformsBySeqNum.Add(t);

                if (updateHighestFetched && t.SequenceNumber > tSequenceNumberHighestFetched)
                {
                    // Only update highest fetched if this addition is 
                    // a result of fetch updates. (If it is a result
                    // of local transmits, there is a possibility that
                    // someone else may have generated an snum (ie while
                    // our transmitLocalChanges was running), and we 
                    // wouldn't want to miss that

                    tSequenceNumberHighestFetched = t.SequenceNumber;
                }
            }

            public List<TransformAbstract> getTransformsBySdtId(String id, Boolean includeLocals)
            {
                List<TransformAbstract> list = new List<TransformAbstract>();

                foreach (TransformAbstract ta in transformsBySeqNum)
                {
                    if (ta.ID.Equals(id))
                        {
                            if (!ta.Local || includeLocals)
                            {
                                list.Add(ta);
                                log.Debug("Instance  --  found a transform applicable to Sdt " + id);
                            }
                        }

                }

                return list;
            }

        }


    }
}
