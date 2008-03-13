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
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.text.AttributeSet;
import javax.swing.text.StyleConstants;

import org.apache.log4j.Logger;
import org.docx4all.ui.main.Constants;
import org.docx4all.ui.main.WordMLEditor;
import org.docx4j.fonts.Substituter;
import org.docx4j.fonts.Substituter.FontMapping;
import org.docx4j.fonts.microsoft.MicrosoftFonts;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.WordprocessingML.FontTablePart;
import org.jdesktop.application.ResourceMap;

/**
 *	@author Jojada Tirtowidjojo - 05/03/2008
 */
public class FontManager {
	private static Logger log = Logger.getLogger(FontManager.class);
	
	public final static String UNKNOWN_FONT_NAME = "<Not Known>";
	public final static String UNKNOWN_FONT_SIZE = "##";
	
	private final static String DOCX4ALL_DEFAULT_FONT_FAMILY_NAME;
	private final static String DOCX4ALL_DEFAULT_FONT_SIZE;
	
	private final static FontManager _instance = new FontManager();
	
	private final static Substituter substituter;
	
	private final static String[] AVAILABLE_FONT_SIZES = new String[] {
		UNKNOWN_FONT_SIZE,
	    "8", "9", "10", "11", "12", "14", "16", "18",
	    "20", "22", "24", "26", "28", "32", "36", "40", 
	    "44", "48", "52", "56", "64", "72"
	};

	private final static String[] AVAILABLE_FONT_FAMILY_NAMES;
	
	static {
		//Prepare available fonts that are listed in font combobox.
		Map<String, MicrosoftFonts.Font> msFontsFilenames = 
			Substituter.getMsFontsFilenames();
		List<String> nameList = new ArrayList<String>();
		for (Map.Entry<String, MicrosoftFonts.Font> entry: msFontsFilenames.entrySet()) {
			MicrosoftFonts.Font font = entry.getValue();
			if (booleanValue(font.isCoreWebFont())
					|| booleanValue(font.isClearTypeCollection())
					|| booleanValue(font.isSecondary())) {
				nameList.add(font.getName());
			}
		}
		
		//Initialise DOCX4ALL_DEFAULT_FONT.
		//The font name and size are configured in WordMLEditor.properties file
        WordMLEditor editor = WordMLEditor.getInstance(WordMLEditor.class);
        ResourceMap rm = editor.getContext().getResourceMap(WordMLEditor.class);
        DOCX4ALL_DEFAULT_FONT_FAMILY_NAME = rm.getString(Constants.APP_DEFAULT_FONT_FAMILY_NAME);
        DOCX4ALL_DEFAULT_FONT_SIZE = rm.getString(Constants.APP_DEFAULT_FONT_SIZE);
        
        if (!nameList.contains(DOCX4ALL_DEFAULT_FONT_FAMILY_NAME)) {
			//defaultFontName has to be listed in nameList
			throw new RuntimeException(
					"Invalid "
					+ Constants.APP_DEFAULT_FONT_FAMILY_NAME
					+ " property value.");
        }
        
        boolean invalidSize = true;
        for (String s: AVAILABLE_FONT_SIZES) {
        	if (s.equals(DOCX4ALL_DEFAULT_FONT_SIZE)) {
        		invalidSize = false;
        	}
        }

        if (invalidSize) {
			throw new RuntimeException(
					"Invalid "
					+ Constants.APP_DEFAULT_FONT_SIZE
					+ " property value.");
        }
        		
		// Initialise substituter with all available font family names
		substituter = new Substituter();

		Map<String, String> fontsInUse = 
			new HashMap<String, String>(nameList.size());
		for (String s : nameList) {
			fontsInUse.put(s, s);
		}

		try {
			FontTablePart fontTablePart = 
				new org.docx4j.openpackaging.parts.WordprocessingML.FontTablePart();
			org.docx4j.wml.Fonts tablePartDefaultFonts = 
				(org.docx4j.wml.Fonts) fontTablePart.unmarshalDefaultFonts();
			// Process embedded fonts in fontTablePart.
			// This has to be done before calling populateFontMappings()
			// so that the embedded fonts can be taken into account.
			fontTablePart.processEmbeddings();
			substituter.populateFontMappings(fontsInUse, tablePartDefaultFonts);
		} catch (Exception exc) {
			throw new RuntimeException(exc);
		}
		
		
		// Populate AVAILABLE_FONT_FAMILY_NAMES.
		Collections.sort(nameList);
		nameList.add(0, UNKNOWN_FONT_NAME);
		AVAILABLE_FONT_FAMILY_NAMES = new String[nameList.size()];
		nameList.toArray(AVAILABLE_FONT_FAMILY_NAMES);		
	}
	
