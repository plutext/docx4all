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

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.io.Writer;
import java.util.List;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.event.CaretEvent;
import javax.swing.event.EventListenerList;
import javax.swing.plaf.basic.BasicTextUI;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Document;
import javax.swing.text.Highlighter;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.Position;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.TextAction;
import javax.swing.text.View;
import javax.swing.text.DefaultStyledDocument.ElementSpec;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import net.sf.vfsjfilechooser.utils.VFSUtils;

import org.apache.commons.vfs.FileObject;
import org.apache.log4j.Logger;
import org.docx4all.swing.WordMLTextPane;
import org.docx4all.swing.event.InputAttributeEvent;
import org.docx4all.swing.event.InputAttributeListener;
import org.docx4all.ui.main.Constants;
import org.docx4all.util.DocUtil;
import org.docx4all.util.SwingUtil;
import org.docx4all.util.XmlUtil;
import org.docx4all.xml.DocumentML;
import org.docx4all.xml.ElementML;
import org.docx4all.xml.ElementMLFactory;
import org.docx4all.xml.ParagraphML;
import org.docx4all.xml.RunDelML;
import org.docx4all.xml.RunInsML;
import org.docx4all.xml.SdtBlockML;
import org.plutext.client.Mediator;
import org.plutext.client.Util;

public class WordMLEditorKit extends DefaultEditorKit {
	private static Logger log = Logger.getLogger(WordMLEditorKit.class);

    /**
     * Name of the action to place a soft line break into
     * the document.  If there is a selection, it is removed before
     * the break is added.
     * @see #getActions
     */
    public static final String insertSoftBreakAction = "insert-soft-break";

    public static final String enterKeyTypedAction = "enter-key-typed-action";
    
    public static final String fontBoldAction = "font-bold";
    
    public static final String fontItalicAction = "font-italic";
    
    public static final String fontUnderlineAction = "font-underline";

    public static final String fetchRemoteEditsAction = "fetch-remote-edits";
    
    public static final String commitLocalEditsAction = "commit-local-edits";

    public static final String acceptRevisionAction = "accept-revision";
    
    public static final String acceptNonConflictingRevisionsAction = "accept-non-conflicting-revisions";
    
    public static final String rejectNonConflictingRevisionsAction = "reject-non-conflicting-revisions";
    
    public static final String rejectRevisionAction = "reject-revision";
    
    public static final String applyRemoteRevisionsInParaAction = "apply-remote-revisions-in-para";
    
    public static final String discardRemoteRevisionsInParaAction = "discard-remote-revisions-in-para";
    
    public static final String selectNextRevisionAction = "select-next-revision";
    
    public static final String selectPrevRevision = "select-prev-revision";
    
	private static final Cursor MoveCursor = Cursor
			.getPredefinedCursor(Cursor.HAND_CURSOR);
	private static final Cursor DefaultCursor = Cursor
			.getPredefinedCursor(Cursor.DEFAULT_CURSOR);

	private static final javax.swing.text.ViewFactory defaultFactory = new ViewFactory();

	// TODO: Later implementation
	private static final Action[] defaultActions = {};

	private Cursor defaultCursor = DefaultCursor;
	private CaretListener caretListener;
	private MouseListener mouseListener;
	private ContentControlTracker contentControlTracker;
	private Mediator plutextClient;
	
    /**
     * This is the set of attributes used to store the
     * input attributes.  
     */  
    private MutableAttributeSet inputAttributes;

    private DocumentElement currentRunE;
    
    private boolean inContentControlEdit;
    
    /**
     * The event listener list for this WordMLEditorKit.
     */
    private EventListenerList listenerList = new EventListenerList();

	/**
	 * Constructs an WordMLEditorKit, creates a StyleContext, and loads the
	 * style sheet.
	 */
	public WordMLEditorKit() {
		caretListener = new CaretListener();
		mouseListener = new MouseListener();
		contentControlTracker = new ContentControlTracker();
		inContentControlEdit = false;
		
        inputAttributes = new SimpleAttributeSet() {
            public AttributeSet getResolveParent() {
                return (currentRunE != null) ?
                		currentRunE.getAttributes() : null;
            }

            public Object clone() {
                return new SimpleAttributeSet(this);
            }
        };
	}

	public void saveCaretText() {
		log.debug("saveCaretText(): getCaretElement()=" + getCaretElement());
		if (getCaretElement() != null) {
			DocUtil.saveTextContentToElementML(getCaretElement());
		}
	}
	
	public final synchronized void beginContentControlEdit(WordMLTextPane editor) {
		inContentControlEdit = true;
		
		WordMLDocument doc = (WordMLDocument) editor.getDocument();
		doc.lockWrite();
		doc.setSnapshotFireBan(true);
	}
	
	public final synchronized void endContentControlEdit(WordMLTextPane editor) {
		WordMLDocument doc = (WordMLDocument) editor.getDocument();
		doc.setSnapshotFireBan(false);
		doc.unlockWrite();
		
		inContentControlEdit = false;
	}
	
	public final synchronized boolean isInContentControlEdit() {
		return inContentControlEdit;
	}
	
    public void addInputAttributeListener(InputAttributeListener listener) {
    	listenerList.add(InputAttributeListener.class, listener);
    }

    public void removeInputAttributeListener(InputAttributeListener listener) {
    	listenerList.remove(InputAttributeListener.class, listener);
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
		throw new UnsupportedOperationException();
	}

    /**
     * Creates a WordMLDocument from the given .docx file
     * 
     * @param f  The file to read from
     * @exception IOException on any I/O error
     */
	public WordMLDocument read(FileObject f) throws IOException {
		if (log.isDebugEnabled()) {
			log.debug("read(): File = " + VFSUtils.getFriendlyName(f.getName().getURI()));
		}
		
		WordMLDocument doc = read(ElementMLFactory.createDocumentML(f));
		return doc;
	}

	public WordMLDocument read(DocumentML documentML) {
		if (log.isDebugEnabled()) {
			log.debug("read(): documentML = " + documentML);
		}
		
		List<ElementSpec> specs = DocUtil.getElementSpecs(documentML);
		
		WordMLDocument doc = (WordMLDocument) createDefaultDocument();
		doc.createElementStructure(specs);
		
		if (log.isDebugEnabled()) {
			DocUtil.displayStructure(specs);
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
		super.install(c);

		c.addCaretListener(caretListener);
		c.addCaretListener(contentControlTracker);
		c.addMouseListener(mouseListener);
		c.addMouseMotionListener(mouseListener);
		c.addPropertyChangeListener(caretListener);
		caretListener.updateCaretElement(0, 0, c);

		initKeyBindings(c);
	}

	/**
	 * Called when the kit is being removed from the JEditorPane. This is used
	 * to unregister any listeners that were attached.
	 * 
	 * @param c
	 *            the JEditorPane
	 */
	public void deinstall(JEditorPane c) {
		super.deinstall(c);
		c.removeCaretListener(caretListener);
		c.removeCaretListener(contentControlTracker);
		c.removeMouseListener(mouseListener);
		c.removeMouseMotionListener(mouseListener);
		c.removePropertyChangeListener(caretListener);
		
		this.plutextClient = null;
	}

	public void initPlutextClient(WordMLTextPane editor) {
		WordMLDocument doc = (WordMLDocument) editor.getDocument();
		try {
			doc.readLock();
			this.plutextClient = new Mediator(editor);
		} finally {
			doc.readUnlock();
		}
	}
	
	public Mediator getPlutextClient() {
		return this.plutextClient;
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
	
    /**
     * Gets the input attributes for the pane.  When
     * the caret moves and there is no selection, the
     * input attributes are automatically mutated to 
     * reflect the character attributes of the current
     * caret location.  The styled editing actions 
     * use the input attributes to carry out their 
     * actions.
     *
     * @return the attribute set
     */
    public MutableAttributeSet getInputAttributesML() {
    	return inputAttributes;
    }

    protected void createInputAttributes(
    	WordMLDocument.TextElement element,
		MutableAttributeSet set, 
		JEditorPane editor) {
    	
		set.removeAttributes(set);
		if (element != null && element.getEndOffset() < element.getDocument().getLength()) {
			set.addAttributes(element.getAttributes());
			// set.removeAttribute(StyleConstants.ComponentAttribute);
			// set.removeAttribute(StyleConstants.IconAttribute);
			// set.removeAttribute(AbstractDocument.ElementNameAttribute);
			// set.removeAttribute(StyleConstants.ComposedTextAttribute);
		}
		fireInputAttributeChanged(new InputAttributeEvent(editor));
	}

    protected void fireInputAttributeChanged(InputAttributeEvent e) {
		// Guaranteed to return a non-null array
		Object[] listeners = listenerList.getListenerList();
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == InputAttributeListener.class) {
				// Lazily create the event:
				// if (e == null)
				// e = new ListSelectionEvent(this, firstIndex, lastIndex);
				((InputAttributeListener) listeners[i + 1]).inputAttributeChanged(e);
			}
		}
	}

