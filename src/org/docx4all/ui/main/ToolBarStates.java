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

package org.docx4all.ui.main;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Hashtable;

import javax.swing.JEditorPane;
import javax.swing.JInternalFrame;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.text.Element;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import org.apache.log4j.Logger;
import org.docx4all.swing.text.WordMLDocument;
import org.docx4all.util.SwingUtil;

/**
 *	@author Jojada Tirtowidjojo - 30/11/2007
 */
public class ToolBarStates extends InternalFrameAdapter implements FocusListener, DocumentListener, CaretListener {
	private static Logger log = Logger.getLogger(ToolBarStates.class);
	
	/**
	 * The binding key used for this ToolBarStates object 
	 * when passed into scripting environment
	 */
	public final static String SCRIPT_BINDING_KEY = "toolBarStates:org.docx4all.ui.main.ToolBarStates";
	
	public final static String DOC_DIRTY_PROPERTY_NAME = "documentDirty";
	public final static String ALL_DOC_DIRTY_PROPERTY_NAME = "allDocumentDirty";
	
	public final static String FONT_FAMILY_PROPERTY_NAME = "fontFamily";
	public final static String FONT_SIZE_PROPERTY_NAME = "fontSize";
	public final static String FONT_BOLD_PROPERTY_NAME = "fontBold";
	public final static String FONT_ITALIC_PROPERTY_NAME = "fontItalic";
	public final static String FONT_UNDERLINED_PROPERTY_NAME = "fontUnderlined";
	
	public final static String PARAGRAPH_STYLE_PROPERTY_NAME = "paragraphStyle";
	public final static String ALIGNMENT_PROPERTY_NAME = "alignment";
	
	private final Hashtable<JEditorPane, Boolean> _dirtyTable;
	private volatile JEditorPane _currentEditor;
	private volatile String _fontFamily;
	private volatile int _fontSize;
	private volatile boolean _fontBold, _fontItalic, _fontUnderlined;
	
	private volatile String _paraStyle;
	private volatile int _alignment;
	
	private PropertyChangeSupport changeSupport = null;
	
	public ToolBarStates() {
		_dirtyTable = new Hashtable<JEditorPane, Boolean>(5);
		_fontBold = false;
		_fontItalic = false;
		_fontUnderlined = false;
		_alignment = -1;
	}
	
	public boolean isDocumentDirty() {
		return isDocumentDirty(_currentEditor);
	}
	
	public boolean isDocumentDirty(JEditorPane editor) {
		Boolean isDirty = Boolean.FALSE;
		if (editor != null) {
			isDirty = _dirtyTable.get(editor);
			if (isDirty == null) {
				isDirty = Boolean.FALSE;
			}
		}
		return isDirty.booleanValue();
	}
	
	public boolean isAllDocumentDirty() {
		boolean isDirty = false;
		
		boolean init = true;
		for (Boolean b: _dirtyTable.values()) {
			if (init) {
				init = false;
				isDirty = b.booleanValue();
			}
			isDirty = isDirty || b.booleanValue();
		}
		return isDirty;
	}
	
	public void setDocumentDirty(boolean dirty) {
		setDocumentDirty(_currentEditor, dirty);
	}
	
	public void setDocumentDirty(JEditorPane editor, boolean dirty) {
		if (editor == null) {
			return;
		}
		
		if (log.isDebugEnabled()) {
			String file = 
				(String) editor.getDocument().getProperty(
					WordMLDocument.FILE_PATH_PROPERTY);
			log.debug("setDocumentDirty(): File = " + file
				+ " - 'dirty' parameter = " + dirty);
		}
		
		//record current isAllDocumentDirty() state for later use
		Boolean oldAllDirty = Boolean.valueOf(isAllDocumentDirty());
		
		Boolean isDirty = _dirtyTable.get(editor);
		if (isDirty == null) {
			isDirty = Boolean.FALSE;
		}
		
		Boolean newDirty = Boolean.valueOf(dirty);
		if (isDirty == newDirty) {
			//no change
			return;
		}
		
		_dirtyTable.put(editor, newDirty);
		if (editor == _currentEditor) {
			firePropertyChange(DOC_DIRTY_PROPERTY_NAME, Boolean.valueOf(isDirty), newDirty);
		}

		//Check the resulting isAllDocumentDirty() state
		newDirty = Boolean.valueOf(isAllDocumentDirty());
		if (oldAllDirty == newDirty) {
			//no change
			return;
		}
		firePropertyChange(ALL_DOC_DIRTY_PROPERTY_NAME, oldAllDirty, newDirty);
	}
	
	public String getParagraphStyle() {
		return _paraStyle;
	}
	
