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

package org.plutext.client.diffengine;

/**
 * @author Jojada Tirtowidjojo - 02/07/2008
 */
public class DiffResultSpan implements Comparable<DiffResultSpan> {
	public static DiffResultSpan createNoChange(int destIndex, int sourceIndex,
			int length) {
		return new DiffResultSpan(DiffResultSpanStatus.NOCHANGE, destIndex,
				sourceIndex, length);
	}

	public static DiffResultSpan createReplace(int destIndex, int sourceIndex,
			int length) {
		return new DiffResultSpan(DiffResultSpanStatus.REPLACE, destIndex,
				sourceIndex, length);
	}

	public static DiffResultSpan createDeleteSource(int sourceIndex, int length) {
		return new DiffResultSpan(DiffResultSpanStatus.DELETE_SOURCE,
				BAD_INDEX, sourceIndex, length);
	}

	public static DiffResultSpan createAddDestination(int destIndex, int length) {
		return new DiffResultSpan(DiffResultSpanStatus.ADD_DESTINATION,
				destIndex, BAD_INDEX, length);
	}

	private final static int BAD_INDEX = -1;
	private int _destIndex;
	private int _sourceIndex;
	private int _length;
	private DiffResultSpanStatus _status;

	protected DiffResultSpan(DiffResultSpanStatus status, int destIndex,
			int sourceIndex, int length) {

		_status = status;
		_destIndex = destIndex;
		_sourceIndex = sourceIndex;
		_length = length;
	}

	public int getDestIndex() {
		return _destIndex;
	}

	public int getSourceIndex() {
		return _sourceIndex;
	}

	public int getLength() {
		return _length;
	}

	public DiffResultSpanStatus getDiffResultSpanStatus() {
		return _status;
	}

	public void addLength(int i) {
		_length += i;
	}

	public String toString() {
		return String.format("{0} (Dest: {1},Source: {2}) {3}", 
					_status.toString(), 
					Integer.valueOf(_destIndex), 
					Integer.valueOf(_sourceIndex),
					Integer.valueOf(_length));
	}

	public int compareTo(DiffResultSpan obj) {
		Integer thisIdx = Integer.valueOf(_destIndex);
		Integer objIdx = Integer.valueOf(((DiffResultSpan) obj)._destIndex);
		return thisIdx.compareTo(objIdx);
	}

}// DiffResultSpan class

