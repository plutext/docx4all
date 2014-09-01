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
import java.awt.Toolkit;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.TabStop;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBIntrospector;
import javax.xml.namespace.QName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.docx4all.xml.ObjectFactory;
import org.docx4j.XmlUtils;
import org.docx4j.fonts.RunFontSelector;
import org.docx4j.jaxb.Context;
import org.docx4j.model.PropertyResolver;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.wml.BooleanDefaultTrue;
import org.docx4j.wml.CTHeight;
import org.docx4j.wml.CTShd;
import org.docx4j.wml.CTTabStop;
import org.docx4j.wml.CTTblCellMar;
import org.docx4j.wml.CTTrPrBase;
import org.docx4j.wml.CTVerticalJc;
import org.docx4j.wml.HpsMeasure;
import org.docx4j.wml.Jc;
import org.docx4j.wml.JcEnumeration;
import org.docx4j.wml.PPr;
import org.docx4j.wml.PPrBase;
import org.docx4j.wml.RFonts;
import org.docx4j.wml.RPr;
import org.docx4j.wml.RStyle;
import org.docx4j.wml.STBorder;
import org.docx4j.wml.STShd;
import org.docx4j.wml.STTabJc;
import org.docx4j.wml.STTabTlc;
import org.docx4j.wml.Tabs;
import org.docx4j.wml.TblBorders;
import org.docx4j.wml.TblPr;
import org.docx4j.wml.TblWidth;
import org.docx4j.wml.TcMar;
import org.docx4j.wml.TcPr;
import org.docx4j.wml.TcPrInner;
import org.docx4j.wml.TrPr;
import org.docx4j.wml.U;
import org.docx4j.wml.PPrBase.PStyle;
import org.docx4j.wml.Styles.LatentStyles.LsdException;
import org.w3c.dom.Document;

/**
 *	@author Jojada Tirtowidjojo - 19/02/2008
 */
public class StyleSheet extends StyleContext {
	private static Logger log = LoggerFactory.getLogger(StyleSheet.class);

	public final static String LATENT_STYLES = "latentStyles";
	public final static String LSD_EXCEPTIONS = "lsdExceptions";
	public final static String UI_STYLES = "uiStyles";
	public final static String ID_STYLES = "idStyles";
	
	public final static String PARAGRAPH_ATTR_VALUE = "paragraph";
	public final static String CHARACTER_ATTR_VALUE = "character";
	
	//Screen resolution is in dots per inch (dpi)
	private final static int SCREEN_RESOLUTION_DPI = 
		Toolkit.getDefaultToolkit().getScreenResolution();
	
    private static StyleSheet defaultStyleSheet;
    
    public static final int toPixels(int twips) {
    	//one inch = 1440 twips = dpi pixels.
    	//Therefore, one twip = dpi/1440 pixels.
    	float conversionFactor = SCREEN_RESOLUTION_DPI / 1440f;
    	return (int) (twips * conversionFactor + 0.5f);
    }
    
    public static final int toTwips(int pixels) {
    	//one inch = 1440 twips = dpi pixels.
    	//Therefore, one twip = dpi/1440 pixels.
    	float conversionFactor = 1440f / SCREEN_RESOLUTION_DPI;
    	return (int) (pixels * conversionFactor);
    }
    
    private static final int toSwingTabStopAlignment(STTabJc stTabJc) {
    	int align = TabStop.ALIGN_LEFT;
    	if (stTabJc == STTabJc.BAR) {
    		align = TabStop.ALIGN_BAR;
    	} else if (stTabJc == STTabJc.CENTER) {
    		align = TabStop.ALIGN_CENTER;
    	} else if (stTabJc == STTabJc.DECIMAL) {
    		align = TabStop.ALIGN_DECIMAL;
    	} else if (stTabJc == STTabJc.LEFT) {
    		//align = TabStop.ALIGN_LEFT;
    	} else if (stTabJc == STTabJc.RIGHT) {
    		align = TabStop.ALIGN_RIGHT;
    	} else {
    		//not supported. 
    		//Default to TabStop.ALIGN_LEFT
    	}
    	
    	return align;
    }
    
    private static final int toSwingTabStopLeader(STTabTlc stTabTlc) {
    	int lead = TabStop.LEAD_NONE;
    	if (stTabTlc == STTabTlc.DOT) {
    		lead = TabStop.LEAD_DOTS;
    	} else if (stTabTlc == STTabTlc.HEAVY) {
    		lead = TabStop.LEAD_THICKLINE;
    	} else if (stTabTlc == STTabTlc.HYPHEN) {
    		lead = TabStop.LEAD_HYPHENS;
    	} else if (stTabTlc == STTabTlc.NONE) {
    		//lead = TabStop.LEAD_NONE;
    	} else if (stTabTlc == STTabTlc.UNDERSCORE) {
    		lead = TabStop.LEAD_UNDERLINE;
    	} else {
    		//not supported STTabTlc.MIDDLE_DOT
    		//Default to TabStop.LEAD_NONE
    	}
    	return lead;
    }
    
