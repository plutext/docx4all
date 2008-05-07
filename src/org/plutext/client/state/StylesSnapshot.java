/*
 *  Copyright 2008, Plutext Pty Ltd.
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

package org.plutext.client.state;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBElement;

import org.docx4j.wml.Style;

import org.apache.log4j.Logger;
import org.docx4j.wml.SdtBlock;

import org.docx4j.XmlUtils;
import org.plutext.client.wrappedTransforms.TransformAbstract;

/* This class keeps track of the styles we know to
     * be in use, and their definitions.  StateDocx 
     * contains an instance of this object.
     * 
     * It is important that if a new style is brought 
     * into use in the document, it is
     * defined in style.xml on the server (if not,
     * the style will be stripped when Word loads
     * the document!).
     * 
     * On ContentControl Exit, we check the style of
     * each paragraph/run (and ultimately table)
     * in the content control, and if a new style has
     * been brought into use, or the definition of a style
     * altered, we report that to the server via the
     * web service.
     * 
     * This kind of approach is necessary because it is 
     * very common for the user to change the style on
     * a paragraph without actually entering its content control!
     * Indeed, other pPr can be changed as well without entering the CC, eg alignment.
     * 
     * TODO: report all the styles this one is based on,
     * as well.
     * 
     * TODO: Updating other clients is a bit more of a challenge,
     * since we can't just paste the XML into the document
     * (or can we?).  One option is to parse the XML, and 
     * programmatically define a suitable style.  Another 
     * might be to use a context free chunk?  Another is 
     * to serialise the Style object, transmit that
     * to the server, and deserialise it on the other 
     * clients.
     */
    public class StylesSnapshot
    {
    	
// TODO - consider how this relates to org.docx4all.swing.text.StyleSheet  
// and to org.docx4j.openpackaging.parts.WordprocessingML.StyleDefinitionsPart
    	
    	private static Logger log = Logger.getLogger(StylesSnapshot.class);
    	
        /*
         * 
            <w:styles xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
              <w:docDefaults>
            :
              <w:latentStyles w:defLockedState="0" w:defUIPriority="99" w:defSemiHidden="1" w:defUnhideWhenUsed="1" w:defQFormat="0" w:count="267">
            :
              <w:lsdException w:name="TOC Heading" w:uiPriority="39" w:qFormat="1" />
              </w:latentStyles>
              <w:style w:type="paragraph" w:default="1" w:styleId="Normal">
                <w:name w:val="Normal" />
            :
              </w:style>

         */


    	// Copy of the styles at a point in time
        HashMap<String, String> stylesSnapshot = new HashMap<String, String>();
        
        // Live styles - regenerated as necessary
        HashMap<String, Style> stylesLive = null;

        // When marshalling, suppress XML declaration
    	boolean suppressDeclaration = true;
        
    	org.docx4j.wml.Styles docxStyles;
    	
        public StylesSnapshot(org.docx4j.wml.Styles docxStyles)
        {
        	this.docxStyles = docxStyles;

        	HashMap<String, Style> stylesLive = new HashMap<String, Style>(); 
            //System.Diagnostics.Debug.WriteLine("Hello!!");
            for (Style s : docxStyles.getStyle() )
            {
                //log.warn(s.OuterXml);
                //log.warn(getStyleName(s));

            	stylesLive.put(getStyleId(s), s);
            	
            	stylesSnapshot.put(getStyleId(s), XmlUtils.marshaltoString(s, suppressDeclaration));

            }
        }

        /*public String getStyleName(XmlNode s)
        {
              //<w:style w:type="paragraph" w:default="1" w:styleId="Normal">
              //  <w:name w:val="Normal" />
            return s.FirstChild.Attributes.GetNamedItem("val", WORDML_NAMESPACE).InnerText;
        }*/

        public String getStyleId(Style s)
        {
            //<w:style w:type="paragraph" w:default="1" w:styleId="Normal">
            return s.getStyleId();
        }

//        public XmlNodeList getStyleNodes()
//        {
//            String oXML = Globals.ThisAddIn.Application.ActiveDocument.WordOpenXML;
//            XmlDocument doc = new XmlDocument();
//            doc.LoadXml(oXML);
//            XmlNamespaceManager nsmgr = new XmlNamespaceManager(doc.NameTable);
//            nsmgr.AddNamespace("w", Namespaces.WORDML_NAMESPACE);
//
//            return doc.SelectNodes("//w:style", nsmgr);
//        }


//        public Style getStyleNode(String stylename)
//        {
//        	return (Style)stylesLive.get(stylename);
//
//        }
        
        


        /* Iterate through the content control looking for any
         * styles which have been added to the document, or updated.
         * Return a string containing these. */
        public String identifyAlteredStyles(SdtBlock cc)
        {
        	
        	// Get the styles in the style definitions part
        	stylesLive = new HashMap<String, Style>();
            for (Style s : docxStyles.getStyle() ) {
            	stylesLive.put(getStyleId(s), s);
            }
        	
        	// Collect the styles in this SdtBlock
        	HashMap stylesInUse = new HashMap();        	
        	traverseRecursive(cc.getSdtContent().getEGContentBlockContent(), stylesInUse);
        	
            StringBuilder result = new StringBuilder();

            // See if any of them have changed
    		Iterator stylesInUseIterator = stylesInUse.entrySet().iterator();
    	    while (stylesInUseIterator.hasNext()) {
    	        Map.Entry pairs = (Map.Entry)stylesInUseIterator.next();
    	        
                String styleName = (String)pairs.getValue();
                
                result.append( identifyAlteredStylesWorker(styleName)  );
    	    }
            return result.toString();
        }


        private String identifyAlteredStylesWorker(String styleName)
        {
            StringBuilder result = new StringBuilder();

            try {
            	
            	Style style = (Style)stylesLive.get(styleName);
            	String currentStyleXml = XmlUtils.marshaltoString(style, suppressDeclaration); 
            	
                String cachedStyleXml = (String)stylesSnapshot.get(styleName);
                
                if (cachedStyleXml==null) {
                    // This is a new style added to the document!
                    result.append("<!-- New -->");
                    result.append(currentStyleXml);
                    stylesSnapshot.put(styleName, currentStyleXml);

                    //log.warn("New style: " + styleName);
                	
                } else if ( !currentStyleXml.equals(cachedStyleXml)) {
                    // The style definition has changed, so add it to result
                    result.append("<!-- Updated -->");
                    result.append(currentStyleXml);

                    // .. and update the hashmap
                    stylesSnapshot.put(styleName, currentStyleXml);
                    // or styleMap.Add(getStyleName(currentStyleNode), currentStyleNode);

                    // log.warn("Old " + cachedStyleNode.OuterXml + " New " + currentStyleNode.OuterXml);
                }
                // else no change
            }
            catch (Exception e) {
            	e.printStackTrace();
            }
            return result.toString();


        }


        
    	void traverseRecursive(List <Object> children,  Map stylesInUse){
    		
    		for (Object o : children ) {
    						
    			//log.debug("object: " + o.getClass().getName() );
    			
    			if (o instanceof org.docx4j.wml.P) {
    				
    				org.docx4j.wml.P p = (org.docx4j.wml.P) o;
    		
    				if (p.getPPr() != null && p.getPPr().getPStyle() != null) {
    					// Note this paragraph style
    					log.debug("put style "
    							+ p.getPPr().getPStyle().getVal());
    					stylesInUse.put(p.getPPr().getPStyle().getVal(), 
    									p.getPPr().getPStyle().getVal());
    				}
    		
    				if (p.getPPr() != null && p.getPPr().getRPr() != null) {
    					// Inspect RPr
    					inspectRPr(p.getPPr().getRPr(), stylesInUse);
    				}
    		
    				traverseRecursive(p.getParagraphContent(), stylesInUse);
    		
    			} else if (o instanceof org.docx4j.wml.SdtContentBlock) {

    				org.docx4j.wml.SdtBlock sdt = (org.docx4j.wml.SdtBlock) o;
    				
    				// Don't bother looking in SdtPr
    				
    				traverseRecursive(sdt.getSdtContent().getEGContentBlockContent(),
    						stylesInUse);
    				
    				
    			} else if (o instanceof org.docx4j.wml.R) {

    				org.docx4j.wml.R run = (org.docx4j.wml.R) o;
    				if (run.getRPr() != null) {
    					inspectRPr(run.getRPr(), stylesInUse);
    				}

    				// don't need to traverse run.getRunContent()
    				
    			} else if (o instanceof org.w3c.dom.Node) {
    				
    				// If Xerces is on the path, this will be a org.apache.xerces.dom.NodeImpl;
    				// otherwise, it will be com.sun.org.apache.xerces.internal.dom.ElementNSImpl;
    				
    				// Ignore these, eg w:bookmarkStart
    				
    				log.debug("not traversing into unhandled Node: " + ((org.w3c.dom.Node)o).getNodeName() );
    				
    			} else if ( o instanceof javax.xml.bind.JAXBElement) {

    				log.debug( "Encountered " + ((JAXBElement) o).getDeclaredType().getName() );
    					
//    				if (((JAXBElement) o).getDeclaredType().getName().equals(
//    						"org.docx4j.wml.P")) {
//    					org.docx4j.wml.P p = (org.docx4j.wml.P) ((JAXBElement) o)
//    							.getValue();
    //
//    					if (p.getPPr() != null && p.getPPr().getPStyle() != null) {
//    						// Note this paragraph style
//    						log.debug("put style "
//    								+ p.getPPr().getPStyle().getVal());
//    						stylesInUse.put(p.getPPr().getPStyle().getVal(), p
//    								.getPPr().getPStyle().getVal());
//    					}
    //
//    					if (p.getPPr() != null && p.getPPr().getRPr() != null) {
//    						// Inspect RPr
//    						inspectRPr(p.getPPr().getRPr(), fontsDiscovered,
//    								stylesInUse);
//    					}
    //
//    					traverseMainDocumentRecursive(p.getParagraphContent(),
//    							fontsDiscovered, stylesInUse);
    //
//    				}
    				
    			} else {
    				log.error( "UNEXPECTED: " + o.getClass().getName() );
    			} 
    		}
    	}
    	
        private void inspectRPr(Object rPrObj, Map stylesInUse) {
        	
        	if ( rPrObj instanceof org.docx4j.wml.RPr) {

        		org.docx4j.wml.RPr rPr =  (org.docx4j.wml.RPr)rPrObj;
        		
            	if (rPr.getRStyle()!=null) {
            		// 	Note this run style
            		//log.debug("put style " + rPr.getRStyle().getVal() );
            		stylesInUse.put(rPr.getRStyle().getVal(), rPr.getRStyle().getVal());
            	}
        		
        		
        	} else if ( rPrObj instanceof org.docx4j.wml.ParaRPr) {

        		org.docx4j.wml.ParaRPr rPr =  (org.docx4j.wml.ParaRPr)rPrObj;
        		
            	if (rPr.getRStyle()!=null) {
            		// 	Note this run style
            		//log.debug("put style " + rPr.getRStyle().getVal() );
            		stylesInUse.put(rPr.getRStyle().getVal(), rPr.getRStyle().getVal());
            	}
        		
        		
        	} else {
        		
        		log.error("Expected some kind of rPr, not " + rPrObj.getClass().getName() );    		
        	}
        	
    }
        

    }
