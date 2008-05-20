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

import javax.swing.text.Element;
import javax.swing.text.Position;
import javax.swing.text.View;

import org.apache.log4j.Logger;

public class ParagraphView extends javax.swing.text.ParagraphView {
	private static Logger log = Logger.getLogger(ParagraphView.class);

	public ParagraphView(Element elem) {
		super(elem);
		strategy = new FlowStrategy();
	}

	//===== INNER CLASS =====\\
	class FlowStrategy extends javax.swing.text.FlowView.FlowStrategy {
		protected View createView(
			javax.swing.text.FlowView fv,
			int startOffset,
			int spanLeft,
			int rowIndex) {

			// Get the child view that contains the given starting position
			View lv = getLogicalView(fv);
			int childIndex =
				lv.getViewIndex(startOffset, Position.Bias.Forward);
			View v = lv.getView(childIndex);

			if (v instanceof RunView) {
				RunView runV = (RunView) v;
				v = runV.getTextView(startOffset);
			}

			if (startOffset == v.getStartOffset()) {
				// return the entire view
				return v;
			}

			// return a fragment.
			v = v.createFragment(startOffset, v.getEndOffset());
			return v;
		}
	}


}// ParagraphView class