	private WordMLDocument.TextElement getCaretElement() {
		return (WordMLDocument.TextElement) caretListener.caretElement;
	}
	
	private void refreshCaretElement(JEditorPane editor) {
		caretListener.caretElement = null;
		int start = editor.getSelectionStart();
		int end = editor.getSelectionEnd();
		
		WordMLDocument doc = (WordMLDocument) editor.getDocument();
		try {
			doc.readLock();
			caretListener.updateCaretElement(start, end, editor);
		} finally {
			doc.readUnlock();
		}
	}
	
	private void initKeyBindings(JEditorPane editor) {
		ActionMap myActionMap = new ActionMap();
		InputMap myInputMap = new InputMap();
		
		KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_MASK);
		myActionMap.put(insertSoftBreakAction, new InsertSoftBreakAction(insertSoftBreakAction));
		myInputMap.put(ks, insertSoftBreakAction);

		ks = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
		myActionMap.put(enterKeyTypedAction, new EnterKeyTypedAction(enterKeyTypedAction));
		myInputMap.put(ks, enterKeyTypedAction);
		
		ks = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0);
		myActionMap.put(deleteNextCharAction, new DeleteNextCharAction(deleteNextCharAction));
		myInputMap.put(ks, deleteNextCharAction);

		ks = KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0);
		myActionMap.put(deletePrevCharAction, new DeletePrevCharAction(deletePrevCharAction));
		myInputMap.put(ks, deletePrevCharAction);

		myActionMap.setParent(editor.getActionMap());
		myInputMap.setParent(editor.getInputMap());
		editor.setActionMap(myActionMap);
		editor.setInputMap(JComponent.WHEN_FOCUSED, myInputMap);
	}
	
	//**************************
	//*****  INNER CLASSES *****
	//**************************	
	public final static class MouseListener extends MouseAdapter {
		private final static DefaultHighlighter.DefaultHighlightPainter
			SDT_BACKGROUND_PAINTER = 
				new DefaultHighlighter.DefaultHighlightPainter(
					new Color(189, 222, 255));
		private Object lastHighlight = null;
		private DocumentElement lastHighlightedE = null;
		
	    public void mouseClicked(MouseEvent e) {
	    	WordMLTextPane editor = (WordMLTextPane) e.getSource();
	    	clearLastHighlight(editor);
	    }
	    
	    public void mouseMoved(MouseEvent e){
	    	WordMLTextPane editor = (WordMLTextPane) e.getSource();
	    	BasicTextUI ui = (BasicTextUI) editor.getUI();
	    	
	    	Point pt = new Point(e.getX(), e.getY());
	    	Position.Bias[] biasRet = new Position.Bias[1];
	    	int pos = ui.viewToModel(editor, pt, biasRet);

	    	highlight(editor, pos);
	    	trackAuthorTooltip(editor, pos);
	    }
	    
	    private void trackAuthorTooltip(WordMLTextPane editor, int pos) {
			WordMLDocument doc = (WordMLDocument) editor.getDocument();
			DocumentElement elem = 
				(DocumentElement) doc.getRunMLElement(pos);
			ElementML parent = elem.getElementML().getParent();
			StringBuilder tipText = null;
			if (parent instanceof RunDelML) {
				String author = ((RunDelML) parent).getAuthor();
				String text = getText(doc, elem);
				if (author != null && author.length() > 0 
					&& text != null && text.length() > 0) {
					tipText = new StringBuilder("<html><p><b>");
					tipText.append(author.substring(0, 1).toUpperCase());
					tipText.append(author.substring(1));
					tipText.append(" deleted:</b></p><p>");
					tipText.append(text);
					tipText.append("</p></html>");
				}
			} else if (parent instanceof RunInsML) {
				String author = ((RunInsML) parent).getAuthor();
				String text = getText(doc, elem);
				if (author != null && author.length() > 0 
					&& text != null && text.length() > 0) {
					tipText = new StringBuilder("<html><p><b>");
					tipText.append(author.substring(0, 1).toUpperCase());
					tipText.append(author.substring(1));
					tipText.append(" inserted:</b></p><p>");
					tipText.append(text);
					tipText.append("</p></html>");
				}
			}
			
			if (tipText == null || tipText.length() == 0) {
				editor.setToolTipText(null);
			} else {
				editor.setToolTipText(tipText.toString());
			}
	    }
	    
	    private String getText(WordMLDocument doc, DocumentElement elem) {
	    	String text = null;
	    	
	    	int start = elem.getStartOffset();
	    	int length = elem.getEndOffset() - start;
	    	try {
	    		text = doc.getText(start, length);
	    	} catch (BadLocationException exc) {
	    		;//should not happen
	    	}
	    	
	    	return text;
	    }
	    
	    private void highlight(WordMLTextPane editor, int pos) {
			WordMLDocument doc = (WordMLDocument) editor.getDocument();
			if (lastHighlightedE != null
					&& lastHighlightedE.getStartOffset() == lastHighlightedE
							.getEndOffset()) {
				// invalid Element. This may happen when the Element
				// has been removed from the document structure.
				lastHighlightedE = null;
			}

			if (lastHighlightedE != null
				&& (lastHighlightedE.getStartOffset() <= pos 
					&& pos <= lastHighlightedE.getEndOffset())) {
				;// do nothing
			} else {
				clearLastHighlight(editor);
				DocumentElement elem = 
					(DocumentElement) doc.getSdtBlockMLElement(pos);
				if (elem != null
					&& !WordMLStyleConstants.getBorderVisible(
							elem.getAttributes())) {
					try {
						Highlighter hl = editor.getHighlighter();
						lastHighlight = 
							hl.addHighlight(
								elem.getStartOffset(), 
								elem.getEndOffset(),
								SDT_BACKGROUND_PAINTER);
						lastHighlightedE = elem;
					} catch (BadLocationException exc) {
						;// should not happen
					}
				}
			}
		}
	    
	    private void clearLastHighlight(WordMLTextPane editor) {
			Highlighter hl = editor.getHighlighter();
			if (lastHighlight != null) {
				hl.removeHighlight(lastHighlight);
			}
			lastHighlightedE = null;
	    }
	    
	}// MouseListener inner class
	
	private class ContentControlTracker implements javax.swing.event.CaretListener, Serializable {
		private Position lastSdtBlockPosition;
		
	    public void caretUpdate(CaretEvent evt) {			
	    	WordMLTextPane editor = (WordMLTextPane) evt.getSource();
	    	if (editor.isInContentControlEdit()) {
	    		return;
	    	}
	    	
    		int start = Math.min(evt.getDot(), evt.getMark());
    		int end = Math.max(evt.getDot(), evt.getMark());
    		
			if (start == end) {
				selectSdtBlock(editor, start);
			} else {
	    		View startV = SwingUtil.getSdtBlockView(editor, start);
	    		View endV = SwingUtil.getSdtBlockView(editor, end - 1);
	    		if (startV == endV && startV != null) {
	    			selectSdtBlock(editor, start);
	    		} else {
	    			resetLastSdtBlockPosition(editor);
	    		}
			}
	    }
	    
	    private void selectSdtBlock(WordMLTextPane editor, int pos) {
			SdtBlockView currentSdt = SwingUtil.getSdtBlockView(editor, pos);
			
			SdtBlockView lastSdt = null;
			if (lastSdtBlockPosition != null) {
				int offset = lastSdtBlockPosition.getOffset();
				lastSdt = SwingUtil.getSdtBlockView(editor, offset);
			}

			if (currentSdt != lastSdt) {
				if (lastSdt != null) {
					lastSdt.setBorderVisible(false);
					editor.getUI().damageRange(editor, lastSdt.getStartOffset(), lastSdt
							.getEndOffset(), Position.Bias.Forward,
							Position.Bias.Forward);
					lastSdtBlockPosition = null;
				}
			}
			
			if (currentSdt != null) {
				currentSdt.setBorderVisible(true);
				editor.getUI().damageRange(editor, currentSdt.getStartOffset(),
						currentSdt.getEndOffset(), Position.Bias.Forward,
						Position.Bias.Forward);
				
				try {
					lastSdtBlockPosition = editor.getDocument().createPosition(pos);
				} catch (BadLocationException exc) {
					lastSdtBlockPosition = null;// should not happen
				}
			}			
		}
	    
	    private void resetLastSdtBlockPosition(WordMLTextPane editor) {
	    	if (lastSdtBlockPosition != null) {
	    		int offset = lastSdtBlockPosition.getOffset();
				SdtBlockView sdt = SwingUtil.getSdtBlockView(editor, offset);
				if (sdt != null) {
					sdt.setBorderVisible(false);
					editor.getUI().damageRange(
						editor, 
						sdt.getStartOffset(), 
						sdt.getEndOffset(), 
						Position.Bias.Forward,
						Position.Bias.Forward);
				}
				lastSdtBlockPosition = null;
			}
	    }	    
	}// ContentControlTracker inner class
	
	private class CaretListener implements javax.swing.event.CaretListener, PropertyChangeListener, Serializable {
		private WordMLDocument.TextElement caretElement;
		
	    public void caretUpdate(CaretEvent evt) {			
	    	WordMLTextPane editor = (WordMLTextPane) evt.getSource();
    		int start = Math.min(evt.getDot(), evt.getMark());
    		int end = Math.max(evt.getDot(), evt.getMark());
    		
	    	WordMLDocument doc = (WordMLDocument) editor.getDocument();
	    	try {
	    		doc.readLock();
	    		
				if (start != end) {
		    		//Validate selected area if any
					new TextSelector(doc, start, end - start);
				}
				updateCaretElement(start, end, editor);
			
//				if (start == end) {
//					selectSdtBlock(editor, start);
//				} else {
//					resetLastSdtBlockPosition(editor);
//				}
				
			} catch (BadSelectionException exc) {
				UIManager.getLookAndFeel().provideErrorFeedback(editor);
				editor.moveCaretPosition(start);
	    	} finally {
	    		doc.readUnlock();
	    	}
	    }
	    
        public void propertyChange(PropertyChangeEvent evt) {
    	    Object newValue = evt.getNewValue();
    	    Object source = evt.getSource();

    	    if ((source instanceof WordMLTextPane)
				 && (newValue instanceof WordMLDocument)) {
				// New document will have changed selection to 0,0.
    	    	WordMLDocument doc = (WordMLDocument) newValue;
    	    	try {
    	    		doc.readLock();
    	    		
    	    		updateCaretElement(0, 0, (WordMLTextPane) source);
    	    	} finally {
    	    		doc.readUnlock();
    	    	}
			}
    	}

	    void updateCaretElement(int start, int end, JEditorPane editor) {
	    	WordMLDocument doc = (WordMLDocument) editor.getDocument();
	    	Position.Bias bias = (start != end) ? Position.Bias.Forward : null;
	    	WordMLDocument.TextElement elem = DocUtil.getInputAttributeElement(doc, start, bias);

			if (caretElement != elem) {
				DocUtil.saveTextContentToElementML(caretElement);
				caretElement = elem;
				
				if (caretElement != null) {
					WordMLEditorKit.this.currentRunE = 
						(DocumentElement) caretElement.getParentElement();
				} else {
					WordMLEditorKit.this.currentRunE = null;
				}
				createInputAttributes(caretElement, getInputAttributesML(), editor);
			}
	    }
	    

//	    private void selectSdtBlock(JEditorPane editor, int pos) {
//			WordMLDocument doc = (WordMLDocument) editor.getDocument();
//			
//			BasicTextUI ui = (BasicTextUI) editor.getUI();
//			View root = ui.getRootView(editor).getView(0);
//
//			SdtBlockView currentSdt = null;
//			int idx = root.getViewIndex(pos, Position.Bias.Forward);
//			View v = root.getView(idx);
//			if (v instanceof SdtBlockView) {
//				currentSdt = (SdtBlockView) v;
//			}
//
//			SdtBlockView lastSdt = null;
//			if (lastSdtBlockPosition != null) {
//				idx = root.getViewIndex(lastSdtBlockPosition.getOffset(),
//						Position.Bias.Forward);
//				lastSdt = (SdtBlockView) root.getView(idx);
//			}
//
//			if (currentSdt != lastSdt) {
//				if (lastSdt != null) {
//					lastSdt.setBorderVisible(false);
//					ui.damageRange(editor, lastSdt.getStartOffset(), lastSdt
//							.getEndOffset(), Position.Bias.Forward,
//							Position.Bias.Forward);
//					lastSdtBlockPosition = null;
//				}
//			}
//			
//			if (currentSdt != null && !currentSdt.isBorderVisible()) {
//				currentSdt.setBorderVisible(true);
//				ui.damageRange(editor, currentSdt.getStartOffset(),
//						currentSdt.getEndOffset(), Position.Bias.Forward,
//						Position.Bias.Forward);
//				try {
//					lastSdtBlockPosition = doc.createPosition(pos);
//				} catch (BadLocationException exc) {
//					lastSdtBlockPosition = null;// should not happen
//				}
//			}
//		}
	    

//	    private void resetLastSdtBlockPosition(JEditorPane editor) {
//	    	if (lastSdtBlockPosition != null) {
//				BasicTextUI ui = (BasicTextUI) editor.getUI();
//				View root = ui.getRootView(editor).getView(0);
//
//				int idx = 
//					root.getViewIndex(
//						lastSdtBlockPosition.getOffset(), 
//						Position.Bias.Forward);
//				SdtBlockView sdt = (SdtBlockView) root.getView(idx);
//				sdt.setBorderVisible(false);
//				ui.damageRange(
//					editor, 
//					sdt.getStartOffset(), 
//					sdt.getEndOffset(), 
//					Position.Bias.Forward,
//					Position.Bias.Forward);
//				lastSdtBlockPosition = null;
//			}
//	    }
	    
	}// CaretListener inner class
	
    public static class FetchRemoteEditsAction extends TextAction {
    	private Exception exc;
    	   	
    	public FetchRemoteEditsAction() {
            super(fetchRemoteEditsAction);
            exc = null;
        }

        /** The operation to perform when this action is triggered. */
        public void actionPerformed(ActionEvent e) {
        	WordMLTextPane editor = (WordMLTextPane) getTextComponent(e);
        	if (editor != null) {
        		Mediator plutextClient = editor.getWordMLEditorKit().getPlutextClient();
        		if (plutextClient != null) {
                	log.debug("FetchRemoteEditsAction.actionPerformed():...");
                	
                	try {
                		editor.saveCaretText();
                		
                		editor.beginContentControlEdit();
                		plutextClient.startSession();
                		plutextClient.fetchUpdates();
                		plutextClient.applyRemoteChanges();
                		
                		WordMLDocument doc = (WordMLDocument) editor.getDocument();
                		int start = plutextClient.getUpdateStartOffset();
                		int end = plutextClient.getUpdateEndOffset();
                		doc.refreshParagraphs(start, end);
                		
                	} catch (Exception exc) {
                		this.exc = exc;
                	} finally {
                		plutextClient.endSession();
                		editor.endContentControlEdit();
                	}
        		}
        	}
        }
        
        public Exception getThrownException() {
        	return exc;
        }
        
        public boolean success() {
        	return (exc == null);
        }
        
    }// FetchRemoteEditsAction inner class
    
    public static class CommitLocalEditsAction extends TextAction {
    	private Exception exc;
    	
    	public CommitLocalEditsAction() {
            super(commitLocalEditsAction);
            exc = null;
        }

        /** The operation to perform when this action is triggered. */
        public void actionPerformed(ActionEvent e) {
        	WordMLTextPane editor = (WordMLTextPane) getTextComponent(e);
        	if (editor != null) {
        		WordMLDocument doc = (WordMLDocument) editor.getDocument();
        		Mediator plutextClient = editor.getWordMLEditorKit().getPlutextClient();
        		if (plutextClient != null) {
                	log.debug("CommitLocalEditsAction.actionPerformed():...");
                	try {
                		doc.lockWrite();
                		
                		editor.saveCaretText();
                		
                		plutextClient.startSession();
                		plutextClient.transmitLocalChanges();
                		
                	} catch (Exception exc) {
                		this.exc = exc;
                		
                	} finally {
                		plutextClient.endSession();
                		doc.unlockWrite();
                	}
        		}
        	}
        }
        
        public Exception getThrownException() {
        	return exc;
        }
        
        public boolean success() {
        	return (exc == null);
        }
        
    }// CommitLocalEditsAction inner class
    
    public static class AcceptNonConflictingRevisionsAction extends TextAction {
    	private Exception exc;
    	
    	public AcceptNonConflictingRevisionsAction() {
    		super(acceptNonConflictingRevisionsAction);
    		this.exc = null;
    	}
    	
        /** The operation to perform when this action is triggered. */
        public void actionPerformed(ActionEvent e) {
        	log.debug("AcceptNonConflictingRevisionsAction.actionPerformed():...");
        	
        	WordMLTextPane editor = (WordMLTextPane) getTextComponent(e);
        	if (editor != null) {
        		WordMLDocument doc = (WordMLDocument) editor.getDocument();
        		Mediator plutextClient = editor.getWordMLEditorKit().getPlutextClient();
        		if (plutextClient != null && plutextClient.hasNonConflictingChanges()) {
                	log.debug("AcceptNonConflictingRevisionsAction.actionPerformed():"
                		+ " plutextClient HAS non-conflicting changes");
                	
            		int caretPos = editor.getCaretPosition();
            		
                	try {
                		doc.lockWrite();
                		
                		editor.saveCaretText();
                		for (String id: plutextClient.getIdsOfNonConflictingChanges()) {
                    		plutextClient.removeTrackedChangeType(id);
                    		
                    		DocumentElement elem = Util.getDocumentElement(doc, id);
                    		if (elem != null) {
                        		
                        		ElementML sdt = elem.getElementML();
                    			String temp = 
                    				org.docx4j.XmlUtils.marshaltoString(
                    						sdt.getDocxObject()
                    					, false);
                    			StreamSource src = new StreamSource(new StringReader(temp));

                    			javax.xml.bind.util.JAXBResult result = 
                    				new javax.xml.bind.util.JAXBResult(
                    					org.docx4j.jaxb.Context.jc);
                    			XmlUtil.applyRemoteRevisions(src, result);
                    			
                    			ElementML newSdt = 
                    				new SdtBlockML(
                    					(org.docx4j.wml.SdtBlock) result.getResult());
                    			boolean notEmpty = 
                    				(XmlUtil.getLastRunContentML(newSdt) != null);
                    			if (notEmpty) {
                        			sdt.addSibling(newSdt, true);                    				
                    			}
                    			sdt.delete();
                    			
                    			int end = doc.getLength() - elem.getEndOffset();
                    			doc.refreshParagraphs(elem.getStartOffset(), 0);
                    			caretPos = doc.getLength() - end;
                    		}
                    	}//for (id) loop
                		
                	} catch (Exception exc) {
                		this.exc = exc;
                		
                	} finally {
                		doc.unlockWrite();
                		editor.setCaretPosition(caretPos);
                	}
        		} else {
                	log.debug("AcceptNonConflictingRevisionsAction.actionPerformed():"
                    		+ " NO plutextClient NOR non-conflicting changes");
        		} //if (plutextClient != null && plutextClient.hasNonConflictingChanges())
        	} //if (editor != null)
        	
        	
        } //actionPerformed()
        
        public Exception getThrownException() {
        	return this.exc;
        }
        
        public boolean success() {
        	return (this.exc == null);
        }
    	
    }// AcceptNonConflictingRevisionsAction inner class
    
    public static class RejectNonConflictingRevisionsAction extends TextAction {
    	private Exception exc;
    	
    	public RejectNonConflictingRevisionsAction() {
    		super(acceptNonConflictingRevisionsAction);
    		this.exc = null;
    	}
    	
        /** The operation to perform when this action is triggered. */
        public void actionPerformed(ActionEvent e) {
        	log.debug("RejectNonConflictingRevisionsAction.actionPerformed():...");
        	
        	WordMLTextPane editor = (WordMLTextPane) getTextComponent(e);
        	if (editor != null) {
        		WordMLDocument doc = (WordMLDocument) editor.getDocument();
        		Mediator plutextClient = editor.getWordMLEditorKit().getPlutextClient();
        		if (plutextClient != null && plutextClient.hasNonConflictingChanges()) {
                	log.debug("RejectNonConflictingRevisionsAction.actionPerformed():"
                    		+ " plutextClient HAS non-conflicting changes");
                	
            		int caretPos = editor.getCaretPosition();
            		
                	try {
                		doc.lockWrite();
                		
                		editor.saveCaretText();
                		for (String id: plutextClient.getIdsOfNonConflictingChanges()) {
                    		plutextClient.removeTrackedChangeType(id);
                    		
                    		DocumentElement elem = Util.getDocumentElement(doc, id);
                    		if (elem != null) {
                        		ElementML sdt = elem.getElementML();
                    			String temp = 
                    				org.docx4j.XmlUtils.marshaltoString(
                    						sdt.getDocxObject()
                    					, false);
                    			StreamSource src = new StreamSource(new StringReader(temp));

                    			javax.xml.bind.util.JAXBResult result = 
                    				new javax.xml.bind.util.JAXBResult(
                    					org.docx4j.jaxb.Context.jc);
                    			XmlUtil.discardRemoteRevisions(src, result);
                    			
                    			ElementML newSdt = 
                    				new SdtBlockML(
                    					(org.docx4j.wml.SdtBlock) result.getResult());
                    			boolean notEmpty = 
                    				(XmlUtil.getLastRunContentML(newSdt) != null);
                    			if (notEmpty) {
                        			sdt.addSibling(newSdt, true);                    				
                    			}
                    			sdt.delete();
                    			
                    			int end = doc.getLength() - elem.getEndOffset();
                    			doc.refreshParagraphs(elem.getStartOffset(), 0);
                    			caretPos = doc.getLength() - end;
                    		}
                    	}//for (id) loop
                		
                	} catch (Exception exc) {
                		this.exc = exc;
                		
                	} finally {
                		doc.unlockWrite();
                		editor.setCaretPosition(caretPos);
                	}
        		} else {
                	log.debug("RejectNonConflictingRevisionsAction.actionPerformed():"
                    		+ " NO plutextClient NOR non-conflicting changes");
        		} //if (plutextClient != null && plutextClient.hasNonConflictingChanges())
        	} //if (editor != null)
        } //actionPerformed()
        
        public Exception getThrownException() {
        	return this.exc;
        }
        
        public boolean success() {
        	return (this.exc == null);
        }
    	
    }// RejectNonConflictingRevisionsAction inner class
    
    public static class AcceptRevisionAction extends TextAction {
    	private boolean success;
    	
    	public AcceptRevisionAction() {
            super(acceptRevisionAction);
            success = Boolean.FALSE;
        }

        /** The operation to perform when this action is triggered. */
        public void actionPerformed(ActionEvent e) {
        	WordMLTextPane editor = (WordMLTextPane) getTextComponent(e);
            if (editor != null) {
            	WordMLDocument doc = (WordMLDocument) editor.getDocument();
            	try {
            		doc.lockWrite();
            		
            		editor.saveCaretText();
            	
            		int start = editor.getSelectionStart();
            		int end = editor.getSelectionEnd();
            		if (start < end 
            			&& start == DocUtil.getRevisionStart(doc, start, SwingConstants.NEXT)
            			&& end == DocUtil.getRevisionEnd(doc, start, SwingConstants.NEXT)) {
            			
            			DocumentElement elem = (DocumentElement) doc.getRunMLElement(start);

    					//new caret position is calculated from end of document
            			end = doc.getLength() - elem.getEndOffset();
            			
        				ElementML parent = elem.getElementML().getParent();
        				if (parent instanceof RunInsML) {
        					for (ElementML run: parent.getChildren()) {
        						ElementML copy = (ElementML) run.clone();
        						parent.addSibling(copy, false);
        					}
        					parent.delete();
        				} else if (parent instanceof RunDelML) {
        					parent.delete();
        				}
        				
        				elem = (DocumentElement) doc.getSdtBlockMLElement(start);
        				if (elem != null) {
        					//if revision is in content control
            				SdtBlockML sdt = (SdtBlockML) elem.getElementML();
            				Mediator client = editor.getWordMLEditorKit().getPlutextClient();
            				if (client != null
            					&& !XmlUtil.containsTrackedChanges(sdt.getDocxObject())) {
            					String id = sdt.getSdtProperties().getIdValue().toString();
            					client.removeTrackedChangeType(id);
            				}

            				boolean isEmpty = (XmlUtil.getLastRunContentML(sdt) == null);
            				if (isEmpty) {
            					sdt.delete();
            					
            					//new caret position is calculated from end of document
                    			end = doc.getLength() - elem.getEndOffset();
            				}
            			}
            			
            			doc.refreshParagraphs(start, 0);
            			editor.setCaretPosition(doc.getLength() - end);
        				success = Boolean.TRUE;
            		}
    			} finally {
    				doc.unlockWrite();
    			}
            }
        }
        
        public boolean success() {
        	return success;
        }
        
    }// AcceptRevisionAction inner class
    
    public static class RejectRevisionAction extends TextAction {
    	private boolean success;
    	
    	public RejectRevisionAction() {
            super(rejectRevisionAction);
            success = Boolean.FALSE;
        }

        /** The operation to perform when this action is triggered. */
        public void actionPerformed(ActionEvent e) {
        	WordMLTextPane editor = (WordMLTextPane) getTextComponent(e);
            if (editor != null) {
            	WordMLDocument doc = (WordMLDocument) editor.getDocument();
    			try {
    				doc.lockWrite();
            	
                	editor.saveCaretText();
                	
                	int start = editor.getSelectionStart();
                	int end = editor.getSelectionEnd();
                	if (start < end 
                		&& start == DocUtil.getRevisionStart(doc, start, SwingConstants.NEXT)
                		&& end == DocUtil.getRevisionEnd(doc, start, SwingConstants.NEXT)) {

        				DocumentElement elem = (DocumentElement) doc.getRunMLElement(start);

        				//new caret position is calculated from end of document
            			end = doc.getLength() - elem.getEndOffset();
            			
        				ElementML parent = elem.getElementML().getParent();
        				if (parent instanceof RunDelML) {
        					for (ElementML run: parent.getChildren()) {
        						ElementML copy = (ElementML) run.clone();
        						parent.addSibling(copy, false);
        					}
        					parent.delete();
        				} else if (parent instanceof RunInsML) {
        					parent.delete();
        				}
        				
        				elem = (DocumentElement) doc.getSdtBlockMLElement(start);
        				if (elem != null) {
        					//if revision is in content control
            				SdtBlockML sdt = (SdtBlockML) elem.getElementML();
            				Mediator client = editor.getWordMLEditorKit().getPlutextClient();
            				if (client != null
            					&& !XmlUtil.containsTrackedChanges(sdt.getDocxObject())) {
            					String id = sdt.getSdtProperties().getIdValue().toString();
            					client.removeTrackedChangeType(id);
            				}

            				boolean isEmpty = (XmlUtil.getLastRunContentML(sdt) == null);
            				if (isEmpty) {
            					sdt.delete();
            					
            					//new caret position is calculated from end of document
                    			end = doc.getLength() - elem.getEndOffset();
            				}
            			}
            			
            			doc.refreshParagraphs(start, 0);
            			editor.setCaretPosition(doc.getLength() - end);
        				success = Boolean.TRUE;
                	}
    			} finally {
    				doc.unlockWrite();
    			}
            }
        }
        
        public boolean success() {
        	return success;
        }
        
    }// RejectRevisionAction inner class
    
    public static class ApplyRemoteRevisionsInParaAction extends TextAction {
    	private boolean success;
    	
    	public ApplyRemoteRevisionsInParaAction() {
            super(applyRemoteRevisionsInParaAction);
            success = Boolean.FALSE;
        }

        /** The operation to perform when this action is triggered. */
        public void actionPerformed(ActionEvent e) {
        	WordMLTextPane editor = (WordMLTextPane) getTextComponent(e);
            if (editor != null) {
            	WordMLDocument doc = (WordMLDocument) editor.getDocument();
            	try {
            		doc.lockWrite();
            		
            		editor.saveCaretText();
            	
            		int pos = editor.getCaretPosition();
            		DocumentElement elem =
            			(DocumentElement) doc.getParagraphMLElement(pos, false);
            		ElementML oldPara = elem.getElementML();
            		
            		int start = editor.getSelectionStart();
            		int end = editor.getSelectionEnd();
            		if (elem.getStartOffset() <= start
            			&& end <= elem.getEndOffset()
            			&& XmlUtil.containsTrackedChanges(oldPara.getDocxObject())) {
            			
            			DocumentElement sdtElem = 
            				(DocumentElement) doc.getSdtBlockMLElement(pos);
            			
            			String temp = 
            				org.docx4j.XmlUtils.marshaltoString(
            					oldPara.getDocxObject(), 
            					false);

            			StreamSource src = new StreamSource(new StringReader(temp));

            			StreamResult result = new StreamResult(new ByteArrayOutputStream());
            			XmlUtil.applyRemoteRevisions(src, result);
            			
            			temp = result.getOutputStream().toString();
            			
            			ElementML newPara = 
            				new ParagraphML(org.docx4j.XmlUtils.unmarshalString(temp));
            			boolean notEmpty = 
            				(XmlUtil.getLastRunContentML(newPara) != null);
            			if (notEmpty) {
            				oldPara.addSibling(newPara, true);                    				
            			}
            			oldPara.delete();
            			
            			if (sdtElem != null) {
            				//if paragraph is in content control
            				SdtBlockML sdt = (SdtBlockML) sdtElem.getElementML();
            				Mediator client = editor.getWordMLEditorKit().getPlutextClient();
            				if (client != null
            					&& !XmlUtil.containsTrackedChanges(sdt.getDocxObject())) {
            					temp = sdt.getSdtProperties().getIdValue().toString();
            					client.removeTrackedChangeType(temp);
            				}

            				notEmpty = (XmlUtil.getLastRunContentML(sdt) != null);
            				if (!notEmpty) {
            					sdt.delete();
            				}
            			}
            			
            			end = doc.getLength() - elem.getEndOffset();
            			doc.refreshParagraphs(pos, 0);
            			editor.setCaretPosition(doc.getLength() - end);
        				success = Boolean.TRUE;
            		}
    			} finally {
    				doc.unlockWrite();
    			}
            }
        }
        
        public boolean success() {
        	return success;
        }
        
    }// ApplyRemoteRevisionsInParaAction inner class
    
    public static class DiscardRemoteRevisionsInParaAction extends TextAction {
    	private boolean success;
    	
    	public DiscardRemoteRevisionsInParaAction() {
            super(discardRemoteRevisionsInParaAction);
            success = Boolean.FALSE;
        }

        /** The operation to perform when this action is triggered. */
        public void actionPerformed(ActionEvent e) {
        	WordMLTextPane editor = (WordMLTextPane) getTextComponent(e);
            if (editor != null) {
            	WordMLDocument doc = (WordMLDocument) editor.getDocument();
            	try {
            		doc.lockWrite();
            		
            		editor.saveCaretText();
            	
            		int pos = editor.getCaretPosition();
            		DocumentElement elem =
            			(DocumentElement) doc.getParagraphMLElement(pos, false);
            		ElementML oldPara = elem.getElementML();
            		
            		int start = editor.getSelectionStart();
            		int end = editor.getSelectionEnd();
            		if (elem.getStartOffset() <= start
            			&& end <= elem.getEndOffset()
            			&& XmlUtil.containsTrackedChanges(oldPara.getDocxObject())) {
            			
            			DocumentElement sdtElem = 
            				(DocumentElement) doc.getSdtBlockMLElement(pos);
            			
            			String temp = 
            				org.docx4j.XmlUtils.marshaltoString(
            					oldPara.getDocxObject(), 
            					false);

            			StreamSource src = new StreamSource(new StringReader(temp));

            			StreamResult result = new StreamResult(new ByteArrayOutputStream());
            			XmlUtil.discardRemoteRevisions(src, result);
            			
            			temp = result.getOutputStream().toString();
            			
            			ElementML newPara = 
            				new ParagraphML(org.docx4j.XmlUtils.unmarshalString(temp));
            			boolean notEmpty = 
            				(XmlUtil.getLastRunContentML(newPara) != null);
            			if (notEmpty) {
            				oldPara.addSibling(newPara, true);                    				
            			}
            			oldPara.delete();
            			
            			if (sdtElem != null) {
            				//if paragraph is in content control
            				SdtBlockML sdt = (SdtBlockML) sdtElem.getElementML();
            				Mediator client = editor.getWordMLEditorKit().getPlutextClient();
            				if (client != null
            					&& !XmlUtil.containsTrackedChanges(sdt.getDocxObject())) {
            					temp = sdt.getSdtProperties().getIdValue().toString();
            					client.removeTrackedChangeType(temp);
            				}

            				notEmpty = (XmlUtil.getLastRunContentML(sdt) != null);
            				if (!notEmpty) {
            					sdt.delete();
            				}
            			}
            			
            			end = doc.getLength() - elem.getEndOffset();
            			doc.refreshParagraphs(pos, 0);
            			editor.setCaretPosition(doc.getLength() - end);
        				success = Boolean.TRUE;
            		}
    			} finally {
    				doc.unlockWrite();
    			}
            }
        }
                
        public boolean success() {
        	return success;
        }
        
    }// DiscardRemoteRevisionsInParaAction inner class
    
    public static class SelectNextRevisionAction extends TextAction {
    	public SelectNextRevisionAction() {
            super(selectNextRevisionAction);
        }

        /** The operation to perform when this action is triggered. */
        public void actionPerformed(ActionEvent e) {
        	WordMLTextPane editor = (WordMLTextPane) getTextComponent(e);
            if (editor != null) {
            	WordMLDocument doc = (WordMLDocument) editor.getDocument();
            	try {
            		doc.readLock();
            		
            		int pos = editor.getCaretPosition();
            		editor.setCaretPosition(pos); //clear up selection
                
            		if (pos < doc.getLength()) {
            			int start = DocUtil.getRevisionStart(doc, pos, SwingConstants.NEXT);
            			if (start > -1) {
            				int end = DocUtil.getRevisionEnd(doc, start, SwingConstants.NEXT);
            				if (end > -1) {
            					editor.setCaretPosition(start);
            					editor.moveCaretPosition(end);
            				}
            			}
            		}
        		} finally {
        			doc.readUnlock();
        		}
            }
        }
    }// SelectNextRevisionAction inner class
    
    public static class SelectPrevRevisionAction extends TextAction {
    	public SelectPrevRevisionAction() {
            super(selectPrevRevision);
        }

        /** The operation to perform when this action is triggered. */
        public void actionPerformed(ActionEvent e) {
        	WordMLTextPane editor = (WordMLTextPane) getTextComponent(e);
            if (editor != null) {
            	WordMLDocument doc = (WordMLDocument) editor.getDocument();
            	try {
            		doc.readLock();
            	
            		int pos = editor.getSelectionStart();
            		editor.setCaretPosition(pos); //clear up selection
                
            		int start = DocUtil.getRevisionStart(doc, pos, SwingConstants.PREVIOUS);
            		if (start > -1) {
            			int end = DocUtil.getRevisionEnd(doc, start, SwingConstants.NEXT);
            			if (end > -1) {
            				editor.setCaretPosition(start);
            				editor.moveCaretPosition(end);
            			}
            		}
        		} finally {
        			doc.readUnlock();
        		}
            }
        }
    }// SelectPrevRevisionAction inner class
    
	public abstract static class StyledTextAction extends javax.swing.text.StyledEditorKit.StyledTextAction {
        public StyledTextAction(String nm) {
    	    super(nm);
    	}
        
        protected final void setRunMLAttributes(final WordMLTextPane editor,
				AttributeSet attrs, boolean replace) {
        	
			Caret caret = editor.getCaret();
			int dot = caret.getDot();
			int mark = caret.getMark();
			
			WordMLEditorKit kit = (WordMLEditorKit) editor.getEditorKit();
			kit.saveCaretText();
			
			WordMLDocument doc = (WordMLDocument) editor.getDocument();
			
			int p0 = editor.getSelectionStart();
			int p1 = editor.getSelectionEnd();
			if (p0 == p1) {
				try {
					int wordStart = DocUtil.getWordStart(editor, p0);
					int wordEnd = DocUtil.getWordEnd(editor, p1);
					if (wordStart < p0 && p0 < wordEnd) {
						p0 = wordStart;
						p1 = wordEnd;
					}
				} catch (BadLocationException exc) {
					;//ignore
				}
			}
			
			if (p0 != p1) {
				try {
					doc.setRunMLAttributes(p0, p1 - p0, attrs, replace);
					editor.setCaretPosition(mark);
					editor.moveCaretPosition(dot);
				} catch (BadLocationException exc) {
					exc.printStackTrace();
					;//ignore
				}
			} else {
				MutableAttributeSet inputAttributes = 
					(MutableAttributeSet) kit.getInputAttributesML();
				if (replace) {
					inputAttributes.removeAttributes(inputAttributes);
				}
				inputAttributes.addAttributes(attrs);
				kit.fireInputAttributeChanged(new InputAttributeEvent(editor));
			}
			
			if (log.isDebugEnabled()) {
				DocUtil.displayStructure(doc);
			}
		}

		protected final void setParagraphMLAttributes(WordMLTextPane editor,
				AttributeSet attr, boolean replace) {
			
			Caret caret = editor.getCaret();
			int dot = caret.getDot();
			int mark = caret.getMark();
			
			WordMLEditorKit kit = (WordMLEditorKit) editor.getEditorKit();
			kit.saveCaretText();
			
			int p0 = editor.getSelectionStart();
			int p1 = editor.getSelectionEnd();
			
			WordMLDocument doc = (WordMLDocument) editor.getDocument();
			try {
				doc.setParagraphMLAttributes(p0, (p1 - p0), attr, replace);
			
				editor.setCaretPosition(mark);
				editor.moveCaretPosition(dot);
			} catch (BadLocationException exc) {
				exc.printStackTrace();//ignore
			}
			
			if (log.isDebugEnabled()) {
				DocUtil.displayStructure(doc);
			}
		}
		
        protected final void setRunStyle(WordMLTextPane editor, String styleId) {
			Caret caret = editor.getCaret();
			int dot = caret.getDot();
			int mark = caret.getMark();
			
			WordMLEditorKit kit = (WordMLEditorKit) editor.getEditorKit();
			kit.saveCaretText();
			
			WordMLDocument doc = (WordMLDocument) editor.getDocument();
			
			int p0 = editor.getSelectionStart();
			int p1 = editor.getSelectionEnd();
			if (p0 == p1) {
				try {
					int wordStart = DocUtil.getWordStart(editor, p0);
					int wordEnd = DocUtil.getWordEnd(editor, p1);
					if (wordStart < p0 && p0 < wordEnd) {
						p0 = wordStart;
						p1 = wordEnd;
					}
				} catch (BadLocationException exc) {
					;//ignore
				}
			}
			
			if (p0 != p1) {
				doc.setRunStyle(p0, p1 - p0, styleId);
				editor.setCaretPosition(mark);
				editor.moveCaretPosition(dot);
				kit.refreshCaretElement(editor);
			} else {
				Style style = doc.getStyleSheet().getReferredStyle(styleId);
				if (style != null) {
					MutableAttributeSet inputAttributes = (MutableAttributeSet) kit
							.getInputAttributesML();
					inputAttributes.removeAttributes(inputAttributes);
					inputAttributes.addAttribute(
							WordMLStyleConstants.RStyleAttribute, styleId);
					kit.fireInputAttributeChanged(new InputAttributeEvent(
							editor));
				}
			}
			
			if (log.isDebugEnabled()) {
				DocUtil.displayStructure(doc);
			}
		}

		protected final void setParagraphStyle(WordMLTextPane editor, String styleId) {
			Caret caret = editor.getCaret();
			int dot = caret.getDot();
			int mark = caret.getMark();
			
			WordMLEditorKit kit = (WordMLEditorKit) editor.getEditorKit();
			kit.saveCaretText();
			
			int p0 = editor.getSelectionStart();
			int p1 = editor.getSelectionEnd();
			WordMLDocument doc = (WordMLDocument) editor.getDocument();
			doc.setParagraphStyle(p0, (p1 - p0), styleId);
			
			editor.setCaretPosition(mark);
			editor.moveCaretPosition(dot);
			
			kit.refreshCaretElement(editor);
			
			if (log.isDebugEnabled()) {
				DocUtil.displayStructure(doc);
			}
		}
		
	}// StyledTextAction inner class
	
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
			if (editor instanceof WordMLTextPane) {
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
				setParagraphMLAttributes((WordMLTextPane) editor, attr, false);
			}
		}
	}// AlignmentAction inner class
	
	public static class ApplyStyleAction extends StyledTextAction {
		private String styleName;
		
		public ApplyStyleAction(String nm, String styleName) {
			super(nm);
			this.styleName = styleName;
		}
		
		public void actionPerformed(ActionEvent e) {
			log.debug("ApplyStyleAction.actionPerformed():...");
			JEditorPane editor = getEditor(e);
			if (editor instanceof WordMLTextPane) {
				if (this.styleName != null) {
					WordMLDocument doc = (WordMLDocument) editor.getDocument();
					Style s = doc.getStyleSheet().getReferredStyle(this.styleName);
					String styleId = 
						(String) s.getAttribute(WordMLStyleConstants.StyleIdAttribute);
					String type = 
						(String) s.getAttribute(WordMLStyleConstants.StyleTypeAttribute);
					if (StyleSheet.PARAGRAPH_ATTR_VALUE.equals(type)) {
						setParagraphStyle((WordMLTextPane) editor, styleId);
					} else if (StyleSheet.CHARACTER_ATTR_VALUE.equals(type)) {
						setRunStyle((WordMLTextPane) editor, styleId);
					}
				} else {
					UIManager.getLookAndFeel().provideErrorFeedback(editor);
				}
			}
		}
	}// ApplyStyleAction inner class
	
    public static class FontFamilyAction extends StyledTextAction {
		private String family;

		/**
		 * Creates a new FontFamilyAction.
		 * 
		 * @param nm
		 *            the action name
		 * @param family
		 *            the font family
		 */
		public FontFamilyAction(String nm, String family) {
			super(nm);
			this.family = family;
		}

		/**
		 * Sets the font family.
		 * 
		 * @param e
		 *            the event
		 */
		public void actionPerformed(ActionEvent e) {
			JEditorPane editor = getEditor(e);
			if (editor instanceof WordMLTextPane) {
				if (this.family != null) {
					MutableAttributeSet attr = new SimpleAttributeSet();
					StyleConstants.setFontFamily(attr, this.family);
					setRunMLAttributes((WordMLTextPane) editor, attr, false);
				} else {
					UIManager.getLookAndFeel().provideErrorFeedback(editor);
				}
			}
		}

	} //FontFamilyAction inner class

    public static class FontSizeAction extends StyledTextAction {

		/**
		 * Creates a new FontSizeAction.
		 * 
		 * @param nm
		 *            the action name
		 * @param size
		 *            the font size
		 */
		public FontSizeAction(String nm, int size) {
			super(nm);
			this.size = size;
		}

		/**
		 * Sets the font size.
		 * 
		 * @param e
		 *            the action event
		 */
		public void actionPerformed(ActionEvent e) {
			JEditorPane editor = getEditor(e);
			if (editor instanceof WordMLTextPane) {
				if (this.size != 0) {
					MutableAttributeSet attr = new SimpleAttributeSet();
					StyleConstants.setFontSize(attr, this.size);
					setRunMLAttributes((WordMLTextPane) editor, attr, false);
				} else {
					UIManager.getLookAndFeel().provideErrorFeedback(editor);
				}
			}
		}

		private int size;
	}

	public static class BoldAction extends StyledTextAction {
		private boolean isBold;
		
		public BoldAction(boolean isBold) {
		    super(fontBoldAction);
		    this.isBold = isBold;
		}

        public void actionPerformed(ActionEvent e) {
			JEditorPane editor = getEditor(e);
			if (editor instanceof WordMLTextPane) {
				SimpleAttributeSet sas = new SimpleAttributeSet();
				StyleConstants.setBold(sas, this.isBold);
				setRunMLAttributes((WordMLTextPane) editor, sas, false);
			}
		}
	}// BoldAction inner class
	
	public static class ItalicAction extends StyledTextAction {
		private boolean isItalic;
		
		public ItalicAction(boolean isItalic) {
		    super(fontItalicAction);
		    this.isItalic = isItalic;
		}

        public void actionPerformed(ActionEvent e) {
			JEditorPane editor = getEditor(e);
			if (editor instanceof WordMLTextPane) {
				SimpleAttributeSet sas = new SimpleAttributeSet();
				StyleConstants.setItalic(sas, this.isItalic);
				setRunMLAttributes((WordMLTextPane) editor, sas, false);
			}
		}
	}// ItalicAction inner class
	
	public static class UnderlineAction extends StyledTextAction {
		private boolean isUnderlined;
		
		public UnderlineAction(boolean isUnderlined) {
		    super(fontUnderlineAction);
		    this.isUnderlined = isUnderlined;
		}

        public void actionPerformed(ActionEvent e) {
			JEditorPane editor = getEditor(e);
			if (editor instanceof WordMLTextPane) {
				SimpleAttributeSet sas = new SimpleAttributeSet();
				StyleConstants.setUnderline(sas, this.isUnderlined);
				setRunMLAttributes((WordMLTextPane) editor, sas, false);
			}
		}
	}// UnderlineAction inner class
	
    private static class InsertSoftBreakAction extends StyledTextAction {
    	public InsertSoftBreakAction(String name) {
    		super(name);
    	}
    	
    	public void actionPerformed(ActionEvent e) {
			JEditorPane editor = getEditor(e);
			if (editor instanceof WordMLTextPane) {
				if ((! editor.isEditable()) || (! editor.isEnabled())) {
				    UIManager.getLookAndFeel().provideErrorFeedback(editor);
				    return;
				}
				
				if (log.isDebugEnabled()) {
					log.debug("InsertSoftBreakAction.actionPerformed()");
				}
			}
    	}
    }// InsertSoftBreakAction inner class
    
    private static class EnterKeyTypedAction extends StyledTextAction {
    	public EnterKeyTypedAction(String name) {
    		super(name);
    	}
    	
    	public void actionPerformed(ActionEvent e) {
			final JEditorPane editor = getEditor(e);
			if (editor instanceof WordMLTextPane) {
				if ((! editor.isEditable()) || (! editor.isEnabled())) {
				    UIManager.getLookAndFeel().provideErrorFeedback(editor);
				    return;
				}
				
				WordMLEditorKit kit = (WordMLEditorKit) editor.getEditorKit();
				kit.saveCaretText();
				
				editor.replaceSelection(Constants.NEWLINE);
			}
    	}
    }// EnterKeyTypedAction inner class
    
    private static class DeleteNextCharAction extends StyledTextAction {

		/* Create this object with the appropriate identifier. */
		DeleteNextCharAction(String name) {
			super(deleteNextCharAction);
		}

		/** The operation to perform when this action is triggered. */
		public void actionPerformed(ActionEvent e) {
			final JEditorPane editor = getEditor(e);
			if (editor instanceof WordMLTextPane) {
				if ((! editor.isEditable()) || (! editor.isEnabled())) {
				    UIManager.getLookAndFeel().provideErrorFeedback(editor);
				    return;
				}
				WordMLDocument doc = (WordMLDocument) editor.getDocument();
				
				Caret caret = editor.getCaret();
				int dot = caret.getDot();
				int mark = caret.getMark();
				
				WordMLEditorKit kit = (WordMLEditorKit) editor.getEditorKit();
				if (kit.getCaretElement() != doc.getCharacterElement(dot)) {
					kit.saveCaretText();
				}
				
				if (log.isDebugEnabled()) {
					log.debug("DeleteNextCharAction.actionPerformed(): dot=" + dot
						+ " doc.getLength()=" + doc.getLength());
				}
							
				try {
					if (dot != mark) {
						doc.remove(Math.min(dot, mark), Math.abs(dot - mark));
						dot = Math.min(dot, mark);
						
					} else if (dot < doc.getLength()) {
						int delChars = 1;

						String dotChars = doc.getText(dot, 2);
						char c0 = dotChars.charAt(0);
						char c1 = dotChars.charAt(1);

						if (c0 >= '\uD800' && c0 <= '\uDBFF'
								&& c1 >= '\uDC00' && c1 <= '\uDFFF') {
							delChars = 2;
						}

						doc.remove(dot, delChars);
					}
					
					caret.setDot(dot);
					
				} catch (BadLocationException exc) {
					;// ignore
				}
				
			}//if (editor != null)
		}//actionPerformed()
	}//DeleteNextCharAction()

    private static class DeletePrevCharAction extends StyledTextAction {

		/* Create this object with the appropriate identifier. */
    	DeletePrevCharAction(String name) {
			super(deletePrevCharAction);
		}

		/** The operation to perform when this action is triggered. */
		public void actionPerformed(ActionEvent e) {
			final JEditorPane editor = getEditor(e);
			if (editor instanceof WordMLTextPane) {
				if ((! editor.isEditable()) || (! editor.isEnabled())) {
				    UIManager.getLookAndFeel().provideErrorFeedback(editor);
				    return;
				}
				
				final WordMLDocument doc = (WordMLDocument) editor.getDocument();
				Caret caret = editor.getCaret();
				int dot = caret.getDot();
				int mark = caret.getMark();
				
				WordMLEditorKit kit = (WordMLEditorKit) editor.getEditorKit();
				if (kit.getCaretElement() != doc.getCharacterElement(dot - 1)) {
					kit.saveCaretText();
				}
				
				if (log.isDebugEnabled()) {
					log.debug("DeletePrevCharAction.actionPerformed(): dot=" + dot
						+ " doc.getLength()=" + doc.getLength());
				}
				
				try {
					if (dot != mark) {
						doc.remove(Math.min(dot, mark), Math.abs(dot - mark));
						dot = Math.min(dot, mark);
						
					} else if (0 < dot && dot < doc.getLength()) {
	                    int delChars = 1;
	                    
	                    if (dot > 1) {
	                        String dotChars = doc.getText(dot - 2, 2);
	                        char c0 = dotChars.charAt(0);
	                        char c1 = dotChars.charAt(1);
	                        
	                        if (c0 >= '\uD800' && c0 <= '\uDBFF' &&
	                            c1 >= '\uDC00' && c1 <= '\uDFFF') {
	                            delChars = 2;
	                        }
	                    }
	                    
	                    dot = dot - delChars;
	                    doc.remove(dot, delChars);
					}
					
					caret.setDot(dot);
					
				} catch (BadLocationException exc) {
					;//ignore
				}
			}//if (editor != null)
		}//actionPerformed()
		
	}//DeletePrevCharAction()

}// WordMLEditorKit class
