    public static final int emuToTwips(int emus) {
    	//one emu = 1/914400 inch
    	//one inch = 1440 twips
    	float conversionFactor = 1440f / 914400;
    	return (int) (emus * conversionFactor);
    }
    
    public static final int emuToPixels(int emus) {
    	return toPixels(emuToTwips(emus));
    }
    
    public static final StyleSheet getDefaultStyleSheet() {
    	log.info("");
        if (defaultStyleSheet == null) {
        	defaultStyleSheet = new StyleSheet();
        	log.info("creating empty document in order to read default styles");
        	defaultStyleSheet.setWordprocessingMLPackage(ObjectFactory.createEmptyDocumentPackage());
        	log.info("\n\n .. done .. created empty document ");
        }
        return defaultStyleSheet;
    }

	public final static void addAttributes(MutableAttributeSet attrs, TblPr tblPr) {
		//ALIGNMENT attribute
		Jc jc = tblPr.getJc();
		addJc(attrs, jc);
		jc = null;
		
		TblBorders tblBorders = tblPr.getTblBorders();
		if (tblBorders != null) {
			org.docx4all.xml.type.TblBorders value = 
				new org.docx4all.xml.type.TblBorders(tblBorders);
			attrs.addAttribute(WordMLStyleConstants.TblBordersAttribute, value);
			tblBorders = null;
		} else {
			//this table shall have the borders specified by 
			//the associated table style. 
			//If no borders are specified in the style hierarchy, 
			//then this table shall not have any table borders
			
			//TODO: Remove this TRIAL TblBorders setting
			Double sz = Double.valueOf(toTwips(1) * 20/8 + 0.5);
			
			org.docx4j.wml.CTBorder left = org.docx4all.xml.type.ObjectFactory.createCTBorder();
			left.setColor("auto");
			left.setSz(BigInteger.valueOf(sz.longValue()));
			left.setVal(STBorder.SINGLE);
			
			org.docx4j.wml.CTBorder top = org.docx4all.xml.type.ObjectFactory.createCTBorder();
			top.setColor("auto");
			top.setSz(BigInteger.valueOf(sz.longValue()));
			top.setVal(STBorder.SINGLE);
			
			org.docx4j.wml.CTBorder right = org.docx4all.xml.type.ObjectFactory.createCTBorder();
			right.setColor("auto");
			right.setSz(BigInteger.valueOf(sz.longValue()));
			right.setVal(STBorder.SINGLE);

			org.docx4j.wml.CTBorder bottom = org.docx4all.xml.type.ObjectFactory.createCTBorder();
			bottom.setColor("auto");
			bottom.setSz(BigInteger.valueOf(sz.longValue()));
			bottom.setVal(STBorder.SINGLE);
			
			org.docx4j.wml.CTBorder insideH = org.docx4all.xml.type.ObjectFactory.createCTBorder();
			insideH.setColor("auto");
			insideH.setSz(BigInteger.valueOf(sz.longValue()));
			insideH.setVal(STBorder.SINGLE);
			
			org.docx4j.wml.CTBorder insideV = org.docx4all.xml.type.ObjectFactory.createCTBorder();
			insideV.setColor("auto");
			insideV.setSz(BigInteger.valueOf(sz.longValue()));
			insideV.setVal(STBorder.SINGLE);
			
			tblBorders = 
				org.docx4all.xml.type.ObjectFactory.createTblBorders(
					left, top, right, bottom, insideH, insideV);
			org.docx4all.xml.type.TblBorders value = 
				new org.docx4all.xml.type.TblBorders(tblBorders);
			attrs.addAttribute(WordMLStyleConstants.TblBordersAttribute, value);
			tblBorders = null;			
		}
		
		TblWidth tw = tblPr.getTblInd();
		if (tw != null) {
			org.docx4all.xml.type.TblWidth indent = 
				new org.docx4all.xml.type.TblWidth(tw);
			if (indent.getType() == org.docx4all.xml.type.TblWidth.Type.DXA
				|| indent.getType() == org.docx4all.xml.type.TblWidth.Type.NIL) {
				int ignoreThisParam = 0;
				int pixels = indent.getWidthInPixel(ignoreThisParam);
				attrs.addAttribute(WordMLStyleConstants.TblIndentAttribute, Integer.valueOf(pixels));
			} else {
				//WordprocessingML Spec says to ignore.
			}
		}
		
		
		tw = tblPr.getTblW();
		if (tw != null) {
			org.docx4all.xml.type.TblWidth tblWidth = 
				new org.docx4all.xml.type.TblWidth(tw);
			attrs.addAttribute(WordMLStyleConstants.TblWidthAttribute, tblWidth);
			tw = null;
		}
		
		CTTblCellMar tmar = tblPr.getTblCellMar();
		if (tmar != null) {
			tw = tmar.getLeft();
			if (tw != null) {
				org.docx4all.xml.type.TblWidth left =
					new org.docx4all.xml.type.TblWidth(tw);
				if (left.getType() == org.docx4all.xml.type.TblWidth.Type.DXA
					|| left.getType() == org.docx4all.xml.type.TblWidth.Type.NIL) {
					attrs.addAttribute(WordMLStyleConstants.TcLeftMarginAttribute, left);
				} else {
					//WordprocessingML Spec says to ignore.
				}
			}
			
			tw = tmar.getRight();
			if (tw != null) {
				org.docx4all.xml.type.TblWidth right =
					new org.docx4all.xml.type.TblWidth(tw);
				if (right.getType() == org.docx4all.xml.type.TblWidth.Type.DXA
					|| right.getType() == org.docx4all.xml.type.TblWidth.Type.NIL) {
					attrs.addAttribute(WordMLStyleConstants.TcLeftMarginAttribute, right);
				} else {
					//WordprocessingML Spec says to ignore.
				}
			}
			
			tw = tmar.getTop();
			if (tw != null) {
				org.docx4all.xml.type.TblWidth top =
					new org.docx4all.xml.type.TblWidth(tw);
				if (top.getType() == org.docx4all.xml.type.TblWidth.Type.DXA
					|| top.getType() == org.docx4all.xml.type.TblWidth.Type.NIL) {
					attrs.addAttribute(WordMLStyleConstants.TcLeftMarginAttribute, top);
				} else {
					//WordprocessingML Spec says to ignore.
				}
			}
			
			tw = tmar.getBottom();
			if (tw != null) {
				org.docx4all.xml.type.TblWidth bottom =
					new org.docx4all.xml.type.TblWidth(tw);
				if (bottom.getType() == org.docx4all.xml.type.TblWidth.Type.DXA
					|| bottom.getType() == org.docx4all.xml.type.TblWidth.Type.NIL) {
					attrs.addAttribute(WordMLStyleConstants.TcLeftMarginAttribute, bottom);
				} else {
					//WordprocessingML Spec says to ignore.
				}
			}
		} //if (tmar != null)
		
		tw = tblPr.getTblCellSpacing();
		if (tw != null) {
			org.docx4all.xml.type.TblWidth space =
				new org.docx4all.xml.type.TblWidth(tw);
			if (space.getType() == org.docx4all.xml.type.TblWidth.Type.DXA
				|| space.getType() == org.docx4all.xml.type.TblWidth.Type.NIL) {
				int ignoreThisParam = 0;
				int pixels = space.getWidthInPixel(ignoreThisParam);
				attrs.addAttribute(WordMLStyleConstants.TblCellSpacingAttribute, pixels);
			} else {
				//WordprocessingML Spec says to ignore.
			}
		}
	} //addAttributes(attrs, tblPr)
	
