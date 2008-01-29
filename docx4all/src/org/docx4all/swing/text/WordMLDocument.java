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

import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Element;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;

import org.apache.log4j.Logger;
import org.docx4all.swing.text.WordMLFragment.ElementMLRecord;
import org.docx4all.util.DocUtil;
import org.docx4all.util.XmlUtil;
import org.docx4all.xml.DocumentML;
import org.docx4all.xml.ElementML;
import org.docx4all.xml.ElementMLFactory;
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
	
	public WordMLDocument() {
		super();
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
					int tempInt = runE.getEndOffset() - (offset + length);
					RunML ml = copyRunML(runE, (offset+length), tempInt);
					runML.addSibling(ml, true);
					
					ml = copyRunML(runE, offset, length);
					ml.addAttributes(attrs, replace);
					runML.addSibling(ml, true);

					tempInt = offset - runE.getStartOffset();
					RunML newSibling = 
						(RunML) DocUtil.splitElementML(runE, tempInt);
					newSibling.delete();
					
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

			// Have to refresh affected paragraphs between [offset, offset+length]
			Element rootE = getDefaultRootElement();
			int start = rootE.getElementIndex(offset);
			int end = rootE.getElementIndex(offset + length - 1);
			
			//Collect the ElementSpecs
			List<ElementSpec> specs = new ArrayList<ElementSpec>();
			int idx = start;
			while (idx <= end) {
				DocumentElement para = (DocumentElement) rootE.getElement(idx++);
				specs.addAll(DocUtil.getElementSpecs(para.getElementML()));
			}
			
			//Remove
			start = rootE.getElement(start).getStartOffset();
			end = rootE.getElement(end).getEndOffset();
			WordMLDocumentFilter filter = (WordMLDocumentFilter) getDocumentFilter();
			filter.setEnabled(false);
			remove(start, end - start);
			filter.setEnabled(true);

			insertParagraphsLater(start, specs);
			
		} finally {
			writeUnlock();
		}
	}
    
    private RunML copyRunML(DocumentElement runE, int offset, int length) {
    	RunML runML = (RunML) runE.getElementML();
    	
		List<ElementML> contents = new ArrayList<ElementML>();
		try {
			TextSelector ts = new TextSelector(this, offset, length);
			List<ElementMLRecord> records = ts.getElementMLRecords();
			for (ElementMLRecord rec: records) {
				contents.add(rec.getElementML());
			}
		} catch (BadSelectionException exc) {
			;//ignore
		}
		
		RunPropertiesML rPr = 
			(RunPropertiesML) runML.getRunProperties();
		if (rPr != null) {
			rPr = (RunPropertiesML) rPr.clone();
		}
		return (RunML) ElementMLFactory.createRunML(contents, rPr);
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
			DocumentElement textE = (DocumentElement) getCharacterElement(offset - 1);
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
				DocumentElement runE = (DocumentElement) textE
						.getParentElement();
				if (runE.getEndOffset() == offset) {
					DocumentElement impliedParaE = (DocumentElement) runE
							.getParentElement();
					if (impliedParaE.getEndOffset() == offset) {
						// paste at the start of a paragraph
						ElementML newlineRunML = runE.getElementML();

						runE = (DocumentElement) getRunMLElement(offset);
						ElementML runML = runE.getElementML();
						if (newlineRunML == runML) {
							// Paste after a soft break
							RunContentML softBreak = (RunContentML) textE
									.getElementML();

							// Split soft break before pasting
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
				} else {
					// paste at somewhere inside a run
					DocUtil
							.splitElementML(runE, offset
									- runE.getStartOffset());
					pasteRecordsAfter((RunContentML) textE.getElementML(),
							paraContentRecords);
				} // if (runE.getEndOffset() == offset) else

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
				pasteRecordsBefore((ParagraphML) paraAtOffset.getElementML(),
						paragraphRecords);
			}

			refreshParagraph(paraAtOffset);
			
		} finally {
			writeUnlock();
		}
	} //insertFragment
	
	private void refreshParagraph(DocumentElement paragraphElement) {
		DocumentElement rootE = 
			(DocumentElement) paragraphElement.getParentElement();
		ElementML bodyML = rootE.getElementML().getChild(0);

		//startIdx and endIdx hold the older 
		//and younger sibling of paragraphElement respectively
		int startIdx = -1;
		int endIdx = bodyML.getChildrenCount() - 1;

		//find older sibling
		int idx = rootE.getElementIndex(paragraphElement.getStartOffset());
		if (idx > 0) {
			DocumentElement tempE = 
				(DocumentElement) rootE.getElement(idx - 1);
			ElementML tempML = tempE.getElementML();
			startIdx = bodyML.getChildIndex(tempML);
		}

		//find younger sibling
		if (idx < rootE.getElementCount() - 1) {
			// Excludes the end of document implied paragraph element
			DocumentElement tempE = (DocumentElement) rootE
					.getElement(idx + 1);
			ElementML tempML = tempE.getElementML();
			endIdx = bodyML.getChildIndex(tempML);
		}

		List<ElementSpec> specs = new ArrayList<ElementSpec>();
		idx = startIdx + 1;
		while (idx < endIdx) {
			ElementML para = bodyML.getChild(idx++);
			specs.addAll(DocUtil.getElementSpecs(para));
		}

		idx = paragraphElement.getStartOffset();
		if (paragraphElement.getEndOffset() != rootE.getEndOffset()) {
			//if not the last paragraph remove 'paragraphElement' and refresh.
			WordMLDocumentFilter filter = (WordMLDocumentFilter) getDocumentFilter();
			filter.setEnabled(false);//Disable temporarily

			try {
				remove(idx, paragraphElement.getEndOffset() - idx);
			} catch (BadLocationException exc) {
				;//ignore
			}

			// Remember to enable filter back
			filter.setEnabled(true);
		}
		
		insertParagraphsLater(idx, specs);
		
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
	
	private void insertParagraphsLater(
		final int offset,
		final List<ElementSpec> specs) {

		final WordMLDocument doc = this;
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				try {
					DocUtil.insertParagraphs(doc, offset, specs);
					
					if (log.isDebugEnabled()) {
						log.debug("insertFragment(): Refreshed new specs...");
						DocUtil.displayStructure(specs);
						DocUtil.displayStructure(doc);
					}
					
				} catch (BadLocationException exc) {
					;// ignore
				}
			}
		});
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
			} else if (ml instanceof RunML) {
				if (!(target instanceof RunML)) {
					target = runContentML.getParent();
				}
			} else {
				//must be a ParagraphML
				if (!(target instanceof ParagraphML)) {
					target = runContentML.getParent().getParent();
				}
			}
			target.addSibling(ml, true);
			target = ml;
		}
	}
	
	private void pasteRecordsBefore(RunML runML, List<ElementMLRecord> paraContentRecords) {
		ElementML target = null;
		//'tempRunML' will hold the leading RunContentML if any
		ElementML tempRunML = null; 
		
		for (int i=paraContentRecords.size()-1; i >= 0; i++) {
			ElementMLRecord rec = paraContentRecords.get(i);
			ElementML ml = rec.getElementML();
			
			if (ml instanceof RunContentML) {
				if (tempRunML == null) {
					tempRunML = new RunML(ObjectFactory.createR(null));
					target.addSibling(ml, false);
					target = ml;			
				}
				tempRunML.addChild(0, ml);
				
			} else {
				//must be a RunML
				if (target == null) {
					target = runML;
				}
				
				target.addSibling(ml, false);
				target = ml;			
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
    
    public void replace(int offset, int length, String text,
            AttributeSet attrs) throws BadLocationException {
    	log.debug("replace(): offset = " + offset 
    		+ " length = " + length 
    		+ " text = " + text);
    	super.replace(offset, length, text, attrs);
    }
    
    public void insertString(int offs, String str, AttributeSet a) 
    	throws BadLocationException {
    	log.debug("insertString(): offset = " + offs + " text = " + str);
    	super.insertString(offs, str, a);
    }
    
    protected void insertUpdate(DefaultDocumentEvent chng, AttributeSet attr) {
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
        	}
        	
        	//Add new leaf
        	ElementSpec es = new ElementSpec(
        			rightLeaf.getAttributes(), 
        			ElementSpec.ContentType, 
        			length);
        	es.setDirection(ElementSpec.JoinNextDirection);
        	specs.add(es);

        } else {
        	ElementSpec es = 
        		new ElementSpec(
        			leftLeaf.getAttributes(), 
        			ElementSpec.ContentType, 
        			length);
        	es.setDirection(ElementSpec.JoinPreviousDirection);
        	specs.add(es);
        }
        
        ElementSpec[] specsArray = new ElementSpec[specs.size()];
        specsArray = specs.toArray(specsArray);
        buffer.insert(offset, length, specsArray, chng);
		
		if (log.isDebugEnabled()) {
			DocUtil.displayStructure(this);
		}
    } //insertUpdate()

	protected void createElementStructure(List<ElementSpec> list) {
		ElementSpec[] specs = new ElementSpec[list.size()];
		list.toArray(specs);
		super.create(specs);
		
		DocumentElement root = (DocumentElement) getDefaultRootElement();
		DocumentElement lastPara = 
			(DocumentElement) root.getElement(root.getElementCount() - 1);
		ElementML lastParaML = lastPara.getElementML();
		lastParaML.delete();
		root.getElementML().getChild(0).addChild(lastParaML);
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

		public String getName() {
			ElementML elem = getElementML();
			if (elem != null) {
				return elem.toString();
			}
			return super.getName();
		}

		public ElementML getElementML() {
			return (ElementML) getAttribute(WordMLStyleConstants.ElementMLAttribute);
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
		
		public AttributeSet getResolveParent() {
			return null;
		}
		
		public void save() {
			;//TODO: Saving 
			log.debug("save(): this=" + this);
		}

	}// BlockElement inner class

    public class TextElement extends LeafElement implements DocumentElement {
		public TextElement(Element parent, AttributeSet a, int offs0, int offs1) {
			super(parent, a, offs0, offs1);
		}

		public ElementML getElementML() {
			return (ElementML) getAttribute(WordMLStyleConstants.ElementMLAttribute);
		}
		
		public boolean isEditable() {
			ElementML elemML = getElementML();
			
			boolean isEditable = !elemML.isDummy();
			if (isEditable) {
				DocumentElement parent = (DocumentElement) getParentElement();
				isEditable = parent.isEditable();
			}
			
			return isEditable;
		}
		
		public String getName() {
			ElementML elem = getElementML();
			if (elem != null) {
				return elem.toString();
			}
			return super.getName();
		}

		public void save() {
			;//TODO: Saving 
			log.debug("save(): this=" + this);
		}
	}// TextElement inner class
    
}// WordMLDocument class

