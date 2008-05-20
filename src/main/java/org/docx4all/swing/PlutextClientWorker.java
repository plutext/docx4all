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

package org.docx4all.swing;

import java.util.List;

import javax.swing.SwingWorker;

import org.apache.log4j.Logger;
import org.docx4j.wml.SdtBlock;
import org.plutext.client.ServerTo;

/**
 *	@author Jojada Tirtowidjojo - 14/05/2008
 */
public class PlutextClientWorker extends SwingWorker<Void, Void> {
	private static Logger log = Logger.getLogger(PlutextClientWorker.class);
	
	private ServerTo plutextClient;
	private List<SdtBlock> dirtySdtBlocks;
	private List<SdtBlock> deletedSdtBlocks;
	private SdtBlock sdtBlockAtWork;
	
	public PlutextClientWorker(ServerTo plutextClient) {
		this.plutextClient = plutextClient;
		this.deletedSdtBlocks = null;
		this.dirtySdtBlocks = null;
		this.sdtBlockAtWork = null;
	}
	
	public void setDirtySdtBlocks(List<SdtBlock> list) {
		this.dirtySdtBlocks = list;
	}
	
	public void setDeletedSdtBlocks(List<SdtBlock> list) {
		this.deletedSdtBlocks = list;
	}
	
	public void setSdtBlockAtWork(SdtBlock sdt) {
		this.sdtBlockAtWork = sdt;
	}
	
	public SdtBlock getSdtBlockAtWork() {
		return this.sdtBlockAtWork;
	}
	
	public boolean hasWorkToDo() {
		return plutextClient != null 
				&& ((dirtySdtBlocks != null && !dirtySdtBlocks.isEmpty())
					|| (deletedSdtBlocks != null && !deletedSdtBlocks.isEmpty())
					|| sdtBlockAtWork != null);
	}
	
    @Override public Void doInBackground() {
    	log.debug("Background process START...@" + hashCode());
    	if (plutextClient != null) {
    		if (deletedSdtBlocks != null) {
				for (SdtBlock sdt:deletedSdtBlocks) {
					plutextClient.userDeletesContentControl(sdt);
				}
    		}
			if (dirtySdtBlocks != null) {
				for (SdtBlock sdt:dirtySdtBlocks) {
					if (sdtBlockAtWork == null
						|| !sdtBlockAtWork.getSdtPr().getId().equals(sdt.getSdtPr().getId())) {
						plutextClient.userExitsContentControl(sdt);
					}
				}
			}
			if (sdtBlockAtWork != null) {
				plutextClient.userEntersContentControl(sdtBlockAtWork);
			}
		}
    	return null;
    }
    
    @Override protected void done() {
    	String s = null;
    	if (isDone()) {
    		s = "Background process DONE !...@" + hashCode();
    	} else if (isCancelled()){
    		s = "Background process CANCELLED !...@" + hashCode();
    	} else {
    		s = "Background process ENDS (SwingWorker.StateValue = " + getState() + ")...@" + hashCode();
    	}
    	log.debug(s);
    }

}// PlutextClientWorker class



















