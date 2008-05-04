package org.plutext.client.state;

import java.util.ArrayList;
import org.docx4j.wml.SdtBlock;

public class Controls {
	
	// This class should define a list, since the
	// controls are ordered.
	
	ArrayList contentControls = new ArrayList<SdtBlock>();

	
	public void add(SdtBlock sdt) {
		
		// FIXME - add it in the correct order; check it is not already there.
		
		contentControls.add(sdt);
		
	}

	/** Remove this content control from Controls and the document.
	 *  Returns false if the id was not found.  */
	public boolean remove(org.docx4j.wml.Id id) {
		
		// Implement me
		return false;
	}
	
}
