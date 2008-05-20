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

public class CustomProperties
    {

        // Names here should be kept consistent with
        // the server org.plutext.server.CustomProperties

        public static final String CHUNKING_STRATEGY = "plutext:Grouping";
        // Expected values: EachBlock, Heading1

        public static final String PROMPT_FOR_CHECKIN_MESSAGE = "plutext:CheckinMessageEnabled";

        // TODO - is this still used??
        public static final String DOCUMENT_ID = "DealerID";

        public static final String DOCUMENT_TRANSFORM_SEQUENCENUMBER = "plutext:transformSequenceNumber";

    }
