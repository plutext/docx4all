/*
 *  This code from http://www.codeproject.com/KB/recipes/diffengine.aspx
 * 
 *  The author, Michael Potter, in a post to that page on 26 Dec 06, had 
 *  said: "You (and anyone else) may use the code anyway you wish at your own risk. 
 *  I don't require that you mention my name."
 * 
 *  On 23 Aug 2007, he again said "Code is free for use in anyway you want"
 */

package org.plutext.client.diffengine;

public class CharData implements IDiffList<Character> {
	private Character[] _charList;

	public CharData(String charData) {
		_charList = new Character[charData.length()];
		
		for (int i=0; i < charData.length(); i++) {
			_charList[i] = Character.valueOf(charData.charAt(i));
		}
	}

	public int count() {
		return _charList.length;
	}

	public Comparable<Character> getByIndex(int index) {
		return _charList[index];
	}
	
} //CharData class