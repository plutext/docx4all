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

import java.awt.Font;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Element;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.xml.bind.JAXBIntrospector;

import org.apache.log4j.Logger;
import org.docx4all.swing.event.WordMLDocumentEvent;
import org.docx4all.swing.event.WordMLDocumentListener;
import org.docx4all.swing.text.WordMLFragment.ElementMLRecord;
import org.docx4all.ui.main.Constants;
import org.docx4all.util.DocUtil;
import org.docx4all.util.XmlUtil;
import org.docx4all.xml.DocumentML;
import org.docx4all.xml.ElementML;
import org.docx4all.xml.ElementMLFactory;
import org.docx4all.xml.ImpliedContainerML;
import org.docx4all.xml.ObjectFactory;
import org.docx4all.xml.ParagraphML;
import org.docx4all.xml.ParagraphPropertiesML;
import org.docx4all.xml.PropertiesContainerML;
import org.docx4all.xml.RunContentML;
import org.docx4all.xml.RunML;
import org.docx4all.xml.RunPropertiesML;
import org.docx4all.xml.SdtBlockML;
import org.docx4j.XmlUtils;
import org.docx4j.wml.SdtBlock;

public class WordMLDocument extends DefaultStyledDocument {
	private static Logger log = Logger.getLogger(WordMLDocument.class);

	public final static String FILE_PATH_PROPERTY = "filePathProperty";
	
	public final static String WML_PACKAGE_PROPERTY = "wmlPackageProperty";
	
	public boolean snapshotFireBan;
	
	public WordMLDocument() {
		super();
		this.snapshotFireBan = false;
	}

	public synchronized final void lockWrite() {
		writeLock();
	}
	
	public synchronized final void unlockWrite() {
		writeUnlock();
	}
	
	public synchronized boolean isSnapshotFireBan() {
		return snapshotFireBan;
	}
	
	public synchronized void setSnapshotFireBan(boolean b) {
		snapshotFireBan = b;
	}
	
	public StyleSheet getStyleSheet() {
		DocumentElement root = (DocumentElement) getDefaultRootElement();
		return root.getElementML().getStyleSheet();
	}
	
    public Font getFont(AttributeSet attr) {
    	return FontManager.getInstance().getFontInAction(attr);
    }

	public Element getParagraphMLElement(int pos, boolean impliedParagraph) {
		Element elem = getRunMLElement(pos);
		if (elem != null) {
			elem = elem.getParentElement();
			if (!impliedParagraph) {
				elem = elem.getParentElement();
			}
		}
		return elem;
	}
	
	public Element getRunMLElement(int pos) {
		Element elem = getCharacterElement(pos);
		if (elem != null) {
			elem = elem.getParentElement();
		}
		return elem;
	}

    public void setRunMLAttributes(
    	int offset, int length, AttributeSet attrs,	boolean replace) 
    	throws BadLocationException {
    	
    	if (offset >= getLength()
    		|| length == 0 
    		|| attrs == null 
    		|| attrs.getAttributeCount() == 0) {
    		return;
    	}
    	
    	length = Math.min(getLength() - offset, length);
    	
		Map<BigInteger, SdtBlock> snapshots = null;
		int blockStart = -1;
		int blockEnd = -1;
		
		try {
			writeLock();

			if (!isSnapshotFireBan()) {
				DocumentElement rootE = (DocumentElement) getDefaultRootElement();
				
				int idx = rootE.getElementIndex(offset);
				DocumentElement elem = (DocumentElement) rootE.getElement(idx);
				//blockStart keeps the distance of elem's start position 
				//from the first character in document (offset == 0)
				blockStart = elem.getStartOffset();
				
				idx = (length == 0) ? idx : rootE.getElementIndex(offset + length - 1);
				elem = (DocumentElement) rootE.getElement(idx);
				//blockEnd keeps the distance of elem's end position
				//from the last character in document (offset == getLength())
				blockEnd = Math.max(0, getLength() - elem.getEndOffset());

				snapshots = getSnapshots(blockStart, (getLength() - blockEnd) - blockStart);				
			}
			
			int lastEnd = Integer.MAX_VALUE;
			for (int pos = offset; pos < (offset + length); pos = lastEnd) {
				DocumentElement runE = (DocumentElement) getRunMLElement(pos);
				RunML runML = (RunML) runE.getElementML();
				
				if (offset <= runE.getStartOffset()
					&& runE.getEndOffset() <= offset + length) {
					runML.addAttributes(attrs, replace);
					
				} else if (runE.getStartOffset() < offset
							&& offset + length < runE.getEndOffset()) {
					try {
						//Firstly, make a copy of RunML that spans from
						//(offset+length) to runE.getEndOffset().
						int tempInt = runE.getEndOffset() - (offset + length);
						TextSelector ts = 
							new TextSelector(this, (offset+length), tempInt);
						//Because [offset + length, tempInt] is inside runE,
						//ts will definitely contain a single record whose
						//ElementML is a RunML.
						ElementML ml = ts.getElementMLRecords().get(0).getElementML();
						//Put this copy to the right of runML
						runML.addSibling(ml, true);
						
						//Secondly, make a copy of RunML that spans from
						//offset to offset + length and apply 'attrs' to it
						ts = new TextSelector(this, offset, length);
						ml = ts.getElementMLRecords().get(0).getElementML();
						((RunML) ml).addAttributes(attrs, replace);
						//This copy has to be at the right of runML
						runML.addSibling(ml, true);
						
						//Finally, chop runE from runE.getStartOffset()
						//to offset position.
						tempInt = offset - runE.getStartOffset();
						RunML newSibling = 
							(RunML) DocUtil.splitElementML(runE, tempInt);
						newSibling.delete();
					} catch (BadSelectionException exc) {
						;//ignore
					}
					
				} else if (runE.getStartOffset() < offset) {
					int idx = offset - runE.getStartOffset();
					RunML newSibling = 
						(RunML) DocUtil.splitElementML(runE, idx);
					newSibling.addAttributes(attrs, replace);
					
				} else {
					//ie: offset + length < runE.getEndOffset()
					int idx = (offset + length) - runE.getStartOffset();
					DocUtil.splitElementML(runE, idx);
					runML.addAttributes(attrs, replace);
				}
				
				lastEnd = runE.getEndOffset();
				if (pos == lastEnd) {
					//finish
					break;
				}
			}

			refreshParagraphs(offset, length);
			
		} finally {
			if (!isSnapshotFireBan() && blockStart >= 0 && blockEnd >= 0) {
				WordMLDocument.WordMLDefaultDocumentEvent evt = 
					new WordMLDefaultDocumentEvent(
							blockStart,
							(getLength() - blockEnd) - blockStart,
							null,
							WordMLDocumentEvent.SNAPSHOT_CHANGED_EVT_NAME);
				evt.setInitialSnapshots(snapshots);
				fireSnapshotChanged(evt);
			}

			writeUnlock();
		}
	}
    
