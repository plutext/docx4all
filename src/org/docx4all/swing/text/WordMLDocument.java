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
import java.util.ArrayList;
import java.util.List;

import javax.swing.event.DocumentEvent;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Element;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;

import org.apache.log4j.Logger;
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

public class WordMLDocument extends DefaultStyledDocument {
	private static Logger log = Logger.getLogger(WordMLDocument.class);

	public final static String FILE_PATH_PROPERTY = "filePathProperty";
	
	public final static String WML_PACKAGE_PROPERTY = "wmlPackageProperty";
	
	public WordMLDocument() {
		super();
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
    	
		try {
			writeLock();

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
			writeUnlock();
		}
	}
    
    public void setParagraphMLAttributes(int offset, int length,
			AttributeSet attrs, boolean replace) {

		if (offset > getLength() || attrs == null
				|| attrs.getAttributeCount() == 0) {
			return;
		}

		length = Math.min(getLength() - offset, length);

		try {
			writeLock();
			DefaultDocumentEvent changes = 
				new DefaultDocumentEvent(offset, length, DocumentEvent.EventType.CHANGE);

		    AttributeSet attrsCopy = attrs.copyAttributes();
		    
			Element rootE = getDefaultRootElement();
			int startIdx = rootE.getElementIndex(offset);
			int endIdx = rootE.getElementIndex(offset + ((length > 0) ? length - 1 : 0));
			for (int i = startIdx; i <= endIdx; i++) {
				DocumentElement paraE = (DocumentElement) rootE.getElement(i);
				ParagraphML paraML = (ParagraphML) paraE.getElementML();
				paraML.addAttributes(attrsCopy, replace);
				
				MutableAttributeSet paraAttr = 
					(MutableAttributeSet) paraE.getAttributes();
				//changes.addEdit(
				//	new AttributeUndoableEdit(paraE, attrsCopy, replace));
				if (replace) {
					paraAttr.removeAttributes(paraAttr);
				}
				paraAttr.addAttributes(attrs);
			}

			changes.end();
			fireChangedUpdate(changes);
			//fireUndoableEditUpdate(new UndoableEditEvent(this, changes));
		} finally {
			writeUnlock();
		}
	}
    
