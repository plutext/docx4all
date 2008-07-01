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
using System.Drawing;
using System.Data;
using System.Text;
using System.Threading;
using System.Windows.Forms;
using Word = Microsoft.Office.Interop.Word;
using System.IO;
using System.Xml;
using System.Xml.XPath;
using System.Xml.Xsl;
using plutext.client.word2007.DifferenceEngine;
using log4net;

namespace plutext.client.word2007
{
    /** This class is the real workhorse.  
     */ 
    public class Mediator
    {
        /* Design goals:
         * 
         * 1. Don't use content control entry and exit handlers:
         *    (i)  these are hard to get right
         *    (ii) if stuff happens at that point, user can't navigate smoothly around doc
         * 
         * 2. Pick up eg results of find/replace; any content user manages to
         *    enter outside a content control; moves.
         * 
         * 3. Efficient use of XML representation ie .WordOpenXML
         *    .. only get this once per Callback.
         * 
         * 4. Efficient use of InsertXML (just used to create new document).
         * 
         * 5. Granular control of differencing, which avoids
         *    Word's Compare which sometimes replaces the entire SDT
         *    with w:customXmlInsRangeStart&End, w:customXmlDelRangeStart&End
         * 
         * Principles (to be added to end user documentation):
         * 
         * (i) All remote changes must be fetched & applied before a user 
         *     is able to transmit his local changes
         * 
         * (ii)Any tracked changes in a cc must be resolved before the 
         *     use is allowed to commit that one
         * 
         * TODO:
         * 
         * - transmit moves, deletions
         * - markup insertions!
         * - styles
         * - XSLT optimisations
         * 
         */

        private static readonly ILog log = LogManager.GetLogger(typeof(Mediator));

        Boolean remoteUpdatesNeedApplying = false;
        // False once all are applied, even if there are conflicts.
        // Purpose: determine whether button is enabled or greyed out

        // NB, this is critical, to get a variable which matches the lifetime of the event handler
        // If you just use Globals.ThisAddIn... directly in establishEventHandlers
        // you will succumb to the so-called "My Button Stopped Working Issue"
        // which manifests itself as:
        //      System.Runtime.InteropServices.InvalidComObjectException: COM object that has been separated from its underlying RCW cannot be used.
        //      at System.Runtime.InteropServices.UCOMIConnectionPoint.Advise(Object pUnkSink, Int32& pdwCookie)
        //      at Microsoft.Office.Interop.Word.DocumentEvents2_EventProvider.add_ContentControlOnEnter(DocumentEvents2_ContentControlOnEnterEventHandler )
        //      at Microsoft.Office.Interop.Word.DocumentClass.add_ContentControlOnEnter(DocumentEvents2_ContentControlOnEnterEventHandler )
        // when the WebReference is later invoked (prob because that is when garbage collection occurs)
        // see further Carter & Lippert p30 and Vogel 2004 article.  RCW = runtime callable wrapper
        public Microsoft.Office.Interop.Word.Document myDoc = Globals.ThisAddIn.Application.ActiveDocument;

        private ContentControlEventHandler serverTo;
        public ContentControlEventHandler ServerTo
        {
            get { return serverTo; }
        }

        private StateDocx stateDocx;
        public StateDocx StateDocx
        {
            get { return stateDocx;}
        }

        // So this doesn't get garbage collected.
        public System.Threading.Timer stateTimer = null;

        public Mediator(ContentControlEventHandler serverTo, StateDocx stateDocx )
        {

            //Skeleton.difftest();

            this.serverTo = serverTo;
            this.stateDocx = stateDocx;

            ws = ChunkServiceOverride.getWebService();

            try {
                // Timer stuff 
                Console.Beep();
                AutoResetEvent autoEvent = new AutoResetEvent(false);
                TimerCallback timerDelegate =
                    new TimerCallback(mediatorCallback);

                // Useful to comment this out to test/debug certain things
                // (eg collision handling)
                stateTimer = new System.Threading.Timer(timerDelegate, null, 20000, 60000); // 20 sec, 1.5 min


            }
            catch (Exception ex)
            {
                log.Debug(ex.Message + ex.StackTrace);
            }
        }


