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
using Word = Microsoft.Office.Interop.Word;
using log4net;
//using System.Windows.Forms;


namespace plutext.client.word2007
{
    /* This class allows the server to return the details of an
     * update which was attempted, but which failed. */
    class TransformFailed : TransformAbstract
    {
        private static readonly ILog log = LogManager.GetLogger(typeof(TransformFailed));

        public TransformFailed(XmlNode n)
            : base(n)
        {

        }

        public override Int32 apply(Mediator mediator, Pkg pkg)
        {
            log.Debug("TransformFailed not fully implemented!");

            return SequenceNumber;
        }

        public override XmlDocument marshal()
        {
            return null;
        }

    }
}
