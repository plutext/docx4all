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

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.FlavorEvent;
import java.awt.datatransfer.FlavorListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Hashtable;

import javax.swing.JEditorPane;
import javax.swing.JInternalFrame;
import javax.swing.SwingUtilities;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.text.AttributeSet;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;

import net.sf.vfsjfilechooser.utils.VFSUtils;

import org.apache.log4j.Logger;
import org.docx4all.datatransfer.WordMLTransferable;
import org.docx4all.swing.WordMLTextPane;
import org.docx4all.swing.event.InputAttributeEvent;
import org.docx4all.swing.event.InputAttributeListener;
import org.docx4all.swing.text.DocumentElement;
import org.docx4all.swing.text.StyleSheet;
import org.docx4all.swing.text.WordMLDocument;
import org.docx4all.swing.text.WordMLStyleConstants;

/**
 *	@author Jojada Tirtowidjojo - 30/11/2007
 */
public class ToolBarStates extends InternalFrameAdapter 
	implements FocusListener, DocumentListener, InputAttributeListener, FlavorListener, CaretListener {
	
	private static Logger log = Logger.getLogger(ToolBarStates.class);
	
	/**
	 * The binding key used for this ToolBarStates object 
	 * when passed into scripting environment
	 */
	public final static String SCRIPT_BINDING_KEY = "toolBarStates:org.docx4all.ui.main.ToolBarStates";
	
	public final static String DOC_DIRTY_PROPERTY_NAME = "documentDirty";
	public final static String ANY_DOC_DIRTY_PROPERTY_NAME = "anyDocumentDirty";
	
	public final static String CURRENT_EDITOR_PROPERTY_NAME = "currentEditor";
	
	public final static String CUT_ENABLED_PROPERTY_NAME = "cutEnabled";
	public final static String COPY_ENABLED_PROPERTY_NAME = "copyEnabled";
	public final static String PASTE_ENABLED_PROPERTY_NAME = "pastedEnabled";
	
	public final static String FONT_FAMILY_PROPERTY_NAME = "fontFamily";
	public final static String FONT_SIZE_PROPERTY_NAME = "fontSize";
	public final static String FONT_BOLD_PROPERTY_NAME = "fontBold";
	public final static String FONT_ITALIC_PROPERTY_NAME = "fontItalic";
	public final static String FONT_UNDERLINED_PROPERTY_NAME = "fontUnderlined";
	
	public final static String IFRAME_NUMBERS_PROPERTY_NAME = "iframeNumbers";
	
	public final static String ALIGNMENT_PROPERTY_NAME = "alignment";
	
	public final static String STYLE_SHEET_PROPERTY_NAME = "styleSheet";
	public final static String SELECTED_STYLE_PROPERTY_NAME = "selectedStyle";
	
	private final Hashtable<JInternalFrame, Boolean> _dirtyTable;
	private volatile JEditorPane _currentEditor;
	private volatile String _fontFamily;
	private volatile int _fontSize;
	private volatile boolean _fontBold, _fontItalic, _fontUnderlined;
	private volatile boolean _isCutEnabled, _isCopyEnabled, _isPasteEnabled;
	
	private volatile int _iframeNumbers;
	
	private volatile String _selectedStyle;
	private volatile int _alignment;
	
	private volatile StyleSheet _styleSheet;
	
	private PropertyChangeSupport changeSupport = null;
	
	public ToolBarStates() {
		_dirtyTable = new Hashtable<JInternalFrame, Boolean>(5);
		_fontBold = false;
		_fontItalic = false;
		_fontUnderlined = false;
		_isCutEnabled = false;
		_isCopyEnabled = false;
		_isPasteEnabled = false;		
		_iframeNumbers = 0;
		_alignment = -1;
	}
	
	public boolean isDocumentDirty() {
		return isDocumentDirty(_currentEditor);
	}
	
	public boolean isDocumentDirty(JEditorPane editor) {
		return isDocumentDirty(getInternalFrame(editor));
	}
	
	public boolean isDocumentDirty(JInternalFrame iframe) {
		Boolean isDirty = Boolean.FALSE;
		if (iframe != null) {
			isDirty = _dirtyTable.get(iframe);
			if (isDirty == null) {
				isDirty = Boolean.FALSE;
			}
		}
		return isDirty.booleanValue();
	}
	
	public boolean isAnyDocumentDirty() {
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
		setDocumentDirty(getInternalFrame(editor), dirty);
	}
	
	public void setDocumentDirty(JInternalFrame iframe, boolean dirty) {
		if (iframe == null) {
			return;
		}
		
		if (log.isDebugEnabled()) {
			String file = 
				(String) iframe.getClientProperty(
					WordMLDocument.FILE_PATH_PROPERTY);
			log.debug("setDocumentDirty(): File = " 
				+ VFSUtils.getFriendlyName(file)
				+ " - 'dirty' parameter = " + dirty);
		}
		
		//record current isAnyDocumentDirty() state for later use
		Boolean oldAllDirty = Boolean.valueOf(isAnyDocumentDirty());
		
		Boolean isDirty = _dirtyTable.get(iframe);
		if (isDirty == null) {
			isDirty = Boolean.FALSE;
		}
		
		Boolean newDirty = Boolean.valueOf(dirty);
		if (isDirty == newDirty) {
			//no change
			return;
		}
		
		_dirtyTable.put(iframe, newDirty);
		
		if (iframe == getCurrentInternalFrame()) {
			firePropertyChange(DOC_DIRTY_PROPERTY_NAME, Boolean.valueOf(isDirty), newDirty);
		}

		//Check the resulting isAnyDocumentDirty() state
		newDirty = Boolean.valueOf(isAnyDocumentDirty());
		if (oldAllDirty == newDirty) {
			//no change
			return;
		}
		firePropertyChange(ANY_DOC_DIRTY_PROPERTY_NAME, oldAllDirty, newDirty);
	}
	
	public String getSelectedStyle() {
		return _selectedStyle;
	}
	
	public void setSelectedStyle(String style) {
		if (log.isDebugEnabled()) {
			log.debug("setSelectedStyle(): _selectedStyle = " + _selectedStyle + " selectedStyle param = " + style);
		}
	
		if (_selectedStyle == style
			|| (_selectedStyle != null && _selectedStyle.equals(style))) {
			return;
		}
		
		String oldValue = _selectedStyle;
		_selectedStyle = style;
		firePropertyChange(SELECTED_STYLE_PROPERTY_NAME, oldValue, style);
	}
	
	public StyleSheet getStyleSheet() {
		return _styleSheet;
	}
	
	public void setStyleSheet(StyleSheet styleSheet) {
		if (log.isDebugEnabled()) {
			String s1 = (_styleSheet == null) ? "NULL" : "StyleSheet@" + _styleSheet.hashCode();
			String s2 = (styleSheet == null) ? "NULL" : "StyleSheet@" + styleSheet.hashCode();
			
			log.debug("setStyleSheet(): _styleSheet = " + s1
				+ " styleSheet param = " + s2);
		}
	
		if (_styleSheet == styleSheet) {
			return;
		}
		
		StyleSheet oldValue = _styleSheet;
		_styleSheet = styleSheet;
		firePropertyChange(STYLE_SHEET_PROPERTY_NAME, oldValue, styleSheet);
	}
	
	public boolean isCutEnabled() {
		return _isCutEnabled;
	}
	
	public void setCutEnabled(boolean enabled) {
		if (log.isDebugEnabled()) {
			log.debug("setCutEnabled(): _isCutEnabled = " + _isCutEnabled + " enabled param = " + enabled);
		}
	
		if (_isCutEnabled == enabled) {
			return;
		}
		
		boolean oldValue = _isCutEnabled;
		_isCutEnabled = enabled;
		
		firePropertyChange(
			CUT_ENABLED_PROPERTY_NAME, 
			Boolean.valueOf(oldValue), 
			Boolean.valueOf(enabled));
	}
	
	public boolean isCopyEnabled() {
		return _isCopyEnabled;
	}
	
	public void setCopyEnabled(boolean enabled) {
		if (log.isDebugEnabled()) {
			log.debug("setCopyEnabled(): _isCopyEnabled = " + _isCopyEnabled + " enabled param = " + enabled);
		}
	
		if (_isCopyEnabled == enabled) {
			return;
		}
		
		boolean oldValue = _isCopyEnabled;
		_isCopyEnabled = enabled;
		
		firePropertyChange(
			COPY_ENABLED_PROPERTY_NAME, 
			Boolean.valueOf(oldValue), 
			Boolean.valueOf(enabled));
	}
	
	public boolean isPasteEnabled() {
		return _isPasteEnabled;
	}
	
	public void setPasteEnabled(boolean enabled) {
		if (log.isDebugEnabled()) {
			log.debug("setPasteEnabled(): _isPasteEnabled = " + _isPasteEnabled + " enabled param = " + enabled);
		}
	
		if (_isPasteEnabled == enabled) {
			return;
		}
		
		boolean oldValue = _isPasteEnabled;
		_isPasteEnabled = enabled;
		
		firePropertyChange(
			PASTE_ENABLED_PROPERTY_NAME, 
			Boolean.valueOf(oldValue), 
			Boolean.valueOf(enabled));
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
	
	public JInternalFrame getCurrentInternalFrame() {
		return getInternalFrame(_currentEditor);
	}
	
	private void setCurrentEditor(JEditorPane editor) {
		if (editor == _currentEditor) {
			return;
		}
		
		//a kind of reseting first
    	setFormatInfo(null, null, SimpleAttributeSet.EMPTY);

		JInternalFrame iframe = getInternalFrame(editor);
		Boolean newDirty = _dirtyTable.get(iframe);
		if (newDirty == null) {
			newDirty = Boolean.FALSE;
		}
    	
		Boolean currentDirty = isDocumentDirty();
		
		// Before calling setDocumentDirty(iframe, newDirty) below
		// we need to satisfy its precondition first;
		// ie: both _currentEditor and _dirtyTable
		// have to be current and valid.
		JEditorPane oldEditor = _currentEditor;
		_currentEditor = editor;
		firePropertyChange(CURRENT_EDITOR_PROPERTY_NAME, oldEditor, editor);

		_dirtyTable.put(iframe, currentDirty);
		setDocumentDirty(iframe, newDirty);
		
    	setFormatInfo(editor);
    	
    	boolean hasSelection = 
    		(editor.getSelectionStart() < editor.getSelectionEnd());
    	setCopyEnabled(hasSelection);
    	setCutEnabled(hasSelection);
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
    	if (!(editor instanceof WordMLTextPane)) {
    		return;
    	}
    	
    	WordMLDocument doc = (WordMLDocument) editor.getDocument();
    	
    	MutableAttributeSet inputAttrs = 
    		(MutableAttributeSet)
    		((WordMLTextPane) editor).getInputAttributesML();
    	if (inputAttrs.isDefined(WordMLStyleConstants.RStyleAttribute)) {
    		//user has selected a style
    		String styleName =
    			(String) inputAttrs.getAttribute(WordMLStyleConstants.RStyleAttribute);
        	if (styleName != null) {
        		Style rStyle = doc.getStyleSheet().getReferredStyle(styleName);
        		setFormatInfo(doc.getStyleSheet(), styleName, rStyle);
        		return;
        	}
    	}
    	
    	DocumentElement elem = (DocumentElement) inputAttrs.getResolveParent();
    	if (elem != null) {
        	setFormatInfo(doc.getStyleSheet(), elem.getStyleNameInAction(), inputAttrs);
    	} else {
    		int pos = editor.getCaretPosition();
    		elem = (DocumentElement) doc.getParagraphMLElement(pos, false);
    		
    		String styleName = elem.getStyleNameInAction();
       		Style pStyle = doc.getStyleSheet().getReferredStyle(styleName);
       		setFormatInfo(doc.getStyleSheet(), styleName, pStyle);
    	}
    }
    
    private void setDefaultFormatInfo() {
    	log.info("");
    	StyleSheet styleSheet = StyleSheet.getDefaultStyleSheet();
    	
		Style style = styleSheet.getStyle(StyleSheet.DEFAULT_STYLE);
		String styleName = 
			(String) style.getAttribute(
						WordMLStyleConstants.DefaultParagraphStyleNameAttribute);
    	style = styleSheet.getReferredStyle(styleName);
		
    	setFormatInfo(styleSheet, styleName, style);
    }
    
    private void setFormatInfo(
    	StyleSheet styleSheet, String selectedStyleName, AttributeSet attrs) {
    	setStyleSheet(styleSheet);
    	
    	setFontFamily(StyleConstants.getFontFamily(attrs));
    	//font size in OpenXML is in half points, not points,
    	//so we need to divide by 2
	    setFontSize(StyleConstants.getFontSize(attrs)/2);
	    setFontBold(StyleConstants.isBold(attrs));
	    setFontItalic(StyleConstants.isItalic(attrs));
	    setFontUnderlined(StyleConstants.isUnderline(attrs));
	    
	    setSelectedStyle(selectedStyleName);
	    setAlignment(StyleConstants.getAlignment(attrs));
    }
    
    private JInternalFrame getInternalFrame(JEditorPane editor) {
		return (JInternalFrame) SwingUtilities.getAncestorOfClass(
				JInternalFrame.class, editor);
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
    	setCurrentEditor((JEditorPane) e.getSource());
    }

    /**
	 * Invoked when a JEditorPane loses the keyboard focus.
	 */
    public void focusLost(FocusEvent e) {
    	if (log.isDebugEnabled()) {
    		log.debug("focusLost():");
    	}
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
    		_currentEditor.putClientProperty(Constants.SYNCHRONIZED_FLAG, Boolean.FALSE);
    		setDocumentDirty(_currentEditor, true);
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
    		_currentEditor.putClientProperty(Constants.SYNCHRONIZED_FLAG, Boolean.FALSE);
    		setDocumentDirty(_currentEditor, true);
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
    		_currentEditor.putClientProperty(Constants.SYNCHRONIZED_FLAG, Boolean.FALSE);
    		setDocumentDirty(_currentEditor, true);
    	}
	}

	//====================================
	//InternalFrameListener Implementation
	//====================================

    public void internalFrameOpened(InternalFrameEvent e) {
    	if (log.isDebugEnabled()) {
    		log.debug("internalFrameClosed():");
    	}
    	
    	Integer oldNumbers = new Integer(_iframeNumbers);
    	Integer newNumbers = new Integer(++_iframeNumbers);
    	firePropertyChange(IFRAME_NUMBERS_PROPERTY_NAME, oldNumbers, newNumbers);
    }
    
    public void internalFrameClosed(InternalFrameEvent e) {
    	if (log.isDebugEnabled()) {
    		log.debug("internalFrameClosed():");
    	}
    	JInternalFrame iframe = e.getInternalFrame();
    	setDocumentDirty(iframe, false);
		_dirtyTable.remove(iframe);
    	
    	Integer oldNumbers = new Integer(_iframeNumbers);
    	Integer newNumbers = new Integer(--_iframeNumbers);
    	firePropertyChange(IFRAME_NUMBERS_PROPERTY_NAME, oldNumbers, newNumbers);
    	
    	if (_iframeNumbers == 0) {
    		//a kind of reseting first
        	setFormatInfo(null, null, SimpleAttributeSet.EMPTY);
        	//Bring up default setting
    		setDefaultFormatInfo();
    	}
    }

	//=====================================
	//InputAttributeListener Implementation
	//=====================================
    
    public void inputAttributeChanged(InputAttributeEvent e) {
    	if (_currentEditor == (JEditorPane) e.getSource()) {
    		setFormatInfo(_currentEditor);
    	}
    }

	//=====================================
	//FlavorListener Implementation
	//=====================================
	public void flavorsChanged(FlavorEvent e) {
		Clipboard clipboard = (Clipboard) e.getSource();
		boolean available = 
			clipboard.isDataFlavorAvailable(WordMLTransferable.STRING_FLAVOR)
			|| clipboard.isDataFlavorAvailable(WordMLTransferable.WORDML_FRAGMENT_FLAVOR);

		//This flavorsChanged() method is fired IF AND ONLY IF there is a DataFlavor change
		//in Clipboard. Therefore, make sure that _isPasteEnable property is initialised correctly.
		//See: WordMLEditor.startup() method where _isPasteEnable property is initialised. 
		setPasteEnabled(available);
	}
	
	//=====================================
	//CaretListener Implementation
	//=====================================
	public void caretUpdate(CaretEvent e) {
    	if (_currentEditor == e.getSource()) {
        	boolean hasSelection = 
        		(_currentEditor.getSelectionStart() < _currentEditor.getSelectionEnd());
        	setCopyEnabled(hasSelection);
        	setCutEnabled(hasSelection);
    	}
	}

}// ToolBarStates class



