        ChunkServiceOverride ws = null;

/* ****************************************************************************************
 *          FETCH REMOTE UPDATES (in background)
 * **************************************************************************************** */ 

        [STAThread]
        void mediatorCallback(Object stateInfo)
        {

            int threadId = System.Threading.Thread.CurrentThread.ManagedThreadId;
            log.Debug( ".. timer thread .." + threadId);

            /*
             * System.Runtime.InteropServices.COMException was unhandled
  Message="The message filter indicated that the application is busy. (Exception from HRESULT: 0x8001010A (RPC_E_SERVERCALL_RETRYLATER))"
  Source="Microsoft.Office.Interop.Word"
  ErrorCode=-2147417846
  StackTrace:
       at Microsoft.Office.Interop.Word.DocumentClass.get_Name()
       at plutext.client.word2007.Mediator.mediatorCallback(Object stateInfo) in C:\Documents and Settings\Jason Harrop\My Documents\plutext-word2007\plutext-word2007-solution\plutext-client-word2007\Mediator.cs:line 133
       at System.Threading._TimerCallback.TimerCallback_Context(Object state)
       at System.Threading.ExecutionContext.Run(ExecutionContext executionContext, ContextCallback callback, Object state)
       at System.Threading._TimerCallback.PerformTimerCallback(Object state)
 
             */

            DateTime startTime = DateTime.Now;
            log.Debug(startTime);

            Console.Beep();

            //- gets updates from server
            fetchUpdates();

            DateTime stopTime = DateTime.Now;
            log.Debug("Thread " + threadId + " finished: " + stopTime);

            /* Compute the duration between the initial and the end time. */
            TimeSpan duration = stopTime - startTime;
            log.Debug("\n\r " + threadId + " Time taken: " + duration + "\n\r");

        }

        void fetchUpdates()
        {
            //diagnostics("Local tSequence for " + myDoc.Name + " : " + tSequenceNumber);
            log.Debug(myDoc.Name + ".. .. fetchUpdates, from " + stateDocx.Transforms.TSequenceNumberHighestFetched);

            String[] updates = null;



            try
            {
                ws = ChunkServiceOverride.getWebService();
                //ChunkServiceOverride ws = ChunkServiceOverride.getWebService();
                updates = ws.getTransforms(stateDocx.DocID,
                                        stateDocx.Transforms.TSequenceNumberHighestFetched);
            }
            catch (Exception ex)
            {
                log.Debug(ex.Message + ex.StackTrace);
                Globals.ThisAddIn.Application.StatusBar = "Fetch error";
            }

            log.Debug(myDoc.Name + " sequence = " + updates[0]);
            if (updates.Length < 2)
            {
                log.Debug(myDoc.Name + " ERROR!!!");
                Globals.ThisAddIn.Application.StatusBar = "Fetch error";
            }
            else
            {
                log.Debug(myDoc.Name + " transforms = " + updates[1]);

                if (Int32.Parse(updates[0]) > stateDocx.Transforms.TSequenceNumberHighestFetched)
                {
                    stateDocx.Transforms.TSequenceNumberHighestFetched = Int32.Parse(updates[0]);
                    Boolean appliedTrue = false;
                    Boolean localTrue = false;
                    registerTransforms(updates[1], appliedTrue, localTrue);
                    Globals.ThisAddIn.Application.StatusBar = "Fetched " + updates[0];

                    remoteUpdatesNeedApplying = true;
                }
                else
                {
                    Globals.ThisAddIn.Application.StatusBar = "No remote updates";

                    // Do not do: remoteUpdatesNeedApplying = false;
                    // since there may be some from the last time this was executed
                }


            }
        }