    public void setParagraphStyle(int offset, int length, String styleId) {
		if (offset > getLength() || styleId == null || styleId.length() == 0) {
			return;
		}

		length = Math.min(getLength() - offset, length);

		try {
			writeLock();
			Style style = getStyleSheet().getReferredStyle(styleId);
			String type = 
				(style == null) 
					? null 
					: (String) style.getAttribute(WordMLStyleConstants.StyleTypeAttribute);
			if (StyleSheet.PARAGRAPH_ATTR_VALUE.equals(type)) {
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
			;//ignore
		} finally {
			writeUnlock();
		}
    }
    
    public void setRunStyle(int offset, int length, String styleId) {
		if (offset >= getLength() || length == 0 || styleId == null
				|| styleId.length() == 0) {
			return;
		}

		length = Math.min(getLength() - offset, length);

		try {
			writeLock();

			Style style = getStyleSheet().getReferredStyle(styleId);
			String type = (style == null) ? null : (String) style
					.getAttribute(WordMLStyleConstants.StyleTypeAttribute);
			if (StyleSheet.CHARACTER_ATTR_VALUE.equals(type)) {
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
			writeUnlock();
		}
	}
    
	public void insertFragment(int offset, WordMLFragment fragment, AttributeSet attrs) 
		throws BadLocationException {
		
		List<ElementMLRecord> paraContentRecords = 
			fragment.getParagraphContentRecords();
		List<ElementMLRecord> paragraphRecords = 
			fragment.getParagraphRecords();
		if (paraContentRecords == null && paragraphRecords == null) {
			if (log.isDebugEnabled()) {
				log.debug("insertFragment(): offset=" + offset
					+ " fragment's record = NULL");
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
		
		writeLock();
		try {
			DocumentElement textE = (DocumentElement) getCharacterElement(Math.max(offset - 1, 0));
			if (0 < offset
					&& offset < textE.getEndOffset()
					&& (!textE.isEditable() || (paraContentRecords == null && paragraphRecords != null))) {
				throw new BadLocationException("Cannot insert here", offset);
			}

			DocumentElement paraAtOffset = (DocumentElement) getParagraphMLElement(offset, false);

			if (paraAtOffset.getEndOffset() == paraAtOffset.getParentElement().getEndOffset()) {
				//Insert at the last paragraph in the document.
				if (paraContentRecords != null) {
					List<ElementML> contents = new ArrayList<ElementML>(
							paraContentRecords.size());
					for (ElementMLRecord rec : paraContentRecords) {
						contents.add(rec.getElementML());
					}
					ElementML newParaML = ElementMLFactory.createParagraphML(
							contents, null, null);
					paraAtOffset.getElementML().addSibling(newParaML, false);
				}

				if (paragraphRecords != null) {
					pasteRecordsBefore((ParagraphML) paraAtOffset.getElementML(), paragraphRecords);
				}

			} else if (paraContentRecords != null && paragraphRecords == null) {
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
							// Paste at an empty paragraph
							ElementML paraML = paraAtOffset.getElementML();
							for (ElementMLRecord rec: paraContentRecords) {
								paraML.addChild(rec.getElementML());
							}
							
						} else if (newlineRunML == runML) {
							// Paste after a soft break
							RunContentML softBreak = (RunContentML) textE
									.getElementML();

							//Split soft break before pasting.
							List<ElementML> runContents = new ArrayList<ElementML>(
									runE.getElementCount());
							for (int i = 0; i < runE.getElementCount(); i++) {
								DocumentElement tempE = (DocumentElement) runE
										.getElement(i);
								ElementML ml = tempE.getElementML();
								ml.delete();
								runContents.add(ml);
							}

							RunPropertiesML rPr = (RunPropertiesML) ((RunML) runML)
									.getRunProperties();
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
							pasteRecordsBefore((RunML) runML,
									paraContentRecords);
						}
					} else {
						// paste at somewhere inside a paragraph
						pasteRecordsAfter((RunContentML) textE.getElementML(),
								paraContentRecords);
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
					DocUtil
							.splitElementML(runE, offset
									- runE.getStartOffset());
					pasteRecordsAfter((RunContentML) textE.getElementML(),
							paraContentRecords);
				}

			} else if (paraContentRecords != null && paragraphRecords != null) {
				if (paraAtOffset.getStartOffset() == offset) {
					ParagraphML paraML = (ParagraphML) paraAtOffset
							.getElementML();
					ParagraphPropertiesML pPr = (ParagraphPropertiesML) paraML
							.getParagraphProperties();
					if (pPr != null) {
						pPr = (ParagraphPropertiesML) pPr.clone();
					}

					List<ElementML> contents = new ArrayList<ElementML>(
							paraContentRecords.size());
					for (ElementMLRecord rec : paraContentRecords) {
						contents.add(rec.getElementML());
					}
					ParagraphML newSibling = ElementMLFactory
							.createParagraphML(contents, pPr, null);
					paraML.addSibling(newSibling, false);

					pasteRecordsBefore(paraML, paragraphRecords);

					ElementMLRecord lastRec = paragraphRecords
							.get(paragraphRecords.size() - 1);
					if (lastRec.isFragmented()) {
						// Join the last fragmented record
						// with the content of 'paraML'
						contents = XmlUtil.deleteChildren(paraML);
						paraML.delete();
						RunContentML rcml = XmlUtil.getLastRunContentML(lastRec
								.getElementML());
						pasteElementMLsAfter(rcml, contents);
					}
				} else {
					ParagraphML newSibling = (ParagraphML) DocUtil
							.splitElementML(paraAtOffset, offset
									- paraAtOffset.getStartOffset());
					pasteRecordsAfter((RunContentML) textE.getElementML(),
							paraContentRecords);

					pasteRecordsBefore(newSibling, paragraphRecords);

					ElementMLRecord lastRec = paragraphRecords
							.get(paragraphRecords.size() - 1);
					if (lastRec.isFragmented()) {
						// Join the last fragmented record
						// with the content of 'newSibling'
						List<ElementML> contents = XmlUtil
								.deleteChildren(newSibling);
						newSibling.delete();
						RunContentML rcml = XmlUtil.getLastRunContentML(lastRec
								.getElementML());
						pasteElementMLsAfter(rcml, contents);
					}
				}

			} else if (paraContentRecords == null && paragraphRecords != null) {
				if (paraAtOffset.getStartOffset() < offset) {
					ParagraphML newSibling = 
						(ParagraphML) DocUtil.splitElementML(
							paraAtOffset, 
							offset - paraAtOffset.getStartOffset());
					pasteRecordsBefore(newSibling, paragraphRecords);
				} else {
					pasteRecordsBefore((ParagraphML) paraAtOffset.getElementML(),
						paragraphRecords);
				}
			}

			refreshParagraphs(paraAtOffset.getStartOffset(), 0);
			
		} finally {
			writeUnlock();
		}
	} //insertFragment
	
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

    public  void insertElementSpecs(int offset, ElementSpec[] specs) 
    	throws BadLocationException {
    	super.insert(offset, specs);
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
    			ElementML paraML = bodyML.getChild(idx);
    			tempContainerML.addChild(paraML, false);
    		}
    		
        	List<ElementSpec> tempSpecs = DocUtil.getElementSpecs(tempContainerML);
        	//Excludes the opening and closing specs of tempContainerML
        	tempSpecs = tempSpecs.subList(1, tempSpecs.size() - 1);
        	
        	
        	if (log.isDebugEnabled()) {
            	log.debug("refreshParagraphs(): offset=" + offset 
                		+ " length=" + length
                		+ " New Specs...");
        		DocUtil.displayStructure(tempSpecs);
        	}
        	
    		List<ElementSpec> specList = 
    			new ArrayList<ElementSpec>(tempSpecs.size() + 3);
    		// Close RunML
    		specList.add(new ElementSpec(null, ElementSpec.EndTagType));
    		// Close Implied ParagraphML
    		specList.add(new ElementSpec(null, ElementSpec.EndTagType));
    		// Close ParagraphML
    		specList.add(new ElementSpec(null, ElementSpec.EndTagType));
    		specList.addAll(tempSpecs);
    		
    		tempSpecs = null;
        	tempContainerML = null;
        	
    		final ElementSpec[] specsArray = new ElementSpec[specList.size()];
    		specList.toArray(specsArray);
    		specList = null;
        	
        	filter.setEnabled(false);
        	
        	if (startOffset == getLength()) {
        		//Do not need to remove the last paragraph element
				insert(startOffset, specsArray);
        		
				if (log.isDebugEnabled()) {
					log.debug("refreshParagraphs(): offset=" + offset
							+ " length=" + length
							+ " After inserting new specs ...");
					DocUtil.displayStructure(this);
				}
				
        	} else {
				insert(endOffset, specsArray);
				
				if (log.isDebugEnabled()) {
					log.debug("refreshParagraphs(): offset=" + offset
							+ " length=" + length
							+ " After inserting new specs ...");
					DocUtil.displayStructure(this);
				}
				
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

		if (length == 0 && frag == null) {
			return;
		}

		writeLock();
		try {
			if (length > 0 && offset < getLength()) {
				remove(offset, length);
			}

			insertFragment(offset, frag, attrs);
			
		} finally {
			writeUnlock();
		}

	}
    
    public void insertString(int offs, String str, AttributeSet a) 
    	throws BadLocationException {
    	log.debug("insertString(): offset = " + offs + " text = " + str);
    	super.insertString(offs, str, a);
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
        		DocumentElement rightPara = 
        			(DocumentElement) rightImpliedPara.getParentElement();
        	
        		// Close RunML
        		specs.add(new ElementSpec(null, ElementSpec.EndTagType));
        		// Close Implied ParagraphML
        		specs.add(new ElementSpec(null, ElementSpec.EndTagType));
        		if (rightImpliedPara.getStartOffset() == rightPara.getStartOffset()) {
        			// Close ParagraphML
        			specs.add(new ElementSpec(null, ElementSpec.EndTagType));
        			// Open New ParagraphML
        			ElementSpec es = 
        				new ElementSpec(
        						rightPara.getAttributes(), 
        						ElementSpec.StartTagType);
        			es.setDirection(ElementSpec.JoinNextDirection);
        			specs.add(es);
        		}
        		//Open new Implied ParagraphML
        		ElementSpec es = 
        			new ElementSpec(
        					rightImpliedPara.getAttributes(), 
        					ElementSpec.StartTagType);
        		es.setDirection(ElementSpec.JoinNextDirection);
        		specs.add(es);
        		//Open new RunML
        		es = new ElementSpec(
        				rightLeaf.getParentElement().getAttributes(), 
        				ElementSpec.StartTagType);
        		es.setDirection(ElementSpec.JoinNextDirection);
        		specs.add(es);
            	
            	//Add new leaf
            	es = new ElementSpec(
            			rightLeaf.getAttributes(), 
            			ElementSpec.ContentType, 
            			length);
            	es.setDirection(ElementSpec.JoinNextDirection);
            	specs.add(es);
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
    
}// WordMLDocument class

