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

package org.docx4all.swing.text;

import javax.swing.text.Position;

/**
 *	@author Jojada Tirtowidjojo - 13/05/2008
 */
public class SdtBlockInfo {
	org.docx4j.wml.SdtBlock sdtBlock;
	Position position;
	
	public SdtBlockInfo() {
		;
	}
	
	public org.docx4j.wml.SdtBlock getSdtBlock() {
		return sdtBlock;
	}
	
	public void setSdtBlock(org.docx4j.wml.SdtBlock sdt) {
		sdtBlock = sdt;
	}
	
	public Position getPosition() {
		return position;
	}
	
	public void setPosition(Position pos) {
		position = pos;
	}
	
}// SdtBlockInfo class



