        /* Put transforms received from server into the transforms collection. */
        public void registerTransforms(String transforms, Boolean setApplied, Boolean setLocal)
        {
            log.Debug(myDoc.Name + ".. .. registerTransforms");

            // Parse the XML document, and put each transform into the transforms collection
            XmlDocument doc = new XmlDocument();
            doc.LoadXml(transforms);
            XmlNode docNode = doc.DocumentElement;
            XmlNamespaceManager nsmgr = new XmlNamespaceManager(doc.NameTable);
            nsmgr.AddNamespace("p", Namespaces.PLUTEXT_TRANSFORMS_NAMESPACE);

            XmlNodeList oNodeList = docNode.SelectNodes("/p:transforms/p:t", nsmgr);
            for (int x = 0; x < oNodeList.Count; x++)
            {
                TransformAbstract t = TransformHelper.construct(oNodeList.Item(x));
                registerTransform(t, setApplied, setLocal, true);
            }
        }

        public void registerTransform(TransformAbstract t, Boolean setApplied, Boolean setLocal, 
            Boolean updateHighestFetched)
        {
            if (setApplied) { t.Applied = true; }
            if (setLocal) { t.Local = true; }

            log.Debug("Instance " + myDoc.Name + " -- Registering " + t.SequenceNumber);
            try
            {
                stateDocx.Transforms.add(t, updateHighestFetched);
                log.Debug(".. done.");
            }
            catch (Exception e)
            {
                log.Debug(".. not done: " + e.Message);
                // Ignore - An item with the same key has already been added.
            }

        }


/* ****************************************************************************************
 *          APPLY REMOTE UPDATES
 * **************************************************************************************** */

        Divergences divergences = null;

        public Divergences Divergences
        {
            get { return divergences; }
            //set { divergences = value; }
        }

    public void applyRemoteChanges()
    {
        // TODO: grey out if there are no remote updates to apply

        // get up to date current state
        pkgB = new Pkg(myDoc);

        DateTime startTime = DateTime.Now;
        log.Debug(startTime);


        /* Create a DiffReport object, which tells us 
         * the difference between what Sdts are actually in the document,
         * and what its state should be, given the last round of server
         * updates.
         * 
         * We'll use this to adjust the insert position of new sdt's.
         * 
         * As we perform any insert/delete transform, we'll update
         * the DiffReport object. 
         * 
         * Note that if this document contains text which is not inside
         * a content control, then we won't know whether to insert
         * before that or after it.  This is an ambiguous situation, 
         * but at least the user will be able to see where the insertion
         * has occurred, since it will be (TODO) marked up.
         */

        // TODO - this is a bit expensive, and it is only necessary
        // if the updates include insertions, so look at them first

        //if (updatesIncludeInsertions)
        //{
            Skeleton actuals  = new Skeleton(pkgB.getInferedSkeleton());
            Skeleton oldserver = new Skeleton(stateDocx.Pkg.getInferedSkeleton());

            DiffEngine drift = new DiffEngine();
            drift.ProcessDiff(oldserver, actuals);
            divergences = new Divergences(drift);

            /* For example

                1324568180	1324568180 (no change)
                1911345834	1911345834 (no change)
                ---	293467343 not at this location in source  <---- if user deletes
                884169107	884169107 (no change)
                528989532	528989532 (no change)
             */
 

        //}


        applyUpdates(pkgB);


        // Replace user's document
        // Two approaches for replacing user's document:
        /* Approach 1: copy/paste new content into existing document.
         *             One downside is that all the rsid's are replaced with 
         *             those of the existing editing session.
         *             Turning them off for the duration of the paste doesn't preserve
         *             the original rsids, but rather, inserts
         *             them with w:rsidR="00000000"
         *             But if we always leave opts.StoreRSIDOnSave = false
         *             then this might be ok. (But how much does this screw
         *             Compare accuracy?)
         * Approach 2: Assuming rsid's in document B were preserved (have to verify this),
         *             close the original document, and treat the new one as our doc
         *             going forward.  
         * We use approach 1 - rsid's disabled in ThisAddIn.
         * 
         * Further considerations:
         * 
         * - can't readily take advantage of rsid's in our own diff algorithm
         * 
         * - we'd have to remove them when we canonicalise. 
         */

        // In either case, we need to make document B
        Word.Document wordDocB = pkgB.createDocument();
        Word.Document wordDocC = null;
        Word.Range sourceRange = null;
        object omissing = Type.Missing;

        // Replace user's document with B.
        sourceRange = wordDocB.Content;

        sourceRange.Copy();

        //Word.Options opts = Globals.ThisAddIn.Application.Options;            

        Word.Range targetRange = myDoc.Content;
        targetRange.Paste();

        System.Boolean savechanges = false;
        object savechangesObj = (object)savechanges;
        ((Word._Document)wordDocB).Close(ref savechangesObj, ref omissing, ref omissing);

        // Update the snapshots        
        // We used to do this by creating a new Pkg from the document
        // using stateDocx.Pkg = new Pkg(myDoc);
        // The intent was to ensure the format of the XML is as Word emits it,
        // so we get a reliable comparison.
        // However, with our canonicalisation algorithm, this is unnecessary.

        stateDocx.Pkg = (Pkg)pkgB.Clone();

        // logout


        DateTime stopTime = DateTime.Now;
        log.Debug(" finished: " + stopTime);

        /* Compute the duration between the initial and the end time. */
        TimeSpan duration = stopTime - startTime;
        log.Debug("\n\r  Time taken: " + duration + "\n\r");

        Globals.ThisAddIn.Application.StatusBar = "n Remote changes applied in " + duration.Seconds + "s";

        remoteUpdatesNeedApplying = false;

    }


