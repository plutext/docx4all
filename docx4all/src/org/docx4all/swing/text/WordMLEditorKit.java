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

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import javax.swing.Action;
import javax.swing.JEditorPane;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.TextAction;

import org.apache.log4j.Logger;
import org.docx4all.util.DocUtil;
import org.docx4all.xml.DocumentML;
import org.docx4all.xml.ElementMLFactory;
import org.docx4all.xml.ElementMLIterator;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class WordMLEditorKit extends StyledEditorKit {
	private static Logger log = Logger.getLogger(WordMLEditorKit.class);

	private static final Cursor MoveCursor = Cursor
			.getPredefinedCursor(Cursor.HAND_CURSOR);
	private static final Cursor DefaultCursor = Cursor
			.getPredefinedCursor(Cursor.DEFAULT_CURSOR);

	private static final javax.swing.text.ViewFactory defaultFactory = new ViewFactory();

	// TODO: Later implementation
	private static final Action[] defaultActions = {};

	private JEditorPane theEditor;
	private Cursor defaultCursor = DefaultCursor;

	/**
	 * Constructs an WordMLEditorKit, creates a StyleContext, and loads the
	 * style sheet.
	 */
	public WordMLEditorKit() {
		super();
	}

	/**
	 * Get the MIME type of the data that this kit represents support for. This
	 * kit supports the type <code>text/xml</code>.
	 * 
	 * @return the type
	 */
	@Override
	public String getContentType() {
		return "application/xml";
	}

	/**
	 * Fetch a factory that is suitable for producing views of any models that
	 * are produced by this kit.
	 * 
	 * @return the factory
	 */
	@Override
	public javax.swing.text.ViewFactory getViewFactory() {
		return defaultFactory;
	}

	/**
	 * Create an uninitialized text storage model that is appropriate for this
	 * type of editor.
	 * 
	 * @return the model
	 */
	@Override
	public Document createDefaultDocument() {
		Document doc = new WordMLDocument();
		if (log.isDebugEnabled()) {
			log.debug("createDefaultDocument():");
			DocUtil.displayStructure(doc);
		}
		return doc;
	}

	@Override
	public void read(Reader in, Document doc, int pos) 
		throws IOException,	BadLocationException {
		throw new NotImplementedException();
	}

    /**
     * Creates a WordMLDocument from the given .docx file
     * 
     * @param f  The file to read from
     * @exception IOException on any I/O error
     */
	public WordMLDocument read(File f) throws IOException {
		DocumentML docML = ElementMLFactory.createDocumentML(f);
		ElementMLIterator parser = new ElementMLIterator(docML);
		ElementMLIteratorCallback result = new ElementMLIteratorCallback();
		parser.cruise(result);
		
		WordMLDocument doc = (WordMLDocument) createDefaultDocument();
		doc.createElementStructure(result.getElementSpecs());
		
		if (log.isDebugEnabled()) {
			log.debug("read(): File = " + f.getAbsolutePath());
			DocUtil.displayStructure(result.getElementSpecs());
			DocUtil.displayStructure(doc);
		}
			
		return doc;
	}

	/**
	 * Write content from a document to the given stream in a format appropriate
	 * for this kind of content handler.
	 * 
	 * @param out
	 *            the stream to write to
	 * @param doc
	 *            the source for the write
	 * @param pos
	 *            the location in the document to fetch the content
	 * @param len
	 *            the amount to write out
	 * @exception IOException
	 *                on any I/O error
	 * @exception BadLocationException
	 *                if pos represents an invalid location within the document
	 */
	@Override
	public void write(Writer out, Document doc, int pos, int len)
			throws IOException, BadLocationException {

		if (doc instanceof WordMLDocument) {
			// TODO: Later implementation
		} else {
			super.write(out, doc, pos, len);
		}
	}

	/**
	 * Called when the kit is being installed into the a JEditorPane.
	 * 
	 * @param c
	 *            the JEditorPane
	 */
	@Override
	public void install(JEditorPane c) {
		// TODO: FIX ME
		// c.addMouseListener(linkHandler);
		// c.addMouseMotionListener(linkHandler);
		// c.addCaretListener(nextLinkAction);
		super.install(c);
		theEditor = c;
	}

	/**
	 * Called when the kit is being removed from the JEditorPane. This is used
	 * to unregister any listeners that were attached.
	 * 
	 * @param c
	 *            the JEditorPane
	 */
	public void deinstall(JEditorPane c) {
		// TODO: FIX ME
		// c.removeMouseListener(linkHandler);
		// c.removeMouseMotionListener(linkHandler);
		// c.removeCaretListener(nextLinkAction);
		// super.deinstall(c);
		theEditor = null;
	}

	/**
	 * Fetches the command list for the editor. This is the list of commands
	 * supported by the superclass augmented by the collection of commands
	 * defined locally for style operations.
	 * 
	 * @return the command list
	 */
	@Override
	public Action[] getActions() {
		return TextAction.augmentList(super.getActions(), WordMLEditorKit.defaultActions);
	}

	public void setDefaultCursor(Cursor cursor) {
		defaultCursor = cursor;
	}

	public Cursor getDefaultCursor() {
		return defaultCursor;
	}

	public abstract static class StyledTextAction extends javax.swing.text.StyledEditorKit.StyledTextAction {
        public StyledTextAction(String nm) {
    	    super(nm);
    	}
        
		protected final static void setAttributesToParagraph(JEditorPane editor,
				AttributeSet attr, boolean replace) {
			int p0 = editor.getSelectionStart();
			int p1 = editor.getSelectionEnd();
			WordMLDocument doc = (WordMLDocument) editor.getDocument();
			
			while (p0 <= p1) {
				Element elem = doc.getParagraphMLElement(p0, false);
				int start = elem.getStartOffset();
				int end = elem.getEndOffset();
				
				doc.setParagraphAttributes(start, (end - start), attr, replace);
				
				p0 = end;
				if (p0 == p1) {
					//finish
					p0 = p1 + 1;
				}
			}
		}
	}
	
    public static class AlignmentAction extends StyledTextAction {

		/**
		 * Creates a new AlignmentAction.
		 *
		 * @param nm the action name
		 * @param a the alignment >= 0
		 */
    	private int _alignment;
    	
		public AlignmentAction(String name, int alignment) {
		    super(name);
		    this._alignment = alignment;
		}

        public void actionPerformed(ActionEvent e) {
			JEditorPane editor = getEditor(e);
			if (editor != null) {
				int a = this._alignment;
				if ((e != null) && (e.getSource() == editor)) {
					String s = e.getActionCommand();
					try {
						a = Integer.parseInt(s, 10);
					} catch (NumberFormatException nfe) {
					}
				}
				MutableAttributeSet attr = new SimpleAttributeSet();
				StyleConstants.setAlignment(attr, a);
				setAttributesToParagraph(editor, attr, false);
			}
		}
    	    
	}// AlignmentAction inner class

}// WordMLEditorKit class

