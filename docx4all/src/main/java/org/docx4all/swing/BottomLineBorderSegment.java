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

package org.docx4all.swing;

import java.awt.Color;
import java.awt.Graphics;

import javax.swing.plaf.basic.BasicGraphicsUtils;

import org.docx4all.xml.type.CTBorder;

/**
 *	@author Jojada Tirtowidjojo - 30/09/2008
 */
public class BottomLineBorderSegment extends LineBorderSegment {
	public BottomLineBorderSegment(CTBorder border) {
		this(border.getColor(), border.getStyle(), border.getSizeInPixels());
	}
	
	public BottomLineBorderSegment(Color c, Style style, int thickness) {
		super(c, Side.BOTTOM, style, thickness);
	}
	
	public void drawLine(Graphics g, int x1, int y1, Direction dir, int length) {
		if (dir == Direction.UP || dir == Direction.DOWN) {
			throw new IllegalArgumentException("Direction=" + dir.getDeclaringClass().getName());
		}
		
		if (dir == Direction.LEFT) {
			x1 -= length;
		}
		
        for(int i = 0; i < thickness; i++)  {
        	if (style == Style.SOLID) {
        		g.drawRect(x1, y1 - i, 0, length);
        	} else {
        		//Style.DASHED
        		BasicGraphicsUtils.drawDashedRect(g, x1, y1 - i, 0, length);
        	}
        }
	}

}// BottomLineBorderSegment class



















