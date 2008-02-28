/*
 *  Copyright 2007, Plutext Pty Ltd.
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

package org.docx4all.script;

import org.docx4all.ui.main.Constants;


/**
 *	@author Jojada Tirtowidjojo - 15/11/2007
 */
public class FxScript {

	public final static FxScript.Path TOOLBAR_FX = 
		new Path(Constants.TOOLBAR_FX);
	public final static FxScript.Path CREATE_EDITOR_PANEL_FX = 
		new Path(Constants.CREATE_EDITOR_PANEL_FX);
	public final static FxScript.Path CREATE_EDITOR_TABBED_PANEL_FX = 
		new Path(Constants.CREATE_EDITOR_TABBED_PANEL_FX);

	private FxScript() {
		;//uninstantiable
	}
	
	public static class Path {
		private final String _path;
		
		private Path(String s) {
			_path = s;
		}
		
		public String toString() {
			return new String(_path);
		}
	}// Path static inner class
	
}// FxScript class



