	public void setParagraphStyle(String paraStyle) {
		if (log.isDebugEnabled()) {
			log.debug("setParagraphStyle(): _paraStyle = " + _paraStyle + " fontFamily param = " + paraStyle);
		}
	
		if (_paraStyle == paraStyle
			|| (_paraStyle != null && _paraStyle.equals(paraStyle))) {
			return;
		}
		
		String oldValue = _paraStyle;
		_paraStyle = paraStyle;
		firePropertyChange(PARAGRAPH_STYLE_PROPERTY_NAME, oldValue, paraStyle);
	}
	
	public int getAlignment() {
		return _alignment;
	}
	
	public void setAlignment(int alignment) {
		if (log.isDebugEnabled()) {
			log.debug("setAlignment(): _alignment = " + _alignment + " alignment param = " + alignment);
		}
	
		if (_alignment == alignment
			|| alignment < StyleConstants.ALIGN_LEFT
			|| alignment > StyleConstants.ALIGN_JUSTIFIED) {
			return;
		}
		int oldValue = _alignment;
		_alignment = alignment;
		firePropertyChange(
			ALIGNMENT_PROPERTY_NAME, 
			Integer.valueOf(oldValue), 
			Integer.valueOf(alignment));
	}
	
	public String getFontFamily() {
		return _fontFamily;
	}
	
	public void setFontFamily(String fontFamily) {
		if (log.isDebugEnabled()) {
			log.debug("setFontFamily(): _fontFamily = " + _fontFamily + " fontFamily param = " + fontFamily);
		}
	
		if (_fontFamily == fontFamily
			|| (_fontFamily != null && _fontFamily.equals(fontFamily))) {
			return;
		}
		
		String oldValue = _fontFamily;
		_fontFamily = fontFamily;
		firePropertyChange(FONT_FAMILY_PROPERTY_NAME, oldValue, fontFamily);
	}
	
	public int getFontSize() {
		return _fontSize;
	}
	
	public void setFontSize(String fontSize) {
		setFontSize(Integer.parseInt(fontSize));
	}
	
	public void setFontSize(int fontSize) {
		if (log.isDebugEnabled()) {
			log.debug("setFontSize(): _fontSize = " + _fontSize + " fontSize param = " + fontSize);
		}
	
		if (_fontSize == fontSize) {
			return;
		}
		int oldValue = _fontSize;
		_fontSize = fontSize;
		firePropertyChange(
			FONT_SIZE_PROPERTY_NAME, Integer.toString(oldValue), Integer.toString(fontSize));
	}
	
	public boolean isFontBold() {
		return _fontBold;
	}
	
	public void setFontBold(boolean bold) {
		if (log.isDebugEnabled()) {
			log.debug("setFontBold(): _fontBold = " + _fontBold + " bold param = " + bold);
		}
	
		if (_fontBold == bold) {
			return;
		}
		boolean oldValue = _fontBold;
		_fontBold = bold;
		firePropertyChange(FONT_BOLD_PROPERTY_NAME, oldValue, bold);
	}
	
	public boolean isFontItalic() {
		return _fontItalic;
	}
	
	public void setFontItalic(boolean italic) {
		if (_fontItalic == italic) {
			return;
		}
		boolean oldValue = _fontItalic;
		_fontItalic = italic;
		firePropertyChange(FONT_ITALIC_PROPERTY_NAME, oldValue, italic);
	}
	
	public boolean isFontUnderlined() {
		return _fontUnderlined;
	}
	
	public void setFontUnderlined(boolean underlined) {
		if (_fontUnderlined == underlined) {
			return;
		}
		boolean oldValue = _fontUnderlined;
		_fontUnderlined = underlined;
		firePropertyChange(FONT_UNDERLINED_PROPERTY_NAME, oldValue, underlined);
	}	
	
	public JEditorPane getCurrentEditor() {
		return _currentEditor;
	}
	