    public void setParagraphMLAttributes(
    	int offset, 
    	int length,
		AttributeSet attrs, 
		boolean replace) 
    	throws BadLocationException {

		if (offset > getLength() || attrs == null
				|| attrs.getAttributeCount() == 0) {
			return;
		}

		length = Math.min(getLength() - offset, length);

		Map<BigInteger, SdtBlock> snapshots = null;
		int blockStart = -1;
		int blockEnd = -1;
		
		try {
			writeLock();
			
			if (!isSnapshotFireBan()) {
				DocumentElement rootE = (DocumentElement) getDefaultRootElement();
				
				int idx = rootE.getElementIndex(offset);
				DocumentElement elem = (DocumentElement) rootE.getElement(idx);
				//blockStart keeps the distance of elem's start position 
				//from the first character in document (offset == 0)
				blockStart = elem.getStartOffset();
				
				idx = (length == 0) ? idx : rootE.getElementIndex(offset + length - 1);
				elem = (DocumentElement) rootE.getElement(idx);
				//blockEnd keeps the distance of elem's end position
				//from the last character in document (offset == getLength())
				blockEnd = Math.max(0, getLength() - elem.getEndOffset());

				snapshots = getSnapshots(blockStart, (getLength() - blockEnd) - blockStart);				
			}
			
			DefaultDocumentEvent changes = 
				new DefaultDocumentEvent(offset, length, DocumentEvent.EventType.CHANGE);

		    AttributeSet attrsCopy = attrs.copyAttributes();
		    
		    int pos = offset;
		    while (pos <= offset + length) {
		    	DocumentElement paraE = 
		    		(DocumentElement) getParagraphMLElement(pos, false);
				ParagraphML paraML = (ParagraphML) paraE.getElementML();
				paraML.addAttributes(attrsCopy, replace);
				
				MutableAttributeSet elemAttr = 
					(MutableAttributeSet) paraE.getAttributes();
				//changes.addEdit(
				//	new AttributeUndoableEdit(paraE, attrsCopy, replace));
				if (replace) {
					elemAttr.removeAttributes(elemAttr);
				}
				elemAttr.addAttributes(attrs);
				pos += (paraE.getEndOffset() - paraE.getStartOffset());
		    }
		    
			changes.end();
			fireChangedUpdate(changes);
			//fireUndoableEditUpdate(new UndoableEditEvent(this, changes));

		} finally {
			if (!isSnapshotFireBan() && blockStart >= 0 && blockEnd >=0) {
				WordMLDocument.WordMLDefaultDocumentEvent evt = 
					new WordMLDefaultDocumentEvent(
							blockStart,
							(getLength() - blockEnd) - blockStart,
							null,
							WordMLDocumentEvent.SNAPSHOT_CHANGED_EVT_NAME);
				evt.setInitialSnapshots(snapshots);
				fireSnapshotChanged(evt);
			}

			writeUnlock();
		}
	}
    
    public void setParagraphStyle(int offset, int length, String styleId) {
		if (offset > getLength() || styleId == null || styleId.length() == 0) {
			return;
		}

		length = Math.min(getLength() - offset, length);

		Map<BigInteger, SdtBlock> snapshots = null;
		int blockStart = -1;
		int blockEnd = -1;
		
		try {
			writeLock();
			
			Style style = getStyleSheet().getReferredStyle(styleId);
			String type = 
				(style == null) 
					? null 
					: (String) style.getAttribute(WordMLStyleConstants.StyleTypeAttribute);
			
			if (StyleSheet.PARAGRAPH_ATTR_VALUE.equals(type)) {
				if (!isSnapshotFireBan()) {
					DocumentElement rootE = (DocumentElement) getDefaultRootElement();
					
					int idx = rootE.getElementIndex(offset);
					DocumentElement elem = (DocumentElement) rootE.getElement(idx);
					//blockStart keeps the distance of elem's start position 
					//from the first character in document (offset == 0)
					blockStart = elem.getStartOffset();
					
					idx = (length == 0) ? idx : rootE.getElementIndex(offset + length - 1);
					elem = (DocumentElement) rootE.getElement(idx);
					//blockEnd keeps the distance of elem's end position
					//from the last character in document (offset == getLength())
					blockEnd = Math.max(0, getLength() - elem.getEndOffset());

					snapshots = getSnapshots(blockStart, (getLength() - blockEnd) - blockStart);				
				}
				
				MutableAttributeSet newAttrs = new SimpleAttributeSet();
				newAttrs.addAttribute(WordMLStyleConstants.PStyleAttribute, styleId);
				
				if (offset == getLength()) {
					insertString(offset, Constants.NEWLINE, newAttrs);
				}
				
				Element rootE = getDefaultRootElement();
				int start = rootE.getElementIndex(offset);
				int end = rootE.getElementIndex(offset
						+ ((length > 0) ? length - 1 : 0));
				
				for (int i = start; i <= end; i++) {
					DocumentElement paraE = (DocumentElement) rootE.getElement(i);
					ParagraphML paraML = (ParagraphML) paraE.getElementML();
					for (ElementML child: paraML.getChildren()) {
						//Clean up child's attributes
						PropertiesContainerML propML = 
							((RunML) child).getRunProperties();
						if (propML != null) {
							propML.removeAttributes(propML.getAttributeSet());
							propML.save();
						}
					}
					paraML.addAttributes(newAttrs, true);
				}
				
				start = rootE.getElement(start).getStartOffset();
				end = rootE.getElement(end).getEndOffset();
				refreshParagraphs(start, end - start);
				// fireUndoableEditUpdate(new UndoableEditEvent(this, changes));
				
			} //if (StyleSheet.PARAGRAPH_ATTR_VALUE.equals(type))
			
		} catch (BadLocationException exc) {
			exc.printStackTrace();//ignore
			
		} finally {
			if (!isSnapshotFireBan() && blockStart >= 0 && blockEnd >= 0) {
				WordMLDocument.WordMLDefaultDocumentEvent evt = 
					new WordMLDefaultDocumentEvent(
							blockStart,
							(getLength() - blockEnd) - blockStart,
							null,
							WordMLDocumentEvent.SNAPSHOT_CHANGED_EVT_NAME);
				evt.setInitialSnapshots(snapshots);
				fireSnapshotChanged(evt);
			}

			writeUnlock();
		}
    }
    
    public void setRunStyle(int offset, int length, String styleId) {
		if (offset >= getLength() || length == 0 || styleId == null
				|| styleId.length() == 0) {
			return;
		}

		length = Math.min(getLength() - offset, length);

		Map<BigInteger, SdtBlock> snapshots = null;
		int blockStart = -1;
		int blockEnd = -1;
		
		try {
			writeLock();

			Style style = getStyleSheet().getReferredStyle(styleId);
			String type = (style == null) ? null : (String) style
					.getAttribute(WordMLStyleConstants.StyleTypeAttribute);
			if (StyleSheet.CHARACTER_ATTR_VALUE.equals(type)) {
				
				if (!isSnapshotFireBan()) {
					DocumentElement rootE = (DocumentElement) getDefaultRootElement();
					
					int idx = rootE.getElementIndex(offset);
					DocumentElement elem = (DocumentElement) rootE.getElement(idx);
					//blockStart keeps the distance of elem's start position 
					//from the first character in document (offset == 0)
					blockStart = elem.getStartOffset();
					
					idx = (length == 0) ? idx : rootE.getElementIndex(offset + length - 1);
					elem = (DocumentElement) rootE.getElement(idx);
					//blockEnd keeps the distance of elem's end position
					//from the last character in document (offset == getLength())
					blockEnd = Math.max(0, getLength() - elem.getEndOffset());

					snapshots = getSnapshots(blockStart, (getLength() - blockEnd) - blockStart);				
				}
				
				MutableAttributeSet newAttrs = new SimpleAttributeSet();
				newAttrs.addAttribute(WordMLStyleConstants.RStyleAttribute, styleId);
				
				int lastEnd = Integer.MAX_VALUE;
				for (int pos = offset; pos < (offset + length); pos = lastEnd) {
					DocumentElement runE = (DocumentElement) getRunMLElement(pos);
					RunML runML = (RunML) runE.getElementML();

					if (offset <= runE.getStartOffset()
							&& runE.getEndOffset() <= offset + length) {
						runML.addAttributes(newAttrs, true);

					} else if (runE.getStartOffset() < offset
							&& offset + length < runE.getEndOffset()) {
						try {
							// Firstly, make a copy of RunML that spans from
							// (offset+length) to runE.getEndOffset().
							int tempInt = runE.getEndOffset()
									- (offset + length);
							TextSelector ts = new TextSelector(this,
									(offset + length), tempInt);
							// Because [offset + length, tempInt] is inside
							// runE,
							// ts will definitely contain a single record whose
							// ElementML is a RunML.
							ElementML ml = ts.getElementMLRecords().get(0)
									.getElementML();
							// Put this copy to the right of runML
							runML.addSibling(ml, true);

							// Secondly, make a copy of RunML that spans from
							// offset to offset + length and apply 'attrs' to it
							ts = new TextSelector(this, offset, length);
							ml = ts.getElementMLRecords().get(0).getElementML();
							((RunML) ml).addAttributes(newAttrs, true);
							// This copy has to be at the right of runML
							runML.addSibling(ml, true);

							// Finally, chop runE from runE.getStartOffset()
							// to offset position.
							tempInt = offset - runE.getStartOffset();
							RunML newSibling = (RunML) DocUtil.splitElementML(
									runE, tempInt);
							newSibling.delete();
						} catch (BadSelectionException exc) {
							;// ignore
						}

					} else if (runE.getStartOffset() < offset) {
						int idx = offset - runE.getStartOffset();
						RunML newSibling = (RunML) DocUtil.splitElementML(runE,
								idx);
						newSibling.addAttributes(newAttrs, true);

					} else {
						// ie: offset + length < runE.getEndOffset()
						int idx = (offset + length) - runE.getStartOffset();
						DocUtil.splitElementML(runE, idx);
						runML.addAttributes(newAttrs, true);
					}

					lastEnd = runE.getEndOffset();
					if (pos == lastEnd) {
						// finish
						break;
					}
				}

				refreshParagraphs(offset, length);
				
			} // if (StyleSheet.CHARACTER_ATTR_VALUE.equals(type))
		} finally {
			if (!isSnapshotFireBan() && blockStart >= 0 && blockEnd >= 0) {
				WordMLDocument.WordMLDefaultDocumentEvent evt = 
					new WordMLDefaultDocumentEvent(
							blockStart,
							(getLength() - blockEnd) - blockStart,
							null,
							WordMLDocumentEvent.SNAPSHOT_CHANGED_EVT_NAME);
				evt.setInitialSnapshots(snapshots);
				fireSnapshotChanged(evt);
			}

			writeUnlock();
		}
	}
    