    /* Apply registered transforms. */
    public void applyUpdates(Pkg pkg)
    {
        /* Note well that it is important to the correct functioning that the 
         * updates are applied IN ORDER.  
         */
 
        log.Debug(myDoc.Name + ".. .. applyUpdates");

        // loop through, fetch, apply 
        List<TransformAbstract> transformsBySeqNum = stateDocx.Transforms.TransformsBySeqNum;
        List<TransformAbstract> discards = new List<TransformAbstract>();
        foreach (TransformAbstract t in transformsBySeqNum)
        {
            // OPTIMISATION: could do the most recent only for each cc
            // (ie reverse order), except for MOVES and INSERTS, which need to 
            // be done in order.

            if (t.Applied)  // then it shouldn't be in the list ?!
            {
                if (stateDocx.Transforms.TSequenceNumberHighestFetched > t.SequenceNumber)
                {
                    discards.Add(t);
                }
                continue;
            }

            log.Debug(".. applying " + t.SequenceNumber );

            Int32 result = applyUpdate(pkg, t);

            log.Debug(".. applied " + t.SequenceNumber);

            if (result > 0)
            {
                // Applied, so can discard, provided highest fetched is higher
                // than this snum (otherwise it will just get fetched again!)
                if (stateDocx.Transforms.TSequenceNumberHighestFetched > t.SequenceNumber)
                {
                    discards.Add(t);
                }
            }
            else
            {
                log.Debug("Failed to apply transformation " + t.SequenceNumber);
            }


        }

        // Now remove the discards
        foreach (TransformAbstract ta in discards)
        {
            transformsBySeqNum.Remove(ta);  
        }

    }

    /* On success, returns the transformation's tSequenceNumber; otherwise, 0 */
    private Int32 applyUpdate(Pkg pkg, TransformAbstract t)
    {
        Int32 result;

        log.Debug("applyUpdate " + t.GetType().Name + " - " + t.SequenceNumber);

        if (t.GetType().Name.Equals("TransformInsert")
                || t.GetType().Name.Equals("TransformMove")
            )
        {
            result = t.apply(this, pkg);
            t.Applied = true;
            log.Debug(t.SequenceNumber + " applied (" + t.GetType().Name + ")");
            return result;
        }
        else if (t.GetType().Name.Equals("TransformStyle"))
        {
            // TODO - Implement TransformStyle
            // that class is currently non functional.
            result = t.apply(this, pkg);
            t.Applied = true;
            log.Debug(t.SequenceNumber + " UNDER CONSTRUCTION (" + t.GetType().Name + ")");
            return result;

        }
        else if (t.GetType().Name.Equals("TransformUpdate"))
        {
            String currentXML = stateDocx.Pkg.StateChunks[t.ID].Xml;

            // The update we will insert in one that contains the results
            // of comparing the server's SDT to the user's local one.
            // This will allow the user to see other people's changes.
            ((TransformUpdate)t).markupChanges(pkg.StateChunks[t.ID].Xml);

            result = t.apply(this, pkg);
            t.Applied = true;
            log.Debug(t.SequenceNumber + " applied (" + t.GetType().Name + ")");

            if (currentXML.Equals(pkg.StateChunks[t.ID].Xml) && !t.Local)
            {
                sdtChangeTypes[t.ID] = TrackedChangeType.OtherUserChange;
            }
            else
            {
                sdtChangeTypes[t.ID] = TrackedChangeType.Conflict;
            }

            return result;

        }
        else
        {

            log.Debug(" How to handle " + t.GetType().Name);
            return -1;
        }

    }




/* ****************************************************************************************
 *          ACCEPT REMOTE CHANGES
 * **************************************************************************************** */

