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

package org.plutext.client.wrappedTransforms;

import java.math.BigInteger;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.docx4j.wml.Id;
import org.docx4j.wml.SdtBlock;
import org.docx4j.wml.Tag;
import org.plutext.client.Mediator;
import org.plutext.client.state.StateChunk;
import org.plutext.transforms.Transforms.T;

public abstract class TransformAbstract {

	private static Logger log = Logger.getLogger(TransformAbstract.class);

	protected SdtBlock sdt = null;

	public SdtBlock getSdt() {
		return sdt;
	}

	public TransformAbstract() {
	}

	public T t = null;

	public TransformAbstract(T t) {
		this.t = t;

		sequenceNumber = t.getSnum();

		sdt = t.getSdt();

		if (t.getIdref() != null) {
			// Case: Delete

			// Convert the idref to an id object
			org.docx4j.wml.ObjectFactory factory = new org.docx4j.wml.ObjectFactory();
			id = factory.createId();
			id.setVal(BigInteger.valueOf(t.getIdref()));

		} else if (t.getOp().equals("style")) {

			// No ID

		} else {

			// Case: Update, Insert

			id = sdt.getSdtPr().getId();

			tag = sdt.getSdtPr().getTag();

		}

		// log.warn("Parsed SDT ID " + id);

	}

	protected Id id;

	public Id getId() {
		return id;
	}

	public void setId(Id id) {
		this.id = id;
	}

	// The tag of the wrapped SDT
	protected Tag tag;

	public Tag getTag() {
		return tag;
	}

	public void setTag(Tag tag) {
		this.tag = tag;
	}

	// Has this transform been applied to the document yet?
	Boolean applied = false;

	public Boolean getApplied() {
		return applied;
	}

	public void setApplied(Boolean applied) {
		this.applied = applied;
	}

	// Is this transform something which came from this
	// plutext client? (If it is, we can always apply it without worrying
	// about conflicts)
	boolean local = false;

	public boolean isLocal() {
		return local;
	}

	public void setLocal(boolean local) {
		this.local = local;
	}

	// The ID of the transformation.
	protected long sequenceNumber = 0;

	public long getSequenceNumber() {
		return sequenceNumber;
	}

	public void setSequenceNumber(long sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
	}

	/* Code to apply the transform */
	public abstract long apply(Mediator mediator, HashMap<String, StateChunk> stateChunks);


}