    /**
     * Adds a PropertyChangeListener to the listener list. The listener is
     * registered for all bound properties of this class.
     * If listener is null, no exception is thrown and no action is performed.
     *
     * @param listener the PropertyChangeListener to be added
     * @see #removePropertyChangeListener
     * @see #getPropertyChangeListeners
     * @see #addPropertyChangeListener(java.lang.String,java.beans.PropertyChangeListener)
     */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
		if (listener != null) {
			synchronized (this) {
				if (changeSupport == null) {
					changeSupport = new PropertyChangeSupport(this);
				}
				changeSupport.addPropertyChangeListener(listener);
			}
		}
	}

    /**
     * Removes a PropertyChangeListener from the listener list. This method
     * should be used to remove the PropertyChangeListeners that were
     * registered for all bound properties of this class.
     * <p>
     * If listener is null, no exception is thrown and no action is performed.
     *
     * @param listener the PropertyChangeListener to be removed
     * @see #addPropertyChangeListener
     * @see #removePropertyChangeListener(java.lang.String,java.beans.PropertyChangeListener)
     */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
		if (listener != null) {
			synchronized (this) {
				if (changeSupport != null) {
					changeSupport.removePropertyChangeListener(listener);
				}
			}
		}
	}
	
    /**
     * Returns an array of all registered property change listeners
     *
     * @return all of this ToolBarStates's
     *         <code>PropertyChangeListener</code>s
     *         or an empty array if no property change 
     *         listeners are currently registered
     *
     * @see #addPropertyChangeListener
     * @see #removePropertyChangeListener
     * @see #getPropertyChangeListeners()
     */
    public synchronized PropertyChangeListener[] getPropertyChangeListeners() {
        if (changeSupport == null) {
            changeSupport = new PropertyChangeSupport(this);
        }
        return changeSupport.getPropertyChangeListeners();
    }

    /**
     * Adds a PropertyChangeListener to the listener list for a specific
     * property. 
     * If listener is null, no exception is thrown and no action is performed.
     *
     * @param propertyName one of the property names listed above
     * @param listener the PropertyChangeListener to be added
     * @see #addPropertyChangeListener(java.beans.PropertyChangeListener)
     * @see #removePropertyChangeListener(java.lang.String,java.beans.PropertyChangeListener)
     * @see #getPropertyChangeListeners(java.lang.String)
     */
    public void addPropertyChangeListener(String propertyName,
			PropertyChangeListener listener) {
		if (listener != null) {
			synchronized (this) {
				if (changeSupport == null) {
					changeSupport = new PropertyChangeSupport(this);
				}
				changeSupport.addPropertyChangeListener(propertyName, listener);
			}
		}
	}

    /**
	 * Removes a PropertyChangeListener from the listener list for a specific
	 * property. This method should be used to remove PropertyChangeListeners
	 * that were registered for a specific bound property.
	 * <p>
	 * If listener is null, no exception is thrown and no action is performed.
	 * 
	 * @param propertyName
	 *            a valid property name
	 * @param listener
	 *            the PropertyChangeListener to be removed
	 * @see #addPropertyChangeListener(java.lang.String,java.beans.PropertyChangeListener)
	 * @see #getPropertyChangeListeners(java.lang.String)
	 * @see #removePropertyChangeListener(java.beans.PropertyChangeListener)
	 */
    public void removePropertyChangeListener(String propertyName,
			PropertyChangeListener listener) {
		if (listener != null) {
			synchronized (this) {
				if (changeSupport != null) {
					changeSupport.removePropertyChangeListener(propertyName,
							listener);
				}
			}
		}
	}

    /**
	 * Returns an array of all the <code>PropertyChangeListener</code>s
	 * associated with the named property.
	 * 
	 * @return all of the <code>PropertyChangeListener</code>s associated
	 *         with the named property or an empty array if no such listeners
	 *         have been added.
	 * 
	 * @see #addPropertyChangeListener(java.lang.String,java.beans.PropertyChangeListener)
	 * @see #removePropertyChangeListener(java.lang.String,java.beans.PropertyChangeListener)
	 */
    public synchronized PropertyChangeListener[] getPropertyChangeListeners(String propertyName) {
        if (changeSupport == null) {
            changeSupport = new PropertyChangeSupport(this);
        }
        return changeSupport.getPropertyChangeListeners(propertyName);
    }

    /**
     * Fires a PropertyChangeEvent in response to a change in a bound property.
     * The event will be delivered to all registered PropertyChangeListeners.
     * No event will be delivered if oldValue and newValue are the same.
     *
     * @param propertyName the name of the property that has changed
     * @param oldValue the property's previous value
     * @param newValue the property's new value
     */
    private void firePropertyChange(String propertyName, Object oldValue,
			Object newValue) {
		if (oldValue == newValue) {
			return;
		}
		PropertyChangeSupport changeSupport = this.changeSupport;
		if (changeSupport != null) {
			changeSupport.firePropertyChange(propertyName, oldValue, newValue);
		}
	}

    private void setFormatInfo(JEditorPane editor) {
    	StyledDocument doc = (StyledDocument) editor.getDocument();
    	
    	int pos = Math.min(editor.getSelectionStart(), editor.getSelectionEnd());
    	Element paragraph = doc.getParagraphElement(pos);
    	Element text = null;
	    // If nothing is selected, get the attributes from the character
	    // before the start of the selection, otherwise get the attributes
	    // from the character element at the start of the selection.
	    if (paragraph.getStartOffset() == pos 
	    	|| editor.getSelectionStart() != editor.getSelectionEnd()) {
			// Get the attributes from the character at the selection
			// if in a different paragrah!
	    	text = doc.getCharacterElement(pos);
		} else {
			text = doc.getCharacterElement(Math.max(pos-1, 0));
		}
  
	    setFontFamily(StyleConstants.getFontFamily(text.getAttributes()));
	    setFontSize(StyleConstants.getFontSize(text.getAttributes()));
	    setFontBold(StyleConstants.isBold(text.getAttributes()));
	    setFontItalic(StyleConstants.isItalic(text.getAttributes()));
	    setFontUnderlined(StyleConstants.isUnderline(text.getAttributes()));
	    
	    setAlignment(StyleConstants.getAlignment(paragraph.getAttributes()));
    }

	// ============================
	// FocusListener Implementation
	// ============================
	
    /**
     * Invoked when a JEditorPane gains the keyboard focus.
     */
    public void focusGained(FocusEvent e) {
    	if (log.isDebugEnabled()) {
    		log.debug("focusGained(): evt.getSource = " + e.getSource());
    		log.debug("focusGained(): _currentEditor = " + _currentEditor);
    	}

    	JEditorPane newEditor = (JEditorPane) e.getSource();
    	setFormatInfo(newEditor);
    	
		Boolean newDirty = _dirtyTable.get(newEditor);
		if (newDirty == null) {
			newDirty = Boolean.FALSE;
		}
    	
		if (newEditor == _currentEditor) {
			;//return
			
		} else if (_currentEditor == null) {
    		//initial condition
    		_dirtyTable.put(newEditor, newDirty);
    		_currentEditor = newEditor;
    	
    	} else {
        	Boolean currentDirty = isDocumentDirty();
        	
        	//Before calling setDocumentDirty(newDirty)
        	//we need to satisfy its precondition first;
        	_currentEditor = newEditor;
        	_dirtyTable.put(newEditor, currentDirty);
        	setDocumentDirty(newDirty);
		}
    }

    /**
     * Invoked when a JEditorPane loses the keyboard focus.
     */
    public void focusLost(FocusEvent e) {
    	if (log.isDebugEnabled()) {
    		log.debug("focusLost():");
    	}
    	;//not implemented
    }
    
	//===============================
	//DocumentListener Implementation
	//===============================
	
    /**
     * Gives notification that there was an insert into the document.  The 
     * range given by the DocumentEvent bounds the freshly inserted region.
     *
     * @param e the document event
     */
    public void insertUpdate(DocumentEvent e) {
    	if (log.isDebugEnabled()) {
    		log.debug("insertUpdate():");
    	}
    	if (_currentEditor != null 
        		&& _currentEditor.getDocument() == e.getDocument()) {
    		setDocumentDirty(true);
    		setFormatInfo(_currentEditor);
    	}
    }

    /**
     * Gives notification that a portion of the document has been 
     * removed.  The range is given in terms of what the view last
     * saw (that is, before updating sticky positions).
     *
     * @param e the document event
     */
    public void removeUpdate(DocumentEvent e) {
    	if (log.isDebugEnabled()) {
    		log.debug("removeUpdate():");
    	}
    	if (_currentEditor != null 
        		&& _currentEditor.getDocument() == e.getDocument()) {
    		setDocumentDirty(true);
    		setFormatInfo(_currentEditor);
    	}
    }

    /**
     * Gives notification that an attribute or set of attributes changed.
     *
     * @param e the document event
     */
    public void changedUpdate(DocumentEvent e) {
    	if (log.isDebugEnabled()) {
    		log.debug("changedUpdate():");
    	}
    	if (_currentEditor != null 
    		&& _currentEditor.getDocument() == e.getDocument()) {
    		setDocumentDirty(true);
    		setFormatInfo(_currentEditor);
    	}
	}

	//====================================
	//InternalFrameListener Implementation
	//====================================

    public void internalFrameClosed(InternalFrameEvent e) {
    	if (log.isDebugEnabled()) {
    		log.debug("internalFrameClosed():");
    	}
    	JInternalFrame iframe = e.getInternalFrame();
    	JEditorPane editor = SwingUtil.getJEditorPane(iframe);
    	if (editor != null && _dirtyTable.containsKey(editor)) {
    		_dirtyTable.remove(editor);
    	}
    }

	//====================================
	//CaretListener Implementation
	//====================================

    public void caretUpdate(CaretEvent e) {
    	if (_currentEditor == (JEditorPane) e.getSource()) {
    		setFormatInfo(_currentEditor);
    	}
    }

}// ToolBarStates class



