        Dictionary<string, TrackedChangeType> sdtChangeTypes = new Dictionary<string, TrackedChangeType>();

        public enum TrackedChangeType
        {
            Conflict,
            OtherUserChange,
            NA
        }

        public void acceptNonConflictingChanges()
        {

            // Iterate through the content controls in the document
            foreach (Word.ContentControl ctrl in Globals.ThisAddIn.Application.ActiveDocument.ContentControls)
            {
                String id = ctrl.ID;

                if (ctrl.Range.Revisions.Count > 0)
                {
                    if (sdtChangeTypes[id] == TrackedChangeType.Conflict)
                    {
                        log.Debug("Change to " + id + " is a conflict, so leave Tracked");

                        // TODO: how to remove this setting, once
                        // user has manually fixed??

                    }
                    else 
                    {
                        log.Debug("Change to " + id + " can be accepted.");
                        ctrl.Range.Revisions.AcceptAll();
                        sdtChangeTypes[id] = TrackedChangeType.NA;
                    }
                }
            }
        }


/* ****************************************************************************************
 *          TRANSMIT LOCAL CHANGES
 * **************************************************************************************** */ 

        //  make a document.xml .. this will become document B, and it is to this that
        //  we apply transforms (eg splitting an SDT with 2 paras into 2 SDTs).   
        //  (this doc will eventually replace existing document) 
        Pkg pkgB;

        public void transmitLocalChanges()
        {

            /* ENTRY CONDITION
             * Remote changes received and applied
             *
             * This is critical, so that we can guarantee
             * that any differences are local changes
             * which need to be transmitted.
             * 
             * TODO - automatically get and apply changes.
             * If there are any, tell the user "Remote changes
             * detected.  Please review, then try transmit 
             * again."
             * 
             */ 

            ////    (Button should be greyed out if this is not the case)
            //if (remoteUpdatesNeedApplying)
            //{
            //    // Ultimately, we'll grey the button out if this is the case.

            //    // Until then ...
            //    MessageBox.Show("Please 'Insert Remote Updates' before transmitting your changes");
            //    return;
            //}


            // Look for local modifications
            // - commit any which are non-conflicting (send these as TRANSFORMS)
            transmitContentUpdates();

            transmitStyleUpdates();

            Globals.ThisAddIn.Application.StatusBar = "Local changes transmitted";
        }


