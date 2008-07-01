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

namespace plutext.client.word2007
{
    public static class CustomProperties
    {

        // Names here should be kept consistent with
        // the server org.plutext.server.CustomProperties

        public static string CHUNKING_STRATEGY = "plutext:Grouping";
        // Expected values: EachBlock, Heading1

        public static string PROMPT_FOR_CHECKIN_MESSAGE = "plutext:CheckinMessageEnabled";

        // TODO - is this still used??
        public static string DOCUMENT_ID = "DealerID";

        public static string DOCUMENT_TRANSFORM_SEQUENCENUMBER = "plutext:transformSequenceNumber";

    }
}
