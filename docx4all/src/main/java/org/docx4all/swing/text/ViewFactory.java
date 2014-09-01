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

import javax.swing.text.AttributeSet;
import javax.swing.text.BoxView;
import javax.swing.text.Element;
import javax.swing.text.View;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.docx4all.xml.ElementML;
import org.docx4all.xml.InlineDrawingML;
import org.docx4all.xml.ParagraphML;
import org.docx4all.xml.RunContentML;
import org.docx4all.xml.RunML;
import org.docx4all.xml.SdtBlockML;
import org.docx4all.xml.TableCellML;
import org.docx4all.xml.TableML;
import org.docx4all.xml.TableRowML;

public class ViewFactory implements javax.swing.text.ViewFactory {
	private static Logger log = LoggerFactory.getLogger(ViewFactory.class);
	
	private TableView tableView;
	
    /**
     * Creates a view from the given structural element of a
     * document.
     *
     * @param elem  the piece of the document to build a view of
     * @return the view
     * @see View
     */
    public View create(Element elem) {
		View theView = null;

		AttributeSet attrs = elem.getAttributes();
		ElementML elementML = WordMLStyleConstants.getElementML(attrs);

		//TODO: Don't quite like this temporary solution
		if (elementML instanceof InlineDrawingML) {
			theView = new InlineImageView(elem);
		} else if (elementML instanceof RunContentML) {
			theView = new LabelView(elem);
		} else if (elementML instanceof RunML) {
			theView = new RunView(elem);
		} else if (elementML == ElementML.IMPLIED_PARAGRAPH) {
			theView = new ImpliedParagraphView(elem);
		} else if (elementML instanceof ParagraphML) {
			theView = new ParagraphView(elem);
		} else if (elementML instanceof SdtBlockML) {
			theView = new SdtBlockView(elem);
		} else if (elementML instanceof TableML) {
			tableView = new TableView(elem);
			theView = tableView;
		} else if (elementML instanceof TableRowML) {
			theView = tableView.new TableRowView(elem);
		} else if (elementML instanceof TableCellML) {
			theView = new TableCellView(elem);
		} else {
			theView = new BoxView(elem, View.Y_AXIS);
		}
		
		return theView;
	}


}// ViewFactory class