	public final static void addAttributes(MutableAttributeSet attrs, TrPr trPr) {
		JAXBIntrospector inspector = Context.jc.createJAXBIntrospector();
		
		List<JAXBElement<?>> list = trPr.getCnfStyleOrDivIdOrGridBefore();
		for (JAXBElement<?> elem: list) {
			Object value = JAXBIntrospector.getValue(elem);
			if (value instanceof Jc) {
				addJc(attrs, (Jc) value);
			}
			if (value instanceof CTHeight) {
				org.docx4all.xml.type.CTHeight trHeight =
					new org.docx4all.xml.type.CTHeight((CTHeight) value);
				attrs.addAttribute(WordMLStyleConstants.TrHeightAttribute, trHeight);
			}
			if (value instanceof CTTrPrBase.GridAfter) {
				CTTrPrBase.GridAfter ga = (CTTrPrBase.GridAfter) value;
				attrs.addAttribute(WordMLStyleConstants.TrGridAfterAttribute, ga.getVal().intValue());
			}
			if (value instanceof CTTrPrBase.GridBefore) {
				CTTrPrBase.GridBefore gb = (CTTrPrBase.GridBefore) value;
				attrs.addAttribute(WordMLStyleConstants.TrGridBeforeAttribute, gb.getVal().intValue());
			}
			if (value instanceof TblWidth) {
				QName qn = inspector.getElementName(elem);
				if ("wAfter".equals(qn.getLocalPart())) {
					attrs.addAttribute(WordMLStyleConstants.TrWAfterAttribute, value);
				}
				if ("wBefore".equals(qn.getLocalPart())) {
					attrs.addAttribute(WordMLStyleConstants.TrWBeforeAttribute, value);
				}
			}
		}
	}
	