	public void insertFragment(int offset, WordMLFragment fragment, AttributeSet attrs) 
		throws BadLocationException {
		
		List<ElementMLRecord> paraContentRecords = null;
		List<ElementMLRecord> paragraphRecords = null;
		if (fragment != null) {
			paraContentRecords = fragment.getParagraphContentRecords();
			paragraphRecords = fragment.getParagraphRecords();
		}		
						
		if (paraContentRecords == null && paragraphRecords == null) {
			if (log.isDebugEnabled()) {
				log.debug("insertFragment(): offset=" + offset
					+ " fragment's records = NULL");
			}
			return;
		}
		
		if (log.isDebugEnabled()) {
			int i=0;
			if (paraContentRecords != null) {
				for (ElementMLRecord rec : paraContentRecords) {
					List<ElementSpec> list =
						DocUtil.getElementSpecs(rec.getElementML());
					log.debug("insertFragment(): records[" 
							+ (i++)
							+ "].isFragmented = " + rec.isFragmented());
					DocUtil.displayStructure(list);
				}
			}
			
			if (paragraphRecords != null) {
				for (ElementMLRecord rec : paragraphRecords) {
					List<ElementSpec> list =
						DocUtil.getElementSpecs(rec.getElementML());
					log.debug("insertFragment(): records[" 
							+ (i++)
							+ "].isFragmented = " + rec.isFragmented());
					DocUtil.displayStructure(list);
				}
			}
		}
		
		if (paragraphRecords == null && canbePastedAsString(paraContentRecords)) {
			insertString(offset, fragment.getText(), attrs);
			return;
		}
		
		//Preparing initial snapshots
		Map<BigInteger, SdtBlock> snapshots = null;
		int blockStart = -1;
		int blockEnd = -1;
		
		try {
			writeLock();
			
			DocumentElement rootE = 
				(DocumentElement) getDefaultRootElement();
			
			DocumentElement textE = 
				(DocumentElement) getCharacterElement(Math.max(offset - 1, 0));
			if (0 < offset
					&& offset < textE.getEndOffset()
					&& (!textE.isEditable() 
						|| (paraContentRecords == null && paragraphRecords != null))) {
				throw new BadLocationException("Cannot insert here", offset);
			}
			
			if (!isSnapshotFireBan()) {
				int idx = rootE.getElementIndex(offset);
				DocumentElement elem = (DocumentElement) rootE.getElement(idx);
				//blockStart keeps the distance of elem's start position 
				//from the first character in document (offset == 0)
				blockStart = elem.getStartOffset();
				//blockEnd keeps the distance of elem's end position
				//from the last character in document (offset == getLength())
				blockEnd = Math.max(0, getLength() - elem.getEndOffset());

				snapshots = getSnapshots(blockStart, (getLength() - blockEnd) - blockStart);				
			}
			
			DocumentElement targetE = (DocumentElement) getParagraphMLElement(offset, false);
			if (targetE.getEndOffset() == rootE.getEndOffset()) {
				insertAtLastParagraph(paraContentRecords, paragraphRecords);
				
			} else if (paraContentRecords != null && paragraphRecords == null) {
				//Note that textE is a leaf/character element at Math.max(offset-1,0)
				DocumentElement runE = (DocumentElement) textE.getParentElement();
				
				if (runE.getEndOffset() == offset) {
					DocumentElement impliedParaE = 
						(DocumentElement) runE.getParentElement();
					if (impliedParaE.getEndOffset() == offset) {
						// paste at the start of a paragraph
						ElementML newlineRunML = runE.getElementML();

						runE = (DocumentElement) getRunMLElement(offset);
						ElementML runML = runE.getElementML();
						if (newlineRunML == runML && runML.isImplied()) {
							// Paste at an empty paragraph.
							// This may happen when offset == 0.
							targetE = (DocumentElement) 
								runE.getParentElement().getParentElement();
							ElementML targetML = targetE.getElementML();
							for (ElementMLRecord rec: paraContentRecords) {
								targetML.addChild(rec.getElementML());
							}
							
						} else if (newlineRunML == runML) {
							// Paste after a soft break
							RunContentML softBreak = 
								(RunContentML) textE.getElementML();
							//Split soft break before pasting.
							List<ElementML> runContents = 
								new ArrayList<ElementML>(runE.getElementCount());
							for (int i = 0; i < runE.getElementCount(); i++) {
								DocumentElement tempE = 
									(DocumentElement) runE.getElement(i);
								ElementML ml = tempE.getElementML();
								ml.delete();
								runContents.add(ml);
							}

							RunPropertiesML rPr = 
								(RunPropertiesML) 
									((RunML) runML).getRunProperties();
							if (rPr != null) {
								rPr = (RunPropertiesML) rPr.clone();
							}
							RunML newSibling = ElementMLFactory.createRunML(
									runContents, rPr);
							runML.addSibling(newSibling, true);

							pasteRecordsAfter(softBreak, paraContentRecords);

						} else {
							// Paste after a common break.
							// This is to paste before 'runML' which is
							// the RunML at 'offset'
							pasteRecordsBefore((RunML) runML, paraContentRecords);
						}
					} else {
						// paste at somewhere inside a paragraph
						pasteRecordsAfter(
							(RunContentML) textE.getElementML(), paraContentRecords);
					}
				} else if (runE.getStartOffset() == offset) {
					//paste at the start of runE.
					//This should only happen when offset is 0 (zero).
					//It is because runE is the parent of textE and
					//textE is a text element at Math.max(offset - 1, 0).
					pasteRecordsBefore((RunML) runE.getElementML(), paraContentRecords);
					
				} else {
					//paste at somewhere inside runE.
					//This necessitates splitting runE.
					DocUtil.splitElementML(runE, offset	- runE.getStartOffset());
					pasteRecordsAfter((RunContentML) textE.getElementML(), paraContentRecords);
				}

			} else if (paraContentRecords != null && paragraphRecords != null) {
				//targetE is ParagraphML element at 'offset' position
				if (targetE.getStartOffset() == offset) {
					//Whether fragment can be pasted at 'offset' position or not
					//depends on whether the paragraphRecords can be pasted.
					//Because paragraphRecords are siblings checking for the last 
					//record is enough.
					ElementMLRecord rec = paragraphRecords.get(paragraphRecords.size() - 1);
					targetE = getElementToPasteAt(targetE, rec, false);
					if (targetE == null) {
						//Cannot paste
						throw new BadLocationException("Cannot insert here", offset);
					}
					
					//Prepare a new ParagraphML to accommodate paraContentRecords
					ParagraphPropertiesML pPr = (ParagraphPropertiesML) 
						((ParagraphML) targetE.getElementML()).getParagraphProperties();
					if (pPr != null) {
						pPr = (ParagraphPropertiesML) pPr.clone();
					}
					List<ElementML> contents = 
						new ArrayList<ElementML>(paraContentRecords.size());
					for (ElementMLRecord temp : paraContentRecords) {
						contents.add(temp.getElementML());
					}
					ElementML newParaML = 
						ElementMLFactory.createParagraphML(contents, pPr, null);
				
					ElementML targetML = targetE.getElementML();
					if (targetML instanceof ParagraphML) {
						targetML.addSibling(newParaML, false);
					} else if (targetML instanceof SdtBlockML) {
						SdtBlockML newSdtBlockML = ElementMLFactory.createSdtBlockML();
						newSdtBlockML.addChild(newParaML, true);
						targetML.addSibling(newSdtBlockML, false);
					} else {
						//Do not know how to accommodate newParaML 
						//that contains paraContentRecords.
						//Bail out.
						throw new BadLocationException("Cannot insert here", offset);
					}
					
					for (int i=0; i < paragraphRecords.size(); i++) {
						rec = paragraphRecords.get(i);
						ElementML ml = rec.getElementML();
						targetML.addSibling(ml, false);
					}						

					//Currently we limit the following merging feature
					//to ParagraphML elements only.
					if (rec.isFragmented()
						&& rec.getElementML() instanceof ParagraphML
						&& targetML instanceof ParagraphML) {
						// Join the last fragmented record
						// with the content of 'targetML'
						contents = XmlUtil.deleteChildren(targetML);
						targetML.delete();
						RunContentML rcml = 
							XmlUtil.getLastRunContentML(rec.getElementML());
						pasteElementMLsAfter(rcml, contents);
					}
				} else {
					//Pasting fragment here requires splitting the content of targetE
					splitParagraphMLAndPaste(offset, paraContentRecords, paragraphRecords);
				}

			} else if (paraContentRecords == null && paragraphRecords != null) {
				//targetE is ParagraphML element at 'offset' position.
				if (targetE.getStartOffset() == offset) {
					//Check whether paragraphRecords can be pasted.
					//Because paragraphRecords are siblings checking for the last 
					//record is enough.
					ElementMLRecord rec = paragraphRecords.get(paragraphRecords.size() - 1);
					targetE = getElementToPasteAt(targetE, rec, false);
					if (targetE == null) {
						//Cannot paste
						throw new BadLocationException("Cannot insert here", offset);
					}
					
					for (int i=0; i < paragraphRecords.size(); i++) {
						rec = paragraphRecords.get(i);
						ElementML ml = rec.getElementML();
						targetE.getElementML().addSibling(ml, false);
					}
		        	
				} else {
					//Pasting fragment here requires splitting the content of targetE
					splitParagraphMLAndPaste(offset, paraContentRecords, paragraphRecords);
				}
			}

			refreshParagraphs(targetE.getStartOffset(), 0);
			
		} finally {
			if (!isSnapshotFireBan() && blockStart >= 0 && blockEnd >= 0) {
				WordMLDocument.WordMLDefaultDocumentEvent evt = 
					new WordMLDefaultDocumentEvent(
							blockStart,
							(getLength() - blockEnd) - blockStart,
							null,
							WordMLDocumentEvent.SNAPSHOT_CHANGED_EVT_NAME);
				evt.setInitialSnapshots(snapshots);
				fireSnapshotChanged(evt);
			}		
			writeUnlock();
		}
	} //insertFragment
	