    private final Hashtable<FontTableKey, Font> _fontTable = 
    	new Hashtable<FontTableKey, Font>();
    private final FontTableKey _fontTableKey = new FontTableKey(null, 0, 0);
    
	public final static FontManager getInstance() {
		return _instance;
	}
	
	private final static boolean booleanValue(Boolean b) {
		return (b == null) ? false : b.booleanValue();
	}
	
	private FontManager() {
		super();
	}
	
	public String[] getAvailableFontFamilyNames() {
		return AVAILABLE_FONT_FAMILY_NAMES;
	}
	
	public String[] getAvailableFontSizes() {
		return AVAILABLE_FONT_SIZES;
	}
	
	public String getDocx4AllDefaultFontFamilyName() {
		return DOCX4ALL_DEFAULT_FONT_FAMILY_NAME;
	}
	
	public int getDocx4AllDefaultFontSize() {
		return Integer.parseInt(DOCX4ALL_DEFAULT_FONT_SIZE);
	}
	
	public void addFontsInUse(WordprocessingMLPackage docPackage) {
		Map fontsInUse = docPackage.getMainDocumentPart().fontsInUse();
		FontTablePart fontTablePart = docPackage.getMainDocumentPart().getFontTablePart();
		
		try {
			// Handle fonts - this is platform specific
			// Algorithm - to be implemented:
			// 1. Get a list of all the fonts in the document
			org.docx4j.wml.Fonts fonts;
			if (fontTablePart != null) {
				fonts = (org.docx4j.wml.Fonts) fontTablePart.getJaxbElement();
			} else {
				log.warn("FontTable missing; creating default part.");
				fontTablePart = new org.docx4j.openpackaging.parts.WordprocessingML.FontTablePart();
				fonts = (org.docx4j.wml.Fonts) fontTablePart.unmarshalDefaultFonts();
			}

			//2. Process embedded fonts in fontTablePart.
			//This has to be done before calling populateFontMappings()
			//so that the embedded fonts can be taken into account.
			fontTablePart.processEmbeddings();
			
			//3. For each font, find the closest match on the system (use OO's
			//VCL.xcu to do this)
			//- do this in a general way, since docx4all needs this as well to
			//display fonts
			substituter.populateFontMappings(fontsInUse, fonts);
			
			if (log.isDebugEnabled()) {
				int i = 0;
				Map fontMappings = substituter.getFontMappings();
				Iterator fontMappingsIterator = fontMappings.entrySet()
						.iterator();
				while (fontMappingsIterator.hasNext()) {
					Map.Entry pairs = (Map.Entry) fontMappingsIterator.next();

					String key = pairs.getKey().toString();
					FontMapping fm = (FontMapping) pairs.getValue();

					log.debug("FontMapping[" + (i++) + "]: key=" + key
							+ " tripletName=" + fm.getTripletName() + " -->> "
							+ fm.getEmbeddedFile());
				}
			}
			
		} catch (Exception exc) {
			throw new RuntimeException(exc);
		}
	}
	
