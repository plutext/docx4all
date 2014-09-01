package org.plutext.client;

import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.docx4j.model.sdt.QueryString;
import org.docx4j.wml.ObjectFactory;
import org.docx4j.wml.SdtBlock;
import org.docx4j.wml.SdtPr;
import org.docx4j.wml.Tag;

/**
 * Encapsulates an Sdt.
 * All ID, version set/get *must* be done through here.
 * (Reason being that Word arbitrarily resets
 *  ID values, so we can't rely on that field, and
 *  must instead store our ID in the tag field.)
 *  
 * The equivalent class in plutext-server is called
 * Rib.
 * 
 * NB: TransformMove and several other places
 * do SdtBlockML .getSdtProperties().getIdValue().
 * 
 * We'll also need to change that to extract the plutextId
 * from our tag.
 *
 */
public class SdtWrapper {
	
	private static Logger log = LoggerFactory.getLogger(SdtWrapper.class);
	
	public final static String PLUTEXT_ID     ="p:id";
	public final static String PLUTEXT_VERSION="p:v";
	
	public SdtWrapper(SdtBlock sdt) {
		this.sdt = sdt;
		//plutextId = sdt.getSdtPr().getId().getVal().toString();
		
		HashMap map = QueryString.parseQueryString( sdt.getSdtPr().getTag().getVal() );
		
		plutextId = (String)map.get(PLUTEXT_ID);
		version = (String)map.get(PLUTEXT_VERSION);
		
	}

	// In TransformAbstract, this is convenient
	// (ie without an actual sdt)
	public SdtWrapper() { }
	
	
	// This is what we use to track the chunk
	// through Word editing sessions.
	private String plutextId;
	public String getPlutextId() {
		return plutextId;
	}
	
	
//	public void setId(Id plutextId) {
//		this.id = plutextId.getVal().toString();
//		
//		// and set it in the underlying SDT! 
//		sdt.getSdtPr().setId(plutextId);
//	}
	
//	public void setId(String plutextId) {
//		this.id = plutextId;
//		
//		if (sdt!=null) {
//			// and set it in the underlying SDT! 
//			sdt.getSdtPr().getId().setVal(java.math.BigInteger.valueOf( Long.valueOf(plutextId) ));
//		}
//	}
	
	public void setVersionNumber(long versionNumber) {
		
		version = Long.toString(versionNumber);
				
		ObjectFactory factory = new ObjectFactory();		
		Tag tag = factory.createTag();
		tag.setVal( generateTag(plutextId, version)  ); 
		
		log.debug("Setting tag: "+ tag.getVal() );
		 
		sdt.getSdtPr().setTag(tag);
	}

	private String version;	
	public String getVersionNumber() {  
		return version;
	}
	
	public Tag getTag() {
		return sdt.getSdtPr().getTag();
	}

	
	private SdtBlock sdt; 
	public SdtBlock getSdt() {
		return sdt;
	}
	

	// Extension function for XSLT, also used above
	public static String generateTag(String id, String version) {
		
		HashMap<String, String> map = new HashMap<String, String>();
		map.put(PLUTEXT_ID, id);
		map.put(PLUTEXT_VERSION, version );
		return QueryString.create(map);	
		
	}

	public static String getPlutextId(String tag) {
		HashMap map = QueryString.parseQueryString( tag );
		return (String)map.get(PLUTEXT_ID);		
	}
	
	public static String getPlutextId(SdtPr sdtPr) {
		HashMap map = QueryString.parseQueryString( sdtPr.getTag().getVal() );		
		return (String)map.get(PLUTEXT_ID);
	}
	
	public static String getVersionNumber(SdtPr sdtPr) {
		HashMap map = QueryString.parseQueryString( sdtPr.getTag().getVal() );		
		return (String) map.get(PLUTEXT_VERSION);
	}
	
	public static String getVersionNumber(String tag) {
		HashMap map = QueryString.parseQueryString( tag );
		return (String) map.get(PLUTEXT_VERSION);		
	}
	
}