	private void insertAtLastParagraph(
		List<ElementMLRecord> paraContentRecords,
		List<ElementMLRecord> paragraphRecords) 
		throws BadLocationException {
		
		DocumentElement rootE = (DocumentElement) getDefaultRootElement();
		DocumentElement lastParaE = 
			(DocumentElement) rootE.getElement(rootE.getElementCount() - 1);
		if (paraContentRecords != null) {
			//Need to check whether we may create a new ParagraphML
			//to accommodate paraContentRecords. 
			if (paragraphRecords != null
				&& (paragraphRecords.get(0).getElementML() instanceof SdtBlockML)) {
				//if paragraphRecords contains a SdtBlockML then we cannot
				//create a new ParagraphML and insert it at the last paragraph
				//because ParagraphML cannot become the sibling of SdtBlockML.
				throw new BadLocationException("Cannot insert here", rootE.getEndOffset() - 1);				
			}
			
			//Create a new ParagraphML to accommodate paraContentRecords
			List<ElementML> contents = new ArrayList<ElementML>(
					paraContentRecords.size());
			for (ElementMLRecord rec : paraContentRecords) {
				contents.add(rec.getElementML());
			}
			ElementML newParaML = ElementMLFactory.createParagraphML(
					contents, null, null);
			
			if (rootE.getElementCount() > 1 ) {
				//lastParaE has an older sibling.
				//Check whether newParaML may become sibling.
				DocumentElement olderSibling = 
					(DocumentElement) rootE.getElement(rootE.getElementCount() - 2);
				if (!olderSibling.getElementML().canAddSibling(newParaML, true)) {
					throw new BadLocationException("Cannot insert here", rootE.getEndOffset() - 1);				
				}
				olderSibling.getElementML().addSibling(newParaML, true);
			} else {
				lastParaE.getElementML().addSibling(newParaML, false);
			}
		}
		
		if (paragraphRecords != null) {
			pasteRecordsBefore((ParagraphML) lastParaE.getElementML(), paragraphRecords);
		}
	}
	
	/**
	 * Splits the content of ParagraphML element at 'offset' position
	 * and pastes 'paraContentRecords' and 'paragraphRecords' in between.
	 * 
	 * @param offset the offset position within document.
	 * @param paraContentRecords a list of ElementMLRecords that contains RunML and/or
	 * RunContentML objects
	 * @param paragraphRecords a list of ElementMLRecords that contains ElementML objects
	 * that are rendered as paragraph blocks; for example: ParagraphML or TableML
	 */
	private void splitParagraphMLAndPaste(
		int offset, 
		List<ElementMLRecord> paraContentRecords,
		List<ElementMLRecord> paragraphRecords) 
		throws BadLocationException {
		
		DocumentElement paraE = (DocumentElement) getParagraphMLElement(offset, false);
		ElementML paraML = paraE.getElementML();
		
		if (paragraphRecords != null) {
			//paragraphRecords has to be able to become siblings of 'paraML'.
			ElementMLRecord rec = paragraphRecords.get(0);
			//Because paragraphRecords are siblings 
			//checking the last record is enough.
			if (!paraML.canAddSibling(rec.getElementML(), true)) {
				//paragraphRecords cannot become siblings of ParagraphML
				throw new BadLocationException("Cannot insert here", offset);
			}
		}
		
		//Split the content of 'paraE' at 'offset' position
		ParagraphML newSibling = 
			(ParagraphML)
				DocUtil.splitElementML(
					paraE, 
					offset - paraE.getStartOffset());
		if (paraContentRecords != null) {
			DocumentElement textE = 
				(DocumentElement) getCharacterElement(Math.max(offset - 1, 0));
			pasteRecordsAfter((RunContentML) textE.getElementML(), paraContentRecords);
		}

		if (paragraphRecords != null) {
			pasteRecordsBefore(newSibling, paragraphRecords);

			ElementMLRecord lastRec = paragraphRecords.get(paragraphRecords.size() - 1);
			//Currently we limit the following merging feature
			//to ParagraphML elements only.
			if (lastRec.isFragmented()
				&& lastRec.getElementML() instanceof ParagraphML) {
				// Join the last fragmented record
				// with the content of 'targetML'
				List<ElementML> contents = XmlUtil.deleteChildren(newSibling);
				newSibling.delete();
				RunContentML rcml = 
					XmlUtil.getLastRunContentML(lastRec.getElementML());
				pasteElementMLsAfter(rcml, contents);
			}
		}
	}