	public final static void addAttributes(MutableAttributeSet attrs, TcPr tcPr) {
		TcPrInner.GridSpan gs = tcPr.getGridSpan();
		if (gs != null && gs.getVal() != null) {
			attrs.addAttribute(WordMLStyleConstants.TcGridSpanAttribute, gs);
		}
		gs = null;
		
		TcPrInner.VMerge vm = tcPr.getVMerge();
		if (vm != null) {
			attrs.addAttribute(WordMLStyleConstants.TcVMergeAttribute, vm);
		}
		vm = null;
		
		TcPrInner.TcBorders tcBorders = tcPr.getTcBorders();
		if (tcBorders != null) {
			org.docx4all.xml.type.TcBorders value = 
				new org.docx4all.xml.type.TcBorders(tcBorders);
			attrs.addAttribute(WordMLStyleConstants.TcBordersAttribute, value);
			tcBorders = null;
		} else {
			//this cell shall honour the borders specified by 
			//its ancestors.
		}
		
		
		TblWidth tw = tcPr.getTcW();
		//TODO: Monitor whether there will be TcWidth object in docx4j library
		if (tw != null) {
			org.docx4all.xml.type.TblWidth tcWidth = 
				new org.docx4all.xml.type.TblWidth(tw);
			attrs.addAttribute(WordMLStyleConstants.TcWidthAttribute, tcWidth);
			tw = null;
		}
		
		TcMar tcMar = tcPr.getTcMar();
		if (tcMar != null) {
			if (tcMar.getLeft() != null) {
				org.docx4all.xml.type.TblWidth w = 
					new org.docx4all.xml.type.TblWidth(tcMar.getLeft());
				if (w.getType() == org.docx4all.xml.type.TblWidth.Type.DXA
					|| w.getType() == org.docx4all.xml.type.TblWidth.Type.NIL) {
					attrs.addAttribute(WordMLStyleConstants.TcLeftMarginAttribute, w);
				} else {
					//WordprocessingML Spec says to ignore.
				}
			} else {
				//honour the left cell margin specified in ancestors.
			}
			if (tcMar.getRight() != null) {
				org.docx4all.xml.type.TblWidth w = 
					new org.docx4all.xml.type.TblWidth(tcMar.getRight());
				if (w.getType() == org.docx4all.xml.type.TblWidth.Type.DXA
					|| w.getType() == org.docx4all.xml.type.TblWidth.Type.NIL) {
					attrs.addAttribute(WordMLStyleConstants.TcRightMarginAttribute, w);
				} else {
					//WordprocessingML Spec says to ignore.
				}
			} else {
				//honour the right cell margin specified in ancestors.
			}
			if (tcMar.getTop() != null) {
				org.docx4all.xml.type.TblWidth w = 
					new org.docx4all.xml.type.TblWidth(tcMar.getTop());
				if (w.getType() == org.docx4all.xml.type.TblWidth.Type.DXA
					|| w.getType() == org.docx4all.xml.type.TblWidth.Type.NIL) {
					attrs.addAttribute(WordMLStyleConstants.TcTopMarginAttribute, w);
				} else {
					//WordprocessingML Spec says to ignore.
				}
			} else {
				//honour the top cell margin specified in ancestors.
			}
			if (tcMar.getBottom() != null) {
				org.docx4all.xml.type.TblWidth w = 
					new org.docx4all.xml.type.TblWidth(tcMar.getBottom());
				if (w.getType() == org.docx4all.xml.type.TblWidth.Type.DXA
					|| w.getType() == org.docx4all.xml.type.TblWidth.Type.NIL) {
					attrs.addAttribute(WordMLStyleConstants.TcBottomMarginAttribute, w);
				} else {
					//WordprocessingML Spec says to ignore.
				}
			} else {
				//honour the bottom cell margin specified in ancestors.
			}
			tcMar = null;
		} else {
			//honour the table cell margins specified in ancestors.
		}
		
		CTVerticalJc valign = tcPr.getVAlign();
		if (valign != null) {
			attrs.addAttribute(WordMLStyleConstants.TcVAlignAttribute, valign.getVal());
			valign = null;
		}
		
		CTShd ctShd = tcPr.getShd();
		if (ctShd != null) {
			//Currently we are still supporting solid background color.
			//Solid background color is set if 
			// - ctShd.getVal() is null; ie: @val attribute is omitted
			// - ctShd.getVal() is clear or nil.
			STShd val = ctShd.getVal();
			if (val == null || val == STShd.CLEAR || val == STShd.NIL) {
				//TODO: Make ctShd.getThemeFill() supersede ctShd.getFill()
				//if (ctShd.getThemeFill() == null) {
					String s = ctShd.getFill();
					if ("auto".equalsIgnoreCase(s)) {
						;//ignore
					} else {
						try {
							Color c = new Color(Integer.parseInt(s, 16));
							attrs.addAttribute(StyleConstants.Background, c);
						} catch (NumberFormatException exc) {
							;//ignore
						}
					}
				//} else {
					//not supported yet.
					//ctShd.getThemeFill() supersedes ctShd.getFill()
				//}
			} else {
				//not supported yet
			}
		}
	} //addAttributes(attrs, tcPr)
	
