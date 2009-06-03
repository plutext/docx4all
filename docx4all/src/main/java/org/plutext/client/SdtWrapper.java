package org.plutext.client;

import org.docx4j.wml.Id;
import org.docx4j.wml.ObjectFactory;
import org.docx4j.wml.SdtBlock;
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
 * We'll also need to change that to extract the id
 * from our tag.
 *
 */
public class SdtWrapper {
	
	public SdtWrapper(SdtBlock sdt) {
		this.sdt = sdt;
		id = sdt.getSdtPr().getId().getVal().toString();
	}

	// In TransformAbstract, this is convenient
	// (ie without an actual sdt)
	public SdtWrapper() { }
	
	
	// This is what we use to track the chunk
	// through Word editing sessions.
	private String id;
		
	
	public String getId() {
		return id;
	}
	
	
//	public void setId(Id id) {
//		this.id = id.getVal().toString();
//		
//		// and set it in the underlying SDT! 
//		sdt.getSdtPr().setId(id);
//	}
	
	public void setId(String id) {
		this.id = id;
		
		if (sdt!=null) {
			// and set it in the underlying SDT! 
			sdt.getSdtPr().getId().setVal(java.math.BigInteger.valueOf( Long.valueOf(id) ));
		}
	}
	
	public void setVersionNumber(long versionNumber) {
		
		ObjectFactory factory = new ObjectFactory();
		
		Tag tag = factory.createTag();
		tag.setVal(  Long.toString(versionNumber) ); 
		 
		sdt.getSdtPr().setTag(tag);
	}

	public String getVersionNumber() {  
		// TODO: decide whether to use string or long
		
		if (sdt.getSdtPr().getTag()==null) {
			return null;
		} else {
			return sdt.getSdtPr().getTag().getVal();
		}
		
	}
	
	public Tag getTag() {
		return sdt.getSdtPr().getTag();
	}

	
	private SdtBlock sdt; 
	public SdtBlock getSdt() {
		return sdt;
	}
	

}
