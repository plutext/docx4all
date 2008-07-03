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
public class DiffState {
	enum DiffStatus {
		MATCHED(1),
		NOMATCH(-1),
		UNKNOWN(-2);
		
		private final int _value;
		
		DiffStatus(int value) {
			_value = value;
		}
		
		int value() {
			return _value;
		}
	}// DiffStatus enum

	
	private final static int BAD_INDEX = -1;
	private int _startIndex;
	private int _length;

	public int getStartIndex() {
		return _startIndex;
	}

	public int getEndIndex() {
		return _startIndex + _length - 1;
	}

	public int getLength() {
		int len;
		if (_length > 0) {
			len = _length;
		} else {
			if (_length == 0) {
				len = 1;
			} else {
				len = 0;
			}
		}
		return len;
	}

	public DiffStatus getDiffStatus() {
		DiffStatus stat = null;
		if (_length > 0) {
			stat = DiffStatus.MATCHED;
		} else {
			switch (_length) {
			case -1:
				stat = DiffStatus.NOMATCH;
				break;
			default:
				if (_length < -2) {
					System.out
							.println("DiffState.getDiffStatus(): Invalid status: _length < -2");
				}
				stat = DiffStatus.UNKNOWN;
				break;
			}
		}
		return stat;
	}

	public DiffState() {
		setToUnkown();
	}

	protected void setToUnkown() {
		_startIndex = BAD_INDEX;
		_length = DiffStatus.UNKNOWN.value();
	}

	public void setMatch(int start, int length) {
		if (length <= 0) {
			throw new IllegalArgumentException(
					"Length must be greater than zero. length=" + length);
		}

		if (start < 0) {
			throw new IllegalArgumentException(
					"Start must be greater than or equal to zero. start="
							+ start);
		}
		_startIndex = start;
		_length = length;
	}

	public void setNoMatch() {
		_startIndex = BAD_INDEX;
		_length = DiffStatus.NOMATCH.value();
	}

	public boolean hasValidLength(int newStart, int newEnd,
			int maxPossibleDestLength) {
		if (_length > 0) // have unlocked match
		{
			if (maxPossibleDestLength < _length
				|| _startIndex < newStart 
				|| getEndIndex() > newEnd) {
				setToUnkown();
			}
		}
		return (_length != DiffStatus.UNKNOWN.value());
	}

	
}// DiffState class

