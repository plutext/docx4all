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

import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.EditorKit;

import org.docx4all.swing.text.WordMLDocument;
import org.docx4all.swing.text.WordMLEditorKit;

/**
 *	@author Jojada Tirtowidjojo - 22/01/2008
 */
public class WordMLTextPane extends JTextPane {

	public WordMLTextPane() {
		super();
	}
	
    public void setDocument(Document doc) {
        if (doc instanceof WordMLDocument) {
            super.setDocument(doc);
        } else {
            throw new IllegalArgumentException("Model must be WordMLDocument");
        }
    }

    public void replaceSelection(String content) {
        if (!isEditable()) {
        	UIManager.getLookAndFeel().provideErrorFeedback(WordMLTextPane.this);
            return;
        }
        
        WordMLDocument doc = (WordMLDocument) getDocument();
        if (doc != null) {
            try {
            	int start = getSelectionStart();
            	int end = getSelectionEnd();
            	final int newCaretPos = 
            		(content != null) ? start + content.length() : start;
            	
            	AttributeSet attrs = getInputAttributes().copyAttributes();
                doc.replace(start, (end - start), content, attrs);
                
                SwingUtilities.invokeLater(new Runnable() {
                	public void run() {
                		setCaretPosition(newCaretPos);
                	}
                });
            } catch (BadLocationException e) {
            	UIManager.getLookAndFeel().provideErrorFeedback(WordMLTextPane.this);
            }
        }
    }

    public void saveCaretText() {
    	((WordMLEditorKit) getEditorKit()).saveCaretText();
    }
    
    protected EditorKit createDefaultEditorKit() {
        return new WordMLEditorKit();
    }


}// WordMLTextPane class



