    public Font getFontInAction(AttributeSet attr) {
		int style = Font.PLAIN;
		if (StyleConstants.isBold(attr)) {
			style |= Font.BOLD;
		}
		if (StyleConstants.isItalic(attr)) {
			style |= Font.ITALIC;
		}
		String family = StyleConstants.getFontFamily(attr);
		
		//font size in OpenXML is in half points, not points,
		//so we need to divide by 2
		int size = StyleConstants.getFontSize(attr)/2;
		
		//But Java2D �point� appears to be smaller than Windows �point.�
		//Adjust with experimental multiplication factor for now.
		size = size * 14 / 9;
			
		//Reduce the font size by 2 for superscript or subscript		
		if (StyleConstants.isSuperscript(attr)
				|| StyleConstants.isSubscript(attr)) {
			size -= 2;
		}

		return getFontInAction(family, style, size);
	}
    
	public Font getFontInAction(String family, int style, int size) {
		_fontTableKey.setValue(family, style, size);
		Font theFont = _fontTable.get(_fontTableKey);
		
		if (theFont == null) {
			//Not in cache.
			//Derive from Substituter.FontMapping
			String fmKey = Substituter.normalise(family);
			FontMapping fm = 
				(FontMapping) substituter.getFontMappings().get(fmKey);
			String path = null;
			if (fm != null && fm.getEmbeddedFile() != null) {
				path = fm.getEmbeddedFile();
				if (path.startsWith("file:/")) {
					if (System.getProperty("os.name").toUpperCase().indexOf("WINDOWS") > -1) {
						path = path.substring(6);
					} else {
						path = path.substring(5);
					}
				}
				
				if (log.isDebugEnabled()) {
					log.debug("family=" + family 
							+ " fmKey=" + fmKey 
							+ " --> FontMapping=" + fm
							+ " - FontMapping.getEmbeddedFile()=" + path);
				}
				
				try {
					int fontFormat = Font.TRUETYPE_FONT;
					if (path.toLowerCase().endsWith(".otf")) {
						fontFormat = Font.TYPE1_FONT;
					}
					theFont = Font.createFont(fontFormat, new File(path));
					theFont = theFont.deriveFont(style, size);
					
		            if (! sun.font.FontManager.fontSupportsDefaultEncoding(theFont)) {
		            	theFont = sun.font.FontManager.getCompositeFontUIResource(theFont);
		            }
		            
		            FontTableKey key = new FontTableKey(family, style, size);
					_fontTable.put(key, theFont);

				} catch (Exception exc) {
					// should not happen.
					throw new RuntimeException(exc);
				}
			} else {
				log.warn("Cannot create font '" + family + "'. Use Docx4all default font.");
				theFont = getFontInAction(getDocx4AllDefaultFontFamilyName(), Font.PLAIN, getDocx4AllDefaultFontSize());
			}
		}
		
		return theFont;
	}
	
    /**
     * key for TableFont
     */
    private static class FontTableKey {
        private String fontFamilyName;
        private int fontStyle;
        private int fontSize;

        public FontTableKey(String fontFamilyName, int fontStyle, int fontSize) {
            setValue(fontFamilyName, fontStyle, fontSize);
        }

        public void setValue(String fontFamilyName, int fontStyle, int fontSize) {
            this.fontFamilyName = (fontFamilyName != null) ? fontFamilyName.intern() : null;
            this.fontStyle = fontStyle;
            this.fontSize = fontSize;
        }
    
        public int hashCode() {
    	    int code = (fontFamilyName != null) ? fontFamilyName.hashCode() : 0;
            return code ^ fontStyle ^ fontSize;
        }
        
        public boolean equals(Object obj) {
            if (obj instanceof FontTableKey) {
            	FontTableKey ftk= (FontTableKey) obj;
                return fontSize == ftk.fontSize 
                		&& fontStyle == ftk.fontStyle
                		&& fontFamilyName.equals((ftk.fontFamilyName));
            }
            return false;
        }
    } //FontTableKey inner class


}// FontManager class



















