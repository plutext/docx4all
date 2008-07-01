using System;
using System.Collections.Generic;
using System.Text;
using System.Xml;
using System.Xml.Xsl;
using log4net;
using Word = Microsoft.Office.Interop.Word;


namespace plutext.client.word2007
{
    public class Pkg : ICloneable
    {

        private static readonly ILog log = LogManager.GetLogger(typeof(Pkg));

        XmlDocument pkg;
        XmlNamespaceManager nsmgr;

        public XmlDocument PkgXmlDocument
        {
            get { return pkg; }
        }

        public Pkg(Word.Document wordDoc)  
        {
            pkg = new XmlDocument();
            pkg.LoadXml(wordDoc.WordOpenXML);
            nsmgr = new XmlNamespaceManager(pkg.NameTable);
            nsmgr.AddNamespace("w", Namespaces.WORDML_NAMESPACE);

            initChunks();
        }

        public Pkg(String pkgXml)  //ie myDoc.WordOpenXML
        {
            pkg = new XmlDocument();
            pkg.LoadXml(pkgXml);
            nsmgr = new XmlNamespaceManager(pkg.NameTable);
            nsmgr.AddNamespace("w", Namespaces.WORDML_NAMESPACE);

            initChunks();
        }

        public Pkg(XmlDocument pkg)  
        {
            this.pkg = pkg;
            nsmgr = new XmlNamespaceManager(pkg.NameTable);
            nsmgr.AddNamespace("w", Namespaces.WORDML_NAMESPACE);

            initChunks();
        }


        public Word.Document createDocument()
        {
            Word.Document newDoc = new Microsoft.Office.Interop.Word.Document();
            Word.Range r = newDoc.Content;

            object missing = Type.Missing;

            try
            {
                r.InsertXML(Util.serialize(pkg), ref missing);
            }
            catch (System.Runtime.InteropServices.COMException com)
            {
                log.Debug(com.Message);
                log.Debug(Util.serialize(pkg));
            }
            return newDoc;
        }


        /* create an inferredSkeleton by transforming document.xml
        //    (this will also contain:            	
        //    (i)  new content controls for anything not in a cc            	
        //    (ii) flattening of any content controls which user has managed to nest via a paste */
        public XmlDocument getInferedSkeleton()
        {
            log.Debug(".. .. getInferedSkeleton");

            // Input
            //XmlNode docNode = pkg.DocumentElement;

            // Stylesheet
            XslTransform xslt = new XslTransform();
            xslt.Load(AppDomain.CurrentDomain.BaseDirectory + "WordML2Skeleton.xslt");

            // Transform
            XmlReader reader = xslt.Transform(pkg, null);

            // Ouptut
            XmlDocument resultDoc = new XmlDocument();
            resultDoc.Load(reader);
            return resultDoc;
        }

        Dictionary<string, StateChunk> stateChunks = null;
        public Dictionary<string, StateChunk> StateChunks
        {
            get { return stateChunks; }
        }

        public void initChunks()
        {
            XmlNamespaceManager nsmgr = new XmlNamespaceManager(pkg.NameTable);
            nsmgr.AddNamespace("w", Namespaces.WORDML_NAMESPACE);

            XmlNodeList nodeList = pkg.SelectNodes("//w:sdt", nsmgr);

            stateChunks = new Dictionary<string, StateChunk>();

            foreach (XmlNode nodex in nodeList)
            {
                StateChunk sc = new StateChunk(nodex);
                stateChunks.Add(sc.ID, sc);
            }


        }

        public Object Clone()
        {
            return new Pkg((XmlDocument)pkg.CloneNode(true), nsmgr, cloneStateChunks());
            // don't bother cloning nsmgr
        }

        public Pkg(XmlDocument pkg, XmlNamespaceManager nsmgr, 
            Dictionary<string, StateChunk> stateChunks)
        {
            this.pkg = pkg;
            this.nsmgr = nsmgr; 
            this.stateChunks = stateChunks;
        }

        Dictionary<string, StateChunk> cloneStateChunks() 
        {

            Dictionary<string, StateChunk> dictNew = 
                new Dictionary<string, StateChunk>();

            foreach (KeyValuePair<string, StateChunk> kvp in stateChunks)
            {
                dictNew.Add(kvp.Key, (StateChunk)kvp.Value.Clone());
            }

            return dictNew;
        }


        //// Accept/reject changes as appropriate
        //// in these, so that the compare results 
        //// will be intuitive
        //public void manageTrackedChanges(Pkg pkgC)
        //{

        //    foreach (KeyValuePair<string, StateChunk> kvp in stateChunks)
        //    {
        //        StateChunk sc = (StateChunk)kvp.Value;

        //        if (!sc.containsTrackedChanges())
        //            continue;

        //        // Accept the changes in B, so it represents
        //        // this user's stuff
        //        sc.acceptTrackedChanges();
        //        updatePkg(sc);

        //        /*
        //         * If there weren't any updates on the server for this
        //         * chunk, then the user shouldn't see any change in how
        //         * it is marked up.  
        //         * 
        //         * If we just leave it with its changes marked up, and 
        //         * input it into document compare, then the user will get
        //         * asked a question (to which they can't respond appropriately).
        //         * 
        //         * So we accept all changes in B, and reject all in C,
        //         * the idea being that the result of the compare will
        //         * be what we previously had.
        //         */
        //        if (!sc.TransformUpdatesExist)
        //        {
        //            // We've accepted the change in B above.

        //            // Now reject it in C.
        //            StateChunk scC = pkgC.StateChunks[sc.ID];
        //            scC.rejectTrackedChanges();
        //            pkgC.updatePkg(scC);
        //        }
        //    }

        //}

        public void updatePkg(StateChunk sc) {

            // sc is assumed to be present in StateChunks

            XmlNode refChild = pkg.SelectSingleNode("//w:sdt[w:sdtPr/w:id/@w:val='" + sc.ID + "']", nsmgr);

            if (refChild == null)
            {
                log.Debug("Couldn't find sdt to update " + sc.ID);
                // TODO - this will happen to a client A which deleted a chunk
                // or B which has the document open, if a client C reinstates
                // a chunk.  It will happen because A and B simply get an
                // update notice

                // One way to address this is to treat it as an insert,
                // asking for the skeleton doc in order to find out where
                // exactly to insert it.

                throw new SystemException();
            }
            else
            {
                XmlDocument tmpDoc = new XmlDocument();
                tmpDoc.LoadXml(sc.Xml);
                XmlNamespaceManager tmpNsmgr = new XmlNamespaceManager(tmpDoc.NameTable);
                tmpNsmgr.AddNamespace("w", Namespaces.WORDML_NAMESPACE);

                XmlNode nodeToCopy = tmpDoc["w:sdt"];

                XmlNode parent = refChild.ParentNode;
                XmlNode importedNode = pkg.ImportNode(nodeToCopy, true);
                parent.ReplaceChild(importedNode, refChild);

                log.Debug("Updated existing sdt " + sc.ID + " in pkg");
            }

        }

    }
}