	public final static void addAttributes(MutableAttributeSet attrs, PPr pPr) {
		//ALIGNMENT attribute
		Jc jc = pPr.getJc();
		addJc(attrs, jc);
		jc = null;
		
		PStyle pStyle = pPr.getPStyle();
		if (pStyle != null) {
			attrs.addAttribute(WordMLStyleConstants.PStyleAttribute, pStyle.getVal());
		}
		pStyle = null;
		
		PPrBase.NumPr numPr = pPr.getNumPr();
		if (numPr != null) {
			attrs.addAttribute(WordMLStyleConstants.NumPrAttribute, numPr);
		}
		numPr = null;
		
		addIndentationAttributes(attrs, pPr);
		addSpacingAttributes(attrs, pPr);
		addTabsAttributes(attrs, pPr);
	}
	
	public final static void addIndentationAttributes(MutableAttributeSet attrs, PPr pPr) {
		PPrBase.Ind ind = pPr.getInd();
		if (ind != null) {
			if (ind.getHanging() != null) {
				int i = toPixels(ind.getHanging().intValue());
				StyleConstants.setFirstLineIndent(attrs, -i);
			} else if (ind.getFirstLineChars() != null) {
				;// TODO: Support firstLineChars attr of paragraph indentation
			} else if (ind.getFirstLine() != null) {
				int i = toPixels(ind.getFirstLine().intValue());
				StyleConstants.setFirstLineIndent(attrs, i);
			}

			if (ind.getLeftChars() != null) {
				;// TODO: Support leftChars attr of paragraph indentation
			} else if (ind.getLeft() != null) {
				int i = toPixels(ind.getLeft().intValue());
				StyleConstants.setLeftIndent(attrs, i);
			}

			if (ind.getRightChars() != null) {
				;// TODO: Support rightChars attr of paragraph indentation
			} else if (ind.getRight() != null) {
				int i = toPixels(ind.getRight().intValue());
				StyleConstants.setRightIndent(attrs, i);
			}
		}
	}
	
	public final static void addSpacingAttributes(MutableAttributeSet attrs, PPr pPr) {
		PPrBase.Spacing spacing = pPr.getSpacing();
		if (spacing != null) {
			//if (spacing.getAfterLines() != null
			//	|| spacing.isAfterAutospacing()) {
				//WordprocessingML Spec says to ignore spacing.getAfter()
				//if spacing.getAfterLines() is specified or 
				//spacing.isAfterAutospacing() is true
				//TODO: Support afterLines and afterAutoSpacing
			//} else {
				if (spacing.getAfter() != null) {
					int i = toPixels(spacing.getAfter().intValue());
					StyleConstants.setSpaceBelow(attrs, i);
				}
			//}
			
			//if (spacing.getBeforeLines() != null
			//	|| spacing.isBeforeAutospacing()) {
				//WordprocessingML Spec says to ignore spacing.getBefore()
				//if spacing.getBeforeLines() is specified or 
				//spacing.isBeforeAutospacing() is true
				//TODO: Support beforeLines and beforeAutoSpacing
			//} else {
				if (spacing.getBefore() != null) {
					int i = toPixels(spacing.getBefore().intValue());
					StyleConstants.setSpaceAbove(attrs, i);
				}
			//}
			
			if (spacing.getLine() != null) {
				//TODO: Support interline spacing
				/*
				STLineSpacingRule rule = 
					(spacing.getLineRule() == null)
						? STLineSpacingRule.AUTO
						: spacing.getLineRule();
				attrs.addAttribute(WordMLStyleConstants.STLineSpacingRuleAttribute, rule);
				*/
			}
		}	
	}
	
	public final static void addTabsAttributes(MutableAttributeSet attrs, PPr pPr) {
		Tabs tabs = pPr.getTabs();
		if (tabs != null) {
			List<CTTabStop> list = tabs.getTab();
			if (!list.isEmpty()) {
				javax.swing.text.TabStop[] tabStops =
					new javax.swing.text.TabStop[list.size()];
				for (Integer i=0; i < list.size(); i++) {
					CTTabStop ctts = list.get(i);
					BigInteger pos = ctts.getPos();
					STTabTlc leader = ctts.getLeader();
					STTabJc val = ctts.getVal();
				
					tabStops[i] = 
						new javax.swing.text.TabStop(
							toPixels(pos.intValue()),
							toSwingTabStopAlignment(val),
							toSwingTabStopLeader(leader));
				}
			
				javax.swing.text.TabSet tabSet =  
					new javax.swing.text.TabSet(tabStops);
				StyleConstants.setTabSet(attrs, tabSet);
			}
		}
	}
	
