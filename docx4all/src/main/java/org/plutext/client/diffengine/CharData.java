/*
 *  This code from http://www.codeproject.com/KB/recipes/diffengine.aspx
 * 
 *  The author, Michael Potter, in a post to that page on 26 Dec 06, had 
 *  said: "You (and anyone else) may use the code anyway you wish at your own risk. 
 *  I don't require that you mention my name."
 * 
 *  On 23 Aug 2007, he again said "Code is free for use in anyway you want"
 */

using System;
using System.Collections;

namespace plutext.client.word2007.DifferenceEngine
{
	public class DiffList_CharData : IDiffList
	{
		private char[] _charList;

		public DiffList_CharData(string charData)
		{
			_charList = charData.ToCharArray();
		}
		#region IDiffList Members

		public int Count()
		{
			return _charList.Length;
		}

		public IComparable GetByIndex(int index)
		{
			return _charList[index];
		}

		#endregion
	}
}