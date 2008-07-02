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

import java.util.ArrayList;
import java.util.Collections;

import org.plutext.client.Skeleton;
import org.plutext.client.TextLine;

/**
 * @author Jojada Tirtowidjojo - 02/07/2008
 */
public class DiffEngine {
	private IDiffList<?> _source;

	public IDiffList<?> getSource() {
		return _source;
	}

	private IDiffList<?> _dest;

	public IDiffList<?> getDestination() {
		return _dest;
	}

	private ArrayList<DiffResultSpan> _matchList;

	private DiffEngineLevel _level;

	private DiffStateList _stateList;

	private ArrayList<DiffResultSpan> _diffLines = null;

	public ArrayList<DiffResultSpan> getDiffLines() {
		return _diffLines;
	}

	public DiffEngine() {
		_source = null;
		_dest = null;
		_matchList = null;
		_stateList = null;
		_level = DiffEngineLevel.FAST_IMPERFECT;
	}

    @SuppressWarnings("unchecked")
	private int getSourceMatchLength(int destIndex, int sourceIndex,
			int maxLength) {
		int matchCount;
		for (matchCount = 0; matchCount < maxLength; matchCount++) {
			Comparable dest = _dest.getByIndex(destIndex + matchCount);
			Comparable src = _source.getByIndex(sourceIndex + matchCount);
			int result = ((Comparable) dest).compareTo(src);
			
			if (result != 0) {
				break;
			}
		}
		return matchCount;
	}
	
	private void getLongestSourceMatch(DiffState curItem, int destIndex,
			int destEnd, int sourceStart, int sourceEnd) {

		int maxDestLength = (destEnd - destIndex) + 1;
		int curLength = 0;
		int curBestLength = 0;
		int curBestIndex = -1;
		int maxLength = 0;
		for (int sourceIndex = sourceStart; sourceIndex <= sourceEnd; sourceIndex++) {
			maxLength = Math.min(maxDestLength, (sourceEnd - sourceIndex) + 1);
			if (maxLength <= curBestLength) {
				// No chance to find a longer one any more
				break;
			}
			curLength = getSourceMatchLength(destIndex, sourceIndex, maxLength);
			if (curLength > curBestLength) {
				// This is the best match so far
				curBestIndex = sourceIndex;
				curBestLength = curLength;
			}
			// jump over the match
			sourceIndex += curBestLength;
		}
		// DiffState cur = _stateList.GetByIndex(destIndex);
		if (curBestIndex == -1) {
			curItem.setNoMatch();
		} else {
			curItem.setMatch(curBestIndex, curBestLength);
		}

	}

	private void processRange(int destStart, int destEnd, int sourceStart,
			int sourceEnd) {
		int curBestIndex = -1;
		int curBestLength = -1;
		int maxPossibleDestLength = 0;
		DiffState curItem = null;
		DiffState bestItem = null;
		for (int destIndex = destStart; destIndex <= destEnd; destIndex++) {
			maxPossibleDestLength = (destEnd - destIndex) + 1;
			if (maxPossibleDestLength <= curBestLength) {
				// we won't find a longer one even if we looked
				break;
			}
			curItem = _stateList.getByIndex(destIndex);

			if (!curItem.hasValidLength(sourceStart, sourceEnd,
					maxPossibleDestLength)) {
				// recalc new best length since it isn't valid or has never been
				// done.
				getLongestSourceMatch(curItem, destIndex, destEnd, sourceStart,
						sourceEnd);
			}
			if (curItem.getDiffStatus() == DiffState.DiffStatus.MATCHED) {
				switch (_level) {
				case FAST_IMPERFECT:
					if (curItem.getLength() > curBestLength) {
						// this is longest match so far
						curBestIndex = destIndex;
						curBestLength = curItem.getLength();
						bestItem = curItem;
					}
					// Jump over the match
					destIndex += curItem.getLength() - 1;
					break;
				case MEDIUM:
					if (curItem.getLength() > curBestLength) {
						// this is longest match so far
						curBestIndex = destIndex;
						curBestLength = curItem.getLength();
						bestItem = curItem;
						// Jump over the match
						destIndex += curItem.getLength() - 1;
					}
					break;
				default:
					if (curItem.getLength() > curBestLength) {
						// this is longest match so far
						curBestIndex = destIndex;
						curBestLength = curItem.getLength();
						bestItem = curItem;
					}
					break;
				}
			}
		}
		if (curBestIndex < 0) {
			// we are done - there are no matches in this span
		} else {

			int sourceIndex = bestItem.getStartIndex();
			_matchList.add(DiffResultSpan.createNoChange(curBestIndex,
					sourceIndex, curBestLength));
			if (destStart < curBestIndex) {
				// Still have more lower destination data
				if (sourceStart < sourceIndex) {
					// Still have more lower source data
					// Recursive call to process lower indexes
					processRange(destStart, curBestIndex - 1, sourceStart,
							sourceIndex - 1);
				}
			}
			int upperDestStart = curBestIndex + curBestLength;
			int upperSourceStart = sourceIndex + curBestLength;
			if (destEnd > upperDestStart) {
				// we still have more upper dest data
				if (sourceEnd > upperSourceStart) {
					// set still have more upper source data
					// Recursive call to process upper indexes
					processRange(upperDestStart, destEnd, upperSourceStart,
							sourceEnd);
				}
			}
		}
	}