	public final static void addAttributes(MutableAttributeSet attrs, RPr rPr) {
		//BOLD Attribute
		BooleanDefaultTrue bdt = rPr.getB();
		if (bdt != null) {
			StyleConstants.setBold(attrs, Boolean.valueOf(bdt.isVal()));
		}

		//ITALIC Attribute
		bdt = rPr.getI();
		if (bdt != null) {
			StyleConstants.setItalic(attrs, Boolean.valueOf(bdt.isVal()));
		}
		
		//UNDERLINE Attribute
		//TODO: To support underline style and color
		if (hasUnderlineSet(rPr)) {
			StyleConstants.setUnderline(attrs, Boolean.TRUE);
		}
		
		//FONT FAMILY Attribute
		RFonts rfonts = rPr.getRFonts();
		if (rfonts != null) {
			String strValue = rfonts.getAscii();
			if (strValue != null) {
				StyleConstants.setFontFamily(attrs, strValue);
			}
		}
		
		//FONT SIZE Attribute
		HpsMeasure sz = rPr.getSz();
		if (sz != null && sz.getVal() != null) {
			StyleConstants.setFontSize(attrs, sz.getVal().intValue());
		}
		
		RStyle rStyle = rPr.getRStyle();
		if (rStyle != null) {
			attrs.addAttribute(WordMLStyleConstants.RStyleAttribute, rStyle.getVal());
		}
	}
	
	public final static boolean hasUnderlineSet(RPr rPr) {
		
		U u = rPr.getU();
		if (u == null) {
			return false;
		} else {
			if (u.getVal()==null) {
				// This does happen eg <w:u w:color="FF0000"/>
				return true;
			} else {
				String s = u.getVal().value();
				//for (String s : u.getVal()) {
					if (s.equalsIgnoreCase("none")) {
						return false;
					}
				//}
				return true;
			}
		}
	}

	private final static void addJc(MutableAttributeSet attrs, Jc jc) {
		if (jc == null) {
			return;
		}
		if (jc.getVal() == JcEnumeration.LEFT) {
			StyleConstants.setAlignment(
					attrs,
					StyleConstants.ALIGN_LEFT);
		} else if (jc.getVal() == JcEnumeration.RIGHT) {
			StyleConstants.setAlignment(
					attrs,
					StyleConstants.ALIGN_RIGHT);
		} else if (jc.getVal() == JcEnumeration.CENTER) {
			StyleConstants.setAlignment(
					attrs,
					StyleConstants.ALIGN_CENTER);
		} else if (jc.getVal() == JcEnumeration.BOTH) {
			StyleConstants.setAlignment(
					attrs,
					StyleConstants.ALIGN_JUSTIFIED);
		}
	}
	
	private WordprocessingMLPackage docPackage;
	
	public StyleSheet() {
		super();
	}
	
	public void setWordprocessingMLPackage(WordprocessingMLPackage docPackage) {
		this.docPackage = docPackage;
		
		//Supply font info before initialising styles
		FontManager.getInstance().addFontsInUse(docPackage);
		
		org.docx4j.wml.Styles docxStyles = 
			(org.docx4j.wml.Styles)
				docPackage.getMainDocumentPart().getStyleDefinitionsPart().getJaxbElement();
		initDefaultStyle(docxStyles);
		initLatentStyles(docxStyles);
		initStyles(docxStyles);
	}
	
	public WordprocessingMLPackage getWordprocessingMLPackage() {
		return this.docPackage;
	}
	
	public List<Style> getUIStyles() {
		Style uiStyles = getStyle(UI_STYLES);
		
		List<Style> theList = new ArrayList<Style>(uiStyles.getAttributeCount());
		
		Enumeration<?> tenum = uiStyles.getAttributeNames();
		while (tenum.hasMoreElements()) {
			Object obj = tenum.nextElement();
			
			obj = uiStyles.getAttribute(obj.toString());
			if (obj instanceof Style) {
				theList.add((Style) obj);
			}
		}
		
		Collections.sort(theList, new UIStyleComparator());
		return theList;
	}
	
	public String[] getUIStyleNames() {
		List<Style> uiStyles = getUIStyles();
		String[] theNames = new String[uiStyles.size()];
		int i=0;
		for (Style s: uiStyles) {
			theNames[i++] = s.getName();
		}
		return theNames;
	}
	
	public Style getUIStyle(String styleUiName) {
		Style s = getStyle(UI_STYLES);
		return getChildStyle(s, styleUiName);
	}
	
	public Style getIDStyle(String styleId) {
		Style s = getStyle(ID_STYLES);
		return getChildStyle(s, styleId);
	}
	
	public Style getReferredStyle(String name) {
		Style s = getUIStyle(name);
		if (s == null) {
			s = getIDStyle(name);
		}
		return s;
	}
	
	protected void initDefaultStyle(org.docx4j.wml.Styles docxStyles) {
		Style defaultStyle = getStyle(DEFAULT_STYLE);
		
		StyleConstants.setFontFamily(
				defaultStyle, 
				FontManager.getInstance().getDocx4AllDefaultFontFamilyName());
		StyleConstants.setFontSize(
				defaultStyle, 
				FontManager.getInstance().getDocx4AllDefaultFontSize());
		
		if (docxStyles.getDocDefaults()!=null &&
				docxStyles.getDocDefaults().getPPrDefault() != null) {
			PPr pPr = docxStyles.getDocDefaults().getPPrDefault().getPPr();
			if (pPr != null) {
				StyleSheet.addAttributes(defaultStyle, pPr);
			}
		}
		
		if (docxStyles.getDocDefaults()!=null &&
				docxStyles.getDocDefaults().getRPrDefault() != null) {
			RPr rPr = docxStyles.getDocDefaults().getRPrDefault().getRPr();
			if (rPr != null) {
				StyleSheet.addAttributes(defaultStyle, rPr);
			}
		}
		
		if (log.isDebugEnabled()) {
			log.debug("initDefaultStyle(): defaultStyle=" + defaultStyle);
		}
	}
	