        void transmitContentUpdates()
        {

            log.Debug(myDoc.Name + ".. .. transmitContentUpdates");

            // The list of transforms to be transmitted
            List<TransformAbstract> transformsToSend = new List<TransformAbstract>();

            /* Pre processing  - chunk as required, so that there is
             * only one paragraph in the sdt.
             * 
             * All clients (ie this Word add in, docx4all) should do this
             * if we're set to chunk on each paragraph. */

            // TODO only if chunking is required.
            List<Word.ContentControl> multiparaSdts = new List<Word.ContentControl>();
            foreach (Word.ContentControl cc in Globals.ThisAddIn.Application.ActiveDocument.ContentControls)
            {
                if (Chunker.containsMultipleBlocks(cc)) // TODO - && no remote changes to apply
                {
                    multiparaSdts.Add(cc);
                }
            }  // NB the actual work is done below in Chunker.chunk(cc)

            /* START workaround (Part 1) for Word bug which affects this Word addin.
             * (Won't affect docx4all) */
            List<String> taintedByStyleSeparator = new List<string>();
            foreach (Word.ContentControl cc in multiparaSdts)
            {
                taintedByStyleSeparator.Add(cc.ID);
                taintedByStyleSeparator.AddRange(Chunker.chunk(cc));
            }
            /* END workaround (Part 1)  */


            /* Any content not in an Sdt, put it in an Sdt.
             * 
             * (This shouldn't be necessary in docx4all, provided docx4all can
             *  guarantee that any content the user enters is in a content control.
             *  What if they make a change in source view?  Maybe we should just
             *  disable that when in collaborative mode.)
             * 
             * Could use XSLT to do this, or do it programmatically
             * Also, flatten any content controls which user has managed to nest via a paste
             * Also, fix any content controls which have changed their nature to inline.
             */

                // TODO - later

            // If we do this, replace the doc with the results of the transform?



            Pkg pkg = new Pkg(myDoc);

            /* START workaround (Part 2) for Word bug which affects this Word addin.
             * (Won't affect docx4all) */
            // clean style separator on each affected chunk
            foreach (String taintedId in taintedByStyleSeparator)
            {
                StateChunk taintedChunk = pkg.StateChunks[taintedId];

                taintedChunk.removeStyleSeparator();
            }
            /* END workaround (Part 2) */

            // Indentify structural changes (ie moves, inserts, deletes)
            // If skeletons are different, there must be local changes 
            // which we need to transmit

            /* For example

                1324568180	1324568180 (no change)
                1911345834	1911345834 (no change)
                ---	293467343 not at this location in source  <---- if user deletes
                884169107	884169107 (no change)
                528989532	528989532 (no change)
             */

            XmlDocument inferredSkelDoc = pkg.getInferedSkeleton();

            // compare the inferredSkeleton to serverSkeleton
            Skeleton inferredSkeleton = new Skeleton(inferredSkelDoc);

            String serverSkeletonStr = ws.getSkeletonDocument(stateDocx.DocID);
            Skeleton serverSkeleton = new Skeleton(serverSkeletonStr);

            // OK, do it
            createTramsformsForStructuralChanges(pkg, transformsToSend,
                inferredSkeleton, serverSkeleton);


            Boolean someTransmitted = false;
            Boolean someConflicted = false;

            int insertPos;
            int i;
            try
            {
                String checkinComment = null;

                insertPos = -1;
                foreach (KeyValuePair<string, StateChunk> kvp in pkg.StateChunks)
                //foreach (Word.ContentControl cc in Globals.ThisAddIn.Application.ActiveDocument.ContentControls)
                {
                    insertPos++;

                    StateChunk chunkCurrent = (StateChunk)kvp.Value;
                    String stdId = chunkCurrent.ID;


                    StateChunk chunkOlder = null;

                    try
                    {
                        chunkOlder = stateDocx.Pkg.StateChunks[stdId];
                        if (chunkCurrent.Xml.Equals(chunkOlder.Xml))
                        {
                            continue;
                        }
                        log.Debug("textChanged:");
                        log.Debug("FROM " + chunkOlder.Xml);
                        log.Debug("");
                        log.Debug("TO   " + chunkCurrent.Xml);
                        log.Debug("");

                    } catch (KeyNotFoundException knf) {

                        log.Debug("Couldn't find " + stdId + " .. handled already ...");

                        continue;

                    }

                    // If we get this far, it is an update
                    // We don't need to worry about the possibility that it has
                    // changed remotely, since we checked all updates
                    // on server had been applied before entering this method.

                    if ( chunkCurrent.containsTrackedChanges())
                    {
                        // This is a conflicting update, so don't transmit ours.
                        // Keep a copy of what this user did in StateChunk
                        // (so that 

                        log.Debug("Conflict! Local edit " + stdId + " not committed.");
                        someConflicted = true;
                        continue;
                    }

                    if (stateDocx.PromptForCheckinMessage)
                    {

                        if (checkinComment == null)
                        {

                            log.Debug("Prompting for checkin message...");
                            // NB with this model, we are no longer applying a 
                            // comment to a single paragraph.  The server will stick
                            // the comment on each affected rib.

                            formCheckin form = new formCheckin();
                            //form.Text = "Changes to '" + plutextTabbedControl.TextBoxChunkDisplayName.Text + "'";
                            form.Text = "Changes ";
                            using (form)
                            {
                                //if (form.ShowDialog(plutextTabbedControl) == DialogResult.OK)
                                if (form.ShowDialog() == DialogResult.OK)
                                {
                                    checkinComment = form.textBoxChange.Text;
                                }
                            }
                        }
                    }
                    else
                    {
                        checkinComment = "edited";
                    }

                    TransformUpdate tu = new TransformUpdate();
                    tu.attachSdt(chunkCurrent.Xml);
                    tu.ID = chunkCurrent.ID;
                    transformsToSend.Add(tu);
                }

                // Ok, now send what we have

                StringBuilder transforms = new StringBuilder();
                transforms.Append("<p:transforms xmlns:p='" + Namespaces.PLUTEXT_TRANSFORMS_NAMESPACE + "'>");
                foreach (TransformAbstract ta in transformsToSend)
                {
                    transforms.Append(ta.marshal().OuterXml);
                }
                transforms.Append("</p:transforms>");

                checkinComment = " whatever!";

                log.Debug("TRANSMITTING " + transforms.ToString());

                String[] result = ws.transform(stateDocx.DocID, transforms.ToString(), checkinComment);

                log.Debug("Checkin also returned results" );

                i = 0;
                // We do what is necessary to in effect apply the changes immediately,
                // so there is no issue with the user making changes before it
                // is applied, and those changes getting lost
                // In strict theory, we shouldn't do this, because they'll end 
                // up in the list in the wrong order.
                // But we actually know there are no conflicting transforms with
                // lower snums, so it isn't a problem.
                // Remember the indocument controls still have the dodgy StyleSeparator, 
                // so we will have to make allowance for that later
                // (that is, until we're able to transform them away ...)
                Boolean appliedTrue = true; 
                Boolean localTrue = true;   // means it wouldn't be treated as a conflict
                // Handle each result appropriately
                foreach (TransformAbstract ta in transformsToSend)
                {
                    log.Debug(ta.ID + " " + ta.GetType().Name + " result " + result[i]);

                    // Primarily, we're expecting sequence numbers

                    // At present, op="update" returns a transform
                    // but it should be no different to what we sent
                    // except that its tag is updated
                    /* When registering these transforms, don't update highest fetched, 
                     * because other clients could have transmitted changes to the server
                     * while this method was running, and we wouldn't want to miss those
                     * changes. */
                    if (result[i].Contains("xmlns"))
                    {

                        XmlDocument doc = new XmlDocument();
                        doc.LoadXml(result[i]);

                        TransformAbstract t = TransformHelper.construct(doc.DocumentElement);
                        registerTransform(t, appliedTrue, localTrue, false);

                        // Set the in-document tag to match the one we got back
                        // ?? the actual sdt or the state chunk?
                        getContentControlWithId(t.ID).Tag = t.Tag;

                        /* A problem with this approach is that it doesn't 
                         *  get rid of any StyleSeparator workaround in an SDT.
                         * 
                         * It doesn't get rid of it now, and it doesn't register
                         * a transform which will be applied later (which would
                         * get rid of it then).  
                         * 
                         * But this doesn't matter, since the Style Separator never
                         * gets saved on the server, so no other user will receive
                         * it; and when this user next re-opens the document, they
                         * won't have it either.*/
                    }
                    else
                    {
                        ta.SequenceNumber = int.Parse(result[i]);
                        registerTransform(ta, appliedTrue, localTrue, false);
                    }



                    i++;
                }

                someTransmitted = true;

                //// Need to wrap in a transforms element
                //result = "<p:transforms xmlns:p='" + Namespaces.PLUTEXT_TRANSFORMS_NAMESPACE + "'>"
                //            + result
                //            + "</p:transforms>";

                //log.Debug("Parsing " + result);



            }
            catch (Exception e)
            {
                log.Debug(e.Message);
                log.Debug(e.StackTrace);
            }

            String checkinResult = null;

            if (someTransmitted)
            {
                checkinResult = "Your changes were transmitted successfully.";
                if (someConflicted)
                    checkinResult += " But there were one or more conflicts.";
            }
            else
            {
                checkinResult = "Your changes were NOT transmitted.";
                if (someConflicted)
                    checkinResult += " This is because there were conflicts.";
            }

            MessageBox.Show(checkinResult);

        }