	private DocumentElement getElementToPasteAt(
		DocumentElement elem, 
		ElementMLRecord record,
		boolean pasteAfter) {
		
		DocumentElement theElem = null;
		
		if (elem.getElementML().canAddSibling(record.getElementML(), pasteAfter)) {
			theElem = elem;
		} else {
			elem = (DocumentElement) elem.getParentElement();
			if (elem != null && elem != getDefaultRootElement()) {
				theElem = getElementToPasteAt(elem, record, pasteAfter);
			}
		}
		
		return theElem;
	}
	
	private boolean canbePastedAsString(List<ElementMLRecord> paraContentRecords) {
		boolean canbe = true;
		for (ElementMLRecord rec : paraContentRecords) {
			if (rec.getElementML() instanceof RunML) {
				RunML run = (RunML) rec.getElementML();
				PropertiesContainerML rPr = run.getRunProperties();
				if (rPr != null && rPr.getAttributeSet() != null
						&& rPr.getAttributeSet().getAttributeCount() > 0) {
					canbe = false;
					break;
				}
			}
		}

		return canbe;
	}
	
	private void pasteRecordsAfter(RunContentML runContentML, List<ElementMLRecord> records) {
		List<ElementML> list = new ArrayList<ElementML>(records.size());
		for (ElementMLRecord rec: records) {
			list.add(rec.getElementML());
		}
		pasteElementMLsAfter(runContentML, list);
	}
	
	private void pasteElementMLsAfter(RunContentML runContentML, List<ElementML> elems) {
		ElementML target = null;
		for (ElementML ml: elems) {
			if (ml instanceof RunContentML) {
				if (!(target instanceof RunContentML)) {
					target = runContentML;
				}
				target.addSibling(ml, true);
				target = ml;
			} else if (ml instanceof RunML) {
				if (!(target instanceof RunML)) {
					target = runContentML.getParent();
				}
				RunML targetRun = (RunML) target;
				PropertiesContainerML targetRPr = targetRun.getRunProperties();
				AttributeSet targetAttrs = 
					(targetRPr != null) ? targetRPr.getAttributeSet() : null;
				PropertiesContainerML mlRPr = ((RunML) ml).getRunProperties();
				AttributeSet mlAttrs = 
					(mlRPr != null) ? mlRPr.getAttributeSet() : null;
				
				if (targetAttrs == mlAttrs
					|| (targetAttrs != null
						&& mlAttrs != null
						&& targetAttrs.isEqual(mlAttrs))) {
					//if both targetAttrs and mlAttrs are equal 
					//we can merge 'target' with 'ml'.
					for (int i=0; i < ml.getChildrenCount(); i++) {
						ElementML child = ml.getChild(i);
						child.delete();
						targetRun.addChild(child);
					}
				} else {
					target.addSibling(ml, true);
					target = ml;
				}
			} else {
				//ml must be a ParagraphML
				if (!(target instanceof ParagraphML)) {
					target = runContentML.getParent().getParent();
				}
				target.addSibling(ml, true);
				target = ml;
			}
		}
	}
	
	private void pasteRecordsBefore(RunML runML, List<ElementMLRecord> paraContentRecords) {
		ElementML target = runML;
		//'tempRunML' will hold the leading RunContentML if any
		ElementML tempRunML = null; 
		
		for (int i=paraContentRecords.size()-1; i >= 0; i--) {
			ElementMLRecord rec = paraContentRecords.get(i);
			ElementML ml = rec.getElementML();
			
			if (ml instanceof RunContentML) {
				if (tempRunML == null) {
					tempRunML = new RunML(ObjectFactory.createR(null));
					target.addSibling(tempRunML, false);
					target = tempRunML;			
				}
				tempRunML.addChild(0, ml);
				
			} else {
				//ml must be a RunML
				PropertiesContainerML targetRPr = ((RunML) target).getRunProperties();
				AttributeSet targetAttrs = 
					(targetRPr != null) ? targetRPr.getAttributeSet() : null;
				PropertiesContainerML mlRPr = ((RunML) ml).getRunProperties();
				AttributeSet mlAttrs = 
					(mlRPr != null) ? mlRPr.getAttributeSet() : null;
					
				if (targetAttrs == mlAttrs
					|| (targetAttrs != null
							&& mlAttrs != null
							&& targetAttrs.isEqual(mlAttrs))) {
					//if both targetAttrs and mlAttrs are equal 
					//we can merge 'target' with 'ml'.
					for (int k = ml.getChildrenCount() - 1; k >=0; k--) {
						ElementML child = ml.getChild(k);
						child.delete();
						target.addChild(0, child);
					}
				} else {
					target.addSibling(ml, false);
					target = ml;
				}
			}
		}
	}
	
	private void pasteRecordsBefore(
		ParagraphML paraML, 
		List<ElementMLRecord> paragraphRecords) {
		
		ElementML target = paraML;
		for (int i=paragraphRecords.size()-1; i >= 0; i--) {
			ElementMLRecord rec = paragraphRecords.get(i);
			ElementML ml = rec.getElementML();
			target.addSibling(ml, false);
			target = ml;			
		}
	}