	protected void initLatentStyles(org.docx4j.wml.Styles docxStyles) {
		Style defaultStyle = getStyle(DEFAULT_STYLE);
		Style latentStyles = addStyle(LATENT_STYLES, defaultStyle);
		
		org.docx4j.wml.Styles.LatentStyles latent = docxStyles.getLatentStyles();
		
		if (latent == null) {
			return;
		}
				
		//Currently still supporting uipriority and qformat attributes.
		latentStyles.addAttribute(
			WordMLStyleConstants.DocxObjectAttribute,
			latent);
		latentStyles.addAttribute(
			WordMLStyleConstants.UiPriorityAttribute,
			(latent.getDefUIPriority() == null) 
				? new Integer(99)
				: new Integer(latent.getDefUIPriority().intValue()));
		latentStyles.addAttribute(
			WordMLStyleConstants.QFormatAttribute,
			Boolean.valueOf(latent.isDefQFormat()));
		
		List<LsdException> list = latent.getLsdException();
		if (list != null) {
			Style lsdExceptions = addChildStyle(latentStyles, LSD_EXCEPTIONS);
			
			int i=0;
			for (LsdException lsd: list) {
				//An <lsdException> specifies the style name 
				//and NOT the style Id.
				Style temp = addChildStyle(lsdExceptions, lsd.getName());
				temp.addAttribute(
					WordMLStyleConstants.DocxObjectAttribute, lsd);
				
				if (lsd.getUiPriority() != null) {
					temp.addAttribute(
						WordMLStyleConstants.UiPriorityAttribute,
						new Integer(lsd.getUiPriority().intValue()));
				}
				temp.addAttribute(
					WordMLStyleConstants.QFormatAttribute,
					Boolean.valueOf(lsd.isQFormat()));
				
				if (log.isDebugEnabled()) {
					log.debug("initLatentStyles(): LsdException Style[" + (i++) +"]=" + temp);
				}
			}
		}
	}
	
