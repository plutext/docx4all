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

package org.plutext.client.wrappedTransforms;

import org.apache.log4j.Logger;
import org.plutext.transforms.Transforms.T;

public class TransformHelper {
	private static Logger log = Logger.getLogger(TransformHelper.class);

	public static TransformAbstract construct(T t) {

		String operation = t.getOp();

		if (operation.equals("update")) {
			return new TransformUpdate(t);
		} else if (operation.equals("delete")) {
			return new TransformDelete(t);
		} else if (operation.equals("insert")) {
			return new TransformInsert(t);
		} else if (operation.equals("move")) {
			return new TransformMove(t);
		} else if (operation.equals("style")) {
			return new TransformStyle(t);
		} else if (operation.equals("failed")) {
			return new TransformFailed(t);
		} else {
			log.error("Unrecognised transform!!!");
			// TODO - throw exception
			return null;
		}

	}
}