        void createTramsformsForStructuralChanges(Pkg pkg , List<TransformAbstract> transformsToSend,
            Skeleton inferredSkeleton, Skeleton serverSkeleton)
        {

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
            Dictionary<String, int> notHereInDest = new Dictionary<String, int>();
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

                            try
                            {

                                int dicVal = notHereInSource[insertionId];
                                // there is a corresponding delete, so this is really a move
                                log.Debug("   " + insertionId + " is a MOVE");

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
                                // delete first (update divergences object)
                                divergences.delete(insertionId); // remove -1

                                int adjPos = divergences.getTargetLocation(insertionId);

                                log.Debug("<transform op=move id=" + insertionId + "  pos=" + adjPos);

                                divergences.insert(insertionId); // change +1 to 0

                                divergences.debugInferred();

                                log.Debug("<transform op=move id=" + insertionId + "  pos=" + adjPos);

                                StateChunk sc = pkg.StateChunks[insertionId];

                                TransformMove tm = new TransformMove();
                                tm.Pos = adjPos.ToString();
                                tm.ID = sc.ID;
                                //tm.attachSdt(sc.Xml);
                                transformsToSend.Add(tm);

                                log.Debug("text moved:");

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
                                log.Debug("Couldn't find " + insertionId + " so inserting at "
                                    + adjPos);

                                divergences.insert(insertionId); // change +1 to 0

                                divergences.debugInferred();

                                StateChunk sc = pkg.StateChunks[insertionId];

                                TransformInsert ti = new TransformInsert();
                                ti.Pos = adjPos.ToString();
                                ti.ID = sc.ID;
                                ti.attachSdt(sc.Xml);
                                transformsToSend.Add(ti);

                                log.Debug("text Inserted:");
                                log.Debug("TO   " + sc.Xml);
                                log.Debug("");

                            }

                        }

