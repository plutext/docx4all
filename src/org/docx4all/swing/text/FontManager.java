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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.docx4all.ui.main.Constants;
import org.docx4all.ui.main.WordMLEditor;
import org.docx4j.fonts.Substituter;
import org.docx4j.fonts.microsoft.MicrosoftFonts;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
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
        
		
		boolean isWindows = 
			(System.getProperty("os.name").toUpperCase().indexOf("WINDOWS") > -1);
		if (isWindows) {
			substituter = null;
		} else {
			//In NON-WINDOWS platform we initialise substituter with
			//all available font family names
			substituter = new Substituter();
			
			Map<String, String> fontsInUse = new HashMap<String, String>(nameList.size());
			for (String s: nameList) {
				fontsInUse.put(s, s);
			}
			
			try {
				substituter.populateFontMappings(fontsInUse);
			} catch (Exception exc) {
				throw new RuntimeException(exc);
			}
		}
		
		// Populate AVAILABLE_FONT_FAMILY_NAMES.
		Collections.sort(nameList);
		nameList.add(0, UNKNOWN_FONT_NAME);
		AVAILABLE_FONT_FAMILY_NAMES = new String[nameList.size()];
		nameList.toArray(AVAILABLE_FONT_FAMILY_NAMES);		
	}
	
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
		if (substituter == null) {
			//In Windows we need not a substituter
			return;
		}
		
		// Handle fonts - this is platform specific
		// Algorithm - to be implemented:
		// 1.  Get a list of all the fonts in the document
		Map fontsInUse = docPackage.getMainDocumentPart().fontsInUse();
		
		// 2.  For each font, find the closest match on the system (use OO's VCL.xcu to do this)
		//     - do this in a general way, since docx4all needs this as well to display fonts
		try {
			substituter.populateFontMappings(fontsInUse);
		} catch (Exception exc) {
			throw new RuntimeException(exc);
		}
	}
	
	public String getFontNameInAction(String fontName) {
		if (substituter == null) {
			//In Windows we still render the font specified by fontName
			//although it may not be one of those in getAvailableFontFamilyNames().
			//Therefore this method returns fontName.
			return fontName;
		}
		
		String fontNameInAction = substituter.getPdfSubstituteFont(fontName);
		if (fontNameInAction.startsWith("noMapping")) {
			//should not be here unless org.docx4j.fonts.substitutions.FontSubstitutions.xml
			//is not complete.
			log.error("Cannot find font substitution for '" + fontName 
				+ "'. Default font name '" + getDocx4AllDefaultFontFamilyName()
				+ "' is used.");
			fontNameInAction = getDocx4AllDefaultFontFamilyName();
		}
		return fontNameInAction;
	}
	
}// FontManager class



