    /**
     * This method will refresh paragraphs in [offset, offset + length].
     * Any paragraph within this range that has not been rendered 
     * will be rendered together with those paragraphs.
     * 
     * @param offset offset position 
     * @param length specified range length
     */
    public void refreshParagraphs(int offset, int length) {
    	offset = Math.max(offset, 0);
    	offset = Math.min(offset, getLength());
    	
    	length = Math.min(length, getLength() - offset);
    	length = Math.max(length, 1);
    	
    	WordMLDocumentFilter filter = 
    		(WordMLDocumentFilter) getDocumentFilter();
    	
    	writeLock();
    	try {
    		DocumentElement rootE = (DocumentElement) getDefaultRootElement();
    		ElementML bodyML = rootE.getElementML().getChild(0);
    		
    		int idx = rootE.getElementIndex(offset);
    		int startOffset = 0;
    		int topIdx = -1;
    		if (idx > 0) {
    			DocumentElement topParaE = 
    				(DocumentElement) rootE.getElement(idx - 1);
        		topIdx = bodyML.getChildIndex(topParaE.getElementML());
        		startOffset = topParaE.getEndOffset();
    		}
    		
			idx = rootE.getElementIndex(offset + length - 1);
			int endOffset = getLength();
			int bottomIdx = bodyML.getChildrenCount() - 1;
			if (idx < rootE.getElementCount() - 1) {
				DocumentElement bottomParaE = (DocumentElement) rootE
						.getElement(idx + 1);
				bottomIdx = bodyML.getChildIndex(bottomParaE.getElementML());
				endOffset = bottomParaE.getStartOffset();
			}

			ElementML tempContainerML = new ImpliedContainerML();
    		for (idx = topIdx + 1; idx < bottomIdx; idx++) {
    			ElementML childML = bodyML.getChild(idx);
    			tempContainerML.addChild(childML, false);
    		}
    		
    		//Prepare the ElementSpecs of all refreshed ElementML
        	List<ElementSpec> tempSpecs = DocUtil.getElementSpecs(tempContainerML);
        	//Excludes the opening and closing specs
        	tempSpecs = tempSpecs.subList(1, tempSpecs.size() - 1);
        	
        	if (log.isDebugEnabled()) {
            	log.debug("refreshParagraphs(): offset=" + offset 
                		+ " length=" + length
                		+ " New Specs...");
        		DocUtil.displayStructure(tempSpecs);
        	}
        	
        	//The inserted ElementSpecs will consist of those that close the paragraph
        	//at (offset - 1) and those kept in tempSpecs.
    		List<ElementSpec> specList = new ArrayList<ElementSpec>();
        	
        	filter.setEnabled(false);
        	
        	if (startOffset == getLength()) {
        		//Refreshing the last paragraph of this document.
        		//if the last paragraph has an older sibling
        		//then create ElementSpecs for closing older sibling element;
        		//otherwise create ElementSpecs for closing the last paragraph.
    			Element tempE = 
    				rootE.getElement(Math.max(rootE.getElementCount() - 2, 0));
    			while (!tempE.isLeaf()) {
    				specList.add(new ElementSpec(null, ElementSpec.EndTagType));
    				tempE = tempE.getElement(0);
    			}
        		
        		//Add those kept in tempSpecs.
        		specList.addAll(tempSpecs);
        		
        		tempSpecs = null;
            	tempContainerML = null;
            	
        		final ElementSpec[] specsArray = new ElementSpec[specList.size()];
        		specList.toArray(specsArray);
        		specList = null;
        		
        		//Do not need to remove the last paragraph after this insertion
				insert(startOffset, specsArray);
        		
				if (log.isDebugEnabled()) {
					log.debug("refreshParagraphs(): offset=" + offset
							+ " length=" + length
							+ " After inserting new specs...");
					DocUtil.displayStructure(this);
				}
				
        	} else {
        		//Create ElementSpecs for closing the last paragraph selected
        		//in this refresh action.
        		idx = rootE.getElementIndex(endOffset - 1);
        		Element tempE = rootE.getElement(idx);
        		while (!tempE.isLeaf()) {
        			specList.add(new ElementSpec(null, ElementSpec.EndTagType));
        			tempE = tempE.getElement(0);
        		}
        		
        		specList.addAll(tempSpecs);
        		
        		tempSpecs = null;
            	tempContainerML = null;
            	
        		final ElementSpec[] specsArray = new ElementSpec[specList.size()];
        		specList.toArray(specsArray);
        		specList = null;
        		
        		//The ElementSpecs is inserted right at the end of the last paragraph
        		//selected in this refresh action.
				insert(endOffset, specsArray);
				
				if (log.isDebugEnabled()) {
					log.debug("refreshParagraphs(): offset=" + offset
							+ " length=" + length
							+ " After inserting new specs...");
					
					DocUtil.displayStructure(this);
					
					log.debug("refreshParagraphs(): offset=" + offset
							+ " length=" + length
							+ " About to remove old paragraph...");
				}
				
				//Remove all old paragraphs selected in this refresh action
				remove(startOffset, endOffset - startOffset);

			}
        	
    	} catch (BadLocationException exc) {
    		exc.printStackTrace();//ignore
    	} finally {
    		writeUnlock();
    		filter.setEnabled(true);
    	}
    }
    
    public void replace(int offset, int length, String text, AttributeSet attrs)
		throws BadLocationException {
    	log.debug("replace(): offset = " + offset 
        		+ " length = " + length 
        		+ " text = " + text);
    	super.replace(offset, length, text, attrs);
    }
    
    public void replace(int offset, int length, WordMLFragment frag, AttributeSet attrs) 
    	throws BadLocationException {
		log.debug("replace(): offset = " + offset + " length = " + length
				+ " fragment = " + frag);

		if (offset < 0 
			|| offset > getLength() 
			|| length < 0 
			|| offset + length > getLength()) {
			throw new BadLocationException("Invalid replace", offset);
		}
		
		if (length == 0) {
			//No text deletion/replacement.
			insertFragment(offset, frag, attrs);
			return;
		}
		
		boolean origFireBanState = isSnapshotFireBan();
		
		Map<BigInteger, SdtBlock> snapshots = null;
		int blockStart = -1;
		int blockEnd = -1;
		
		try {
			writeLock();
			
			if (!isSnapshotFireBan()) {
				DocumentElement rootE = (DocumentElement) getDefaultRootElement();
				
				int idx = rootE.getElementIndex(offset);
				DocumentElement elem = (DocumentElement) rootE.getElement(idx);
				//blockStart keeps the distance of elem's start position 
				//from the first character in document (offset == 0)
				blockStart = elem.getStartOffset();
				
				//Remember that (offset <= doc.getLength() - length) or otherwise
				//BadLocationException must have been thrown. 
				idx = (length == 0) ? idx : rootE.getElementIndex(offset + length - 1);
				elem = (DocumentElement) rootE.getElement(idx);
				//blockEnd keeps the distance of elem's end position
				//from the last character in document (offset == getLength())
				blockEnd = Math.max(0, getLength() - elem.getEndOffset());

				snapshots = getSnapshots(blockStart, (getLength() - blockEnd) - blockStart);
			}
			
			//We want to fire snapshot change event just once and
			//fire the one that belongs to this replace() method only.
			//Therefore we stop insertFragment() and remove() methods
			//called shortly from firing snapshot change event.
			//We have kept the current state of snapshot fire ban in
			//'origFireBanState' variable and will restore it later. 
			setSnapshotFireBan(true);
			
			Element elem = getDefaultRootElement();
			elem = elem.getElement(elem.getElementCount() - 2);
			if (offset == elem.getStartOffset()
				&& elem.getEndOffset() == offset + length) {
				//Replacing the last block element, which is the
				//immediate older sibling of the last end paragraph,
				//has to be done differently. Instead of removing
				//selected area first, we insert the fragment
				//first. If we do not do this then the application
				//will be FROZEN for unknown reason.
				//TODO: Investigate this further.
				insertFragment(offset + length, frag, attrs);
				remove(offset, length);
					
			} else {
				remove(offset, length);

				// The removed text may have been simply part of
				// a single TextElement's content. If that is the case
				// then there has not been ElementML manipulation involved
				// and hence, we need to save the remaining text content to
				// its
				// ElementML.
				// Now, the issue is the 'offset' position may have become
				// either at the start or end of the TextElement. Therefore,
				// we save two TextElements in here.
				elem = getCharacterElement(offset - 1);
				DocUtil.saveTextContentToElementML((TextElement) elem);
				elem = (TextElement) getCharacterElement(offset);
				DocUtil.saveTextContentToElementML((TextElement) elem);
				
				insertFragment(offset, frag, attrs);
			}
			
		} finally {
			//Restore the original fire ban state
			setSnapshotFireBan(origFireBanState);
			
			if (!isSnapshotFireBan() && blockStart >= 0 && blockEnd >= 0) {
				WordMLDocument.WordMLDefaultDocumentEvent evt = 
					new WordMLDefaultDocumentEvent(
							blockStart,
							(getLength() - blockEnd) - blockStart,
							null,
							WordMLDocumentEvent.SNAPSHOT_CHANGED_EVT_NAME);
				evt.setInitialSnapshots(snapshots);
				fireSnapshotChanged(evt);
			}
			
			writeUnlock();
		}

	}
    
    @Override public void insertString(int offs, String str, AttributeSet a) 
    	throws BadLocationException {
    	log.debug("insertString(): offset = " + offs + " text = " + str);
    	super.insertString(offs, str, a);
    }
  
