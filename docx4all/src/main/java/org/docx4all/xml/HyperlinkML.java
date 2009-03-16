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

package org.docx4all.xml;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import javax.swing.text.SimpleAttributeSet;

import net.sf.vfsjfilechooser.utils.VFSURIParser;
import net.sf.vfsjfilechooser.utils.VFSUtils;

import org.apache.commons.vfs.FileObject;
import org.apache.commons.vfs.FileSystemException;
import org.apache.log4j.Logger;
import org.docx4all.swing.text.WordMLStyleConstants;
import org.docx4all.util.XmlUtil;
import org.docx4j.XmlUtils;
import org.docx4j.openpackaging.parts.relationships.Namespaces;

/**
 *	@author Jojada Tirtowidjojo - 20/11/2008
 */
public class HyperlinkML extends ElementML {
	
	private static Logger log = Logger.getLogger(HyperlinkML.class);	
	
	public final static String encodeTarget(
		HyperlinkML ml, FileObject sourceFile, boolean inFriendlyFormat) {
		String target = ml.getTarget();
		if (target == null) {
			return null;
		}
		
		target = target.replace('\\', '/');
		int idx = target.indexOf("://");
		if (idx > 0) {
			//if protocol is specified, directly construct 
			//target by decoding it
			try {
				target = URLDecoder.decode(target, "UTF-8");
			} catch (UnsupportedEncodingException exc) {
				//should not happen
			}
			
			if (inFriendlyFormat) {
				//target should already be in friendly format.
			} else if (sourceFile != null) {
				String sourcePath = sourceFile.getName().getURI();
				if (sourcePath.startsWith("file://")
					|| target.startsWith("file://")) {
					//either sourcePath or target is local.
					//No need for user credentials
				} else {
					VFSURIParser parser = new VFSURIParser(sourcePath, false);
					String username = parser.getUsername();
					String password = parser.getPassword();
					
					StringBuilder sb = new StringBuilder();
					sb.append(target.substring(0, idx + 3));
					sb.append(username);
					sb.append(":");
					sb.append(password);
					sb.append("@");
					sb.append(target.substring(idx+3));
				}
			}
		} else if (sourceFile != null) {
			//protocol is NOT specified in target.
			//Construct target by appending target to 
			//sourceFile directory.
			String base = null;
			try {
				base = sourceFile.getParent().getName().getURI();
			} catch (FileSystemException exc) {
				;//ignore
			}
			if (base != null) {
				if (inFriendlyFormat) {
					base = VFSUtils.getFriendlyName(base, false);
				}
			
				StringBuilder sb = new StringBuilder();
				sb.append(base.replace('\\', '/'));
				if (!base.endsWith("/")) {
					sb.append("/");
				}
				if (target.startsWith("/")) {
					target = target.substring(1);
				}
				sb.append(target);
				target = sb.toString();
			}
		}
		
		return target;
	}
	
	private String dummyTarget;
	
	public HyperlinkML(Object docxObject) {
		this(docxObject, false);
		this.dummyTarget = null;
	}
	
	public HyperlinkML(Object docxObject, boolean isDummy) {
		super(docxObject, isDummy);
	}
	
	public String getId() {
		return ((org.docx4j.wml.P.Hyperlink) this.docxObject).getId();
	}
	
	public void setId(String id) {
		((org.docx4j.wml.P.Hyperlink) this.docxObject).setId(id);
	}
	
	public String getDummyTarget() {
		return this.dummyTarget;
	}
	
	public void setDummyTarget(String s) {
		this.dummyTarget = s;
	}
	
	public String getTarget() {
		String theTarget = null;
		
		org.docx4j.openpackaging.packages.WordprocessingMLPackage wmlPkg =
			getWordprocessingMLPackage();
		if (wmlPkg != null && wmlPkg.getMainDocumentPart() != null) {
			org.docx4j.openpackaging.parts.relationships.RelationshipsPart part = 
				wmlPkg.getMainDocumentPart().getRelationshipsPart();
			if (part != null) {
				org.docx4j.relationships.Relationship rel = 
					part.getRelationshipByID(getId());
				if (rel != null) {
					theTarget = rel.getTarget();
				}
			}
		}
		
		return theTarget;
	}
	
	public boolean canSetTarget() {
		boolean canSet = false;
		
		org.docx4j.openpackaging.packages.WordprocessingMLPackage wmlPkg =
			getWordprocessingMLPackage();
		if (wmlPkg != null && wmlPkg.getMainDocumentPart() != null) {
			org.docx4j.openpackaging.parts.relationships.RelationshipsPart part = 
				wmlPkg.getMainDocumentPart().getRelationshipsPart();
			canSet = (part != null);
		}
		
		return canSet;
	}
	
	public final static java.lang.CharSequence space = (new String(" ")).subSequence(0, 1);
	public final static java.lang.CharSequence encodedSpace = (new String("%20")).subSequence(0, 3);	
	