	protected void initStyles(org.docx4j.wml.Styles docxStyles) {
		List<org.docx4j.wml.Style> styleList = docxStyles.getStyle();
		if (styleList.isEmpty()) {
			return;
		}
		
		Style defaultStyle = getStyle(DEFAULT_STYLE);
		Style latentStyles = getStyle(LATENT_STYLES);
		Style lsdExceptions = getChildStyle(latentStyles, LSD_EXCEPTIONS);
		
		Style idStyles = addStyle(ID_STYLES, defaultStyle);
		Style uiStyles = addStyle(UI_STYLES, defaultStyle);
		
		int i=0;
		List<Style> stylesWithBasedOn = new ArrayList<Style>();
		for (org.docx4j.wml.Style st: styleList) {			
			//Latent styles comprises those styles known to an application.
			//Therefore, all styles in styleList are descendants of latentStyles.
			Style tmpStyle = null;
			String uiName = st.getStyleId();
			if (st.getName() != null) {
				//Check whether st is listed in lsdExceptions.
				//Remember that a <lsdException> specifies the style name 
				//and NOT the style Id.
				uiName = st.getName().getVal();
				tmpStyle = getChildStyle(lsdExceptions, uiName);
			}
			if (tmpStyle == null) {
				tmpStyle = new NamedStyle(st.getStyleId(), latentStyles);
			}
			
			tmpStyle.addAttribute(
					WordMLStyleConstants.StyleUINameAttribute, uiName);
			tmpStyle.addAttribute(
					WordMLStyleConstants.DocxObjectAttribute, st);
			tmpStyle.addAttribute(
					WordMLStyleConstants.StyleIdAttribute, st.getStyleId());
			idStyles.addAttribute(st.getStyleId(), tmpStyle);
			
			if (st.getPPr() != null) {
				StyleSheet.addAttributes(tmpStyle, st.getPPr());
			}
			if (st.getRPr() != null) {
				StyleSheet.addAttributes(tmpStyle, st.getRPr());
			}
			
			//Override font family name setting with that provided by Docx4j.
			//Docx4j tries to mimic the algorithm of MS Word 2007 in finding font name from style.
			//String tmpStr = getWordprocessingMLPackage().getMainDocumentPart().getPropertyResolver().getFontnameFromStyle(st);
			
			String tmpStr = null;
    		RPr pRPr = getWordprocessingMLPackage().getMainDocumentPart().getPropertyResolver().getEffectiveRPr(st.getStyleId());
			if (pRPr!=null
					&& pRPr.getRFonts()!=null) {
				
				// 3.2.0 simple approach for now
				tmpStr= pRPr.getRFonts().getAscii();
			}
			
			
			if (tmpStr != null) {
				StyleConstants.setFontFamily(tmpStyle, tmpStr);
			}
			
			if (st.getQFormat() != null) {
				tmpStyle.addAttribute(
					WordMLStyleConstants.QFormatAttribute
					, Boolean.valueOf(st.getQFormat().isVal()));
			}
			
			tmpStr = (st.getType() == null) ? PARAGRAPH_ATTR_VALUE : st.getType();
			tmpStyle.addAttribute(
				WordMLStyleConstants.StyleTypeAttribute, tmpStr);

			if (st.getLink() != null) {
				//A link value is a style id.
				//Check whether the link style has already been created.
				Style target = getChildStyle(idStyles, st.getLink().getVal());
				if (target != null) {
					org.docx4j.wml.Style targetDocxStyles =
						(org.docx4j.wml.Style)
							target.getAttribute(WordMLStyleConstants.DocxObjectAttribute);
					String targetType = 
						(String) target.getAttribute(WordMLStyleConstants.StyleTypeAttribute);
					if (PARAGRAPH_ATTR_VALUE.equalsIgnoreCase(tmpStr)
							&& CHARACTER_ATTR_VALUE.equalsIgnoreCase(targetType)
							&& targetDocxStyles.getRPr() != null) {
						StyleSheet.addAttributes(tmpStyle, targetDocxStyles.getRPr());
						
					} else if (CHARACTER_ATTR_VALUE.equalsIgnoreCase(tmpStr)
							&& PARAGRAPH_ATTR_VALUE.equalsIgnoreCase(targetType)
							&& targetDocxStyles.getPPr() != null) {
						StyleSheet.addAttributes(tmpStyle, targetDocxStyles.getPPr());
						
					} else {
						//Specs says to ignore this
					}
				}
			} //if (st.getLink() != null)
			
			if (st.getBasedOn() != null) {
				stylesWithBasedOn.add(tmpStyle);
			}
			
			Boolean qformat = (Boolean) tmpStyle.getAttribute(WordMLStyleConstants.QFormatAttribute);
			if (qformat.booleanValue()) {
				uiStyles.addAttribute(uiName, tmpStyle);
			}

			//FIXME: st.isDefault() is still buggy at this moment.
			//Therefore, we check whether uiName starts with "Normal".
			//Fix this condition when st.isDefault() has been fixed.
			if (st.isDefault() 
				&& qformat.booleanValue()
				&& uiName.startsWith("Normal")
				&& PARAGRAPH_ATTR_VALUE.equalsIgnoreCase(tmpStr)) {
				defaultStyle.addAttribute(WordMLStyleConstants.DefaultParagraphStyleNameAttribute, uiName);
			}
			
			if (log.isDebugEnabled()) {
				log.debug("initStyles(): style[" + (i++) + "]=" + tmpStyle);
			}
			
		} //for (org.docx4j.wml.Styles.Style st: styleList)
		styleList = null;
		
		for (Style style: stylesWithBasedOn) {
			
			if (log.isDebugEnabled()) {
				log.debug("initStyles(): style with BasedOn[" + (i++) + "]=" + style);
			}
			
			org.docx4j.wml.Style wmlStyle = WordMLStyleConstants.getDocxStyle(style);
			Style parent = getChildStyle(idStyles, wmlStyle.getBasedOn().getVal());
			if (parent != null) {
				String type = (String) style.getAttribute(WordMLStyleConstants.StyleTypeAttribute);
				String parentType = (String) parent.getAttribute(WordMLStyleConstants.StyleTypeAttribute);
				if (type.equals(parentType)) {
					if (PARAGRAPH_ATTR_VALUE.equals(type)
						|| CHARACTER_ATTR_VALUE.equals(type)) {
						
						// docx4all uses MutableAttributeSet's resolve function
						// to climb the style hierarchy.  It seems to work 
						// well.  (docx4j has a PropertyResolver class which
						// does this as well, but docx4all doesn't use it)
						
						style.setResolveParent(parent);
					}
				}
			}
		}
	}
	
	private Style addChildStyle(Style parent, String childName) {
		Style child = new NamedStyle(childName, parent);
		parent.addAttribute(childName, child);
		return child;
	}
	
	private Style getChildStyle(Style parent, String childName) {
		return (Style) parent.getAttribute(childName);
	}
	
	private class UIStyleComparator implements Comparator<Style> {
	    public int compare(Style s1, Style s2) {
	    	Integer p1 = (Integer) s1.getAttribute(WordMLStyleConstants.UiPriorityAttribute);
	    	Integer p2 = (Integer) s2.getAttribute(WordMLStyleConstants.UiPriorityAttribute);
	    	
	    	int sign = p1.compareTo(p2);
	    	if (sign == 0) {
	    		sign = s1.getName().compareToIgnoreCase(s2.getName());
	    	}
	    	
	    	return sign;
	    }
	} //StyleUIComparator inner class
	
}// StyleSheet class



















