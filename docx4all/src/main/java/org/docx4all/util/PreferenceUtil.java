package org.docx4all.util;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.docx4j.XmlUtils;

public class PreferenceUtil {
	
	// Whenever we flush Preferences in a Swing application on Linux, 
	// < Java 6u10 RC (as of b23)
	// we'll get java.util.prefs.BackingStoreException: java.lang.IllegalArgumentException: Not supported: indent-number
	// if we are using our Xalan jar.
	// See http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6396599
	// So Swing applications will need to use the original 
	// setting, which we record for their convenience here.
	// eg com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl
	// It would be nice to reset to the original whenever we finish
	// using, but that goal seems to be elusive!
	
	//		Exception in thread "Timer-0" java.lang.AbstractMethodError: org.apache.crimson.tree.XmlDocument.getXmlStandalone()Z
	//			at com.sun.org.apache.xalan.internal.xsltc.trax.DOM2TO.setDocumentInfo(DOM2TO.java:373)
	//			at com.sun.org.apache.xalan.internal.xsltc.trax.DOM2TO.parse(DOM2TO.java:127)
	//			at com.sun.org.apache.xalan.internal.xsltc.trax.DOM2TO.parse(DOM2TO.java:94)
	//			at com.sun.org.apache.xalan.internal.xsltc.trax.TransformerImpl.transformIdentity(TransformerImpl.java:663)		
	//
	
	public static void flush(Preferences prefs) {
		
		if (System.getProperty("os.name").toLowerCase().indexOf("linux")>-1
				&& System.getProperty("java.version").startsWith("1.6")
				&& System.getProperty("java.vendor").startsWith("Sun") ) {
			

			/* Shouldn't be necessary with modern docx4j? 
			 * 
				System.setProperty("javax.xml.transform.TransformerFactory", 
						XmlUtils.TRANSFORMER_FACTORY_ORIGINAL);  
			 */
			
			System.setProperty("javax.xml.parsers.SAXParserFactory", 
					"com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl");
			//System.setProperty("javax.xml.parsers.SAXParserFactory", "org.apache.xerces.jaxp.SAXParserFactoryImpl");
			System.setProperty("javax.xml.parsers.DocumentBuilderFactory", 
					"com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl");		
			//System.setProperty("javax.xml.parsers.DocumentBuilderFactory", "org.apache.xerces.jaxp.DocumentBuilderFactoryImpl");		
		}

		try {
			prefs.flush();
		} catch (BackingStoreException e) {
			e.printStackTrace();
		}
		
		
	}

}