    /**
     * Take the current snapshots of content controls within [offset, offset + length].
     * Each snapshot is a clone of content control's SdtBlockML.
     * 
     * @param offset
     * @param length
     * @return A Map whose key is SdtBlock Id and value is SdtBlockML 
     * if there are content controls within the specified area;
     *         null, otherwise;
     */
    public Map<BigInteger, SdtBlock> getSnapshots(int offset, int length) {
    	offset = Math.max(offset, 0);
    	offset = Math.min(offset, getLength());
    	
    	length = Math.min(length, getLength() - offset);
    	length = Math.max(length, 1);
    	
		Map<BigInteger, SdtBlock> theSnapshots = 
			new HashMap<BigInteger, SdtBlock>();
		
		try {
			readLock();
			
			DocumentElement rootE = (DocumentElement) getDefaultRootElement();
			int topIdx = rootE.getElementIndex(offset) - 1;
			int bottomIdx = Math.min(
					rootE.getElementIndex(offset + length - 1) + 1, rootE
							.getElementCount() - 1);
			for (int i = topIdx + 1; i < bottomIdx; i++) {
				DocumentElement elem = (DocumentElement) rootE.getElement(i);
				ElementML elemML = elem.getElementML();
				if (elemML instanceof SdtBlockML) {
					SdtBlockML elemSdt = (SdtBlockML) elemML;

					Object cloneObj = XmlUtils
							.deepCopy(elemSdt.getDocxObject());
					org.docx4j.wml.SdtBlock snapshot = (org.docx4j.wml.SdtBlock) JAXBIntrospector
							.getValue(cloneObj);
					theSnapshots.put(snapshot.getSdtPr().getId().getVal(),
							snapshot);
				}
			}
			
		} finally {
			readUnlock();
		}
		
		if (theSnapshots.isEmpty()) {
			theSnapshots = null;
		}
		return theSnapshots;
    }
    