	public <T> long processDiff(
		IDiffList<? extends T> source, 
		IDiffList<? extends T> destination,
		DiffEngineLevel level) {
		_level = level;
		return processDiff(source, destination);
	}

	public long processDiff(
		IDiffList<?> source, 
		IDiffList<?> destination) {
		long timeStart = System.currentTimeMillis();

		_source = source;
		_dest = destination;
		_matchList = new ArrayList<DiffResultSpan>();

		int dcount = _dest.count();
		int scount = _source.count();

		if ((dcount > 0) && (scount > 0)) {
			_stateList = new DiffStateList(dcount);
			processRange(0, dcount - 1, 0, scount - 1);
		}

		diffReport();

		return System.currentTimeMillis() - timeStart;
	}

	private boolean addChanges(ArrayList<DiffResultSpan> report, int curDest,
			int nextDest, int curSource, int nextSource) {
		boolean retval = false;
		int diffDest = nextDest - curDest;
		int diffSource = nextSource - curSource;
		int minDiff = 0;
		// if (diffDest > 0)
		// {
		// if (diffSource > 0)
		// {
		// minDiff = Math.Min(diffDest,diffSource);
		// report.Add(DiffResultSpan.CreateReplace(curDest,curSource,minDiff));
		// if (diffDest > diffSource)
		// {
		// curDest+=minDiff;
		// report.Add(DiffResultSpan.CreateAddDestination(curDest,diffDest -
		// diffSource));
		// }
		// else
		// {
		// if (diffSource > diffDest)
		// {
		// curSource+= minDiff;
		// report.Add(DiffResultSpan.CreateDeleteSource(curSource,diffSource -
		// diffDest));
		// }
		// }
		// }
		// else
		// {
		// report.Add(DiffResultSpan.CreateAddDestination(curDest,diffDest));
		// }
		// retval = true;
		// }
		// else
		// {
		// if (diffSource > 0)
		// {
		// report.Add(DiffResultSpan.CreateDeleteSource(curSource,diffSource));
		// retval = true;
		// }
		// }

		if (diffDest > 0) {
			report.add(DiffResultSpan.createAddDestination(curDest, diffDest));
			retval = true;
		}
		if (diffSource > 0) {
			report
					.add(DiffResultSpan.createDeleteSource(curSource,
							diffSource));
			retval = true;
		}

		return retval;
	}

	public void diffReport() {
		_diffLines = new ArrayList<DiffResultSpan>();
		int dcount = _dest.count();
		int scount = _source.count();

		// Deal with the special case of empty files
		if (dcount == 0) {
			if (scount > 0) {
				_diffLines.add(DiffResultSpan.createDeleteSource(0, scount));
			}
			return;
		} else {
			if (scount == 0) {
				_diffLines.add(DiffResultSpan.createAddDestination(0, dcount));
				return;
			}
		}

		Collections.sort(_matchList);
		int curDest = 0;
		int curSource = 0;
		DiffResultSpan last = null;

		// Process each match record
		for (DiffResultSpan drs : _matchList) {
			if ((!addChanges(_diffLines, curDest, drs.getDestIndex(),
					curSource, drs.getSourceIndex()))
					&& (last != null)) {
				last.addLength(drs.getLength());
			} else {
				_diffLines.add(drs);
			}
			curDest = drs.getDestIndex() + drs.getLength();
			curSource = drs.getSourceIndex() + drs.getLength();
			last = drs;
		}

		// Process any tail end data
		addChanges(_diffLines, curDest, dcount, curSource, scount);

	}

	public String Results(Skeleton source, Skeleton destination) {

		String result = "";
		int cnt = 1;
		int i;

		for (DiffResultSpan drs : getDiffLines()) {
			switch (drs.getDiffResultSpanStatus()) {
			case DELETE_SOURCE:
				for (i = 0; i < drs.getLength(); i++) {
					result += "\n"
							+ ((TextLine) source.getByIndex(drs
									.getSourceIndex()
									+ i)).Line
							+ " not at this location in dest";
					cnt++;
				}

				break;
			case NOCHANGE:
				for (i = 0; i < drs.getLength(); i++) {
					result += "\n"
							+ ((TextLine) source.getByIndex(drs
									.getSourceIndex()
									+ i)).Line
							+ "\t"
							+ ((TextLine) destination.getByIndex(drs
									.getDestIndex()
									+ i)).Line + " (no change)";
					cnt++;
				}

				break;
			case ADD_DESTINATION:
				for (i = 0; i < drs.getLength(); i++) {
					result += "\n"
							+ "---"
							+ "\t"
							+ ((TextLine) destination.getByIndex(drs
									.getDestIndex()
									+ i)).Line
							+ " not at this location in source";
					cnt++;
				}

				break;
			case REPLACE:
				for (i = 0; i < drs.getLength(); i++) {
					result += "\n"
							+ ((TextLine) source.getByIndex(drs
									.getSourceIndex()
									+ i)).Line
							+ "\t"
							+ ((TextLine) destination.getByIndex(drs
									.getDestIndex()
									+ i)).Line + " replaced ";
					cnt++;
				}

				break;
			}

		}

		return result;
	}

}// DiffEngine class