	public void setTarget(String s) {
		org.docx4j.openpackaging.packages.WordprocessingMLPackage wmlPkg =
			getWordprocessingMLPackage();
		if (wmlPkg != null && wmlPkg.getMainDocumentPart() != null) {
			org.docx4j.openpackaging.parts.relationships.RelationshipsPart part = 
				wmlPkg.getMainDocumentPart().getRelationshipsPart();
			if (part != null) {
				org.docx4j.relationships.Relationship rel = 
					part.getRelationshipByID(getId());
				if (rel == null) {
					log.debug("Creating new rel for hyperlink");
					org.docx4j.relationships.ObjectFactory factory =
						new org.docx4j.relationships.ObjectFactory();
					rel = factory.createRelationship();
					rel.setType( Namespaces.HYPERLINK  );
					
					// Word says the document is corrupt if @Target contains
					// spaces, so encode as %20
					if (s.indexOf(" ")>-1) {			               
			               s = s.replace(space, encodedSpace);					               
					}
					
					rel.setTarget(s);
					rel.setTargetMode("External");  
					part.addRelationship(rel);//id is auto generated
					setId(rel.getId()); //remember to do this.
				} else {
					rel.setTarget(s);
				}
			} else {
				throw new IllegalStateException("No RelationshipsPart");
			}
		} else {
			throw new IllegalStateException("No WordprocessingMLPackage nor MainDocumentPart");
		}
	}
	
	public String getTooltip() {
		return ((org.docx4j.wml.P.Hyperlink) this.docxObject).getTooltip();
	}
	
	public void setTooltip(String s) {
		((org.docx4j.wml.P.Hyperlink) this.docxObject).setTooltip(s);
	}
	
	public void setDisplayText(String s) {
		RunML runML = getRunMLChild();
		if (runML == null) {
			runML = new RunML(ObjectFactory.createR(s));
			RunPropertiesML rPr = createRunPropertiesML();
			runML.setRunProperties(rPr);
			addChild(runML);
			
		} else {
			if (runML.getRunProperties() == null) {
				RunPropertiesML rPr = createRunPropertiesML();
				runML.setRunProperties(rPr);
			}
			RunContentML ml = XmlUtil.getFirstRunContentML(runML);
			if (ml == null) {
				ml = new RunContentML(ObjectFactory.createT(s));
				runML.addChild(ml);
			} else {
				ml.setTextContent(s);
			}
		}
	}
	
	public String getDisplayText() {
		RunContentML ml = XmlUtil.getFirstRunContentML(this);
		return (ml == null) ? null : ml.getTextContent();
	}
	
	private RunML getRunMLChild() {
		RunML theChild = null;
		if (getChildren() != null) {
			for (ElementML ml: getChildren()) {
				if (ml instanceof RunML) {
					theChild = (RunML) ml;
					break;
				}
			}
		}
		return theChild;
	}
	
	private RunPropertiesML createRunPropertiesML() {
		SimpleAttributeSet attrs = new SimpleAttributeSet();
		attrs.addAttribute(WordMLStyleConstants.RStyleAttribute, "Hyperlink");
		return ElementMLFactory.createRunPropertiesML(attrs);
	}
	
	public boolean canAddSibling(ElementML elem, boolean after) {
		boolean canAdd = false;
		
		if (elem instanceof RunML 
			|| elem instanceof RunDelML 
			|| elem instanceof RunDelML
			|| elem instanceof HyperlinkML) {
			canAdd = super.canAddSibling(elem, after);
		}
		
		return canAdd;
	}
	
	public Object clone() {
		Object obj = null;
		if (this.docxObject != null) {
			obj = XmlUtils.deepCopy(this.docxObject);
		}
		
		return new HyperlinkML(obj, this.isDummy);
	}
	
	public boolean canAddChild(int idx, ElementML child) {
		boolean canAdd = true;
		
		if (!(child instanceof RunML)) {
			canAdd = false;
		} else {
			canAdd = super.canAddChild(idx, child);
		}
		
		return canAdd;
	}
	
	public void addChild(int idx, ElementML child, boolean adopt) {
		if (!(child instanceof RunML)) {
			throw new IllegalArgumentException("NOT a RunML");
		}
		if (child.getParent() != null) {
			throw new IllegalArgumentException("Not an orphan.");
		}
		super.addChild(idx, child, adopt);
	}
		
	public void setParent(ElementML parent) {
		if (parent != null && !(parent instanceof ParagraphML)) {
			throw new IllegalArgumentException("NOT a ParagraphML.");
		}
		this.parent = parent;
	}
	
	protected List<Object> getDocxChildren() {
		List<Object> theChildren = null;
		
		if (this.docxObject instanceof org.docx4j.wml.P.Hyperlink) {
			theChildren = ((org.docx4j.wml.P.Hyperlink) this.docxObject).getParagraphContent();
		}
		
		return theChildren;
	}
	
	protected void init(Object docxObject) {
		org.docx4j.wml.P.Hyperlink link = null;
		
		if (docxObject == null) {
			;// implied HyperlinkML
			
		} else if (docxObject instanceof org.docx4j.wml.P.Hyperlink) {
			link = (org.docx4j.wml.P.Hyperlink) docxObject;
			this.isDummy = false;
			
		} else {
			throw new IllegalArgumentException("Unsupported Docx Object = "
					+ docxObject);
		}

		initChildren(link);
	}
	
	private void initChildren(org.docx4j.wml.P.Hyperlink link) {
		this.children = null;
		
		if (link == null) {
			return;
		}
		
		List<Object> pKids = link.getParagraphContent();
		if (!pKids.isEmpty()) {
			this.children = new ArrayList<ElementML>(pKids.size());
			
			for (Object o : pKids) {
				RunML run = new RunML(o, this.isDummy);
				run.setParent(HyperlinkML.this);
				this.children.add(run);				
			}
		}
	}// initChildren()
	
}// HyperlinkML class



