                        break;
                    case DiffResultSpanStatus.NoChange:
                        for (i = 0; i < drs.Length; i++)
                        {

                            log.Debug(insertPos + ": " + ((TextLine)inferredSkeleton.GetByIndex(drs.SourceIndex + i)).Line
                                + "\t" + ((TextLine)serverSkeleton.GetByIndex(drs.DestIndex + i)).Line + " (no change)");

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

                                TransformDelete td = new TransformDelete(deletionId);
                                transformsToSend.Add(td);

                                log.Debug("text deleted:");

                            }
                        }

                        break;
                }
            }




        }

        void transmitStyleUpdates()
        {

            log.Debug(myDoc.Name + ".. .. transmitStyleUpdates");

            try
            {
                // TODO
                String newStyles = ""; // stateDocx.StyleMap.identifyAlteredStyles();
                if (newStyles.Equals(""))
                {
                    log.Debug("styles haven't Changed ..");
                }
                else
                {
                    log.Debug("stylesChanged");
                    log.Debug("Committing new/updated styles" + newStyles);
                    //stateDocx.TSequenceNumberHighestSeen = Int32.Parse(ws.style(stateDocx.DocID, newStyles));                    
                    String[] result = { "", "" };

                    // TODO - call transforms
                    //result = ws.style(stateDocx.DocID, newStyles);

                    log.Debug(result[1]);

                    Boolean appliedTrue = true; // Don't have to do anything more
                    Boolean localTrue = true;
                    registerTransforms(result[1], appliedTrue, localTrue);
                      // TODO, can't use that, since it automatically updates highest fetched.

                }
            }
            catch (Exception e)
            {
                MessageBox.Show(e.Message + e.StackTrace);
            }
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

        public Word.ContentControl getContentControlWithId(String id)
        {
            foreach (Word.ContentControl ctrl in Globals.ThisAddIn.Application.ActiveDocument.ContentControls)
            {
                if ( ctrl.ID !=null && ctrl.ID.Equals(id))
                {
                    return ctrl;
                }
            }
            return null;
        }



    }
}