    protected void fireSnapshotChanged(WordMLDocumentEvent e) {
    	if (isSnapshotFireBan()) {
    		return;
    	}
    	
		// Guaranteed to return a non-null array
		Object[] listeners = listenerList.getListenerList();
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == DocumentListener.class
				&& listeners[i + 1] instanceof WordMLDocumentListener) {
				((WordMLDocumentListener) listeners[i + 1]).snapshotChangedUpdate(e);
			}
		}
	}

    protected void insertUpdate(DefaultDocumentEvent chng, AttributeSet attrs) {
        int offset = chng.getOffset();
        int length = chng.getLength();
        
        DocumentElement leftLeaf = 
        	(DocumentElement) getCharacterElement(offset);
        DocumentElement rightLeaf = 
        	(DocumentElement) getCharacterElement(offset + length);
        DocumentElement rightImpliedPara =
        	(DocumentElement) rightLeaf.getParentElement().getParentElement();
        
        List<ElementSpec> specs = new ArrayList<ElementSpec>();
        if (rightImpliedPara.getStartOffset() == offset + length) {
        	if (offset > 0) {
        		//Needs to fill in specs with a collection of ElementSpecs
        		//that closes the paragraph block that consists of leftLeaf
        		//and opens a new paragraph block that consists of rightLeaf.
        		DocumentElement commonParent = 
        			DocUtil.getCommonParentElement(leftLeaf, rightLeaf);
        		
        		Element tempE = leftLeaf.getParentElement();
        		while (tempE != commonParent) {
        			//Close ElementSpec from leftLeaf's parent up to commonParent
            		specs.add(new ElementSpec(null, ElementSpec.EndTagType)); 
            		tempE = tempE.getParentElement();
        		}
        		
       			List<ElementSpec> newOpenSpecs = new ArrayList<ElementSpec>();
        		//ElementSpecs that opens a new paragraph block start with
        		//a new leaf ElementSpec for rightLeaf's content.
       			ElementSpec es = 
       				new ElementSpec(
            			rightLeaf.getAttributes(), 
            			ElementSpec.ContentType, 
            			length);
            	es.setDirection(ElementSpec.JoinNextDirection);
            	newOpenSpecs.add(es);
            	
            	tempE = rightLeaf.getParentElement();
            	while(tempE != commonParent) {
            		//Open ElementSpec from rightLeaf's parent up to commonParent
        			es = new ElementSpec(tempE.getAttributes(), ElementSpec.StartTagType);
        			es.setDirection(ElementSpec.JoinNextDirection);
        			newOpenSpecs.add(es);
        			tempE = tempE.getParentElement();
            	}
            	
            	//Add newOpenSpecs to specs in reverse order
        		for (int i=newOpenSpecs.size()-1; 0 <= i; i--) {
        			specs.add(newOpenSpecs.get(i));
        		}
        	} else {
        		//Should never come here ?
        	}
        } else if (attrs.getAttributeCount() > 0
        			&& leftLeaf.getAttributes().containsAttributes(attrs)) {
        	
        	ElementSpec es = 
        		new ElementSpec(
        			leftLeaf.getAttributes(), 
        			ElementSpec.ContentType, 
        			length);
        	es.setDirection(ElementSpec.JoinPreviousDirection);
        	specs.add(es);
        	
        } else if (attrs.getAttributeCount() > 0
    			&& rightLeaf.getAttributes().containsAttributes(attrs)) {
    	
    		// Close RunML
    		specs.add(new ElementSpec(null, ElementSpec.EndTagType));
    		
    		// Open new RunML
    		ElementSpec es = 
    			new ElementSpec(
    				rightLeaf.getParentElement().getAttributes(), 
    				ElementSpec.StartTagType);
    		es.setDirection(ElementSpec.JoinNextDirection);
    		specs.add(es);
    		
    		// Add new leaf
        	es = 
        		new ElementSpec(
        			rightLeaf.getAttributes(), 
        			ElementSpec.ContentType, 
        			length);
        	es.setDirection(ElementSpec.JoinNextDirection);
        	specs.add(es);
        }
        
        if (!specs.isEmpty()) {
			ElementSpec[] specsArray = new ElementSpec[specs.size()];
			specsArray = specs.toArray(specsArray);
			buffer.insert(offset, length, specsArray, chng);
		}
        
	} // insertUpdate()

	protected void createElementStructure(List<ElementSpec> list) {
		ElementSpec[] specs = new ElementSpec[list.size()];
		list.toArray(specs);
		try {
			writeLock();
			super.create(specs);

			DocumentElement root = (DocumentElement) getDefaultRootElement();
			StyleConstants.setFontFamily(
					(MutableAttributeSet) root.getAttributes(),
					FontManager.getInstance().getDocx4AllDefaultFontFamilyName());
			
			StyleConstants.setFontSize(
					(MutableAttributeSet) root.getAttributes(),
					FontManager.getInstance().getDocx4AllDefaultFontSize());
			
			//Needs to validate the last ParagraphML's parent.
			DocumentElement lastPara = (DocumentElement) root.getElement(root
					.getElementCount() - 1);
			ElementML lastParaML = lastPara.getElementML();
			//detach from its previous parent
			lastParaML.delete();
			//make the new document root as new parent
			root.getElementML().getChild(0).addChild(lastParaML);
			
		} finally {
			writeUnlock();
		}
	}
	
    /**
     * Creates the root element to be used to represent the
     * default document structure.
     *
     * @return the element base
     */
	@Override
    protected AbstractElement createDefaultRoot() {
		DocumentML docML = ElementMLFactory.createEmptyDocumentML();
		
		ElementML bodyML = docML.getChild(0);
		ElementML paraML = bodyML.getChild(0);
		ElementML runML = paraML.getChild(0);
		ElementML rcML = runML.getChild(0);
				
		if (runML == null || rcML == null) {
			//Very unlikely but just in case
			throw new RuntimeException("Invalid default DocumentML");
		}
		
		writeLock();
		MutableAttributeSet a = new SimpleAttributeSet();
		
		//Document
		a.addAttribute(WordMLStyleConstants.ElementMLAttribute, docML);
        a.addAttribute(
        		StyleConstants.FontFamily, 
        		FontManager.getInstance().getDocx4AllDefaultFontFamilyName());
        a.addAttribute(
        		StyleConstants.FontSize, 
        		FontManager.getInstance().getDocx4AllDefaultFontSize());
		BlockElement document = new BlockElement(null, a.copyAttributes());
		a.removeAttributes(a);
		
		//Body
		//a.addAttribute(WordMLStyleConstants.ElementMLAttribute, bodyML);
		//BlockElement body = new BlockElement(document, a.copyAttributes());
		//a.removeAttributes(a);
		
		//Paragraph
		a.addAttribute(WordMLStyleConstants.ElementMLAttribute, paraML);
		//BlockElement paragraph = new BlockElement(body, a.copyAttributes());
		BlockElement paragraph = new BlockElement(document, a.copyAttributes());
		a.removeAttributes(a);
		
		//Implied Paragraph
		a.addAttribute(WordMLStyleConstants.ElementMLAttribute, ElementML.IMPLIED_PARAGRAPH);
		BlockElement impliedParagraph = new BlockElement(paragraph, a.copyAttributes());
		a.removeAttributes(a);
		
		//Run
		a.addAttribute(WordMLStyleConstants.ElementMLAttribute, runML);
		BlockElement run = new BlockElement(impliedParagraph, a.copyAttributes());
		a.removeAttributes(a);

		//Text
		a.addAttribute(WordMLStyleConstants.ElementMLAttribute, rcML);
		TextElement text = new TextElement(run, a, 0, 1);

		Element[] buff = new Element[1];
		buff[0] = text;
		run.replace(0, 0, buff);
		
		buff[0] = run;
		impliedParagraph.replace(0, 0, buff);
		
		buff[0] = impliedParagraph;
		paragraph.replace(0, 0, buff);
		
		buff[0] = paragraph;
		document.replace(0, 0, buff);
		//body.replace(0, 0, buff);
		
		//buff[0] = body;
		//document.replace(0, 0, buff);
		
		writeUnlock();
		return document;
	}
	
    /**
     * Creates a document branch element, that can contain other elements.
     * This is implemented to return an element of type 
     * <code>WordMLDocument.BlockElement</code> or
     * <code>WordMLDocument.RunElement</code>
     *
     * @param parent the parent element
     * @param a the attributes
     * @return the element
     */
	@Override
    protected Element createBranchElement(Element parent, AttributeSet a) {
		return new BlockElement(parent, a);
	}

    /**
     * Creates a document leaf element that directly represents
     * text (doesn't have any children).  This is implemented
     * to return an element of type 
     * <code>WordMLDocument.TextElement</code>.
     *
     * @param parent the parent element
     * @param a the attributes for the element
     * @param p0 the beginning of the range (must be at least 0)
     * @param p1 the end of the range (must be at least p0)
     * @return the new element
     */
    protected Element createLeafElement(Element parent, AttributeSet a, int p0, int p1) {
    	return new TextElement(parent, a, p0, p1);
    }

	//============= INNER CLASS SECTION =============

    public class BlockElement extends BranchElement implements DocumentElement {
		public BlockElement(Element parent, AttributeSet a) {
			super(parent, a);
		}

		public ElementML getElementML() {
			return (ElementML) getAttribute(WordMLStyleConstants.ElementMLAttribute);
		}
		
		public String getStyleNameInAction() {
			String styleName = null;
			
			DocumentElement parent = (DocumentElement) getParentElement();
			if (parent == null) {
				Style defaultStyle = getStyleSheet().getStyle(StyleSheet.DEFAULT_STYLE);
				styleName = 
					(String) defaultStyle.getAttribute(
								WordMLStyleConstants.DefaultParagraphStyleNameAttribute);
			} else {
				ElementML elemML = getElementML();
				String styleId = null;
				if (elemML instanceof RunML) {
					styleId = (String) getAttribute(WordMLStyleConstants.RStyleAttribute);
				} else if (elemML instanceof ParagraphML && !elemML.isImplied()) {
					styleId = (String) getAttribute(WordMLStyleConstants.PStyleAttribute);
				}
				if (styleId != null) {
					//Search for style name
					Style temp = getStyleSheet().getIDStyle(styleId);
					if (temp != null) {
						styleName = 
							(String) temp.getAttribute(WordMLStyleConstants.StyleUINameAttribute);
					}
				}
				
				if (styleName == null) {
					styleName = parent.getStyleNameInAction();
				}
			}
			return styleName;
		}
		
		public boolean isEditable() {
			ElementML elemML = getElementML();
			
			if ((elemML instanceof ParagraphML) && elemML.isImplied()) {
				DocumentElement parent = (DocumentElement) getParentElement();
				return parent.isEditable();
			}
			
			boolean isEditable = !elemML.isDummy();
			if (isEditable) {
				DocumentElement parent = (DocumentElement) getParentElement();
				isEditable = (parent == null || parent.isEditable());
			}
			return isEditable;
		}
		
		public void save() {
			;//TODO: Saving 
			log.debug("save(): this=" + this);
		}

		public String getName() {
			//This name has to be unique so that this element
			//cannot be joined when document structure is edited
			ElementML elem = getElementML();
			if (elem != null) {
				return "BlockElement@" + hashCode() + "[" + elem.toString() + "]";
			}
			return super.getName();
		}

		public String toString() {
		    return getName() + "(" + getStartOffset() + "," +
			getEndOffset() + ")\n";
		}
	}// BlockElement inner class

    public class TextElement extends LeafElement implements DocumentElement {
		public TextElement(Element parent, AttributeSet a, int offs0, int offs1) {
			super(parent, a, offs0, offs1);
		}

		public ElementML getElementML() {
			return (ElementML) getAttribute(WordMLStyleConstants.ElementMLAttribute);
		}
		
		public String getStyleNameInAction() {
			DocumentElement parent = (DocumentElement) getParentElement();
			return parent.getStyleNameInAction();
		}
		
		public boolean isEditable() {
			ElementML elemML = getElementML();
			
			boolean isEditable = !elemML.isDummy() && !elemML.isImplied();
			if (isEditable) {
				DocumentElement parent = (DocumentElement) getParentElement();
				isEditable = parent.isEditable();
			}
			
			return isEditable;
		}
		
		public void save() {
			;//TODO: Saving 
			log.debug("save(): this=" + this);
		}
		
		public String getName() {
			//This name has to be unique so that this element
			//cannot be joined when document structure is edited
			ElementML elem = getElementML();
			if (elem != null) {
				return "TextElement@" + hashCode() + "[" + elem.toString() + "]";
			}
			return super.getName();
		}

		public String toString() {
		    return getName() + "(" + getStartOffset() + "," +
			getEndOffset() + ")\n";
		}
	}// TextElement inner class
    
    public class WordMLDefaultDocumentEvent extends DefaultDocumentEvent implements WordMLDocumentEvent {
    	private String eventName;
        private Map<BigInteger, SdtBlock> initialSnapshots;

        public WordMLDefaultDocumentEvent(int offs, int len, DocumentEvent.EventType type, String eventName) {
        	super(offs, len, type);
        	this.eventName = 
        		(eventName == null && type != null) ? type.toString() : eventName;
        }
        
    	public String getEventName() {
    		return eventName;
    	}
    	
    	public void setInitialSnapshots(Map<BigInteger, SdtBlock> snapshots) {
    		initialSnapshots = snapshots;
    	}
    	
    	public Map<BigInteger, SdtBlock> getInitialSnapshots() {
    		return initialSnapshots;
    	}
   }
}// WordMLDocument class

