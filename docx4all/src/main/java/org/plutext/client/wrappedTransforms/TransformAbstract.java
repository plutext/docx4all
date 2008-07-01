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
using System.Windows.Forms;
using log4net;


namespace plutext.client.word2007
{
    public abstract class TransformAbstract
    {

        private static readonly ILog log = LogManager.GetLogger(typeof(TransformAbstract));

        protected XmlNode sdt = null;
        public XmlNode SDT
        {
            get { return sdt; }
        }

        // For debug purposes only.
        protected XmlNode tNode = null;
        public XmlNode TNode
        {
            get { return tNode; }
        }

        public TransformAbstract()
        { }

        public TransformAbstract(XmlNode n)
        {
            sequenceNumber = Int32.Parse(n.Attributes.GetNamedItem("snum", Namespaces.PLUTEXT_TRANSFORMS_NAMESPACE).Value);

            // For debug purposes only
            tNode = n;

            sdt = n.FirstChild;


            if (n.Attributes.GetNamedItem("idref", Namespaces.PLUTEXT_TRANSFORMS_NAMESPACE) != null)
            {
                // Case: Delete 
                id = n.Attributes.GetNamedItem("idref", Namespaces.PLUTEXT_TRANSFORMS_NAMESPACE).Value;
            }
            else if (this.GetType().Name.Equals("TransformStyle")) { 
            
                // No ID
            
            } else
            {
                // Case: Update, Insert

                XmlNode sdtPr = sdt.FirstChild;

                XmlNamespaceManager nsmgr = new XmlNamespaceManager(n.OwnerDocument.NameTable);
                nsmgr.AddNamespace("w", Namespaces.WORDML_NAMESPACE);

                //id = sdtPr.FirstChild.Attributes.GetNamedItem("val", "http://schemas.openxmlformats.org/wordprocessingml/2006/main").Value;
                XmlNode idAt = sdtPr.SelectSingleNode("w:id/@w:val", nsmgr);
                if (idAt == null)
                {
                    log.Debug("ERROR: - no id?!");
                }
                id = idAt.Value;

                //tag = sdtPr.LastChild.Attributes.GetNamedItem("val", "http://schemas.openxmlformats.org/wordprocessingml/2006/main").Value;
                XmlNode tagAt = sdtPr.SelectSingleNode("w:tag/@w:val", nsmgr);
                if (tagAt == null)
                {
                    log.Debug("ERROR: - no tag?!");
                }
                tag = tagAt.Value;

            }

            //MessageBox.Show("Parsed SDT ID " + id);
            

        }


        // The ID of the wrapped SDT. This is *not* the ID of the transformation.
        // TODO - rename, so that this is more obvious.
        protected string id;
        public String ID
        {
            get { return id; }
            set { id = value; }
        }
        // The tag of the wrapped SDT
        protected string tag;
        public String Tag
        {
            get { return tag; }
        }


        // Has this transform been applied to the document yet?
        Boolean applied = false;
        public Boolean Applied
        {
            get { return applied; }
            set { applied = value; }
        }

        // Is this transform something which came from this
        // plutext client?  (If it is, we can always apply it without worrying
        // about conflicts)
        Boolean local = false;
        public Boolean Local
        {
            get { return local; }
            set { local = value; }
        }


        // The ID of the transformation.
        protected Int32 sequenceNumber = 0;
        public Int32 SequenceNumber
        {
            get { return sequenceNumber; }
            set { sequenceNumber = value; }
        }


        /* Code to apply the transform */
        public abstract Int32 apply(Mediator mediator, Pkg pkg);

        public abstract XmlDocument marshal();

        protected XmlDocument createDocument()
        {
            XmlDocument tf = new XmlDocument();
            XmlNamespaceManager nsmgr = new XmlNamespaceManager(tf.NameTable);
            nsmgr.AddNamespace("w", Namespaces.WORDML_NAMESPACE);
            nsmgr.AddNamespace(Namespaces.PLUTEXT_TRANSFORMS_NS_PREFIX, 
                Namespaces.PLUTEXT_TRANSFORMS_NAMESPACE);

            return tf;
        }
    }
}
